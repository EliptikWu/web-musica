import { expect, type Page, test } from '@playwright/test';
import { abrir, objetivosDe44, sinViolaciones, vigilarConsola } from './support';

// Iteración 5: el tablero analítico (PB-45 a PB-49). Datos de ejemplo de la simulación.

async function entrar(page: Page, correo = 'admin@rockola.demo') {
  await abrir(page, '/panel/entrar');
  await page.getByLabel('Correo').fill(correo);
  await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.getByRole('region', { name: 'Estado del servicio' }).waitFor();
}

test('resumen: cifras rotuladas como ejemplo y lo que sonó en una noche (PB-45)', async ({ page }) => {
  const errores = vigilarConsola(page);
  await entrar(page);
  // El enlace de las pestañas (en la operación también está «Ver el tablero analítico»).
  await page.getByRole('navigation', { name: 'Secciones del panel' }).getByRole('link', { name: 'Tablero analítico' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Tablero analítico' })).toBeVisible();
  await expect(page.getByText('Datos de ejemplo').first()).toBeVisible();
  await expect(page.getByText('20 noches, del 10 de septiembre al 21 de octubre.')).toBeVisible();
  const cifras = page.getByRole('region', { name: 'En estas noches' });
  await expect(cifras.getByText('Solicitudes')).toBeVisible();
  await sinViolaciones(page, 'resumen del tablero');
  await objetivosDe44(page);
  await page.getByLabel('Noches', { exact: true }).selectOption({ label: 'Sábado, 17 de octubre' });
  await expect(page).toHaveURL(/noche=n-2026-10-17/);
  const historial = page.getByRole('region', { name: 'Lo que sonó, en orden' });
  await expect(historial.getByRole('row')).toHaveCount(13);
  await historial.getByRole('button', { name: /^Ver las \d+$/ }).click();
  await expect(historial.getByRole('row')).not.toHaveCount(13);
  await sinViolaciones(page, 'resumen de una noche');
  expect(errores).toEqual([]);
});

test('participación: géneros por votos, franjas con teclado y filtro por día (PB-47)', async ({ page }) => {
  await entrar(page);
  await page.goto('/panel/analitica/participacion');
  const generos = page.getByRole('region', { name: 'Géneros más pedidos' });
  await expect(generos.getByRole('img', { name: /^Géneros por solicitudes: Urbano/ })).toBeVisible();
  await generos.getByRole('radio', { name: 'Votos' }).check();
  await expect(generos.getByRole('img', { name: /^Géneros por votos:/ })).toBeVisible();
  await generos.getByText('Ver como tabla').click();
  await expect(generos.getByRole('table')).toContainText('Sin clasificar');
  // Una columna enfocada con el teclado muestra su valor.
  const franjas = page.getByRole('region', { name: 'Franjas horarias' });
  await franjas.getByRole('img', { name: /^10 p\. m\.: / }).focus();
  await expect(franjas.locator('.grafico-tip')).toBeVisible();
  await sinViolaciones(page, 'participación');
  await page.getByLabel('Día de la semana').selectOption({ label: 'Viernes' });
  await expect(page.getByText(/^6 noches, del 11 de septiembre al 16 de octubre\.$/)).toBeVisible();
});

test('diagnóstico: umbrales visibles y configurables; el cambio queda en la bitácora (PB-46)', async ({ page }) => {
  await entrar(page);
  await page.goto('/panel/analitica/diagnostico');
  await expect(page.getByRole('region', { name: 'Muy votadas que no sonaron' }).getByRole('row').nth(1)).toBeVisible();
  await expect(page.getByText(/Por encima del umbral: sáb 10 oct \(\d+ %\)/)).toBeVisible();
  await expect(page.getByText(/por debajo del mínimo: .*jueves, 1 de octubre/)).toBeVisible();
  await sinViolaciones(page, 'diagnóstico');
  await page.getByLabel('Teléfonos mínimos').fill('30');
  await page.getByRole('button', { name: 'Guardar umbrales' }).click();
  await expect(page.getByText('Umbrales guardados. El diagnóstico ya los usa.')).toBeVisible();
  await expect(page.getByText(/noches quedaron por debajo del mínimo/)).toBeVisible();
  await page.goto('/panel/bitacora');
  await expect(page.getByRole('region', { name: 'Acciones' })).toContainText('Cambiaste los umbrales del tablero');
});

test('comparar: dos o más noches, o agrupadas por día (PB-48)', async ({ page }) => {
  await entrar(page);
  await page.goto('/panel/analitica/comparar');
  await expect(page.getByRole('region', { name: 'Solicitudes' }).getByRole('img')).toHaveAccessibleName(/sáb 17 oct.*dom 18 oct.*mié 21 oct/);
  await sinViolaciones(page, 'comparar noches');
  await page.getByRole('checkbox', { name: /Domingo, 18 de octubre/ }).uncheck();
  await page.getByRole('checkbox', { name: /Miércoles, 21 de octubre/ }).uncheck();
  await expect(page.getByText('Elige al menos dos noches para comparar.')).toBeVisible();
  await page.getByRole('radio', { name: 'Días de la semana' }).check();
  await expect(page.getByText('Viernes (6 noches)')).toBeVisible();
  await sinViolaciones(page, 'comparar por día');
});

test('recomendaciones: dicen en qué se basan y llevan a donde se decide (PB-49)', async ({ page }) => {
  await entrar(page);
  await page.goto('/panel/analitica/recomendaciones');
  await expect(page.getByText('Nada se aplica solo: cada sugerencia te lleva a la sección donde decides.')).toBeVisible();
  await expect(page.getByText(/^En qué se basa: /).first()).toBeVisible();
  await sinViolaciones(page, 'recomendaciones');
  await page.getByRole('link', { name: 'Ir a Bloqueos' }).click();
  await expect(page).toHaveURL(/\/panel\/bloqueos$/);
});

test('el Operador no ve el tablero ni lo pide al servidor (RF-049)', async ({ page }) => {
  await entrar(page, 'operador@rockola.demo');
  await expect(page.getByRole('link', { name: 'Tablero analítico' })).toHaveCount(0);
  const pedidas: string[] = [];
  page.on('request', (r) => {
    if (r.url().includes('/api/panel/analitica')) pedidas.push(r.url());
  });
  await page.goto('/panel/analitica/diagnostico');
  await expect(page.getByRole('heading', { name: 'Esta sección es del Administrador' })).toBeVisible();
  await page.waitForTimeout(1000);
  expect(pedidas).toEqual([]);
});
