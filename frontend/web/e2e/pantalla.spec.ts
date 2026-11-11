import { expect, type Page, test } from '@playwright/test';
import { abrir, abrirTvVinculada, sinViolaciones, vigilarConsola, vincularTv } from './support';

// Iteración 2: la pantalla del bar (PB-17 a PB-20, PB-38). Lienzo de 1280 × 720 ampliado.

async function encender(page: Page) {
  await page.getByRole('button', { name: 'Encender' }).click();
}

test.describe('a 1280 × 720', () => {
  test.use({ viewport: { width: 1280, height: 720 } });

  test('lo que suena, lo que viene con su subida y el QR (PB-17, RF-016)', async ({ page }) => {
    const errores = vigilarConsola(page);
    await abrirTvVinculada(page);
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
    await abrirTvVinculada(page);
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
    await abrirTvVinculada(page);
    await encender(page);
    const telefono = await context.newPage();
    await telefono.setViewportSize({ width: 360, height: 780 });
    await telefono.goto('/b/demo/cola');
    await telefono.getByRole('button', { name: 'Votar por La Bicicleta' }).click();
    const viene = page.getByRole('region', { name: 'Lo que viene' }).getByRole('listitem');
    await expect(viene.nth(2)).toContainText('La Bicicleta', { timeout: 3000 });
    await expect(viene.nth(2)).toContainText('16');
  });

  test('sin vincular: muestra el código, no reporta, y al vincularla en el panel pasa a la rockola en 3 s (RF-058)', async ({ page, context }) => {
    const errores = vigilarConsola(page, { permitir: [401] });
    await abrir(page, '/b/demo/pantalla');
    await expect(page.getByRole('heading', { name: 'Vincula esta pantalla' })).toBeVisible();
    await expect(page.getByText('Sin vincular')).toBeVisible();
    await expect(page.locator('.codigo-tv .tecla')).toHaveCount(6);
    await expect(page.getByRole('button', { name: 'Encender' })).toHaveCount(0);
    await sinViolaciones(page, 'TV sin vincular');
    // Sin vincular, un reporte no mueve la cola.
    const estado = await page.evaluate(async () => {
      const ahora = await fetch('/api/bares/demo/ahora').then((r) => r.json());
      const r = await fetch(`/api/bares/demo/pantalla/reproducciones/${ahora.reproduccion.id}/fin`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Idempotency-Key': crypto.randomUUID() },
        body: JSON.stringify({ posicion_seg: 358, codigo_youtube: null }),
      });
      return r.status;
    });
    expect(estado).toBe(401);

    // El Administrador escribe el código en el panel, desde otra pestaña.
    const codigo = (await page.locator('.codigo-tv .solo-lector').textContent())!.replace(/\s/g, '');
    const panel = await context.newPage();
    await panel.setViewportSize({ width: 1440, height: 1000 });
    await panel.goto('/panel/entrar');
    await panel.getByLabel('Correo').fill('admin@rockola.demo');
    await panel.getByLabel('Clave', { exact: true }).fill('rockola-demo');
    await panel.getByRole('button', { name: 'Entrar' }).click();
    await panel.waitForURL(/\/panel$/);
    await panel.goto('/panel/establecimiento');
    await expect(panel.getByText('Ninguna pantalla está vinculada')).toBeVisible();
    // Como lo escribiría alguien: en minúsculas y con un guion en la mitad.
    await panel.getByLabel('Código que muestra la TV').fill(`${codigo.slice(0, 3)}-${codigo.slice(3)}`.toLowerCase());
    await expect(panel.getByLabel('Código que muestra la TV')).toHaveValue(codigo);
    await panel.getByLabel('Nombre de la pantalla').fill('TV de la barra');
    await panel.getByRole('button', { name: 'Vincular pantalla' }).click();
    await expect(panel.getByText('«TV de la barra» quedó vinculada', { exact: false })).toBeVisible();
    await expect(panel.getByRole('listitem').filter({ hasText: 'TV de la barra' })).toContainText('Todavía no ha puesto música');
    await expect(page.getByRole('button', { name: 'Encender' })).toBeVisible({ timeout: 3000 });
    await sinViolaciones(panel, 'Bar y QR con una pantalla');

    // Desvincularla desde el panel: la fila desaparece y el foco no se pierde.
    await panel.getByRole('button', { name: 'Desvincular «TV de la barra»' }).click();
    await panel.getByRole('dialog').getByRole('button', { name: 'Desvincular pantalla' }).click();
    await expect(panel.getByText('«TV de la barra» se desvinculó.')).toBeVisible();
    await expect(panel.getByRole('heading', { name: 'Pantallas del bar' })).toBeFocused();
    expect(errores).toEqual([]);
  });

  test('desvincularla: en 10 s vuelve al código y avisa por qué (RF-058)', async ({ page }) => {
    await abrir(page, '/b/demo/pantalla');
    await vincularTv(page, 'TV de la terraza');
    await expect(page.getByRole('button', { name: 'Encender' })).toBeVisible({ timeout: 5000 });
    // Alguien la desvincula desde el panel (la sesión del Administrador quedó en esta página).
    await page.evaluate(async () => {
      const { pantallas } = await fetch('/api/panel/pantallas').then((r) => r.json());
      await fetch(`/api/panel/pantallas/${pantallas[0].id}`, { method: 'DELETE' });
    });
    await expect(page.getByText('Esta pantalla se desvinculó desde el panel', { exact: false })).toBeVisible({ timeout: 12_000 });
    await expect(page.locator('.codigo-tv .tecla')).toHaveCount(6);
  });

  test('fuera de horario: la luz se apaga, el horario y la lista de la casa sonando', async ({ page }) => {
    await abrirTvVinculada(page, 'fuera-de-horario');
    await encender(page);
    await expect(page.getByText('Pedidos cerrados')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Por ahora no recibimos canciones' })).toBeVisible();
    await expect(page.getByText(/Los pedidos abren hoy a las 8:00/)).toBeVisible();
    await expect(page.getByText('Suena ahora, de la lista de la casa')).toBeVisible();
    await sinViolaciones(page, 'TV fuera de horario');
  });

  test('cola vacía: invita a escanear y pedir la primera', async ({ page }) => {
    await abrirTvVinculada(page, 'cola-vacia');
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
      await abrirTvVinculada(page);
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
