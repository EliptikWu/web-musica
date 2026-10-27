import { describe, expect, it } from 'vitest';
import { endpoints, rutaDe } from './endpoints.ts';

describe('el contrato', () => {
  it('arma rutas con sus parámetros y escapa los valores', () => {
    expect(rutaDe('/bares/{slug}/solicitudes/{id}/votos', { slug: 'demo', id: 's 1' })).toBe('/bares/demo/solicitudes/s%201/votos');
    expect(() => rutaDe('/bares/{slug}', {})).toThrow(/slug/);
  });

  it('cada operación tiene un método y una ruta únicos', () => {
    const vistas = new Set<string>();
    for (const e of Object.values(endpoints)) {
      const clave = `${e.metodo} ${e.ruta}`;
      expect(vistas.has(clave), clave).toBe(false);
      vistas.add(clave);
    }
  });

  it('todo lo del panel pide rol, salvo entrar y salir', () => {
    const sinRol = Object.entries(endpoints)
      .filter(([, e]) => e.superficie === 'panel' && !('rol' in e))
      .map(([n]) => n);
    expect(sinRol.sort()).toEqual(['entrar', 'salir']);
  });

  it('pedir y votar son idempotentes (RNF-008)', () => {
    expect(endpoints.pedir.idempotente).toBe(true);
    expect(endpoints.votar.idempotente).toBe(true);
  });

  it('cada endpoint cita al menos un requisito', () => {
    for (const [nombre, e] of Object.entries(endpoints)) expect(e.rf.length, nombre).toBeGreaterThan(0);
  });
});
