'use client';

import { type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes, useEffect, useId, useRef } from 'react';
import { Icono } from './Icono';

/** Campo con etiqueta visible, ayuda y error unidos por `aria-describedby` (doc 05). */
export function Campo({
  etiqueta,
  ayuda,
  error,
  multilinea = false,
  claseEntrada,
  invalido = false,
  ...resto
}: {
  etiqueta: string;
  ayuda?: string;
  error?: string | null;
  multilinea?: boolean;
  /** Clase extra de la entrada (p. ej. `campo__entrada--hora`). */
  claseEntrada?: string;
  /** Marca la entrada como inválida aunque el mensaje esté en otro lado (p. ej. un tramo del horario). */
  invalido?: boolean;
} & InputHTMLAttributes<HTMLInputElement> &
  TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const id = useId();
  const describe = [ayuda && `${id}-ayuda`, error && `${id}-error`].filter(Boolean).join(' ') || undefined;
  const comunes = { id, className: claseEntrada ? `campo__entrada ${claseEntrada}` : 'campo__entrada', 'aria-invalid': error || invalido ? true : undefined, 'aria-describedby': describe };
  return (
    <div className="campo">
      <label className="campo__etiqueta" htmlFor={id}>
        {etiqueta}
      </label>
      {ayuda ? (
        <p id={`${id}-ayuda`} className="campo__ayuda">
          {ayuda}
        </p>
      ) : null}
      {multilinea ? <textarea {...(resto as TextareaHTMLAttributes<HTMLTextAreaElement>)} {...comunes} /> : <input {...(resto as InputHTMLAttributes<HTMLInputElement>)} {...comunes} />}
      {error ? (
        <p id={`${id}-error`} className="campo__error">
          <Icono nombre="alerta" />
          {error}
        </p>
      ) : null}
    </div>
  );
}

/**
 * Lista desplegable con etiqueta visible unida por `id`: su nombre accesible es solo la
 * etiqueta (envuelta en el `<label>`, algunos lectores le suman las opciones).
 */
export function Selector({ etiqueta, children, ...resto }: { etiqueta: string; children: ReactNode } & SelectHTMLAttributes<HTMLSelectElement>) {
  const id = useId();
  return (
    <div className="campo">
      <label className="campo__etiqueta" htmlFor={id}>
        {etiqueta}
      </label>
      <select {...resto} id={id} className="campo__entrada">
        {children}
      </select>
    </div>
  );
}

/**
 * Una opción de `GrupoTeclas`. Con `figura` (una vista previa, decorativa) y `detalle` (una
 * frase que la describe) el grupo se dibuja en columnas: figura, tecla y detalle (RF-059).
 */
export type Opcion<V extends string> = { valor: V; texto: string; figura?: ReactNode; detalle?: string };

/** Opciones excluyentes como teclas: la elegida queda hundida. Es un grupo de radios de verdad. */
export function GrupoTeclas<V extends string>({
  leyenda,
  nombre,
  opciones,
  valor,
  onCambio,
  leyendaOculta = false,
}: {
  leyenda: string;
  nombre: string;
  opciones: Opcion<V>[];
  valor: V;
  onCambio: (valor: V) => void;
  /** Cuando un título visible ya nombra el grupo: la leyenda queda solo para lectores de pantalla. */
  leyendaOculta?: boolean;
}) {
  const id = useId();
  const conFiguras = opciones.some((o) => o.figura);
  return (
    <fieldset className={conFiguras ? 'grupo-teclas grupo-teclas--figuras' : 'grupo-teclas'}>
      <legend className={leyendaOculta ? 'solo-lector' : undefined}>{leyenda}</legend>
      {opciones.map((o) => (
        <label key={o.valor}>
          <input
            type="radio"
            name={nombre}
            value={o.valor}
            checked={valor === o.valor}
            onChange={() => onCambio(o.valor)}
            aria-describedby={o.detalle ? `${id}-${o.valor}` : undefined}
          />
          {o.figura ? (
            <span className="grupo-teclas__figura" aria-hidden="true">
              {o.figura}
            </span>
          ) : null}
          <span className="tecla" aria-hidden="true">
            {o.texto}
          </span>
          <span className="solo-lector">{o.texto}</span>
          {o.detalle ? (
            <span className="grupo-teclas__detalle" id={`${id}-${o.valor}`}>
              {o.detalle}
            </span>
          ) : null}
        </label>
      ))}
    </fieldset>
  );
}

/**
 * Diálogo modal con el `<dialog>` del navegador: atrapa el foco, Escape lo cierra y devuelve
 * el foco a quien lo abrió. Se abre y se cierra con `abierto`.
 */
export function Dialogo({ abierto, titulo, onCerrar, children }: { abierto: boolean; titulo: string; onCerrar: () => void; children: ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null);
  const idTitulo = useId();
  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (abierto && !d.open) d.showModal();
    if (!abierto && d.open) d.close();
  }, [abierto]);
  return (
    <dialog
      ref={ref}
      className="dialogo"
      aria-labelledby={idTitulo}
      onCancel={(e) => {
        e.preventDefault();
        onCerrar();
      }}
    >
      {abierto ? (
        <div className="dialogo__cuerpo">
          <h2 id={idTitulo} className="dialogo__titulo">
            {titulo}
          </h2>
          {children}
        </div>
      ) : null}
    </dialog>
  );
}
