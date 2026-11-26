import { expect, type Page, test } from '@playwright/test';
import { abrir, abrirTvVinculada } from './support';

// Rendimiento (doc 07, «Presupuestos»; doc 09, «Rendimiento»). Se mide contra la app compilada en
// modo simulado y en esta máquina: sirve para notar que algo empeoró, no reemplaza medir contra el
// backend real (red, base de datos y SSE de verdad). Cada prueba escribe lo medido en el reporte.

/**
 * Techo del JS comprimido de /b/[bar] en esta compilación simulada: 225 kB medidos el 2026-11-25 y
 * un margen pequeño. Aquí cuenta también el registro completo del contrato, que solo baja porque lo
 * pide la simulación; en producción el teléfono baja 201 kB sin los esquemas del panel (doc 07).
 */
const TECHO_JS_TELEFONO_KB = 230;

/** Lo medido queda como anotación de la prueba, para copiarlo al doc 07. */
function anotar(nombre: string, valor: string) {
  test.info().annotations.push({ type: nombre, description: valor });
  console.log(`[rendimiento] ${nombre}: ${valor}`);
}

async function entrarAlPanel(page: Page) {
  await page.goto('/panel');
  await expect(page.getByRole('region', { name: 'Estado del servicio' })).toBeVisible({ timeout: 10_000 });
}

test('primera pantalla del cliente: el título en menos de 2 s (RF-001)', async ({ page }) => {
  // La primera visita instala la simulación; se mide la siguiente, como la de quien escanea el QR.
  await abrir(page, '/b/demo');
  const inicio = Date.now();
  await page.goto('/b/demo');
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  const ms = Date.now() - inicio;
  anotar('primera pantalla', `${ms} ms`);
  expect(ms).toBeLessThan(2000);
});

test('el JavaScript del teléfono se mantiene en su presupuesto (doc 07)', async ({ page }) => {
  await abrir(page, '/b/demo');
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  // Lo que baja la ruta, comprimido. La simulación (MSW, motor y datos) no cuenta: en producción
  // no se descarga (doc 07, «Mediciones» del 2026-10-27).
  const { total, simulacion } = await page.evaluate(async () => {
    const scripts = performance.getEntriesByType('resource').filter((r) => (r as PerformanceResourceTiming).initiatorType === 'script') as PerformanceResourceTiming[];
    let total = 0;
    let simulacion = 0;
    for (const s of scripts) {
      const texto = await fetch(s.name).then((r) => r.text());
      if (/mockServiceWorker|setupWorker/.test(texto)) simulacion += s.encodedBodySize;
      else total += s.encodedBodySize;
    }
    return { total, simulacion };
  });
  const kb = Math.round(total / 1024);
  anotar('JS de /b/demo', `${kb} kB comprimidos (sin los ${Math.round(simulacion / 1024)} kB de la simulación)`);
  // Techo, no presupuesto (el propuesto es 180 kB, doc 07): no deja que crezca; al bajarlo, se baja el techo.
  expect(kb).toBeLessThanOrEqual(TECHO_JS_TELEFONO_KB);
});

test('pausar la música: el panel lo muestra al pulsar y el servidor lo confirma en menos de 3 s (RNF-002)', async ({ page }) => {
  await abrir(page, '/panel/entrar');
  await page.getByLabel('Correo').fill('admin@rockola.demo');
  await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('region', { name: 'Estado del servicio' })).toBeVisible();
  const respuesta = page.waitForResponse((r) => /\/panel\/reproduccion\/[^/]+\/pausa$/.test(r.url()) && r.request().method() === 'POST');
  const inicio = Date.now();
  await page.getByRole('button', { name: 'Pausar música' }).click();
  const reanudar = page.getByRole('button', { name: 'Reanudar música' });
  await expect(reanudar).toBeVisible({ timeout: 1000 });
  const alPulsar = Date.now() - inicio;
  // Confirmado: el servidor respondió y la tecla dejó de estar ocupada, que es cuando llegó la noche nueva.
  expect((await respuesta).status()).toBe(204);
  await expect(reanudar).not.toHaveAttribute('aria-busy', 'true', { timeout: 3000 });
  const confirmado = Date.now() - inicio;
  anotar('pausar', `se ve en ${alPulsar} ms, confirmado en ${confirmado} ms`);
  expect(confirmado).toBeLessThan(3000);
});

test('saltar desde el panel llega a la TV en menos de 3 s (RNF-002, RNF-009)', async ({ page, context }) => {
  await abrirTvVinculada(page);
  await page.getByRole('button', { name: 'Encender' }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Rebelión' })).toBeVisible();
  // vincularTv ya dejó la sesión del Administrador en este navegador.
  const panel = await context.newPage();
  await entrarAlPanel(panel);
  const inicio = Date.now();
  await panel.getByRole('button', { name: 'Saltar' }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Cali Pachanguero' })).toBeVisible({ timeout: 3000 });
  const tv = Date.now() - inicio;
  await expect(panel.getByRole('heading', { level: 2, name: 'Cali Pachanguero' })).toBeVisible({ timeout: 3000 });
  const enPanel = Date.now() - inicio;
  anotar('saltar', `TV en ${tv} ms, panel en ${enPanel} ms`);
  expect(Math.max(tv, enPanel)).toBeLessThan(3000);
});
