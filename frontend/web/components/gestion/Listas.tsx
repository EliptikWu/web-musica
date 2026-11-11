'use client';

import type { ListaCasa } from '@rockola/contrato';
import { Campo, claseTecla, Dialogo, GrupoTeclas, Tecla } from '@rockola/ui';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { DialogoConfirmar } from '@/components/panel/Dialogos';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { useAccionGestion } from '@/lib/api/hooks-panel';
import { contarCanciones, conPunto, fechaYHora, formatoTiempoTotal } from '@/lib/formato';
import { Cargando, mensajeDeError, PaginaGestion } from './PaginaGestion';

export const ROTACIONES = [
  { valor: 'secuencial', texto: 'En orden' },
  { valor: 'aleatoria', texto: 'Al azar, sin repetir' },
] as const;

/**
 * Listas de la casa (RF-027 a RF-030, RF-033; PB-35, 37, 39): lo que suena cuando la cola se
 * vacía. Solo una está activa; esa no se puede eliminar.
 */
export function Listas() {
  const zona = useYoDelPanel().establecimiento.zona_horaria;
  const listas = useQuery({ queryKey: claves.panel.listas, queryFn: ({ signal }) => pedir('listas', { signal }) });
  const crear = useAccionGestion('crearLista', [claves.panel.listas]);
  const eliminar = useAccionGestion('eliminarLista', [claves.panel.listas]);
  const activar = useAccionGestion('activarLista', [claves.panel.listas]);
  const [nombre, setNombre] = useState('');
  const [aviso, setAviso] = useState<{ tipo: 'luz' | 'error'; texto: string } | null>(null);
  const [aEliminar, setAEliminar] = useState<ListaCasa | null>(null);
  const [aActivar, setAActivar] = useState<ListaCasa | null>(null);
  const [modo, setModo] = useState<'secuencial' | 'aleatoria'>('secuencial');

  return (
    <PaginaGestion titulo="Listas de la casa" nota="Suenan cuando la cola se vacía, para que la música no pare. Solo una está activa a la vez.">
      {aviso ? <Aviso tipo={aviso.tipo} className="mt-4" titulo={aviso.texto} /> : null}
      <div className="gestion-rejilla gestion-rejilla--dos">
        <section className="caja" aria-labelledby="t-listas">
          <div className="caja__cabeza">
            <h2 id="t-listas">Listas</h2>
          </div>
          <Cargando consulta={listas}>
            {() =>
              listas.data!.listas.length === 0 ? (
                <p className="secundario">Todavía no hay listas. Crea la primera al lado.</p>
              ) : (
                <ul className="lista-gestion">
                  {listas.data!.listas.map((l) => (
                    <li key={l.id}>
                      <div className="lista-gestion__info">
                        <p className="lista-gestion__titulo">
                          {l.nombre}
                          {l.activa ? <span className="etiqueta etiqueta--luz">Activa</span> : null}
                        </p>
                        <p className="lista-gestion__meta">
                          {contarCanciones(l.total_canciones)}, {formatoTiempoTotal(l.duracion_total_seg)}
                          {l.activa ? `, ${l.modo_rotacion === 'secuencial' ? 'en orden' : 'al azar'}` : ''}. {conPunto(`Cambiada el ${fechaYHora(l.actualizada_en, zona)}`)}
                        </p>
                      </div>
                      <div className="lista-gestion__acciones">
                        <Link href={`/panel/listas/${l.id}`} className={claseTecla('normal', 'chica')} aria-label={`Editar ${l.nombre}`}>
                          Editar
                        </Link>
                        {!l.activa ? (
                          <>
                            <Tecla
                              tamano="chica"
                              aria-label={`Activar ${l.nombre}`}
                              onClick={() => {
                                setModo(l.modo_rotacion);
                                setAActivar(l);
                              }}
                            >
                              Activar
                            </Tecla>
                            <Tecla tamano="chica" aria-label={`Eliminar ${l.nombre}`} onClick={() => setAEliminar(l)}>
                              Eliminar
                            </Tecla>
                          </>
                        ) : (
                          <Tecla
                            tamano="chica"
                            aria-label={`Cambiar la rotación de ${l.nombre}`}
                            onClick={() => {
                              setModo(l.modo_rotacion);
                              setAActivar(l);
                            }}
                          >
                            Rotación
                          </Tecla>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              )
            }
          </Cargando>
        </section>

        <form
          className="caja flex flex-col gap-3 self-start"
          onSubmit={(e) => {
            e.preventDefault();
            if (!nombre.trim()) return;
            crear.mutate(
              { cuerpo: { nombre: nombre.trim() } },
              {
                onSuccess: (l) => {
                  setNombre('');
                  setAviso({ tipo: 'luz', texto: `Lista «${l.nombre}» creada. Agrégale canciones para poder activarla.` });
                },
                onError: (err) => setAviso({ tipo: 'error', texto: mensajeDeError(err, { conflicto: 'Ya hay una lista con ese nombre.' }) }),
              },
            );
          }}
        >
          <h2 className="titulo-seccion">Crear una lista</h2>
          <Campo etiqueta="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={60} placeholder="Por ejemplo, Viernes crossover" />
          <Tecla type="submit" variante="cereza" tamano="ancha" ocupada={crear.isPending}>
            Crear lista
          </Tecla>
        </form>
      </div>

      <DialogoConfirmar
        abierto={aEliminar !== null}
        titulo={`¿Eliminar «${aEliminar?.nombre ?? ''}»?`}
        texto="La lista y su orden se borran. Las canciones siguen pudiéndose pedir."
        confirmar="Eliminar lista"
        ocupado={eliminar.isPending}
        onCerrar={() => setAEliminar(null)}
        onConfirmar={() =>
          aEliminar &&
          eliminar.mutate(
            { parametros: { id: aEliminar.id } },
            {
              onSuccess: () => setAviso({ tipo: 'luz', texto: `Lista «${aEliminar.nombre}» eliminada.` }),
              onError: (err) => setAviso({ tipo: 'error', texto: mensajeDeError(err, { conflicto: 'La lista activa no se puede eliminar.' }) }),
              onSettled: () => setAEliminar(null),
            },
          )
        }
      />
      <Dialogo abierto={aActivar !== null} titulo={aActivar?.activa ? `Rotación de «${aActivar.nombre}»` : `Activar «${aActivar?.nombre ?? ''}»`} onCerrar={() => setAActivar(null)}>
        <p className="dialogo__texto">{aActivar?.activa ? 'Cambia desde la próxima canción de la lista.' : 'Reemplaza a la lista activa desde la próxima canción de la lista de la casa.'}</p>
        <GrupoTeclas leyenda="Cómo suena" nombre="rotacion" opciones={[...ROTACIONES]} valor={modo} onCambio={setModo} />
        <div className="dialogo__acciones">
          <Tecla type="button" onClick={() => setAActivar(null)}>
            Cancelar
          </Tecla>
          <Tecla
            type="button"
            variante="cereza"
            ocupada={activar.isPending}
            onClick={() =>
              aActivar &&
              activar.mutate(
                { parametros: { id: aActivar.id }, cuerpo: { modo_rotacion: modo } },
                {
                  onSuccess: () => setAviso({ tipo: 'luz', texto: `«${aActivar.nombre}» es la lista activa, ${modo === 'secuencial' ? 'en orden' : 'al azar'}.` }),
                  onError: (err) => setAviso({ tipo: 'error', texto: mensajeDeError(err, { conflicto: 'Una lista vacía no se puede activar.' }) }),
                  onSettled: () => setAActivar(null),
                },
              )
            }
          >
            {aActivar?.activa ? 'Guardar' : 'Activar'}
          </Tecla>
        </div>
      </Dialogo>
    </PaginaGestion>
  );
}
