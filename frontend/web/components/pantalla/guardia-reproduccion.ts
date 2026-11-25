/**
 * Qué hacer cuando el video debería sonar y YouTube lo tiene detenido (RF-019, RNF-009, ADR-28).
 * YouTube o el navegador pueden pausarlo por su cuenta y en un bar nadie va a darle «seguir». Va
 * de lo más suave a lo más fuerte: reanudar, recargar y, si nada funciona, sonar en silencio
 * para que la música y la cola no se queden paradas. Después vuelve a probar con audio.
 */

export const YT_SIN_EMPEZAR = -1;
export const YT_TERMINADO = 0;
export const YT_SONANDO = 1;
export const YT_PAUSADO = 2;
export const YT_CARGANDO = 3;
export const YT_LISTO = 5;

const DETENIDO = new Set([YT_SIN_EMPEZAR, YT_PAUSADO, YT_LISTO]);
export const estaDetenido = (estadoYT: number) => DETENIDO.has(estadoYT);
/** Margen para que un video recién cargado arranque solo. */
export const GRACIA_MS = 2500;
export const ENTRE_INTENTOS_MS = 3000;
/** Tiempo sonando sin parar para dar el problema por resuelto: reanudar pasa por «cargando». */
export const ESTABLE_MS = 4000;

export type Guardia = { detenidoDesde: number | null; sonandoDesde: number | null; intentos: number; ultimoIntento: number };
export type AccionGuardia = 'nada' | 'reanudar' | 'recargar' | 'silenciar';
export const GUARDIA_INICIAL: Guardia = { detenidoDesde: null, sonandoDesde: null, intentos: 0, ultimoIntento: 0 };

export function decidirGuardia(g: Guardia, { estadoYT, debeSonar, ahora }: { estadoYT: number; debeSonar: boolean; ahora: number }): { guardia: Guardia; accion: AccionGuardia } {
  if (!debeSonar) return { guardia: GUARDIA_INICIAL, accion: 'nada' };
  if (estadoYT === YT_SONANDO) {
    const sonandoDesde = g.sonandoDesde ?? ahora;
    if (ahora - sonandoDesde >= ESTABLE_MS) return { guardia: GUARDIA_INICIAL, accion: 'nada' };
    return { guardia: { ...g, sonandoDesde }, accion: 'nada' };
  }
  if (!estaDetenido(estadoYT)) return { guardia: { ...g, sonandoDesde: null }, accion: 'nada' };
  const detenidoDesde = g.detenidoDesde ?? ahora;
  if (ahora - detenidoDesde < GRACIA_MS || (g.intentos > 0 && ahora - g.ultimoIntento < ENTRE_INTENTOS_MS)) {
    return { guardia: { ...g, detenidoDesde, sonandoDesde: null }, accion: 'nada' };
  }
  const intentos = g.intentos + 1;
  const accion: AccionGuardia = intentos <= 2 ? 'reanudar' : intentos === 3 ? 'recargar' : 'silenciar';
  return { guardia: { detenidoDesde, sonandoDesde: null, intentos, ultimoIntento: ahora }, accion };
}

// ── Volver a probar con audio ──────────────────────────────────────────────

/**
 * Un corte de red también lleva a sonar en silencio: el bar no se queda mudo toda la noche. Se
 * prueba pronto y seguido (el equipo vio la TV muda largos ratos con 30 s a 5 min, 2026-11-25).
 */
export const PRIMERA_PRUEBA_MS = 10_000;
export const PRUEBA_MAXIMA_MS = 60_000;
export const CONFIRMAR_AUDIO_MS = 3000;

export type PruebaAudio = { siguiente: number; espera: number; probandoDesde: number | null };
export type AccionAudio = 'nada' | 'probar' | 'confirmar' | 'volver';

export const pruebaInicial = (ahora: number): PruebaAudio => ({ siguiente: ahora + PRIMERA_PRUEBA_MS, espera: PRIMERA_PRUEBA_MS, probandoDesde: null });
/** Probar ya: la pestaña volvió a verse o el panel cambió el volumen. */
export const pruebaYa = (ahora: number): PruebaAudio => ({ siguiente: ahora, espera: PRIMERA_PRUEBA_MS, probandoDesde: null });

export function decidirAudio(p: PruebaAudio, { estadoYT, silenciado, ahora }: { estadoYT: number; silenciado: boolean; ahora: number }): { prueba: PruebaAudio; accion: AccionAudio } {
  if (p.probandoDesde === null) {
    if (estadoYT === YT_SONANDO && ahora >= p.siguiente) return { prueba: { ...p, probandoDesde: ahora }, accion: 'probar' };
    return { prueba: p, accion: 'nada' };
  }
  // Al quitar el silencio, el navegador lo detuvo o lo volvió a silenciar: sigue bloqueado.
  if (estaDetenido(estadoYT) || silenciado) {
    const espera = Math.min(p.espera * 2, PRUEBA_MAXIMA_MS);
    return { prueba: { siguiente: ahora + espera, espera, probandoDesde: null }, accion: 'volver' };
  }
  if (estadoYT === YT_SONANDO && ahora - p.probandoDesde >= CONFIRMAR_AUDIO_MS) return { prueba: p, accion: 'confirmar' };
  return { prueba: p, accion: 'nada' };
}
