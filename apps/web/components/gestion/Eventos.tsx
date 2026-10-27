'use client';

import type { EventoBar } from '@rockola/contrato';
import { Campo, Dialogo, Selector, Tecla } from '@rockola/ui';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { DialogoConfirmar } from '@/components/panel/Dialogos';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { useAccionGestion } from '@/lib/api/hooks-panel';
import { useAhoraServidor } from '@/lib/reloj-servidor';
import { conPunto, formatoHora, isoALocal, localAIso } from '@/lib/formato';
import { Cargando, mensajeDeError, PaginaGestion } from './PaginaGestion';

const TIPOS = { normal: 'Noche normal', karaoke: 'Karaoke', partido: 'Partido', tematica: 'Temática', otro: 'Otro' } as const;
type Tipo = keyof typeof TIPOS;

function fecha(iso: string, zona: string) {
  return `${new Date(iso).toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long', timeZone: zona })}, ${formatoHora(iso, zona)}`;
}

/**
 * Calendario de eventos (RF-045, PB-41): las noches especiales, para comparar después en la
 * analítica. De un evento pasado solo se corrige la descripción; uno que ya empezó no se borra.
 */
export function Eventos() {
  const yo = useYoDelPanel();
  const zona = yo.establecimiento.zona_horaria;
  // La hora del servidor (en la simulación, la de la noche simulada); 0 hasta montarse.
  const ahora = useAhoraServidor();
  const eventos = useQuery({ queryKey: claves.panel.eventos, queryFn: ({ signal }) => pedir('eventos', { signal }) });
  const invalidar = [claves.panel.eventos];
  const crear = useAccionGestion('crearEvento', invalidar);
  const editar = useAccionGestion('editarEvento', invalidar);
  const eliminar = useAccionGestion('eliminarEvento', invalidar);
  const [edicion, setEdicion] = useState<{ evento: EventoBar | null } | null>(null);
  const [aEliminar, setAEliminar] = useState<EventoBar | null>(null);
  const [aviso, setAviso] = useState<{ tipo: 'luz' | 'error'; texto: string } | null>(null);

  const lista = eventos.data?.eventos ?? [];
  const proximos = lista.filter((e) => Date.parse(e.fin) >= ahora);
  const pasados = lista.filter((e) => Date.parse(e.fin) < ahora).reverse();

  const fila = (e: EventoBar, pasado: boolean) => (
    <li key={e.id}>
      <div className="lista-gestion__info">
        <p className="lista-gestion__titulo">
          {e.nombre}
          <span className="etiqueta">{TIPOS[e.tipo_evento]}</span>
        </p>
        <p className="lista-gestion__meta">
          {conPunto(`${fecha(e.inicio, zona)} a ${formatoHora(e.fin, zona)}`)}
          {e.descripcion ? ` ${e.descripcion}` : ''}
        </p>
      </div>
      <div className="lista-gestion__acciones">
        <Tecla tamano="chica" aria-label={`Editar ${e.nombre}`} onClick={() => setEdicion({ evento: e })}>
          {pasado ? 'Descripción' : 'Editar'}
        </Tecla>
        {Date.parse(e.inicio) > ahora ? (
          <Tecla tamano="chica" aria-label={`Eliminar ${e.nombre}`} onClick={() => setAEliminar(e)}>
            Eliminar
          </Tecla>
        ) : null}
      </div>
    </li>
  );

  return (
    <PaginaGestion
      titulo="Eventos"
      nota="Karaoke, partidos y noches temáticas: el tablero compara estas noches con las normales."
      accion={
        <Tecla variante="cereza" tamano="ancha" onClick={() => setEdicion({ evento: null })}>
          Agregar evento
        </Tecla>
      }
    >
      {aviso ? <Aviso tipo={aviso.tipo} className="mt-4" titulo={aviso.texto} /> : null}
      <Cargando consulta={{ ...eventos, data: ahora === 0 ? undefined : eventos.data }}>
        {() => (
          <div className="gestion-rejilla">
            <section className="caja" aria-labelledby="t-proximos">
              <div className="caja__cabeza">
                <h2 id="t-proximos">Próximos</h2>
              </div>
              {proximos.length === 0 ? <p className="secundario">No hay eventos próximos.</p> : <ul className="lista-gestion">{proximos.map((e) => fila(e, false))}</ul>}
            </section>
            <section className="caja" aria-labelledby="t-pasados">
              <div className="caja__cabeza">
                <h2 id="t-pasados">Pasados</h2>
              </div>
              {pasados.length === 0 ? <p className="secundario">Todavía no hay eventos pasados.</p> : <ul className="lista-gestion">{pasados.map((e) => fila(e, true))}</ul>}
            </section>
          </div>
        )}
      </Cargando>

      <DialogoEvento
        key={edicion ? (edicion.evento?.id ?? 'nuevo') : 'cerrado'}
        abierto={edicion !== null}
        evento={edicion?.evento ?? null}
        soloDescripcion={edicion?.evento ? Date.parse(edicion.evento.fin) < ahora : false}
        zona={zona}
        ocupado={crear.isPending || editar.isPending}
        onCerrar={() => setEdicion(null)}
        onGuardar={(datos) => {
          const fin = { onError: (e: unknown) => setAviso({ tipo: 'error', texto: mensajeDeError(e, { conflicto: 'De un evento pasado solo se cambia la descripción.', datos_invalidos: 'El evento tiene que terminar después de empezar.' }) }), onSettled: () => setEdicion(null) };
          if (edicion?.evento) editar.mutate({ parametros: { id: edicion.evento.id }, cuerpo: datos }, { ...fin, onSuccess: () => setAviso({ tipo: 'luz', texto: 'Evento guardado.' }) });
          else crear.mutate({ cuerpo: datos as Omit<EventoBar, 'id'> }, { ...fin, onSuccess: () => setAviso({ tipo: 'luz', texto: 'Evento agregado.' }) });
        }}
      />
      <DialogoConfirmar
        abierto={aEliminar !== null}
        titulo={`¿Eliminar «${aEliminar?.nombre ?? ''}»?`}
        texto="Se quita del calendario."
        confirmar="Eliminar evento"
        ocupado={eliminar.isPending}
        onCerrar={() => setAEliminar(null)}
        onConfirmar={() =>
          aEliminar &&
          eliminar.mutate({ parametros: { id: aEliminar.id } }, { onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e, { conflicto: 'Un evento que ya empezó no se elimina.' }) }), onSettled: () => setAEliminar(null) })
        }
      />
    </PaginaGestion>
  );
}

function DialogoEvento({
  abierto,
  evento,
  soloDescripcion,
  zona,
  ocupado,
  onCerrar,
  onGuardar,
}: {
  abierto: boolean;
  evento: EventoBar | null;
  soloDescripcion: boolean;
  zona: string;
  ocupado: boolean;
  onCerrar: () => void;
  onGuardar: (datos: Partial<Omit<EventoBar, 'id'>>) => void;
}) {
  const [nombre, setNombre] = useState(evento?.nombre ?? '');
  const [tipo, setTipo] = useState<Tipo>(evento?.tipo_evento ?? 'tematica');
  const [inicio, setInicio] = useState(evento ? isoALocal(evento.inicio, zona) : '');
  const [fin, setFin] = useState(evento ? isoALocal(evento.fin, zona) : '');
  const [descripcion, setDescripcion] = useState(evento?.descripcion ?? '');
  const [intento, setIntento] = useState(false);
  const errorFin = intento && inicio && fin && fin <= inicio ? 'Tiene que terminar después de empezar.' : null;
  return (
    <Dialogo abierto={abierto} titulo={evento ? `Editar «${evento.nombre}»` : 'Agregar evento'} onCerrar={onCerrar}>
      <form
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          setIntento(true);
          if (soloDescripcion) return onGuardar({ descripcion: descripcion.trim() || null });
          if (!nombre.trim() || !inicio || !fin || fin <= inicio) return;
          onGuardar({ nombre: nombre.trim(), tipo_evento: tipo, inicio: localAIso(inicio, zona), fin: localAIso(fin, zona), descripcion: descripcion.trim() || null });
        }}
      >
        {soloDescripcion ? <p className="dialogo__texto">Este evento ya pasó: solo se puede corregir la descripción.</p> : null}
        {!soloDescripcion ? (
          <>
            <Campo etiqueta="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} error={intento && !nombre.trim() ? 'Escribe el nombre.' : null} autoFocus />
            <Selector etiqueta="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value as Tipo)}>
              {Object.entries(TIPOS).map(([v, t]) => (
                <option key={v} value={v}>
                  {t}
                </option>
              ))}
            </Selector>
            <div className="formulario-fila">
              <Campo etiqueta="Empieza" type="datetime-local" value={inicio} onChange={(e) => setInicio(e.target.value)} error={intento && !inicio ? 'Elige cuándo empieza.' : null} />
              <Campo etiqueta="Termina" type="datetime-local" value={fin} onChange={(e) => setFin(e.target.value)} error={errorFin ?? (intento && !fin ? 'Elige cuándo termina.' : null)} />
            </div>
          </>
        ) : null}
        <Campo etiqueta="Descripción (opcional)" multilinea value={descripcion} onChange={(e) => setDescripcion(e.target.value)} maxLength={300} />
        <div className="dialogo__acciones">
          <Tecla type="button" onClick={onCerrar}>
            Cancelar
          </Tecla>
          <Tecla type="submit" variante="cereza" ocupada={ocupado}>
            Guardar
          </Tecla>
        </div>
      </form>
    </Dialogo>
  );
}
