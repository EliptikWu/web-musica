'use client';

import type { Rol } from '@rockola/contrato';
import { Icono, type NombreIcono } from '@rockola/ui';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useRef } from 'react';
import { type Accion, puede } from '@/lib/permisos';
import { P } from '@/lib/textos/panel';

const SECCIONES: { href: string; texto: string; icono: NombreIcono; permiso: Accion }[] = [
  { href: '/panel', texto: P.operacion, icono: 'reproducir', permiso: 'verOperacion' },
  { href: '/panel/horario', texto: 'Horario', icono: 'reloj', permiso: 'gestion' },
  { href: '/panel/listas', texto: 'Listas de la casa', icono: 'lista', permiso: 'gestion' },
  { href: '/panel/bloqueos', texto: 'Bloqueos', icono: 'bloquear', permiso: 'desbloquear' },
  { href: '/panel/bitacora', texto: 'Bitácora', icono: 'libro', permiso: 'bitacora' },
  { href: '/panel/usuarios', texto: 'Personal', icono: 'usuario', permiso: 'gestion' },
  { href: '/panel/establecimiento', texto: 'Bar y QR', icono: 'qr', permiso: 'gestion' },
  { href: '/panel/eventos', texto: 'Eventos', icono: 'calendario', permiso: 'gestion' },
  { href: '/panel/manual', texto: 'Manual de uso', icono: 'libro', permiso: 'verOperacion' },
];

/** Las secciones del panel que el rol puede abrir (D9); las demás no se muestran. */
export function NavPanel({ rol }: { rol: Rol }) {
  const ruta = usePathname();
  const nav = useRef<HTMLElement>(null);
  // En el teléfono las pestañas se desplazan de lado: la actual queda a la vista, sin mover la página.
  useEffect(() => {
    const barra = nav.current;
    const actual = barra?.querySelector<HTMLElement>('[aria-current="page"]');
    if (!barra || !actual || barra.scrollWidth <= barra.clientWidth) return;
    const b = barra.getBoundingClientRect();
    const a = actual.getBoundingClientRect();
    barra.scrollLeft += a.left - b.left - (b.width - a.width) / 2;
  }, [ruta]);
  return (
    <nav ref={nav} className="panel-subnav no-imprimir" aria-label="Secciones del panel">
      {SECCIONES.filter((s) => puede(rol, s.permiso)).map((s) => {
        const actual = s.href === '/panel' ? ruta === '/panel' : ruta === s.href || ruta.startsWith(`${s.href}/`);
        return (
          <Link key={s.href} href={s.href} aria-current={actual ? 'page' : undefined}>
            <Icono nombre={s.icono} />
            {s.texto}
          </Link>
        );
      })}
    </nav>
  );
}
