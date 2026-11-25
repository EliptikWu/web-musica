import { expect, type Page, test } from '@playwright/test';
import { abrir, objetivosDe44, sinViolaciones, vigilarConsola } from './support';

// RF-068: corregir el género de una canción desde el panel.

async function entrar(page: Page, correo = 'admin@rockola.demo') {
  await abrir(page, '/panel/entrar');
  await page.getByLabel('Correo').fill(correo);
  await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.getByRole('region', { name: 'Estado del servicio' }).waitFor();
}

test('canciones: la que no tiene género se corrige y queda puesta a mano', async ({ page }) => {
  const errores = vigilarConsola(page);
  await entrar(page);
  // El menú va por grupos (punto 3): Canciones está en Música, junto al tablero.
  await page.getByRole('link', { name: 'Música' }).click();
  await page.getByRole('group', { name: 'Secciones de Música' }).getByRole('link', { name: 'Canciones' }).click();
  await expect(page.getByRole('heading', { name: 'Canciones', level: 1 })).toBeVisible();

  const lista = page.getByRole('region', { name: 'Canciones sin género' }).getByRole('listitem');
  await expect(lista.first()).toBeVisible();
  const cuantas = await lista.count();
  const fila = lista.first();
  const titulo = (await fila.locator('.tarjeta__titulo').textContent())!.trim();
  await sinViolaciones(page, 'canciones sin género');

  await expect(fila.getByRole('button', { name: 'Guardar género' })).toBeDisabled();
  await fila.getByRole('combobox').selectOption('Salsa');
  await fila.getByRole('button', { name: 'Guardar género' }).click();
  await expect(page.getByRole('status').filter({ hasText: `«${titulo}» quedó en Salsa.` })).toBeVisible();
  // Sin recargar: la lista de «Sin género» ya no la tiene.
  await expect(lista).toHaveCount(cuantas - 1);
  // El foco no se pierde: pasa al selector de la canción que quedó en su lugar.
  await expect(page.locator(':focus')).toHaveRole('combobox');

  await page.getByRole('radio', { name: 'Todas' }).check();
  await page.getByLabel('Buscar por título o artista').fill(titulo);
  const corregida = page.getByRole('region', { name: 'Todas las canciones' }).getByRole('listitem').filter({ hasText: titulo }).first();
  await expect(corregida).toContainText('Salsa');
  await expect(corregida).toContainText('Puesto a mano.');
  await sinViolaciones(page, 'canciones, todas');
  await objetivosDe44(page, 'main button, main select');

  await page.goto('/panel/bitacora');
  await expect(page.getByText(`Cambiaste el género de «${titulo}»`).first()).toBeVisible();
  expect(errores).toEqual([]);
});

test('canciones: el operador no la ve en el menú ni puede abrirla', async ({ page }) => {
  await entrar(page, 'operador@rockola.demo');
  await expect(page.getByRole('link', { name: 'Canciones' })).toHaveCount(0);
  await page.goto('/panel/canciones');
  await expect(page.getByRole('link', { name: /Volver/ })).toBeVisible();
});
