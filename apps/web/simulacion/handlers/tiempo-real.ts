import { sse } from 'msw';
import { alCambiar, conEstado } from '../almacen';
import { construirAhora } from '../motor';

/**
 * El canal de tiempo real de la simulación: el mismo SSE que dará el backend (doc 04). Cada
 * pestaña manda la foto nueva cuando la simulación cambia aquí o en otra pestaña, y un
 * reloj de un segundo hace avanzar la noche aunque nadie toque nada.
 */
const PING_MS = 15_000;

export const manejadorTiempoReal = sse<{ ahora: unknown; panel: unknown; ping: unknown }>('/api/bares/:slug/eventos', async ({ client, request, params }) => {
  const foto = await conEstado((estado, ahora) => ({ cortada: estado.fallos.conexion === 'cortada', slug: estado.bar.slug, ahora: construirAhora(estado, ahora) }));
  if (foto.cortada || params.slug !== foto.slug) {
    client.error();
    return;
  }
  let version = foto.ahora.version;
  client.send({ id: String(version), event: 'ahora', data: { tipo: 'ahora', ahora: foto.ahora } });

  let ocupado = false;
  const enviarSiCambio = async () => {
    if (ocupado) return;
    ocupado = true;
    try {
      await enviar();
    } finally {
      ocupado = false;
    }
  };
  const enviar = async () => {
    const actual = await conEstado((estado, ahora) => ({ cortada: estado.fallos.conexion === 'cortada', ahora: construirAhora(estado, ahora) }));
    if (actual.cortada) {
      cerrar();
      client.error();
      return;
    }
    if (actual.ahora.version === version) return;
    version = actual.ahora.version;
    client.send({ id: String(version), event: 'ahora', data: { tipo: 'ahora', ahora: actual.ahora } });
    client.send({ event: 'panel', data: { tipo: 'panel', recursos: ['noche', 'bitacora', 'bloqueos'] } });
  };

  const dejarDeOir = alCambiar(() => void enviarSiCambio());
  const reloj = setInterval(() => void enviarSiCambio(), 1000);
  const ping = setInterval(() => {
    void conEstado((_estado, ahora) => ahora).then((ahora) => client.send({ event: 'ping', data: { tipo: 'ping', servidor_ahora: new Date(ahora).toISOString() } }));
  }, PING_MS);
  function cerrar() {
    dejarDeOir();
    clearInterval(reloj);
    clearInterval(ping);
  }
  request.signal.addEventListener('abort', cerrar);
});
