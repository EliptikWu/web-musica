'use client';

import { claseTecla } from '@rockola/ui';
import Link from 'next/link';
import { useBar } from '@/lib/api/hooks-cliente';
import { MarcoCliente } from './MarcoCliente';

/**
 * Política de datos (RF-004, PB-06): qué se registra, para qué y qué no. El texto legal
 * completo del bar (Ley 1581 de 2012) está pendiente y la página lo dice.
 */
export function Politica({ slug }: { slug: string }) {
  const bar = useBar(slug);
  return (
    <MarcoCliente slug={slug}>
      <header>
        <p className="secundario text-[14px]">{bar.data?.nombre ?? ' '}</p>
        <h1 className="placa mt-1 text-[24px]">Política de datos</h1>
      </header>

      <div className="ventana mt-5 px-4 py-4 text-left text-[15px] leading-[1.5]">
        <h2 className="titulo-seccion text-tinta">Qué se registra</h2>
        <p className="mt-1">
          Un código anónimo que se crea en este navegador para reconocer tu teléfono, las canciones que pides y los votos que das, con su fecha y hora.
        </p>
        <h2 className="titulo-seccion mt-4 text-tinta">Qué no</h2>
        <p className="mt-1">No pedimos tu nombre, tu correo ni tu número de teléfono. No necesitas cuenta ni descargar nada.</p>
        <h2 className="titulo-seccion mt-4 text-tinta">Para qué</h2>
        <p className="mt-1">Para respetar tu cupo de pedidos, contar un solo voto por canción y saber qué música funciona en el bar.</p>
        <h2 className="titulo-seccion mt-4 text-tinta">Si borras los datos del sitio</h2>
        <p className="mt-1">El código se pierde y tu teléfono cuenta como nuevo.</p>
      </div>

      <p className="secundario mt-4 text-[14px] leading-[1.45]">
        Tratamiento conforme a la Ley 1581 de 2012. El texto legal completo del bar está pendiente de publicarse.
      </p>

      <div className="mt-auto pt-8">
        <Link href={`/b/${slug}`} className={claseTecla('normal', 'grande')}>
          Volver
        </Link>
      </div>
    </MarcoCliente>
  );
}
