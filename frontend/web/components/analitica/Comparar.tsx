'use client';

import { GrupoTeclas } from '@rockola/ui';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { Cargando } from '@/components/gestion/PaginaGestion';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { capitalizar, formatoTiempoTotal } from '@/lib/formato';
import { fechaNoche, Filtros, numero, OrigenDeLosDatos, useFiltroAnalitica, useNoches } from './Comun';
import { Barras, Proporcion, TablaDatos } from './Graficos';

type Agrupar = 'noche' | 'dia' | 'evento';

/**
 * Indicadores comparativos (RF-053, PB-48): dos o más noches, o agrupadas por día de la semana
 * o por tipo de evento. Cada indicador en su propio gráfico (escalas distintas, nunca dos ejes)
 * y lo planeado (la lista de la casa) frente a lo pedido.
 */
export function Comparar() {
  const zona = useYoDelPanel().establecimiento.zona_horaria;
  const { filtro } = useFiltroAnalitica();
  const noches = useNoches();
  const lista = noches.data?.noches ?? [];
  const [agrupar, setAgrupar] = useState<Agrupar>('noche');
  const [elegidas, setElegidas] = useState<string[] | null>(null);
  // Por defecto, las tres últimas noches.
  const ids = elegidas ?? lista.slice(-3).map((n) => n.id);
  const listo = agrupar !== 'noche' || ids.length >= 2;
  const consulta = agrupar === 'noche' ? { agrupar, noches: ids.join(',') } : { agrupar, desde: filtro.desde, hasta: filtro.hasta };
  const c = useQuery({
    queryKey: claves.panel.analitica('compararAnalitica', consulta),
    queryFn: ({ signal }) => pedir('compararAnalitica', { consulta, signal }),
    enabled: listo && noches.isSuccess,
    placeholderData: keepPreviousData,
    retry: false,
  });

  if (!noches.isSuccess) return <Cargando consulta={noches}>{() => null}</Cargando>;
  if (lista.length === 0) return <p className="secundario mt-6">Todavía no hay noches cerradas para comparar.</p>;

  return (
    <>
      <div className="mt-4">
        <GrupoTeclas
          leyenda="Comparar"
          nombre="agrupar"
          valor={agrupar}
          onCambio={setAgrupar}
          opciones={[
            { valor: 'noche', texto: 'Noches' },
            { valor: 'dia', texto: 'Días de la semana' },
            { valor: 'evento', texto: 'Tipos de evento' },
          ]}
        />
      </div>
      {agrupar === 'noche' ? (
        <fieldset className="caja mt-4">
          <legend className="solo-lector">Noches a comparar</legend>
          <p className="mb-2 text-[15px] font-semibold">Elige dos o más noches</p>
          <div className="seleccion-noches">
            {[...lista].reverse().map((n) => (
              <label key={n.id}>
                <input
                  type="checkbox"
                  checked={ids.includes(n.id)}
                  onChange={(e) => setElegidas(e.target.checked ? [...ids, n.id] : ids.filter((x) => x !== n.id))}
                />
                {capitalizar(fechaNoche(n.inicio, zona))}
                {n.evento ? `, ${n.evento.nombre}` : ''}
              </label>
            ))}
          </div>
        </fieldset>
      ) : (
        <Filtros sinNoche />
      )}

      {!listo ? (
        <Aviso tipo="info" className="mt-4" titulo="Elige al menos dos noches para comparar." />
      ) : (
        <Cargando consulta={c}>
          {() => {
            const d = c.data!;
            const g = d.grupos;
            const fila = (k: 'solicitudes' | 'telefonos' | 'votos') => g.map((x) => ({ clave: x.clave, etiqueta: x.etiqueta, valor: x[k] }));
            const conNoches = (x: (typeof g)[number]) => (agrupar === 'noche' ? x.etiqueta : `${x.etiqueta} (${x.noches} ${x.noches === 1 ? 'noche' : 'noches'})`);
            return (
              <div className="analitica-cuerpo" aria-busy={c.isFetching || undefined}>
                <OrigenDeLosDatos datos={d.datos} />
                {g.length === 0 ? (
                  <p className="secundario mt-6">No hay noches con este filtro.</p>
                ) : (
                  <>
                    {agrupar !== 'noche' ? <p className="secundario mt-2 text-[14px]">Cifras en promedio por noche, para comparar grupos de tamaño distinto.</p> : null}
                    <div className="gestion-rejilla gestion-rejilla--dos mt-4">
                      {(
                        [
                          ['solicitudes', 'Solicitudes'],
                          ['telefonos', 'Teléfonos'],
                          ['votos', 'Votos'],
                        ] as const
                      ).map(([k, titulo]) => (
                        <section key={k} className="caja" aria-label={titulo}>
                          <div className="caja__cabeza">
                            <h2>{titulo}</h2>
                          </div>
                          <Barras titulo={titulo} filas={fila(k)} formato={numero} />
                        </section>
                      ))}
                      <section className="caja" aria-label="Espera promedio">
                        <div className="caja__cabeza">
                          <h2>Espera promedio</h2>
                        </div>
                        {/* Sin pedidos que sonaran no hay espera que medir: ese grupo no se dibuja como cero. */}
                        <Barras
                          titulo="Espera promedio"
                          filas={g.filter((x) => x.espera_promedio_seg !== null).map((x) => ({ clave: x.clave, etiqueta: x.etiqueta, valor: x.espera_promedio_seg! }))}
                          formato={(v) => formatoTiempoTotal(v)}
                        />
                        {g.some((x) => x.espera_promedio_seg === null) ? (
                          <p className="secundario mt-2 text-[14px]">
                            Sin espera que medir (no sonó ningún pedido): {g.filter((x) => x.espera_promedio_seg === null).map((x) => x.etiqueta).join(', ')}.
                          </p>
                        ) : null}
                      </section>
                    </div>

                    <section className="caja mt-4" aria-labelledby="t-planeado">
                      <div className="caja__cabeza">
                        <h2 id="t-planeado">Lo planeado y lo pedido</h2>
                        <p>La lista de la casa frente a lo que sonó por solicitudes</p>
                      </div>
                      <div className="grid gap-5">
                        {g.map((x) => (
                          <div key={x.clave}>
                            <p className="mb-2 text-[15px] font-semibold">{conNoches(x)}</p>
                            <Proporcion
                              titulo={x.etiqueta}
                              partes={[
                                { clave: 'pedidos', etiqueta: 'Pedidos del público', valor: x.pedidos_seg, tono: 'dato' },
                                { clave: 'casa', etiqueta: 'Lista de la casa', valor: x.lista_casa_seg, tono: 'contexto' },
                              ]}
                            />
                          </div>
                        ))}
                      </div>
                      <TablaDatos
                        titulo="Comparación"
                        columnas={[
                          { titulo: agrupar === 'noche' ? 'Noche' : agrupar === 'dia' ? 'Día' : 'Tipo de evento' },
                          { titulo: 'Noches', num: true },
                          { titulo: 'Solicitudes', num: true },
                          { titulo: 'Teléfonos', num: true },
                          { titulo: 'Votos', num: true },
                          { titulo: 'Espera', num: true },
                          { titulo: 'Pedidos', num: true },
                          { titulo: 'Lista de la casa', num: true },
                        ]}
                        filas={g.map((x) => {
                          const total = x.pedidos_seg + x.lista_casa_seg;
                          const pct = (v: number) => `${total > 0 ? Math.round((v / total) * 100) : 0} %`;
                          return {
                            clave: x.clave,
                            celdas: [x.etiqueta, x.noches, numero(x.solicitudes), numero(x.telefonos), numero(x.votos), x.espera_promedio_seg === null ? '—' : formatoTiempoTotal(x.espera_promedio_seg), pct(x.pedidos_seg), pct(x.lista_casa_seg)],
                          };
                        })}
                      />
                    </section>
                  </>
                )}
              </div>
            );
          }}
        </Cargando>
      )}
    </>
  );
}
