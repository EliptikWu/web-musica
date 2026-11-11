'use client';

import { Tecla } from '@rockola/ui';
import Link from 'next/link';
import { useState } from 'react';
import { Cargando } from '@/components/gestion/PaginaGestion';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { formatoHora, formatoTiempoTotal } from '@/lib/formato';
import { Filtros, numero, OrigenDeLosDatos, useAnalitica, useFiltroAnalitica } from './Comun';
import { Proporcion } from './Graficos';

const VISIBLES = 12;

/**
 * Tablero descriptivo (RF-049, RF-050; PB-45): solicitudes, teléfonos, votos, la más votada,
 * esperas, cuánto sonó lo pedido frente a la lista de la casa y, de una noche, lo que sonó.
 */
export function Resumen() {
  const zona = useYoDelPanel().establecimiento.zona_horaria;
  const { consulta, query } = useFiltroAnalitica();
  const r = useAnalitica('resumenAnalitica', consulta);
  const [todo, setTodo] = useState(false);

  return (
    <>
      <Filtros />
      <Cargando consulta={r}>
        {() => {
          const d = r.data!;
          const una = d.datos.noches === 1;
          const historial = todo ? d.historial : d.historial.slice(0, VISIBLES);
          return (
            <div className="analitica-cuerpo" aria-busy={r.isFetching || undefined}>
              <OrigenDeLosDatos datos={d.datos} />
              {d.datos.noches === 0 ? (
                <p className="secundario mt-6">No hay noches con este filtro.</p>
              ) : (
                <div className="mt-4 grid gap-4">
                  <section className="caja" aria-labelledby="t-cifras">
                    <div className="caja__cabeza">
                      <h2 id="t-cifras">{una ? 'En esa noche' : 'En estas noches'}</h2>
                    </div>
                    <dl className="metricas cifras">
                      <div className="metrica">
                        <dt>Solicitudes</dt>
                        <dd>{numero(d.solicitudes)}</dd>
                      </div>
                      <div className="metrica">
                        <dt>Teléfonos</dt>
                        <dd>{numero(d.telefonos)}</dd>
                      </div>
                      <div className="metrica">
                        <dt>Votos</dt>
                        <dd>{numero(d.votos)}</dd>
                      </div>
                      <div className="metrica metrica--texto">
                        <dt>La más votada</dt>
                        <dd>
                          {d.mas_popular ? (
                            <>
                              {d.mas_popular.cancion.titulo}
                              <small>
                                {d.mas_popular.cancion.artista}, {numero(d.mas_popular.votos)} votos
                              </small>
                            </>
                          ) : (
                            '—'
                          )}
                        </dd>
                      </div>
                    </dl>
                  </section>

                  <div className="gestion-rejilla gestion-rejilla--dos">
                    <section className="caja" aria-labelledby="t-esperas">
                      <div className="caja__cabeza">
                        <h2 id="t-esperas">Esperas</h2>
                        <p>De la solicitud a que empezó a sonar</p>
                      </div>
                      <dl className="metricas">
                        <div className="metrica">
                          <dt>Promedio</dt>
                          <dd>{d.espera ? formatoTiempoTotal(d.espera.promedio_seg) : '—'}</dd>
                        </div>
                        <div className="metrica">
                          <dt>Máxima</dt>
                          <dd>{d.espera ? formatoTiempoTotal(d.espera.maxima_seg) : '—'}</dd>
                        </div>
                      </dl>
                    </section>

                    <section className="caja" aria-labelledby="t-sono">
                      <div className="caja__cabeza">
                        <h2 id="t-sono">Qué sonó</h2>
                        <p>Del tiempo con música</p>
                      </div>
                      <Proporcion
                        titulo="Qué sonó"
                        partes={[
                          { clave: 'pedidos', etiqueta: 'Pedidos del público', valor: d.tiempo.pedidos_seg, tono: 'dato', detalle: formatoTiempoTotal(d.tiempo.pedidos_seg) },
                          { clave: 'casa', etiqueta: 'Lista de la casa', valor: d.tiempo.lista_casa_seg, tono: 'contexto', detalle: formatoTiempoTotal(d.tiempo.lista_casa_seg) },
                        ]}
                      />
                      <p className="mt-3 text-[15px]">
                        {d.no_sonaron === 0 ? (
                          'Todas las solicitudes sonaron.'
                        ) : (
                          <>
                            {numero(d.no_sonaron)} {d.no_sonaron === 1 ? 'solicitud no sonó' : 'solicitudes no sonaron'}.{' '}
                            <Link href={`/panel/analitica/diagnostico${query ? `?${query}` : ''}`} className="enlace">
                              Ver por qué
                            </Link>
                          </>
                        )}
                      </p>
                    </section>
                  </div>

                  <section className="caja" aria-labelledby="t-historial">
                    <div className="caja__cabeza">
                      <h2 id="t-historial">Lo que sonó, en orden</h2>
                      {una ? <p>{numero(d.historial.length)} canciones</p> : null}
                    </div>
                    {una ? (
                      <>
                        <table className="tabla-datos">
                          <thead>
                            <tr>
                              <th scope="col">Hora</th>
                              <th scope="col">Canción</th>
                              <th scope="col" className="solo-ancho">
                                De dónde
                              </th>
                              <th scope="col" className="num">
                                Votos
                              </th>
                            </tr>
                          </thead>
                          <tbody>
                            {historial.map((h) => (
                              <tr key={h.inicio}>
                                <td className="whitespace-nowrap">{formatoHora(h.inicio, zona)}</td>
                                <td>
                                  {h.cancion.titulo}
                                  <span className="secundario">, {h.cancion.artista}</span>
                                  <span className="linea-2 solo-angosto">{h.origen === 'solicitud' ? 'Pedida' : 'Lista de la casa'}</span>
                                </td>
                                <td className="solo-ancho">{h.origen === 'solicitud' ? 'Pedida' : 'Lista de la casa'}</td>
                                <td className="num">{h.origen === 'solicitud' ? numero(h.votos) : '—'}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        {d.historial.length > VISIBLES ? (
                          <Tecla tamano="ancha" className="mt-3" onClick={() => setTodo((t) => !t)} aria-expanded={todo}>
                            {todo ? 'Ver menos' : `Ver las ${d.historial.length}`}
                          </Tecla>
                        ) : null}
                      </>
                    ) : (
                      <p className="secundario">Elige una noche arriba para ver lo que sonó, canción por canción.</p>
                    )}
                  </section>
                </div>
              )}
            </div>
          );
        }}
      </Cargando>
    </>
  );
}
