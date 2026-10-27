/**
 * Enfoca un elemento en cuanto exista (lo pinta React un poco después): lo intenta durante
 * unos cuadros y se rinde sin error. Para no perder el foco cuando un control se reemplaza
 * por otro (WCAG 2.4.3).
 */
export function enfocarCuandoExista(selector: string, cuadros = 30): void {
  const intentar = (quedan: number) => {
    const el = document.querySelector<HTMLElement>(selector);
    if (el) el.focus();
    else if (quedan > 0) requestAnimationFrame(() => intentar(quedan - 1));
  };
  requestAnimationFrame(() => intentar(cuadros));
}
