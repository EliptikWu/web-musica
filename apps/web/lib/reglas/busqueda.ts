import type { ResultadoBusqueda } from '@rockola/contrato';

/**
 * Filtros de la búsqueda (RF-055, PB-51): género, duración y popularidad, uno o varios a la
 * vez. Filtran los resultados que ya llegaron, en el teléfono: el cambio es inmediato y no
 * gasta cuota de YouTube. Quitarlos devuelve la lista completa.
 */
export type FiltrosBusqueda = {
  /** `''` todos; `'sin'` sin clasificar; si no, el género. */
  genero: string;
  duracion: '' | 'corta' | 'media' | 'larga';
  popularidad: '' | 'pedidas' | 'muy_pedidas';
};

export const SIN_FILTROS: FiltrosBusqueda = { genero: '', duracion: '', popularidad: '' };

/** Límites en segundos, con el de arriba incluido: hasta 4:00, de 4:01 a 6:00, más de 6:00. */
const DURACIONES = { corta: [0, 240], media: [241, 360], larga: [361, Infinity] } as const;

/** Desde cuántos pedidos en el bar una canción cuenta como «muy pedida» (*propuesto*). */
export const MUY_PEDIDA = 5;

export function cuantosFiltros(f: FiltrosBusqueda): number {
  return [f.genero, f.duracion, f.popularidad].filter(Boolean).length;
}

export function filtrarResultados(resultados: readonly ResultadoBusqueda[], f: FiltrosBusqueda): ResultadoBusqueda[] {
  const salida = resultados.filter((r) => {
    if (f.genero === 'sin' && r.genero !== null) return false;
    if (f.genero && f.genero !== 'sin' && r.genero !== f.genero) return false;
    if (f.duracion) {
      const [desde, hasta] = DURACIONES[f.duracion];
      if (r.cancion.duracion_seg < desde || r.cancion.duracion_seg > hasta) return false;
    }
    if (f.popularidad === 'pedidas' && r.pedidas_en_el_bar < 1) return false;
    if (f.popularidad === 'muy_pedidas' && r.pedidas_en_el_bar < MUY_PEDIDA) return false;
    return true;
  });
  // Con la popularidad elegida, las más pedidas primero (el orden es estable).
  return f.popularidad ? salida.sort((a, b) => b.pedidas_en_el_bar - a.pedidas_en_el_bar) : salida;
}

/** Los géneros presentes en los resultados, por nombre; «sin clasificar» (`null`) al final. */
export function generosDe(resultados: readonly ResultadoBusqueda[]): (string | null)[] {
  const conGenero = [...new Set(resultados.map((r) => r.genero).filter((g): g is string => g !== null))].sort((a, b) => a.localeCompare(b, 'es'));
  return resultados.some((r) => r.genero === null) ? [...conGenero, null] : conGenero;
}
