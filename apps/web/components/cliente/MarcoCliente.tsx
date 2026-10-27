'use client';

import type { ReactNode } from 'react';
import { useAhora } from '@/lib/api/hooks-cliente';
import { useConexionCaida } from '@/lib/tiempo-real/estado';
import { conexion } from '@/lib/textos/cliente';
import { PieCliente } from './PieCliente';

/**
 * El mueble del teléfono (doc 05, «Responsive»): una columna de 100dvh; desde 768 px se centra
 * sobre la madera. Con los pedidos cerrados la luz del arco se apaga también en el fondo.
 */
export function MarcoCliente({ slug, pie = false, children }: { slug: string; pie?: boolean; children: ReactNode }) {
  const { data: ahora } = useAhora(slug);
  const apagada = ahora ? ahora.servicio.estado !== 'abierto' : false;
  return (
    <div className={apagada ? 'madera madera--apagada' : 'madera'}>
      <div className="mx-auto flex min-h-dvh w-full max-w-[30rem] flex-col">
        <BannerConexion />
        <main className="flex flex-1 flex-col px-4 pt-[22px] pb-6">{children}</main>
        {pie ? <PieCliente slug={slug} /> : null}
      </div>
    </div>
  );
}

/** Sale si la conexión en vivo lleva 5 s caída (doc 03, *propuesto*). */
function BannerConexion() {
  const { caida, estado } = useConexionCaida();
  return (
    <p role="status" className={caida ? 'sticky top-0 z-10 flex items-center gap-2 bg-cereza px-4 py-2 text-[14px] font-medium text-crema' : 'solo-lector'}>
      {caida ? (
        <>
          <span aria-hidden="true" className="h-2 w-2 shrink-0 rounded-full bg-crema/70" />
          {estado === 'desconectado' ? conexion.desconectado : conexion.reconectando}
        </>
      ) : null}
    </p>
  );
}
