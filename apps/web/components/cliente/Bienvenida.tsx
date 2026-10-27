'use client';

import { Arco, Avance, claseTecla, Insignia, LuzEstado, TarjetaTitulo, Ventana } from '@rockola/ui';
import Link from 'next/link';
import { codigoDe } from '@/lib/api/errores';
import { useAhora, useAvisoVisto, useBar, useDestacadas, useYo } from '@/lib/api/hooks-cliente';
import { formatoDuracion, formatoHora } from '@/lib/formato';
import { posicionActual } from '@/lib/reglas/espera';
import { useAhoraServidor } from '@/lib/reloj-servidor';
import { useConexionCaida } from '@/lib/tiempo-real/estado';
import { conexion, T } from '@/lib/textos/cliente';
import { BarNoEncontrado, CargandoCliente, EnlacePolitica, EstadoCerrado, FalloCarga } from './Estados';
import { MarcoCliente } from './MarcoCliente';

/**
 * Bienvenida (prototipo 1, PB-03, PB-04, PB-06): el nombre del bar, lo que suena dentro del
 * arco, lo más votado de la noche y el aviso de datos, que se registra como visto (RF-004).
 * Con los pedidos cerrados, la pantalla es la del prototipo 4.
 */
export function Bienvenida({ slug }: { slug: string }) {
  const bar = useBar(slug);
  const ahora = useAhora(slug);
  const yo = useYo(slug);
  const destacadas = useDestacadas(slug);
  const instante = useAhoraServidor();
  const { caida } = useConexionCaida();
  useAvisoVisto(slug, yo.data);

  if (codigoDe(bar.error) === 'no_encontrado') return <BarNoEncontrado />;
  if (!bar.data || !ahora.data) {
    return (
      <MarcoCliente slug={slug}>
        {bar.error || ahora.error ? (
          <FalloCarga onReintentar={() => void Promise.all([bar.refetch(), ahora.refetch()])} />
        ) : (
          <CargandoCliente />
        )}
      </MarcoCliente>
    );
  }

  if (ahora.data.servicio.estado !== 'abierto') {
    return (
      <MarcoCliente slug={slug}>
        <EstadoCerrado slug={slug} bar={bar.data} ahora={ahora.data} />
      </MarcoCliente>
    );
  }

  const rep = ahora.data.reproduccion;
  const posicion = rep ? posicionActual(rep, ahora.data.servidor_ahora, instante || Date.parse(ahora.data.servidor_ahora)) : 0;
  const zona = bar.data.zona_horaria;

  return (
    <MarcoCliente slug={slug}>
      <header className="text-center">
        <h1 className="placa text-[25px]">{bar.data.nombre}</h1>
        <LuzEstado encendida={!caida} className="mt-2">
          {caida ? conexion.luz : T.enVivo}
        </LuzEstado>
      </header>

      <Arco className="mt-5" aria-label={T.suenaAhora}>
        <div className="flex min-h-[292px] flex-col justify-end px-5 pt-24 pb-5">
          <Ventana className="px-4 pt-3 pb-[14px]">
            {rep ? (
              <>
                <p className="ventana__etiqueta">{T.suenaAhora}</p>
                <h2 className="condensada mt-1 text-[40px] leading-none [overflow-wrap:anywhere]">{rep.cancion.titulo}</h2>
                <p className="mt-1 text-[16px]">{rep.cancion.artista}</p>
                {rep.votos !== null ? (
                  <Insignia className="mt-2">{rep.votos === 1 ? '1 voto' : `${rep.votos} votos`}</Insignia>
                ) : (
                  <p className="mt-2 text-[13px] font-semibold text-tinta-2">De la {T.listaDeLaCasa.toLowerCase()}</p>
                )}
                <Avance
                  className="mt-[10px] text-tinta-2 [&_.surco]:text-tinta"
                  valor={posicion}
                  maximo={rep.cancion.duracion_seg}
                  textoInicio={formatoDuracion(posicion)}
                  textoFin={formatoDuracion(rep.cancion.duracion_seg)}
                />
              </>
            ) : (
              <>
                <h2 className="condensada text-[32px] leading-none">{T.nadaSuena}</h2>
                <p className="mt-2 text-[15px] text-tinta-2">Pide una canción y será la primera en sonar.</p>
              </>
            )}
          </Ventana>
        </div>
      </Arco>

      <section className="mt-6" aria-labelledby="t-top">
        <h2 id="t-top" className="titulo-seccion mb-3 text-center">
          {T.loMasVotado}
        </h2>
        {destacadas.data ? (
          destacadas.data.destacadas.length > 0 ? (
            <ol className="filas">
              {destacadas.data.destacadas.map((d, i) => {
                const contexto = d.contexto === 'sonando' ? 'suena ahora' : d.contexto === 'sono' && d.hora ? `sonó a las ${formatoHora(d.hora, zona)}` : 'en la cola';
                return (
                  <TarjetaTitulo
                    key={d.cancion.video_id}
                    como="li"
                    puesto={i + 1}
                    titulo={d.cancion.titulo}
                    artista={`${d.cancion.artista}, ${contexto}`}
                    votos={d.votos}
                    miniatura={d.cancion.miniatura_url}
                  />
                );
              })}
            </ol>
          ) : (
            <p className="vacio">{T.sinVotosAun}</p>
          )
        ) : (
          <div className="grid gap-2" aria-hidden="true">
            <div className="esqueleto h-14" />
            <div className="esqueleto h-14" />
          </div>
        )}
      </section>

      <div className="mt-auto pt-8 text-center">
        <p className="secundario mb-4 text-[14px] leading-[1.45]">
          {T.privacidadBreve}{' '}
          {/* Enlace dentro de la frase: el relleno con margen negativo le da 44 px de toque sin mover el texto. */}
          <Link href={`/b/${slug}/politica-de-datos`} className="-my-[15px] inline-block py-[15px] font-semibold text-crema underline underline-offset-[3px]">
            {T.politica}
          </Link>
        </p>
        <Link href={`/b/${slug}/buscar`} className={claseTecla('cereza', 'grande')}>
          {T.entrar}
        </Link>
      </div>
    </MarcoCliente>
  );
}

export { EnlacePolitica };
