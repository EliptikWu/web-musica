'use client';

import type { Rol } from '@rockola/contrato';
import { Icono, type NombreIcono } from '@rockola/ui';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useRef } from 'react';
import { type Accion, puede } from '@/lib/permisos';
import { P } from '@/lib/textos/panel';

type Seccion = { href: string; texto: string; icono: NombreIcono; permiso: Accion };
type Grupo = { texto: string; icono: NombreIcono; secciones: Seccion[] };

/**
 * Las secciones en grupos (punto 3 del equipo, 2026-11-25): eran 11 pestañas seguidas. El orden
 * va de lo de cada noche a lo de vez en cuando; Música queda junto a Análisis porque «Canciones»
 * corrige el género que lee el tablero (punto 14).
 */
const GRUPOS: Grupo[] = [
  {
    texto: 'Noche',
    icono: 'reproducir',
    secciones: [
      { href: '/panel', texto: P.operacion, icono: 'reproducir', permiso: 'verOperacion' },
      { href: '/panel/horario', texto: 'Horario', icono: 'reloj', permiso: 'gestion' },
      { href: '/panel/eventos', texto: 'Eventos', icono: 'calendario', permiso: 'gestion' },
      { href: '/panel/bloqueos', texto: 'Bloqueos', icono: 'bloquear', permiso: 'desbloquear' },
    ],
  },
  {
    texto: 'Bar',
    icono: 'qr',
    secciones: [
      { href: '/panel/establecimiento', texto: 'Bar y QR', icono: 'qr', permiso: 'gestion' },
      { href: '/panel/usuarios', texto: 'Personal', icono: 'usuario', permiso: 'gestion' },
      { href: '/panel/bitacora', texto: 'Bitácora', icono: 'libro', permiso: 'bitacora' },
    ],
  },
  {
    texto: 'Música',
    icono: 'lista',
    secciones: [
      { href: '/panel/listas', texto: 'Listas de la casa', icono: 'lista', permiso: 'gestion' },
      { href: '/panel/canciones', texto: 'Canciones', icono: 'pedidos', permiso: 'gestion' },
    ],
  },
  { texto: 'Análisis', icono: 'grafica', secciones: [{ href: '/panel/analitica', texto: 'Tablero analítico', icono: 'grafica', permiso: 'analitica' }] },
  { texto: 'Manual', icono: 'libro', secciones: [{ href: '/panel/manual', texto: 'Manual de uso', icono: 'libro', permiso: 'verOperacion' }] },
];

const esActual = (href: string, ruta: string) => (href === '/panel' ? ruta === '/panel' : ruta === href || ruta.startsWith(`${href}/`));

/**
 * Las secciones del panel que el rol puede abrir (D9); las demás no se muestran. Arriba, los
 * grupos; debajo, las secciones del grupo actual. Todo son enlaces (sin menús que abrir): se
 * usa igual con el dedo, el teclado y un lector de pantalla. Un grupo con una sola sección
 * visible enlaza directo a ella y lleva su nombre (el Operador ve «Operación» y «Manual de uso»).
 */
export function NavPanel({ rol }: { rol: Rol }) {
  const ruta = usePathname();
  const nav = useRef<HTMLElement>(null);
  const grupos = GRUPOS.map((g) => ({ ...g, secciones: g.secciones.filter((s) => puede(rol, s.permiso)) })).filter((g) => g.secciones.length > 0);
  const actual = grupos.find((g) => g.secciones.some((s) => esActual(s.href, ruta)));
  // En el teléfono las pestañas se desplazan de lado: la actual queda a la vista, sin mover la página.
  useEffect(() => {
    const barra = nav.current?.querySelector<HTMLElement>('.panel-subnav');
    const elegida = barra?.querySelector<HTMLElement>('[aria-current]');
    if (!barra || !elegida || barra.scrollWidth <= barra.clientWidth) return;
    const b = barra.getBoundingClientRect();
    const a = elegida.getBoundingClientRect();
    barra.scrollLeft += a.left - b.left - (b.width - a.width) / 2;
  }, [ruta]);
  return (
    <nav ref={nav} className="no-imprimir" aria-label="Secciones del panel">
      <div className="panel-subnav">
        {grupos.map((g) => {
          const sola = g.secciones.length === 1 ? g.secciones[0]! : null;
          const enEste = g === actual;
          return (
            <Link
              key={g.texto}
              href={g.secciones[0]!.href}
              aria-current={enEste ? (sola && esActual(sola.href, ruta) && ruta === sola.href ? 'page' : 'true') : undefined}
            >
              <Icono nombre={sola?.icono ?? g.icono} />
              {sola?.texto ?? g.texto}
            </Link>
          );
        })}
      </div>
      {actual && actual.secciones.length > 1 ? (
        <div className="panel-subnav panel-subnav--secciones" role="group" aria-label={`Secciones de ${actual.texto}`}>
          {actual.secciones.map((s) => (
            <Link key={s.href} href={s.href} aria-current={esActual(s.href, ruta) ? 'page' : undefined}>
              <Icono nombre={s.icono} />
              {s.texto}
            </Link>
          ))}
        </div>
      ) : null}
    </nav>
  );
}
