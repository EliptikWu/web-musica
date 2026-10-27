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
  /** Miniatura del video (RF-006, RF-015). Con ella, el puesto va encima como etiqueta. */
  miniatura?: string | null;
  /** Una columna más antes de los votos (la TV pone ahí lo que subió, ↑N). */
  columnaExtra?: ReactNode;
  /** Mostrar «votos» bajo la cifra (en la TV no: la cifra grande basta y se lee de lejos). */
  etiquetaVotos?: boolean;
  /** Nombre del elemento: `li` dentro de una lista, `div` dentro de una fila con tecla. */
  como?: 'div' | 'li';
  className?: string;
};

/**
 * Tarjeta de título: la tira de papel que identificaba cada disco (doc 05). Papel crema,
 * título condensado con su grafía real y franja cereza con el artista; ámbar si es tuya,
 * apagada si no se puede pedir.
 */
export function TarjetaTitulo({ titulo, artista, variante = 'normal', puesto, votos, duracion, miniatura, columnaExtra, etiquetaVotos = true, como = 'div', className }: Props) {
  const Elemento = como;
  const esResultado = puesto === undefined;
  const sinCifra = votos === undefined && duracion === undefined;
  const clases = [
    'tarjeta',
    esResultado && (sinCifra ? 'tarjeta--sola' : 'tarjeta--resultado'),
    variante !== 'normal' && `tarjeta--${variante}`,
    miniatura && 'tarjeta--miniatura',
    className,
  ].filter(Boolean).join(' ');
  return (
    <Elemento className={clases}>
      {miniatura ? (
        <span className="tarjeta__miniatura">
          {/* Decorativa: el título y el artista ya dicen qué canción es. */}
          <img src={miniatura} alt="" width={120} height={90} loading="lazy" decoding="async" />
          {puesto !== undefined && (
            <span className="tarjeta__etiqueta">
              <span className="solo-lector">Puesto </span>
              {puesto}
            </span>
          )}
        </span>
      ) : (
        puesto !== undefined && (
          <span className="tarjeta__num">
            <span className="solo-lector">Puesto </span>
            {puesto}
          </span>
        )
      )}
      <div className="tarjeta__centro">
        <p className="tarjeta__titulo">{titulo}</p>
        <p className="tarjeta__artista">{artista}</p>
      </div>
      {columnaExtra}
      {votos !== undefined && (
        <p className="tarjeta__votos">
          {votos}
          {etiquetaVotos ? <small>{votos === 1 ? 'voto' : 'votos'}</small> : <span className="solo-lector">{votos === 1 ? ' voto' : ' votos'}</span>}
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
