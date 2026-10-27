'use client';

import { useLayoutEffect, useRef } from 'react';

type Props = {
  /** Segundos transcurridos y duración total. */
  valor: number;
  maximo: number;
  textoInicio: string;
  textoFin: string;
  etiqueta?: string;
  className?: string;
};

/**
 * Barra de avance de una canción. El ancho se pone desde una ref y no con `style=""`:
 * la política de contenido no deja estilos en línea en el HTML del servidor (doc 03).
 */
export function Avance({ valor, maximo, textoInicio, textoFin, etiqueta = 'Avance de la canción', className }: Props) {
  const relleno = useRef<HTMLSpanElement>(null);
  const fraccion = maximo > 0 ? Math.min(1, Math.max(0, valor / maximo)) : 0;
  useLayoutEffect(() => {
    if (relleno.current) relleno.current.style.transform = `scaleX(${fraccion})`;
  }, [fraccion]);
  return (
    <div className={className ? `avance ${className}` : 'avance'}>
      <span aria-hidden="true">{textoInicio}</span>
      <div
        className="surco"
        role="progressbar"
        aria-label={etiqueta}
        aria-valuemin={0}
        aria-valuemax={Math.round(maximo)}
        aria-valuenow={Math.round(Math.min(valor, maximo))}
        aria-valuetext={`${textoInicio} de ${textoFin}`}
      >
        <span ref={relleno} />
      </div>
      <span aria-hidden="true">{textoFin}</span>
    </div>
  );
}

/** La raya ámbar bajo el mini reproductor: decorativa, el avance ya se lee arriba. */
export function MiniAvance({ fraccion }: { fraccion: number }) {
  const relleno = useRef<HTMLSpanElement>(null);
  const f = Math.min(1, Math.max(0, fraccion));
  useLayoutEffect(() => {
    if (relleno.current) relleno.current.style.transform = `scaleX(${f})`;
  }, [f]);
  return (
    <div className="mini-avance" aria-hidden="true">
      <span ref={relleno} />
    </div>
  );
}
