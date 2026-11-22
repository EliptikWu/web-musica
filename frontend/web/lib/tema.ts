import { Tema } from '@rockola/contrato';

/** Dónde recuerda el navegador el diseño de un bar (RF-059), para pintarlo antes de que llegue `/bar`. */
export function claveTema(slug: string): string {
  return `rockola.tema.${slug}`;
}

/** El diseño del bar de la sesión del panel (D19). La URL del panel no dice de qué bar es. */
export const CLAVE_TEMA_PANEL = 'rockola.tema-panel';

/** Vuelve a Rockola. Lo usan las pantallas que no pertenecen a un bar. */
export function quitarTema() {
  delete document.documentElement.dataset.tema;
}

/** Al terminar la sesión, por la razón que sea: la siguiente cuenta puede ser de otro bar. */
export function olvidarTemaPanel() {
  quitarTema();
  try {
    localStorage.removeItem(CLAVE_TEMA_PANEL);
  } catch {
    // Sin almacenamiento no hay nada que olvidar.
  }
}

/**
 * El script previo de ADR-26: en `/b/<slug>/…` y en el panel con sesión, antes de pintar, pone
 * en `<html>` el último diseño que este navegador vio de ese bar. Va en el layout raíz (se escribe
 * una sola vez, no en cada navegación) con el nonce de la CSP. Solo acepta los temas del contrato;
 * el slug sale de la URL con la misma forma que valida el layout del bar. Entrar y cambiar la
 * clave quedan en Rockola: ahí todavía no se sabe de qué bar es la cuenta.
 */
export const SCRIPT_TEMA_PREVIO = `try{var p=location.pathname.split("/"),c=null;if(p[1]==="b"&&/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(p[2]||""))c=${JSON.stringify(claveTema(''))}+p[2];else if(p[1]==="panel"&&p[2]!=="entrar"&&p[2]!=="cambiar-clave")c=${JSON.stringify(CLAVE_TEMA_PANEL)};if(c){var t=localStorage.getItem(c);if(${JSON.stringify(Tema.options)}.indexOf(t)>-1)document.documentElement.dataset.tema=t}}catch(e){}`;
