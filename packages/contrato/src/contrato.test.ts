import { describe, expect, it } from 'vitest';
import { Bar } from './bar.ts';
import { endpoints, rutaDe } from './endpoints.ts';
import { CambioEstablecimiento } from './panel.ts';

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

  it('solo una pantalla vinculada reporta la reproducción (RF-058)', () => {
    const conPantalla = Object.entries(endpoints)
      .filter(([, e]) => 'pantalla' in e && e.pantalla)
      .map(([n]) => n);
    expect(conPantalla.sort()).toEqual(['reportarError', 'reportarFin', 'reportarInicio']);
  });

  it('pedir y votar son idempotentes (RNF-008)', () => {
    expect(endpoints.pedir.idempotente).toBe(true);
    expect(endpoints.votar.idempotente).toBe(true);
  });

  it('cada endpoint cita al menos un requisito', () => {
    for (const [nombre, e] of Object.entries(endpoints)) expect(e.rf.length, nombre).toBeGreaterThan(0);
  });
});

describe('el tema del bar (RF-059)', () => {
  const base = { slug: 'demo', nombre: 'Bar', zona_horaria: 'America/Bogota', pedidos_maximos: 3, duracion_max_seg: 600, ventana_no_repeticion_min: 60, horario: [] };

  it('sin dato del backend, el bar se ve en Rockola', () => {
    expect(Bar.parse(base).tema).toBe('rockola');
  });

  it('solo acepta los tres diseños', () => {
    expect(Bar.parse({ ...base, tema: 'afiche' }).tema).toBe('afiche');
    expect(Bar.safeParse({ ...base, tema: 'neon' }).success).toBe(false);
  });

  it('un cambio sin tema no lo toca', () => {
    expect(CambioEstablecimiento.parse({ nombre: 'Otro' })).not.toHaveProperty('tema');
    expect(CambioEstablecimiento.parse({ tema: 'vidrio' })).toEqual({ tema: 'vidrio' });
  });
});
