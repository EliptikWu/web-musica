'use client';

import type { ReactNode } from 'react';
import type { Ahora } from '@rockola/contrato';
import { useAhora, useBar } from '@/lib/api/hooks-cliente';
import { formatoHora } from '@/lib/formato';
import { minutosParaCerrar } from '@/lib/reglas/horario';
import { useAhoraServidor } from '@/lib/reloj-servidor';
import { useConexionCaida } from '@/lib/tiempo-real/estado';
import { avisoCierre, conexion } from '@/lib/textos/cliente';
import { PieCliente } from './PieCliente';

/**
 * El mueble del teléfono (doc 05, «Responsive»): una columna de 100dvh; desde 768 px se centra
 * sobre la madera. Con los pedidos cerrados la luz del arco se apaga también en el fondo.
 */
export function MarcoCliente({ slug, pie = false, children }: { slug: string; pie?: boolean; children: ReactNode }) {
  const { data: ahora } = useAhora(slug);
  const zona = useBar(slug).data?.zona_horaria;
  const apagada = ahora ? ahora.servicio.estado !== 'abierto' : false;
  return (
    <div className={apagada ? 'madera madera--apagada' : 'madera'}>
      <div className="mx-auto flex min-h-dvh w-full max-w-[30rem] flex-col">
        {/* Un solo bloque pegado arriba: los dos avisos se apilan en vez de taparse. */}
        <div className="sticky top-0 z-10">
          <BannerConexion />
          {ahora ? <BannerCierre ahora={ahora} zona={zona} /> : null}
        </div>
        <main className="flex flex-1 flex-col px-4 pt-[22px] pb-6">{children}</main>
        {pie ? <PieCliente slug={slug} /> : null}
      </div>
    </div>
  );
}

/** Punto 6: en los últimos minutos antes de que el horario cierre los pedidos, se avisa arriba. */
function BannerCierre({ ahora, zona }: { ahora: Ahora; zona: string | undefined }) {
  const instante = useAhoraServidor();
  const s = ahora.servicio;
  const minutos = s.estado === 'abierto' && s.modo_qr !== 'forzado_activo' ? minutosParaCerrar(s.hasta, instante) : null;
  if (minutos === null || !s.hasta) return null;
  const { anuncio, cuenta } = avisoCierre(minutos, formatoHora(s.hasta, zona));
  // El lector de pantalla oye la hora una vez; la cuenta regresiva cambia cada minuto y no se lee.
  return (
    <p className="banner-cierre px-4 py-2 text-[14px] font-medium">
      <span role="status">{anuncio}</span> <span aria-hidden="true">{cuenta}</span>
    </p>
  );
}

/** Sale si la conexión en vivo lleva 5 s caída (doc 03, *propuesto*). */
function BannerConexion() {
  const { caida, estado } = useConexionCaida();
  return (
    <p role="status" className={caida ? 'banner-conexion flex items-center gap-2 px-4 py-2 text-[14px] font-medium' : 'solo-lector'}>
      {caida ? (
        <>
          <span aria-hidden="true" className="banner-conexion__punto h-2 w-2 shrink-0 rounded-full" />
          {estado === 'desconectado' ? conexion.desconectado : conexion.reconectando}
        </>
      ) : null}
    </p>
  );
}
