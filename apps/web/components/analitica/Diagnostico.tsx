'use client';

import type { UmbralesAnalitica } from '@rockola/contrato';
import { Campo, Icono, Tecla } from '@rockola/ui';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { Cargando, mensajeDeError } from '@/components/gestion/PaginaGestion';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { useAccionGestion } from '@/lib/api/hooks-panel';
import { capitalizar } from '@/lib/formato';
import { CAUSAS, fechaCorta, fechaNoche, Filtros, numero, OrigenDeLosDatos, useAnalitica, useFiltroAnalitica } from './Comun';
import { Columnas, Medidor, TablaDatos } from './Graficos';

/**
 * Analítica diagnóstica (RF-052, PB-46): las muy votadas que no sonaron y por qué, si pocos
 * teléfonos concentran los votos y las noches con poca participación. Los umbrales están a la
 * vista y el Administrador los cambia aquí.
 */
export function Diagnostico() {
  const zona = useYoDelPanel().establecimiento.zona_horaria;
  const { consulta } = useFiltroAnalitica();
  const d = useAnalitica('diagnosticoAnalitica', consulta);

  return (
    <>
      <Filtros />
      <Cargando consulta={d}>
        {() => {
          const x = d.data!;
          const u = x.umbrales;
          const variasNoches = x.noches.length > 1;
          const flojas = x.noches.filter((n) => n.telefonos < u.participacion_minima);
          const concentradas = x.noches.filter((n) => n.pct_votos_del_10 > u.concentracion_pct);
          const alta = x.concentracion.pct_votos_del_10 > u.concentracion_pct;
          return (
            <div className="analitica-cuerpo" aria-busy={d.isFetching || undefined}>
              <OrigenDeLosDatos datos={x.datos} />
              {x.datos.noches === 0 ? (
                <p className="secundario mt-6">No hay noches con este filtro.</p>
              ) : (
                <div className="mt-4 grid gap-4">
                  <section className="caja" aria-labelledby="t-no-sonaron">
                    <div className="caja__cabeza">
                      <h2 id="t-no-sonaron">Muy votadas que no sonaron</h2>
                      <p>Con {u.votos_minimos} votos o más</p>
                    </div>
                    {x.no_sonaron.length === 0 ? (
                      <p className="secundario">Ninguna canción con {u.votos_minimos} votos o más se quedó sin sonar.</p>
                    ) : (
                      <table className="tabla-datos">
                        <thead>
                          <tr>
                            <th scope="col">Canción</th>
                            <th scope="col" className="num">
                              Votos
                            </th>
                            <th scope="col" className="solo-ancho">
                              Noche
                            </th>
                            <th scope="col" className="solo-ancho">
                              Causa probable
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {x.no_sonaron.map((s) => (
                            <tr key={s.solicitud_id}>
                              <td>
                                {s.cancion.titulo}
                                <span className="secundario">, {s.cancion.artista}</span>
                                <span className="linea-2 solo-angosto">
                                  {CAUSAS[s.causa]}, {fechaCorta(s.noche_inicio, zona)}
                                </span>
                              </td>
                              <td className="num">{numero(s.votos)}</td>
                              <td className="solo-ancho whitespace-nowrap">{fechaCorta(s.noche_inicio, zona)}</td>
                              <td className="solo-ancho">{CAUSAS[s.causa]}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </section>

                  <div className="gestion-rejilla gestion-rejilla--dos">
                    <section className="caja self-start" aria-labelledby="t-concentracion">
                      <div className="caja__cabeza">
                        <h2 id="t-concentracion">Concentración de votos</h2>
                        {variasNoches ? <p>Promedio por noche</p> : null}
                      </div>
                      <p className="text-[15px]">
                        <b className="cifra-grande">{x.concentracion.pct_votos_del_10} %</b> de los votos los dio el 10 % de los teléfonos que más votaron.
                      </p>
                      <div className="mt-3">
                        <Medidor titulo="Concentración de votos" valor={x.concentracion.pct_votos_del_10} umbral={u.concentracion_pct} etiquetaUmbral={`Umbral ${u.concentracion_pct} %`} />
                      </div>
                      <p className="estado-diagnostico mt-3">
                        <Icono nombre={alta ? 'alerta' : 'ok'} />
                        {alta ? 'Alta: pocos teléfonos están decidiendo la cola.' : 'Normal: los votos están repartidos.'}
                      </p>
                      {variasNoches && concentradas.length > 0 ? (
                        <p className="secundario mt-2 text-[14px]">
                          Por encima del umbral: {concentradas.map((n) => `${fechaCorta(n.inicio, zona)} (${n.pct_votos_del_10} %)`).join(', ')}.
                        </p>
                      ) : null}
                    </section>

                    <section className="caja" aria-labelledby="t-participacion">
                      <div className="caja__cabeza">
                        <h2 id="t-participacion">Teléfonos por noche</h2>
                        <p>Mínimo: {u.participacion_minima}</p>
                      </div>
                      {variasNoches ? (
                        <>
                          <p className="text-[15px]">
                            {flojas.length === 0
                              ? 'Ninguna noche quedó por debajo del mínimo.'
                              : `${flojas.length === 1 ? 'Una noche quedó' : `${flojas.length} noches quedaron`} por debajo del mínimo: ${flojas.map((n) => fechaNoche(n.inicio, zona)).join(', ')}.`}
                          </p>
                          <div className="mt-2">
                            <Columnas
                              titulo="Teléfonos únicos por noche"
                              filas={x.noches.map((n) => ({ clave: n.noche_id, etiqueta: fechaCorta(n.inicio, zona), valor: n.telefonos }))}
                              formato={numero}
                              enfasis={new Set(flojas.map((n) => n.noche_id))}
                              umbral={{ valor: u.participacion_minima, etiqueta: `Mínimo ${u.participacion_minima}` }}
                            />
                          </div>
                        </>
                      ) : (
                        <p className="text-[15px]">
                          <b className="cifra-grande">{numero(x.noches[0]!.telefonos)}</b> teléfonos participaron:{' '}
                          {x.noches[0]!.telefonos < u.participacion_minima ? 'por debajo del mínimo.' : 'por encima del mínimo.'}
                        </p>
                      )}
                      <TablaDatos
                        titulo="Participación por noche"
                        columnas={[{ titulo: 'Noche' }, { titulo: 'Teléfonos', num: true }, { titulo: 'Solicitudes', num: true }, { titulo: 'Concentración', num: true }]}
                        filas={x.noches.map((n) => ({
                          clave: n.noche_id,
                          marcada: n.telefonos < u.participacion_minima || n.pct_votos_del_10 > u.concentracion_pct,
                          celdas: [capitalizar(fechaNoche(n.inicio, zona)), numero(n.telefonos), numero(n.solicitudes), `${n.pct_votos_del_10} %`],
                        }))}
                      />
                    </section>
                  </div>
                </div>
              )}
            </div>
          );
        }}
      </Cargando>
      <Umbrales />
    </>
  );
}

type Mensaje = { tipo: 'luz' | 'error'; texto: string };

function Umbrales() {
  const umbrales = useQuery({ queryKey: claves.panel.analitica('umbralesAnalitica'), queryFn: ({ signal }) => pedir('umbralesAnalitica', { signal }) });
  // La acción y el aviso viven aquí: al guardar, el formulario se vuelve a montar con lo nuevo.
  const guardar = useAccionGestion('guardarUmbrales', [['panel', 'analitica']]);
  const [aviso, setAviso] = useState<Mensaje | null>(null);
  return (
    <section className="caja mt-4" aria-labelledby="t-umbrales">
      <div className="caja__cabeza">
        <h2 id="t-umbrales">Umbrales del diagnóstico</h2>
        <p>Valen para todo el tablero</p>
      </div>
      <Cargando consulta={umbrales}>
        {() => <FormularioUmbrales key={JSON.stringify(umbrales.data)} actuales={umbrales.data!} guardar={guardar} aviso={aviso} setAviso={setAviso} />}
      </Cargando>
    </section>
  );
}

function FormularioUmbrales({
  actuales,
  guardar,
  aviso,
  setAviso,
}: {
  actuales: UmbralesAnalitica;
  guardar: ReturnType<typeof useAccionGestion<'guardarUmbrales'>>;
  aviso: Mensaje | null;
  setAviso: (m: Mensaje | null) => void;
}) {
  const [datos, setDatos] = useState({ votos: String(actuales.votos_minimos), concentracion: String(actuales.concentracion_pct), participacion: String(actuales.participacion_minima) });
  const entero = (v: string, min: number, max: number) => Number.isInteger(Number(v)) && Number(v) >= min && Number(v) <= max;
  const errores = {
    votos: entero(datos.votos, 1, 500) ? null : 'De 1 a 500.',
    concentracion: entero(datos.concentracion, 10, 100) ? null : 'De 10 a 100.',
    participacion: entero(datos.participacion, 1, 1000) ? null : 'De 1 a 1.000.',
  };
  const poner = (k: keyof typeof datos) => (e: { target: { value: string } }) => {
    setAviso(null);
    setDatos((d) => ({ ...d, [k]: e.target.value }));
  };
  return (
    <form
      className="flex flex-col gap-4"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        if (Object.values(errores).some(Boolean)) return;
        guardar.mutate(
          { cuerpo: { votos_minimos: Number(datos.votos), concentracion_pct: Number(datos.concentracion), participacion_minima: Number(datos.participacion) } },
          { onSuccess: () => setAviso({ tipo: 'luz', texto: 'Umbrales guardados. El diagnóstico ya los usa.' }), onError: (err) => setAviso({ tipo: 'error', texto: mensajeDeError(err) }) },
        );
      }}
    >
      <div className="formulario-fila">
        <Campo etiqueta="Votos para «muy pedida»" ayuda="Desde cuántos votos una canción que no sonó aparece arriba." type="number" min={1} max={500} value={datos.votos} onChange={poner('votos')} error={errores.votos} />
        <Campo etiqueta="Concentración (%)" ayuda="Alerta si el 10 % de los teléfonos da más que esto de los votos." type="number" min={10} max={100} value={datos.concentracion} onChange={poner('concentracion')} error={errores.concentracion} />
        <Campo etiqueta="Teléfonos mínimos" ayuda="Por noche; menos, y la noche se marca." type="number" min={1} max={1000} value={datos.participacion} onChange={poner('participacion')} error={errores.participacion} />
      </div>
      {aviso ? <Aviso tipo={aviso.tipo} titulo={aviso.texto} /> : null}
      <Tecla type="submit" tamano="ancha" className="self-start" ocupada={guardar.isPending}>
        Guardar umbrales
      </Tecla>
    </form>
  );
}
