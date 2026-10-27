import { Arco, Ventana } from '@rockola/ui';
import type { ReactNode } from 'react';

/**
 * Pantalla de aviso (no encontrado, error, sin bar): el arco apagado con su mensaje. Toda
 * pantalla de aviso lleva su h1 (doc 05, «Accesibilidad»).
 */
export function AvisoPantalla({ titulo, children, accion, encima }: { titulo: string; children?: ReactNode; accion?: ReactNode; encima?: ReactNode }) {
  return (
    <main className="madera madera--apagada flex min-h-dvh flex-col items-center px-4 pt-12 pb-8">
      <div className="flex w-full max-w-[26rem] flex-1 flex-col">
        {encima}
        <Arco apagado className="mt-6" aria-label="Aviso">
          <div className="px-[18px] pt-[104px] pb-[18px]">
            <Ventana className="px-4 pt-4 pb-[18px]">
              <h1 className="condensada text-[34px] leading-[.98]">{titulo}</h1>
              {children ? <div className="mt-[10px] text-[15px] leading-[1.4] text-tinta-2">{children}</div> : null}
            </Ventana>
          </div>
        </Arco>
        {accion ? <div className="mt-auto pt-10">{accion}</div> : null}
      </div>
    </main>
  );
}
