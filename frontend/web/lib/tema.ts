import { Tema } from '@rockola/contrato';

/** Dónde recuerda el navegador el diseño de un bar (RF-059), para pintarlo antes de que llegue `/bar`. */
export function claveTema(slug: string): string {
  return `rockola.tema.${slug}`;
}

/**
 * El script previo de ADR-26: en `/b/<slug>/…`, antes de pintar, pone en `<html>` el último
 * diseño que este navegador vio de ese bar. Va en el layout raíz (se escribe una sola vez, no en
 * cada navegación) con el nonce de la CSP. Solo acepta los temas del contrato; el slug sale de
 * la URL con la misma forma que valida el layout del bar.
 */
export const SCRIPT_TEMA_PREVIO = `try{var p=location.pathname.split("/");if(p[1]==="b"&&/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(p[2]||"")){var t=localStorage.getItem(${JSON.stringify(claveTema(''))}+p[2]);if(${JSON.stringify(Tema.options)}.indexOf(t)>-1)document.documentElement.dataset.tema=t}}catch(e){}`;
