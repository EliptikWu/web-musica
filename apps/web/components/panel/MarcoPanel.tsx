'use client';

import type { YoPanel } from '@rockola/contrato';
import { Icono } from '@rockola/ui';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { createContext, type ReactNode, useContext, useEffect } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { codigoDe } from '@/lib/api/errores';
import { useTiempoRealBar } from '@/lib/api/hooks-cliente';
import { useSalir, useYoPanel } from '@/lib/api/hooks-panel';
import { errores, P, ROL } from '@/lib/textos/panel';
import { useConexionCaida } from '@/lib/tiempo-real/estado';
import { conexion } from '@/lib/textos/cliente';

const ContextoPanel = createContext<YoPanel | null>(null);

/** Quién entró al panel y de qué bar. Solo existe dentro de `MarcoPanel`. */
export function useYoDelPanel(): YoPanel {
  const yo = useContext(ContextoPanel);
  if (!yo) throw new Error('useYoDelPanel fuera de MarcoPanel');
  return yo;
}

/**
 * Lo común a las pantallas del panel con sesión: comprueba la sesión con el servidor (la
 * cookie la revisó el proxy, pero quien decide es `GET /panel/yo`), manda a cambiar la clave
 * si toca, y pone la cabecera y la conexión en vivo del bar.
 */
export function MarcoPanel({ children }: { children: ReactNode }) {
  const yo = useYoPanel();
  const router = useRouter();
  const codigo = codigoDe(yo.error);

  useEffect(() => {
    if (codigo === 'no_autenticado') router.replace('/panel/entrar?motivo=caducada');
    if (codigo === 'debe_cambiar_clave' || yo.data?.usuario.debe_cambiar_clave) router.replace('/panel/cambiar-clave');
  }, [codigo, router, yo.data]);

  if (yo.isError && codigo !== 'no_autenticado' && codigo !== 'debe_cambiar_clave') {
    return (
      <div className="madera min-h-dvh px-4 py-10">
        <div className="mx-auto max-w-[30rem]">
          <Aviso tipo="error" titulo="El panel no cargó" meta={errores.generico} accion={<button type="button" onClick={() => void yo.refetch()}>Reintentar</button>} />
        </div>
      </div>
    );
  }
  if (!yo.data || yo.data.usuario.debe_cambiar_clave) {
    return (
      <div className="madera min-h-dvh px-4 py-6" aria-busy="true">
        <p className="solo-lector" role="status">
          Cargando el panel…
        </p>
        <div className="esqueleto mx-auto h-12 max-w-[1376px]" />
        <div className="esqueleto mx-auto mt-4 h-24 max-w-[1376px] rounded-marco" />
      </div>
    );
  }

  return (
    <ContextoPanel.Provider value={yo.data}>
      <PanelConVivo yo={yo.data}>{children}</PanelConVivo>
    </ContextoPanel.Provider>
  );
}

function PanelConVivo({ yo, children }: { yo: YoPanel; children: ReactNode }) {
  useTiempoRealBar(yo.establecimiento.slug);
  const { caida } = useConexionCaida();
  return (
    <div className="madera min-h-dvh">
      <p role="status" className={caida ? 'bg-cereza px-4 py-2 text-[14px] font-medium text-crema sm:px-8' : 'solo-lector'}>
        {caida ? conexion.reconectando : null}
      </p>
      <div className="mx-auto max-w-[1440px] px-4 pt-5 pb-8 sm:px-8">
        <CabezaPanel yo={yo} />
        <main>{children}</main>
      </div>
    </div>
  );
}

function CabezaPanel({ yo }: { yo: YoPanel }) {
  const ruta = usePathname();
  const salir = useSalir();
  const inicial = yo.usuario.nombre.trim().charAt(0).toUpperCase() || '?';
  return (
    <header className="panel-cabeza flex-wrap">
      <div className="panel-marca">
        <p className="placa panel-marca__bar">{yo.establecimiento.nombre}</p>
        <p className="secundario hidden text-[15px] sm:block">{P.panelDeLaRockola}</p>
      </div>
      <nav className="panel-nav flex-wrap" aria-label="Panel">
        <Link href="/panel" aria-current={ruta === '/panel' ? 'page' : undefined}>
          <Icono nombre="reproducir" />
          {P.operacion}
        </Link>
        <span className="panel-nav__usuario inline-flex items-center gap-2">
          <span className="panel-usuario inline-flex h-11 items-center gap-2 rounded-dia">
            <span className="panel-usuario__inicial" aria-hidden="true">
              {inicial}
            </span>
            <span className="leading-tight">
              {yo.usuario.nombre}
              <span className="panel-usuario__rol block">{ROL[yo.usuario.rol]}</span>
            </span>
          </span>
          <button type="button" aria-busy={salir.isPending || undefined} onClick={() => salir.mutate()}>
            <Icono nombre="salir" />
            {P.salir}
          </button>
        </span>
      </nav>
      {salir.isError ? (
        <Aviso tipo="error" className="w-full" titulo="No se pudo cerrar la sesión" meta="La sesión sigue abierta en este equipo. Revisa la conexión e inténtalo de nuevo." />
      ) : null}
    </header>
  );
}
