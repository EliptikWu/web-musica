/**
 * Junta avisos seguidos (ADR-33): el primero dispara en el acto y los que llegan durante
 * `intervaloMs` se juntan en un solo disparo al final. Una ráfaga de votos vuelve a pedir el
 * cubo dos veces, no una por voto, y el último cambio nunca se pierde.
 */
export function limitar(fn: () => void, intervaloMs: number): { avisar: () => void; cerrar: () => void } {
  let espera: ReturnType<typeof setTimeout> | null = null;
  let pendiente = false;
  const abrirVentana = () => {
    espera = setTimeout(() => {
      espera = null;
      if (!pendiente) return;
      pendiente = false;
      fn();
      abrirVentana();
    }, intervaloMs);
  };
  return {
    avisar() {
      if (espera) {
        pendiente = true;
        return;
      }
      fn();
      abrirVentana();
    },
    cerrar() {
      if (espera) clearTimeout(espera);
      espera = null;
      pendiente = false;
    },
  };
}
