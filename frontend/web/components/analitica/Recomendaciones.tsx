'use client';

import type { Recomendacion } from '@rockola/contrato';
import { claseTecla } from '@rockola/ui';
import Link from 'next/link';
import { Cargando } from '@/components/gestion/PaginaGestion';
import { Filtros, OrigenDeLosDatos, useAnalitica, useFiltroAnalitica } from './Comun';

const DESTINOS: Record<Recomendacion['destino'], string> = { listas: 'Ir a Listas de la casa', bloqueos: 'Ir a Bloqueos', horario: 'Ir al Horario' };

function enlace(r: Recomendacion): string {
  if (r.destino === 'listas') return r.lista_id ? `/panel/listas/${r.lista_id}` : '/panel/listas';
  return r.destino === 'bloqueos' ? '/panel/bloqueos' : '/panel/horario';
}

/**
 * Recomendaciones para la operación (RF-054, PB-49): cada una dice en qué dato se basa y lleva
 * a la sección donde se decide. Nada se aplica solo.
 */
export function Recomendaciones() {
  const { consulta } = useFiltroAnalitica();
  const r = useAnalitica('recomendacionesAnalitica', consulta);
  return (
    <>
      <Filtros />
      <Cargando consulta={r}>
        {() => {
          const d = r.data!;
          return (
            <div className="analitica-cuerpo" aria-busy={r.isFetching || undefined}>
              <OrigenDeLosDatos datos={d.datos} />
              <p className="mt-3 text-[15px]">Nada se aplica solo: cada sugerencia te lleva a la sección donde decides.</p>
              {d.recomendaciones.length === 0 ? (
                <p className="secundario mt-6">Con estas noches no hay nada que sugerir.</p>
              ) : (
                <ul className="mt-4 grid gap-3">
                  {d.recomendaciones.map((x) => (
                    <li key={x.id} className="caja recomendacion">
                      <h2>{x.titulo}</h2>
                      <p>En qué se basa: {x.base}</p>
                      <Link href={enlace(x)} className={claseTecla('normal', 'ancha')}>
                        {DESTINOS[x.destino]}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          );
        }}
      </Cargando>
    </>
  );
}
