import { type CeldaCubo, TODOS_CUBO as T } from '@rockola/contrato';
import { describe, expect, it } from 'vitest';
import { alternar, drillDown, ejesDeTabla, indiceCeldas, KPIS, leerKpi, luzDe, miembrosVisibles, operacion, ordenEjes, rollUp, SIN_DATOS, SIN_SELECCION } from './cubo-kpi';

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

describe('RF-067: sin datos no es cero', () => {
  it('una combinación sin celda da «sin datos»; una celda con 0 da 0', () => {
    const conCero = indiceCeldas([celda('Salsa', T, T, { solicitudes: 0, lista_casa_seg: 120 })]);
    expect(leerKpi(conCero, kpi('solicitudes'), { ...SIN_SELECCION, genero: ['Salsa'] })).toBe(0);
    expect(leerKpi(conCero, kpi('solicitudes'), { ...SIN_SELECCION, genero: ['Rock'] })).toBe(SIN_DATOS);
    expect(leerKpi(conCero, kpi('votos'), { ...SIN_SELECCION, genero: ['Rock', 'Pop'] })).toBe(SIN_DATOS);
  });

  it('el tiempo de reproducción suma lo pedido y la lista de la casa, también en un dice', () => {
    expect(leerKpi(indice, kpi('reproduccion'), SIN_SELECCION)).toBe(100);
    expect(leerKpi(indice, kpi('reproduccion'), { ...SIN_SELECCION, genero: ['Salsa', 'Pop'] })).toBe(100);
    expect(kpi('reproduccion').formato(3600)).toBe('60 min');
  });

  it('el cubo completo dice de dónde salen las noches', () => {
    expect(operacion(SIN_SELECCION, 'vivo').detalle).toMatch(/también la que está abierta/);
    expect(operacion(SIN_SELECCION, 'historico').detalle).toMatch(/pipeline/);
  });
});

describe('RF-067: qué géneros dibuja el cubo', () => {
  const generos = ['Urbano', 'Salsa', 'Pop', 'Rock', 'Tropical', 'Vallenato', 'Sin clasificar'];
  // Vallenato es el de menos actividad: sale de la vista; los demás conservan el orden de la API.
  const actividad: Record<string, number> = { Urbano: 9, Salsa: 8, Pop: 7, Rock: 6, Tropical: 5, Vallenato: 1, 'Sin clasificar': 4 };
  const datos = {
    miembros: { genero: generos, franja: ['Antes de las 9', '9 a 11 p. m.'], tercero: ['Jueves'] },
    celdas: generos.map((g) => celda(g, T, T, { solicitudes: actividad[g]! })),
  };

  it('sin filtro, los cinco con más actividad', () => {
    expect(miembrosVisibles(datos, SIN_SELECCION, false).genero).toEqual(['Urbano', 'Salsa', 'Pop', 'Rock', 'Tropical']);
  });

  it('se pueden ver todos', () => {
    expect(miembrosVisibles(datos, SIN_SELECCION, true).genero).toEqual(generos);
  });

  it('con filtro, solo lo elegido, aunque esté fuera de los cinco', () => {
    const v = miembrosVisibles(datos, { ...SIN_SELECCION, genero: ['Vallenato', 'Urbano'], franja: ['9 a 11 p. m.'] }, false);
    expect(v.genero).toEqual(['Urbano', 'Vallenato']);
    expect(v.franja).toEqual(['9 a 11 p. m.']);
    expect(v.tercero).toEqual(['Jueves']);
  });
});
