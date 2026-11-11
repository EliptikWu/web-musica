import { describe, expect, it } from 'vitest';
import { claveDe, nuevaClave } from './idempotencia';

// La forma que acepta el backend: UUID canónico en minúsculas (doc 04, «Reintentos»).
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

describe('claves de idempotencia', () => {
  it('una clave nueva es un UUID y cambia cada vez', () => {
    expect(nuevaClave()).toMatch(UUID);
    expect(nuevaClave()).not.toBe(nuevaClave());
  });

  it('claveDe da siempre el mismo UUID v8 para el mismo texto', () => {
    const clave = claveDe('42-salto');
    expect(clave).toMatch(UUID);
    expect(clave[14]).toBe('8');
    expect(claveDe('42-salto')).toBe(clave);
  });

  it('claveDe distingue operaciones y reproducciones', () => {
    const claves = new Set(['42-salto', '43-salto', '42-reportarInicio', '42-reportarFin', '42-reportarError'].map(claveDe));
    expect(claves.size).toBe(5);
  });
});
