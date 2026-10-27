'use client';

import type { Ahora, ItemCola, MiSolicitud, Yo } from '@rockola/contrato';
import { claseTecla, Icono, Insignia, LuzEstado, TarjetaTitulo } from '@rockola/ui';
import Link from 'next/link';
import { Aviso } from '@/components/comun/Aviso';
import { codigoDe } from '@/lib/api/errores';
import { useAhora, useBar, useYo } from '@/lib/api/hooks-cliente';
import { formatoEspera, formatoEsperaCorta, formatoResumenCola } from '@/lib/formato';
import { duracionCola, ejemploDesempate, puestosSubidos } from '@/lib/reglas/cola';
import { esperaHasta, restanteActual } from '@/lib/reglas/espera';
import { aceptaPedidos, estadoTecla, solicitudesPropiasPendientes } from '@/lib/reglas/pedidos';
import { useAhoraServidor } from '@/lib/reloj-servidor';
import { useConexionCaida } from '@/lib/tiempo-real/estado';
import { colaCerrada, colaVacia, conexion, desempate, ESPERAS_ESTIMADAS, estadoPedido, luzServicio, T } from '@/lib/textos/cliente';
import { BarNoEncontrado, CargandoCliente, EnlacePolitica, FalloCarga } from './Estados';
import { FilaCancion } from './FilaCancion';
import { MarcoCliente } from './MarcoCliente';
import { useVotarConAviso } from './useVotarConAviso';

/**
 * La cola en tiempo real (prototipo 3, PB-14, PB-17 a PB-19, PB-22): «Tu canción», la cola
 * ordenada por votos con la espera de cada una, la regla de desempate con el caso real y lo
 * que pasó con los pedidos de este teléfono.
 */
export function Cola({ slug }: { slug: string }) {
  const bar = useBar(slug);
  const ahora = useAhora(slug);
  const yo = useYo(slug);
  const instante = useAhoraServidor();
  const { alVotar, aviso, cerrarAviso, anuncio, enviando } = useVotarConAviso(slug);
  const { caida } = useConexionCaida();

  if (codigoDe(bar.error) === 'no_encontrado') return <BarNoEncontrado />;
  if (!bar.data || !ahora.data) {
    return (
      <MarcoCliente slug={slug} pie>
        {bar.error || ahora.error ? <FalloCarga onReintentar={() => void Promise.all([bar.refetch(), ahora.refetch()])} /> : <CargandoCliente arco={false} />}
      </MarcoCliente>
    );
  }

  const foto = ahora.data;
  const cola = foto.cola;
  const abierto = aceptaPedidos(foto.servicio.estado);
  const restante = restanteActual(foto.reproduccion, foto.servidor_ahora, instante || Date.parse(foto.servidor_ahora));
  const propias = solicitudesPropiasPendientes(yo.data);
  const tuya = cola.find((i) => propias.has(i.solicitud_id));
  const par = ejemploDesempate(cola, propias);

  return (
    <MarcoCliente slug={slug} pie>
      <p className="solo-lector" role="status">
        {anuncio}
      </p>
      <header className="flex items-start justify-between gap-3">
        <div>
          <h1 className="placa text-[24px]">{T.laCola}</h1>
          <p className="secundario mt-1 text-[14px]">{formatoResumenCola(cola.length, duracionCola(cola))}</p>
        </div>
        <LuzEstado encendida={abierto && !caida} className="mt-[6px] shrink-0">
          {caida ? conexion.luz : luzServicio(foto.servicio.estado)}
        </LuzEstado>
      </header>

      {foto.servicio.estado !== 'abierto' ? <Aviso tipo="info" className="mt-4" meta={colaCerrada(foto.servicio.estado)} /> : null}

      {tuya ? (
        <TuCancion foto={foto} item={tuya} restante={restante} />
      ) : yo.data && foto.reproduccion?.solicitud_id && yo.data.solicitudes.some((s) => s.solicitud_id === foto.reproduccion?.solicitud_id) ? (
        <TuCancionSuena titulo={foto.reproduccion.cancion.titulo} artista={foto.reproduccion.cancion.artista} />
      ) : null}

      {aviso ? <Aviso tipo={aviso.tipo} className="mt-4" titulo={aviso.titulo} meta={aviso.meta} accion={<button type="button" onClick={cerrarAviso}>Cerrar</button>} /> : null}

      {cola.length > 0 ? (
        <ol className="filas mt-[14px]" aria-label="Canciones en la cola">
          {cola.map((item, i) => (
            <FilaCancion
              key={item.solicitud_id}
              item={item}
              tecla={estadoTecla(item.solicitud_id, yo.data)}
              artista={`${item.cancion.artista}, ${formatoEsperaCorta(esperaHasta(cola, i, restante))}`}
              puedeVotar={abierto && Boolean(yo.data)}
              ocupada={enviando === item.solicitud_id}
              onVotar={alVotar}
            />
          ))}
        </ol>
      ) : (
        <div className="vacio mt-[14px]">
          <p>
            <b>{(foto.lista_casa ? colaVacia.conLista : colaVacia.sinLista).titulo}</b> {(foto.lista_casa ? colaVacia.conLista : colaVacia.sinLista).meta}
          </p>
          {abierto ? (
            <Link href={`/b/${slug}/buscar`} className={claseTecla('cereza', 'chica', 'mt-3')}>
              {T.pedirUna}
            </Link>
          ) : null}
        </div>
      )}

      {cola.length > 0 ? (
        <p className="secundario mt-3 text-[14px] leading-[1.45]">
          {par ? (
            <>
              {desempate(par.antes.cancion.titulo, par.despues.cancion.titulo).regla} Por eso <b className="font-bold text-crema">{par.antes.cancion.titulo}</b> va antes que{' '}
              <b className="font-bold text-crema">{par.despues.cancion.titulo}</b>.{' '}
            </>
          ) : null}
          {ESPERAS_ESTIMADAS}
        </p>
      ) : null}

      {yo.data ? <TusPedidos yo={yo.data} /> : null}

      <EnlacePolitica slug={slug} className="mt-auto pt-6" />
    </MarcoCliente>
  );
}

/** La tarjeta destacada (PB-19, PB-22): en qué puesto va, si subió y cuánto falta. */
function TuCancion({ foto, item, restante }: { foto: Ahora; item: ItemCola; restante: number }) {
  const { puesto } = item;
  const indice = foto.cola.indexOf(item);
  const subio = puestosSubidos(item);
  return (
    <section className="marco mt-4" aria-label="Tu canción">
      <div className="marco__luz">
        <div className="ventana px-[14px] pt-[10px] pb-3 text-left">
          <div className="flex items-center justify-between gap-2">
            <p className="text-[14px] font-semibold text-tinta-2">{T.tuCancionVaDe}</p>
            {subio > 0 ? (
              <Insignia className="h-[26px]">
                <Icono nombre="subir" />
                {subio === 1 ? 'Subió 1 puesto' : `Subió ${subio} puestos`}
              </Insignia>
            ) : null}
          </div>
          <div className="mt-[2px] flex items-baseline gap-3">
            <p className="cifra text-[60px] tracking-[-.01em]">
              <span className="solo-lector">Puesto </span>#{puesto}
            </p>
            <div className="min-w-0">
              <p className="condensada text-[30px] leading-none [overflow-wrap:anywhere]">{item.cancion.titulo}</p>
              <p className="mt-[3px] text-[14px] text-tinta-2">
                {item.cancion.artista}, {item.votos_total === 1 ? '1 voto' : `${item.votos_total} votos`}
              </p>
            </div>
          </div>
          <p className="mt-[6px] text-[14px] text-tinta-2">Suena {formatoEspera(esperaHasta(foto.cola, indice, restante))}</p>
        </div>
      </div>
    </section>
  );
}

function TuCancionSuena({ titulo, artista }: { titulo: string; artista: string }) {
  return (
    <section className="marco mt-4" aria-label="Tu canción">
      <div className="marco__luz">
        <div className="ventana px-[14px] pt-[10px] pb-3 text-left">
          <p className="text-[14px] font-semibold text-tinta-2">{T.tuCancionSuena}</p>
          <p className="condensada mt-1 text-[30px] leading-none">{titulo}</p>
          <p className="mt-[3px] text-[14px] text-tinta-2">{artista}</p>
        </div>
      </div>
    </section>
  );
}

/** RF-013: todos los pedidos de este teléfono esta noche, con su estado y el motivo si lo hay. */
function TusPedidos({ yo }: { yo: Yo }) {
  if (yo.solicitudes.length === 0) return null;
  return (
    <section className="mt-7" aria-labelledby="t-tus-pedidos">
      <h2 id="t-tus-pedidos" className="titulo-seccion mb-3">
        {T.tusPedidos}
      </h2>
      <ul className="filas">
        {yo.solicitudes.map((s) => (
          <TarjetaTitulo key={s.solicitud_id} como="li" titulo={s.cancion.titulo} artista={lineaPedido(s)} variante={varianteDe(s)} miniatura={s.cancion.miniatura_url} />
        ))}
      </ul>
    </section>
  );
}

function lineaPedido(s: MiSolicitud): string {
  const estado = estadoPedido(s.estado, s.puesto);
  return s.motivo ? `${estado}: ${s.motivo}` : estado;
}

function varianteDe(s: MiSolicitud): 'tuya' | 'apagada' | 'normal' {
  if (s.estado === 'pendiente' || s.estado === 'reproduciendo') return 'tuya';
  if (s.estado === 'reproducida') return 'normal';
  return 'apagada';
}
