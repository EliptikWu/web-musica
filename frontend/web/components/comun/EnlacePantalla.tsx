import type { AnchorHTMLAttributes } from 'react';

/**
 * Enlace a la pantalla del bar. Siempre carga el documento entero (no navegación suave de
 * Next): la política de contenido se fija al cargar el documento, y solo la de la pantalla
 * deja embeber el video de YouTube (doc 03). Úsalo en cualquier sitio que lleve a la TV.
 */
export function EnlacePantalla({ slug, ...resto }: AnchorHTMLAttributes<HTMLAnchorElement> & { slug: string }) {
  return <a href={`/b/${slug}/pantalla`} {...resto} />;
}
