'use client';

import type { Ahora, Bar, ItemCola, VinculoPantalla } from '@rockola/contrato';
import { Arco, Avance, DiasSemana, Icono, LuzEstado, TarjetaTitulo, Tecla } from '@rockola/ui';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { QRCodeSVG } from 'qrcode.react';
import { type ReactNode, useLayoutEffect, useRef, useState } from 'react';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { codigoDe } from '@/lib/api/errores';
import { useAhora, useBar } from '@/lib/api/hooks-cliente';
import { DIAS, formatoDuracion, formatoEsperaCorta, resumenHorario } from '@/lib/formato';
import { puestosSubidos } from '@/lib/reglas/cola';
import { avanceDesdeFoto, esperaDe, posicionActual } from '@/lib/reglas/espera';
import { diasQueAbren, momentoLocal } from '@/lib/reglas/horario';
import { useAhoraServidor } from '@/lib/reloj-servidor';
import { claveDe } from '@/lib/idempotencia';
import { useConexionCaida } from '@/lib/tiempo-real/estado';
import { conexion, luzServicio, servicioCerrado } from '@/lib/textos/cliente';
import { TV } from '@/lib/textos/pantalla';
import { Reproductor } from './Reproductor';
import { useVinculoPantalla } from './vinculo';

const VISIBLES = 5;

/**
 * La pantalla del bar (prototipo pantalla-bar, PB-17 a PB-20, PB-38): el video dentro del arco,
 * lo que suena con su origen, lo que viene con sus votos en grande y el QR para participar.
 * Se ve desde lejos y se actualiza sola.
 */
export function PantallaBar({ slug }: { slug: string }) {
  const { vinculo, sinRed, desvinculada, alPerderVinculo } = useVinculoPantalla(slug);
  const bar = useBar(slug);
  const ahora = useAhora(slug);
  const qr = useQuery({ queryKey: claves.qrPublico(slug), queryFn: ({ signal }) => pedir('qrPublico', { parametros: { slug }, signal }), staleTime: 0, refetchInterval: 30_000 });
  const instante = useAhoraServidor();
  const { caida } = useConexionCaida();
  const [encendida, setEncendida] = useState(false);
  const consultas = useQueryClient();
  const reporte = useMutation({
    mutationFn: ({ tipo, id, posicion, codigo }: { tipo: 'reportarInicio' | 'reportarFin' | 'reportarError'; id: string; posicion: number; codigo: number | null }) =>
      // La clave es fija por reproducción y tipo: dos pantallas que reportan el mismo fin no saltan dos canciones.
      pedir(tipo, { parametros: { slug, id }, cuerpo: { posicion_seg: posicion, codigo_youtube: codigo }, idempotencia: claveDe(`${id}-${tipo}`) }),
    onError: (error) => {
      // 401: la desvincularon desde el panel; vuelve a mostrar un código (RF-058).
      if (codigoDe(error) === 'no_autenticado') alPerderVinculo();
      else console.error('[pantalla] no se pudo reportar', error);
    },
    onSuccess: (_respuesta, variables) => {
      // Si la señal SSE se retrasa o cae, la TV vuelve a consultar qué debe sonar.
      if (variables.tipo !== 'reportarInicio') void consultas.invalidateQueries({ queryKey: claves.ahora(slug) });
    },
  });

  if (codigoDe(bar.error) === 'no_encontrado') {
    return (
      <LienzoTv apagada>
        <div className="grid h-full place-items-center">
          <p className="placa text-[30px]">{TV.barNoEncontrado}</p>
        </div>
      </LienzoTv>
    );
  }
  if (!vinculo.data) {
    return (
      <LienzoTv>
        <div className="grid h-full place-items-center">
          <p className="secundario text-[18px]" role="status">
            {vinculo.error ? TV.noCargo : TV.cargando}
          </p>
        </div>
      </LienzoTv>
    );
  }
  if (vinculo.data.estado !== 'vinculada') {
    return <SinVincular nombreBar={bar.data?.nombre ?? null} vinculo={vinculo.data} sinRed={sinRed} desvinculada={desvinculada} instante={instante} />;
  }
  if (!bar.data || !ahora.data) {
    return (
      <LienzoTv>
        <div className="grid h-full place-items-center">
          <p className="secundario text-[18px]" role="status">
            {bar.error || ahora.error ? TV.noCargo : TV.cargando}
          </p>
        </div>
      </LienzoTv>
    );
  }

  const foto = ahora.data;
  const rep = foto.reproduccion;
  const momento = instante || Date.parse(foto.servidor_ahora);
  const posicion = rep ? posicionActual(rep, foto.servidor_ahora, momento) : 0;
  const avance = avanceDesdeFoto(rep, foto.servidor_ahora, momento);
  const estado = foto.servicio.estado;
  const apagada = estado !== 'abierto';

  return (
    <LienzoTv apagada={apagada}>
      {caida ? <p className="tv-banner" role="status">{conexion.reconectando}</p> : null}
      <div className="tv">
        <section className="tv__izq" aria-label="Suena ahora">
          <Arco apagado={apagada}>
            <h1 className="placa placa-bar">{bar.data.nombre}</h1>
            {rep ? (
              <Reproductor
                reproduccion={rep}
                posicionSeg={posicion}
                encendida={encendida}
                volumen={foto.volumen}
                onInicio={(id, p) => reporte.mutate({ tipo: 'reportarInicio', id, posicion: p, codigo: null })}
                onFin={(id, p) => reporte.mutate({ tipo: 'reportarFin', id, posicion: p, codigo: null })}
                onError={(id, p, codigo) => reporte.mutate({ tipo: 'reportarError', id, posicion: p, codigo })}
              />
            ) : (
              <div className="video">
                <div className="video__centro">
                  <p className="placa text-[24px]">{TV.nadaSuena}</p>
                </div>
              </div>
            )}
          </Arco>

          {rep ? (
            <div className="tarjeta ahora-tv">
              <div className="tarjeta__centro">
                <p className="ahora-tv__etiqueta">{rep.origen === 'lista_casa' ? TV.suenaCasa : TV.suenaPedida}</p>
                <h2 className="ahora-tv__titulo condensada">{rep.cancion.titulo}</h2>
                <p className="tarjeta__artista">{rep.cancion.artista}</p>
                <Avance valor={posicion} maximo={rep.cancion.duracion_seg} textoInicio={formatoDuracion(posicion)} textoFin={formatoDuracion(rep.cancion.duracion_seg)} />
              </div>
              {rep.votos !== null ? (
                <p className="tarjeta__votos">
                  {rep.votos}
                  <small>{rep.votos === 1 ? 'voto' : 'votos'}</small>
                </p>
              ) : (
                <p className="tarjeta__votos">
                  <span className="ahora-tv__casa">{TV.listaDeLaCasa}</span>
                </p>
              )}
            </div>
          ) : null}
        </section>

        <section className="tv__der" aria-labelledby="t-viene">
          <div className="tv__cabeza">
            <h2 id="t-viene" className="placa">
              {TV.loQueViene}
            </h2>
            <LuzEstado encendida={!apagada && !caida}>{caida ? conexion.luz : luzServicio(estado)}</LuzEstado>
          </div>
          <LadoDerecho foto={foto} bar={bar.data} avance={avance} />
          <PlacaQr url={qr.data?.url ?? null} />
        </section>
      </div>

      {!encendida ? <Encender onEncender={() => setEncendida(true)} /> : null}
    </LienzoTv>
  );
}

function LadoDerecho({ foto, bar, avance }: { foto: Ahora; bar: Bar; avance: number }) {
  const estado = foto.servicio.estado;
  if (estado === 'fuera_de_horario' || estado === 'mantenimiento') {
    const abren = diasQueAbren(bar.horario);
    const hoy = momentoLocal(new Date(Date.parse(foto.servidor_ahora)), bar.zona_horaria).dia;
    // El mismo texto que el teléfono: dice a qué hora abren (RF-003).
    const textos = estado === 'mantenimiento' ? TV.mantenimiento : { titulo: TV.cerrado.titulo, detalle: servicioCerrado(estado, foto.servicio.proxima_apertura, bar.zona_horaria, Date.parse(foto.servidor_ahora)).detalle };
    return (
      <div className="tv-aviso">
        <h3>{textos.titulo}</h3>
        <p>{textos.detalle}</p>
        <DiasSemana dias={DIAS.map((d, i) => ({ ...d, abre: abren.has(i + 1), hoy: hoy === i + 1 }))} />
        {resumenHorario(bar.horario).map((l) => (
          <p key={l.dias}>
            {l.dias}, <b className="text-tinta">{l.horas}</b>
          </p>
        ))}
      </div>
    );
  }
  if (foto.cola.length === 0) {
    return (
      <div className="tv-aviso">
        <h3>{TV.colaVacia.titulo}</h3>
        <p>{foto.lista_casa ? TV.colaVacia.conLista : TV.colaVacia.sinLista}</p>
      </div>
    );
  }
  const visibles = foto.cola.slice(0, VISIBLES);
  const resto = foto.cola.length - visibles.length;
  return (
    <>
      <ol className="filas viene">
        {visibles.map((item) => (
          <FilaTv key={item.solicitud_id} item={item} espera={esperaDe(item, avance)} />
        ))}
      </ol>
      <p className="viene__mas secundario">
        {estado === 'en_pausa' ? `${TV.enPausa} ` : ''}
        {resto > 0 ? `${TV.yMas(resto)} ` : ''}
        {TV.desempate}
      </p>
    </>
  );
}

function FilaTv({ item, espera }: { item: ItemCola; espera: number }) {
  const subio = puestosSubidos(item);
  // La tarjeta de la TV lleva una columna más: lo que subió (↑N), como en el prototipo.
  return (
    <TarjetaTitulo
      como="li"
      puesto={item.puesto}
      titulo={item.cancion.titulo}
      artista={`${item.cancion.artista}, ${formatoEsperaCorta(espera)}`}
      votos={item.votos_total}
      etiquetaVotos={false}
      miniatura={item.cancion.miniatura_url}
      columnaExtra={
        <span className="sube">
          {subio > 0 ? (
            <>
              <Icono nombre="subir" />
              {subio}
              <span className="solo-lector">{subio === 1 ? ' puesto subido' : ' puestos subidos'}</span>
            </>
          ) : null}
        </span>
      }
    />
  );
}

function PlacaQr({ url }: { url: string | null }) {
  return (
    <div className="qr-tv">
      <div className="qr-tv__placa">
        <div className="qr-tv__codigo">
          {/* QRCodeSVG ya es role=img: su nombre va en el título del SVG. Sus colores los pone el CSS
              (`color: var(--qr-tinta)` sobre `--qr-fondo`): siguen al tema aunque cambie en vivo. */}
          {url ? <QRCodeSVG value={url} size={102} level="M" fgColor="currentColor" bgColor="transparent" marginSize={0} title={TV.qrEtiqueta} /> : null}
        </div>
        <div>
          <h2 className="placa">{TV.escaneaYVota}</h2>
          <p>{TV.sinDescargas}</p>
          {url ? <p className="qr-tv__enlace">{url.replace(/^https?:\/\//, '')}</p> : null}
        </div>
      </div>
    </div>
  );
}

/**
 * La TV sin vincular (RF-058): el código dentro del arco apagado, como teclas de selección
 * que se leen desde la barra, y los pasos para escribirlo en el panel. No reproduce ni reporta.
 */
function SinVincular({
  nombreBar,
  vinculo,
  sinRed,
  desvinculada,
  instante,
}: {
  nombreBar: string | null;
  vinculo: Exclude<VinculoPantalla, { estado: 'vinculada' }>;
  sinRed: boolean;
  desvinculada: boolean;
  instante: number;
}) {
  const t = TV.vincular;
  const codigo = vinculo.estado === 'esperando' ? vinculo.codigo : null;
  const minutos = vinculo.estado === 'esperando' && instante ? Math.floor((Date.parse(vinculo.expira_en) - instante) / 60_000) : null;
  return (
    <LienzoTv apagada>
      {desvinculada ? <p className="tv-banner" role="status">{t.desvinculada}</p> : null}
      <div className="tv tv--vincular">
        <section className="tv__izq" aria-label={t.etiquetaCodigo}>
          <Arco apagado>
            {nombreBar ? <h1 className="placa placa-bar">{nombreBar}</h1> : null}
            <div className="video">
              <div className="video__centro">
                {codigo ? (
                  <div className="codigo-tv">
                    <p className="codigo-tv__etiqueta">{t.etiquetaCodigo}</p>
                    <p>
                      {/* El lector lo deletrea; las teclas son solo para verlo de lejos. */}
                      <span className="solo-lector">{codigo.split('').join(' ')}</span>
                      <span className="codigo-tv__teclas" aria-hidden="true">
                        {codigo.split('').map((letra, i) => (
                          <span key={i} className="tecla">
                            {letra}
                          </span>
                        ))}
                      </span>
                    </p>
                  </div>
                ) : (
                  <p className="placa text-[24px]" role="status">
                    {sinRed ? t.noPidio : t.pidiendo}
                  </p>
                )}
              </div>
            </div>
          </Arco>
        </section>

        <section className="tv__der" aria-labelledby="t-vincular">
          <div className="tv__cabeza">
            <h2 id="t-vincular" className="placa">
              {t.titulo}
            </h2>
            <LuzEstado encendida={false}>{t.luz}</LuzEstado>
          </div>
          <div className="tv-aviso">
            <p className="mt-0!">{t.porQue}</p>
            <ol className="pasos-tv">
              {t.pasos.map((paso) => (
                <li key={paso}>{paso}</li>
              ))}
            </ol>
          </div>
          {minutos !== null ? <p className="vence-tv">{t.vence(Math.max(0, minutos))}</p> : null}
        </section>
      </div>
    </LienzoTv>
  );
}

/**
 * El navegador no deja sonar un video con audio sin un toque (doc 03). La pantalla se enciende
 * una vez, a pantalla completa si se puede.
 */
function Encender({ onEncender }: { onEncender: () => void }) {
  return (
    <div className="tv-encender">
      <div className="tv-encender__caja">
        <h2 className="placa">{TV.encender.titulo}</h2>
        <p>{TV.encender.detalle}</p>
        <Tecla
          variante="cereza"
          tamano="grande"
          autoFocus
          onClick={() => {
            onEncender();
            // Algunos navegadores de TV no devuelven una promesa: se encierra todo.
            try {
              void Promise.resolve(document.documentElement.requestFullscreen?.()).catch(() => undefined);
            } catch {
              // Sin pantalla completa, la TV funciona igual.
            }
          }}
        >
          {TV.encender.boton}
        </Tecla>
      </div>
    </div>
  );
}

/**
 * El lienzo de 1280 × 720 del prototipo, ampliado con `zoom` hasta llenar la pantalla sin
 * deformarse (doc 05, «Responsive»). El valor se pone desde una ref: la CSP no deja `style=""`.
 */
function LienzoTv({ apagada = false, children }: { apagada?: boolean; children: ReactNode }) {
  const lienzo = useRef<HTMLDivElement>(null);
  useLayoutEffect(() => {
    const ajustar = () => {
      if (lienzo.current) lienzo.current.style.zoom = String(Math.min(window.innerWidth / 1280, window.innerHeight / 720));
    };
    ajustar();
    window.addEventListener('resize', ajustar);
    return () => window.removeEventListener('resize', ajustar);
  }, []);
  return (
    <div className="tv-marco">
      <div ref={lienzo} className={apagada ? 'tv-lienzo madera madera--apagada' : 'tv-lienzo madera'}>
        {children}
      </div>
    </div>
  );
}