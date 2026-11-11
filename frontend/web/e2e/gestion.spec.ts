import { expect, type Page, test } from '@playwright/test';
import { abrir, sinViolaciones } from './support';

// Iteración 4: la gestión (PB-01, 02, 23, 24, 26, 28, 34–37, 39–42).

async function entrar(page: Page, correo = 'admin@rockola.demo') {
  await abrir(page, '/panel/entrar');
  await page.getByLabel('Correo').fill(correo);
  await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.getByRole('region', { name: 'Estado del servicio' }).waitFor();
}

test('horario: avisa los tramos que se pisan y guarda el bueno (PB-28)', async ({ page }) => {
  await entrar(page);
  await page.getByRole('link', { name: 'Horario' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Horario del QR' })).toBeVisible();
  await sinViolaciones(page, 'horario');
  // El viernes de 1:00 a. m. pisa el jueves, que cierra a las 2:00 a. m.
  const viernes = page.getByRole('listitem').filter({ hasText: 'Viernes' });
  await viernes.getByLabel('Abre').fill('01:00');
  await viernes.getByLabel('Cierra').fill('03:00');
  await expect(page.getByText('Se pisa con el tramo de otro día.').first()).toBeVisible();
  await viernes.getByLabel('Abre').fill('20:00');
  await viernes.getByLabel('Cierra').fill('02:00');
  await page.getByRole('listitem').filter({ hasText: 'Domingo' }).getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Guardar horario' }).click();
  await expect(page.getByText('Horario guardado.')).toBeVisible();
  await expect(page.getByText(/Jueves a domingo/)).toBeVisible();
});

test('listas: crear, agregar, ordenar, activar y no poder borrar la activa (PB-35 a PB-39)', async ({ page }) => {
  await entrar(page);
  await page.goto('/panel/listas');
  await page.getByLabel('Nombre').fill('Boleros de la casa');
  await page.getByRole('button', { name: 'Crear lista' }).click();
  await expect(page.getByText('Lista «Boleros de la casa» creada.')).toBeVisible();
  await page.getByRole('link', { name: 'Editar Boleros de la casa' }).click();
  await page.getByRole('searchbox', { name: 'Buscar en YouTube para agregar' }).fill('celia');
  await page.getByRole('button', { name: 'Agregar La vida es un carnaval' }).click();
  await page.getByRole('button', { name: 'Agregar Quimbara' }).click();
  const canciones = page.getByRole('region', { name: 'Canciones, en el orden en que suenan' }).getByRole('listitem');
  await expect(canciones).toHaveCount(2);
  // Con teclado: al llegar arriba, «Subir» se apaga y el foco pasa a «Bajar» de la misma canción.
  await page.getByRole('button', { name: 'Subir Quimbara' }).focus();
  await page.keyboard.press('Enter');
  await expect(canciones.first()).toContainText('Quimbara');
  await expect(page.getByRole('button', { name: 'Bajar Quimbara' })).toBeFocused();
  await expect(page.getByText('«Quimbara» quedó en el puesto 1.')).toBeAttached();
  await sinViolaciones(page, 'editor de lista');
  // Al quitar, el foco pasa a la canción que queda en su lugar.
  await page.getByRole('button', { name: 'Quitar Quimbara' }).focus();
  await page.keyboard.press('Enter');
  await expect(canciones).toHaveCount(1);
  await expect(page.getByRole('button', { name: 'Quitar La vida es un carnaval' })).toBeFocused();
  await page.getByRole('link', { name: 'Todas las listas' }).click();
  await page.getByRole('button', { name: 'Activar Boleros de la casa' }).click();
  await page.getByRole('dialog').getByRole('radio', { name: 'Al azar, sin repetir' }).check();
  await page.getByRole('dialog').getByRole('button', { name: 'Activar' }).click();
  await expect(page.getByText('«Boleros de la casa» es la lista activa, al azar.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Eliminar Boleros de la casa' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Eliminar Salsa y tropical de la casa' })).toBeVisible();
});

test('bloqueos: desbloquear y ver los vencidos (PB-34)', async ({ page }) => {
  await entrar(page);
  await page.goto('/panel/bloqueos');
  await sinViolaciones(page, 'bloqueos');
  await page.getByRole('button', { name: 'Desbloquear Despechá' }).click();
  await expect(page.getByText('«Despechá» se puede pedir otra vez.')).toBeVisible();
  await page.getByRole('radio', { name: 'Vencidos' }).check();
  await expect(page.getByRole('region', { name: 'Bloqueos vencidos' })).toContainText('Despechá');
});

test('personal: crear con clave temporal que se ve una vez; el último Administrador se protege (PB-23, PB-24)', async ({ page }) => {
  await entrar(page);
  await page.goto('/panel/usuarios');
  await page.getByRole('button', { name: 'Agregar a alguien' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Agregar a alguien del personal' });
  await dialogo.getByLabel('Nombre').fill('Barra de la terraza');
  await dialogo.getByLabel('Correo').fill('terraza@rockola.demo');
  await dialogo.getByRole('button', { name: 'Agregar' }).click();
  const clave = page.getByRole('dialog', { name: 'Clave temporal de Barra de la terraza' });
  await expect(clave).toContainText('Solo se muestra ahora');
  await sinViolaciones(page, 'clave temporal');
  await clave.getByRole('button', { name: 'Listo' }).click();
  await expect(page.getByRole('region', { name: 'Personal' })).toContainText('Barra de la terraza');
  // El único Administrador no se puede pasar a Operador.
  await page.getByRole('button', { name: 'Editar a Admin de prueba' }).click();
  await page.getByRole('dialog').getByRole('radio', { name: 'Operador' }).check();
  await page.getByRole('dialog').getByRole('button', { name: 'Guardar' }).click();
  await expect(page.getByRole('alert').filter({ hasText: 'No se puede' })).toBeVisible();
});

test('bar y QR: regenerar deja inservible el QR anterior (PB-02, RF-035)', async ({ page, context }) => {
  await entrar(page);
  await page.goto('/panel/establecimiento');
  await expect(page.getByRole('img', { name: 'Código QR del bar' })).toBeVisible();
  await sinViolaciones(page, 'bar y QR');
  await page.getByLabel('Pedidos por teléfono').fill('2');
  await page.getByRole('button', { name: 'Guardar datos' }).click();
  await expect(page.getByText('Datos del bar guardados.')).toBeVisible();
  await expect(page.getByLabel('Pedidos por teléfono')).toHaveValue('2');
  await page.getByRole('button', { name: 'Regenerar' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Regenerar QR' }).click();
  await expect(page.getByText('QR nuevo listo.')).toBeVisible();
  const telefono = await context.newPage();
  await telefono.goto('/q/demo-qr-1');
  await expect(telefono.getByRole('heading', { name: 'Este QR ya no está en uso' })).toBeVisible();
  await telefono.goto('/q/demo-qr-2');
  await expect(telefono).toHaveURL(/\/b\/demo$/);
});

test('bitácora: filtra por tipo de acción (PB-40)', async ({ page }) => {
  await entrar(page);
  await page.goto('/panel/bitacora');
  await expect(page.getByRole('region', { name: 'Acciones' })).toContainText('Bloqueaste «La Bachata»');
  await page.getByLabel('Qué').selectOption('abrir_sesion');
  await expect(page.getByRole('region', { name: 'Acciones' })).not.toContainText('La Bachata');
  await expect(page.getByRole('region', { name: 'Acciones' })).toContainText('Se abrieron las solicitudes con el horario del QR');
  await sinViolaciones(page, 'bitácora');
});

test('eventos: agregar uno próximo (PB-41)', async ({ page }) => {
  await entrar(page);
  await page.goto('/panel/eventos');
  await page.getByRole('button', { name: 'Agregar evento' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Agregar evento' });
  await dialogo.getByLabel('Nombre').fill('Noche de boleros');
  await dialogo.getByLabel('Empieza').fill('2026-11-05T20:00');
  await dialogo.getByLabel('Termina').fill('2026-11-06T01:00');
  await dialogo.getByRole('button', { name: 'Guardar' }).click();
  await expect(page.getByRole('region', { name: 'Próximos' })).toContainText('Noche de boleros');
  await sinViolaciones(page, 'eventos');
});

test('manual: el Operador ve su parte y nada de la gestión (PB-42, RNF-016)', async ({ page }) => {
  await entrar(page, 'operador@rockola.demo');
  await expect(page.getByRole('link', { name: 'Personal' })).toHaveCount(0);
  await page.getByRole('link', { name: 'Manual de uso' }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Bloquear una canción' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Dar acceso al personal' })).toHaveCount(0);
  await sinViolaciones(page, 'manual del Operador');
  // Por URL directa: ve que no es su sección y la pantalla no pide nada al servidor.
  const pedidas: string[] = [];
  page.on('request', (r) => {
    if (/\/api\/panel\/(usuarios|listas|horario|eventos|establecimiento)/.test(r.url())) pedidas.push(r.url());
  });
  for (const ruta of ['/panel/usuarios', '/panel/listas', '/panel/establecimiento/qr/imprimir']) {
    await page.goto(ruta);
    await expect(page.getByRole('heading', { name: 'Esta sección es del Administrador' })).toBeVisible();
  }
  await page.waitForTimeout(1500);
  expect(pedidas).toEqual([]);
});
