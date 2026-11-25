import type { OrigenCategoria } from '@rockola/contrato';

/** Alfabético en español (tildes y eñes en su sitio). */
export const ordenarGeneros = (generos: readonly string[]) => [...generos].sort((a, b) => a.localeCompare(b, 'es'));

/**
 * De dónde salió el género. Una canción guardada antes de que se anotara el origen trae género
 * sin origen: no es que nadie se lo haya puesto (punto 18).
 */
export function textoOrigen(categoria: string | null, origen: OrigenCategoria | null | undefined): string {
  if (origen) return `${ORIGEN[origen]}.`;
  return categoria ? 'Automático, de antes.' : 'Nadie le ha puesto género.';
}

const ORIGEN: Record<NonNullable<OrigenCategoria>, string> = {
  MANUAL: 'Puesto a mano',
  ETIQUETAS: 'Automático, por las etiquetas del video',
  TITULO: 'Automático, por el título',
  DESCRIPCION: 'Automático, por la descripción',
};

export type TotalReclasificacion = { clasificadas: number; sinPistas: number; noDisponibles: number; interrumpida: 'cuota_agotada' | 'youtube_no_disponible' | null };

/** El resultado de «Clasificar las que faltan» (RF-068), para el aviso de «Canciones». */
export function resumenReclasificacion(t: TotalReclasificacion): { tipo: 'luz' | 'error'; titulo: string; meta?: string } {
  const titulo =
    t.clasificadas === 0 ? 'No se encontró el género de ninguna.' : t.clasificadas === 1 ? 'Se encontró el género de 1 canción.' : `Se encontró el género de ${t.clasificadas} canciones.`;
  const partes = [
    t.sinPistas > 0 ? `${t.sinPistas === 1 ? '1 no tiene' : `${t.sinPistas} no tienen`} pistas en YouTube: ponles el género a mano.` : null,
    t.noDisponibles > 0 ? `${t.noDisponibles === 1 ? '1 ya no está' : `${t.noDisponibles} ya no están`} en YouTube.` : null,
    t.interrumpida === 'cuota_agotada' ? 'Se acabó la cuota de YouTube por hoy: vuelve a intentarlo mañana.' : null,
    t.interrumpida === 'youtube_no_disponible' ? 'YouTube no respondió: vuelve a intentarlo en un rato.' : null,
  ].filter(Boolean);
  return { tipo: t.interrumpida && t.clasificadas === 0 ? 'error' : 'luz', titulo, meta: partes.length > 0 ? partes.join(' ') : undefined };
}
