import { type Locator, type Page, test } from '@playwright/test';
import path from 'node:path';
import { abrir } from './support';

/**
 * Las capturas del manual de uso (RNF-016): se rehacen con `npm run capturas -w @rockola/web`
 * cada vez que cambia una pantalla que el manual muestra, y quedan en `public/manual/`.
 * Simulación con el reloj quieto: la misma noche de los prototipos. Cada captura lleva marcado,
 * con un anillo y una flecha, dónde se toca (punto 20 del equipo): como se hace aquí con la
 * pantalla de verdad, la marca sigue al botón aunque la interfaz se mueva.
 */
test.use({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 1 });

const destino = (nombre: string) => path.join(import.meta.dirname, '..', 'public', 'manual', `${nombre}.jpg`);

/** Toma la captura con los objetivos marcados y quita las marcas después. */
async function tomar(page: Page, nombre: string, marcar: Locator[] = []) {
  const cajas = [];
  for (const objetivo of marcar) {
    const caja = await objetivo.first().boundingBox();
    if (caja) cajas.push(caja);
  }
  // Los colores salen de los tokens de la página (ámbar con halo de nogal), no de valores sueltos.
  await page.evaluate((cajas) => {
    const capa = document.createElement('div');
    capa.id = 'marcas-manual';
    capa.style.cssText = 'position:fixed;inset:0;pointer-events:none;z-index:2147483647;color:var(--ambar-claro)';
    const svg = 'http://www.w3.org/2000/svg';
    for (const c of cajas) {
      const m = 8;
      const anillo = document.createElement('div');
      anillo.style.cssText = `position:absolute;left:${c.x - m}px;top:${c.y - m}px;width:${c.width + 2 * m}px;height:${c.height + 2 * m}px;border:4px solid currentColor;border-radius:14px;box-shadow:0 0 0 3px var(--nogal-hondo),inset 0 0 0 3px var(--nogal-hondo)`;
      capa.appendChild(anillo);
      // La flecha llega desde la izquierda; si no cabe, desde la derecha, y si tampoco, desde abajo.
      const porIzquierda = c.x > 120;
      const porDerecha = !porIzquierda && c.x + c.width + 120 < window.innerWidth;
      const punta = porIzquierda
        ? { x: c.x - m - 4, y: c.y + c.height / 2 }
        : porDerecha
          ? { x: c.x + c.width + m + 4, y: c.y + c.height / 2 }
          : { x: c.x + Math.min(c.width / 2, 160), y: c.y + c.height + m + 4 };
      const cola = porIzquierda ? { x: punta.x - 70, y: punta.y + 60 } : porDerecha ? { x: punta.x + 70, y: punta.y + 60 } : { x: punta.x + 60, y: punta.y + 70 };
      // La punta: dos trazos a 30° del cuerpo de la flecha.
      const angulo = Math.atan2(cola.y - punta.y, cola.x - punta.x);
      const ala = (giro: number) => ({ x: punta.x + 18 * Math.cos(angulo + giro), y: punta.y + 18 * Math.sin(angulo + giro) });
      const [a1, a2] = [ala(Math.PI / 6), ala(-Math.PI / 6)];
      const dibujo = document.createElementNS(svg, 'svg');
      dibujo.setAttribute('width', String(window.innerWidth));
      dibujo.setAttribute('height', String(window.innerHeight));
      dibujo.style.cssText = 'position:absolute;inset:0;overflow:visible';
      for (const [ancho, color] of [[9, 'var(--nogal-hondo)'], [5, 'currentColor']] as const) {
        const trazo = document.createElementNS(svg, 'path');
        trazo.setAttribute('d', `M${cola.x},${cola.y} L${punta.x},${punta.y} M${a1.x},${a1.y} L${punta.x},${punta.y} L${a2.x},${a2.y}`);
        trazo.setAttribute('fill', 'none');
        trazo.setAttribute('stroke-linecap', 'round');
        trazo.setAttribute('stroke-linejoin', 'round');
        trazo.style.cssText = `stroke:${color};stroke-width:${ancho}px`;
        dibujo.appendChild(trazo);
      }
      capa.appendChild(dibujo);
    }
    document.body.appendChild(capa);
  }, cajas);
  await page.screenshot({ path: destino(nombre), type: 'jpeg', quality: 72 });
  await page.evaluate(() => document.getElementById('marcas-manual')?.remove());
}

test('capturas del manual', async ({ page }) => {
  test.setTimeout(180_000);
  await abrir(page, '/panel/entrar');
  await tomar(page, 'entrar', [page.getByRole('button', { name: 'Entrar' })]);

  await page.getByLabel('Correo').fill('admin@rockola.demo');
  await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
  await page.getByRole('button', { name: 'Entrar' }).click();
  const franja = page.getByRole('region', { name: 'Estado del servicio' });
  await franja.waitFor();
  await page.waitForTimeout(600);
  const menu = page.getByRole('navigation', { name: 'Secciones del panel' });
  await tomar(page, 'menu', [menu.locator('.panel-subnav').first()]);
  await tomar(page, 'operacion', [franja]);
  await tomar(page, 'musica', [page.getByRole('button', { name: 'Saltar', exact: true }), page.getByRole('button', { name: 'Pausar música' })]);
  await tomar(page, 'mantenimiento', [franja.getByRole('button', { name: 'Cerrar la noche' }), franja.getByRole('button', { name: 'Modo mantenimiento' })]);

  await franja.getByRole('button', { name: 'Pausar solicitudes' }).click();
  await page.getByText('En pausa, no se reciben canciones nuevas').waitFor();
  await tomar(page, 'pausa', [franja.getByRole('button', { name: 'Reanudar solicitudes' })]);
  await franja.getByRole('button', { name: 'Reanudar solicitudes' }).click();
  await page.getByText('Abierto, recibiendo canciones').waitFor();

  await page.getByRole('button', { name: 'Retirar TQG' }).first().click();
  await page.getByRole('dialog').getByRole('radio', { name: 'Duplicada' }).check();
  await tomar(page, 'retirar', [page.getByRole('dialog').getByRole('radio', { name: 'Duplicada' })]);
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();

  await page.getByRole('button', { name: 'Bloquear', exact: true }).click();
  await page.getByRole('dialog').getByLabel('Motivo').fill('No va con la noche de salsa');
  await tomar(page, 'bloquear', [page.getByRole('dialog').getByLabel('Motivo')]);
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();

  const paginas: [string, string, string, (p: Page) => Locator][] = [
    ['/panel/horario', 'horario', 'Guardar horario', (p) => p.getByRole('button', { name: 'Guardar horario' })],
    ['/panel/listas/l-salsa', 'listas', 'Agregar canciones', (p) => p.getByRole('search')],
    ['/panel/canciones', 'canciones', 'Clasificar las que faltan', (p) => p.getByRole('button', { name: 'Clasificar las que faltan' })],
    ['/panel/bloqueos', 'bloqueos', 'Mostrar', (p) => p.getByRole('main').getByRole('button', { name: /Desbloquear/ })],
    ['/panel/usuarios', 'personal', 'Agregar a alguien', (p) => p.getByRole('button', { name: 'Agregar a alguien' })],
    ['/panel/establecimiento', 'bar-qr', 'Código QR de las mesas', (p) => p.locator('.qr-grande__codigo')],
    ['/panel/bitacora', 'bitacora', 'Quién', (p) => p.getByLabel('Quién')],
    ['/panel/eventos', 'eventos', 'Próximos', (p) => p.getByRole('button', { name: 'Agregar evento' })],
    ['/panel/analitica', 'tablero', 'En estas noches', (p) => p.getByRole('navigation', { name: /tablero/i }).or(p.locator('.analitica-nav'))],
  ];
  for (const [ruta, nombre, esperar, objetivo] of paginas) {
    await page.goto(ruta);
    await page.getByText(esperar, { exact: false }).first().waitFor();
    await page.waitForTimeout(400);
    await tomar(page, nombre, [objetivo(page)]);
  }

  // La TV sin vincular, con su código (RF-058). El código sale al azar en cada captura.
  const tv = await page.context().newPage();
  await tv.setViewportSize({ width: 1280, height: 720 });
  await tv.goto('/b/demo/pantalla');
  await tv.locator('.codigo-tv .tecla').first().waitFor();
  await tv.waitForTimeout(400);
  await tomar(tv, 'pantalla-codigo', [tv.locator('.codigo-tv')]);
});
