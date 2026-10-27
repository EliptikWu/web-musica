import { describe, expect, it } from 'vitest';
import { construirCsp, esRutaPantalla } from './csp';

describe('construirCsp', () => {
  it('en producción no deja estilos ni scripts sin nonce', () => {
    const csp = construirCsp({ nonce: 'abc', desarrollo: false, https: true, pantalla: false });
    expect(csp).toContain("script-src 'self' 'nonce-abc' 'strict-dynamic'");
    expect(csp).toContain("style-src 'self' 'nonce-abc'");
    expect(csp).not.toContain('unsafe-inline');
    expect(csp).not.toContain('unsafe-eval');
    expect(csp).toContain("frame-src 'none'");
    expect(csp).toContain('upgrade-insecure-requests');
  });

  it('solo la pantalla del bar puede embeber YouTube', () => {
    expect(construirCsp({ nonce: 'n', desarrollo: false, https: false, pantalla: true })).toContain('frame-src https://www.youtube-nocookie.com');
  });

  it('permite el Service Worker de la simulación', () => {
    expect(construirCsp({ nonce: 'n', desarrollo: true, https: false, pantalla: false })).toContain("worker-src 'self'");
  });
});

describe('esRutaPantalla', () => {
  it('reconoce la ruta de la TV y nada más', () => {
    expect(esRutaPantalla('/b/demo/pantalla')).toBe(true);
    expect(esRutaPantalla('/b/demo/pantalla/')).toBe(true);
    expect(esRutaPantalla('/b/demo/cola')).toBe(false);
    expect(esRutaPantalla('/b/demo/pantalla/otra')).toBe(false);
  });
});
