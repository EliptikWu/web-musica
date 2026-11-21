import { expect, type Page, test } from '@playwright/test';
import { abrir, sinViolaciones, vigilarConsola } from './support';

// Iteración 15: el cubo de KPI (RF-063). Datos de ejemplo de la simulación.

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
  await expect(page.locator('.cubito')).toHaveCount(125);
  await expect(page.getByText('Datos de ejemplo').first()).toBeVisible();

  const kpis = page.getByRole('group', { name: 'KPI de lo elegido' });
  const unicos = kpis.getByRole('button', { name: /Usuarios únicos/ });
  await expect(unicos).toContainText(/\d/);

  await page.getByRole('button', { name: 'Urbano', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Slice' })).toBeVisible();

  await page.getByRole('button', { name: 'Salsa', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Dice' })).toBeVisible();
  await expect(unicos).toContainText('No aplica');

  await page.getByRole('button', { name: 'Limpiar' }).click();
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
