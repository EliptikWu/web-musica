import { type CeldaCubo, TODOS_CUBO as T } from '@rockola/contrato';
import { describe, expect, it } from 'vitest';
import { alternar, drillDown, ejesDeTabla, indiceCeldas, KPIS, leerKpi, luzDe, operacion, ordenEjes, rollUp, SIN_SELECCION } from './cubo-kpi';

const celda = (genero: string, franja: string, tercero: string, x: Partial<CeldaCubo>): CeldaCubo => ({
  genero, franja, tercero, solicitudes: 0, votos: 0, telefonos: 0, no_sonaron: 0, espera_promedio_seg: null, espera_maxima_seg: null,
  pedidos_seg: 0, lista_casa_seg: 0, popular: null, ...x,
});
const kpi = (clave: string) => KPIS.find((k) => k.clave === clave)!;

// Un mismo teléfono participa en Salsa y en Pop: el total de únicos es 3, no 2 + 2.
const indice = indiceCeldas([
  celda(T, T, T, { solicitudes: 5, telefonos: 3, espera_promedio_seg: 100, espera_maxima_seg: 300, pedidos_seg: 75, lista_casa_seg: 25 }),
  celda('Salsa', T, T, { solicitudes: 3, telefonos: 2, espera_promedio_seg: 60, espera_maxima_seg: 120, pedidos_seg: 50, lista_casa_seg: 0 }),
  celda('Pop', T, T, { solicitudes: 2, telefonos: 2, espera_promedio_seg: 160, espera_maxima_seg: 300, pedidos_seg: 25, lista_casa_seg: 25 }),
]);

describe('leerKpi', () => {
  it('lee los únicos de la celda ya calculada, sin sumar', () => {
    expect(leerKpi(indice, kpi('telefonos'), SIN_SELECCION)).toBe(3);
  });
  it('en un dice suma lo aditivo', () => {
    expect(leerKpi(indice, kpi('solicitudes'), { ...SIN_SELECCION, genero: ['Salsa', 'Pop'] })).toBe(5);
  });
  it('en un dice no inventa usuarios únicos ni promedios', () => {
    const dice = { ...SIN_SELECCION, genero: ['Salsa', 'Pop'] };
    expect(leerKpi(indice, kpi('telefonos'), dice)).toBeNull();
    expect(leerKpi(indice, kpi('espera_promedio'), dice)).toBeNull();
  });
  it('en un dice la máxima es la mayor y el cumplimiento sale de los segundos', () => {
    const dice = { ...SIN_SELECCION, genero: ['Salsa', 'Pop'] };
    expect(leerKpi(indice, kpi('espera_maxima'), dice)).toBe(300);
    expect(leerKpi(indice, kpi('lista_casa'), dice)).toBe(25);
  });
});

describe('luzDe', () => {
  it('nunca cae en la franja de ámbar donde el texto no contrasta', () => {
    for (let i = 0; i <= 100; i++) {
      const { brillo, encendido } = luzDe(i / 100);
      expect(encendido ? brillo >= 0.63 : brillo <= 0.4).toBe(true);
    }
  });
  it('llega al ámbar pleno en el máximo', () => {
    expect(luzDe(1)).toEqual({ brillo: 1, encendido: true });
    expect(luzDe(0)).toEqual({ brillo: 0, encendido: false });
  });
});

describe('operaciones', () => {
  const miembros = { genero: ['Salsa', 'Pop'], franja: ['A'], tercero: ['Jueves'] };
  it('nombra la operación según lo fijado', () => {
    expect(operacion(SIN_SELECCION).nombre).toBe('Cubo completo');
    expect(operacion({ ...SIN_SELECCION, genero: ['Salsa'] }).nombre).toBe('Slice');
    expect(operacion({ ...SIN_SELECCION, genero: ['Salsa', 'Pop'] }).nombre).toBe('Dice');
    expect(operacion({ genero: ['Salsa'], franja: ['A'], tercero: ['Jueves'] }).nombre).toBe('Celda');
  });
  it('drill-down y roll-up son inversos', () => {
    const abajo = drillDown(SIN_SELECCION, miembros);
    expect(abajo.genero).toEqual(['Salsa']);
    expect(rollUp(abajo)).toEqual(SIN_SELECCION);
  });
  it('roll-up deshace el drill-down aunque ya hubiera otro eje fijo', () => {
    const inicio = { ...SIN_SELECCION, franja: ['A'] };
    const orden = ordenEjes(['franja'], drillDown(inicio, miembros));
    expect(orden).toEqual(['franja', 'genero']);
    expect(rollUp(drillDown(inicio, miembros), orden)).toEqual(inicio);
  });
  it('alternar agrega y quita', () => {
    const uno = alternar(SIN_SELECCION, 'franja', 'A');
    expect(uno.franja).toEqual(['A']);
    expect(alternar(uno, 'franja', 'A').franja).toEqual([]);
  });
  it('pivot intercambia filas y columnas y deja lo fijo aparte', () => {
    const s = { ...SIN_SELECCION, genero: ['Salsa'] };
    expect(ejesDeTabla(s, false)).toEqual({ filas: 'franja', columnas: 'tercero', resto: 'genero' });
    expect(ejesDeTabla(s, true)).toEqual({ filas: 'tercero', columnas: 'franja', resto: 'genero' });
  });
});
