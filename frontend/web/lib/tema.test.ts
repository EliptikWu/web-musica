import { describe, expect, it } from 'vitest';
import { SCRIPT_TEMA_PREVIO } from './tema';

/** Corre el script previo (ADR-26) con una URL y un `localStorage` de mentira. */
function correr(ruta: string, guardado: Record<string, string>): string | undefined {
  const dataset: Record<string, string> = {};
  const entorno = {
    location: { pathname: ruta },
    localStorage: { getItem: (k: string) => guardado[k] ?? null },
    document: { documentElement: { dataset } },
  };
  new Function('location', 'localStorage', 'document', SCRIPT_TEMA_PREVIO)(entorno.location, entorno.localStorage, entorno.document);
  return dataset.tema;
}

describe('el script previo del diseño (ADR-26)', () => {
  it('pone el diseño guardado del bar de la URL', () => {
    expect(correr('/b/demo', { 'rockola.tema.demo': 'afiche' })).toBe('afiche');
    expect(correr('/b/el-bar/pantalla', { 'rockola.tema.el-bar': 'vidrio' })).toBe('vidrio');
  });

  it('no pone nada fuera del bar, con un tema que no existe o sin dato', () => {
    expect(correr('/plataforma', { 'rockola.tema-panel': 'afiche' })).toBeUndefined();
    expect(correr('/b/demo', { 'rockola.tema.demo': 'neon' })).toBeUndefined();
    expect(correr('/b/Demo', { 'rockola.tema.Demo': 'afiche' })).toBeUndefined();
    expect(correr('/b/demo', {})).toBeUndefined();
  });

  it('en el panel con sesión pone el diseño de su bar (D19)', () => {
    expect(correr('/panel', { 'rockola.tema-panel': 'afiche' })).toBe('afiche');
    expect(correr('/panel/analitica/cubo', { 'rockola.tema-panel': 'vidrio' })).toBe('vidrio');
    expect(correr('/panel', { 'rockola.tema-panel': 'neon' })).toBeUndefined();
  });

  it('entrar y cambiar la clave siguen en Rockola (D19)', () => {
    expect(correr('/panel/entrar', { 'rockola.tema-panel': 'afiche' })).toBeUndefined();
    expect(correr('/panel/cambiar-clave', { 'rockola.tema-panel': 'afiche' })).toBeUndefined();
  });
});
