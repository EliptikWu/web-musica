/**
 * Lo que el teléfono puede hacer con cada canción (RF-009 a RF-012, RN-01 a RN-04). La
 * regla manda en el servidor; aquí se decide qué tecla se pinta.
 */

export type EstadoTecla = 'votar' | 'votaste' | 'tuya';

type YoMinimo = {
  votos: readonly string[];
  solicitudes: readonly { solicitud_id: string; estado: string }[];
};

/** «Tuya» si la pidió este teléfono; «Votaste» si ya votó; si no, «+1». */
export function estadoTecla(solicitudId: string, yo: YoMinimo | null | undefined): EstadoTecla {
  if (!yo) return 'votar';
  if (yo.solicitudes.some((s) => s.solicitud_id === solicitudId)) return 'tuya';
  if (yo.votos.includes(solicitudId)) return 'votaste';
  return 'votar';
}

/** IDs de las solicitudes propias que siguen en la cola. */
export function solicitudesPropiasPendientes(yo: YoMinimo | null | undefined): Set<string> {
  return new Set((yo?.solicitudes ?? []).filter((s) => s.estado === 'pendiente').map((s) => s.solicitud_id));
}

/** El servicio deja pedir y votar solo si está abierto (RF-003). */
export function aceptaPedidos(estadoServicio: string): boolean {
  return estadoServicio === 'abierto';
}
