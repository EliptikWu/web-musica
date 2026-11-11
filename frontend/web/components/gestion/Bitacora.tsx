'use client';

import type { TipoAccion } from '@rockola/contrato';
import { Campo, Selector, Tecla } from '@rockola/ui';
import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { fechaYHora, localAIso } from '@/lib/formato';
import { lineasRegistro } from '@/lib/textos/panel';
import { Cargando, PaginaGestion } from './PaginaGestion';

const TIPOS: { valor: '' | TipoAccion; texto: string }[] = [
  { valor: '', texto: 'Todas las acciones' },
  { valor: 'saltar', texto: 'Saltos' },
  { valor: 'bloquear', texto: 'Bloqueos' },
  { valor: 'desbloquear', texto: 'Desbloqueos' },
  { valor: 'retirar', texto: 'Retiros' },
  { valor: 'pausar_solicitudes', texto: 'Pausas de solicitudes' },
  { valor: 'mantenimiento', texto: 'Mantenimiento' },
  { valor: 'abrir_sesion', texto: 'Aperturas de la noche' },
  { valor: 'editar_horario', texto: 'Cambios de horario' },
  { valor: 'editar_lista', texto: 'Cambios en listas' },
  { valor: 'crear_usuario', texto: 'Usuarios creados' },
  { valor: 'editar_umbrales', texto: 'Umbrales del tablero' },
  { valor: 'error_youtube', texto: 'Errores de YouTube' },
  { valor: 'intento_no_permitido', texto: 'Intentos sin permiso' },
];

/** Inicio y fin de un día en la zona del bar, como ISO. */
const inicioDelDia = (fecha: string, zona: string) => (fecha ? localAIso(`${fecha}T00:00:00`, zona) : undefined);
const finDelDia = (fecha: string, zona: string) => (fecha ? localAIso(`${fecha}T23:59:59`, zona) : undefined);

/**
 * Bitácora (RF-044, PB-40): quién hizo qué y cuándo, incluidos los errores de YouTube. Solo
 * lectura. Se filtra por fechas, persona y tipo de acción.
 */
export function Bitacora() {
  const yo = useYoDelPanel();
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [usuario, setUsuario] = useState('');
  const [tipo, setTipo] = useState<'' | TipoAccion>('');
  const personal = useQuery({ queryKey: claves.panel.usuarios, queryFn: ({ signal }) => pedir('usuarios', { signal }) });
  const zona = yo.establecimiento.zona_horaria;
  const filtros = { desde: inicioDelDia(desde, zona), hasta: finDelDia(hasta, zona), usuario: usuario || undefined, tipo: tipo || undefined };
  const paginas = useInfiniteQuery({
    queryKey: [...claves.panel.bitacora, filtros],
    queryFn: ({ pageParam, signal }) => pedir('bitacora', { consulta: { ...filtros, cursor: pageParam ?? undefined }, signal }),
    initialPageParam: null as string | null,
    getNextPageParam: (ultima) => ultima.siguiente,
  });
  const acciones = paginas.data?.pages.flatMap((p) => p.acciones) ?? [];

  return (
    <PaginaGestion titulo="Bitácora" nota="Todo lo que el personal y el sistema hicieron, con fecha y hora. No se puede editar.">
      <form className="caja formulario-fila mt-4" onSubmit={(e) => e.preventDefault()} aria-label="Filtros">
        <Campo etiqueta="Desde" type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
        <Campo etiqueta="Hasta" type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
        <Selector etiqueta="Quién" value={usuario} onChange={(e) => setUsuario(e.target.value)}>
          <option value="">Todos</option>
          <option value="sistema">El sistema</option>
          {personal.data?.usuarios.map((u) => (
            <option key={u.id} value={u.id}>
              {u.nombre}
            </option>
          ))}
        </Selector>
        <Selector etiqueta="Qué" value={tipo} onChange={(e) => setTipo(e.target.value as '' | TipoAccion)}>
          {TIPOS.map((t) => (
            <option key={t.valor} value={t.valor}>
              {t.texto}
            </option>
          ))}
        </Selector>
      </form>
      <section className="caja mt-4" aria-label="Acciones">
        <Cargando consulta={paginas}>
          {() =>
            acciones.length === 0 ? (
              <p className="secundario">No hay acciones con estos filtros.</p>
            ) : (
              <>
                <ol className="registro">
                  {lineasRegistro(acciones, yo.usuario.id).map((l) => (
                    <li key={l.id} className="[grid-template-columns:150px_1fr]!">
                      <time dateTime={l.fecha}>{fechaYHora(l.fecha, zona)}</time>
                      <span>{l.texto}</span>
                    </li>
                  ))}
                </ol>
                {paginas.hasNextPage ? (
                  <Tecla tamano="ancha" className="mt-4" ocupada={paginas.isFetchingNextPage} onClick={() => void paginas.fetchNextPage()}>
                    Cargar más
                  </Tecla>
                ) : null}
              </>
            )
          }
        </Cargando>
      </section>
    </PaginaGestion>
  );
}
