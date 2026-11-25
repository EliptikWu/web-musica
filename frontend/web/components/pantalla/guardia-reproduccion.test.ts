import { describe, expect, it } from 'vitest';
import {
  type AccionGuardia, CONFIRMAR_AUDIO_MS, decidirAudio, decidirGuardia, ENTRE_INTENTOS_MS, ESTABLE_MS, GRACIA_MS, GUARDIA_INICIAL, type Guardia, PRIMERA_PRUEBA_MS,
  PRUEBA_MAXIMA_MS, pruebaInicial, pruebaYa, YT_CARGANDO, YT_LISTO, YT_PAUSADO, YT_SIN_EMPEZAR, YT_SONANDO, YT_TERMINADO,
} from './guardia-reproduccion';

/** Revisa cada segundo, como el reproductor, y devuelve las acciones que no son «nada». */
function recorrer(segundos: number, estado: number | ((s: number) => number), debeSonar = true) {
  let guardia: Guardia = GUARDIA_INICIAL;
  const acciones: AccionGuardia[] = [];
  for (let s = 0; s <= segundos; s++) {
    const r = decidirGuardia(guardia, { estadoYT: typeof estado === 'function' ? estado(s) : estado, debeSonar, ahora: 10_000 + s * 1000 });
    guardia = r.guardia;
    if (r.accion !== 'nada') acciones.push(r.accion);
  }
  return acciones;
}

describe('decidirGuardia', () => {
  it('un video pausado por YouTube se reanuda solo tras la gracia', () => {
    const inicio = decidirGuardia(GUARDIA_INICIAL, { estadoYT: YT_PAUSADO, debeSonar: true, ahora: 1000 });
    expect(inicio.accion).toBe('nada');
    expect(decidirGuardia(inicio.guardia, { estadoYT: YT_PAUSADO, debeSonar: true, ahora: 1000 + GRACIA_MS }).accion).toBe('reanudar');
  });

  it('escala: reanudar dos veces, recargar y por último sonar en silencio', () => {
    expect(recorrer(3 + (ENTRE_INTENTOS_MS / 1000) * 3, YT_PAUSADO)).toEqual(['reanudar', 'reanudar', 'recargar', 'silenciar']);
  });

  it('sigue escalando aunque cada intento pase un momento por «cargando»', () => {
    // Como un navegador que bloquea: reanudar → cargando → pausado otra vez.
    const acciones = recorrer(20, (s) => (s % 3 === 1 ? YT_CARGANDO : YT_PAUSADO));
    expect(acciones).toContain('recargar');
    expect(acciones).toContain('silenciar');
  });

  it('un instante sonando no reinicia la escalera; sonar estable sí', () => {
    const casi = { detenidoDesde: 0, sonandoDesde: null, intentos: 3, ultimoIntento: 0 };
    const breve = decidirGuardia(casi, { estadoYT: YT_SONANDO, debeSonar: true, ahora: 9000 });
    expect(breve.guardia.intentos).toBe(3);
    expect(decidirGuardia(breve.guardia, { estadoYT: YT_SONANDO, debeSonar: true, ahora: 9000 + ESTABLE_MS }).guardia).toEqual(GUARDIA_INICIAL);
  });

  it.each([YT_SIN_EMPEZAR, YT_LISTO, YT_PAUSADO])('recupera el estado detenido %i', (estado) => {
    expect(recorrer(4, estado)).toContain('reanudar');
  });

  it.each([YT_SONANDO, YT_CARGANDO, YT_TERMINADO])('no toca el estado %i', (estado) => {
    expect(recorrer(30, estado)).toEqual([]);
  });

  it('respeta la pausa del panel', () => {
    expect(recorrer(30, YT_PAUSADO, false)).toEqual([]);
  });
});

describe('decidirAudio', () => {
  it('al volver a la pestaña o con un volumen nuevo, prueba el audio enseguida', () => {
    expect(decidirAudio(pruebaYa(5000), { estadoYT: YT_SONANDO, silenciado: true, ahora: 5000 }).accion).toBe('probar');
  });

  it('prueba con audio y, si sigue sonando, lo da por recuperado', () => {
    const inicial = pruebaInicial(0);
    expect(decidirAudio(inicial, { estadoYT: YT_SONANDO, silenciado: true, ahora: PRIMERA_PRUEBA_MS - 1 }).accion).toBe('nada');
    const probando = decidirAudio(inicial, { estadoYT: YT_SONANDO, silenciado: true, ahora: PRIMERA_PRUEBA_MS });
    expect(probando.accion).toBe('probar');
    expect(decidirAudio(probando.prueba, { estadoYT: YT_SONANDO, silenciado: false, ahora: PRIMERA_PRUEBA_MS + CONFIRMAR_AUDIO_MS }).accion).toBe('confirmar');
  });

  it.each([{ estadoYT: YT_PAUSADO, silenciado: false }, { estadoYT: YT_SONANDO, silenciado: true }])('si sigue bloqueado vuelve al silencio y espera el doble: %o', (caso) => {
    const probando = { ...pruebaInicial(0), probandoDesde: 30_000 };
    const r = decidirAudio(probando, { ...caso, ahora: 31_000 });
    expect(r.accion).toBe('volver');
    expect(r.prueba.espera).toBe(PRIMERA_PRUEBA_MS * 2);
  });

  it('la espera entre pruebas tiene tope', () => {
    const r = decidirAudio({ siguiente: 0, espera: PRUEBA_MAXIMA_MS, probandoDesde: 0 }, { estadoYT: YT_PAUSADO, silenciado: false, ahora: 1 });
    expect(r.prueba.espera).toBe(PRUEBA_MAXIMA_MS);
  });
});
