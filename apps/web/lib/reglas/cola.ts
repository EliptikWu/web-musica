/**
 * Reglas de la cola que la interfaz necesita repetir: el orden lo decide el servidor, pero el
 * voto optimista reordena antes de que llegue su respuesta (RF-012), y el orden tiene que
 * ser el mismo (RN-05).
 */

type Ordenable = { votos_total: number; fecha_solicitud: string };

/** RN-05: más votos primero; en empate, la que se pidió antes. Devuelve una copia. */
export function ordenarCola<T extends Ordenable>(cola: readonly T[]): T[] {
  return [...cola].sort(
    (a, b) => b.votos_total - a.votos_total || Date.parse(a.fecha_solicitud) - Date.parse(b.fecha_solicitud),
  );
}

/** Reasigna `puesto` según el orden actual y guarda el anterior para «Subió N puestos». */
export function renumerar<T extends Ordenable & { puesto: number; puesto_anterior: number | null }>(cola: readonly T[]): T[] {
  return ordenarCola(cola).map((item, i) => ({
    ...item,
    puesto_anterior: item.puesto !== i + 1 ? item.puesto : item.puesto_anterior,
    puesto: i + 1,
  }));
}

/** Puestos que subió en el último reordenamiento; 0 si no subió. */
export function puestosSubidos(item: { puesto: number; puesto_anterior: number | null }): number {
  return item.puesto_anterior !== null && item.puesto_anterior > item.puesto ? item.puesto_anterior - item.puesto : 0;
}

/** Duración de lo que falta por sonar en la cola, sin la canción actual. */
export function duracionCola(cola: readonly { cancion: { duracion_seg: number } }[]): number {
  return cola.reduce((total, item) => total + item.cancion.duracion_seg, 0);
}

/**
 * El caso de desempate que se explica debajo de la cola («Con los mismos votos, suena primero
 * la que se pidió antes. Por eso Monaco va antes que La Bicicleta»). Prefiere un empate en
 * el que esté la canción propia; si no, el primero de la cola.
 */
export function ejemploDesempate<T extends Ordenable & { solicitud_id: string; cancion: { titulo: string } }>(
  cola: readonly T[],
  propias: ReadonlySet<string> = new Set(),
): { antes: T; despues: T } | null {
  const pares: { antes: T; despues: T }[] = [];
  for (let i = 0; i + 1 < cola.length; i++) {
    const a = cola[i]!;
    const b = cola[i + 1]!;
    if (a.votos_total === b.votos_total) pares.push({ antes: a, despues: b });
  }
  return pares.find((p) => propias.has(p.antes.solicitud_id) || propias.has(p.despues.solicitud_id)) ?? pares[0] ?? null;
}
