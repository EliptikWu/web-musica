import { expect, test } from '@playwright/test';
import { abrir, sinViolaciones, vigilarConsola } from './support';

// Iteración 0: la base funciona en un navegador de verdad (doc 08).

type Foto = { version: number; cola: { solicitud_id: string; votos_total: number }[] };
type ConFotos = { fotos: Foto[] };

test('la portada abre la simulación sin errores y pasa axe', async ({ page }) => {
  const errores = vigilarConsola(page);
  await abrir(page, '/');
  await expect(page.getByRole('heading', { level: 1, name: 'Rockola' })).toBeVisible();
  await sinViolaciones(page, 'portada');
  expect(errores).toEqual([]);
});

test('la galería usa Archivo con su eje de ancho y pasa axe', async ({ page }) => {
  const errores = vigilarConsola(page);
  await abrir(page, '/muestras');
  const ancho = await page.locator('.condensada').first().evaluate((el) => getComputedStyle(el).fontStretch);
  expect(ancho).toBe('75%');
  await page.evaluate(() => document.fonts.ready);
  const familias = await page.evaluate(() => [...document.fonts].filter((f) => f.status === 'loaded').map((f) => `${f.family} ${f.stretch}`));
  // La fuente propia se cargó con el rango de ancho completo (ADR-08), no el respaldo del sistema.
  expect(familias.some((f) => /Archivo/.test(f) && /62%\s+125%/.test(f))).toBe(true);
  await sinViolaciones(page, 'galería');
  expect(errores).toEqual([]);
});

test('las respuestas llevan la política de contenido con nonce', async ({ request }) => {
  const r = await request.get('/');
  const csp = r.headers()['content-security-policy'] ?? '';
  expect(csp).toMatch(/script-src 'self' 'nonce-[^']+' 'strict-dynamic'/);
  expect(csp).toContain("worker-src 'self'");
  expect(csp).toContain("frame-src 'none'");
});

test('un slug que termina como una extensión (pacifico) también lleva CSP; el panel pide sesión', async ({ request }) => {
  const r = await request.get('/b/pacifico', { maxRedirects: 0 });
  expect(r.headers()['content-security-policy'] ?? '').toContain("default-src 'self'");
  const panel = await request.get('/panel/xico', { maxRedirects: 0 });
  expect(panel.status()).toBe(307);
  expect(panel.headers().location).toContain('/panel/entrar');
});

test('la simulación responde el contrato y el tiempo real llega a otra pestaña', async ({ page, context }) => {
  await abrir(page, '/muestras');
  const otra = await context.newPage();
  await otra.goto('/muestras');
  await otra.waitForFunction(() => Boolean(navigator.serviceWorker?.controller));

  // La otra pestaña escucha el canal SSE y anota cada foto.
  await otra.evaluate(() => {
    const w = window as unknown as ConFotos;
    w.fotos = [];
    const fuente = new EventSource('/api/bares/demo/eventos');
    fuente.addEventListener('ahora', (e) => w.fotos.push(JSON.parse((e as MessageEvent<string>).data).ahora));
  });
  await expect.poll(() => otra.evaluate(() => (window as unknown as ConFotos).fotos.length)).toBeGreaterThan(0);

  // Un teléfono nuevo vota en la primera pestaña.
  const voto = await page.evaluate(async () => {
    const r = await fetch('/api/bares/demo/solicitudes/s-bici/votos', {
      method: 'POST',
      headers: { 'X-Rockola-Dispositivo': 'telefono-e2e', 'Idempotency-Key': crypto.randomUUID() },
    });
    return { estado: r.status, cuerpo: await r.json() };
  });
  expect(voto).toEqual({ estado: 201, cuerpo: { solicitud_id: 's-bici', votos_total: 16 } });

  // En menos de 3 s la otra pestaña ve La Bicicleta con 16 votos (RNF-009).
  await expect
    .poll(() => otra.evaluate(() => (window as unknown as ConFotos).fotos.at(-1)?.cola.find((i) => i.solicitud_id === 's-bici')?.votos_total), { timeout: 3000 })
    .toBe(16);
});

test('la misma Idempotency-Key no duplica el voto (RNF-008)', async ({ page }) => {
  await abrir(page, '/muestras');
  const [a, b] = await page.evaluate(async () => {
    const clave = crypto.randomUUID();
    const votar = () =>
      fetch('/api/bares/demo/solicitudes/s-luna/votos', { method: 'POST', headers: { 'X-Rockola-Dispositivo': 'telefono-idem', 'Idempotency-Key': clave } }).then(async (r) => ({
        estado: r.status,
        cuerpo: await r.json(),
      }));
    return [await votar(), await votar()];
  });
  expect(a).toEqual(b);
  expect(a!.cuerpo.votos_total).toBe(8);
});
