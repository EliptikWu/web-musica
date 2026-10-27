'use client';

import type { Cancion, Noche } from '@rockola/contrato';
import { Avance, DiasSemana, Icono, TarjetaTitulo, Tecla } from '@rockola/ui';
import Link from 'next/link';
import { type ReactNode, useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { codigoDe } from '@/lib/api/errores';
import { nuevaClave } from '@/lib/api/hooks-cliente';
import { useAccionPanel, useNoche } from '@/lib/api/hooks-panel';
import { DIAS, formatoDuracion, formatoHace, formatoHora, formatoMinutos, formatoResumenCola, minutosEnteros, resumenHorario } from '@/lib/formato';
import { MODO } from '@/lib/modo';
import { puede } from '@/lib/permisos';
import { duracionCola } from '@/lib/reglas/cola';
import { posicionActual } from '@/lib/reglas/espera';
import { diasQueAbren, momentoLocal } from '@/lib/reglas/horario';
import { useAhoraServidor } from '@/lib/reloj-servidor';
import { acciones, dialogos, errores, franjaEstado, lineasRegistro, P } from '@/lib/textos/panel';
import { DialogoBloquear, DialogoConfirmar, DialogoRetirar } from './Dialogos';
import { useYoDelPanel } from './MarcoPanel';

type Dialogo = { tipo: 'bloquear'; cancion: Cancion } | { tipo: 'retirar'; id: string; titulo: string } | { tipo: 'mantenimiento' } | { tipo: 'cerrarNoche' } | null;

/**
 * La operación de la noche (prototipo panel-admin y #pausa; PB-11, 12, 25, 27, 31–34, 38, 40):
 * el estado del servicio, lo que suena con sus controles, la cola con Retirar y Bloquear, el
 * registro, las métricas, el horario, la lista de la casa y los bloqueos. Cada rol ve solo lo
 * que puede hacer (D9); el servidor lo confirma.
 */
export function Operacion() {
  const yo = useYoDelPanel();
  const rol = yo.usuario.rol;
  const noche = useNoche();
  const instante = useAhoraServidor();
  const [dialogo, setDialogo] = useState<Dialogo>(null);
  const [error, setError] = useState<string | null>(null);

  const servicio = useAccionPanel('cambiarServicio');
  const mantenimiento = useAccionPanel('mantenimiento');
  const abrir = useAccionPanel('abrirSesion');
  const cerrar = useAccionPanel('cerrarSesion');
  const saltar = useAccionPanel('saltar');
  const pausar = useAccionPanel('pausarReproduccion');
  const reanudar = useAccionPanel('reanudarReproduccion');
  const retirar = useAccionPanel('retirar');
  const bloquear = useAccionPanel('bloquear');
  const desbloquear = useAccionPanel('desbloquear');

  /** Lo que pasa al terminar cualquier acción: si falló, se dice por qué. */
  const alTerminar = { onError: (e: unknown) => setError(codigoDe(e) === 'sin_permiso' ? errores.sinPermiso : codigoDe(e) === 'no_pendiente' ? errores.noPendiente : errores.generico), onSuccess: () => setError(null) };

  if (!noche.data) {
    return noche.isError ? (
      <Aviso tipo="error" className="mt-6" titulo="La operación no cargó" meta={errores.generico} accion={<button type="button" onClick={() => void noche.refetch()}>Reintentar</button>} />
    ) : (
      <div className="mt-4 grid gap-4" aria-busy="true">
        <p className="solo-lector" role="status">
          Cargando la noche…
        </p>
        <div className="esqueleto h-24 rounded-marco" />
        <div className="esqueleto h-64 rounded-caja" />
      </div>
    );
  }

  const n = noche.data;
  const zona = yo.establecimiento.zona_horaria;
  const momento = instante || Date.parse(n.servidor_ahora);

  return (
    <>
      <BarraEstado
        noche={n}
        zona={zona}
        acciones={
          <>
            {n.servicio.estado === 'abierto' && puede(rol, 'pausarSolicitudes') ? (
              <Tecla variante="cereza" ocupada={servicio.isPending} onClick={() => servicio.mutate({ cuerpo: { modo_qr: 'pausado' } }, alTerminar)}>
                {acciones.pausarSolicitudes}
              </Tecla>
            ) : null}
            {n.servicio.estado === 'en_pausa' && puede(rol, 'pausarSolicitudes') ? (
              <Tecla variante="cereza" ocupada={servicio.isPending} onClick={() => servicio.mutate({ cuerpo: { modo_qr: 'automatico' } }, alTerminar)}>
                {acciones.reanudarSolicitudes}
              </Tecla>
            ) : null}
            {n.servicio.estado === 'mantenimiento' && puede(rol, 'mantenimiento') ? (
              <Tecla variante="cereza" ocupada={servicio.isPending} onClick={() => servicio.mutate({ cuerpo: { modo_qr: 'automatico' } }, alTerminar)}>
                {acciones.reanudarSolicitudes}
              </Tecla>
            ) : null}
            {n.servicio.estado === 'fuera_de_horario' && puede(rol, 'abrirCerrarNoche') ? (
              <Tecla variante="cereza" ocupada={abrir.isPending} onClick={() => abrir.mutate({ cuerpo: { tipo_evento: 'normal' } }, alTerminar)}>
                {acciones.abrirNoche}
              </Tecla>
            ) : null}
            {n.servicio.estado !== 'mantenimiento' && puede(rol, 'mantenimiento') ? <Tecla onClick={() => setDialogo({ tipo: 'mantenimiento' })}>{acciones.mantenimiento}</Tecla> : null}
            {n.sesion && n.servicio.estado !== 'mantenimiento' && puede(rol, 'abrirCerrarNoche') ? (
              <Tecla onClick={() => setDialogo({ tipo: 'cerrarNoche' })}>{acciones.cerrarNoche}</Tecla>
            ) : null}
          </>
        }
      />

      {error ? <Aviso tipo="error" className="mt-3" titulo={error} accion={<button type="button" onClick={() => setError(null)}>Cerrar</button>} /> : null}
      {n.alertas.some((a) => a.tipo === 'sin_lista_activa') ? (
        <p className="alerta-panel" role="status">
          <Icono nombre="alerta" />
          {P.sinListaActiva}
        </p>
      ) : null}
      {n.alertas
        .filter((a) => a.tipo === 'errores_youtube')
        .map((a) => (
          <p key={a.tipo} className="alerta-panel" role="status">
            <Icono nombre="alerta" />
            YouTube no pudo reproducir algunas canciones ({a.detalle}). Mira el registro.
          </p>
        ))}

      <div className="rejilla-panel">
        <div className="rejilla-panel__col">
          <Caja titulo={P.suenaAhora} oculto>
            {n.reproduccion ? (
              <div className="actual">
                <div className="tarjeta">
                  <div className="tarjeta__centro">
                    <p className="actual__etiqueta">
                      {n.reproduccion.estado === 'pausada' ? 'En pausa' : P.suenaAhora}
                      {n.reproduccion.origen === 'lista_casa' ? ', de la lista de la casa' : ''}
                    </p>
                    <h2 className="actual__titulo condensada">{n.reproduccion.cancion.titulo}</h2>
                    <p className="tarjeta__artista">
                      {n.reproduccion.cancion.artista}
                      {n.reproduccion.pedida_por ? `, pedida por el teléfono ${n.reproduccion.pedida_por}` : ''}
                      {n.reproduccion.pedida_en ? ` ${formatoHace(n.reproduccion.pedida_en, momento)}` : ''}
                    </p>
                    <AvanceActual noche={n} momento={momento} />
                  </div>
                  <p className="tarjeta__votos">
                    {n.reproduccion.votos ?? '—'}
                    <small>{n.reproduccion.votos === null ? 'lista' : n.reproduccion.votos === 1 ? 'voto' : 'votos'}</small>
                  </p>
                </div>
                <div className="actual__acciones">
                  {puede(rol, 'saltar') ? (
                    <Tecla ocupada={saltar.isPending} onClick={() => saltar.mutate({ parametros: { id: n.reproduccion!.id }, idempotencia: `${n.reproduccion!.id}-salto` }, alTerminar)}>
                      {P.saltar}
                    </Tecla>
                  ) : null}
                  {puede(rol, 'pausarMusica') ? (
                    n.reproduccion.estado === 'sonando' ? (
                      <Tecla ocupada={pausar.isPending} onClick={() => pausar.mutate({ parametros: { id: n.reproduccion!.id } }, alTerminar)}>
                        {P.pausarMusica}
                      </Tecla>
                    ) : (
                      <Tecla variante="cereza" ocupada={reanudar.isPending} onClick={() => reanudar.mutate({ parametros: { id: n.reproduccion!.id } }, alTerminar)}>
                        {P.reanudarMusica}
                      </Tecla>
                    )
                  ) : null}
                  {puede(rol, 'bloquearNoche') ? <Tecla onClick={() => setDialogo({ tipo: 'bloquear', cancion: n.reproduccion!.cancion })}>{P.bloquear}</Tecla> : null}
                </div>
              </div>
            ) : (
              <p className="secundario">{P.nadaSuena}</p>
            )}
          </Caja>

          <Caja titulo={P.cola} nota={n.cola.length > 0 ? `${formatoResumenCola(n.cola.length, duracionCola(n.cola))}. Ordenada por votos; en empate, por hora de pedido.` : undefined}>
            {n.cola.length === 0 ? (
              <p className="secundario">{P.colaVacia}</p>
            ) : (
              <>
                <table className="tabla-cola">
                  <thead>
                    <tr>
                      <th scope="col" className="text-center!">
                        #
                      </th>
                      <th scope="col">Canción</th>
                      <th scope="col" className="num">
                        Duración
                      </th>
                      <th scope="col" className="num">
                        Pedida hace
                      </th>
                      <th scope="col" className="num">
                        Votos
                      </th>
                      <th scope="col">
                        <span className="solo-lector">Acciones</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {n.cola.map((item) => (
                      <tr key={item.solicitud_id}>
                        <td className="pos">{item.puesto}</td>
                        <td className="cancion">
                          <b>{item.cancion.titulo}</b>
                          <span>{item.cancion.artista}</span>
                        </td>
                        <td className="num">{formatoDuracion(item.duracion_seg)}</td>
                        <td className="num">{formatoMinutos((momento - Date.parse(item.fecha_solicitud)) / 1000)}</td>
                        <td className="num votos">{item.votos_total}</td>
                        <td className="accion">
                          <span className="acciones-fila">
                            {puede(rol, 'retirar') ? (
                              <Tecla tamano="chica" aria-label={`Retirar ${item.cancion.titulo}`} onClick={() => setDialogo({ tipo: 'retirar', id: item.solicitud_id, titulo: item.cancion.titulo })}>
                                {P.retirar}
                              </Tecla>
                            ) : null}
                            {puede(rol, 'bloquearNoche') ? (
                              <Tecla tamano="chica" aria-label={`Bloquear ${item.cancion.titulo}`} onClick={() => setDialogo({ tipo: 'bloquear', cancion: item.cancion })}>
                                {P.bloquear}
                              </Tecla>
                            ) : null}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <ul className="cola-tarjetas" aria-label="Cola">
                  {n.cola.map((item) => (
                    <li key={item.solicitud_id}>
                      <TarjetaTitulo puesto={item.puesto} titulo={item.cancion.titulo} artista={item.cancion.artista} votos={item.votos_total} />
                      <p className="cola-tarjetas__meta">
                        {formatoDuracion(item.duracion_seg)}, pedida hace {minutosEnteros((momento - Date.parse(item.fecha_solicitud)) / 1000)} min por el teléfono {item.pedida_por}
                      </p>
                      <div className="cola-tarjetas__acciones">
                        {puede(rol, 'retirar') ? (
                          <Tecla tamano="chica" aria-label={`Retirar ${item.cancion.titulo}`} onClick={() => setDialogo({ tipo: 'retirar', id: item.solicitud_id, titulo: item.cancion.titulo })}>
                            {P.retirar}
                          </Tecla>
                        ) : null}
                        {puede(rol, 'bloquearNoche') ? (
                          <Tecla tamano="chica" aria-label={`Bloquear ${item.cancion.titulo}`} onClick={() => setDialogo({ tipo: 'bloquear', cancion: item.cancion })}>
                            {P.bloquear}
                          </Tecla>
                        ) : null}
                      </div>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </Caja>

          <Caja titulo={P.registro} nota={P.estaNoche}>
            {n.registro.length === 0 ? (
              <p className="secundario">{P.sinRegistro}</p>
            ) : (
              <ol className="registro">
                {lineasRegistro(n.registro, yo.usuario.id)
                  .slice(0, 8)
                  .map((l) => (
                    <li key={l.id}>
                      <time dateTime={l.fecha}>{formatoHora(l.fecha, zona)}</time>
                      <span>{l.texto}</span>
                    </li>
                  ))}
              </ol>
            )}
          </Caja>
        </div>

        <div className="rejilla-panel__col">
          <Caja titulo={P.metricas} nota={n.metricas.desde ? `Desde las ${formatoHora(n.metricas.desde, zona)}` : undefined}>
            <dl className="metricas">
              <Metrica nombre="Solicitudes" valor={n.metricas.solicitudes} />
              <Metrica nombre="Teléfonos" valor={n.metricas.telefonos} />
              <Metrica nombre="Votos" valor={n.metricas.votos} />
              <Metrica nombre="Espera promedio" valor={n.metricas.espera_promedio_seg === null ? '—' : minutosEnteros(n.metricas.espera_promedio_seg)} unidad={n.metricas.espera_promedio_seg === null ? undefined : 'min'} />
            </dl>
            {MODO === 'simulado' ? <p className="secundario mt-2 text-[13px]">{P.datosDeEjemplo}: las cifras de la simulación no son de un bar real.</p> : null}
            {puede(yo.usuario.rol, 'analitica') ? (
              <p className="mt-2 text-[14px]">
                <Link href="/panel/analitica" className="enlace">
                  Ver el tablero analítico
                </Link>
              </p>
            ) : null}
          </Caja>

          <Caja titulo={P.horario}>
            <DiasSemana
              dias={DIAS.map((d, i) => ({ ...d, abre: diasQueAbren(n.horario).has(i + 1), hoy: momentoLocal(new Date(momento), zona).dia === i + 1 }))}
            />
            <div className="horas">
              {resumenHorario(n.horario).map((l) => (
                <b key={l.dias}>
                  {l.dias}, {l.horas}
                </b>
              ))}
              {resumenHorario(n.horario).length === 0 ? <span className="secundario">Sin horario activo.</span> : null}
            </div>
          </Caja>

          <Caja titulo={P.listaCasa} nota={P.suenaSiVacia}>
            {n.lista_casa ? (
              <div className="casa">
                <TarjetaTitulo titulo={n.lista_casa.nombre} artista={`${n.lista_casa.total_canciones} canciones, ${n.lista_casa.modo_rotacion === 'secuencial' ? 'en orden' : 'al azar'}`} />
              </div>
            ) : (
              <p className="secundario">{P.sinListaActiva}</p>
            )}
          </Caja>

          <Caja titulo={P.bloqueadas} nota={String(n.bloqueos.length)}>
            {n.bloqueos.length === 0 ? (
              <p className="secundario">{P.sinBloqueos}</p>
            ) : (
              <ul className="bloqueadas">
                {n.bloqueos.map((b) => (
                  <li key={b.id}>
                    <TarjetaTitulo titulo={b.cancion.titulo} artista={b.cancion.artista} variante="apagada" />
                    {puede(rol, 'desbloquear') ? (
                      <Tecla tamano="chica" ocupada={desbloquear.isPending} aria-label={`Desbloquear ${b.cancion.titulo}`} onClick={() => desbloquear.mutate({ parametros: { id: b.id } }, alTerminar)}>
                        {P.desbloquear}
                      </Tecla>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </Caja>

        </div>
      </div>

      <DialogoBloquear
        key={dialogo?.tipo === 'bloquear' ? `b-${dialogo.cancion.video_id}` : 'b'}
        cancion={dialogo?.tipo === 'bloquear' ? dialogo.cancion : null}
        soloNoche={!puede(rol, 'bloquearMas')}
        ocupado={bloquear.isPending}
        onCerrar={() => setDialogo(null)}
        onConfirmar={(datos) =>
          dialogo?.tipo === 'bloquear' &&
          bloquear.mutate({ cuerpo: { video_id: dialogo.cancion.video_id, ...datos }, idempotencia: nuevaClave() }, { ...alTerminar, onSettled: () => setDialogo(null) })
        }
      />
      <DialogoRetirar
        key={dialogo?.tipo === 'retirar' ? `r-${dialogo.id}` : 'r'}
        titulo={dialogo?.tipo === 'retirar' ? dialogo.titulo : null}
        ocupado={retirar.isPending}
        onCerrar={() => setDialogo(null)}
        onConfirmar={(datos) =>
          dialogo?.tipo === 'retirar' && retirar.mutate({ parametros: { id: dialogo.id }, cuerpo: datos, idempotencia: nuevaClave() }, { ...alTerminar, onSettled: () => setDialogo(null) })
        }
      />
      <DialogoConfirmar
        abierto={dialogo?.tipo === 'mantenimiento'}
        {...dialogos.mantenimiento(n.cola.length)}
        ocupado={mantenimiento.isPending}
        onCerrar={() => setDialogo(null)}
        onConfirmar={() => mantenimiento.mutate({}, { ...alTerminar, onSettled: () => setDialogo(null) })}
      />
      <DialogoConfirmar
        abierto={dialogo?.tipo === 'cerrarNoche'}
        {...dialogos.cerrarNoche(n.cola.length)}
        ocupado={cerrar.isPending}
        onCerrar={() => setDialogo(null)}
        onConfirmar={() => cerrar.mutate({}, { ...alTerminar, onSettled: () => setDialogo(null) })}
      />
    </>
  );
}

function BarraEstado({ noche, zona, acciones: botones }: { noche: Noche; zona: string; acciones: ReactNode }) {
  const s = noche.servicio;
  const textos = franjaEstado(s.estado, s.hasta, s.desde ?? noche.sesion?.apertura ?? null, s.proxima_apertura, s.modo_qr === 'forzado_activo', zona);
  const apagada = s.estado !== 'abierto';
  return (
    <section className={apagada ? 'estado-bar estado-bar--apagada' : 'estado-bar'} aria-label="Estado del servicio">
      <div className="estado-bar__luz flex-wrap">
        <div className="ventana">
          <span className="foco" aria-hidden="true" />
          <div>
            <h1 className="estado-bar__titulo">{textos.titulo}</h1>
            <p className="estado-bar__sub">{textos.sub}</p>
          </div>
        </div>
        <div className="estado-bar__acciones">{botones}</div>
      </div>
    </section>
  );
}

function AvanceActual({ noche, momento }: { noche: Noche; momento: number }) {
  const rep = noche.reproduccion!;
  const posicion = posicionActual(rep, noche.servidor_ahora, momento);
  return <Avance valor={posicion} maximo={rep.cancion.duracion_seg} textoInicio={formatoDuracion(posicion)} textoFin={formatoDuracion(rep.cancion.duracion_seg)} />;
}

function Caja({ titulo, nota, oculto = false, children }: { titulo: string; nota?: string; oculto?: boolean; children: ReactNode }) {
  const id = `caja-${titulo.toLowerCase().replace(/[^a-z]+/g, '-')}`;
  return (
    <section className="caja" aria-labelledby={id}>
      <div className={oculto ? 'solo-lector' : 'caja__cabeza'}>
        <h2 id={id}>{titulo}</h2>
        {nota ? <p>{nota}</p> : null}
      </div>
      {children}
    </section>
  );
}

function Metrica({ nombre, valor, unidad }: { nombre: string; valor: number | string; unidad?: string }) {
  return (
    <div className="metrica">
      <dt>{nombre}</dt>
      <dd>
        {valor}
        {unidad ? <small> {unidad}</small> : null}
      </dd>
    </div>
  );
}
