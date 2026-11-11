'use client';

import type { ItemCola } from '@rockola/contrato';
import { TarjetaTitulo, Tecla, TeclaFija } from '@rockola/ui';
import type { ReactNode } from 'react';
import type { EstadoTecla } from '@/lib/reglas/pedidos';
import { voto } from '@/lib/textos/cliente';

type Props = {
  item: ItemCola;
  tecla: EstadoTecla;
  /** Franja de la tarjeta: el artista, a veces con su espera. */
  artista?: ReactNode;
  /** Si el servicio deja votar. Si no, la fila no lleva tecla. */
  puedeVotar: boolean;
  ocupada: boolean;
  onVotar: (item: ItemCola) => void;
  /** Con esto, «Votaste» se pulsa para mover el voto a otra canción (RF-014). */
  onMover?: (item: ItemCola) => void;
};

/**
 * Una canción de la cola con su tecla (doc 05): «+1» para votar, «Votaste» hundida, «Tuya»
 * ámbar. Los tres estados se distinguen por texto y forma, no solo por color (WCAG 1.4.1).
 */
export function FilaCancion({ item, tecla, artista, puedeVotar, ocupada, onVotar, onMover }: Props) {
  const { titulo } = item.cancion;
  return (
    <li className="fila" data-solicitud={item.solicitud_id}>
      <TarjetaTitulo
        puesto={item.puesto}
        titulo={titulo}
        artista={artista ?? item.cancion.artista}
        votos={item.votos_total}
        variante={tecla === 'tuya' ? 'tuya' : 'normal'}
        miniatura={item.cancion.miniatura_url}
      />
      {tecla === 'tuya' ? (
        <TeclaFija variante="tuya">Tuya</TeclaFija>
      ) : tecla === 'votaste' && puedeVotar && onMover ? (
        // Sigue hundida (ya votaste); al pulsarla se elige a qué otra canción pasa el voto.
        <Tecla variante="hundida" data-tecla-fija="" aria-label={voto.mover(titulo)} aria-haspopup="dialog" ocupada={ocupada} onClick={() => onMover(item)}>
          Votaste
        </Tecla>
      ) : tecla === 'votaste' ? (
        <TeclaFija variante="hundida">Votaste</TeclaFija>
      ) : puedeVotar ? (
        <Tecla aria-label={voto.etiqueta(titulo)} ocupada={ocupada} onClick={() => onVotar(item)}>
          +1
        </Tecla>
      ) : null}
    </li>
  );
}
