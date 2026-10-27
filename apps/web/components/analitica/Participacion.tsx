'use client';

import { GrupoTeclas } from '@rockola/ui';
import { useState } from 'react';
import { Cargando } from '@/components/gestion/PaginaGestion';
import { Filtros, horaRedonda, nombreGenero, numero, OrigenDeLosDatos, useAnalitica, useFiltroAnalitica } from './Comun';
import { Barras, Columnas, TablaDatos } from './Graficos';

type MedidaGenero = 'solicitudes' | 'votos';
type MedidaFranja = 'solicitudes' | 'votos' | 'telefonos';
const NOMBRE: Record<MedidaFranja, string> = { solicitudes: 'solicitudes', votos: 'votos', telefonos: 'teléfonos' };

/**
 * Géneros y franjas horarias (RF-051, PB-47). Una medida a la vez: solicitudes y votos tienen
 * escalas distintas y nunca comparten eje.
 */
export function Participacion() {
  const { consulta } = useFiltroAnalitica(true);
  const p = useAnalitica('participacionAnalitica', consulta);
  const [medidaGenero, setMedidaGenero] = useState<MedidaGenero>('solicitudes');
  const [medidaFranja, setMedidaFranja] = useState<MedidaFranja>('solicitudes');

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
            return d.franjas.find((f) => f.hora === hora) ?? { hora, solicitudes: 0, votos: 0, telefonos: 0 };
          });
          const pico = d.franjas.reduce<(typeof d.franjas)[number] | null>((m, f) => (!m || f[medidaFranja] > m[medidaFranja] ? f : m), null);
          return (
            <div className="analitica-cuerpo" aria-busy={p.isFetching || undefined}>
              <OrigenDeLosDatos datos={d.datos} />
              {d.datos.noches === 0 ? (
                <p className="secundario mt-6">No hay noches con este filtro.</p>
              ) : (
                <div className="mt-4 grid gap-4">
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
                </div>
              )}
            </div>
          );
        }}
      </Cargando>
    </>
  );
}
