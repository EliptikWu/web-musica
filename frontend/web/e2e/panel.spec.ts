import { expect, type Page, test } from '@playwright/test';
import { abrir, objetivosDe44, sinViolaciones, vigilarConsola } from './support';

// Iteración 3: el panel operativo (PB-10, 11, 12, 25, 27, 29, 31–34, 38, 40).

async function entrarComo(page: Page, correo: string, clave = 'rockola-demo', escenario = 'momento-b') {
  await abrir(page, '/panel/entrar', escenario);
  await page.getByLabel('Correo').fill(correo);
  await page.getByLabel('Clave', { exact: true }).fill(clave);
  await page.getByRole('button', { name: 'Entrar' }).click();
}

const franja = (page: Page) => page.getByRole('region', { name: 'Estado del servicio' });
const registro = (page: Page) => page.getByRole('region', { name: 'Registro de acciones' });

test.describe('entrar', () => {
  test('clave equivocada: mensaje genérico; bien: a la operación (RF-021)', async ({ page }) => {
    const errores = vigilarConsola(page, { permitir: [401] });
    await entrarComo(page, 'admin@rockola.demo', 'otra-clave');
    await expect(page.getByRole('alert').filter({ hasText: 'Correo o clave incorrectos.' })).toBeVisible();
    await sinViolaciones(page, 'entrar con error');
    await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/panel$/);
    await expect(franja(page)).toContainText('Abierto, recibiendo canciones');
    expect(errores).toEqual([]);
  });

  test('cinco intentos fallidos bloquean la cuenta unos minutos', async ({ page }) => {
    await entrarComo(page, 'operador@rockola.demo', 'mal');
    for (let i = 0; i < 4; i++) {
      await page.getByRole('button', { name: 'Entrar' }).click();
      await page.waitForTimeout(150);
    }
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByRole('alert').filter({ hasText: 'Demasiados intentos' })).toBeVisible();
  });

  test('sin sesión, el panel manda a la puerta; con la sesión vencida, lo dice', async ({ page, context }) => {
    await page.goto('/panel');
    await expect(page).toHaveURL(/\/panel\/entrar$/);
    await context.addCookies([{ name: 'rockola_panel', value: 'vencida', url: 'http://localhost:3200' }]);
    await abrir(page, '/panel');
    await expect(page).toHaveURL(/motivo=caducada/);
    await expect(page.getByText('Tu sesión terminó. Entra de nuevo.')).toBeVisible();
  });

  test('primer ingreso: cambiar la clave antes de seguir (RF-023)', async ({ page }) => {
    await entrarComo(page, 'nuevo@rockola.demo', 'temporal-demo');
    await expect(page).toHaveURL(/\/panel\/cambiar-clave$/);
    await page.getByLabel('Clave actual (la temporal)').fill('temporal-demo');
    await page.getByLabel('Clave nueva', { exact: true }).fill('corta');
    await page.getByRole('button', { name: 'Guardar y entrar' }).click();
    await expect(page.getByText('Al menos 10 caracteres.').last()).toBeVisible();
    await page.getByLabel('Clave nueva', { exact: true }).fill('una-clave-segura');
    await page.getByLabel('Repite la clave nueva').fill('una-clave-segura');
    await page.getByRole('button', { name: 'Guardar y entrar' }).click();
    await expect(page).toHaveURL(/\/panel$/);
    await expect(franja(page)).toBeVisible();
  });
});

test.describe('Administrador', () => {
  test('la operación del prototipo y su accesibilidad', async ({ page }) => {
    await entrarComo(page, 'admin@rockola.demo');
    await expect(page.getByText('Joe Arroyo, pedida por el teléfono 7F3K hace 31 min')).toBeVisible();
    await expect(page.getByRole('table')).toContainText('Cali Pachanguero');
    await expect(registro(page)).toContainText('Saltaste «Gasolina», de Daddy Yankee, y la bloqueaste');
    await expect(page.getByRole('region', { name: 'Métricas de la noche' })).toContainText('162');
    await sinViolaciones(page, 'panel del Administrador');
  });

  test('pausar las solicitudes: la franja se apaga y el teléfono lo ve (PB-27)', async ({ page, context }) => {
    await entrarComo(page, 'admin@rockola.demo');
    const telefono = await context.newPage();
    await telefono.goto('/b/demo');
    await expect(telefono.getByRole('link', { name: 'Entrar y votar' })).toBeVisible();
    await franja(page).getByRole('button', { name: 'Pausar solicitudes' }).click();
    await expect(franja(page)).toContainText('En pausa, no se reciben canciones nuevas');
    await expect(telefono.getByRole('heading', { name: 'Los pedidos están en pausa' })).toBeVisible({ timeout: 3000 });
    await sinViolaciones(page, 'panel en pausa');
    await franja(page).getByRole('button', { name: 'Reanudar solicitudes' }).click();
    await expect(franja(page)).toContainText('Abierto, recibiendo canciones');
    await expect(registro(page)).toContainText('Reanudaste las solicitudes');
  });

  test('saltar la canción que suena (PB-32)', async ({ page }) => {
    await entrarComo(page, 'admin@rockola.demo');
    await page.getByRole('button', { name: 'Saltar' }).click();
    await expect(page.getByRole('heading', { level: 2, name: 'Cali Pachanguero' })).toBeVisible();
    await expect(registro(page)).toContainText('Saltaste «Rebelión», de Joe Arroyo');
  });

  test('bloquear una de la cola con motivo: sale de la cola y quien la pidió ve por qué (PB-33)', async ({ page, context }) => {
    await entrarComo(page, 'admin@rockola.demo');
    await page.getByRole('button', { name: 'Bloquear Monaco' }).first().click();
    const dialogo = page.getByRole('dialog', { name: 'Bloquear «Monaco»' });
    await expect(dialogo).toBeVisible();
    await sinViolaciones(page, 'diálogo bloquear');
    await dialogo.getByRole('button', { name: 'Bloquear' }).click();
    await expect(dialogo.getByText('Escribe el motivo.')).toBeVisible();
    await dialogo.getByLabel('Motivo').fill('Ya sonó dos veces');
    await dialogo.getByRole('radio', { name: 'Por horas' }).check();
    await dialogo.getByLabel('Horas', { exact: true }).fill('2');
    await dialogo.getByRole('button', { name: 'Bloquear' }).click();
    await expect(dialogo).toBeHidden();
    await expect(page.getByRole('table')).not.toContainText('Monaco');
    await expect(page.getByRole('region', { name: 'Canciones bloqueadas' })).toContainText('Monaco');
    // El teléfono que la pidió (el de esta simulación) ve el motivo.
    const telefono = await context.newPage();
    await telefono.goto('/b/demo/cola');
    await expect(telefono.getByRole('region', { name: 'Tus pedidos de esta noche' })).toContainText('Bloqueada por el bar: Ya sonó dos veces');
  });

  test('retirar una solicitud con motivo (PB-25)', async ({ page }) => {
    await entrarComo(page, 'admin@rockola.demo');
    await page.getByRole('button', { name: 'Retirar Tití Me Preguntó' }).first().click();
    const dialogo = page.getByRole('dialog', { name: 'Retirar «Tití Me Preguntó» de la cola' });
    await dialogo.getByRole('radio', { name: 'Duplicada' }).check();
    await dialogo.getByRole('button', { name: 'Retirar' }).click();
    await expect(page.getByRole('table')).not.toContainText('Tití Me Preguntó');
    await expect(registro(page)).toContainText('Retiraste «Tití Me Preguntó», de Bad Bunny de la cola: Duplicada');
  });

  test('mantenimiento: confirma, cierra la noche y vacía la cola (PB-31)', async ({ page }) => {
    await entrarComo(page, 'admin@rockola.demo');
    await franja(page).getByRole('button', { name: 'Modo mantenimiento' }).click();
    const dialogo = page.getByRole('dialog', { name: '¿Poner la rockola en mantenimiento?' });
    await expect(dialogo).toContainText('las 9 canciones de la cola se descartan');
    await dialogo.getByRole('button', { name: 'Poner en mantenimiento' }).click();
    await expect(franja(page)).toContainText('En mantenimiento');
    await expect(page.getByText('No hay solicitudes en la cola.')).toBeVisible();
    await franja(page).getByRole('button', { name: 'Reanudar solicitudes' }).click();
    await expect(franja(page)).toContainText('Abierto, recibiendo canciones');
  });

  test('cerrar la noche a mano dentro del horario no se reabre sola (PB-12)', async ({ page }) => {
    await entrarComo(page, 'admin@rockola.demo');
    await franja(page).getByRole('button', { name: 'Cerrar la noche' }).click();
    await page.getByRole('dialog', { name: '¿Cerrar la noche?' }).getByRole('button', { name: 'Cerrar la noche' }).click();
    await expect(page.getByText('No hay solicitudes en la cola.')).toBeVisible();
    await page.waitForTimeout(2000);
    await expect(registro(page)).toContainText('Cerraste la noche');
  });
});

test('cerrar sesión lleva a la puerta sin decir que caducó; el panel ya no abre', async ({ page }) => {
  await entrarComo(page, 'admin@rockola.demo');
  await expect(franja(page)).toBeVisible();
  await page.getByRole('button', { name: 'Salir' }).click();
  await expect(page).toHaveURL(/\/panel\/entrar$/);
  await expect(page.getByText('Tu sesión terminó')).toHaveCount(0);
  await page.goto('/panel');
  await expect(page).toHaveURL(/\/panel\/entrar/);
});

test('si la sesión vence a mitad de la noche, el panel lleva a la puerta', async ({ page }) => {
  await entrarComo(page, 'admin@rockola.demo');
  await expect(franja(page)).toBeVisible();
  // El servidor olvida la sesión (como al vencer): la siguiente acción lo nota.
  await page.evaluate(() => {
    const estado = JSON.parse(localStorage.getItem('rockola.sim.v1') ?? '{}');
    estado.sesiones_panel = {};
    localStorage.setItem('rockola.sim.v1', JSON.stringify(estado));
  });
  await page.getByRole('button', { name: 'Saltar' }).click();
  await expect(page).toHaveURL(/motivo=caducada/);
});

test('el diálogo de bloqueo empieza vacío cada vez', async ({ page }) => {
  await entrarComo(page, 'admin@rockola.demo');
  await page.getByRole('button', { name: 'Bloquear Luna' }).first().click();
  let dialogo = page.getByRole('dialog', { name: 'Bloquear «Luna»' });
  await dialogo.getByLabel('Motivo').fill('Motivo de Luna');
  await dialogo.getByRole('button', { name: 'Bloquear' }).click();
  await expect(dialogo).toBeHidden();
  await page.getByRole('button', { name: 'Bloquear TQG' }).first().click();
  dialogo = page.getByRole('dialog', { name: 'Bloquear «TQG»' });
  await expect(dialogo.getByLabel('Motivo')).toHaveValue('');
});

test.describe('Operador (D9)', () => {
  test('ve la noche, pero no el mantenimiento, el desbloqueo ni la duración del bloqueo', async ({ page }) => {
    await entrarComo(page, 'operador@rockola.demo');
    await expect(page.getByText('Operador de prueba')).toBeVisible();
    await expect(franja(page).getByRole('button', { name: 'Pausar solicitudes' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Modo mantenimiento' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /^Desbloquear/ })).toHaveCount(0);
    await expect(registro(page)).toContainText('Admin de prueba bloqueó «La Bachata», de Manuel Turizo');
    await page.getByRole('button', { name: 'Bloquear', exact: true }).click();
    await expect(page.getByRole('dialog').getByText('Tu rol bloquea hasta el fin de la noche.')).toBeVisible();
    await sinViolaciones(page, 'panel del Operador');
  });

  test('el servidor rechaza lo que su rol no permite y lo deja en la bitácora (RF-022)', async ({ page, context }) => {
    await entrarComo(page, 'operador@rockola.demo');
    await expect(franja(page)).toBeVisible();
    const respuestas = await page.evaluate(async () => {
      const mantenimiento = await fetch('/api/panel/servicio/mantenimiento', { method: 'POST' });
      const forzar = await fetch('/api/panel/servicio', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ modo_qr: 'forzado_activo' }) });
      return [mantenimiento.status, forzar.status];
    });
    expect(respuestas).toEqual([403, 403]);
    // El Administrador lo ve en el registro.
    await context.clearCookies();
    const admin = await context.newPage();
    await admin.goto('/panel/entrar');
    await admin.getByLabel('Correo').fill('admin@rockola.demo');
    await admin.getByLabel('Clave', { exact: true }).fill('rockola-demo');
    await admin.getByRole('button', { name: 'Entrar' }).click();
    await expect(registro(admin)).toContainText('Operador de prueba intentó algo que su rol no permite');
  });
});

test.describe('en el teléfono del personal (360 px)', () => {
  test.use({ viewport: { width: 360, height: 780 } });

  test('la cola pasa a tarjetas y todo se toca con el pulgar', async ({ page }) => {
    await entrarComo(page, 'operador@rockola.demo');
    await expect(page.getByRole('table')).toBeHidden();
    await expect(page.getByRole('list', { name: 'Cola' })).toContainText('Cali Pachanguero');
    await sinViolaciones(page, 'panel a 360 px');
    await objetivosDe44(page, 'main button, main a, header button, header a');
  });
});
