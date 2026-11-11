import { describe, expect, it } from 'vitest';
import { cupo, diaRelativo, servicioCerrado } from './cliente';

const normal = (s: string) => s.replace(/[  ]/g, ' ');

describe('textos del cliente', () => {
  it('el cupo en singular, plural y agotado', () => {
    expect(cupo(2, 3)).toBe('Te quedan 2 de 3 pedidos');
    expect(cupo(1, 3)).toBe('Te queda 1 de 3 pedidos');
    expect(cupo(0, 3)).toBe('Usaste tus 3 pedidos');
  });

  it('fuera de horario dice cuándo abren, sin doble punto', () => {
    const ahora = Date.parse('2026-10-22T18:30:00-05:00');
    const t = servicioCerrado('fuera_de_horario', '2026-10-22T20:00:00-05:00', 'America/Bogota', ahora);
    expect(normal(t.detalle)).toBe('Los pedidos abren hoy a las 8:00 p. m. Mientras tanto suena la lista de la casa.');
  });

  it('hoy, mañana o el día de la semana, en la zona del bar', () => {
    const jueves = Date.parse('2026-10-22T18:30:00-05:00');
    expect(diaRelativo('2026-10-22T20:00:00-05:00', jueves, 'America/Bogota')).toBe('hoy');
    expect(diaRelativo('2026-10-23T20:00:00-05:00', jueves, 'America/Bogota')).toBe('mañana');
    expect(diaRelativo('2026-10-29T20:00:00-05:00', Date.parse('2026-10-25T21:00:00-05:00'), 'America/Bogota')).toBe('el jueves');
  });
});
