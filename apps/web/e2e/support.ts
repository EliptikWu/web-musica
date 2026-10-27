import AxeBuilder from '@axe-core/playwright';
import { expect, type Page } from '@playwright/test';

/** Abre una ruta con un escenario de la simulación recién sembrado y espera a que responda. */
export async function abrir(pagina: Page, ruta: string, escenario = 'momento-b', reloj: 'fijo' | 'normal' | 'rapido' = 'fijo') {
  const url = new URL(ruta, 'http://x');
  url.searchParams.set('escenario', escenario);
  url.searchParams.set('reiniciar', '1');
  if (reloj !== 'normal') url.searchParams.set('reloj', reloj);
  await pagina.goto(`${url.pathname}${url.search}`);
  await pagina.waitForFunction(() => Boolean(navigator.serviceWorker?.controller), undefined, { timeout: 15_000 });
}

/** Auditoría de accesibilidad (wcag2a, wcag2aa, wcag21aa) del estado que se ve ahora (doc 09). */
export async function sinViolaciones(pagina: Page, contexto: string) {
  const { violations } = await new AxeBuilder({ page: pagina }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
  const resumen = violations.map((v) => `${v.id} (${v.impact}): ${v.nodes.map((n) => n.target.join(' ')).slice(0, 3).join(' | ')}`);
  expect(resumen, `axe en «${contexto}»`).toEqual([]);
}

/**
 * Recoge errores de consola y de la política de contenido para revisarlos al final. Las
 * respuestas fallidas solo se pasan por alto si la prueba las espera (`permitir`); el 404 de
 * las precargas de Next siempre.
 */
export function vigilarConsola(pagina: Page, { permitir = [] as number[] } = {}): string[] {
  const errores: string[] = [];
  const esperadas = [404, ...permitir];
  pagina.on('console', (m) => {
    if (m.type() !== 'error') return;
    const estado = /status of (\d{3})/.exec(m.text())?.[1];
    if (estado && esperadas.includes(Number(estado))) return;
    errores.push(m.text());
  });
  pagina.on('pageerror', (e) => errores.push(e.message));
  return errores;
}

/** Todo control táctil de la vista mide 44 × 44 px o más (doc 05, «Accesibilidad»). */
export async function objetivosDe44(pagina: Page, selector = 'main a, main button, nav a, nav button, footer a, footer button') {
  const pequenos = await pagina.$$eval(selector, (els) =>
    els
      .filter((el) => (el as HTMLElement).offsetParent !== null)
      .map((el) => ({ texto: (el.textContent ?? el.getAttribute('aria-label') ?? '').trim().slice(0, 30), r: el.getBoundingClientRect() }))
      .filter(({ r }) => r.width < 44 || r.height < 44)
      .map(({ texto, r }) => `${texto} (${Math.round(r.width)}×${Math.round(r.height)})`),
  );
  expect(pequenos, 'objetivos de menos de 44 px').toEqual([]);
}
