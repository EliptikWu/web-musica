import type { ReactNode } from 'react';

type Props = {
  tipo?: 'luz' | 'error' | 'info';
  titulo?: string;
  meta?: ReactNode;
  accion?: ReactNode;
  className?: string;
};

/**
 * Aviso dentro de una pantalla (doc 05): ámbar cuando algo salió bien y es tuyo, cereza cuando
 * falló, madera para información. Se anuncia a los lectores de pantalla: el error con `alert`,
 * lo demás con `status`.
 */
export function Aviso({ tipo = 'luz', titulo, meta, accion, className }: Props) {
  const clase = ['aviso', tipo === 'error' && 'aviso--error', tipo === 'info' && 'aviso--info', className].filter(Boolean).join(' ');
  return (
    <div className={clase} role={tipo === 'error' ? 'alert' : 'status'}>
      <div className="aviso__texto">
        {titulo ? <p className="aviso__titulo">{titulo}</p> : null}
        {meta ? <p className="aviso__meta">{meta}</p> : null}
      </div>
      {accion}
    </div>
  );
}
