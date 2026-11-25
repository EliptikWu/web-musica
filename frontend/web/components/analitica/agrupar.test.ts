import { describe, expect, it } from 'vitest';
import { contarPorCausa } from './agrupar';

const CAUSAS = { omitida: 'Omitida', bloqueada: 'Bloqueada', fuera_de_tiempo: 'No alcanzó', retirada: 'Retirada', no_disponible: 'No disponible' } as const;

describe('«Por qué no sonaron» (punto 19)', () => {
  it('cuenta por causa, la más frecuente primero', () => {
    const filas = contarPorCausa([{ causa: 'bloqueada' }, { causa: 'fuera_de_tiempo' }, { causa: 'fuera_de_tiempo' }], CAUSAS);
    expect(filas.map((f) => [f.clave, f.valor])).toEqual([['fuera_de_tiempo', 2], ['bloqueada', 1]]);
    expect(filas[0]!.etiqueta).toBe(CAUSAS.fuera_de_tiempo);
  });
  it('sin canciones, sin filas', () => {
    expect(contarPorCausa([], CAUSAS)).toEqual([]);
  });
});
