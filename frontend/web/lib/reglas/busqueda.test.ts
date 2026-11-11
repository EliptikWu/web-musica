import type { ResultadoBusqueda } from '@rockola/contrato';
import { describe, expect, it } from 'vitest';
import { cuantosFiltros, filtrarResultados, generosDe, SIN_FILTROS } from './busqueda';

const r = (id: string, duracion: number, genero: string | null, pedidas: number): ResultadoBusqueda => ({
  cancion: { video_id: id, titulo: id, artista: 'x', duracion_seg: duracion, miniatura_url: null },
  pedible: true,
  motivo: null,
  disponible_desde: null,
  genero,
  pedidas_en_el_bar: pedidas,
});

const lista = [r('a', 200, 'salsa', 0), r('b', 300, 'urbano', 7), r('c', 420, null, 2), r('d', 239, 'urbano', 12)];
const ids = (xs: ResultadoBusqueda[]) => xs.map((x) => x.cancion.video_id);

describe('filtros de la búsqueda (RF-055)', () => {
  it('sin filtros devuelve la lista completa, en su orden', () => {
    expect(ids(filtrarResultados(lista, SIN_FILTROS))).toEqual(['a', 'b', 'c', 'd']);
    expect(cuantosFiltros(SIN_FILTROS)).toBe(0);
  });

  it('combina género, duración y popularidad', () => {
    expect(ids(filtrarResultados(lista, { ...SIN_FILTROS, genero: 'urbano' }))).toEqual(['b', 'd']);
    expect(ids(filtrarResultados(lista, { ...SIN_FILTROS, genero: 'sin' }))).toEqual(['c']);
    expect(ids(filtrarResultados(lista, { ...SIN_FILTROS, duracion: 'corta' }))).toEqual(['a', 'd']);
    expect(ids(filtrarResultados(lista, { ...SIN_FILTROS, duracion: 'larga' }))).toEqual(['c']);
    expect(ids(filtrarResultados(lista, { genero: 'urbano', duracion: 'corta', popularidad: '' }))).toEqual(['d']);
    expect(cuantosFiltros({ genero: 'urbano', duracion: 'corta', popularidad: '' })).toBe(2);
  });

  it('los bordes de los tramos: 4:00 es «hasta 4», 6:00 es «de 4 a 6»', () => {
    const bordes = [r('cuatro', 240, null, 0), r('seis', 360, null, 0), r('seis-y-uno', 361, null, 0)];
    expect(ids(filtrarResultados(bordes, { ...SIN_FILTROS, duracion: 'corta' }))).toEqual(['cuatro']);
    expect(ids(filtrarResultados(bordes, { ...SIN_FILTROS, duracion: 'media' }))).toEqual(['seis']);
    expect(ids(filtrarResultados(bordes, { ...SIN_FILTROS, duracion: 'larga' }))).toEqual(['seis-y-uno']);
  });

  it('por popularidad, las más pedidas en el bar primero', () => {
    expect(ids(filtrarResultados(lista, { ...SIN_FILTROS, popularidad: 'pedidas' }))).toEqual(['d', 'b', 'c']);
    expect(ids(filtrarResultados(lista, { ...SIN_FILTROS, popularidad: 'muy_pedidas' }))).toEqual(['d', 'b']);
  });

  it('los géneros presentes, con «sin clasificar» al final', () => {
    expect(generosDe(lista)).toEqual(['salsa', 'urbano', null]);
  });
});
