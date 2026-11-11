'use client';

import { CodigoPantalla, type PantallaVinculada } from '@rockola/contrato';
import { Campo, Tecla } from '@rockola/ui';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { DialogoConfirmar } from '@/components/panel/Dialogos';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { useAccionGestion } from '@/lib/api/hooks-panel';
import { fechaYHora } from '@/lib/formato';
import { pantallas as T } from '@/lib/textos/panel';
import { Cargando, mensajeDeError } from './PaginaGestion';

type Mensaje = { tipo: 'luz' | 'error'; texto: string };

const esCodigo = (codigo: string) => CodigoPantalla.safeParse(codigo).success;
const sinSuscripcion = () => () => {};

/**
 * Las pantallas del bar (RF-058, ADR-23): las TV vinculadas, desvincular, y vincular la que
 * muestra un código. Solo el Administrador llega aquí (la página es de gestión).
 */
export function Pantallas() {
  const lista = useQuery({ queryKey: claves.panel.pantallas, queryFn: ({ signal }) => pedir('pantallas', { signal }) });
  // El aviso vive aquí para que sobreviva a la recarga de la lista.
  const [aviso, setAviso] = useState<Mensaje | null>(null);
  return (
    <div className="gestion-rejilla gestion-rejilla--dos">
      <section className="caja self-start" aria-labelledby="t-pantallas">
        <div className="caja__cabeza">
          <h2 id="t-pantallas" tabIndex={-1}>
            {T.titulo}
          </h2>
          <p>{T.nota}</p>
        </div>
        {aviso ? <Aviso tipo={aviso.tipo} titulo={aviso.texto} className="mb-3" /> : null}
        <Cargando consulta={lista}>{() => <ListaPantallas pantallas={lista.data!.pantallas} setAviso={setAviso} />}</Cargando>
      </section>
      <FormularioVincular setAviso={setAviso} />
    </div>
  );
}

function ListaPantallas({ pantallas, setAviso }: { pantallas: PantallaVinculada[]; setAviso: (a: Mensaje | null) => void }) {
  const yo = useYoDelPanel();
  const zona = yo.establecimiento.zona_horaria;
  const [quitar, setQuitar] = useState<PantallaVinculada | null>(null);
  const desvincular = useAccionGestion('desvincularPantalla', [claves.panel.pantallas, claves.panel.bitacora]);
  // La fila y su botón desaparecen: al cerrar el diálogo, el navegador devolvería el foco a un
  // botón que ya no existe. Este efecto corre después del cierre (los del hijo van primero).
  const enfocarTitulo = useRef(false);
  useEffect(() => {
    if (quitar === null && enfocarTitulo.current) {
      enfocarTitulo.current = false;
      document.getElementById('t-pantallas')?.focus();
    }
  }, [quitar]);

  if (pantallas.length === 0) {
    return (
      <div className="vacio">
        <p>
          <b>{T.vacio.titulo}.</b> {T.vacio.detalle}
        </p>
      </div>
    );
  }
  return (
    <>
      <ul className="lista-gestion">
        {pantallas.map((p) => (
          <li key={p.id}>
            <div className="lista-gestion__info">
              <p className="lista-gestion__titulo">{p.nombre}</p>
              <p className="lista-gestion__meta">{T.vinculada(fechaYHora(p.vinculada_en, zona), p.vinculada_por.nombre)}</p>
              <p className="lista-gestion__meta">{p.ultimo_reporte ? T.ultimoReporte(fechaYHora(p.ultimo_reporte, zona)) : T.sinReporte}</p>
            </div>
            <div className="lista-gestion__acciones">
              <Tecla tamano="chica" onClick={() => setQuitar(p)} aria-label={`${T.desvincular} «${p.nombre}»`}>
                {T.desvincular}
              </Tecla>
            </div>
          </li>
        ))}
      </ul>
      <DialogoConfirmar
        abierto={quitar !== null}
        titulo={quitar ? T.dialogo.titulo(quitar.nombre) : ''}
        texto={T.dialogo.texto}
        confirmar={T.dialogo.confirmar}
        ocupado={desvincular.isPending}
        onCerrar={() => setQuitar(null)}
        onConfirmar={() => {
          if (!quitar) return;
          const nombre = quitar.nombre;
          desvincular.mutate(
            { parametros: { id: quitar.id } },
            {
              onSuccess: () => {
                setAviso({ tipo: 'luz', texto: T.desvinculada(nombre) });
                enfocarTitulo.current = true;
              },
              onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e) }),
              onSettled: () => setQuitar(null),
            },
          );
        }}
      />
    </>
  );
}

function FormularioVincular({ setAviso }: { setAviso: (a: Mensaje | null) => void }) {
  const f = T.formulario;
  const slug = useYoDelPanel().establecimiento.slug;
  // El origen solo existe en el navegador; en el servidor se muestra la ruta sin él.
  const origen = useSyncExternalStore(sinSuscripcion, () => window.location.origin, () => '');
  const ruta = `${origen}/b/${slug}/pantalla`;
  const [codigo, setCodigo] = useState('');
  const [nombre, setNombre] = useState('');
  const [enviado, setEnviado] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const vincular = useAccionGestion('vincularPantalla', [claves.panel.pantallas, claves.panel.bitacora]);
  const errores = {
    codigo: enviado && !esCodigo(codigo) ? f.errorCodigo : null,
    nombre: enviado && !nombre.trim() ? f.errorNombre : null,
  };
  return (
    <form
      className="caja flex flex-col gap-4 self-start"
      aria-labelledby="t-vincular"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        setEnviado(true);
        setError(null);
        if (!esCodigo(codigo) || !nombre.trim()) return;
        const elegido = nombre.trim();
        vincular.mutate(
          { cuerpo: { codigo, nombre: elegido } },
          {
            onSuccess: () => {
              setAviso({ tipo: 'luz', texto: f.listo(elegido) });
              setCodigo('');
              setNombre('');
              setEnviado(false);
            },
            onError: (err) => setError(mensajeDeError(err, { no_encontrado: f.noExiste })),
          },
        );
      }}
    >
      <div className="caja__cabeza mb-0!">
        <h2 id="t-vincular">{f.titulo}</h2>
      </div>
      <p className="secundario text-[14px]">
        {f.abre} <b className="[overflow-wrap:anywhere]">{ruta}</b> {f.abreDespues}
      </p>
      <Campo
        etiqueta={f.codigo}
        ayuda={f.codigoAyuda}
        error={errores.codigo}
        claseEntrada="campo__entrada--codigo"
        value={codigo}
        // Se escribe como se ve en la TV: mayúsculas, sin espacios ni guiones.
        onChange={(e) => {
          setError(null);
          setCodigo(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6));
        }}
        autoComplete="off"
        autoCapitalize="characters"
        // Sin maxLength: pegar «TEW-APT» lo cortaría antes de quitar el guion.
        spellCheck={false}
      />
      <Campo etiqueta={f.nombre} ayuda={f.nombreAyuda} error={errores.nombre} value={nombre}
        onChange={(e) => {
          setError(null);
          setNombre(e.target.value);
        }}
        maxLength={40}
      />
      {error ? <Aviso tipo="error" titulo={error} /> : null}
      <Tecla type="submit" variante="cereza" tamano="ancha" className="self-start" ocupada={vincular.isPending}>
        {f.boton}
      </Tecla>
    </form>
  );
}
