import { EventoTiempoReal } from '@rockola/contrato';
import { BASE_API } from '../api/cliente';
import { esperarSimulacion } from '../modo';

export type EstadoConexion = 'conectando' | 'conectado' | 'reconectando' | 'desconectado';

/**
 * Canal de tiempo real (doc 03, ADR-07). Hoy es SSE; si el backend elige WebSocket, se escribe
 * otra función con la misma firma y nada más cambia.
 */
export type ConectarTiempoReal = (
  slug: string,
  alEvento: (evento: EventoTiempoReal) => void,
  alEstado: (estado: EstadoConexion) => void,
) => () => void;

const ESPERAS_MS = [1000, 2000, 4000, 8000, 15000];
const SIN_PING_MS = 35_000; // dos pings perdidos (cada 15 s) y uno de gracia

/** SSE sobre `/api/bares/{slug}/eventos`, con reconexión escalonada y detección de silencio. */
export const conectarSse: ConectarTiempoReal = (slug, alEvento, alEstado) => {
  let fuente: EventSource | null = null;
  let intento = 0;
  let cerrado = false;
  let reintento: ReturnType<typeof setTimeout> | undefined;
  let vigilante: ReturnType<typeof setTimeout> | undefined;

  const vigilar = () => {
    clearTimeout(vigilante);
    vigilante = setTimeout(() => reconectar(), SIN_PING_MS);
  };

  const recibir = (mensaje: MessageEvent<string>) => {
    vigilar();
    let datos: unknown;
    try {
      datos = JSON.parse(mensaje.data);
    } catch {
      console.error('[tiempo real] mensaje que no es JSON', mensaje.data);
      return;
    }
    const evento = EventoTiempoReal.safeParse(datos);
    if (evento.success) alEvento(evento.data);
    else console.error('[tiempo real] evento fuera del contrato', evento.error.issues);
  };

  const abrir = async () => {
    await esperarSimulacion();
    if (cerrado) return;
    alEstado(intento === 0 ? 'conectando' : 'reconectando');
    fuente = new EventSource(`${BASE_API}/bares/${encodeURIComponent(slug)}/eventos`);
    fuente.onopen = () => {
      intento = 0;
      alEstado('conectado');
      vigilar();
    };
    for (const tipo of ['ahora', 'panel', 'ping'] as const) fuente.addEventListener(tipo, recibir);
    fuente.onerror = () => reconectar();
  };

  const reconectar = () => {
    fuente?.close();
    fuente = null;
    clearTimeout(vigilante);
    if (cerrado) return;
    alEstado(intento >= ESPERAS_MS.length ? 'desconectado' : 'reconectando');
    const espera = ESPERAS_MS[Math.min(intento, ESPERAS_MS.length - 1)]! * (0.8 + Math.random() * 0.4);
    intento++;
    clearTimeout(reintento);
    reintento = setTimeout(() => void abrir(), espera);
  };

  void abrir();
  return () => {
    cerrado = true;
    clearTimeout(reintento);
    clearTimeout(vigilante);
    fuente?.close();
  };
};
