'use client';

import { GrupoTeclas } from '@rockola/ui';
import { useState } from 'react';
import { Cargando } from '@/components/gestion/PaginaGestion';
import { nombreGenero } from '@/lib/formato';
import { Filtros, horaRedonda, numero, OrigenDeLosDatos, useAnalitica, useFiltroAnalitica } from './Comun';
import { Barras, Columnas, TablaDatos } from './Graficos';

type MedidaGenero = 'solicitudes' | 'votos';
type Ranking = 'canciones' | 'artistas';
type MedidaFranja = 'solicitudes' | 'votos' | 'telefonos';
/** Pedidos mínimos en una hora para mostrar su espera promedio *(propuesto)*: con menos, un caso suelto la decide. */
const MINIMO_PARA_ESPERA = 10;
/**
 * Las horas seguidas más largas (en el orden de la noche, desde el mediodía): el eje de la espera
 * no salta horas, y un dato suelto de una tarde de evento no se mezcla con la noche.
 */
function tramoMasLargo<T extends { hora: number }>(horas: T[]): T[] {
  let mejor: T[] = [];
  let actual: T[] = [];
  for (const h of horas) {
    actual = actual.length > 0 && (actual.at(-1)!.hora + 1) % 24 === h.hora ? [...actual, h] : [h];
    if (actual.length > mejor.length) mejor = actual;
  }
  return mejor;
}

const NOMBRE: Record<MedidaFranja, string> = { solicitudes: 'solicitudes', votos: 'votos', telefonos: 'teléfonos' };

/**
 * Lo más pedido, géneros, franjas horarias y espera por hora (RF-051, PB-47). Una medida a la
 * vez: solicitudes, votos y minutos tienen escalas distintas y nunca comparten eje.
 */
export function Participacion() {
  const { consulta } = useFiltroAnalitica(true);
  const p = useAnalitica('participacionAnalitica', consulta);
  const [medidaGenero, setMedidaGenero] = useState<MedidaGenero>('solicitudes');
  const [medidaFranja, setMedidaFranja] = useState<MedidaFranja>('solicitudes');
  const [ranking, setRanking] = useState<Ranking>('canciones');

  return (
    <>
      <Filtros conDia />
      <Cargando consulta={p}>
        {() => {
          const d = p.data!;
          // «Sin clasificar» siempre al final: no compite en el ranking.
          const generos = [...d.generos].sort((a, b) => (a.genero === null ? 1 : b.genero === null ? -1 : b[medidaGenero] - a[medidaGenero]));
          // De la primera a la última hora con actividad, sin saltos (las vacías, en cero).
          const desdeMediodia = (h: number) => (h + 12) % 24;
          const primera = d.franjas[0] ? desdeMediodia(d.franjas[0].hora) : 0;
          const ultima = d.franjas.at(-1) ? desdeMediodia(d.franjas.at(-1)!.hora) : -1;
          const franjas = Array.from({ length: ultima - primera + 1 }, (_, i) => {
            const hora = (primera + i + 12) % 24;
            return d.franjas.find((f) => f.hora === hora) ?? { hora, solicitudes: 0, votos: 0, telefonos: 0, espera_promedio_seg: null };
          });
          // Espera por hora: solo las horas con pedidos suficientes para que el promedio diga algo, y
          // en las que algo llegó a sonar (sin dato no es cero).
          const esperas = tramoMasLargo(
            d.franjas.filter((f) => f.espera_promedio_seg !== null && f.solicitudes >= MINIMO_PARA_ESPERA).map((f) => ({ hora: f.hora, min: Math.round(f.espera_promedio_seg! / 60) })),
          );
          const masEspera = esperas.reduce<(typeof esperas)[number] | null>((m, f) => (!m || f.min > m.min ? f : m), null);
          const filasRanking =
            ranking === 'canciones'
              ? d.mas_pedidas.map((c) => ({ clave: c.cancion.video_id, nombre: c.cancion.titulo, detalle: c.cancion.artista, solicitudes: c.solicitudes, votos: c.votos }))
              : d.artistas.map((a) => ({ clave: a.artista, nombre: a.artista, detalle: '', solicitudes: a.solicitudes, votos: a.votos }));
          const pico = d.franjas.reduce<(typeof d.franjas)[number] | null>((m, f) => (!m || f[medidaFranja] > m[medidaFranja] ? f : m), null);
          return (
            <div className="analitica-cuerpo" aria-busy={p.isFetching || undefined}>
              <OrigenDeLosDatos datos={d.datos} />
              {d.datos.noches === 0 ? (
                <p className="secundario mt-6">No hay noches con este filtro.</p>
              ) : (
                <div className="mt-4 grid gap-4">
                  <section className="caja" aria-labelledby="t-mas-pedidas">
                    <div className="caja__cabeza">
                      <h2 id="t-mas-pedidas">Lo más pedido</h2>
                      <p>Las 10 primeras, por solicitudes</p>
                    </div>
                    <GrupoTeclas
                      leyenda="Ver"
                      nombre="ranking"
                      valor={ranking}
                      onCambio={setRanking}
                      opciones={[
                        { valor: 'canciones', texto: 'Canciones' },
                        { valor: 'artistas', texto: 'Artistas' },
                      ]}
                    />
                    {filasRanking.length === 0 ? (
                      <p className="secundario mt-4">Nadie pidió canciones con este filtro.</p>
                    ) : (
                      <div className="mt-4">
                        <Barras titulo={`${ranking === 'canciones' ? 'Canciones' : 'Artistas'} más pedidos, por solicitudes`} filas={filasRanking.map((f) => ({ clave: f.clave, etiqueta: f.nombre, valor: f.solicitudes }))} formato={numero} />
                      </div>
                    )}
                    <TablaDatos
                      titulo={ranking === 'canciones' ? 'Canciones más pedidas' : 'Artistas más pedidos'}
                      columnas={[{ titulo: ranking === 'canciones' ? 'Canción' : 'Artista' }, ...(ranking === 'canciones' ? [{ titulo: 'Artista' }] : []), { titulo: 'Solicitudes', num: true }, { titulo: 'Votos', num: true }]}
                      filas={filasRanking.map((f) => ({ clave: f.clave, celdas: [f.nombre, ...(ranking === 'canciones' ? [f.detalle] : []), numero(f.solicitudes), numero(f.votos)] }))}
                    />
                  </section>

                  <section className="caja" aria-labelledby="t-generos">
                    <div className="caja__cabeza">
                      <h2 id="t-generos">Géneros más pedidos</h2>
                    </div>
                    <GrupoTeclas
                      leyenda="Ordenar por"
                      nombre="medida-genero"
                      valor={medidaGenero}
                      onCambio={setMedidaGenero}
                      opciones={[
                        { valor: 'solicitudes', texto: 'Solicitudes' },
                        { valor: 'votos', texto: 'Votos' },
                      ]}
                    />
                    <div className="mt-4">
                      <Barras
                        titulo={`Géneros por ${medidaGenero}`}
                        filas={generos.map((g) => ({ clave: g.genero ?? 'sin', etiqueta: nombreGenero(g.genero), valor: g[medidaGenero] }))}
                        formato={numero}
                      />
                    </div>
                    <p className="secundario mt-2 text-[14px]">«Sin clasificar»: canciones sin género en los datos de YouTube.</p>
                    <TablaDatos
                      titulo="Géneros"
                      columnas={[{ titulo: 'Género' }, { titulo: 'Solicitudes', num: true }, { titulo: 'Votos', num: true }]}
                      filas={generos.map((g) => ({ clave: g.genero ?? 'sin', celdas: [nombreGenero(g.genero), numero(g.solicitudes), numero(g.votos)] }))}
                    />
                  </section>

                  <section className="caja" aria-labelledby="t-franjas">
                    <div className="caja__cabeza">
                      <h2 id="t-franjas">Franjas horarias</h2>
                      <p>Por hora en que llegó cada acción</p>
                    </div>
                    <GrupoTeclas
                      leyenda="Ver"
                      nombre="medida-franja"
                      valor={medidaFranja}
                      onCambio={setMedidaFranja}
                      opciones={[
                        { valor: 'solicitudes', texto: 'Solicitudes' },
                        { valor: 'votos', texto: 'Votos' },
                        { valor: 'telefonos', texto: 'Teléfonos' },
                      ]}
                    />
                    {pico ? (
                      <p className="mt-4 text-[15px]">
                        La hora con más {NOMBRE[medidaFranja]}: de {horaRedonda(pico.hora)} a {horaRedonda((pico.hora + 1) % 24)}, con {numero(pico[medidaFranja])}.
                      </p>
                    ) : null}
                    <div className="mt-2">
                      <Columnas
                        titulo={`${NOMBRE[medidaFranja][0]!.toUpperCase()}${NOMBRE[medidaFranja].slice(1)} por hora`}
                        filas={franjas.map((f) => ({ clave: String(f.hora), etiqueta: horaRedonda(f.hora), valor: f[medidaFranja] }))}
                        formato={numero}
                        enfasis={new Set(pico ? [String(pico.hora)] : [])}
                      />
                    </div>
                    <TablaDatos
                      titulo="Franjas horarias"
                      columnas={[{ titulo: 'Hora' }, { titulo: 'Solicitudes', num: true }, { titulo: 'Votos', num: true }, { titulo: 'Teléfonos', num: true }]}
                      filas={d.franjas.map((f) => ({
                        clave: String(f.hora),
                        marcada: f.hora === pico?.hora,
                        celdas: [`${horaRedonda(f.hora)} a ${horaRedonda((f.hora + 1) % 24)}`, numero(f.solicitudes), numero(f.votos), numero(f.telefonos)],
                      }))}
                    />
                  </section>

                  <section className="caja" aria-labelledby="t-espera-hora">
                    <div className="caja__cabeza">
                      <h2 id="t-espera-hora">Espera por hora</h2>
                      <p>De que se pidió a que sonó; horas seguidas con {MINIMO_PARA_ESPERA} pedidos o más</p>
                    </div>
                    {masEspera ? (
                      <>
                        <p className="text-[15px]">
                          Lo pedido de {horaRedonda(masEspera.hora)} a {horaRedonda((masEspera.hora + 1) % 24)} esperó más: {masEspera.min} min en promedio.
                        </p>
                        <div className="mt-2">
                          <Columnas
                            titulo="Espera promedio por hora, en minutos"
                            filas={esperas.map((f) => ({ clave: String(f.hora), etiqueta: horaRedonda(f.hora), valor: f.min }))}
                            formato={(v) => `${numero(v)} min`}
                            enfasis={new Set([String(masEspera.hora)])}
                          />
                        </div>
                        <TablaDatos
                          titulo="Espera por hora"
                          columnas={[{ titulo: 'Hora del pedido' }, { titulo: 'Espera promedio', num: true }]}
                          filas={esperas.map((f) => ({ clave: String(f.hora), marcada: f.hora === masEspera.hora, celdas: [`${horaRedonda(f.hora)} a ${horaRedonda((f.hora + 1) % 24)}`, `${numero(f.min)} min`] }))}
                        />
                      </>
                    ) : (
                      <p className="secundario">Ninguna hora tiene {MINIMO_PARA_ESPERA} pedidos que hayan sonado con este filtro.</p>
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
