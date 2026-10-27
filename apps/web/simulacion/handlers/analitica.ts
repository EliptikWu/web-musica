import type { UmbralesAnalitica } from '@rockola/contrato';
import { comparar, diagnostico, type Filtro, nochesDeEjemplo, origenDatos, participacion, recomendaciones, resumen, seleccionar, UMBRALES_POR_DEFECTO } from '../analitica';
import { anotar } from '../gestion';
import { error, manejar, ok, type Peticion } from './comun';

/** El tablero analítico (RF-049 a RF-054) con datos de ejemplo rotulados (RNF-015). */

const FECHA = /^\d{4}-\d{2}-\d{2}$/;

function filtroDe(p: Peticion): Filtro | null {
  const f = { noche: p.consulta.get('noche'), desde: p.consulta.get('desde'), hasta: p.consulta.get('hasta'), dia_semana: p.consulta.get('dia_semana') };
  if ((f.desde && !FECHA.test(f.desde)) || (f.hasta && !FECHA.test(f.hasta))) return null;
  if (f.dia_semana && !/^[1-7]$/.test(f.dia_semana)) return null;
  return f;
}

/** Valida el filtro, elige las noches y pasa a `fn`; 400 o 404 si no se puede. */
function conNoches(p: Peticion, fn: (todas: ReturnType<typeof nochesDeEjemplo>, elegidas: ReturnType<typeof nochesDeEjemplo>) => unknown) {
  const filtro = filtroDe(p);
  if (!filtro) return error(400, 'datos_invalidos', 'Fechas en formato AAAA-MM-DD y día de 1 a 7');
  const todas = nochesDeEjemplo(p.estado);
  const elegidas = seleccionar(todas, filtro);
  if (!elegidas) return error(404, 'no_encontrado', 'Esa noche no está en el tablero');
  return ok(fn(todas, elegidas));
}

const umbrales = (p: Peticion) => p.estado.umbrales_analitica ?? UMBRALES_POR_DEFECTO;

export const manejadoresAnalitica = [
  manejar('nochesAnalitica', ({ estado }) => {
    const todas = nochesDeEjemplo(estado);
    return ok({ datos: origenDatos(todas, todas), noches: todas.map((n) => n.noche) });
  }),
  manejar('resumenAnalitica', (p) => conNoches(p, resumen)),
  manejar('participacionAnalitica', (p) => conNoches(p, participacion)),
  manejar('diagnosticoAnalitica', (p) => conNoches(p, (todas, elegidas) => diagnostico(todas, elegidas, umbrales(p)))),
  manejar('umbralesAnalitica', (p) => ok(umbrales(p))),
  manejar('guardarUmbrales', (p) => {
    const u = p.cuerpo as UmbralesAnalitica;
    p.estado.umbrales_analitica = u;
    const detalle = `${u.votos_minimos} votos para «muy pedida», ${u.concentracion_pct} % de concentración y ${u.participacion_minima} teléfonos por noche`;
    anotar(p.estado, p.ahora, { id: p.personal!.id, nombre: p.personal!.nombre }, 'editar_umbrales', null, detalle);
    return ok(u);
  }),
  manejar('compararAnalitica', (p) => {
    const agrupar = p.consulta.get('agrupar');
    if (agrupar !== 'noche' && agrupar !== 'dia' && agrupar !== 'evento') return error(400, 'datos_invalidos', 'Agrupa por noche, día o evento');
    const filtro = filtroDe(p);
    if (!filtro) return error(400, 'datos_invalidos', 'Fechas en formato AAAA-MM-DD');
    const todas = nochesDeEjemplo(p.estado);
    const ids = (p.consulta.get('noches') ?? '').split(',').filter(Boolean);
    const elegidas = agrupar === 'noche' && ids.length > 0 ? todas.filter((n) => ids.includes(n.noche.id)) : seleccionar(todas, { desde: filtro.desde, hasta: filtro.hasta })!;
    return ok(comparar(todas, elegidas, agrupar));
  }),
  manejar('recomendacionesAnalitica', (p) =>
    conNoches(p, (todas, elegidas) => ({ datos: origenDatos(todas, elegidas), recomendaciones: recomendaciones(p.estado, elegidas, p.ahora) })),
  ),
];
