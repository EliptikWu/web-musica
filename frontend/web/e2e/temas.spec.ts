import { expect, test } from '@playwright/test';
import { abrir, objetivosDe44, sinViolaciones, vigilarConsola, vincularTv } from './support';

// Iteraciones 9 y 10: el diseño que elige el bar (RF-059, ADR-25). Cada tema pasa por las
// mismas reglas que Rockola: axe sin violaciones, objetivos de 44 px y estados distinguibles.

for (const tema of ['afiche', 'vidrio'] as const) {
  test.describe(`diseño ${tema}`, () => {
    test('teléfono: bienvenida, buscar, cola y cerrado pasan axe y miden 44 px', async ({ page }) => {
      const errores = vigilarConsola(page);
      await abrir(page, `/b/demo?tema=${tema}`);
      await expect(page.locator('html')).toHaveAttribute('data-tema', tema);
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await sinViolaciones(page, `${tema}: bienvenida`);
      await objetivosDe44(page);

      await page.getByRole('link', { name: 'Entrar y votar' }).click();
      await page.getByRole('searchbox').fill('bad bunny');
      await expect(page.getByRole('button', { name: /^Pedir / }).first()).toBeVisible();
      await sinViolaciones(page, `${tema}: buscar`);
      await objetivosDe44(page);

      await page.getByRole('link', { name: 'Cola', exact: true }).click();
      await expect(page.getByRole('region', { name: 'Tu canción' })).toBeVisible();
      // «Votaste», «Tuya» y «+1» se distinguen por el texto, no solo por la forma del gesto.
      await expect(page.getByText('Votaste', { exact: true }).first()).toBeVisible();
      await expect(page.getByText('Tuya', { exact: true })).toBeVisible();
      await sinViolaciones(page, `${tema}: cola`);
      await objetivosDe44(page);

      await abrir(page, `/b/demo?tema=${tema}`, 'fuera-de-horario');
      await expect(page.getByRole('heading', { name: 'Por ahora no recibimos canciones' })).toBeVisible();
      await sinViolaciones(page, `${tema}: cerrado`);
      expect(errores).toEqual([]);
    });

    test('el diseño se pinta antes de cargar el bar, con el script de la CSP (ADR-26)', async ({ browser }) => {
      // Sin el JavaScript de la app (ni Service Worker): solo puede ponerlo el script previo, y
      // solo si la CSP lo deja correr (lleva el nonce).
      const contexto = await browser.newContext({ serviceWorkers: 'block' });
      const page = await contexto.newPage();
      const bloqueos: string[] = [];
      page.on('console', (m) => m.type() === 'error' && /Content Security Policy/i.test(m.text()) && bloqueos.push(m.text()));
      await page.route('**/_next/static/**/*.js', (r) => r.abort());
      await page.goto('/panel/entrar');
      await page.evaluate((t) => localStorage.setItem('rockola.tema.demo', t), tema);
      await page.goto('/b/demo/cola');
      expect(await page.evaluate(() => document.documentElement.dataset.tema)).toBe(tema);
      expect(bloqueos).toEqual([]);
      // Un tema que no existe, o fuera de las páginas del bar, no se aplica.
      await page.evaluate(() => localStorage.setItem('rockola.tema.demo', 'neon'));
      await page.goto('/b/demo');
      expect(await page.evaluate(() => document.documentElement.dataset.tema)).toBeUndefined();
      await page.evaluate((t) => localStorage.setItem('rockola.tema.demo', t), tema);
      await page.goto('/panel/entrar');
      expect(await page.evaluate(() => document.documentElement.dataset.tema)).toBeUndefined();
      await contexto.close();
    });

    test('TV: sin vincular y vinculada pasan axe', async ({ page }) => {
      await page.setViewportSize({ width: 1280, height: 720 });
      await abrir(page, `/b/demo/pantalla?tema=${tema}`);
      await expect(page.locator('html')).toHaveAttribute('data-tema', tema);
      await sinViolaciones(page, `${tema}: TV sin vincular`);
      await vincularTv(page);
      await page.getByRole('button', { name: 'Encender' }).click();
      await expect(page.getByRole('heading', { name: 'Lo que viene' })).toBeVisible();
      await sinViolaciones(page, `${tema}: TV`);
      // El QR se tiene que poder escanear: módulos oscuros sobre el fondo claro, con el color del tema.
      const contraste = await page.locator('.qr-tv__codigo').evaluate((caja) => {
        const rgb = (c: string) => (c.match(/\d+(\.\d+)?/g) ?? []).slice(0, 3).map(Number);
        const lum = (c: string) => {
          const [r = 0, g = 0, b = 0] = rgb(c).map((v) => (v / 255 <= 0.03928 ? v / 255 / 12.92 : ((v / 255 + 0.055) / 1.055) ** 2.4));
          return 0.2126 * r + 0.7152 * g + 0.0722 * b;
        };
        const modulos = getComputedStyle(caja.querySelectorAll('svg path')[1]!).fill;
        const fondo = getComputedStyle(caja).backgroundColor;
        const [clara, oscura] = [lum(modulos), lum(fondo)].sort((x, y) => y - x) as [number, number];
        return (clara + 0.05) / (oscura + 0.05);
      });
      expect(contraste).toBeGreaterThan(7);
    });

    test('panel: el tema del bar en sus estados, el tablero y el diálogo; al salir vuelve a Rockola (D19)', async ({ page }) => {
      // Al salir, la consulta de la noche que iba en curso responde 401; la ruta inexistente, 404.
      const errores = vigilarConsola(page, { permitir: [401, 404] });
      for (const escenario of ['momento-b', 'en-pausa', 'fuera-de-horario', 'cola-vacia', 'sin-lista-activa']) {
        await abrir(page, `/panel/entrar?tema=${tema}`, escenario);
        await expect(page.locator('html')).not.toHaveAttribute('data-tema', /.*/);
        await page.getByLabel('Correo').fill('admin@rockola.demo');
        await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
        await page.getByRole('button', { name: 'Entrar' }).click();
        await page.getByRole('region', { name: 'Estado del servicio' }).waitFor();
        await expect(page.locator('html')).toHaveAttribute('data-tema', tema);
        await sinViolaciones(page, `${tema}: panel, ${escenario}`);
        await objetivosDe44(page);
        await page.getByRole('button', { name: 'Salir' }).click();
        await page.waitForURL('**/panel/entrar');
        await expect(page.locator('html')).not.toHaveAttribute('data-tema', /.*/);
        expect(await page.evaluate(() => localStorage.getItem('rockola.tema-panel'))).toBeNull();
      }

      await abrir(page, `/panel/entrar?tema=${tema}`);
      await page.getByLabel('Correo').fill('admin@rockola.demo');
      await page.getByLabel('Clave', { exact: true }).fill('rockola-demo');
      await page.getByRole('button', { name: 'Entrar' }).click();
      const estado = page.getByRole('region', { name: 'Estado del servicio' });
      await estado.waitFor();
      await estado.getByRole('button', { name: 'Modo mantenimiento' }).click();
      await expect(page.getByRole('dialog')).toBeVisible();
      await sinViolaciones(page, `${tema}: diálogo del panel`);
      await page.keyboard.press('Escape');

      // Recargar no pasa por Rockola: el script previo ya sabe el diseño.
      await page.goto('/panel/analitica/cubo');
      await page.locator('.cubito').first().waitFor();
      await expect(page.locator('html')).toHaveAttribute('data-tema', tema);
      await sinViolaciones(page, `${tema}: cubo`);
      await page.getByRole('group', { name: 'KPI de lo elegido' }).getByRole('button', { name: /^Votos/ }).click();
      await page.getByRole('button', { name: 'Urbano', exact: true }).click();
      await sinViolaciones(page, `${tema}: cubo recortado`);

      // Una dirección que no existe no se queda a medio tema.
      await page.goto('/panel/ruta-que-no-existe');
      await expect(page.getByRole('heading', { name: 'Aquí no hay ninguna rockola' })).toBeVisible();
      await expect(page.locator('html')).not.toHaveAttribute('data-tema', /.*/);
      expect(errores).toEqual([]);
    });
  });
}
