import { expect, test } from '@playwright/test';
import { abrir, objetivosDe44, sinViolaciones, vigilarConsola } from './support';

// Iteración 6: extras del cliente (PB-50, PB-51). Teléfono de 360 px.
test.use({ viewport: { width: 360, height: 780 } });

test('mover el voto: desde «Votaste» a otra canción de la cola (PB-50, RF-014)', async ({ page }) => {
  const errores = vigilarConsola(page);
  await abrir(page, '/b/demo/cola');
  const cola = page.getByRole('list', { name: 'Canciones en la cola' });
  await page.getByRole('button', { name: 'Votar por La Bicicleta' }).click();
  const bici = cola.getByRole('listitem').filter({ hasText: 'La Bicicleta' });
  await bici.getByRole('button', { name: 'Votaste por La Bicicleta. Mover tu voto' }).click();

  const dialogo = page.getByRole('dialog', { name: 'Mover tu voto de La Bicicleta' });
  await expect(dialogo).toBeVisible();
  // No se ofrece la misma canción ni las ya votadas ni la propia.
  await expect(dialogo.getByRole('button', { name: 'Mover tu voto a La Bicicleta' })).toHaveCount(0);
  await sinViolaciones(page, 'mover el voto');
  await dialogo.getByRole('button', { name: 'Mover tu voto a Luna' }).click();

  await expect(page.getByText('Tu voto pasó a Luna')).toBeVisible();
  const luna = cola.getByRole('listitem').filter({ hasText: 'Luna' });
  // El foco queda en la nueva «Votaste» y La Bicicleta se puede votar otra vez.
  await expect(luna.getByRole('button', { name: 'Votaste por Luna. Mover tu voto' })).toBeFocused();
  await expect(bici.getByRole('button', { name: 'Votar por La Bicicleta' })).toBeVisible();
  await objetivosDe44(page);
  expect(errores).toEqual([]);
});

test('filtrar los resultados por género, duración y popularidad (PB-51, RF-055)', async ({ page }) => {
  await abrir(page, '/b/demo/buscar');
  await page.getByRole('searchbox', { name: 'Buscar en YouTube' }).fill('bad bunny');
  const resultados = page.getByRole('region', { name: 'Resultados en YouTube' });
  const filas = resultados.getByRole('list').first().getByRole('listitem');
  await expect(filas).toHaveCount(4);
  await resultados.getByRole('button', { name: 'Filtrar' }).click();
  await resultados.getByLabel('Duración').selectOption({ label: 'Más de 6 min' });
  await expect(filas).toHaveCount(2);
  await resultados.getByLabel('Género').selectOption({ label: 'Sin clasificar' });
  await expect(filas).toHaveCount(1);
  await expect(filas.first()).toContainText('BAILE INoLVIDABLE');
  await expect(resultados.getByRole('button', { name: 'Filtrar (2)' })).toBeVisible();
  await sinViolaciones(page, 'búsqueda con filtros');
  await objetivosDe44(page);
  await resultados.getByRole('button', { name: 'Quitar filtros' }).first().click();
  await expect(filas).toHaveCount(4);
});
