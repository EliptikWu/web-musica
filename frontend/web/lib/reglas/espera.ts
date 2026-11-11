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

/**
 * Segundos que sonaron desde la foto. La espera de cada canción (`espera_seg`, RF-018) la
 * calcula el backend en `servidor_ahora`; entre una foto y la siguiente baja lo mismo que
 * avanza lo que suena (nada si está en pausa).
 */
export function avanceDesdeFoto(reproduccion: ReproduccionMinima | null, servidorAhoraIso: string, ahoraServidorMs: number): number {
  if (!reproduccion) return 0;
  const enLaFoto = Math.min(reproduccion.posicion_seg, reproduccion.cancion.duracion_seg);
  return Math.max(0, posicionActual(reproduccion, servidorAhoraIso, ahoraServidorMs) - enLaFoto);
}

/** Espera estimada de una canción ahora mismo. Es una estimación y la interfaz lo dice («unos»). */
export function esperaDe(item: { espera_seg: number }, avance: number): number {
  return Math.max(0, item.espera_seg - avance);
}
