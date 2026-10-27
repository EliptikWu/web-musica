'use client';

import type { ResultadoBusqueda } from '@rockola/contrato';
import { Fichas, Icono, TarjetaTitulo, Tecla, TeclaFija } from '@rockola/ui';
import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { codigoDe, ErrorApiCliente } from '@/lib/api/errores';
import { nuevaClave, useAhora, useBar, useBusqueda, usePedir, useYo } from '@/lib/api/hooks-cliente';
import { formatoDuracion, formatoHora } from '@/lib/formato';
import { aceptaPedidos, estadoTecla } from '@/lib/reglas/pedidos';
import { busqueda as textosBusqueda, cupo, MOTIVO_CORTO, motivoLargo, pedido, sinCupo, T } from '@/lib/textos/cliente';
import { BarNoEncontrado, CargandoCliente, EnlacePolitica, EstadoCerrado, FalloCarga } from './Estados';
import { FilaCancion } from './FilaCancion';
import { MarcoCliente } from './MarcoCliente';
import { useVotarConAviso } from './useVotarConAviso';

type AvisoPedido = { tipo: 'luz' | 'error' | 'info'; titulo: string; meta: string; verCola?: boolean } | null;

/** Lo que este teléfono acaba de pedir: se ve «Tuya» con su puesto mientras dure la búsqueda. */
type Pedida = { videoId: string; puesto: number };

/**
 * Buscar, pedir y votar (prototipo 2, PB-07 a PB-09, PB-13 a PB-15, PB-21). Busca mientras se
 * escribe; lo que ya está en la cola se vota en vez de pedirse (RN-02); lo que no se puede
 * pedir dice por qué en lugar de un botón gris (RN-06, RN-07).
 */
export function Buscar({ slug }: { slug: string }) {
  const bar = useBar(slug);
  const ahora = useAhora(slug);
  const yo = useYo(slug);
  const [texto, setTexto] = useState('');
  const [consulta, setConsulta] = useState('');
  const [aviso, setAviso] = useState<AvisoPedido>(null);
  const [pedidas, setPedidas] = useState<Pedida[]>([]);
  const [enviando, setEnviando] = useState<string | null>(null);
  const resultados = useBusqueda(slug, consulta);
  const pedir = usePedir(slug);
  const votos = useVotarConAviso(slug);
  const refAviso = useRef<HTMLDivElement>(null);

  // Espera 300 ms tras la última tecla antes de buscar (RF-005).
  useEffect(() => {
    const t = setTimeout(() => setConsulta(texto), 300);
    return () => clearTimeout(t);
  }, [texto]);

  if (codigoDe(bar.error) === 'no_encontrado') return <BarNoEncontrado />;
  if (!bar.data || !ahora.data) {
    return (
      <MarcoCliente slug={slug} pie>
        {bar.error || ahora.error ? <FalloCarga onReintentar={() => void Promise.all([bar.refetch(), ahora.refetch()])} /> : <CargandoCliente arco={false} />}
      </MarcoCliente>
    );
  }
  if (!aceptaPedidos(ahora.data.servicio.estado)) {
    return (
      <MarcoCliente slug={slug} pie>
        <EstadoCerrado slug={slug} bar={bar.data} ahora={ahora.data} />
      </MarcoCliente>
    );
  }

  const zona = bar.data.zona_horaria;
  const maximo = yo.data?.pedidos_maximos ?? bar.data.pedidos_maximos;
  const restantes = yo.data?.pedidos_restantes ?? maximo;
  const datos = consulta.trim().length >= 2 ? resultados.data : undefined;

  function alPedir(r: ResultadoBusqueda) {
    if (enviando) return;
    const { titulo } = r.cancion;
    setAviso(null);
    setEnviando(r.cancion.video_id);
    pedir.mutate(
      { videoId: r.cancion.video_id, clave: nuevaClave() },
      {
        onSuccess: (respuesta) => {
          setPedidas((p) => [...p.filter((x) => x.videoId !== r.cancion.video_id), { videoId: r.cancion.video_id, puesto: respuesta.puesto }]);
          setAviso(
            respuesta.resultado === 'creada'
              ? { tipo: 'luz', ...pedido.entro(titulo, respuesta.puesto, respuesta.espera_seg), verCola: true }
              : { tipo: 'luz', ...pedido.eraVoto(titulo, respuesta.puesto), verCola: true },
          );
          // La fila cambia de sección: el foco va al aviso, que dice qué pasó (WCAG 2.4.3).
          requestAnimationFrame(() => refAviso.current?.focus());
        },
        onError: (error) => {
          const codigo = codigoDe(error);
          if (codigo === 'limite_pedidos') setAviso({ tipo: 'info', ...sinCupo(maximo) });
          else if (codigo === 'servicio_no_disponible') setAviso({ tipo: 'info', ...pedido.cerrado });
          else if (codigo === 'bloqueada' || codigo === 'no_disponible' || codigo === 'no_embebible' || codigo === 'muy_larga' || codigo === 'reproducida_hace_poco') {
            const desde = error instanceof ErrorApiCliente && typeof error.detalle?.disponible_desde === 'string' ? error.detalle.disponible_desde : null;
            setAviso({ tipo: 'info', ...pedido.rechazado(titulo, motivoLargo(codigo, desde, zona)) });
          } else setAviso({ tipo: 'error', ...pedido.fallo(titulo) });
        },
        onSettled: () => setEnviando(null),
      },
    );
  }

  return (
    <MarcoCliente slug={slug} pie>
      <p className="solo-lector" role="status">
        {votos.anuncio || (resultados.isPlaceholderData ? '' : anuncioResultados(consulta, datos))}
      </p>
      <header>
        <h1 className="placa text-[24px]">{T.pideUnaCancion}</h1>
        <p className="secundario mt-[10px] flex items-center gap-[10px] text-[14px]">
          <Fichas libres={restantes} total={maximo} />
          {cupo(restantes, maximo)}
        </p>
      </header>

      <form className="buscador mt-4" role="search" onSubmit={(e) => e.preventDefault()}>
        <Icono nombre="buscar" />
        <input
          type="search"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          aria-label={T.buscarEtiqueta}
          placeholder={T.buscarMarcador}
          enterKeyHint="search"
          autoComplete="off"
          autoCorrect="off"
          spellCheck={false}
          maxLength={80}
        />
        {texto ? (
          <button type="button" aria-label={T.borrarBusqueda} onClick={() => setTexto('')}>
            <Icono nombre="borrar" />
          </button>
        ) : null}
      </form>

      <div ref={refAviso} tabIndex={-1} className="outline-none">
        {aviso ? (
          <Aviso
            tipo={aviso.tipo}
            className="mt-[14px]"
            titulo={aviso.titulo}
            meta={aviso.meta}
            accion={aviso.verCola ? <Link href={`/b/${slug}/cola`}>{T.verCola}</Link> : <button type="button" onClick={() => setAviso(null)}>Cerrar</button>}
          />
        ) : null}
      </div>
      {votos.aviso ? <Aviso tipo={votos.aviso.tipo} className="mt-[14px]" titulo={votos.aviso.titulo} meta={votos.aviso.meta} accion={<button type="button" onClick={votos.cerrarAviso}>Cerrar</button>} /> : null}
      {restantes <= 0 && !aviso ? <Aviso tipo="info" className="mt-[14px]" {...sinCupo(maximo)} /> : null}

      {consulta.trim().length < 2 ? (
        <p className="secundario mt-5 text-center text-[15px]">{T.buscarVacio}</p>
      ) : resultados.isError && !datos ? (
        <ErrorDeBusqueda error={resultados.error} onReintentar={() => void resultados.refetch()} />
      ) : !datos || (resultados.isPlaceholderData && datos.en_cola.length === 0 && datos.resultados.length === 0) ? (
        <div className="mt-5 grid gap-2" aria-busy="true">
          <p className="solo-lector" role="status">
            Buscando…
          </p>
          <div className="esqueleto h-14" />
          <div className="esqueleto h-14" />
          <div className="esqueleto h-14" />
        </div>
      ) : datos.en_cola.length === 0 && datos.resultados.length === 0 ? (
        <div className="vacio mt-5">
          <p>
            <b>{textosBusqueda.sinResultados(datos.consulta).titulo}.</b> {textosBusqueda.sinResultados(datos.consulta).meta}
          </p>
        </div>
      ) : (
        <div aria-busy={resultados.isFetching || undefined}>
          {datos.desde_cache ? <Aviso tipo="info" className="mt-[14px]" meta={textosBusqueda.desdeCache} /> : null}

          {datos.en_cola.length > 0 ? (
            <section className="mt-[18px]" aria-labelledby="t-cola">
              <div className="mb-[10px] flex items-baseline justify-between gap-3">
                <h2 id="t-cola" className="titulo-seccion">
                  {T.yaEnLaCola}
                </h2>
                <span className="secundario text-[13px]">{T.votaEnVez}</span>
              </div>
              <ul className="filas">
                {datos.en_cola.map((item) => (
                  <FilaCancion
                    key={item.solicitud_id}
                    item={item}
                    tecla={estadoTecla(item.solicitud_id, yo.data)}
                    puedeVotar={Boolean(yo.data)}
                    ocupada={votos.enviando === item.solicitud_id}
                    onVotar={votos.alVotar}
                  />
                ))}
              </ul>
            </section>
          ) : null}

          {datos.resultados.length > 0 ? (
            <section className="mt-[18px]" aria-labelledby="t-yt">
              <h2 id="t-yt" className="titulo-seccion mb-[10px]">
                {T.resultadosYoutube}
              </h2>
              <ul className="filas">
                {datos.resultados.map((r) => (
                  <FilaResultado
                    key={r.cancion.video_id}
                    resultado={r}
                    pedida={pedidas.find((p) => p.videoId === r.cancion.video_id)}
                    sinCupo={restantes <= 0}
                    ocupada={enviando === r.cancion.video_id}
                    zona={zona}
                    onPedir={alPedir}
                  />
                ))}
              </ul>
            </section>
          ) : null}
        </div>
      )}

      <EnlacePolitica slug={slug} className="mt-auto pt-6" />
    </MarcoCliente>
  );
}

function FilaResultado({
  resultado,
  pedida,
  sinCupo,
  ocupada,
  zona,
  onPedir,
}: {
  resultado: ResultadoBusqueda;
  pedida: Pedida | undefined;
  sinCupo: boolean;
  ocupada: boolean;
  zona: string;
  onPedir: (r: ResultadoBusqueda) => void;
}) {
  const { cancion, pedible, motivo, disponible_desde } = resultado;
  const duracion = formatoDuracion(cancion.duracion_seg);
  if (pedida) {
    return (
      <li className="fila">
        <TarjetaTitulo titulo={cancion.titulo} artista={`${cancion.artista}, puesto ${pedida.puesto}`} duracion={duracion} variante="tuya" miniatura={cancion.miniatura_url} />
        <TeclaFija variante="tuya">Tuya</TeclaFija>
      </li>
    );
  }
  if (!pedible && motivo) {
    const artista = motivo === 'reproducida_hace_poco' && disponible_desde ? `${cancion.artista}, se puede pedir desde las ${formatoHora(disponible_desde, zona)}` : cancion.artista;
    return (
      <li className="fila">
        <TarjetaTitulo titulo={cancion.titulo} artista={artista} duracion={duracion} variante="apagada" miniatura={cancion.miniatura_url} />
        <p className="motivo">{MOTIVO_CORTO[motivo]}</p>
      </li>
    );
  }
  return (
    <li className="fila">
      <TarjetaTitulo titulo={cancion.titulo} artista={cancion.artista} duracion={duracion} miniatura={cancion.miniatura_url} />
      {sinCupo ? (
        <p className="motivo">{MOTIVO_CORTO.sin_cupo}</p>
      ) : (
        <Tecla variante="cereza" aria-label={`Pedir ${cancion.titulo}`} ocupada={ocupada} onClick={() => onPedir(resultado)}>
          Pedir
        </Tecla>
      )}
    </li>
  );
}

/** Lo que oye un lector de pantalla cuando llegan resultados mientras escribe. */
function anuncioResultados(consulta: string, datos: { en_cola: unknown[]; resultados: unknown[] } | undefined): string {
  if (consulta.trim().length < 2 || !datos) return '';
  const { en_cola, resultados } = datos;
  if (en_cola.length === 0 && resultados.length === 0) return 'Sin resultados';
  const partes = [en_cola.length > 0 && `${en_cola.length} en la cola`, resultados.length > 0 && `${resultados.length} en YouTube`].filter(Boolean);
  return partes.join(' y ');
}

function ErrorDeBusqueda({ error, onReintentar }: { error: unknown; onReintentar: () => void }) {
  const codigo = codigoDe(error);
  const textos = codigo === 'red' ? textosBusqueda.sinRed : textosBusqueda.youtubeCaido;
  return (
    <Aviso
      tipo="error"
      className="mt-5"
      titulo={textos.titulo}
      meta={textos.meta}
      accion={
        <button type="button" onClick={onReintentar}>
          {T.reintentar}
        </button>
      }
    />
  );
}
