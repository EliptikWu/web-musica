import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { limitar } from './limitador';

describe('limitar (ADR-33)', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('el primer aviso dispara en el acto', () => {
    const fn = vi.fn();
    limitar(fn, 1500).avisar();
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it('una ráfaga dispara dos veces: al empezar y al cerrar la ventana', () => {
    const fn = vi.fn();
    const l = limitar(fn, 1500);
    for (let i = 0; i < 5; i++) {
      l.avisar();
      vi.advanceTimersByTime(100);
    }
    expect(fn).toHaveBeenCalledTimes(1);
    vi.advanceTimersByTime(1500);
    expect(fn).toHaveBeenCalledTimes(2);
    vi.advanceTimersByTime(5000);
    expect(fn).toHaveBeenCalledTimes(2);
  });

  it('pasada la ventana sin avisos, el siguiente vuelve a disparar en el acto', () => {
    const fn = vi.fn();
    const l = limitar(fn, 1500);
    l.avisar();
    vi.advanceTimersByTime(1600);
    l.avisar();
    expect(fn).toHaveBeenCalledTimes(2);
  });

  it('cerrar descarta el disparo pendiente', () => {
    const fn = vi.fn();
    const l = limitar(fn, 1500);
    l.avisar();
    l.avisar();
    l.cerrar();
    vi.advanceTimersByTime(3000);
    expect(fn).toHaveBeenCalledTimes(1);
  });
});
