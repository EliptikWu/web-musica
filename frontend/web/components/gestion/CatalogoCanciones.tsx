'use client';

import type { CancionCatalogo } from '@rockola/contrato';
import { GrupoTeclas, Selector, TarjetaTitulo, Tecla } from '@rockola/ui';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { useAccionGestion } from '@/lib/api/hooks-panel';
import { ordenarGeneros, resumenReclasificacion, textoOrigen, type TotalReclasificacion } from '@/lib/generos';
import { Cargando, mensajeDeError, PaginaGestion } from './PaginaGestion';

const FILTROS = [
  { valor: 'sin', texto: 'Sin género' },
  { valor: 'todas', texto: 'Todas' },
] as const;

/** Lo que el backend devuelve como máximo, sin paginar: con más, toca buscar. */
const LIMITE = 100;


/**
 * Catálogo de canciones (RF-068): el género de lo que el bar usa. La clasificación automática
 * a veces no encuentra género o se equivoca; aquí se corrige, y el backend la marca como
 * manual para que la automática no la vuelva a cambiar.
 */
export function CatalogoCanciones() {
  const [filtro, setFiltro] = useState<'sin' | 'todas'>('sin');
  const [texto, setTexto] = useState('');
  const [q, setQ] = useState('');
  useEffect(() => {
    const t = setTimeout(() => setQ(texto.trim()), 300);
    return () => clearTimeout(t);
  }, [texto]);
  const consulta = { soloSinCategoria: filtro === 'sin' ? 'true' : 'false', q, limite: String(LIMITE) } as const;
  const canciones = useQuery({
    queryKey: claves.panel.canciones(consulta),
    queryFn: ({ signal }) => pedir('cancionesPanel', { consulta, signal }),
    placeholderData: keepPreviousData,
  });
  // En orden alfabético (punto 14): el servidor los manda por familias y eran 61 sin orden visible.
  const generos = useQuery({ queryKey: claves.panel.generos, queryFn: ({ signal }) => pedir('generos', { signal }), staleTime: Infinity, select: ordenarGeneros });
  const [aviso, setAviso] = useState<{ tipo: 'luz' | 'error'; titulo: string; meta?: string } | null>(null);
  // Tras guardar, la fila se va («Sin género») o se vuelve a montar («Todas»): el foco se pone
  // cuando la lista ya muestra el cambio, en su selector o en el de la siguiente.
  const foco = useRef<{ id: string; categoria: string; siguiente: string | null } | null>(null);
  const [guardadas, setGuardadas] = useState(0);
  useEffect(() => {
    const f = foco.current;
    if (!f || !canciones.data) return;
    const fila = canciones.data.find((x) => x.id === f.id);
    if (fila && fila.categoria !== f.categoria) return;
    const selector = (id: string | null) => (id ? document.querySelector<HTMLElement>(`[data-cancion="${CSS.escape(id)}"] select`) : null);
    (selector(fila ? f.id : f.siguiente) ?? document.getElementById('catalogo-buscar'))?.focus();
    foco.current = null;
  }, [guardadas, canciones.data]);

  return (
    <PaginaGestion titulo="Canciones" nota="El género de cada canción que se pidió, sonó o está en una lista del bar. Si quedó sin género o con uno equivocado, corrígelo aquí.">
      <div className="catalogo-filtros mt-4">
        <GrupoTeclas leyenda="Mostrar" nombre="catalogo-filtro" opciones={[...FILTROS]} valor={filtro} onCambio={setFiltro} />
        <div className="campo">
          <label className="campo__etiqueta" htmlFor="catalogo-buscar">
            Buscar por título o artista
          </label>
          <input id="catalogo-buscar" type="search" className="campo__entrada" value={texto} onChange={(e) => setTexto(e.target.value)} autoComplete="off" />
        </div>
      </div>
      <Reclasificar onAviso={setAviso} />
      {generos.isError ? (
        <Aviso
          tipo="error"
          className="mt-4"
          titulo="No cargó la lista de géneros, así que no se puede cambiar ninguno."
          accion={
            <button type="button" onClick={() => void generos.refetch()}>
              Reintentar
            </button>
          }
        />
      ) : null}
      {aviso ? <Aviso tipo={aviso.tipo} className="mt-4" titulo={aviso.titulo} meta={aviso.meta} /> : null}
      <section className="caja mt-4" aria-label={filtro === 'sin' ? 'Canciones sin género' : 'Todas las canciones'} aria-busy={canciones.isFetching || undefined}>
        <Cargando consulta={canciones}>
          {() => {
            const lista = canciones.data!;
            if (lista.length === 0) {
              return (
                <p className="secundario">
                  {q ? `Ninguna canción del bar coincide con «${q}».` : filtro === 'sin' ? 'No quedan canciones sin género.' : 'El bar todavía no tiene canciones. Aparecen cuando alguien pide una o la agregas a una lista de la casa.'}
                </p>
              );
            }
            return (
              <>
                <ul className="lista-gestion">
                  {lista.map((c, i) => (
                    // La clave lleva el género: si cambia (aquí u otra pantalla), la fila vuelve a empezar con él.
                    <FilaCancion key={`${c.id}|${c.categoria ?? ''}`} cancion={c} generos={generos.data ?? []} onAviso={setAviso} alGuardar={(categoria) => {
                        foco.current = { id: c.id, categoria, siguiente: (lista[i + 1] ?? lista[i - 1])?.id ?? null };
                        setGuardadas((n) => n + 1);
                      }} />
                  ))}
                </ul>
                {lista.length === LIMITE ? <p className="secundario mt-3 text-[14px]">Se muestran las primeras {LIMITE}. Busca por título o artista para encontrar otra.</p> : null}
              </>
            );
          }}
        </Cargando>
      </section>
    </PaginaGestion>
  );
}

/** Pasa por el clasificador del backend, de 10 en 10, las que no tienen género (hasta 100 por vez). */
async function reclasificarTodas(): Promise<TotalReclasificacion> {
  const total: TotalReclasificacion = { clasificadas: 0, sinPistas: 0, noDisponibles: 0, interrumpida: null };
  let desde = 0;
  for (let vuelta = 0; vuelta < 10; vuelta++) {
    const r = await pedir('reclasificarCanciones', { consulta: { limite: '10', despuesDe: String(desde) } });
    total.clasificadas += r.clasificadas;
    total.sinPistas += r.sin_coincidencia;
    total.noDisponibles += r.no_disponibles;
    total.interrumpida = r.interrumpida;
    desde = r.siguiente_desde_id;
    if (r.interrumpida || !r.hay_mas) break;
  }
  return total;
}

/** Punto 18: la ruta ya existía en el backend y nadie la podía usar desde el panel. */
function Reclasificar({ onAviso }: { onAviso: (a: { tipo: 'luz' | 'error'; titulo: string; meta?: string }) => void }) {
  const qc = useQueryClient();
  const accion = useMutation({
    mutationFn: reclasificarTodas,
    onSuccess: (t) => onAviso(resumenReclasificacion(t)),
    onError: (e) => onAviso({ tipo: 'error', titulo: 'No se pudo clasificar.', meta: mensajeDeError(e, {}) }),
    onSettled: () => qc.invalidateQueries({ queryKey: ['panel', 'canciones'] }),
  });
  return (
    <div className="catalogo-reclasificar mt-3">
      <Tecla tamano="ancha" ocupada={accion.isPending} onClick={() => accion.mutate()}>
        Clasificar las que faltan
      </Tecla>
      <p className="secundario text-[14px]">Vuelve a leer en YouTube las etiquetas, el título y la descripción de las que no tienen género. Lo que pusiste a mano no se toca.</p>
    </div>
  );
}

function FilaCancion({
  cancion: c, generos, onAviso, alGuardar,
}: {
  cancion: CancionCatalogo;
  generos: string[];
  alGuardar: (categoria: string) => void;
  onAviso: (a: { tipo: 'luz' | 'error'; titulo: string; meta?: string }) => void;
}) {
  const corregir = useAccionGestion('corregirCategoria', [['panel', 'canciones']]);
  const [elegido, setElegido] = useState(c.categoria ?? '');
  const cambio = elegido !== '' && elegido !== c.categoria;
  // El catálogo puede no traer un género viejo: se muestra igual para no perderlo.
  const opciones = c.categoria && !generos.includes(c.categoria) ? [c.categoria, ...generos] : generos;

  const guardar = () => {
    corregir.mutate(
      { parametros: { id: c.id }, cuerpo: { categoria: elegido } },
      {
        // `useAccionGestion` ya esperó la recarga: la lista está al día cuando llega aquí.
        onSuccess: (r) => {
          alGuardar(r.categoria);
          onAviso({ tipo: 'luz', titulo: `«${c.titulo}» quedó en ${r.categoria}.`, meta: 'El cubo en vivo ya lo cuenta así; el histórico, cuando vuelva a correr el pipeline.' });
        },
        onError: (e) => onAviso({ tipo: 'error', titulo: `No se guardó el género de «${c.titulo}».`, meta: mensajeDeError(e, { datos_invalidos: 'Ese género no está en la lista. Elige uno de la lista.', no_encontrado: 'La canción ya no está en el bar. Vuelve a cargar la página.' }) }),
      },
    );
  };

  return (
    <li data-cancion={c.id}>
      <div className="w-full max-w-[18rem]">
        <TarjetaTitulo titulo={c.titulo} artista={c.artista ?? ''} variante="apagada" />
      </div>
      <div className="lista-gestion__info">
        <p className="lista-gestion__titulo">{c.categoria ?? 'Sin género'}</p>
        <p className="lista-gestion__meta">
          {textoOrigen(c.categoria, c.origen_categoria)} {c.pedidos === 1 ? 'Pedida 1 vez' : `Pedida ${c.pedidos} veces`} en el bar.
        </p>
      </div>
      <form
        className="catalogo-accion"
        onSubmit={(e) => {
          e.preventDefault();
          if (cambio) guardar();
        }}
      >
        <Selector etiqueta="Cambiar a" aria-label={`Género de «${c.titulo}»`} value={elegido} onChange={(e) => setElegido(e.target.value)} disabled={generos.length === 0}>
          {elegido === '' ? <option value="">Elige un género</option> : null}
          {opciones.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </Selector>
        <Tecla type="submit" tamano="chica" variante="cereza" disabled={!cambio} ocupada={corregir.isPending}>
          Guardar género
        </Tecla>
      </form>
    </li>
  );
}
