import { type Page, test } from '@playwright/test';
import path from 'node:path';
import { abrir } from './support';

/**
 * Las capturas del manual de uso (RNF-016): se rehacen con `npm run capturas -w @rockola/web`
 * cada vez que cambia una pantalla que el manual muestra, y quedan en `public/manual/`.
 * Simulación con el reloj quieto: la misma noche de los prototipos.
 */
test.use({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 1 });

const destino = (nombre: string) => path.join(import.meta.dirname, '..', 'public', 'manual', `${nombre}.jpg`);
const tomar = (page: Page, nombre: string) => page.screenshot({ path: destino(nombre), type: 'jpeg', quality: 72 });

test('capturas del manual', async ({ page }) => {
  test.setTimeout(120_000);
  await abrir(page, '/panel/entrar');
  await tomar(page, 'entrar');

  await page.getByLabel('Correo').fill('admin@rockola.demo');
  await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
  await page.getByRole('button', { name: 'Entrar' }).click();
  const franja = page.getByRole('region', { name: 'Estado del servicio' });
  await franja.waitFor();
  await page.waitForTimeout(600);
  await tomar(page, 'operacion');

  await franja.getByRole('button', { name: 'Pausar solicitudes' }).click();
  await page.getByText('En pausa, no se reciben canciones nuevas').waitFor();
  await tomar(page, 'pausa');
  await franja.getByRole('button', { name: 'Reanudar solicitudes' }).click();
  await page.getByText('Abierto, recibiendo canciones').waitFor();

  await page.getByRole('button', { name: 'Retirar TQG' }).first().click();
  await page.getByRole('dialog').getByRole('radio', { name: 'Duplicada' }).check();
  await tomar(page, 'retirar');
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();

  await page.getByRole('button', { name: 'Bloquear', exact: true }).click();
  await page.getByRole('dialog').getByLabel('Motivo').fill('No va con la noche de salsa');
  await tomar(page, 'bloquear');
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();

  for (const [ruta, nombre, esperar] of [
    ['/panel/horario', 'horario', 'Guardar horario'],
    ['/panel/listas/l-salsa', 'listas', 'Agregar canciones'],
    ['/panel/bloqueos', 'bloqueos', 'Mostrar'],
    ['/panel/usuarios', 'personal', 'Agregar a alguien'],
    ['/panel/establecimiento', 'bar-qr', 'Código QR de las mesas'],
    ['/panel/bitacora', 'bitacora', 'Quién'],
    ['/panel/eventos', 'eventos', 'Próximos'],
    ['/panel/analitica', 'tablero', 'En estas noches'],
  ] as const) {
    await page.goto(ruta);
    await page.getByText(esperar, { exact: false }).first().waitFor();
    await page.waitForTimeout(400);
    await tomar(page, nombre);
  }
});
