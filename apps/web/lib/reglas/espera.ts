/**
 * Avance de lo que suena y espera estimada de cada canción (RF-016, RF-018). El servidor
 * manda `posicion_seg` en el instante `servidor_ahora`; con el desfase del reloj local se
 * calcula dónde va ahora mismo sin preguntar cada segundo.
 */

type ReproduccionMinima = { posicion_seg: number; estado: 'sonando' | 'pausada'; cancion: { duracion_seg: number } };

/** Segundos reproducidos en el instante `ahoraServidorMs` (hora del servidor, ya corregida). */
export function posicionActual(reproduccion: ReproduccionMinima, servidorAhoraIso: string, ahoraServidorMs: number): number {
  const base = reproduccion.posicion_seg;
  if (reproduccion.estado === 'pausada') return Math.min(base, reproduccion.cancion.duracion_seg);
  const transcurrido = Math.max(0, (ahoraServidorMs - Date.parse(servidorAhoraIso)) / 1000);
  return Math.min(base + transcurrido, reproduccion.cancion.duracion_seg);
}

/** Lo que le falta a la canción actual. */
export function restanteActual(reproduccion: ReproduccionMinima | null, servidorAhoraIso: string, ahoraServidorMs: number): number {
  if (!reproduccion) return 0;
  return Math.max(0, reproduccion.cancion.duracion_seg - posicionActual(reproduccion, servidorAhoraIso, ahoraServidorMs));
}

/**
 * Espera de la canción en la posición `indice` (0 = la siguiente): lo que le falta a la actual
 * más lo que duran las de delante. Es una estimación y la interfaz lo dice («unos»).
 */
export function esperaHasta(cola: readonly { cancion: { duracion_seg: number } }[], indice: number, restante: number): number {
  let total = restante;
  for (let i = 0; i < indice && i < cola.length; i++) total += cola[i]!.cancion.duracion_seg;
  return total;
}
