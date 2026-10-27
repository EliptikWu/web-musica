import type { HTMLAttributes, ReactNode } from 'react';

/** Arco de luz: marco de cromo y tubos; encendido se reciben canciones, apagado no. */
export function Arco({ apagado = false, className, children, ...resto }: HTMLAttributes<HTMLElement> & { apagado?: boolean }) {
  return (
    <section className={['arco', apagado && 'arco--apagado', className].filter(Boolean).join(' ')} {...resto}>
      <div className="arco__luz">{children}</div>
    </section>
  );
}

/** Ventana de papel dentro del arco o de un marco. */
export function Ventana({ className, ...resto }: HTMLAttributes<HTMLDivElement>) {
  return <div className={className ? `ventana ${className}` : 'ventana'} {...resto} />;
}

/** Luz de estado: punto ámbar encendido, gris apagado. El texto dice el estado. */
export function LuzEstado({ encendida, children, className }: { encendida: boolean; children: ReactNode; className?: string }) {
  return <p className={['luz-estado', !encendida && 'luz-estado--apagada', className].filter(Boolean).join(' ')}>{children}</p>;
}

/**
 * Fichas: los pedidos que le quedan al teléfono. Son decorativas: siempre van con el texto
 * «Te quedan N de M pedidos» al lado (doc 05).
 */
export function Fichas({ libres, total }: { libres: number; total: number }) {
  return (
    <span className="fichas" aria-hidden="true">
      {Array.from({ length: total }, (_, i) => (
        <i key={i} className={i < libres ? 'ficha ficha--libre' : 'ficha'} />
      ))}
    </span>
  );
}

/** Insignia cereza: «24 votos», «Subió 2 puestos». */
export function Insignia({ className, ...resto }: HTMLAttributes<HTMLSpanElement>) {
  return <span className={className ? `insignia ${className}` : 'insignia'} {...resto} />;
}

export type Dia = { corto: string; largo: string; abre: boolean; hoy?: boolean };

/** Los siete días del horario: los que abren son teclas; «Hoy» marca el día actual. */
export function DiasSemana({ dias, className }: { dias: Dia[]; className?: string }) {
  return (
    <ul className={className ? `dias ${className}` : 'dias'}>
      {dias.map((dia) => (
        <li key={dia.corto} className={dia.abre ? 'dia dia--abre' : 'dia'}>
          <span aria-hidden="true">{dia.corto}</span>
          <span className="solo-lector">{`${dia.largo}: ${dia.abre ? 'abre' : 'cerrado'}${dia.hoy ? ', hoy' : ''}`}</span>
          {dia.hoy ? <small aria-hidden="true">Hoy</small> : null}
        </li>
      ))}
    </ul>
  );
}
