import { describe, expect, it } from 'vitest';
import { estaCaida, siguienteConexion } from './estado';

describe('estado de la conexión (barrido de la iteración 1)', () => {
  it('pasar de reconectando a desconectado y vuelta no reinicia la cuenta: el banner no parpadea', () => {
    let c = siguienteConexion({ estado: 'conectado', desde: 0 }, 'reconectando', 1000);
    expect(c.desde).toBe(1000);
    c = siguienteConexion(c, 'desconectado', 20_000);
    c = siguienteConexion(c, 'reconectando', 35_000);
    expect(c.desde).toBe(1000);
    expect(estaCaida(c, 36_000)).toBe(true);
  });

  it('al volver a conectar, se cuenta desde cero', () => {
    const c = siguienteConexion({ estado: 'reconectando', desde: 1000 }, 'conectado', 9000);
    expect(c).toEqual({ estado: 'conectado', desde: 9000 });
    expect(estaCaida(c, 60_000)).toBe(false);
  });

  it('si nunca llega a conectarse, también avisa a los 5 s de cargar', () => {
    const c = siguienteConexion({ estado: 'conectando', desde: 1000 }, 'reconectando', 1500);
    expect(c.desde).toBe(1000);
    expect(estaCaida(c, 5000)).toBe(false);
    expect(estaCaida(c, 6500)).toBe(true);
  });

  it('en el servidor y en la hidratación (tic 0) nunca está caída', () => {
    expect(estaCaida({ estado: 'desconectado', desde: 0 }, 0)).toBe(false);
  });
});
