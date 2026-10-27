import { describe, expect, it } from 'vitest';
import { formatoDuracion, formatoHace, formatoHora, formatoHoraDelDia, formatoResumenCola, resumenHorario } from './formato';

const normal = (s: string) => s.replace(/[  ]/g, ' ');

describe('formato es-CO', () => {
  it('horas de 12 h como en los prototipos', () => {
    expect(normal(formatoHora('2026-10-22T20:00:00-05:00'))).toBe('8:00 p. m.');
    expect(normal(formatoHora('2026-10-23T02:00:00-05:00'))).toBe('2:00 a. m.');
    expect(normal(formatoHora('2026-10-22T21:40:00-05:00'))).toBe('9:40 p. m.');
  });

  it('«p. m.» no se separa de la hora al partir la línea', () => {
    expect(formatoHora('2026-10-22T20:00:00-05:00')).toBe('8:00 p. m.');
  });

  it('horas del día desde HH:MM', () => {
    expect(normal(formatoHoraDelDia('20:00'))).toBe('8:00 p. m.');
    expect(normal(formatoHoraDelDia('02:00'))).toBe('2:00 a. m.');
    expect(normal(formatoHoraDelDia('00:30'))).toBe('12:30 a. m.');
    expect(normal(formatoHoraDelDia('12:00'))).toBe('12:00 p. m.');
  });

  it('duraciones m:ss y h:mm:ss', () => {
    expect(formatoDuracion(267)).toBe('4:27');
    expect(formatoDuracion(4360)).toBe('1:12:40');
    expect(formatoDuracion(5)).toBe('0:05');
  });

  it('resumen de la cola en singular y vacía', () => {
    expect(formatoResumenCola(1, 200)).toBe('1 canción, unos 3 min');
    expect(formatoResumenCola(0, 0)).toBe('Sin canciones en la cola');
  });

  it('antigüedad', () => {
    expect(formatoHace('2026-10-22T22:14:00-05:00', Date.parse('2026-10-22T22:45:00-05:00'))).toBe('hace 31 min');
    expect(formatoHace('2026-10-22T22:44:40-05:00', Date.parse('2026-10-22T22:45:00-05:00'))).toBe('hace un momento');
  });
});

describe('resumen del horario', () => {
  const f = (dia: number, inicio = '20:00', fin = '02:00') => ({ dia_semana: dia, hora_inicio: inicio, hora_fin: fin, activo: true });

  it('días seguidos van en rango, como en el prototipo', () => {
    const [linea] = resumenHorario([f(4), f(5), f(6)]);
    expect(linea?.dias).toBe('Jueves a sábado');
    expect(normal(linea?.horas ?? '')).toBe('8:00 p. m. a 2:00 a. m.');
  });

  it('dos días, días sueltos y horas distintas', () => {
    expect(resumenHorario([f(5), f(6)])[0]?.dias).toBe('Viernes y sábado');
    expect(resumenHorario([f(1), f(3), f(4), f(5)])[0]?.dias).toBe('Lunes y miércoles a viernes');
    const dos = resumenHorario([f(5), f(6), f(7, '15:00', '22:00')]);
    expect(dos.map((l) => l.dias)).toEqual(['Viernes y sábado', 'Domingo']);
  });

  it('sin franjas activas no hay líneas', () => {
    expect(resumenHorario([{ ...f(1), activo: false }])).toEqual([]);
  });
});

describe('espera corta de las listas', () => {
  it('minutos enteros, sin «unos»', async () => {
    const { formatoEsperaCorta } = await import('./formato');
    expect(formatoEsperaCorta(197)).toBe('en 3 min');
    expect(formatoEsperaCorta(30)).toBe('en menos de 1 min');
    expect(formatoEsperaCorta(8000)).toBe('en más de 2 h');
  });
});
