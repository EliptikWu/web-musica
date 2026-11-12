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

test('quitar el voto desde «Votaste» (RF-060)', async ({ page }) => {
  const errores = vigilarConsola(page);
  await abrir(page, '/b/demo/cola');
  const cola = page.getByRole('list', { name: 'Canciones en la cola' });
  const dtmf = cola.getByRole('listitem').filter({ hasText: 'DtMF' });
  await dtmf.getByRole('button', { name: 'Votaste por DtMF. Mover tu voto' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Mover tu voto de DtMF' });
  await dialogo.getByRole('button', { name: 'Quitar mi voto' }).click();
  await expect(page.getByText('Quitaste tu voto de DtMF')).toBeVisible();
  // «Votaste» vuelve a ser «+1», con el foco, y DtMF tiene un voto menos.
  await expect(dtmf.getByRole('button', { name: 'Votar por DtMF' })).toBeFocused();
  await expect(dtmf).toContainText('17');
  expect(errores).toEqual([]);
});

test('retirar tu pedido desde «Tuya», con confirmación (RF-061)', async ({ page }) => {
  const errores = vigilarConsola(page);
  await abrir(page, '/b/demo/cola');
  const cola = page.getByRole('list', { name: 'Canciones en la cola' });
  const monaco = cola.getByRole('listitem').filter({ hasText: 'Monaco' });
  await monaco.getByRole('button', { name: 'Tuya: Monaco. Retirar tu pedido' }).click();
  const dialogo = page.getByRole('dialog', { name: '¿Retirar Monaco de la cola?' });
  await expect(dialogo).toContainText('recuperas ese pedido');
  await sinViolaciones(page, 'retirar tu pedido');
  // Dejarla: no pasa nada.
  await dialogo.getByRole('button', { name: 'Dejarla en la cola' }).click();
  await expect(monaco).toBeVisible();
  await monaco.getByRole('button', { name: 'Tuya: Monaco. Retirar tu pedido' }).click();
  await page.getByRole('dialog', { name: '¿Retirar Monaco de la cola?' }).getByRole('button', { name: 'Retirar mi pedido' }).click();
  await expect(page.getByText('Retiraste Monaco')).toBeVisible();
  await expect(monaco).toHaveCount(0);
  // El foco no se pierde: pasa a la canción que seguía.
  await expect(cola.getByRole('listitem').filter({ hasText: 'La Bicicleta' }).getByRole('button')).toBeFocused();
  // «Tu canción» ya no habla de Monaco (en este momento de la noche suena la otra que pediste).
  await expect(page.getByRole('region', { name: 'Tu canción' })).not.toContainText('Monaco');
  await expect(page.getByRole('list').filter({ hasText: 'La retiraste tú' })).toBeVisible();
  await objetivosDe44(page);
  expect(errores).toEqual([]);
});
