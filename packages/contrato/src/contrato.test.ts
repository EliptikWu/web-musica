import { describe, expect, it } from 'vitest';
import { Bar } from './bar.ts';
import { endpoints, rutaDe } from './endpoints.ts';
import { readFileSync } from 'node:fs';
import { CambioEstablecimiento, PaginaBitacora, TipoAccion } from './panel.ts';

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

describe('la bitácora (RF-044, RF-068)', () => {
  const accion = (tipo: string, usuario: { id: string; nombre: string } | null, detalle: string) => ({
    id: '1', fecha: '2026-11-24T23:10:00-05:00', usuario, tipo,
    objetivo: { tipo: 'cancion', nombre: 'Rebelión', detalle: 'Joe Arroyo' }, detalle,
  });

  it('acepta el cambio de género, a mano o de la clasificación automática', () => {
    const pagina = {
      acciones: [accion('editar_categoria', { id: '7', nombre: 'Ana' }, 'Asignó género: Salsa'), accion('editar_categoria', null, 'Clasificación ETIQUETAS: Salsa'), accion('retirar', { id: '7', nombre: 'Ana' }, 'otro')],
      siguiente: null,
    };
    expect(PaginaBitacora.safeParse(pagina).success).toBe(true);
  });

  it('conoce los mismos tipos de acción que el backend devuelve', () => {
    // Si el backend anota un tipo que el contrato no conoce, la página entera falla la validación.
    const kotlin = readFileSync(new URL('../../../rokolaBackend/src/main/kotlin/com/proyectoN/rokolaBackend/compat/ContratoBitacora.kt', import.meta.url), 'utf8');
    const bloque = /TIPOS_ACCION = setOf\(([\s\S]*?)\)/.exec(kotlin)?.[1] ?? '';
    const delBackend = [...bloque.matchAll(/"([a-z_]+)"/g)].map((m) => m[1]).sort();
    expect(delBackend.length).toBeGreaterThan(0);
    expect(delBackend).toEqual([...TipoAccion.options].sort());
  });
});
