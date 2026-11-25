import type { CausaNoSono } from '@rockola/contrato';
import type { Fila } from './Graficos';

/**
 * «Por qué no sonaron» (punto 19): cuántas canciones muy votadas se quedaron sin sonar por cada
 * causa, de la más frecuente a la menos. Sale de `DiagnosticoAnalitica.no_sonaron`, que antes
 * solo se veía como tabla.
 */
export function contarPorCausa(noSonaron: readonly { causa: CausaNoSono }[], nombres: Record<CausaNoSono, string>): Fila[] {
  const cuenta = new Map<CausaNoSono, number>();
  for (const s of noSonaron) cuenta.set(s.causa, (cuenta.get(s.causa) ?? 0) + 1);
  return [...cuenta]
    .map(([causa, valor]) => ({ clave: causa, etiqueta: nombres[causa], valor }))
    .sort((a, b) => b.valor - a.valor || a.etiqueta.localeCompare(b.etiqueta, 'es'));
}
