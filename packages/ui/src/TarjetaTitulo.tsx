import type { ReactNode } from 'react';

export type VarianteTarjeta = 'normal' | 'tuya' | 'apagada';

type Props = {
  titulo: string;
  /** La franja: el artista, a veces con contexto («Joe Arroyo, suena ahora»). */
  artista: ReactNode;
  variante?: VarianteTarjeta;
  /** Puesto en la cola. Sin él, la tarjeta es un resultado de búsqueda. */
  puesto?: number;
  /** Votos o, en un resultado, la duración ya formateada. */
  votos?: number;
  duracion?: string;
  /** Nombre del elemento: `li` dentro de una lista, `div` dentro de una fila con tecla. */
  como?: 'div' | 'li';
  className?: string;
};

/**
 * Tarjeta de título: la tira de papel que identificaba cada disco (doc 05). Papel crema,
 * título condensado con su grafía real y franja cereza con el artista; ámbar si es tuya,
 * apagada si no se puede pedir.
 */
export function TarjetaTitulo({ titulo, artista, variante = 'normal', puesto, votos, duracion, como = 'div', className }: Props) {
  const Elemento = como;
  const esResultado = puesto === undefined;
  const sinCifra = votos === undefined && duracion === undefined;
  const clases = [
    'tarjeta',
    esResultado && (sinCifra ? 'tarjeta--sola' : 'tarjeta--resultado'),
    variante !== 'normal' && `tarjeta--${variante}`,
    className,
  ].filter(Boolean).join(' ');
  return (
    <Elemento className={clases}>
      {puesto !== undefined && (
        <span className="tarjeta__num">
          <span className="solo-lector">Puesto </span>
          {puesto}
        </span>
      )}
      <div className="tarjeta__centro">
        <p className="tarjeta__titulo">{titulo}</p>
        <p className="tarjeta__artista">{artista}</p>
      </div>
      {votos !== undefined && (
        <p className="tarjeta__votos">
          {votos}
          <small>{votos === 1 ? 'voto' : 'votos'}</small>
        </p>
      )}
      {duracion !== undefined && (
        <p className="tarjeta__dura">
          <span className="solo-lector">Duración </span>
          {duracion}
        </p>
      )}
    </Elemento>
  );
}
