import { http, HttpResponse } from 'msw';
import { setupWorker } from 'msw/browser';
import { preparar } from './almacen';
import { manejadoresCliente } from './handlers/cliente';
import { manejadoresAnalitica } from './handlers/analitica';
import { manejadoresGestion } from './handlers/gestion';
import { manejadoresPanel } from './handlers/panel';
import { manejadoresPantalla } from './handlers/pantalla';
import { manejadorTiempoReal } from './handlers/tiempo-real';

/** Lo que la simulación aún no sabe hacer responde 501, con un mensaje claro, en vez de ir a la red. */
const noImplementado = http.all('/api/*', ({ request }) =>
  HttpResponse.json({ codigo: 'error_interno', mensaje: `La simulación todavía no responde ${request.method} ${new URL(request.url).pathname}` }, { status: 501 }),
);

/**
 * Arranca la simulación (doc 03): prepara el escenario de la URL y registra el Service Worker
 * de MSW. Solo se carga en modo simulado, desde instrumentation-client.ts.
 */
export async function arrancar(): Promise<void> {
  if (!('serviceWorker' in navigator)) {
    throw new Error('La simulación necesita Service Workers: abre la app en localhost o por https.');
  }
  await preparar(new URLSearchParams(location.search));
  const worker = setupWorker(manejadorTiempoReal, ...manejadoresCliente, ...manejadoresPantalla, ...manejadoresPanel, ...manejadoresGestion, ...manejadoresAnalitica, noImplementado);
  await worker.start({ onUnhandledFrame: 'bypass', quiet: true, serviceWorker: { url: '/mockServiceWorker.js' } });
}
