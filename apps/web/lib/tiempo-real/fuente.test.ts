import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { EstadoConexion } from './fuente';

/** EventSource falso: cada instancia abre y, según el guion, corta o manda una foto. */
class FuenteFalsa {
  static guion: 'corta' | 'responde' = 'corta';
  static instancias = 0;
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  private oyentes = new Map<string, (m: MessageEvent<string>) => void>();
  constructor() {
    FuenteFalsa.instancias++;
    setTimeout(() => {
      this.onopen?.();
      if (FuenteFalsa.guion === 'corta') this.onerror?.();
      else this.oyentes.get('ping')?.(new MessageEvent('ping', { data: JSON.stringify({ tipo: 'ping', servidor_ahora: '2026-10-22T22:45:00-05:00' }) }));
    }, 10);
  }
  addEventListener(tipo: string, fn: (m: MessageEvent<string>) => void) {
    this.oyentes.set(tipo, fn);
  }
  close() {}
}

describe('canal de tiempo real (barrido de la iteración 1)', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.stubGlobal('EventSource', FuenteFalsa);
    FuenteFalsa.instancias = 0;
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('un servidor que acepta y corta enseguida nunca cuenta como conectado y acaba en «desconectado»', async () => {
    FuenteFalsa.guion = 'corta';
    const { conectarSse } = await import('./fuente');
    const estados: EstadoConexion[] = [];
    const cerrar = conectarSse('demo', () => {}, (e) => estados.push(e));
    await vi.advanceTimersByTimeAsync(60_000);
    cerrar();
    expect(estados).not.toContain('conectado');
    expect(estados.at(-1)).toBe('desconectado');
    expect(FuenteFalsa.instancias).toBeGreaterThan(4);
  });

  it('con datos llegando, queda conectado', async () => {
    FuenteFalsa.guion = 'responde';
    const { conectarSse } = await import('./fuente');
    const estados: EstadoConexion[] = [];
    const eventos: string[] = [];
    const cerrar = conectarSse('demo', (e) => eventos.push(e.tipo), (e) => estados.push(e));
    await vi.advanceTimersByTimeAsync(100);
    cerrar();
    expect(estados).toEqual(['conectando', 'conectado']);
    expect(eventos).toEqual(['ping']);
  });
});
