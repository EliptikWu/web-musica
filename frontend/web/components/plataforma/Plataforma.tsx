'use client';

import { type BarPlataforma, type CuentaBar, CuerpoCuentaBar, type Rol, type YoPlataforma } from '@rockola/contrato';
import { Campo, Dialogo, GrupoTeclas, Icono, LuzEstado, Tecla } from '@rockola/ui';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type FormEvent, type ReactNode, useEffect, useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { DialogoConfirmar } from '@/components/panel/Dialogos';
import { Cargando, mensajeDeError } from '@/components/gestion/PaginaGestion';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { codigoDe } from '@/lib/api/errores';
import { MODO } from '@/lib/modo';
import { ROL } from '@/lib/textos/panel';
import { aSlug, plataforma as T } from '@/lib/textos/plataforma';

const ROLES = [
  { valor: 'operador', texto: 'Operador' },
  { valor: 'administrador', texto: 'Administrador' },
] as const;
/** La misma regla del contrato: lo que aquí pasa, el servidor lo acepta. */
const correoValido = (correo: string) => CuerpoCuentaBar.shape.correo.safeParse(correo.trim()).success;
const SLUG = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const ZONA_POR_DEFECTO = 'America/Bogota';

/**
 * La plataforma (RF-064, ADR-29): quien administra Rockola crea bares con su primer Administrador
 * y agrega cuentas. Quien decide es el servidor; esta pantalla solo refleja `GET /plataforma/yo`.
 */
export function Plataforma() {
  const yo = useQuery({ queryKey: claves.plataforma.yo, queryFn: ({ signal }) => pedir('yoPlataforma', { signal }), retry: false });
  const codigo = codigoDe(yo.error);

  if (yo.isPending) {
    return (
      <main className="madera min-h-dvh px-4 py-6" aria-busy="true">
        <p className="solo-lector" role="status">{T.cargando}</p>
        <div className="esqueleto mx-auto h-12 max-w-[1100px]" />
      </main>
    );
  }
  // La sesión terminó, aunque quede una respuesta anterior en memoria: de vuelta a la puerta.
  if (codigo === 'no_autenticado') return <Entrar />;
  // Un fallo pasajero con datos ya cargados no borra la pantalla ni una clave temporal abierta.
  if (yo.isError && !yo.data) {
    return (
      <main className="madera min-h-dvh px-4 py-10">
        <div className="mx-auto max-w-[30rem]">
          <Aviso tipo="error" titulo={T.noCargo} meta={T.errorGenerico} accion={<button type="button" onClick={() => void yo.refetch()}>Reintentar</button>} />
        </div>
      </main>
    );
  }
  if (!yo.data) return <Entrar />;
  if (yo.data.debe_cambiar_clave) return <CambiarClave />;
  return <Bares yo={yo.data} />;
}

function Puerta({ titulo, children, onEnviar }: { titulo: string; children: ReactNode; onEnviar: (e: FormEvent) => void }) {
  return (
    <main className="madera flex min-h-dvh flex-col items-center px-4 pt-14 pb-10">
      <div className="w-full max-w-[26rem]">
        <header className="text-center">
          <p className="placa text-[22px]">Rockola</p>
          <LuzEstado encendida className="mt-2">{T.marca}</LuzEstado>
        </header>
        <form className="caja mt-6 flex flex-col gap-4 px-5 pt-5 pb-6" onSubmit={onEnviar} noValidate>
          <h1 className="titulo-seccion text-[20px]">{titulo}</h1>
          {children}
        </form>
        {MODO === 'simulado' ? <Aviso tipo="info" className="mt-4" meta={T.simulacion} /> : null}
      </div>
    </main>
  );
}

function Entrar() {
  const qc = useQueryClient();
  const [correo, setCorreo] = useState('');
  const [clave, setClave] = useState('');
  const [error, setError] = useState<string | null>(null);
  const entrar = useMutation({ mutationFn: () => pedir('entrarPlataforma', { cuerpo: { correo: correo.trim(), clave } }) });

  function enviar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!correoValido(correo) || !clave) return setError(T.credenciales);
    entrar.mutate(undefined, {
      onSuccess: (yo) => qc.setQueryData(claves.plataforma.yo, yo),
      onError: (err) => setError(mensajeDeError(err, { credenciales_invalidas: T.credenciales, datos_invalidos: T.credenciales })),
    });
  }
  return (
    <Puerta titulo={T.entrar} onEnviar={enviar}>
      {error ? <Aviso tipo="error" titulo={error} /> : null}
      <Campo etiqueta="Correo" type="email" autoComplete="username" inputMode="email" required value={correo} onChange={(e) => setCorreo(e.target.value)} />
      <Campo etiqueta="Clave" type="password" autoComplete="current-password" required value={clave} onChange={(e) => setClave(e.target.value)} />
      <Tecla type="submit" variante="cereza" tamano="grande" ocupada={entrar.isPending}>Entrar</Tecla>
    </Puerta>
  );
}

function CambiarClave() {
  const qc = useQueryClient();
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [repetir, setRepetir] = useState('');
  const [error, setError] = useState<string | null>(null);
  const cambiar = useMutation({ mutationFn: () => pedir('cambiarClavePlataforma', { cuerpo: { actual, nueva } }) });

  function enviar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (nueva.length < 12) return setError(T.claveCorta);
    if (nueva !== repetir) return setError(T.noCoinciden);
    cambiar.mutate(undefined, {
      onSuccess: () => void qc.invalidateQueries({ queryKey: claves.plataforma.yo }),
      onError: (err) => setError(mensajeDeError(err, { credenciales_invalidas: T.actualMal, datos_invalidos: T.claveCorta })),
    });
  }
  return (
    <Puerta titulo={T.cambiarClave} onEnviar={enviar}>
      <p className="secundario text-[15px]">{T.cambiarClaveNota}</p>
      {error ? <Aviso tipo="error" titulo={error} /> : null}
      <Campo etiqueta="Clave actual" type="password" autoComplete="current-password" value={actual} onChange={(e) => setActual(e.target.value)} />
      <Campo etiqueta="Clave nueva" type="password" autoComplete="new-password" value={nueva} onChange={(e) => setNueva(e.target.value)} />
      <Campo etiqueta="Repite la clave nueva" type="password" autoComplete="new-password" value={repetir} onChange={(e) => setRepetir(e.target.value)} />
      <Tecla type="submit" variante="cereza" tamano="grande" ocupada={cambiar.isPending}>Guardar clave</Tecla>
    </Puerta>
  );
}

function Bares({ yo }: { yo: YoPlataforma }) {
  const qc = useQueryClient();
  const bares = useQuery({ queryKey: claves.plataforma.bares, queryFn: ({ signal }) => pedir('baresPlataforma', { signal }) });
  const [creandoBar, setCreandoBar] = useState(false);
  const [barCuenta, setBarCuenta] = useState<BarPlataforma | null>(null);
  const [clave, setClave] = useState<{ quien: string; donde: string; clave: string } | null>(null);
  const [aRestablecer, setARestablecer] = useState<{ cuenta: CuentaBar; bar: string } | null>(null);
  const [errorClave, setErrorClave] = useState<string | null>(null);
  const restablecer = useMutation({
    mutationFn: (id: string) => pedir('claveCuentaPlataforma', { parametros: { id } }),
    onSettled: () => qc.invalidateQueries({ queryKey: claves.plataforma.bares }),
  });
  const codigoBares = codigoDe(bares.error);
  useEffect(() => {
    if (codigoBares === 'no_autenticado') void qc.invalidateQueries({ queryKey: claves.plataforma.yo });
  }, [codigoBares, qc]);
  const salir = useMutation({
    mutationFn: () => pedir('salirPlataforma'),
    onSuccess: () => {
      qc.removeQueries({ queryKey: claves.plataforma.bares });
      qc.setQueryData(claves.plataforma.yo, null);
      void qc.invalidateQueries({ queryKey: claves.plataforma.yo });
    },
  });

  return (
    <div className="madera min-h-dvh">
      <div className="mx-auto max-w-[1100px] px-4 pt-5 pb-8 sm:px-8">
        <header className="panel-cabeza flex-wrap">
          <div className="panel-marca">
            <p className="placa panel-marca__bar">Rockola</p>
            <p className="secundario hidden text-[15px] sm:block">{T.marca}</p>
          </div>
          <nav className="panel-nav flex-wrap" aria-label="Plataforma">
            <span className="panel-nav__usuario inline-flex items-center gap-2">
              <span className="text-[15px]">{yo.nombre}</span>
              <button type="button" aria-busy={salir.isPending || undefined} onClick={() => salir.mutate()}>
                <Icono nombre="salir" />
                Salir
              </button>
            </span>
          </nav>
        </header>
        <main>
          <div className="gestion-cabeza">
            <div>
              <h1 className="placa">{T.titulo}</h1>
              <p>{T.nota}</p>
            </div>
            <Tecla variante="cereza" tamano="ancha" onClick={() => setCreandoBar(true)}>{T.crearBar}</Tecla>
          </div>
          {salir.isError ? <Aviso tipo="error" className="mt-4" titulo={T.noSalio} /> : null}
          {errorClave ? <Aviso tipo="error" className="mt-4" titulo={errorClave} /> : null}
          <Cargando consulta={bares}>
            {() =>
              bares.data!.bares.length === 0 ? (
                <section className="caja mt-4"><p className="secundario">{T.vacio}</p></section>
              ) : (
                <>
                  {bares.data!.bares.map((b) => (
                    <section key={b.id} className="caja mt-4" aria-label={b.nombre}>
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <div className="min-w-0">
                          <h2 className="titulo-seccion text-[20px]">
                            {b.nombre} {b.activo ? null : <span className="etiqueta etiqueta--cereza">Inactivo</span>}
                          </h2>
                          <p className="secundario text-[14px]"><span className="break-all">/b/{b.slug}</span> <span className="whitespace-nowrap">({b.zona_horaria})</span></p>
                        </div>
                        {b.activo ? <Tecla tamano="chica" aria-label={`Agregar una cuenta a ${b.nombre}`} onClick={() => setBarCuenta(b)}>{T.agregarCuenta}</Tecla> : null}
                      </div>
                      {b.cuentas.length === 0 ? (
                        <p className="secundario mt-3 text-[14px]">{T.sinCuentas}</p>
                      ) : (
                        <ul className="lista-gestion mt-2">
                          {b.cuentas.map((c) => (
                            <li key={c.id}>
                              <div className="lista-gestion__info">
                                <p className="lista-gestion__titulo">
                                  {c.nombre}
                                  <span className="etiqueta">{ROL[c.rol]}</span>
                                  {c.estado === 'inactivo' ? <span className="etiqueta etiqueta--cereza">Inactiva</span> : null}
                                  {c.debe_cambiar_clave ? <span className="etiqueta">Clave temporal</span> : null}
                                </p>
                                <p className="lista-gestion__meta break-all">{c.correo}</p>
                              </div>
                              <div className="lista-gestion__acciones">
                                <Tecla tamano="chica" aria-label={`Dar una clave nueva a ${c.nombre}, de ${b.nombre}`} onClick={() => setARestablecer({ cuenta: c, bar: b.nombre })}>{T.nuevaClave}</Tecla>
                              </div>
                            </li>
                          ))}
                        </ul>
                      )}
                    </section>
                  ))}
                </>
              )
            }
          </Cargando>
        </main>
      </div>

      {creandoBar ? (
        <DialogoBar
          onCerrar={() => setCreandoBar(false)}
          onCreado={(r) => {
            setCreandoBar(false);
            setClave({ quien: r.bar.cuentas[0]?.nombre ?? '', donde: r.bar.nombre, clave: r.clave_temporal });
          }}
        />
      ) : null}
      {barCuenta ? (
        <DialogoCuenta
          bar={barCuenta}
          onCerrar={() => setBarCuenta(null)}
          onCreada={(nombre, nuevaClaveTemporal) => {
            setClave({ quien: nombre, donde: barCuenta.nombre, clave: nuevaClaveTemporal });
            setBarCuenta(null);
          }}
        />
      ) : null}
      <DialogoConfirmar
        abierto={aRestablecer !== null}
        titulo={`¿Dar una clave nueva a ${aRestablecer?.cuenta.nombre ?? ''}?`}
        texto={T.nuevaClaveNota}
        confirmar={T.nuevaClave}
        ocupado={restablecer.isPending}
        onCerrar={() => { if (!restablecer.isPending) setARestablecer(null); }}
        onConfirmar={() => {
          if (!aRestablecer) return;
          const { cuenta, bar } = aRestablecer;
          setErrorClave(null);
          restablecer.mutate(cuenta.id, {
            onSuccess: (r) => setClave({ quien: cuenta.nombre, donde: bar, clave: r.clave_temporal }),
            onError: (err) => {
              if (codigoDe(err) === 'no_autenticado') void qc.invalidateQueries({ queryKey: claves.plataforma.yo });
              setErrorClave(mensajeDeError(err, { no_encontrado: T.cuentaNoExiste }));
            },
            onSettled: () => setARestablecer(null),
          });
        }}
      />
      <Dialogo abierto={clave !== null} titulo={`Clave temporal de ${clave?.quien ?? ''}`} onCerrar={() => setClave(null)}>
        <p className="dialogo__texto">{T.claveNota(clave?.donde ?? '')}</p>
        <p className="ventana px-4 py-3 text-[22px] font-bold tracking-wide select-all">{clave?.clave}</p>
        <div className="dialogo__acciones">
          <Tecla variante="cereza" onClick={() => setClave(null)}>Listo</Tecla>
        </div>
      </Dialogo>
    </div>
  );
}

function DialogoBar({ onCerrar, onCreado }: { onCerrar: () => void; onCreado: (r: { bar: BarPlataforma; clave_temporal: string }) => void }) {
  const qc = useQueryClient();
  const [nombre, setNombre] = useState('');
  const [slug, setSlug] = useState('');
  const [slugTocado, setSlugTocado] = useState(false);
  const [zona, setZona] = useState(ZONA_POR_DEFECTO);
  const [nombreAdmin, setNombreAdmin] = useState('');
  const [correo, setCorreo] = useState('');
  const [intento, setIntento] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const crear = useMutation({
    mutationFn: () =>
      pedir('crearBarPlataforma', {
        cuerpo: { nombre: nombre.trim(), slug, zona_horaria: zona.trim(), administrador: { nombre: nombreAdmin.trim(), correo: correo.trim() } },
      }),
  });
  // Con la petición en curso no se cierra: la clave temporal se perdería.
  const cerrar = () => { if (!crear.isPending) onCerrar(); };
  const valido = nombre.trim() && SLUG.test(slug) && zona.trim() && nombreAdmin.trim() && correoValido(correo);

  return (
    <Dialogo abierto titulo={T.crearBar} onCerrar={cerrar}>
      <form
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          setIntento(true);
          setError(null);
          if (!valido) return;
          crear.mutate(undefined, {
            onSuccess: async (r) => {
              await qc.invalidateQueries({ queryKey: claves.plataforma.bares });
              onCreado(r);
            },
            onError: (err) => {
              if (codigoDe(err) === 'no_autenticado') void qc.invalidateQueries({ queryKey: claves.plataforma.yo });
              setError(mensajeDeError(err, { conflicto: T.slugOcupado, datos_invalidos: T.datosMal }));
            },
          });
        }}
      >
        {error ? <Aviso tipo="error" titulo={error} /> : null}
        <Campo
          etiqueta="Nombre del bar"
          value={nombre}
          autoFocus
          onChange={(e) => {
            setNombre(e.target.value);
            if (!slugTocado) setSlug(aSlug(e.target.value));
          }}
          error={intento && !nombre.trim() ? 'Escribe el nombre del bar.' : null}
        />
        <Campo
          etiqueta="Dirección web del bar"
          value={slug}
          onChange={(e) => {
            setSlugTocado(true);
            setSlug(e.target.value.toLowerCase());
          }}
          error={intento && !SLUG.test(slug) ? T.slugMal : null}
        />
        <p className="secundario -mt-2 text-[14px] break-all">{T.slugAyuda(slug)}</p>
        <Campo etiqueta="Zona horaria" value={zona} onChange={(e) => setZona(e.target.value)} error={intento && !zona.trim() ? 'Escribe la zona horaria.' : null} />
        <h3 className="titulo-seccion text-[17px]">{T.primerAdmin}</h3>
        <Campo etiqueta="Nombre" value={nombreAdmin} onChange={(e) => setNombreAdmin(e.target.value)} error={intento && !nombreAdmin.trim() ? 'Escribe el nombre.' : null} />
        <Campo etiqueta="Correo" type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} error={intento && !correoValido(correo) ? 'Escribe un correo válido.' : null} />
        <div className="dialogo__acciones">
          <Tecla type="button" disabled={crear.isPending} onClick={cerrar}>Cancelar</Tecla>
          <Tecla type="submit" variante="cereza" ocupada={crear.isPending}>{T.crearBar}</Tecla>
        </div>
      </form>
    </Dialogo>
  );
}

function DialogoCuenta({ bar, onCerrar, onCreada }: { bar: BarPlataforma; onCerrar: () => void; onCreada: (nombre: string, clave: string) => void }) {
  const qc = useQueryClient();
  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [rol, setRol] = useState<Rol>('operador');
  const [intento, setIntento] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const crear = useMutation({
    mutationFn: () => pedir('crearCuentaPlataforma', { parametros: { id: bar.id }, cuerpo: { nombre: nombre.trim(), correo: correo.trim(), rol } }),
  });
  const cerrar = () => { if (!crear.isPending) onCerrar(); };
  return (
    <Dialogo abierto titulo={`Agregar una cuenta a ${bar.nombre}`} onCerrar={cerrar}>
      <form
        className="flex flex-col gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          setIntento(true);
          setError(null);
          if (!nombre.trim() || !correoValido(correo)) return;
          crear.mutate(undefined, {
            onSuccess: async (r) => {
              await qc.invalidateQueries({ queryKey: claves.plataforma.bares });
              onCreada(r.cuenta.nombre, r.clave_temporal);
            },
            onError: (err) => {
              if (codigoDe(err) === 'no_autenticado') void qc.invalidateQueries({ queryKey: claves.plataforma.yo });
              setError(mensajeDeError(err, { conflicto: T.correoOcupado, datos_invalidos: T.datosCuenta }));
            },
          });
        }}
      >
        {error ? <Aviso tipo="error" titulo={error} /> : null}
        <Campo etiqueta="Nombre" value={nombre} autoFocus onChange={(e) => setNombre(e.target.value)} error={intento && !nombre.trim() ? 'Escribe el nombre.' : null} />
        <Campo etiqueta="Correo" type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} error={intento && !correoValido(correo) ? 'Escribe un correo válido.' : null} />
        <GrupoTeclas leyenda="Rol" nombre="rol-cuenta" opciones={[...ROLES]} valor={rol} onCambio={setRol} />
        <div className="dialogo__acciones">
          <Tecla type="button" disabled={crear.isPending} onClick={cerrar}>Cancelar</Tecla>
          <Tecla type="submit" variante="cereza" ocupada={crear.isPending}>Crear cuenta</Tecla>
        </div>
      </form>
    </Dialogo>
  );
}
