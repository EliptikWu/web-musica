'use client';

import { Campo, Selector } from '@rockola/ui';
import type { CausaNoSono, NombreEndpoint, OrigenDatos, Respuesta } from '@rockola/contrato';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import { type ReactNode, useState } from 'react';
import { PaginaGestion } from '@/components/gestion/PaginaGestion';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { capitalizar, conPunto, DIAS, fechaYHora } from '@/lib/formato';

// ── Formatos ───────────────────────────────────────────────────────────────

const enteros = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });
/** 1284 → «1.284»; 50.1 → «50,1». */
export const numero = (n: number) => enteros.format(n);

const GENEROS: Record<string, string> = { salsa: 'Salsa', urbano: 'Urbano', pop: 'Pop', tropical: 'Tropical', vallenato: 'Vallenato' };
export const nombreGenero = (g: string | null) => (g === null ? 'Sin clasificar' : (GENEROS[g] ?? capitalizar(g)));

/** 20 → «8 p. m.»; 0 → «12 a. m.». */
export const horaRedonda = (h: number) => `${h % 12 === 0 ? 12 : h % 12} ${h < 12 ? 'a. m.' : 'p. m.'}`;

export const CAUSAS: Record<CausaNoSono, string> = {
  omitida: 'La saltaron',
  bloqueada: 'Estaba bloqueada',
  fuera_de_tiempo: 'No alcanzó: cerró el bar',
  retirada: 'La retiró el personal',
  no_disponible: 'YouTube no la pudo reproducir',
};

/** «sábado 17 de octubre». */
export function fechaNoche(iso: string, zona: string): string {
  return new Date(iso).toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long', timeZone: zona });
}

/** «17 de octubre»: para los rangos, donde el día de la semana sobra. */
export function fechaSinDia(iso: string, zona: string): string {
  return new Date(iso).toLocaleDateString('es-CO', { day: 'numeric', month: 'long', timeZone: zona });
}

/** «sáb 17 oct»: para rótulos cortos (ejes, tablas). */
export function fechaCorta(iso: string, zona: string): string {
  const d = new Date(iso);
  const parte = (o: Intl.DateTimeFormatOptions) => d.toLocaleDateString('es-CO', { ...o, timeZone: zona }).replace(/\.$/, '');
  return `${parte({ weekday: 'short' })} ${parte({ day: 'numeric' })} ${parte({ month: 'short' })}`;
}

// ── Filtro (en la URL: lo comparten las partes del tablero) ────────────────

export type FiltroUrl = { noche?: string; desde?: string; hasta?: string; dia?: string };

/** El filtro de la URL. El día de la semana solo lo usa Participación (`conDia`). */
export function useFiltroAnalitica(conDia = false) {
  const sp = useSearchParams();
  const router = useRouter();
  const ruta = usePathname();
  const filtro: FiltroUrl = {
    noche: sp.get('noche') ?? undefined,
    desde: sp.get('desde') ?? undefined,
    hasta: sp.get('hasta') ?? undefined,
    dia: sp.get('dia') ?? undefined,
  };
  const cambiar = (nuevo: FiltroUrl) => {
    const p = new URLSearchParams();
    for (const [k, v] of Object.entries(nuevo)) if (v) p.set(k, v);
    router.replace(p.size > 0 ? `${ruta}?${p}` : ruta, { scroll: false });
  };
  /** Lo que se manda a la API. */
  const consulta = { noche: filtro.noche, desde: filtro.desde, hasta: filtro.hasta, dia_semana: conDia ? filtro.dia : undefined };
  return { filtro, cambiar, consulta, query: sp.toString() };
}

type ConFiltro = 'resumenAnalitica' | 'participacionAnalitica' | 'diagnosticoAnalitica' | 'recomendacionesAnalitica';

/** Una parte del tablero con el filtro de la URL; mientras recarga, se queda lo anterior. */
export function useAnalitica<N extends ConFiltro>(nombre: N, consulta: ReturnType<typeof useFiltroAnalitica>['consulta']) {
  return useQuery<Respuesta<N>>({
    queryKey: claves.panel.analitica(nombre, consulta),
    queryFn: ({ signal }) => (pedir as (n: NombreEndpoint, o: object) => Promise<Respuesta<N>>)(nombre, { consulta, signal }),
    placeholderData: keepPreviousData,
    retry: false,
  });
}

export function useNoches() {
  return useQuery({ queryKey: claves.panel.analitica('nochesAnalitica'), queryFn: ({ signal }) => pedir('nochesAnalitica', { signal }), staleTime: 5 * 60_000 });
}

// ── Armazón ────────────────────────────────────────────────────────────────

const PARTES = [
  { href: '/panel/analitica', texto: 'Resumen' },
  { href: '/panel/analitica/participacion', texto: 'Participación' },
  { href: '/panel/analitica/diagnostico', texto: 'Diagnóstico' },
  { href: '/panel/analitica/comparar', texto: 'Comparar' },
  { href: '/panel/analitica/recomendaciones', texto: 'Recomendaciones' },
];

/** Título, las partes del tablero (conservan el filtro) y la parte elegida. */
export function MarcoAnalitica({ children }: { children: ReactNode }) {
  const ruta = usePathname();
  const { query } = useFiltroAnalitica();
  // El día de la semana es de Participación: a las demás partes no se lleva (no tienen su control).
  const sinDia = new URLSearchParams(query);
  sinDia.delete('dia');
  const enlace = (href: string) => {
    if (href === '/panel/analitica/comparar') return href;
    const q = href === '/panel/analitica/participacion' ? query : sinDia.toString();
    return q ? `${href}?${q}` : href;
  };
  return (
    <PaginaGestion titulo="Tablero analítico" nota="Cómo funcionaron las noches cerradas: lo que se pidió, lo que sonó y qué conviene ajustar.">
      <nav className="analitica-nav" aria-label="Partes del tablero">
        {PARTES.map((p) => (
          <Link key={p.href} href={enlace(p.href)} aria-current={ruta === p.href ? 'page' : undefined}>
            {p.texto}
          </Link>
        ))}
      </nav>
      {children}
    </PaginaGestion>
  );
}

/** De dónde salen las cifras y de cuándo son (RNF-015). */
export function OrigenDeLosDatos({ datos }: { datos: OrigenDatos }) {
  const zona = useYoDelPanel().establecimiento.zona_horaria;
  const rango =
    datos.desde && datos.hasta
      ? datos.noches === 1
        ? `La noche del ${fechaNoche(datos.desde, zona)}.`
        : `${numero(datos.noches)} noches, del ${fechaSinDia(datos.desde, zona)} al ${fechaSinDia(datos.hasta, zona)}.`
      : 'Ninguna noche con este filtro.';
  return (
    <p className="analitica-origen">
      {datos.origen === 'ejemplo' ? <span className="etiqueta">Datos de ejemplo</span> : null}
      <span>{rango}</span>
      <span>
        {conPunto(`Última carga: ${fechaYHora(datos.actualizado_en, zona)}`)} La noche en curso entra cuando se cierra.
      </span>
    </p>
  );
}

const ultimas4Semanas = (ultima: string) => {
  const hasta = ultima.slice(0, 10);
  const desde = new Date(Date.parse(`${hasta}T12:00:00Z`) - 27 * 86_400_000).toISOString().slice(0, 10);
  return { desde, hasta };
};

/**
 * La fila de filtros, arriba de todo lo que filtra: atajos primero (todas, las últimas cuatro
 * semanas, una noche) y las fechas a mano al final. Con `conDia`, también el día de la semana;
 * con `sinNoche` (comparar por grupos), sin la opción de una sola noche.
 */
export function Filtros({ conDia = false, sinNoche = false }: { conDia?: boolean; sinNoche?: boolean }) {
  const zona = useYoDelPanel().establecimiento.zona_horaria;
  const { filtro: completo, cambiar: cambiarUrl } = useFiltroAnalitica(conDia);
  const filtro = { ...completo, noche: sinNoche ? undefined : completo.noche, dia: conDia ? completo.dia : undefined };
  const cambiar = (nuevo: FiltroUrl) => cambiarUrl({ ...nuevo, dia: conDia ? nuevo.dia : undefined });
  const noches = useNoches();
  const lista = noches.data?.noches ?? [];
  const ultimaFecha = lista.at(-1) ? localDe(lista.at(-1)!.inicio, zona) : null;
  const atajo = ultimaFecha ? ultimas4Semanas(ultimaFecha) : null;
  const [aMano, setAMano] = useState(false);
  // «Elegir fechas» se respeta aunque el rango coincida con el atajo de cuatro semanas.
  const valor = filtro.noche
    ? `n:${filtro.noche}`
    : aMano
      ? 'fechas'
      : atajo && filtro.desde === atajo.desde && filtro.hasta === atajo.hasta
        ? '4s'
        : filtro.desde || filtro.hasta
          ? 'fechas'
          : '';

  return (
    <form className="analitica-filtros" aria-label="Qué noches mirar" onSubmit={(e) => e.preventDefault()}>
      <Selector
        etiqueta="Noches"
        value={valor}
        onChange={(e) => {
          const v = e.target.value;
          setAMano(v === 'fechas');
          if (v === '') cambiar({ dia: filtro.dia });
          else if (v === '4s' && atajo) cambiar({ ...atajo, dia: filtro.dia });
          else if (v.startsWith('n:')) cambiar({ noche: v.slice(2) });
          else if (v === 'fechas') cambiar({ desde: filtro.desde ?? atajo?.desde, hasta: filtro.hasta ?? atajo?.hasta, dia: filtro.dia });
        }}
      >
        <option value="">Todas ({lista.length})</option>
        <option value="4s">Últimas 4 semanas</option>
        {sinNoche ? null : (
          <optgroup label="Una noche">
            {[...lista].reverse().map((n) => (
              <option key={n.id} value={`n:${n.id}`}>
                {capitalizar(fechaNoche(n.inicio, zona))}
                {n.evento ? `, ${n.evento.nombre}` : ''}
              </option>
            ))}
          </optgroup>
        )}
        <option value="fechas">Elegir fechas…</option>
      </Selector>
      {valor === 'fechas' ? (
        <>
          <Campo etiqueta="Desde" type="date" value={filtro.desde ?? ''} max={filtro.hasta} onChange={(e) => cambiar({ ...filtro, noche: undefined, desde: e.target.value })} />
          <Campo etiqueta="Hasta" type="date" value={filtro.hasta ?? ''} min={filtro.desde} onChange={(e) => cambiar({ ...filtro, noche: undefined, hasta: e.target.value })} />
        </>
      ) : null}
      {conDia && !filtro.noche ? (
        <Selector etiqueta="Día de la semana" value={filtro.dia ?? ''} onChange={(e) => cambiar({ ...filtro, dia: e.target.value || undefined })}>
          <option value="">Todos</option>
          {DIAS.map((d, i) => (
            <option key={d.corto} value={String(i + 1)}>
              {capitalizar(d.largo)}
            </option>
          ))}
        </Selector>
      ) : null}
    </form>
  );
}

/** La fecha de pared (`AAAA-MM-DD`) de un instante en la zona del bar. */
export function localDe(iso: string, zona: string): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: zona, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(iso));
}
