import { expect, type Page, test } from '@playwright/test';
import { abrir, sinViolaciones, vigilarConsola } from './support';

// Iteración 16: el cubo de KPI (RF-063). Datos de ejemplo de la simulación.

async function entrar(page: Page) {
  await abrir(page, '/panel/entrar');
  await page.getByLabel('Correo').fill('admin@rockola.demo');
  await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.getByRole('region', { name: 'Estado del servicio' }).waitFor();
}

test('cubo: KPI, slice, dice sin sumar únicos, drill-down, roll-up y pivot', async ({ page }) => {
  const errores = vigilarConsola(page);
  await entrar(page);
  await page.goto('/panel/analitica/cubo');
  await expect(page.getByText('Datos en vivo')).toBeVisible();
  // El histórico es el del pipeline: tres géneros y «Otros», cuatro franjas, cuatro días, con sus «Todos».
  await page.getByRole('radio', { name: 'Histórico' }).check();
  await expect(page.locator('.cubito')).toHaveCount(125);
  // Tocar un cubito elige su celda y la resalta, sin recortar el cubo.
  // Con la vista de frente, la cara delantera del primer cubito no tiene nada encima.
  await page.getByRole('button', { name: 'Frente', exact: true }).click();
  await page.waitForTimeout(400);
  const cara = (await page.locator('.cubito:not([data-total]):not([data-vacio]) > i').first().boundingBox())!;
  await page.mouse.click(cara.x + cara.width / 2, cara.y + cara.height / 2);
  await expect(page.getByRole('heading', { name: 'Celda' })).toBeVisible();
  await expect(page.locator('.cubito')).toHaveCount(125);
  await page.getByRole('button', { name: 'Restablecer filtros' }).click();
  await expect(page.getByText('Datos de ejemplo').first()).toBeVisible();

  const kpis = page.getByRole('group', { name: 'KPI de lo elegido' });
  const unicos = kpis.getByRole('button', { name: /Usuarios únicos/ });
  await expect(unicos).toContainText(/\d/);

  await page.getByRole('button', { name: 'Urbano', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Slice' })).toBeVisible();

  await page.getByRole('button', { name: 'Salsa', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Dice' })).toBeVisible();
  await expect(unicos).toContainText('No aplica');

  await page.getByRole('button', { name: 'Restablecer filtros' }).click();
  await page.getByRole('button', { name: 'Drill-down' }).click();
  await expect(page.getByRole('heading', { name: 'Slice' })).toBeVisible();
  await page.getByRole('button', { name: 'Roll-up' }).click();
  await expect(page.getByRole('heading', { name: 'Cubo completo' })).toBeVisible();

  await expect(page.getByRole('heading', { name: 'Solicitudes por género y franja' })).toBeVisible();
  await page.getByRole('button', { name: 'Pivot' }).click();
  await expect(page.getByRole('heading', { name: 'Solicitudes por franja y género' })).toBeVisible();

  await sinViolaciones(page, 'cubo de KPI');
  expect(errores).toEqual([]);
});

/** El número de un KPI del panel, sin separadores de miles. */
async function cifra(page: Page, nombre: RegExp): Promise<number> {
  const texto = await page.getByRole('group', { name: 'KPI de lo elegido' }).getByRole('button', { name: nombre }).locator('b').textContent();
  return Number((texto ?? '').replace(/\D/g, ''));
}

test('cubo en vivo: un voto en otra pestaña lo mueve sin recargar, y una ráfaga no repite peticiones (RF-067)', async ({ page, context }) => {
  const errores = vigilarConsola(page);
  await entrar(page);
  await page.goto('/panel/analitica/cubo');
  await expect(page.getByText('Datos en vivo')).toBeVisible();
  // De entrada, cinco géneros más «Todos» en la vertical, aunque haya más.
  await expect(page.locator('.cubo-rotulo--g')).toHaveCount(6);
  const antes = await cifra(page, /^Votos/);

  let pedidas = 0;
  page.on('request', (r) => {
    if (r.url().includes('/api/panel/analitica/cubo')) pedidas++;
  });

  const telefono = await context.newPage();
  await telefono.goto('/muestras');
  await telefono.waitForFunction(() => Boolean(navigator.serviceWorker?.controller));
  const votar = (dispositivo: string) =>
    telefono.evaluate(async (d) => {
      const r = await fetch('/api/bares/demo/solicitudes/s-bici/votos', { method: 'POST', headers: { 'X-Rockola-Dispositivo': d, 'Idempotency-Key': crypto.randomUUID() } });
      return r.status;
    }, dispositivo);

  expect(await votar('telefono-cubo-0')).toBe(201);
  // RNF-009: en menos de 3 s, sin recargar.
  await expect.poll(() => cifra(page, /^Votos/), { timeout: 3000 }).toBe(antes + 1);

  await new Promise((r) => setTimeout(r, 2000));
  pedidas = 0;
  for (let i = 1; i <= 5; i++) expect(await votar(`telefono-cubo-${i}`)).toBe(201);
  await expect.poll(() => cifra(page, /^Votos/), { timeout: 5000 }).toBe(antes + 6);
  await new Promise((r) => setTimeout(r, 2000));
  expect(pedidas, 'peticiones del cubo por cinco votos seguidos').toBeLessThanOrEqual(3);

  await page.getByLabel(/Ver los \d+ géneros en el cubo/).check();
  await expect(page.locator('.cubo-rotulo--g')).not.toHaveCount(6);
  await sinViolaciones(page, 'cubo en vivo');
  expect(errores).toEqual([]);
});
