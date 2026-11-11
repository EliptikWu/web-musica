import { expect, test } from '@playwright/test';
import { abrir, objetivosDe44, sinViolaciones, vigilarConsola } from './support';

// Iteración 1: el cliente (doc 02, «Historias por iteración»). Teléfono de 360 px.
test.use({ viewport: { width: 360, height: 780 } });

test('bienvenida: el bar, lo que suena y lo más votado (PB-03, PB-06)', async ({ page }) => {
  const errores = vigilarConsola(page);
  await abrir(page, '/b/demo', 'momento-a');
  await expect(page.getByRole('heading', { level: 1, name: 'Bar de demostración' })).toBeVisible();
  const arco = page.getByRole('region', { name: 'Suena ahora' });
  await expect(arco.getByRole('heading', { name: 'Rebelión' })).toBeVisible();
  await expect(arco.getByText('24 votos')).toBeVisible();
  const top = page.getByRole('list').filter({ hasText: 'Hips' });
  await expect(top.getByRole('listitem')).toHaveText([/Rebelión.*suena ahora.*24/, /Hips Don't Lie.*sonó a las 9:40.*19/, /Cali Pachanguero.*en la cola.*15/]);
  await expect(page.getByText('tu teléfono queda como un código anónimo')).toBeVisible();
  await sinViolaciones(page, 'bienvenida');
  await objetivosDe44(page);
  expect(errores).toEqual([]);
});

test('buscar y pedir: Monaco entra de cuarta y queda 1 de 3 (PB-07, PB-13)', async ({ page }) => {
  await abrir(page, '/b/demo/buscar', 'momento-a');
  await expect(page.getByText('Te quedan 2 de 3 pedidos')).toBeVisible();
  await page.getByRole('searchbox', { name: 'Buscar en YouTube' }).fill('bad bunny');
  const enCola = page.getByRole('region', { name: 'Ya está en la cola' });
  await expect(enCola.getByRole('listitem')).toHaveCount(2);
  await expect(enCola.getByText('Votaste')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Resultados en YouTube' }).getByText('Dura más de 10 min')).toBeVisible();
  await sinViolaciones(page, 'buscar con resultados');

  await page.getByRole('button', { name: 'Pedir Monaco' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Monaco entró a la cola' })).toContainText('Puesto #4, suena en unos 14 min');
  await expect(page.getByText('Te queda 1 de 3 pedidos')).toBeVisible();
  await expect(enCola.getByRole('listitem').filter({ hasText: 'Monaco' }).getByText('Tuya')).toBeVisible();
  await sinViolaciones(page, 'después de pedir');
  await objetivosDe44(page);
});

test('buscar sin resultados y con la búsqueda vacía', async ({ page }) => {
  await abrir(page, '/b/demo/buscar');
  await expect(page.getByText('Escribe el nombre de una canción o de un artista.')).toBeVisible();
  await page.getByRole('searchbox').fill('zzzz sin nada');
  await expect(page.getByText('No encontramos «zzzz sin nada».')).toBeVisible();
});

test('votar: la tecla se hunde al instante y la cola se reordena (PB-14, PB-15)', async ({ page }) => {
  await abrir(page, '/b/demo/cola');
  const cola = page.getByRole('list', { name: 'Canciones en la cola' });
  await expect(cola.getByRole('listitem').nth(2)).toContainText('Monaco');
  await page.getByRole('button', { name: 'Votar por La Bicicleta' }).focus();
  await page.keyboard.press('Enter');
  const fila = cola.getByRole('listitem').filter({ hasText: 'La Bicicleta' });
  await expect(fila.getByText('Votaste')).toBeVisible({ timeout: 1000 });
  // Con teclado, el foco no se pierde: queda en «Votaste» de la misma fila (WCAG 2.4.3).
  await expect(fila.getByText('Votaste')).toBeFocused();
  await expect(cola.getByRole('listitem').nth(2)).toContainText('La Bicicleta');
  await expect(cola.getByRole('listitem').nth(2)).toContainText('16');
  await sinViolaciones(page, 'cola después de votar');
});

test('la cola: tu canción, la espera y el desempate explicado (PB-17 a PB-19, PB-22)', async ({ page }) => {
  await abrir(page, '/b/demo/cola');
  await expect(page.getByText('9 canciones, unos 36 min')).toBeVisible();
  const tuya = page.getByRole('region', { name: 'Tu canción' });
  await expect(tuya).toContainText('#3');
  await expect(tuya).toContainText('Subió 2 puestos');
  await expect(tuya).toContainText('Suena en unos 13 min');
  await expect(page.getByText('Por eso Monaco va antes que La Bicicleta.')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Tus pedidos de esta noche' })).toContainText('En espera, puesto #3');
  await sinViolaciones(page, 'cola');
  await objetivosDe44(page);
});

for (const [escenario, titulo] of [
  ['fuera-de-horario', 'Por ahora no recibimos canciones'],
  ['en-pausa', 'Los pedidos están en pausa'],
  ['mantenimiento', 'La rockola está en mantenimiento'],
] as const) {
  test(`servicio cerrado (${escenario}): se explica y no se puede pedir (PB-30)`, async ({ page }) => {
    await abrir(page, '/b/demo', escenario);
    await expect(page.getByRole('heading', { name: titulo })).toBeVisible();
    await expect(page.getByText('Horario de pedidos')).toBeVisible();
    await sinViolaciones(page, escenario);
    await page.getByRole('link', { name: 'Ver qué está sonando' }).click();
    await expect(page).toHaveURL(/\/b\/demo\/cola$/);
    await expect(page.getByRole('button', { name: /Votar por/ })).toHaveCount(0);
    await sinViolaciones(page, `${escenario}: cola`);
  });
}

test('fuera de horario dice cuándo abren', async ({ page }) => {
  await abrir(page, '/b/demo', 'fuera-de-horario');
  await expect(page.getByText(/Los pedidos abren hoy a las 8:00.p\..m\. Mientras tanto/)).toBeVisible();
  await expect(page.getByText('Jueves a sábado,')).toBeVisible();
});

test('sin pedidos libres: se dice y no hay tecla «Pedir»', async ({ page }) => {
  await abrir(page, '/b/demo/buscar', 'sin-pedidos');
  await expect(page.getByText('Usaste tus 3 pedidos')).toBeVisible();
  await page.getByRole('searchbox').fill('karol');
  await expect(page.getByText('Sin pedidos libres').first()).toBeVisible();
  await expect(page.getByRole('button', { name: /^Pedir / })).toHaveCount(0);
});

test('YouTube caído: mensaje claro y reintentar (PB-09)', async ({ page }) => {
  const errores = vigilarConsola(page, { permitir: [503] });
  await abrir(page, '/b/demo/buscar', 'youtube-caido');
  await page.getByRole('searchbox').fill('salsa');
  // Next tiene su propio anunciador de rutas con role=alert: se filtra por el texto.
  await expect(page.getByRole('alert').filter({ hasText: 'YouTube no respondió' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Reintentar' })).toBeVisible();
  await sinViolaciones(page, 'youtube caído');
  expect(errores).toEqual([]);
});

test('cuota agotada: resultados guardados, avisando que pueden estar incompletos (PB-54)', async ({ page }) => {
  await abrir(page, '/b/demo/buscar', 'cuota-agotada');
  await page.getByRole('searchbox').fill('niche');
  await expect(page.getByText(/estos resultados son de búsquedas anteriores/)).toBeVisible();
});

test('el QR lleva al bar; uno que no existe lo dice (RF-001, RF-035)', async ({ page }) => {
  await abrir(page, '/q/demo-qr-1');
  await expect(page).toHaveURL(/\/b\/demo$/);
  await page.goto('/q/no-existe');
  await expect(page.getByRole('heading', { name: 'Aquí no hay ninguna rockola' })).toBeVisible();
});

test('política de datos: qué se registra y qué no (PB-06)', async ({ page }) => {
  await abrir(page, '/b/demo');
  await page.getByRole('link', { name: 'Política de datos' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Política de datos' })).toBeVisible();
  await expect(page.getByText('No pedimos tu nombre, tu correo ni tu número de teléfono.')).toBeVisible();
  await sinViolaciones(page, 'política');
});

test('un pedido en otro teléfono llega a la cola en menos de 3 s (PB-18, RNF-009)', async ({ page, browser }) => {
  await abrir(page, '/b/demo/cola');
  await expect(page.getByText('9 canciones, unos 36 min')).toBeVisible();
  // Otro teléfono: otro contexto, otro ID de dispositivo, la misma simulación no (otro almacenamiento).
  // Por eso el pedido se hace en esta misma pestaña, como lo haría otro teléfono por la API.
  await page.evaluate(() =>
    fetch('/api/bares/demo/solicitudes', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Rockola-Dispositivo': 'otro-telefono', 'Idempotency-Key': crypto.randomUUID() },
      body: JSON.stringify({ video_id: 'sim-provenza' }),
    }),
  );
  await expect(page.getByRole('list', { name: 'Canciones en la cola' })).toContainText('PROVENZA', { timeout: 3000 });
  await expect(page.getByText('10 canciones', { exact: false })).toBeVisible();
  void browser;
});

test('sin conexión en vivo: aviso a los 5 s y la cola sigue al día por sondeo (RF-017)', async ({ page }) => {
  await abrir(page, '/b/demo/cola', 'desconexion');
  await expect(page.getByText(/conexión en vivo/)).toBeVisible({ timeout: 12_000 });
  // La luz no dice «En vivo» mientras dura el corte.
  await expect(page.getByText('Sin conexión', { exact: true })).toBeVisible();
  await expect(page.getByText('9 canciones, unos 36 min')).toBeVisible();
  await sinViolaciones(page, 'sin conexión');
});
