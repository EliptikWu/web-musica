'use client';

import { useSyncExternalStore } from 'react';
import { useTic } from '../reloj-servidor';
import type { EstadoConexion } from './fuente';

/**
 * Estado de la conexión en vivo, para el banner de desconexión y el sondeo de respaldo
 * (doc 03, «Tiempo real»). `desde` es cuándo se dejó de estar conectado (o cuándo cargó la
 * página, si nunca se conectó): pasar de «reconectando» a «desconectado» y vuelta no lo
 * reinicia, así el banner no parpadea con cada reintento.
 */
type Conexion = { estado: EstadoConexion; desde: number };

let actual: Conexion = { estado: 'conectando', desde: typeof window === 'undefined' ? 0 : Date.now() };
const oyentes = new Set<() => void>();

/** El cambio de estado, sin efectos: se prueba aparte. */
export function siguienteConexion(previa: Conexion, estado: EstadoConexion, ahora: number): Conexion {
  if (previa.estado === estado) return previa;
  const cruza = (previa.estado === 'conectado') !== (estado === 'conectado');
  return { estado, desde: cruza ? ahora : previa.desde };
}

export function anotarConexion(estado: EstadoConexion): void {
  const nueva = siguienteConexion(actual, estado, Date.now());
  if (nueva === actual) return;
  actual = nueva;
  for (const o of oyentes) o();
}

/** Lleva más de 5 s sin conexión en vivo (doc 03, *propuesto*). */
export function estaCaida(conexion: Conexion, tic: number): boolean {
  return conexion.estado !== 'conectado' && tic !== 0 && tic - conexion.desde > 5000;
}

function suscribir(oyente: () => void) {
  oyentes.add(oyente);
  return () => oyentes.delete(oyente);
}

const enServidor: Conexion = { estado: 'conectando', desde: 0 };

export function useConexion(): Conexion {
  return useSyncExternalStore(suscribir, () => actual, () => enServidor);
}

/** Sin conexión en vivo hace más de 5 s: sale el banner y se apaga la luz. */
export function useConexionCaida(): { caida: boolean; estado: EstadoConexion } {
  const conexion = useConexion();
  const tic = useTic();
  return { caida: estaCaida(conexion, tic), estado: conexion.estado };
}
