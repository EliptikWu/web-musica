'use client';

import type { Reproduccion } from '@rockola/contrato';
import { Icono } from '@rockola/ui';
import { useEffect, useRef } from 'react';

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
};

export const esIdSimulado = (videoId: string) => videoId.startsWith('sim-');

export function Reproductor(props: PropsReproductor) {
  return esIdSimulado(props.reproduccion.cancion.video_id) ? <ReproductorSimulado {...props} /> : <ReproductorYouTube {...props} />;
}

function ReproductorSimulado({ reproduccion }: PropsReproductor) {
  return (
    <div className="video" role="img" aria-label={`Video simulado de ${reproduccion.cancion.titulo}`}>
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
  destroy(): void;
};
type ApiYT = {
  Player: new (
    elemento: HTMLElement,
    opciones: {
      host?: string;
      videoId: string;
      playerVars?: Record<string, number | string>;
      events?: { onReady?: () => void; onStateChange?: (e: { data: number }) => void; onError?: (e: { data: number }) => void };
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

/**
 * El reproductor de YouTube. Reglas que evitan los fallos del barrido de la iteración 2:
 * - Nada se le pide al reproductor antes de `onReady` (`listo`).
 * - Lo que se reporta lleva el ID de lo que está cargado en el reproductor, no el de la
 *   última foto: si el servidor ya avanzó, un fin tardío no salta otra canción.
 * - La pausa del panel manda también al encender; nunca se le dice «play» a un video terminado.
 * - El desfase se revisa cada 5 s, no en cada render.
 */
function ReproductorYouTube({ reproduccion, posicionSeg, encendida, onInicio, onFin, onError }: PropsReproductor) {
  const contenedor = useRef<HTMLDivElement>(null);
  const reproductor = useRef<ReproductorYT | null>(null);
  const listo = useRef(false);
  const idCargado = useRef<string | null>(null);
  const iniciosReportados = useRef(new Set<string>());
  const deseado = useRef({ reproduccion, posicionSeg });
  const llamadas = useRef({ onInicio, onFin, onError });
  useEffect(() => {
    deseado.current = { reproduccion, posicionSeg };
    llamadas.current = { onInicio, onFin, onError };
  });

  const posicionReal = () => Math.floor(reproductor.current?.getCurrentTime() ?? 0);

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
              if (e.data === TERMINADO) llamadas.current.onFin(id, posicionReal());
            },
            onError: (e) => {
              if (idCargado.current) llamadas.current.onError(idCargado.current, posicionReal(), e.data);
            },
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
      destino.remove();
    };
  }, [encendida]);

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
    </div>
  );
}
