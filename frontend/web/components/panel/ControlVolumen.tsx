import { Tecla } from '@rockola/ui';
import { useEffect, useId, useRef, useState } from 'react';
import { P } from '@/lib/textos/panel';

const PASO = 10;
const ESPERA_MS = 300;

/**
 * Volumen de la pantalla del bar (RF-062). Las teclas mueven de 10 en 10; la barra, de 1 en 1.
 * Se ve el valor al instante y al servidor va el último, 300 ms después de dejar de moverlo:
 * una barra arrastrada no manda una petición por paso (el servidor ignora un valor repetido).
 * Si el volumen cambia desde otro panel se toma el nuevo, salvo mientras se mueve aquí; si el
 * envío falla, vuelve al que tiene la TV. Las teclas no se apagan en los extremos para no
 * perder el foco (WCAG 2.4.3): en 100, «Subir» no hace nada.
 */
export function ControlVolumen({ volumen, onCambiar }: { volumen: number; onCambiar: (v: number, alFallar: () => void) => void }) {
  const [valor, setValor] = useState(volumen);
  const pendiente = useRef<number | null>(null);
  const temporizador = useRef<ReturnType<typeof setTimeout> | null>(null);
  const delServidor = useRef(volumen);
  const enviar = useRef(onCambiar);
  const id = useId();

  useEffect(() => {
    delServidor.current = volumen;
    enviar.current = onCambiar;
    if (pendiente.current === null) setValor(volumen);
  }, [volumen, onCambiar]);

  // Lo pendiente se manda aunque se salga de la pantalla antes de los 300 ms.
  useEffect(
    () => () => {
      if (temporizador.current) clearTimeout(temporizador.current);
      if (pendiente.current !== null) enviar.current(pendiente.current, () => {});
    },
    [],
  );

  function despachar() {
    const v = pendiente.current;
    pendiente.current = null;
    temporizador.current = null;
    if (v !== null) enviar.current(v, () => setValor(delServidor.current));
  }

  function cambiar(v: number) {
    const nuevo = Math.max(0, Math.min(100, Math.round(v)));
    if (nuevo === valor && pendiente.current === null) return;
    setValor(nuevo);
    pendiente.current = nuevo;
    if (temporizador.current) clearTimeout(temporizador.current);
    temporizador.current = setTimeout(despachar, ESPERA_MS);
  }

  return (
    <div className="volumen">
      <label htmlFor={id} className="volumen__etiqueta">
        {P.volumen}
      </label>
      <div className="volumen__mandos">
        <Tecla tamano="chica" aria-label={P.bajarVolumenEtiqueta} onClick={() => cambiar(valor - PASO)}>
          {P.bajarVolumen}
        </Tecla>
        <input id={id} type="range" min={0} max={100} step={1} value={valor} aria-valuetext={valor === 0 ? P.silencio : `${valor} %`} onChange={(e) => cambiar(Number(e.target.value))} />
        <Tecla tamano="chica" aria-label={P.subirVolumenEtiqueta} onClick={() => cambiar(valor + PASO)}>
          {P.subirVolumen}
        </Tecla>
        <output htmlFor={id} className="volumen__valor cifra" aria-hidden="true">
          {valor === 0 ? P.silencio : `${valor} %`}
        </output>
      </div>
    </div>
  );
}
