'use client';

import type { Reproduccion } from '@rockola/contrato';
import { Icono } from '@rockola/ui';
import { useCallback, useEffect, useRef, useState } from 'react';
import { TV } from '@/lib/textos/pantalla';
import { decidirAudio, decidirGuardia, GUARDIA_INICIAL, type PruebaAudio, pruebaInicial } from './guardia-reproduccion';

/**
 * El reproductor de la pantalla del bar (RF-019, RNF-011). Con un ID de YouTube real usa el
 * reproductor embebido oficial (youtube-nocookie, sin descargar nada); con los IDs de la
 * simulación (`sim-…`) muestra un disco que gira y lo dice. La cola avanza con lo que el
 * reproductor reporta: inicio, fin y error (doc 04, «Pantalla del bar»).
 */
export type PropsReproductor = {
  reproduccion: Reproduccion;
  /** Dónde debería ir ahora, según el servidor (para unirse a mitad de canción). */
  posicionSeg: number;
  /** El toque de «Encender la pantalla» ya se hizo: el navegador deja sonar con audio. */
  encendida: boolean;
  onInicio: (reproduccionId: string, posicionSeg: number) => void;
  onFin: (reproduccionId: string, posicionSeg: number) => void;
  onError: (reproduccionId: string, posicionSeg: number, codigoYoutube: number) => void;
  /** RF-062: lo fija el panel; la TV lo aplica en cuanto llega. */
  volumen: number;
  /** El navegador solo dejó sonar en silencio: hace falta un toque para el audio. */
  onSinSonido?: (sinSonido: boolean) => void;
};

export const esIdSimulado = (videoId: string) => videoId.startsWith('sim-');

export function Reproductor(props: PropsReproductor) {
  return esIdSimulado(props.reproduccion.cancion.video_id) ? <ReproductorSimulado {...props} /> : <ReproductorYouTube {...props} />;
}

function ReproductorSimulado({ reproduccion, volumen }: PropsReproductor) {
  // Sin sonido de verdad: el volumen queda en el atributo para comprobar que llegó (RF-062).
  return (
    <div className="video" role="img" aria-label={`Video simulado de ${reproduccion.cancion.titulo}`} data-volumen={volumen}>
      <div className="video__centro">
        <div className={reproduccion.estado === 'sonando' ? 'video__disco video__disco--gira' : 'video__disco'} />
      </div>
      <span className="video__marca">
        <Icono nombre="youtube" />
        Video simulado
      </span>
    </div>
  );
}

// ── YouTube IFrame API (tipos mínimos de lo que se usa) ─────────────────────

type ReproductorYT = {
  loadVideoById(opciones: { videoId: string; startSeconds?: number }): void;
  cueVideoById(opciones: { videoId: string; startSeconds?: number }): void;
  playVideo(): void;
  pauseVideo(): void;
  seekTo(segundos: number, permitirBuscar: boolean): void;
  getCurrentTime(): number;
  getPlayerState(): number;
  setVolume(volumen: number): void;
  mute(): void;
  unMute(): void;
  isMuted(): boolean;
  destroy(): void;
};

/** RF-062: 0 es silencio (mute); lo demás, el volumen del reproductor de YouTube (0 a 100). */
function aplicarVolumen(r: ReproductorYT, volumen: number) {
  if (volumen <= 0) {
    r.mute();
    return;
  }
  r.unMute();
  r.setVolume(volumen);
}
type ApiYT = {
  Player: new (
    elemento: HTMLElement,
    opciones: {
      host?: string;
      videoId: string;
      playerVars?: Record<string, number | string>;
      events?: { onReady?: () => void; onStateChange?: (e: { data: number }) => void; onError?: (e: { data: number }) => void; onAutoplayBlocked?: () => void };
    },
  ) => ReproductorYT;
};

declare global {
  interface Window {
    YT?: ApiYT;
    onYouTubeIframeAPIReady?: () => void;
  }
}

let cargandoApi: Promise<ApiYT> | null = null;

/** Carga la IFrame API una sola vez. La CSP la deja pasar por 'strict-dynamic' (doc 03). */
function cargarApiYouTube(): Promise<ApiYT> {
  if (window.YT?.Player) return Promise.resolve(window.YT);
  cargandoApi ??= new Promise((resolver, rechazar) => {
    window.onYouTubeIframeAPIReady = () => (window.YT ? resolver(window.YT) : rechazar(new Error('YT no quedó definido')));
    const script = document.createElement('script');
    script.src = 'https://www.youtube.com/iframe_api';
    script.async = true;
    script.onerror = () => {
      cargandoApi = null;
      rechazar(new Error('No cargó la API de YouTube'));
    };
    document.head.appendChild(script);
  });
  return cargandoApi;
}

// Estados de YT.PlayerState que se usan.
const TERMINADO = 0;
const SONANDO = 1;
const DESFASE_MAXIMO_SEG = 4;
const REVISAR_DESFASE_MS = 5000;
// Si YouTube no emite ENDED, la TV intenta recuperar la cola sin intervención manual.
const REVISAR_FINAL_MS = 2000;
const FINAL_SIN_EVENTO_MS = 8000;
const REINTENTAR_FIN_MS = 12000;

/**
 * El reproductor de YouTube. Reglas que evitan los fallos del barrido de la iteración 2:
 * - Nada se le pide al reproductor antes de `onReady` (`listo`).
 * - Lo que se reporta lleva el ID de lo que está cargado en el reproductor, no el de la
 *   última foto: si el servidor ya avanzó, un fin tardío no salta otra canción.
 * - La pausa del panel manda también al encender; nunca se le dice «play» a un video terminado.
 * - El desfase se revisa cada 5 s, no en cada render.
 * - Si YouTube o el navegador lo detienen sin que el panel lo pida, se retoma solo (guardia).
 */
function ReproductorYouTube({ reproduccion, posicionSeg, encendida, onInicio, onFin, onError, volumen, onSinSonido }: PropsReproductor) {
  const contenedor = useRef<HTMLDivElement>(null);
  const reproductor = useRef<ReproductorYT | null>(null);
  const listo = useRef(false);
  const idCargado = useRef<string | null>(null);
  const iniciosReportados = useRef(new Set<string>());
  const ultimoFinEnviado = useRef<{ id: string; instante: number } | null>(null);
  const finalDetectado = useRef<{ id: string; desde: number } | null>(null);
  const deseado = useRef({ reproduccion, posicionSeg, volumen });
  const llamadas = useRef({ onInicio, onFin, onError, onSinSonido });
  const guardia = useRef(GUARDIA_INICIAL);
  const silencioForzado = useRef(false);
  const pruebaAudio = useRef<PruebaAudio | null>(null);
  const conError = useRef(new Set<string>());
  const [sinAudio, setSinAudio] = useState(false);
  useEffect(() => {
    deseado.current = { reproduccion, posicionSeg, volumen };
    llamadas.current = { onInicio, onFin, onError, onSinSonido };
  });

  const posicionReal = () => Math.floor(reproductor.current?.getCurrentTime() ?? 0);

  /** Con el audio bloqueado, quitar el silencio volvería a detener el video. */
  const ponerVolumen = useCallback((r: ReproductorYT, valor: number) => {
    if (silencioForzado.current) r.setVolume(Math.max(valor, 1));
    else aplicarVolumen(r, valor);
  }, []);

  const sonarEnSilencio = useCallback(() => {
    const r = reproductor.current;
    if (!r || deseado.current.reproduccion.estado !== 'sonando') return;
    r.mute();
    r.playVideo();
    if (!silencioForzado.current) {
      silencioForzado.current = true;
      pruebaAudio.current = pruebaInicial(Date.now());
      setSinAudio(true);
      llamadas.current.onSinSonido?.(true);
    }
  }, []);

  const terminarSilencio = useCallback(() => {
    if (!silencioForzado.current) return;
    silencioForzado.current = false;
    pruebaAudio.current = null;
    guardia.current = GUARDIA_INICIAL;
    setSinAudio(false);
    llamadas.current.onSinSonido?.(false);
  }, []);

  /** Con un toque el navegador ya deja sonar con audio. */
  const recuperarAudio = useCallback(() => {
    const r = reproductor.current;
    if (!r || !silencioForzado.current) return;
    terminarSilencio();
    aplicarVolumen(r, deseado.current.volumen);
    if (deseado.current.reproduccion.estado === 'sonando') r.playVideo();
  }, [terminarSilencio]);

  // El mismo ID/tipo siempre se informa con la misma posición (duración completa).
  // Así las retransmisiones conservan el cuerpo exigido por la clave idempotente del backend.
  const avisarFin = useCallback((id: string) => {
    const rep = deseado.current.reproduccion;
    if (rep.id !== id || rep.estado === 'pausada') return;
    const ahora = Date.now();
    const anterior = ultimoFinEnviado.current;
    if (anterior?.id === id && ahora - anterior.instante < REINTENTAR_FIN_MS) return;
    ultimoFinEnviado.current = { id, instante: ahora };
    llamadas.current.onFin(id, Math.max(0, rep.cancion.duracion_seg));
  }, []);

  // Crea el reproductor al encender la pantalla.
  useEffect(() => {
    if (!encendida || !contenedor.current) return;
    let vivo = true;
    const destino = document.createElement('div');
    destino.dataset.reproductor = '';
    contenedor.current.appendChild(destino);
    const inicial = deseado.current.reproduccion;
    cargarApiYouTube()
      .then((YT) => {
        if (!vivo) return;
        idCargado.current = inicial.id;
        reproductor.current = new YT.Player(destino, {
          host: 'https://www.youtube-nocookie.com',
          videoId: inicial.cancion.video_id,
          playerVars: { autoplay: 0, controls: 0, disablekb: 1, rel: 0, playsinline: 1, iv_load_policy: 3 },
          events: {
            onReady: () => {
              listo.current = true;
              const r = reproductor.current!;
              const { reproduccion: rep, posicionSeg: pos } = deseado.current;
              ponerVolumen(r, deseado.current.volumen);
              // Si mientras cargaba el servidor cambió de canción, se carga la vigente.
              if (rep.id !== idCargado.current) {
                idCargado.current = rep.id;
                if (rep.estado === 'pausada') r.cueVideoById({ videoId: rep.cancion.video_id, startSeconds: Math.floor(pos) });
                else r.loadVideoById({ videoId: rep.cancion.video_id, startSeconds: Math.floor(pos) });
                return;
              }
              r.seekTo(pos, true);
              if (rep.estado === 'sonando') r.playVideo();
              else r.pauseVideo();
            },
            onStateChange: (e) => {
              const id = idCargado.current;
              if (!id) return;
              if (e.data === SONANDO && !iniciosReportados.current.has(id)) {
                iniciosReportados.current.add(id);
                llamadas.current.onInicio(id, posicionReal());
              }
              if (e.data === TERMINADO) avisarFin(id);
            },
            onError: (e) => {
              if (!idCargado.current) return;
              // YouTube ya lo rechazó: la guardia no debe insistir mientras el servidor avanza.
              conError.current.add(idCargado.current);
              llamadas.current.onError(idCargado.current, posicionReal(), e.data);
            },
            onAutoplayBlocked: sonarEnSilencio,
          },
        });
      })
      .catch((error: unknown) => console.error('[pantalla] la API de YouTube no cargó', error));
    return () => {
      vivo = false;
      listo.current = false;
      reproductor.current?.destroy();
      reproductor.current = null;
      idCargado.current = null;
      guardia.current = GUARDIA_INICIAL;
      terminarSilencio();
      destino.remove();
    };
  }, [encendida, avisarFin, ponerVolumen, sonarEnSilencio, terminarSilencio]);

  // Guardia: si debería sonar y quedó detenido, se retoma sin que nadie toque la pantalla.
  useEffect(() => {
    if (!encendida) return;
    const temporizador = window.setInterval(() => {
      const r = reproductor.current;
      if (!r || !listo.current) return;
      const { reproduccion: rep, posicionSeg: pos } = deseado.current;
      // Al final de la canción manda la recuperación del fin, no esta.
      const debeSonar = idCargado.current === rep.id && rep.estado === 'sonando' && !conError.current.has(rep.id) && pos < rep.cancion.duracion_seg - 1;
      const ahora = Date.now();
      const estadoYT = r.getPlayerState();
      const { guardia: siguiente, accion } = decidirGuardia(guardia.current, { estadoYT, debeSonar, ahora });
      guardia.current = siguiente;
      if (accion === 'reanudar') r.playVideo();
      else if (accion === 'recargar') r.loadVideoById({ videoId: rep.cancion.video_id, startSeconds: Math.floor(pos) });
      else if (accion === 'silenciar') sonarEnSilencio();

      // En silencio por un corte pasajero, el bar no se queda mudo: se vuelve a probar con audio.
      if (!silencioForzado.current || !pruebaAudio.current || !debeSonar) return;
      if (deseado.current.volumen <= 0) {
        terminarSilencio();
        return;
      }
      const { prueba, accion: audio } = decidirAudio(pruebaAudio.current, { estadoYT, silenciado: r.isMuted(), ahora });
      pruebaAudio.current = prueba;
      if (audio === 'probar') aplicarVolumen(r, deseado.current.volumen);
      else if (audio === 'confirmar') terminarSilencio();
      else if (audio === 'volver') {
        guardia.current = GUARDIA_INICIAL;
        r.mute();
        r.playVideo();
      }
    }, 1000);
    return () => window.clearInterval(temporizador);
  }, [encendida, sonarEnSilencio, terminarSilencio]);

  // Un toque o una tecla devuelve el audio que el navegador había bloqueado.
  useEffect(() => {
    if (!encendida) return;
    window.addEventListener('pointerdown', recuperarAudio);
    window.addEventListener('keydown', recuperarAudio);
    return () => {
      window.removeEventListener('pointerdown', recuperarAudio);
      window.removeEventListener('keydown', recuperarAudio);
    };
  }, [encendida, recuperarAudio]);

  // Otra canción: se carga desde donde vaya (por si la pantalla se une tarde).
  const { id, estado } = reproduccion;
  const videoId = reproduccion.cancion.video_id;
  useEffect(() => {
    const r = reproductor.current;
    if (!r || !listo.current || idCargado.current === id) return;
    idCargado.current = id;
    const desde = Math.floor(deseado.current.posicionSeg);
    if (deseado.current.reproduccion.estado === 'pausada') r.cueVideoById({ videoId, startSeconds: desde });
    else r.loadVideoById({ videoId, startSeconds: desde });
  }, [id, videoId]);

  // Pausa y reanudación desde el panel (RF-040).
  useEffect(() => {
    const r = reproductor.current;
    if (!r || !listo.current) return;
    if (estado === 'pausada') r.pauseVideo();
    else if (r.getPlayerState() !== TERMINADO) r.playVideo();
  }, [estado]);

  // Volumen desde el panel (RF-062).
  useEffect(() => {
    const r = reproductor.current;
    if (r && listo.current) ponerVolumen(r, volumen);
  }, [volumen, ponerVolumen]);

  // Recuperación cuando el navegador llega al final sin emitir el evento ENDED.
  // IMPORTANTE: no concluye una canción mientras YouTube sigue reproduciendo lejos del final.
  // Si la TV se vinculó tarde y la posición del servidor ya está agotada, espera un margen
  // breve antes de liberar la reproducción atascada; no requiere otro POST manual.
  useEffect(() => {
    if (!encendida) return;
    const temporizador = window.setInterval(() => {
      const r = reproductor.current;
      if (!r || !listo.current) return;
      const { reproduccion: rep, posicionSeg: posServidor } = deseado.current;
      if (idCargado.current !== rep.id || rep.estado === 'pausada') {
        finalDetectado.current = null;
        return;
      }
      const duracion = rep.cancion.duracion_seg;
      if (duracion <= 0) return;
      const estadoYT = r.getPlayerState();
      const posicionYT = r.getCurrentTime();
      const cercaDelFinalYT = posicionYT >= Math.max(0, duracion - 2);
      const servidorAgotado = posServidor >= duracion - 1;
      const youtubeDetenido = estadoYT !== SONANDO;
      // No saltar un video que todavía está sonando normalmente a mitad de camino.
      const finProbable = estadoYT === TERMINADO || cercaDelFinalYT || (servidorAgotado && youtubeDetenido);
      if (!finProbable) {
        finalDetectado.current = null;
        return;
      }
      const ahora = Date.now();
      if (finalDetectado.current?.id !== rep.id) finalDetectado.current = { id: rep.id, desde: ahora };
      // ENDED es señal definitiva; otros casos esperan por un posible retraso de YouTube.
      if (estadoYT === TERMINADO || ahora - finalDetectado.current.desde >= FINAL_SIN_EVENTO_MS) {
        avisarFin(rep.id);
      }
    }, REVISAR_FINAL_MS);
    return () => window.clearInterval(temporizador);
  }, [encendida, avisarFin]);

  // Si se desfasó del servidor (se colgó la red, se unió tarde), se corrige cada 5 s.
  useEffect(() => {
    const revisar = setInterval(() => {
      const r = reproductor.current;
      if (!r || !listo.current || r.getPlayerState() !== SONANDO) return;
      if (Math.abs(r.getCurrentTime() - deseado.current.posicionSeg) > DESFASE_MAXIMO_SEG) r.seekTo(deseado.current.posicionSeg, true);
    }, REVISAR_DESFASE_MS);
    return () => clearInterval(revisar);
  }, []);

  return (
    <div ref={contenedor} className="video" aria-label={`Video de ${reproduccion.cancion.titulo}`} role="region">
      {!encendida ? (
        <span className="video__marca">
          <Icono nombre="youtube" />
          Video de YouTube
        </span>
      ) : null}
      {/* Un toque dentro del iframe no llega a esta página: esta capa lo recibe y toma el foco del control remoto. */}
      {sinAudio ? <button type="button" className="video__tocar" aria-label={TV.activarSonido} autoFocus onClick={recuperarAudio} /> : null}
    </div>
  );
}