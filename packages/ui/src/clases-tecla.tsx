import type { HTMLAttributes, ReactNode } from 'react';

export type VarianteTecla = 'normal' | 'cereza' | 'hundida' | 'tuya';
export type TamanoTecla = 'normal' | 'grande' | 'chica' | 'ancha';

/**
 * Clases de una tecla, para usarlas también en enlaces (`<Link className={claseTecla(...)}>`):
 * este paquete no depende del enrutador de la app. Vive aparte de `Tecla` para poder
 * llamarse desde componentes del servidor.
 */
export function claseTecla(variante: VarianteTecla = 'normal', tamano: TamanoTecla = 'normal', extra?: string): string {
  return ['tecla', variante !== 'normal' && `tecla--${variante}`, tamano !== 'normal' && `tecla--${tamano}`, extra]
    .filter(Boolean)
    .join(' ');
}

type PropsFija = HTMLAttributes<HTMLSpanElement> & {
  variante: 'hundida' | 'tuya';
  tamano?: TamanoTecla;
  children: ReactNode;
};

/**
 * Tecla que no se pulsa: «Votaste» (hundida) y «Tuya» (ámbar). Es texto, no un botón
 * deshabilitado: dice el estado en vez de esconder la acción (doc 05). Sin `aria-label`: en un
 * `span` ARIA lo prohíbe; dentro de su fila, el texto visible ya se entiende. Acepta el foco
 * por programa (`tabIndex={-1}`) para que el teclado no se pierda cuando reemplaza a «+1».
 */
export function TeclaFija({ variante, tamano = 'normal', className, ...resto }: PropsFija) {
  return <span className={claseTecla(variante, tamano, className)} tabIndex={-1} data-tecla-fija="" {...resto} />;
}
