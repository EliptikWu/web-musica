import type { RecursoPanel } from '@rockola/contrato';
import { sse } from 'msw';
import { alCambiar, conEstado } from '../almacen';
import { bloqueoVigente, construirAhora } from '../motor';
import type { EstadoSim } from '../tipos';

/**
 * El canal de tiempo real de la simulación: el mismo SSE que dará el backend (doc 04). Cada
 * pestaña manda la foto nueva cuando la simulación cambia aquí o en otra pestaña, y un
 * reloj de un segundo hace avanzar la noche aunque nadie toque nada.
 */
const PING_MS = 15_000;

/**
 * Una huella por recurso del panel: el evento `panel` nombra solo los que cambiaron. Así un voto
 * no recarga el personal ni las listas, y un 403 (que deja rastro en la bitácora) no hace
 * volver a pedir lo que se rechazó.
 */
function huellas(estado: EstadoSim, ahora: number): Record<RecursoPanel, string> {
  const ultima = estado.acciones.at(-1);
  return {
    noche: String(estado.version),
    yo: JSON.stringify([estado.bar.nombre, estado.bar.zona_horaria, estado.usuarios]),
    bitacora: `${estado.acciones.length}:${ultima?.id ?? ''}`,
    // Un bloqueo por horas vence solo con el tiempo: cuentan también los que siguen vigentes.
    bloqueos: JSON.stringify([estado.bloqueos, estado.bloqueos.filter((b) => bloqueoVigente(estado, b.video_id, ahora)?.id === b.id).map((b) => b.id)]),
    listas: JSON.stringify(estado.listas),
    horario: JSON.stringify(estado.horario),
    usuarios: JSON.stringify(estado.usuarios),
    establecimiento: JSON.stringify([estado.bar, estado.qr.token]),
    eventos: JSON.stringify(estado.eventos),
  };
}

export const manejadorTiempoReal = sse<{ ahora: unknown; panel: unknown; ping: unknown }>('/api/bares/:slug/eventos', async ({ client, request, params }) => {
  const foto = await conEstado((estado, ahora) => ({ cortada: estado.fallos.conexion === 'cortada', slug: estado.bar.slug, ahora: construirAhora(estado, ahora), huellas: huellas(estado, ahora) }));
  if (foto.cortada || params.slug !== foto.slug) {
    client.error();
    return;
  }
  let version = foto.ahora.version;
  let ultimas = foto.huellas;
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
    const actual = await conEstado((estado, ahora) => ({ cortada: estado.fallos.conexion === 'cortada', ahora: construirAhora(estado, ahora), huellas: huellas(estado, ahora) }));
    if (actual.cortada) {
      cerrar();
      client.error();
      return;
    }
    if (actual.ahora.version === version) return;
    version = actual.ahora.version;
    client.send({ id: String(version), event: 'ahora', data: { tipo: 'ahora', ahora: actual.ahora } });
    const recursos = (Object.keys(actual.huellas) as RecursoPanel[]).filter((r) => actual.huellas[r] !== ultimas[r]);
    ultimas = actual.huellas;
    client.send({ event: 'panel', data: { tipo: 'panel', recursos } });
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
