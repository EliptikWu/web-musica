import {
  type Consulta,
  type Cuerpo,
  ErrorApi,
  endpoints,
  type NombreEndpoint,
  type Parametros,
  type Respuesta,
  rutaDe,
} from '@rockola/contrato';
import { idDispositivo } from '../dispositivo';
import { nuevaClave } from '../idempotencia';
import { esperarSimulacion } from '../modo';
import { ErrorApiCliente, ErrorContrato, ErrorRed } from './errores';

/** Todo pasa por /api del mismo origen: en modo real, la ruta de Next lo reenvía al backend (doc 03). */
export const BASE_API = '/api';
const TIEMPO_MAXIMO_MS = 8000; // (propuesto)

type SinParametros<N extends NombreEndpoint> = keyof Parametros<N> extends never ? true : false;

export type OpcionesPedido<N extends NombreEndpoint> = {
  cuerpo?: Cuerpo<N>;
  consulta?: Consulta<N>;
  /** Para reintentar sin duplicar: la misma clave devuelve la misma respuesta. */
  idempotencia?: string;
  signal?: AbortSignal;
} & (SinParametros<N> extends true ? { parametros?: Parametros<N> } : { parametros: Parametros<N> });

type Argumentos<N extends NombreEndpoint> = SinParametros<N> extends true ? [opciones?: OpcionesPedido<N>] : [opciones: OpcionesPedido<N>];

/**
 * Llama a un endpoint del contrato y valida la respuesta con su esquema. Lanza
 * `ErrorApiCliente`, `ErrorRed` o `ErrorContrato`; nunca devuelve datos sin validar.
 */
export async function pedir<N extends NombreEndpoint>(nombre: N, ...[opciones]: Argumentos<N>): Promise<Respuesta<N>> {
  const endpoint = endpoints[nombre];
  const op = (opciones ?? {}) as OpcionesPedido<N> & { parametros?: Record<string, string> };
  await esperarSimulacion();

  let url = BASE_API + rutaDe(endpoint.ruta, op.parametros ?? {});
  if (op.consulta) {
    const consulta = new URLSearchParams();
    for (const [clave, valor] of Object.entries(op.consulta as Record<string, unknown>)) {
      if (valor !== undefined && valor !== null && valor !== '') consulta.set(clave, String(valor));
    }
    const texto = consulta.toString();
    if (texto) url += `?${texto}`;
  }

  const cabeceras: Record<string, string> = { Accept: 'application/json' };
  if (endpoint.superficie === 'cliente' && typeof window !== 'undefined') cabeceras['X-Rockola-Dispositivo'] = idDispositivo();
  if ('idempotente' in endpoint && endpoint.idempotente) cabeceras['Idempotency-Key'] = op.idempotencia ?? nuevaClave();
  if (op.cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';

  const limite = AbortSignal.timeout(TIEMPO_MAXIMO_MS);
  let respuesta: Response;
  try {
    respuesta = await fetch(url, {
      method: endpoint.metodo,
      headers: cabeceras,
      body: op.cuerpo === undefined ? undefined : JSON.stringify(op.cuerpo),
      credentials: 'same-origin',
      cache: 'no-store',
      signal: op.signal ? AbortSignal.any([op.signal, limite]) : limite,
    });
  } catch (causa) {
    if (op.signal?.aborted) throw causa; // Cancelada a propósito (otra búsqueda la reemplazó).
    throw new ErrorRed(causa, limite.aborted);
  }

  if (!respuesta.ok) {
    const cuerpo: unknown = await respuesta.json().catch(() => null);
    const error = ErrorApi.safeParse(cuerpo);
    if (error.success) throw new ErrorApiCliente(respuesta.status, error.data.codigo, error.data.mensaje, error.data.detalle);
    throw new ErrorApiCliente(respuesta.status, 'error_interno', `Respuesta ${respuesta.status} sin cuerpo de error`);
  }

  if (respuesta.status === 204) return null as Respuesta<N>;
  const datos: unknown = await respuesta.json().catch(() => undefined);
  const validado = endpoint.respuesta.safeParse(datos);
  if (!validado.success) {
    console.error(`[contrato] ${nombre}`, validado.error.issues);
    throw new ErrorContrato(nombre, validado.error.issues);
  }
  return validado.data as Respuesta<N>;
}
