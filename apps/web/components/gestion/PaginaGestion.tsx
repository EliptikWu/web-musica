'use client';

import { claseTecla } from '@rockola/ui';
import Link from 'next/link';
import type { ReactNode } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { codigoDe } from '@/lib/api/errores';
import { type Accion, puede } from '@/lib/permisos';
import { entrada, errores } from '@/lib/textos/panel';

/**
 * La puerta de cada ruta de gestión: quien no tiene el permiso ve por qué y cómo volver, y la
 * pantalla ni se monta, así que no pide nada (el servidor igual respondería 403).
 */
export function ConPermiso({ permiso = 'gestion', children }: { permiso?: Accion; children: ReactNode }) {
  const yo = useYoDelPanel();
  if (!puede(yo.usuario.rol, permiso)) {
    return (
      <section className="mx-auto mt-10 max-w-[32rem] text-center">
        <h1 className="placa text-[24px]">{entrada.sinPermisoTitulo}</h1>
        <p className="secundario mt-3">{entrada.sinPermisoTexto}</p>
        <Link href="/panel" className={claseTecla('cereza', 'ancha', 'mt-6')}>
          {entrada.volver}
        </Link>
      </section>
    );
  }
  return <>{children}</>;
}

/** Lo común a cada pantalla de gestión: título, una frase de qué es y una acción principal. */
export function PaginaGestion({ titulo, nota, accion, children }: { titulo: string; nota?: string; accion?: ReactNode; children: ReactNode }) {
  return (
    <>
      <div className="gestion-cabeza">
        <div>
          <h1 className="placa">{titulo}</h1>
          {nota ? <p>{nota}</p> : null}
        </div>
        {accion}
      </div>
      {children}
    </>
  );
}

/** Mientras carga, si falla (con reintentar) o el contenido. */
export function Cargando({ consulta, children }: { consulta: { isError: boolean; error: unknown; data: unknown; refetch: () => unknown }; children: () => ReactNode }) {
  if (consulta.data !== undefined) return <>{children()}</>;
  if (consulta.isError) {
    return (
      <Aviso
        tipo="error"
        className="mt-6"
        titulo={codigoDe(consulta.error) === 'sin_permiso' ? errores.sinPermiso : 'No cargó'}
        meta={errores.generico}
        accion={
          <button type="button" onClick={() => void consulta.refetch()}>
            Reintentar
          </button>
        }
      />
    );
  }
  return (
    <div className="mt-6 grid gap-3" aria-busy="true">
      <p className="solo-lector" role="status">
        Cargando…
      </p>
      <div className="esqueleto h-16 rounded-caja" />
      <div className="esqueleto h-16 rounded-caja" />
    </div>
  );
}

/** El mensaje de un error de una acción de gestión, sin tecnicismos. */
export function mensajeDeError(error: unknown, porCodigo: Partial<Record<string, string>> = {}): string {
  const codigo = codigoDe(error);
  return porCodigo[codigo] ?? (codigo === 'sin_permiso' ? errores.sinPermiso : errores.generico);
}
