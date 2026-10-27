'use client';

import type { ButtonHTMLAttributes } from 'react';
import { claseTecla, type TamanoTecla, type VarianteTecla } from './clases-tecla';

type PropsBoton = ButtonHTMLAttributes<HTMLButtonElement> & {
  variante?: Exclude<VarianteTecla, 'hundida' | 'tuya'>;
  tamano?: TamanoTecla;
  /** Envío en curso: la tecla no responde a otro toque (RNF-008). */
  ocupada?: boolean;
};

/** Tecla que se pulsa. Mientras está ocupada ignora los toques sin perder el foco. */
export function Tecla({ variante = 'normal', tamano = 'normal', ocupada = false, className, onClick, type = 'button', ...resto }: PropsBoton) {
  return (
    <button
      type={type}
      className={claseTecla(variante, tamano, className)}
      aria-busy={ocupada || undefined}
      onClick={(evento) => {
        if (ocupada) {
          evento.preventDefault();
          return;
        }
        onClick?.(evento);
      }}
      {...resto}
    />
  );
}
