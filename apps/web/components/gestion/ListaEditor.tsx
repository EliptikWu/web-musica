'use client';

import type { ListaCasaDetalle } from '@rockola/contrato';
import { Campo, claseTecla, Icono, TarjetaTitulo, Tecla } from '@rockola/ui';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { codigoDe } from '@/lib/api/errores';
import { useAccionGestion } from '@/lib/api/hooks-panel';
import { contarCanciones, formatoDuracion, formatoTiempoTotal } from '@/lib/formato';
import { Cargando, mensajeDeError, PaginaGestion } from './PaginaGestion';

/**
 * Armar una lista (RF-029, RF-031, RF-032; PB-36): renombrarla, agregar canciones buscando en
 * YouTube, quitarlas y ordenarlas con las flechas (también con teclado). La lista activa no se
 * puede quedar vacía.
 */
export function ListaEditor({ id }: { id: string }) {
  const lista = useQuery({ queryKey: claves.panel.lista(id), queryFn: ({ signal }) => pedir('lista', { parametros: { id }, signal }), retry: false });
  const titulo = lista.data ? `Lista «${lista.data.nombre}»` : 'Lista de la casa';
  return (
    <PaginaGestion
      titulo={titulo}
      nota={lista.data ? `${contarCanciones(lista.data.total_canciones)}, ${formatoTiempoTotal(lista.data.duracion_total_seg)}${lista.data.activa ? '. Es la lista activa.' : '.'}` : undefined}
      accion={
        <Link href="/panel/listas" className={claseTecla('normal', 'ancha')}>
          <Icono nombre="atras" />
          Todas las listas
        </Link>
      }
    >
      {codigoDe(lista.error) === 'no_encontrado' ? <Aviso tipo="info" className="mt-4" titulo="Esa lista no existe o se eliminó." /> : <Cargando consulta={lista}>{() => <Editor lista={lista.data!} />}</Cargando>}
    </PaginaGestion>
  );
}

function Editor({ lista }: { lista: ListaCasaDetalle }) {
  const invalidar = [claves.panel.lista(lista.id), claves.panel.listas];
  const renombrar = useAccionGestion('renombrarLista', invalidar);
  const quitar = useAccionGestion('quitarDeLista', invalidar);
  const ordenar = useAccionGestion('ordenarLista', invalidar);
  const agregar = useAccionGestion('agregarALista', invalidar);
  const [nombre, setNombre] = useState(lista.nombre);
  const [aviso, setAviso] = useState<{ tipo: 'luz' | 'error'; texto: string } | null>(null);
  const [texto, setTexto] = useState('');
  const [consulta, setConsulta] = useState('');
  useEffect(() => {
    const t = setTimeout(() => setConsulta(texto.trim()), 300);
    return () => clearTimeout(t);
  }, [texto]);
  const busqueda = useQuery({
    queryKey: claves.panel.busqueda(consulta.toLowerCase()),
    queryFn: ({ signal }) => pedir('buscarPanel', { consulta: { q: consulta }, signal }),
    enabled: consulta.length >= 2,
    placeholderData: keepPreviousData,
    retry: false,
  });
  const enLista = new Set(lista.canciones.map((c) => c.cancion.video_id));

  const [anuncio, setAnuncio] = useState('');

  function mover(indice: number, delta: -1 | 1) {
    const ids = lista.canciones.map((c) => c.elemento_id);
    const destino = indice + delta;
    if (destino < 0 || destino >= ids.length) return;
    [ids[indice], ids[destino]] = [ids[destino]!, ids[indice]!];
    const movida = lista.canciones[indice]!;
    ordenar.mutate(
      { parametros: { id: lista.id }, cuerpo: { elementos: ids } },
      {
        onSuccess: () => {
          // El foco sigue a la canción con la misma flecha (o la otra, si llegó a un extremo).
          const flechas = delta < 0 ? ['[data-flecha="arriba"]', '[data-flecha="abajo"]'] : ['[data-flecha="abajo"]', '[data-flecha="arriba"]'];
          enfocarFila(movida.elemento_id, destino, flechas);
          setAnuncio(`«${movida.cancion.titulo}» quedó en el puesto ${destino + 1}.`);
        },
        onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e) }),
      },
    );
  }

  function quitarCancion(indice: number) {
    const quitada = lista.canciones[indice]!;
    // El foco pasa a la canción que queda en su lugar (o a la anterior, si era la última).
    const vecina = lista.canciones[indice + 1] ?? lista.canciones[indice - 1];
    quitar.mutate(
      { parametros: { id: lista.id, elemento: quitada.elemento_id } },
      {
        onSuccess: () => {
          if (vecina) enfocarFila(vecina.elemento_id, lista.canciones[indice + 1] ? indice : indice - 1, ['[data-quitar]']);
          else document.querySelector<HTMLElement>('[data-buscar-agregar]')?.focus();
          setAnuncio(`«${quitada.cancion.titulo}» salió de la lista.`);
        },
        onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e, { conflicto: 'La lista activa no puede quedar vacía.' }) }),
      },
    );
  }

  return (
    <div className="gestion-rejilla gestion-rejilla--dos">
      <section className="caja" aria-labelledby="t-canciones">
        <div className="caja__cabeza">
          <h2 id="t-canciones">Canciones, en el orden en que suenan</h2>
        </div>
        {aviso ? <Aviso tipo={aviso.tipo} className="mb-3" titulo={aviso.texto} /> : null}
        <p className="solo-lector" aria-live="polite">
          {anuncio}
        </p>
        {lista.canciones.length === 0 ? (
          <p className="secundario">La lista está vacía. Busca canciones al lado para agregarlas.</p>
        ) : (
          <ol className="filas" data-orden>
            {lista.canciones.map((c, i) => (
              <li key={c.elemento_id} className="fila" data-elemento={c.elemento_id}>
                <TarjetaTitulo puesto={i + 1} titulo={c.cancion.titulo} artista={c.cancion.artista} duracion={formatoDuracion(c.cancion.duracion_seg)} />
                <div className="orden-teclas">
                  <Tecla tamano="chica" data-flecha="arriba" aria-label={`Subir ${c.cancion.titulo}`} disabled={i === 0} ocupada={ordenar.isPending} onClick={() => mover(i, -1)}>
                    <Icono nombre="arriba" />
                  </Tecla>
                  <Tecla tamano="chica" data-flecha="abajo" aria-label={`Bajar ${c.cancion.titulo}`} disabled={i === lista.canciones.length - 1} ocupada={ordenar.isPending} onClick={() => mover(i, 1)}>
                    <Icono nombre="abajo" />
                  </Tecla>
                  <Tecla tamano="chica" data-quitar aria-label={`Quitar ${c.cancion.titulo}`} ocupada={quitar.isPending} onClick={() => quitarCancion(i)}>
                    <Icono nombre="retirar" />
                  </Tecla>
                </div>
              </li>
            ))}
          </ol>
        )}
      </section>

      <div className="flex flex-col gap-4">
        <form
          className="caja flex flex-col gap-3"
          onSubmit={(e) => {
            e.preventDefault();
            if (!nombre.trim() || nombre.trim() === lista.nombre) return;
            renombrar.mutate(
              { parametros: { id: lista.id }, cuerpo: { nombre: nombre.trim() } },
              {
                onSuccess: () => setAviso({ tipo: 'luz', texto: 'Nombre guardado.' }),
                onError: (err) => setAviso({ tipo: 'error', texto: mensajeDeError(err, { conflicto: 'Ya hay una lista con ese nombre.' }) }),
              },
            );
          }}
        >
          <Campo etiqueta="Nombre de la lista" value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={60} />
          <Tecla type="submit" tamano="ancha" ocupada={renombrar.isPending}>
            Guardar nombre
          </Tecla>
        </form>

        <section className="caja flex flex-col gap-3" aria-labelledby="t-agregar">
          <h2 id="t-agregar" className="titulo-seccion">
            Agregar canciones
          </h2>
          <div className="buscador" role="search">
            <Icono nombre="buscar" />
            <input type="search" data-buscar-agregar value={texto} onChange={(e) => setTexto(e.target.value)} aria-label="Buscar en YouTube para agregar" placeholder="Canción o artista" autoComplete="off" />
          </div>
          {consulta.length < 2 ? (
            <p className="secundario text-[14px]">Escribe al menos dos letras.</p>
          ) : busqueda.isError ? (
            <Aviso tipo="error" titulo="YouTube no respondió" meta="La búsqueda falló. Inténtalo de nuevo en un momento." />
          ) : !busqueda.data ? (
            <div className="esqueleto h-14" aria-busy="true" />
          ) : busqueda.data.resultados.length === 0 ? (
            <p className="secundario text-[14px]">No encontramos «{consulta}».</p>
          ) : (
            <ul className="filas">
              {busqueda.data.resultados.map((c) => (
                <li key={c.video_id} className="fila">
                  <TarjetaTitulo titulo={c.titulo} artista={c.artista} duracion={formatoDuracion(c.duracion_seg)} variante={enLista.has(c.video_id) ? 'apagada' : 'normal'} />
                  {enLista.has(c.video_id) ? (
                    <p className="motivo">Ya está en la lista</p>
                  ) : (
                    <Tecla
                      variante="cereza"
                      tamano="chica"
                      aria-label={`Agregar ${c.titulo}`}
                      ocupada={agregar.isPending}
                      onClick={() =>
                        agregar.mutate(
                          { parametros: { id: lista.id }, cuerpo: { video_id: c.video_id } },
                          { onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e, { bloqueada: 'Esa canción está bloqueada.', no_disponible: 'Ese video no está disponible.', conflicto: 'Ya está en la lista.' }) }) },
                        )
                      }
                    >
                      Agregar
                    </Tecla>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}

/**
 * Enfoca una tecla de la fila cuando la fila ya está en su puesto nuevo (React la pinta un poco
 * después de la recarga). Prueba los selectores en orden y salta las teclas desactivadas.
 */
function enfocarFila(elemento: string, puesto: number, selectores: string[], cuadros = 30) {
  const intentar = (quedan: number) => {
    const fila = document.querySelectorAll<HTMLElement>('[data-orden] > [data-elemento]')[puesto];
    if (fila?.dataset.elemento === elemento) {
      for (const selector of selectores) {
        const tecla = fila.querySelector<HTMLButtonElement>(selector);
        if (tecla && !tecla.disabled) return tecla.focus();
      }
    } else if (quedan > 0) requestAnimationFrame(() => intentar(quedan - 1));
  };
  requestAnimationFrame(() => intentar(cuadros));
}
