'use client';

import { useEffect, useRef, useState } from 'react';

/**
 * Gráficos del tablero (doc 05, «Gráficos»; skill dataviz). SVG propio, sin `style` (CSP): las
 * medidas son atributos y los colores, clases con tokens. Marcas finas (≤ 24 px) con la punta
 * redondeada y la base recta; rejilla de un pelo; el texto nunca lleva el color del dato.
 * Cada gráfico tiene su tabla gemela (`TablaDatos`), así ningún valor depende del puntero.
 */

export type Fila = { clave: string; etiqueta: string; valor: number };

/** El ancho real del contenedor: los textos no se encogen en el teléfono. */
function useAncho<T extends HTMLElement>() {
  const ref = useRef<T>(null);
  const [ancho, setAncho] = useState(0);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const ro = new ResizeObserver(([e]) => setAncho(Math.floor(e!.contentRect.width)));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);
  return [ref, ancho] as const;
}

/** Barra horizontal: punta redondeada a la derecha, base recta. */
function barraH(x: number, y: number, w: number, h: number): string {
  if (w <= 0) return '';
  const r = Math.min(4, w, h / 2);
  return `M${x},${y}H${x + w - r}Q${x + w},${y} ${x + w},${y + r}V${y + h - r}Q${x + w},${y + h} ${x + w - r},${y + h}H${x}Z`;
}

/** Columna: punta redondeada arriba, base recta. */
function columna(x: number, base: number, w: number, h: number): string {
  if (h <= 0) return '';
  const r = Math.min(4, h, w / 2);
  return `M${x},${base}V${base - h + r}Q${x},${base - h} ${x + r},${base - h}H${x + w - r}Q${x + w},${base - h} ${x + w},${base - h + r}V${base}Z`;
}

/** Segmento de una barra 100 %: redondea solo los extremos de afuera. */
function segmento(x: number, y: number, w: number, h: number, izq: boolean, der: boolean): string {
  if (w <= 0) return '';
  const r = Math.min(4, w / 2, h / 2);
  const ri = izq ? r : 0;
  const rd = der ? r : 0;
  return `M${x + ri},${y}H${x + w - rd}${rd ? `Q${x + w},${y} ${x + w},${y + rd}` : ''}V${y + h - rd}${rd ? `Q${x + w},${y + h} ${x + w - rd},${y + h}` : ''}H${x + ri}${ri ? `Q${x},${y + h} ${x},${y + h - ri}` : ''}V${y + ri}${ri ? `Q${x},${y} ${x + ri},${y}` : ''}Z`;
}

/** Marcas del eje redondas: 0, 50, 100… (máximo tres o cuatro líneas). */
export function marcasEje(maximo: number): number[] {
  if (maximo <= 0) return [0, 1];
  const bruto = maximo / 3;
  const mag = 10 ** Math.floor(Math.log10(bruto));
  const paso = [1, 2, 2.5, 5, 10].map((m) => m * mag).find((p) => p >= bruto)!;
  const tope = Math.ceil(maximo / paso) * paso;
  return Array.from({ length: Math.round(tope / paso) + 1 }, (_, i) => Math.round(i * paso * 100) / 100);
}

/** Barras horizontales con el valor en la punta (rankings y comparaciones). */
export function Barras({ titulo, filas, formato, enfasis }: { titulo: string; filas: Fila[]; formato: (v: number) => string; enfasis?: ReadonlySet<string> }) {
  const [ref, ancho] = useAncho<HTMLDivElement>();
  const fila = 32;
  const grosor = 18;
  const etiquetas = Math.round(Math.min(170, Math.max(78, ancho * 0.3)));
  const espacioValor = 64;
  const area = Math.max(0, ancho - etiquetas - espacioValor);
  const maximo = Math.max(...filas.map((f) => f.valor), 0);
  const descripcion = `${titulo}: ${filas.map((f) => `${f.etiqueta}, ${formato(f.valor)}`).join('; ')}`;
  return (
    <div className="grafico" ref={ref}>
      {ancho > 0 ? (
        <svg width={ancho} height={filas.length * fila} role="img" aria-label={descripcion}>
          {filas.map((f, i) => {
            const y = i * fila + (fila - grosor) / 2;
            const w = maximo > 0 ? (f.valor / maximo) * area : 0;
            return (
              <g key={f.clave} className="g-marca">
                <text x={0} y={y + grosor / 2} dominantBaseline="central">
                  {f.etiqueta}
                </text>
                <path d={barraH(etiquetas, y, w, grosor)} className={!enfasis || enfasis.has(f.clave) ? 'g-dato' : 'g-contexto'} />
                <text x={etiquetas + w + 8} y={y + grosor / 2} dominantBaseline="central" className="g-valor">
                  {formato(f.valor)}
                </text>
              </g>
            );
          })}
        </svg>
      ) : (
        <div className="h-8" />
      )}
    </div>
  );
}

/**
 * Columnas (franjas horarias, noches). Se rotulan solo las resaltadas; el resto se lee al
 * pasar el puntero o con el teclado, y en la tabla.
 */
export function Columnas({
  titulo,
  filas,
  formato,
  enfasis,
  umbral,
}: {
  titulo: string;
  filas: Fila[];
  formato: (v: number) => string;
  enfasis: ReadonlySet<string>;
  umbral?: { valor: number; etiqueta: string };
}) {
  const [ref, ancho] = useAncho<HTMLDivElement>();
  const tip = useRef<HTMLDivElement>(null);
  const [activo, setActivo] = useState<number | null>(null);
  const eje = 36;
  // Con umbral, su clave va en el margen de arriba: así no tapa ninguna columna.
  const arriba = umbral ? 34 : 22;
  const alto = 170;
  const abajo = 26;
  const marcas = marcasEje(Math.max(...filas.map((f) => f.valor), umbral?.valor ?? 0));
  const tope = marcas.at(-1)!;
  const y = (v: number) => arriba + alto - (v / tope) * alto;
  const hueco = filas.length > 0 ? (ancho - eje) / filas.length : 0;
  const grosor = Math.max(6, Math.min(24, hueco * 0.62));
  // Rótulos del eje x: todos si caben; si no, uno cada tantos (y siempre el último). Se mide el
  // rótulo más largo con un ancho por letra aproximado (12 px de cuerpo).
  const largo = Math.max(...filas.map((f) => f.etiqueta.length), 1) * 6.6 + 12;
  const cada = Math.max(1, Math.ceil(largo / Math.max(hueco, 1)));

  useEffect(() => {
    const el = tip.current;
    if (!el || activo === null) return;
    const f = filas[activo];
    if (!f) return;
    el.style.left = `${eje + hueco * activo + hueco / 2}px`;
    el.style.top = `${y(f.valor)}px`;
  });

  const f = activo !== null ? filas[activo] : null;
  return (
    <div className="grafico" ref={ref}>
      {ancho > 0 ? (
        <svg width={ancho} height={arriba + alto + abajo} role="group" aria-label={titulo}>
          {marcas.map((m) => (
            <g key={m}>
              <line x1={eje} x2={ancho} y1={y(m)} y2={y(m)} className={m === 0 ? 'g-eje' : 'g-rejilla'} />
              <text x={eje - 8} y={y(m)} textAnchor="end" dominantBaseline="central" className="g-suave">
                {m}
              </text>
            </g>
          ))}
          {filas.map((fila, i) => {
            const x = eje + hueco * i + (hueco - grosor) / 2;
            const resaltada = enfasis.has(fila.clave);
            return (
              <g
                key={fila.clave}
                className="g-marca"
                tabIndex={0}
                role="img"
                aria-label={`${fila.etiqueta}: ${formato(fila.valor)}`}
                onPointerEnter={() => setActivo(i)}
                onPointerLeave={() => setActivo(null)}
                onFocus={() => setActivo(i)}
                onBlur={() => setActivo(null)}
              >
                {/* El blanco es toda la franja, no solo la barra. */}
                <rect x={eje + hueco * i} y={arriba} width={hueco} height={alto} fill="transparent" />
                <path d={columna(x, y(0), grosor, y(0) - y(fila.valor))} className={resaltada ? 'g-dato' : 'g-contexto'} />
                <rect x={x - 3} y={y(fila.valor) - 3} width={grosor + 6} height={y(0) - y(fila.valor) + 6} rx={5} className="g-foco" fill="none" />
                {resaltada ? (
                  <text x={x + grosor / 2} y={y(fila.valor) - 8} textAnchor="middle" className="g-valor">
                    {formato(fila.valor)}
                  </text>
                ) : null}
                {(filas.length - 1 - i) % cada === 0 ? (
                  <text
                    x={eje + hueco * i + hueco / 2}
                    y={arriba + alto + 17}
                    // Los de las puntas se alinean hacia dentro para no salirse del gráfico.
                    textAnchor={eje + hueco * i + hueco / 2 + largo / 2 > ancho ? 'end' : eje + hueco * i + hueco / 2 - largo / 2 < 0 ? 'start' : 'middle'}
                    dx={eje + hueco * i + hueco / 2 + largo / 2 > ancho ? hueco / 2 : 0}
                    className="g-suave"
                  >
                    {fila.etiqueta}
                  </text>
                ) : null}
              </g>
            );
          })}
          {umbral ? (
            <g aria-hidden="true">
              <line x1={eje} x2={ancho} y1={y(umbral.valor)} y2={y(umbral.valor)} className="g-umbral" />
              {/* La clave del umbral: un trazo como la línea y su rótulo, arriba a la derecha. */}
              <line x1={ancho - umbral.etiqueta.length * 6.6 - 30} x2={ancho - umbral.etiqueta.length * 6.6 - 12} y1={10} y2={10} className="g-umbral" />
              <text x={ancho} y={10} textAnchor="end" dominantBaseline="central" className="g-suave">
                {umbral.etiqueta}
              </text>
            </g>
          ) : null}
        </svg>
      ) : (
        <div className="h-[218px]" />
      )}
      {f ? (
        <div className="grafico-tip" ref={tip} aria-hidden="true">
          <b>{formato(f.valor)}</b>
          <span>{f.etiqueta}</span>
        </div>
      ) : null}
    </div>
  );
}

export type Parte = { clave: string; etiqueta: string; valor: number; tono: 'dato' | 'contexto'; detalle?: string };

/** Una barra 100 % de dos partes (lo pedido frente a la lista de la casa), con su leyenda. */
export function Proporcion({ titulo, partes }: { titulo: string; partes: Parte[] }) {
  const [ref, ancho] = useAncho<HTMLDivElement>();
  const total = partes.reduce((s, p) => s + p.valor, 0);
  const pct = (v: number) => (total > 0 ? Math.round((v / total) * 100) : 0);
  const grosor = 20;
  const gap = 2;
  // Posiciones antes de pintar: el hueco de 2 px separa las partes, sin borde.
  const visibles = partes.filter((p) => p.valor > 0);
  const util = Math.max(0, ancho - gap * (visibles.length - 1));
  const segmentos = visibles.reduce<{ p: Parte; x: number; w: number }[]>((acc, p) => {
    const previo = acc.at(-1);
    acc.push({ p, x: previo ? previo.x + previo.w + gap : 0, w: total > 0 ? (p.valor / total) * util : 0 });
    return acc;
  }, []);
  return (
    <div className="grafico" ref={ref}>
      {ancho > 0 ? (
        <svg width={ancho} height={grosor} role="img" aria-label={`${titulo}: ${partes.map((p) => `${p.etiqueta}, ${pct(p.valor)} %`).join('; ')}`}>
          {segmentos.map(({ p, x, w }, i) => (
            <path key={p.clave} d={segmento(x, 0, w, grosor, i === 0, i === segmentos.length - 1)} className={p.tono === 'dato' ? 'g-dato' : 'g-contexto'} />
          ))}
        </svg>
      ) : (
        <div className="h-5" />
      )}
      <ul className="leyenda">
        {partes.map((p) => (
          <li key={p.clave}>
            <i className={p.tono === 'dato' ? 'l-dato' : 'l-contexto'} aria-hidden="true" />
            <span>
              {p.etiqueta} <b>{pct(p.valor)} %</b>
              {p.detalle ? <span className="secundario">, {p.detalle}</span> : null}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

/** Un porcentaje frente a su umbral (concentración de votos). */
export function Medidor({ titulo, valor, umbral, etiquetaUmbral }: { titulo: string; valor: number; umbral: number; etiquetaUmbral: string }) {
  const [ref, ancho] = useAncho<HTMLDivElement>();
  const grosor = 14;
  const arriba = 22;
  const xu = (umbral / 100) * ancho;
  return (
    <div className="grafico" ref={ref}>
      {ancho > 0 ? (
        <svg width={ancho} height={arriba + grosor + 6} role="img" aria-label={`${titulo}: ${valor} %, umbral ${umbral} %`}>
          <path d={segmento(0, arriba, ancho, grosor, true, true)} className="g-pista" />
          <path d={segmento(0, arriba, (Math.min(100, valor) / 100) * ancho, grosor, true, true)} className="g-dato" />
          <line x1={xu} x2={xu} y1={arriba - 5} y2={arriba + grosor + 5} className="g-umbral" />
          <text x={xu} y={arriba - 9} textAnchor={umbral > 80 ? 'end' : umbral < 20 ? 'start' : 'middle'} className="g-suave">
            {etiquetaUmbral}
          </text>
        </svg>
      ) : (
        <div className="h-[42px]" />
      )}
    </div>
  );
}

/** La tabla gemela de un gráfico: los mismos valores, sin depender del color ni del puntero. */
export function TablaDatos({
  titulo,
  columnas,
  filas,
  abierta = false,
}: {
  titulo: string;
  columnas: { titulo: string; num?: boolean }[];
  filas: { clave: string; celdas: (string | number)[]; marcada?: boolean }[];
  abierta?: boolean;
}) {
  return (
    <details className="ver-tabla" open={abierta}>
      <summary>Ver como tabla</summary>
      <table className="tabla-datos">
        <caption className="solo-lector">{titulo}</caption>
        <thead>
          <tr>
            {columnas.map((c) => (
              <th key={c.titulo} scope="col" className={c.num ? 'num' : undefined}>
                {c.titulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.map((f) => (
            <tr key={f.clave} data-marcada={f.marcada || undefined}>
              {f.celdas.map((c, i) => (
                <td key={i} className={columnas[i]?.num ? 'num' : undefined}>
                  {c}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </details>
  );
}
