import { expect, type Page, test } from '@playwright/test';
import { abrir, sinViolaciones, vigilarConsola } from './support';

// Iteración 2: la pantalla del bar (PB-17 a PB-20, PB-38). Lienzo de 1280 × 720 ampliado.

async function encender(page: Page) {
  await page.getByRole('button', { name: 'Encender' }).click();
}

test.describe('a 1280 × 720', () => {
  test.use({ viewport: { width: 1280, height: 720 } });

  test('lo que suena, lo que viene con su subida y el QR (PB-17, RF-016)', async ({ page }) => {
    const errores = vigilarConsola(page);
    await abrir(page, '/b/demo/pantalla');
    await expect(page.getByRole('heading', { name: 'Encender la pantalla' })).toBeVisible();
    await sinViolaciones(page, 'TV antes de encender');
    await encender(page);
    await expect(page.getByRole('heading', { level: 1, name: 'Bar de demostración' })).toBeVisible();
    await expect(page.getByText('Suena ahora, pedida por el público')).toBeVisible();
    const viene = page.getByRole('region', { name: 'Lo que viene' }).getByRole('listitem');
    await expect(viene).toHaveCount(5);
    await expect(viene.nth(2)).toContainText('Monaco');
    await expect(viene.nth(2)).toContainText('2 puestos subidos');
    await expect(viene.nth(0)).toContainText('Grupo Niche, en 3 min');
    await expect(page.getByText('Y 4 más en la cola.')).toBeVisible();
    await expect(page.getByRole('img', { name: /Código QR/ })).toBeVisible();
    await sinViolaciones(page, 'TV');
    expect(errores).toEqual([]);
  });

  test('cuando termina la canción, la TV pasa a la siguiente (RF-019)', async ({ page }) => {
    await abrir(page, '/b/demo/pantalla');
    await encender(page);
    await expect(page.getByRole('heading', { level: 2, name: 'Rebelión' })).toBeVisible();
    // Lo que reportaría el reproductor de YouTube al terminar el video.
    await page.evaluate(async () => {
      const ahora = await fetch('/api/bares/demo/ahora').then((r) => r.json());
      await fetch(`/api/bares/demo/pantalla/reproducciones/${ahora.reproduccion.id}/fin`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Idempotency-Key': `${ahora.reproduccion.id}-fin` },
        body: JSON.stringify({ posicion_seg: 358, codigo_youtube: null }),
      });
    });
    await expect(page.getByRole('heading', { level: 2, name: 'Cali Pachanguero' })).toBeVisible({ timeout: 3000 });
    await expect(page.getByRole('region', { name: 'Lo que viene' }).getByRole('listitem').first()).toContainText('DtMF');
  });

  test('un voto desde un teléfono reordena la TV en menos de 3 s (RNF-009)', async ({ page, context }) => {
    await abrir(page, '/b/demo/pantalla');
    await encender(page);
    const telefono = await context.newPage();
    await telefono.setViewportSize({ width: 360, height: 780 });
    await telefono.goto('/b/demo/cola');
    await telefono.getByRole('button', { name: 'Votar por La Bicicleta' }).click();
    const viene = page.getByRole('region', { name: 'Lo que viene' }).getByRole('listitem');
    await expect(viene.nth(2)).toContainText('La Bicicleta', { timeout: 3000 });
    await expect(viene.nth(2)).toContainText('16');
  });

  test('fuera de horario: la luz se apaga, el horario y la lista de la casa sonando', async ({ page }) => {
    await abrir(page, '/b/demo/pantalla', 'fuera-de-horario');
    await encender(page);
    await expect(page.getByText('Pedidos cerrados')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Por ahora no recibimos canciones' })).toBeVisible();
    await expect(page.getByText(/Los pedidos abren hoy a las 8:00/)).toBeVisible();
    await expect(page.getByText('Suena ahora, de la lista de la casa')).toBeVisible();
    await sinViolaciones(page, 'TV fuera de horario');
  });

  test('cola vacía: invita a escanear y pedir la primera', async ({ page }) => {
    await abrir(page, '/b/demo/pantalla', 'cola-vacia');
    await encender(page);
    await expect(page.getByRole('heading', { name: 'La cola está vacía' })).toBeVisible();
    await sinViolaciones(page, 'TV cola vacía');
  });
});

for (const [ancho, alto] of [
  [1920, 1080],
  [3840, 2160],
  [1366, 768],
] as const) {
  test.describe(`a ${ancho} × ${alto}`, () => {
    test.use({ viewport: { width: ancho, height: alto } });

    test('el lienzo llena la pantalla y el QR cabe entero', async ({ page }) => {
      await abrir(page, '/b/demo/pantalla');
      await encender(page);
      const qr = await page.locator('.qr-tv').boundingBox();
      expect(qr).not.toBeNull();
      expect(qr!.y + qr!.height).toBeLessThanOrEqual(alto);
      expect(qr!.x + qr!.width).toBeLessThanOrEqual(ancho);
      const lienzo = await page.locator('.tv-lienzo').boundingBox();
      // Llena el lado que limita (16:9), sin deformarse.
      expect(Math.max(lienzo!.width / ancho, lienzo!.height / alto)).toBeGreaterThan(0.99);
      expect(lienzo!.width / lienzo!.height).toBeCloseTo(1280 / 720, 2);
    });
  });
}
