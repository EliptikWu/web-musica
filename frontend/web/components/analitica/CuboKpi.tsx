'use client';

import { TODOS_CUBO, type CuboAnalitica } from '@rockola/contrato';
import { GrupoTeclas, Tecla } from '@rockola/ui';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { type KeyboardEvent, type PointerEvent, useMemo, useRef, useState } from 'react';
import { Cargando } from '@/components/gestion/PaginaGestion';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { OrigenDeLosDatos } from './Comun';
import {
  alternar, celdasDe, type ClaveKpi, claveCelda, type Dim, DIMS, dentro, drillDown, ejesDeTabla, indiceCeldas, KPIS, type Kpi, leerKpi, luzDe, operacion, ordenEjes,
  rollUp, type Seleccion, SIN_SELECCION,
} from './cubo-kpi';

type Eje = CuboAnalitica['eje'];
const VISTAS = { Isométrica: [-24, -36], Frente: [0, 0], Arriba: [-82, 0], Lado: [-8, -80] } as const;

/** Pone variables CSS desde una ref: la CSP no admite `style=""` en el HTML. */
const conVariables = (variables: Record<string, string>) => (el: HTMLElement | null) => {
  if (el) for (const [nombre, valor] of Object.entries(variables)) el.style.setProperty(nombre, valor);
};

/** Cubo de KPI (RF-063): el mecanismo de la rockola, con la luz en la celda que más pesa. */
export function CuboKpi() {
  const [eje, setEje] = useState<Eje>('dia');
  const consulta = useQuery({
    queryKey: claves.panel.analitica('cuboAnalitica', { eje }),
    queryFn: ({ signal }) => pedir('cuboAnalitica', { consulta: { eje }, signal }),
    placeholderData: keepPreviousData,
    retry: false,
  });
  return (
    <>
      {/* Fuera de `Cargando`: si un eje falla, se puede volver al otro. */}
      <div className="cubo-mando">
        <GrupoTeclas
          leyenda="Tercer eje"
          nombre="cubo-eje"
          valor={eje}
          onCambio={setEje}
          opciones={[
            { valor: 'dia', texto: 'Día' },
            { valor: 'evento', texto: 'Evento' },
          ]}
        />
      </div>
      <Cargando consulta={consulta}>
        {() => {
          const d = consulta.data!;
          return (
            <div className="analitica-cuerpo mt-4" aria-busy={consulta.isFetching || undefined}>
              <OrigenDeLosDatos datos={d.datos} />
              {d.celdas.length === 0 ? (
                <p className="secundario mt-6">Todavía no hay noches cargadas. El cubo se llena cuando corre el pipeline.</p>
              ) : (
                <Laboratorio key={d.eje} datos={d} />
              )}
            </div>
          );
        }}
      </Cargando>
    </>
  );
}

function Laboratorio({ datos }: { datos: CuboAnalitica }) {
  const [claveKpi, setClaveKpi] = useState<ClaveKpi>('solicitudes');
  const [sel, setSelBase] = useState<Seleccion>(SIN_SELECCION);
  const [orden, setOrden] = useState<Dim[]>([]);
  const setSel = (nueva: Seleccion) => {
    setOrden((o) => ordenEjes(o, nueva));
    setSelBase(nueva);
  };
  const [pivot, setPivot] = useState(false);
  const [separar, setSeparar] = useState(false);
  const kpi = KPIS.find((k) => k.clave === claveKpi)!;
  const indice = useMemo(() => indiceCeldas(datos.celdas), [datos.celdas]);
  const nombres: Record<Dim, string> = { genero: 'Género', franja: 'Franja', tercero: datos.eje === 'dia' ? 'Día' : 'Evento' };
  const op = operacion(sel);
  const ficha = celdasDe(indice, sel);
  const unica = DIMS.every((d) => sel[d].length <= 1);
  const popular = unica ? (ficha[0]?.popular ?? null) : null;

  return (
    <>
      <section className="caja mt-4" aria-labelledby="t-kpis">
        <div className="caja__cabeza">
          <h2 id="t-kpis">KPI de lo elegido</h2>
        </div>
        <p className="secundario text-[14px]">Toca un KPI para iluminar el cubo con él.</p>
        <div className="cubo-kpis" role="group" aria-labelledby="t-kpis">
          {KPIS.map((k) => {
            const v = leerKpi(indice, k, sel);
            const esPopular = k.clave === 'popular';
            return (
              <button key={k.clave} type="button" className="cubo-kpi" aria-pressed={claveKpi === k.clave} onClick={() => setClaveKpi(k.clave)}>
                <span>{esPopular ? 'Canción más popular' : k.nombre}</span>
                {esPopular && popular ? (
                  <b className="cubo-kpi__texto">
                    {popular.cancion.titulo}
                    <small>
                      {popular.cancion.artista}, {popular.votos} votos
                    </small>
                  </b>
                ) : (
                  <b>{v === null ? 'No aplica' : k.formato(v)}</b>
                )}
              </button>
            );
          })}
        </div>
        {!unica ? <p className="secundario mt-3 text-[14px]">Con varios valores por eje, usuarios únicos, espera promedio y canción más popular no se pueden sumar sin contar de más.</p> : null}
      </section>

      <div className="cubo-lab">
        <section className="caja cubo-escena-caja" aria-labelledby="t-cubo">
          <h2 id="t-cubo" className="sr-only">
            Cubo de {kpi.nombre.toLowerCase()}
          </h2>
          <Escena datos={datos} indice={indice} kpi={kpi} sel={sel} separar={separar} nombres={nombres} onCelda={setSel} />
        </section>

        <section className="caja cubo-controles" aria-labelledby="t-recorte">
          <div className="caja__cabeza">
            <h2 id="t-recorte">{op.nombre}</h2>
          </div>
          <p className="secundario text-[15px]">{op.detalle}</p>
          {DIMS.map((d) => (
            <fieldset key={d} className="cubo-eje">
              <legend>{nombres[d]}</legend>
              <div>
                {datos.miembros[d].map((m) => (
                  <button key={m} type="button" className="cubo-miembro" aria-pressed={sel[d].includes(m)} onClick={() => setSel(alternar(sel, d, m))}>
                    {m}
                  </button>
                ))}
              </div>
            </fieldset>
          ))}
          <div className="cubo-operaciones">
            <Tecla onClick={() => setSel(rollUp(sel, orden))} disabled={DIMS.every((d) => sel[d].length === 0)}>
              Roll-up
            </Tecla>
            <Tecla onClick={() => setSel(drillDown(sel, datos.miembros))} disabled={DIMS.every((d) => sel[d].length > 0)}>
              Drill-down
            </Tecla>
            <Tecla onClick={() => setPivot((p) => !p)} aria-pressed={pivot}>
              Pivot
            </Tecla>
            <Tecla onClick={() => setSel(SIN_SELECCION)} disabled={DIMS.every((d) => sel[d].length === 0)}>
              Limpiar
            </Tecla>
          </div>
          <label className="cubo-separar">
            <input type="checkbox" checked={separar} onChange={(e) => setSeparar(e.target.checked)} /> Separar la capa elegida
          </label>
        </section>
      </div>

      <TablaCruce datos={datos} indice={indice} kpi={kpi} sel={sel} pivot={pivot} nombres={nombres} />
    </>
  );
}

function Escena({
  datos, indice, kpi, sel, separar, nombres, onCelda,
}: {
  datos: CuboAnalitica;
  indice: ReturnType<typeof indiceCeldas>;
  kpi: Kpi;
  sel: Seleccion;
  separar: boolean;
  nombres: Record<Dim, string>;
  onCelda: (s: Seleccion) => void;
}) {
  const cuboRef = useRef<HTMLDivElement>(null);
  const giro = useRef({ x: -24, y: -36 });
  const arrastre = useRef<{ x: number; y: number; gx: number; gy: number; movio: boolean } | null>(null);
  const [lectura, setLectura] = useState<string | null>(null);
  const aplicar = () => cuboRef.current?.style.setProperty('--giro', `rotateX(${giro.current.x}deg) rotateY(${giro.current.y}deg)`);
  const fijarRef = (el: HTMLDivElement | null) => {
    cuboRef.current = el;
    if (el) aplicar();
  };

  const ejes = { g: [...datos.miembros.genero, TODOS_CUBO], f: [...datos.miembros.franja, TODOS_CUBO], t: [...datos.miembros.tercero, TODOS_CUBO] };
  const maximo = Math.max(1, ...datos.celdas.filter((c) => c.genero !== TODOS_CUBO && c.franja !== TODOS_CUBO && c.tercero !== TODOS_CUBO).map((c) => kpi.valor(c) ?? 0));
  const hay = DIMS.some((d) => sel[d].length > 0);
  const desplazamiento = (lista: string[], elegidos: string[], m: string) => {
    if (!separar || elegidos.length !== 1) return 0;
    const i = lista.indexOf(m);
    const e = lista.indexOf(elegidos[0]!);
    return i === e ? 0 : Math.sign(i - e) * 0.9;
  };

  const nombreDe = (m: string, d: Dim) => (m === TODOS_CUBO ? `Todo ${nombres[d].toLowerCase()}` : m);

  const alSoltar = (e: PointerEvent<HTMLDivElement>) => {
    const a = arrastre.current;
    arrastre.current = null;
    if (!a || a.movio) return;
    const el = (document.elementFromPoint(e.clientX, e.clientY) as HTMLElement | null)?.closest<HTMLElement>('[data-g]');
    if (!el) return;
    const fijo = (m: string) => (m === TODOS_CUBO ? [] : [m]);
    onCelda({ genero: fijo(el.dataset.g!), franja: fijo(el.dataset.f!), tercero: fijo(el.dataset.t!) });
  };

  const teclado = (e: KeyboardEvent<HTMLDivElement>) => {
    const paso = { ArrowLeft: [0, -12], ArrowRight: [0, 12], ArrowUp: [12, 0], ArrowDown: [-12, 0] }[e.key];
    if (!paso) return;
    e.preventDefault();
    giro.current = { x: Math.max(-89, Math.min(89, giro.current.x + paso[0]!)), y: giro.current.y + paso[1]! };
    aplicar();
  };

  return (
    <>
      <div
        className="cubo-escena"
        role="img"
        tabIndex={0}
        aria-label={`Cubo de ${kpi.nombre.toLowerCase()} por ${nombres.genero.toLowerCase()}, ${nombres.franja.toLowerCase()} y ${nombres.tercero.toLowerCase()}. Gíralo con las flechas. Los mismos datos están en la tabla de abajo.`}
        onKeyDown={teclado}
        onPointerDown={(e) => {
          arrastre.current = { x: e.clientX, y: e.clientY, gx: giro.current.x, gy: giro.current.y, movio: false };
          e.currentTarget.setPointerCapture(e.pointerId);
        }}
        onPointerMove={(e) => {
          const a = arrastre.current;
          if (!a) {
            const el = (e.target as HTMLElement).closest<HTMLElement>('[data-g]');
            setLectura(el ? el.dataset.lectura! : null);
            return;
          }
          const dx = e.clientX - a.x;
          const dy = e.clientY - a.y;
          if (!a.movio && Math.hypot(dx, dy) > 5) a.movio = true;
          if (a.movio) {
            giro.current = { x: Math.max(-89, Math.min(89, a.gx - dy * 0.45)), y: a.gy + dx * 0.45 };
            aplicar();
          }
        }}
        onPointerUp={alSoltar}
        onPointerCancel={() => (arrastre.current = null)}
        onPointerLeave={() => setLectura(null)}
      >
        <div className="cubo" ref={fijarRef}>
          {ejes.g.map((m, i) => (
            <span key={`g-${m}`} aria-hidden="true" className="cubo-rotulo cubo-rotulo--g" ref={conVariables({ '--y': String(i - (ejes.g.length - 1) / 2), '--borde-x': String((ejes.f.length - 1) / 2), '--borde-z': String((ejes.t.length - 1) / 2) })}>
              {m === TODOS_CUBO ? 'Todos' : m}
            </span>
          ))}
          {ejes.f.map((m, i) => (
            <span key={`f-${m}`} aria-hidden="true" className="cubo-rotulo cubo-rotulo--f" ref={conVariables({ '--x': String(i - (ejes.f.length - 1) / 2), '--borde-y': String((ejes.g.length - 1) / 2), '--borde-z': String((ejes.t.length - 1) / 2) })}>
              {m === TODOS_CUBO ? 'Todas' : m}
            </span>
          ))}
          {ejes.t.map((m, i) => (
            <span key={`t-${m}`} aria-hidden="true" className="cubo-rotulo cubo-rotulo--t" ref={conVariables({ '--z': String((ejes.t.length - 1) / 2 - i), '--borde-x': String((ejes.f.length - 1) / 2), '--borde-y': String((ejes.g.length - 1) / 2) })}>
              {m === TODOS_CUBO ? 'Todos' : m}
            </span>
          ))}
          {ejes.t.flatMap((t, it) =>
            ejes.g.flatMap((g, ig) =>
              ejes.f.map((f, ifr) => {
                const c = indice.get(claveCelda(g, f, t));
                const v = c ? kpi.valor(c) : null;
                const total = g === TODOS_CUBO || f === TODOS_CUBO || t === TODOS_CUBO;
                const activo = hay && dentro(sel, g, f, t);
                const x = ifr - (ejes.f.length - 1) / 2 + desplazamiento(ejes.f, sel.franja, f);
                const y = ig - (ejes.g.length - 1) / 2 + desplazamiento(ejes.g, sel.genero, g);
                const z = (ejes.t.length - 1) / 2 - it - desplazamiento(ejes.t, sel.tercero, t);
                const luz = luzDe(total || v === null ? 0 : v / maximo);
                const variables = { '--x': String(x), '--y': String(y), '--z': String(z), '--brillo': luz.brillo.toFixed(3) };
                const texto = v === null ? '–' : kpi.formato(v).replace(' min', 'm').replace(' %', '%');
                const lectura = `${v === null ? 'Sin dato' : kpi.formato(v)}: ${nombreDe(g, 'genero')}, ${nombreDe(f, 'franja')}, ${nombreDe(t, 'tercero')}`;
                return (
                  <div
                    key={`${g}|${f}|${t}`}
                    className="cubito"
                    ref={conVariables(variables)}
                    data-g={g}
                    data-f={f}
                    data-t={t}
                    data-lectura={lectura}
                    data-total={total || undefined}
                    data-encendido={luz.encendido || undefined}
                    data-activo={activo || undefined}
                    data-apagado={(hay && !activo) || undefined}
                  >
                    {[0, 1, 2, 3, 4, 5].map((cara) => (
                      <i key={cara}>{texto}</i>
                    ))}
                  </div>
                );
              }),
            ),
          )}
        </div>
      </div>
      <div className="cubo-pie">
        <p className="cubo-lectura" aria-live="off">
          {lectura ?? `Arrastra para girar. Toca un cubito para ver su celda. Vertical: ${nombres.genero}. Horizontal: ${nombres.franja}. Fondo: ${nombres.tercero}.`}
        </p>
        <div className="cubo-vistas" role="group" aria-label="Vistas del cubo">
          {Object.entries(VISTAS).map(([nombre, [x, y]]) => (
            <button
              key={nombre}
              type="button"
              onClick={() => {
                giro.current = { x, y };
                aplicar();
              }}
            >
              {nombre}
            </button>
          ))}
        </div>
      </div>
    </>
  );
}

function TablaCruce({
  datos, indice, kpi, sel, pivot, nombres,
}: {
  datos: CuboAnalitica;
  indice: ReturnType<typeof indiceCeldas>;
  kpi: Kpi;
  sel: Seleccion;
  pivot: boolean;
  nombres: Record<Dim, string>;
}) {
  const { filas, columnas, resto } = ejesDeTabla(sel, pivot);
  const de = (d: Dim) => (sel[d].length > 0 ? sel[d] : datos.miembros[d]);
  const leer = (f: string[], c: string[]) => leerKpi(indice, kpi, { ...sel, [filas]: f, [columnas]: c });
  const listaF = de(filas);
  const listaC = de(columnas);
  const conTotalF = listaF.length > 1;
  const conTotalC = listaC.length > 1;
  const cuerpo = listaF.flatMap((f) => listaC.map((c) => leer([f], [c]) ?? 0));
  const maximo = Math.max(1, ...cuerpo);
  const textoResto = sel[resto].length > 0 ? sel[resto].join(', ') : 'todos';
  const valor = (n: number | null) => (n === null ? 'No aplica' : kpi.formato(n));

  return (
    <section className="caja mt-4 cubo-tabla-caja" aria-labelledby="t-cruce">
      <div className="caja__cabeza">
        <h2 id="t-cruce">{kpi.nombre} por {nombres[filas].toLowerCase()} y {nombres[columnas].toLowerCase()}</h2>
      </div>
      <p className="secundario text-[14px]">
        {nombres[resto]}: {textoResto}.
      </p>
      <div className="cubo-tabla-marco" tabIndex={0} role="group" aria-label="Tabla de cruce, se desplaza de lado">
        <table className="tabla-datos cubo-tabla">
          <thead>
            <tr>
              <th scope="col">{nombres[filas]}</th>
              {listaC.map((c) => (
                <th key={c} scope="col" className="num">
                  {c}
                </th>
              ))}
              {conTotalC ? <th scope="col" className="num">Total</th> : null}
            </tr>
          </thead>
          <tbody>
            {listaF.map((f) => (
              <tr key={f}>
                <th scope="row">{f}</th>
                {listaC.map((c) => {
                  const n = leer([f], [c]);
                  const luz = luzDe((n ?? 0) / maximo);
                  return (
                    <td key={c} className="num cubo-calor" ref={conVariables({ '--brillo': luz.brillo.toFixed(3) })} data-encendido={luz.encendido || undefined}>
                      {valor(n)}
                    </td>
                  );
                })}
                {conTotalC ? <td className="num cubo-total">{valor(leer([f], sel[columnas]))}</td> : null}
              </tr>
            ))}
            {conTotalF ? (
              <tr>
                <th scope="row">Total</th>
                {listaC.map((c) => (
                  <td key={c} className="num cubo-total">
                    {valor(leer(sel[filas], [c]))}
                  </td>
                ))}
                {conTotalC ? <td className="num cubo-total">{valor(leer(sel[filas], sel[columnas]))}</td> : null}
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </section>
  );
}
