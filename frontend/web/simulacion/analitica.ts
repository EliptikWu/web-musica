import type {
  CausaNoSono, ComparacionAnalitica, DiagnosticoAnalitica, NocheAnalitica, OrigenDatos, ParticipacionAnalitica, Recomendacion, ResumenAnalitica,
  UmbralesAnalitica,
} from '@rockola/contrato';
import { localAIso } from '../lib/formato';
import { CATALOGO, LISTA_SALSA_Y_TROPICAL } from './datos/catalogo';
import { aContrato, bloqueoVigente } from './motor';
import type { CancionSim, EstadoSim } from './tipos';

/**
 * El tablero analítico de la simulación (RF-049 a RF-054). Todo es **dato de ejemplo**
 * (RNF-015): se generan, siempre iguales, las noches cerradas del 10 de septiembre al 21 de
 * octubre de 2026 según el horario del bar y su calendario de eventos, y cada noche se juega
 * con una cola de verdad (los votos ordenan, la lista de la casa llena los silencios, lo que
 * queda al cerrar no suena). Los agregados de abajo son lo que el backend calculará con los
 * datos reales; la noche en curso entra cuando se cierra (PB-43).
 */

export const UMBRALES_POR_DEFECTO: UmbralesAnalitica = { votos_minimos: 6, concentracion_pct: 40, participacion_minima: 15 };

const PRIMER_DIA = '2026-09-10';
const ULTIMO_DIA = '2026-10-21';
const HORA = 3_600_000;

type Causa = CausaNoSono;
type SolicitudA = {
  id: string;
  video_id: string;
  dispositivo: string;
  pedida: number;
  votos: { en: number; dispositivo: string }[];
  estado: 'sono' | Causa;
  inicio: number | null;
};
type ReproduccionA = { inicio: number; video_id: string; origen: 'solicitud' | 'lista_casa'; votos: number };
export type NocheA = {
  noche: NocheAnalitica;
  /** La zona del bar: las fechas y las horas del tablero son las de allá. */
  zona: string;
  inicio: number;
  fin: number;
  solicitudes: SolicitudA[];
  reproducciones: ReproduccionA[];
  pedidos_seg: number;
  casa_seg: number;
};

// ── Azar repetible ─────────────────────────────────────────────────────────

function semilla(texto: string): number {
  let h = 2166136261;
  for (const c of texto) h = Math.imul(h ^ c.charCodeAt(0), 16777619);
  return h >>> 0;
}

function azar(texto: string) {
  let a = semilla(texto);
  const siguiente = () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  return {
    numero: siguiente,
    entero: (min: number, max: number) => min + Math.floor(siguiente() * (max - min + 1)),
    /** Elige con pesos. */
    pesado: <T>(opciones: readonly (readonly [T, number])[]): T => {
      const total = opciones.reduce((s, [, p]) => s + p, 0);
      let r = siguiente() * total;
      for (const [valor, peso] of opciones) if ((r -= peso) <= 0) return valor;
      return opciones.at(-1)![0];
    },
  };
}

// ── Las noches ─────────────────────────────────────────────────────────────

const PERFILES = {
  normal: { generos: [['urbano', 44], ['salsa', 30], ['pop', 10], ['tropical', 8], ['vallenato', 4], ['sin_clasificar', 4]] },
  karaoke: { generos: [['pop', 38], ['salsa', 24], ['vallenato', 18], ['urbano', 12], ['tropical', 8]] },
  partido: { generos: [['urbano', 55], ['salsa', 15], ['pop', 15], ['tropical', 10], ['sin_clasificar', 5]] },
} as const;

/** Las pedibles de cada género, de la más a la menos pedida (el orden del catálogo). */
const POR_GENERO: Record<string, CancionSim[]> = {};
for (const c of Object.values(CATALOGO)) {
  if (c.duracion_seg > 600) continue;
  (POR_GENERO[c.genero] ??= []).push(c);
}

const CASA = LISTA_SALSA_Y_TROPICAL.map((id) => CATALOGO[id]).filter((c): c is CancionSim => Boolean(c));

function diaSemana(fecha: string): number {
  const d = new Date(`${fecha}T12:00:00Z`).getUTCDay();
  return d === 0 ? 7 : d;
}

function fechas(): string[] {
  const salida: string[] = [];
  for (let t = Date.parse(`${PRIMER_DIA}T12:00:00Z`); t <= Date.parse(`${ULTIMO_DIA}T12:00:00Z`); t += 24 * HORA) salida.push(new Date(t).toISOString().slice(0, 10));
  return salida;
}

function generarNoche(fecha: string, evento: NocheAnalitica['evento'], ventana: { inicio: number; fin: number }, zona: string): NocheA {
  const r = azar(`noche-${fecha}`);
  const dia = diaSemana(fecha);
  const tipo = evento?.tipo_evento === 'karaoke' || evento?.tipo_evento === 'partido' ? evento.tipo_evento : 'normal';
  const base = tipo === 'karaoke' ? 22 : tipo === 'partido' ? 18 : dia === 4 ? 36 : dia === 5 ? 54 : 62;
  // Un jueves de lluvia: la noche con poca gente que el diagnóstico debe marcar.
  const factor = fecha === '2026-10-01' ? 0.32 : 0.82 + r.numero() * 0.36;
  const cuantas = Math.max(6, Math.round(base * factor));
  const largo = ventana.fin - ventana.inicio;

  // Los teléfonos: habituales (se repiten entre noches) y nuevos de la noche.
  const habituales = Array.from({ length: Math.round(cuantas * 0.35) }, () => `d-h${r.entero(1, 140)}`);
  const nuevos = Array.from({ length: Math.round(cuantas * 0.22) + 2 }, (_, i) => `d-${fecha}-${i}`);
  const pedidores = [...new Set([...habituales, ...nuevos])];
  const mirones = Array.from({ length: Math.round(cuantas * 0.12) }, (_, i) => `d-${fecha}-m${i}`);
  const votantes = [...pedidores, ...mirones];
  // Una noche con un grupo que vota todo junto: la concentración se dispara.
  const barra = fecha === '2026-10-10' ? votantes.slice(0, 5) : null;

  const solicitudes: SolicitudA[] = [];
  for (let i = 0; i < cuantas; i++) {
    const genero = r.pesado(PERFILES[tipo].generos);
    const opciones = POR_GENERO[genero] ?? POR_GENERO.salsa!;
    // Dentro del género, las primeras se piden más.
    const cancion = opciones[Math.min(opciones.length - 1, Math.floor(opciones.length * r.numero() ** 1.4))]!;
    // Más pedidos a mitad de la noche; los viernes y sábados, también al final.
    const tardio = (dia === 5 || dia === 6) && tipo === 'normal' && r.numero() < 0.22;
    const posicion = tardio ? 0.8 + r.numero() * 0.19 : Math.min(0.97, (r.numero() + r.numero() + r.numero()) / 3);
    const pedida = ventana.inicio + Math.round(posicion * largo);
    const pedidor = pedidores[r.entero(0, pedidores.length - 1)]!;
    // Un voto por teléfono y canción, y nadie vota lo suyo (RN-02, RN-03).
    const nVotos = Math.min(30, Math.round(-Math.log(1 - r.numero()) * 4.2));
    const quienes = new Set<string>();
    if (barra) for (const d of barra) if (d !== pedidor && r.numero() < 0.85) quienes.add(d);
    for (let intento = 0; quienes.size < nVotos && intento < nVotos * 6; intento++) {
      const d = votantes[Math.floor(votantes.length * r.numero() ** 1.6)]!;
      if (d !== pedidor) quienes.add(d);
    }
    const votos = [...quienes].map((dispositivo) => ({ en: pedida + Math.round(r.numero() ** 1.5 * 12 * 60_000), dispositivo }));
    solicitudes.push({ id: `${fecha}-s${i}`, video_id: cancion.video_id, dispositivo: pedidor, pedida, votos, estado: 'sono', inicio: null });
  }
  solicitudes.sort((a, b) => a.pedida - b.pedida);

  // Se juega la noche: la de más votos primero; sin pedidos, la lista de la casa.
  const reproducciones: ReproduccionA[] = [];
  let pedidos_seg = 0;
  let casa_seg = 0;
  let indiceCasa = r.entero(0, CASA.length - 1);
  let t = ventana.inicio;
  const pendientes = [...solicitudes];
  while (t < ventana.fin) {
    const listas = pendientes.filter((s) => s.pedida <= t);
    if (listas.length === 0) {
      const c = CASA[indiceCasa++ % CASA.length]!;
      reproducciones.push({ inicio: t, video_id: c.video_id, origen: 'lista_casa', votos: 0 });
      const dura = Math.min(c.duracion_seg * 1000, ventana.fin - t);
      casa_seg += Math.round(dura / 1000);
      t += c.duracion_seg * 1000;
      continue;
    }
    // Ordenan los votos que ya llegaron; los que vendrían después de este momento no cuentan.
    const hasta = (s: SolicitudA) => s.votos.filter((v) => v.en <= t).length;
    listas.sort((a, b) => hasta(b) - hasta(a) || a.pedida - b.pedida);
    const s = listas[0]!;
    pendientes.splice(pendientes.indexOf(s), 1);
    s.votos = s.votos.filter((v) => v.en <= t);
    const sorteo = r.numero();
    if (sorteo < 0.02) s.estado = 'retirada';
    else if (sorteo < 0.035) s.estado = 'no_disponible';
    else if (sorteo < 0.05 || (s.video_id === 'sim-la-bachata' && fecha >= '2026-10-08')) s.estado = 'bloqueada';
    if (s.estado !== 'sono') continue;
    const c = CATALOGO[s.video_id]!;
    const omitida = r.numero() < 0.035;
    s.inicio = t;
    s.estado = omitida ? 'omitida' : 'sono';
    reproducciones.push({ inicio: t, video_id: s.video_id, origen: 'solicitud', votos: s.votos.length });
    const dura = omitida ? 40 : c.duracion_seg;
    pedidos_seg += Math.min(dura, Math.round((ventana.fin - t) / 1000));
    t += dura * 1000;
  }
  for (const s of pendientes) {
    s.estado = 'fuera_de_tiempo';
    s.votos = s.votos.filter((v) => v.en < ventana.fin);
  }

  return {
    noche: { id: `n-${fecha}`, inicio: new Date(ventana.inicio).toISOString(), fin: new Date(ventana.fin).toISOString(), dia_semana: dia, evento },
    zona,
    inicio: ventana.inicio,
    fin: ventana.fin,
    solicitudes,
    reproducciones,
    pedidos_seg,
    casa_seg,
  };
}

let memoria: { clave: string; noches: NocheA[] } | null = null;

/** Las noches cerradas, de la más vieja a la más nueva. Siempre las mismas para el mismo calendario. */
export function nochesDeEjemplo(estado: EstadoSim): NocheA[] {
  const zona = estado.bar.zona_horaria;
  const clave = JSON.stringify([estado.eventos, estado.horario, zona]);
  if (memoria?.clave === clave) return memoria.noches;
  const noches: NocheA[] = [];
  for (const fecha of fechas()) {
    const dia = diaSemana(fecha);
    const ev = estado.eventos.find((e) => fechaLocal(Date.parse(e.inicio), zona) === fecha && e.tipo_evento !== 'normal');
    const franja = estado.horario.find((f) => f.dia_semana === dia && f.activo);
    let ventana: { inicio: number; fin: number } | null = null;
    if (ev) ventana = { inicio: Date.parse(ev.inicio), fin: Date.parse(ev.fin) };
    else if (franja) {
      const inicio = Date.parse(localAIso(`${fecha}T${franja.hora_inicio}`, zona));
      // Si cruza medianoche, cierra al día siguiente (en la zona del bar).
      const otroDia = franja.hora_fin <= franja.hora_inicio ? new Date(Date.parse(`${fecha}T12:00:00Z`) + 24 * HORA).toISOString().slice(0, 10) : fecha;
      ventana = { inicio, fin: Date.parse(localAIso(`${otroDia}T${franja.hora_fin}`, zona)) };
    }
    if (!ventana) continue;
    noches.push(generarNoche(fecha, ev ? { id: ev.id, nombre: ev.nombre, tipo_evento: ev.tipo_evento } : null, ventana, zona));
  }
  memoria = { clave, noches };
  return noches;
}

const formatos = new Map<string, Intl.DateTimeFormat>();

/** Fecha (`AAAA-MM-DD`), hora y mes de un instante en la zona del bar. */
function enZona(instante: number, zona: string) {
  let f = formatos.get(zona);
  if (!f) {
    f = new Intl.DateTimeFormat('en-CA', { timeZone: zona, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', hourCycle: 'h23' });
    formatos.set(zona, f);
  }
  const p = Object.fromEntries(f.formatToParts(new Date(instante)).map((x) => [x.type, x.value]));
  return { fecha: `${p.year}-${p.month}-${p.day}`, hora: Number(p.hour), mes: Number(p.month), dia: Number(p.day) };
}

const fechaLocal = (instante: number, zona: string) => enZona(instante, zona).fecha;
const horaLocal = (instante: number, zona: string) => enZona(instante, zona).hora;

const popularidades = new WeakMap<NocheA[], { dia: number; mapa: Map<string, number> }>();

/**
 * La popularidad de la búsqueda (PB-51): cuántas veces se pidió cada canción en el bar en las
 * cuatro semanas hasta `ahora`, contando la noche en curso. Lo de las noches cerradas se
 * calcula una vez por día; lo de esta noche, en cada búsqueda (son pocas solicitudes).
 */
export function pedidasEnElBar(estado: EstadoSim, ahora: number): (videoId: string) => number {
  const noches = nochesDeEjemplo(estado);
  const dia = Math.floor(ahora / (24 * HORA));
  let guardado = popularidades.get(noches);
  if (!guardado || guardado.dia !== dia) {
    const desde = ahora - 28 * 24 * HORA;
    const mapa = new Map<string, number>();
    for (const n of noches) if (n.inicio > desde && n.inicio <= ahora) for (const s of n.solicitudes) mapa.set(s.video_id, (mapa.get(s.video_id) ?? 0) + 1);
    guardado = { dia, mapa };
    popularidades.set(noches, guardado);
  }
  const cerradas = guardado.mapa;
  const hoy = new Map<string, number>();
  for (const s of estado.solicitudes) if (s.fecha >= ahora - 28 * 24 * HORA) hoy.set(s.video_id, (hoy.get(s.video_id) ?? 0) + 1);
  return (videoId) => (cerradas.get(videoId) ?? 0) + (hoy.get(videoId) ?? 0);
}

// ── Selección ──────────────────────────────────────────────────────────────

export type Filtro = { noche?: string | null; desde?: string | null; hasta?: string | null; dia_semana?: string | null };

/** Las noches que entran en la consulta, o `null` si la noche pedida no existe. */
export function seleccionar(noches: NocheA[], f: Filtro): NocheA[] | null {
  if (f.noche) {
    const una = noches.find((n) => n.noche.id === f.noche);
    return una ? [una] : null;
  }
  const dia = f.dia_semana ? Number(f.dia_semana) : null;
  return noches.filter((n) => {
    const fecha = fechaLocal(n.inicio, n.zona);
    return (!f.desde || fecha >= f.desde) && (!f.hasta || fecha <= f.hasta) && (!dia || n.noche.dia_semana === dia);
  });
}

export function origenDatos(todas: NocheA[], elegidas: NocheA[]): OrigenDatos {
  const ultima = todas.at(-1);
  return {
    origen: 'ejemplo',
    // El pipeline carga la noche un cuarto de hora después de cerrarla.
    actualizado_en: new Date((ultima?.fin ?? 0) + 15 * 60_000).toISOString(),
    noches: elegidas.length,
    desde: elegidas[0]?.noche.inicio ?? null,
    hasta: elegidas.at(-1)?.noche.inicio ?? null,
  };
}

const sumar = (xs: number[]) => xs.reduce((s, x) => s + x, 0);
const contrato = (videoId: string) => aContrato(CATALOGO[videoId]!);

function telefonosDe(noches: NocheA[]): number {
  const unicos = new Set<string>();
  for (const n of noches) for (const s of n.solicitudes) {
    unicos.add(s.dispositivo);
    for (const v of s.votos) unicos.add(v.dispositivo);
  }
  return unicos.size;
}

function esperas(noches: NocheA[]): number[] {
  return noches.flatMap((n) => n.solicitudes.filter((s) => s.inicio !== null).map((s) => Math.round((s.inicio! - s.pedida) / 1000)));
}

// ── RF-049, RF-050 ─────────────────────────────────────────────────────────

export function resumen(todas: NocheA[], noches: NocheA[]): ResumenAnalitica {
  const solicitudes = noches.flatMap((n) => n.solicitudes);
  const porCancion = new Map<string, { votos: number; solicitudes: number }>();
  for (const s of solicitudes) {
    const x = porCancion.get(s.video_id) ?? { votos: 0, solicitudes: 0 };
    x.votos += s.votos.length;
    x.solicitudes += 1;
    porCancion.set(s.video_id, x);
  }
  const popular = [...porCancion.entries()].sort(([, a], [, b]) => b.votos - a.votos || b.solicitudes - a.solicitudes)[0];
  const e = esperas(noches);
  return {
    datos: origenDatos(todas, noches),
    solicitudes: solicitudes.length,
    telefonos: telefonosDe(noches),
    votos: sumar(solicitudes.map((s) => s.votos.length)),
    mas_popular: popular ? { cancion: contrato(popular[0]), ...popular[1] } : null,
    espera: e.length > 0 ? { promedio_seg: Math.round(sumar(e) / e.length), maxima_seg: Math.max(...e) } : null,
    tiempo: { pedidos_seg: sumar(noches.map((n) => n.pedidos_seg)), lista_casa_seg: sumar(noches.map((n) => n.casa_seg)) },
    no_sonaron: solicitudes.filter((s) => s.estado !== 'sono').length,
    historial:
      noches.length === 1
        ? noches[0]!.reproducciones.map((x) => ({ inicio: new Date(x.inicio).toISOString(), cancion: contrato(x.video_id), origen: x.origen, votos: x.votos }))
        : [],
  };
}

// ── RF-051 ─────────────────────────────────────────────────────────────────

export function participacion(todas: NocheA[], noches: NocheA[]): ParticipacionAnalitica {
  const generos = new Map<string, { solicitudes: number; votos: number }>();
  const franjas = new Map<number, { solicitudes: number; votos: number; telefonos: Set<string> }>();
  const franja = (hora: number) => {
    let f = franjas.get(hora);
    if (!f) franjas.set(hora, (f = { solicitudes: 0, votos: 0, telefonos: new Set() }));
    return f;
  };
  for (const n of noches) {
    for (const s of n.solicitudes) {
      const g = CATALOGO[s.video_id]!.genero;
      const x = generos.get(g) ?? { solicitudes: 0, votos: 0 };
      x.solicitudes += 1;
      x.votos += s.votos.length;
      generos.set(g, x);
      const f = franja(horaLocal(s.pedida, n.zona));
      f.solicitudes += 1;
      f.telefonos.add(s.dispositivo);
      for (const v of s.votos) {
        if (v.en >= n.fin) continue;
        const fv = franja(horaLocal(v.en, n.zona));
        fv.votos += 1;
        fv.telefonos.add(v.dispositivo);
      }
    }
  }
  // La noche empieza por la tarde: las horas se ordenan desde el mediodía.
  const desdeMediodia = (h: number) => (h + 12) % 24;
  return {
    datos: origenDatos(todas, noches),
    generos: [...generos.entries()]
      .map(([g, x]) => ({ genero: g === 'sin_clasificar' ? null : g, ...x }))
      .sort((a, b) => (a.genero === null ? 1 : b.genero === null ? -1 : b.solicitudes - a.solicitudes)),
    franjas: [...franjas.entries()]
      .map(([hora, f]) => ({ hora, solicitudes: f.solicitudes, votos: f.votos, telefonos: f.telefonos.size }))
      .sort((a, b) => desdeMediodia(a.hora) - desdeMediodia(b.hora)),
  };
}

// ── RF-052 ─────────────────────────────────────────────────────────────────

/** Qué parte de los votos de una noche dio el 10 % de los teléfonos que más votaron. */
function concentracionDe(n: NocheA) {
  const porTelefono = new Map<string, number>();
  for (const s of n.solicitudes) for (const v of s.votos) porTelefono.set(v.dispositivo, (porTelefono.get(v.dispositivo) ?? 0) + 1);
  const conteos = [...porTelefono.values()].sort((a, b) => b - a);
  const votos = sumar(conteos);
  const top = Math.max(1, Math.ceil(conteos.length * 0.1));
  return { telefonos: conteos.length, votos, pct: votos > 0 ? Math.round((sumar(conteos.slice(0, top)) / votos) * 100) : 0 };
}

export function diagnostico(todas: NocheA[], noches: NocheA[], umbrales: UmbralesAnalitica): DiagnosticoAnalitica {
  // Por noche: en un rango, los habituales de muchas noches inflarían la concentración.
  const porNoche = noches.map((n) => ({ n, c: concentracionDe(n) }));
  return {
    datos: origenDatos(todas, noches),
    umbrales,
    no_sonaron: noches
      .flatMap((n) => n.solicitudes.filter((s) => s.estado !== 'sono' && s.votos.length >= umbrales.votos_minimos).map((s) => ({ s, n })))
      .sort((a, b) => b.s.votos.length - a.s.votos.length || b.n.inicio - a.n.inicio)
      .slice(0, 25)
      .map(({ s, n }) => ({ solicitud_id: s.id, cancion: contrato(s.video_id), votos: s.votos.length, noche_id: n.noche.id, noche_inicio: n.noche.inicio, causa: s.estado as Causa })),
    concentracion: {
      telefonos: telefonosDe(noches),
      votos: sumar(porNoche.map((x) => x.c.votos)),
      pct_votos_del_10: porNoche.length > 0 ? Math.round(sumar(porNoche.map((x) => x.c.pct)) / porNoche.length) : 0,
    },
    noches: porNoche.map(({ n, c }) => ({ noche_id: n.noche.id, inicio: n.noche.inicio, telefonos: telefonosDe([n]), solicitudes: n.solicitudes.length, pct_votos_del_10: c.pct })),
  };
}

// ── RF-053 ─────────────────────────────────────────────────────────────────

const DIAS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
const TIPOS: Record<string, string> = { normal: 'Noche normal', karaoke: 'Karaoke', partido: 'Partido', tematica: 'Temática', otro: 'Otro' };

export function comparar(todas: NocheA[], noches: NocheA[], agrupar: 'noche' | 'dia' | 'evento'): ComparacionAnalitica {
  const grupos = new Map<string, { etiqueta: string; orden: number; noches: NocheA[] }>();
  for (const n of noches) {
    const [clave, etiqueta, orden] =
      agrupar === 'noche'
        ? [n.noche.id, etiquetaNoche(n), n.inicio]
        : agrupar === 'dia'
          ? [String(n.noche.dia_semana), DIAS[n.noche.dia_semana]!, n.noche.dia_semana]
          : [n.noche.evento?.tipo_evento ?? 'normal', TIPOS[n.noche.evento?.tipo_evento ?? 'normal']!, Object.keys(TIPOS).indexOf(n.noche.evento?.tipo_evento ?? 'normal')];
    const g = grupos.get(clave) ?? { etiqueta, orden, noches: [] };
    g.noches.push(n);
    grupos.set(clave, g);
  }
  const promedio = (xs: number[], k: number) => Math.round((sumar(xs) / k) * 10) / 10;
  return {
    datos: origenDatos(todas, noches),
    agrupar,
    grupos: [...grupos.entries()]
      .sort(([, a], [, b]) => a.orden - b.orden)
      .map(([clave, g]) => {
        const k = g.noches.length;
        const e = esperas(g.noches);
        return {
          clave,
          etiqueta: g.etiqueta,
          noches: k,
          solicitudes: promedio(g.noches.map((n) => n.solicitudes.length), k),
          telefonos: promedio(g.noches.map((n) => telefonosDe([n])), k),
          votos: promedio(g.noches.map((n) => sumar(n.solicitudes.map((s) => s.votos.length))), k),
          espera_promedio_seg: e.length > 0 ? Math.round(sumar(e) / e.length) : null,
          pedidos_seg: promedio(g.noches.map((n) => n.pedidos_seg), k),
          lista_casa_seg: promedio(g.noches.map((n) => n.casa_seg), k),
        };
      }),
  };
}

function etiquetaNoche(n: NocheA): string {
  const { dia, mes } = enZona(n.inicio, n.zona);
  const nombre = DIAS[n.noche.dia_semana]!.slice(0, 3).toLowerCase();
  return `${nombre} ${dia} ${['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'][mes - 1]}`;
}

// ── RF-054 ─────────────────────────────────────────────────────────────────

const NOMBRE_GENERO: Record<string, string> = { salsa: 'salsa', urbano: 'urbano', pop: 'pop', tropical: 'tropical', vallenato: 'vallenato' };
const pct = (parte: number, todo: number) => (todo > 0 ? Math.round((parte / todo) * 100) : 0);

/** Sugerencias con el dato en que se basan. Ninguna cambia nada: llevan a donde se decide. */
export function recomendaciones(estado: EstadoSim, noches: NocheA[], ahora: number): Recomendacion[] {
  const salida: Recomendacion[] = [];
  const activa = estado.listas.find((l) => l.activa) ?? null;
  const enLista = new Set(activa?.elementos.map((e) => e.video_id) ?? []);
  const solicitudes = noches.flatMap((n) => n.solicitudes.map((s) => ({ s, n })));
  if (solicitudes.length === 0) return salida;

  // Las más pedidas que la lista de la casa no tiene.
  const porCancion = new Map<string, { veces: number; noches: Set<string> }>();
  for (const { s, n } of solicitudes) {
    const x = porCancion.get(s.video_id) ?? { veces: 0, noches: new Set() };
    x.veces += 1;
    x.noches.add(n.noche.id);
    porCancion.set(s.video_id, x);
  }
  // Solo los bloqueos vigentes ahora: uno vencido ya no impide sugerir la canción.
  const vigentes = estado.bloqueos.filter((b) => bloqueoVigente(estado, b.video_id, ahora)?.id === b.id);
  const bloqueadas = new Set(vigentes.map((b) => b.video_id));
  const fuera = [...porCancion.entries()].filter(([id, x]) => !enLista.has(id) && !bloqueadas.has(id) && x.veces >= 3).sort(([, a], [, b]) => b.veces - a.veces);
  for (const [id, x] of fuera.slice(0, 2)) {
    const c = CATALOGO[id]!;
    salida.push({
      id: `agregar-${id}`,
      tipo: 'agregar_a_lista',
      titulo: `Agregar «${c.titulo}», de ${c.artista}, a la lista de la casa`,
      base: `La pidieron ${x.veces} veces en ${x.noches.size} ${x.noches.size === 1 ? 'noche' : 'noches'} y no está en «${activa?.nombre ?? 'la lista activa'}».`,
      destino: 'listas',
      lista_id: activa?.id ?? null,
    });
  }

  // El género que más se pide frente a lo que hay en la lista.
  if (activa && activa.elementos.length > 0) {
    const porGenero = new Map<string, number>();
    for (const { s } of solicitudes) porGenero.set(CATALOGO[s.video_id]!.genero, (porGenero.get(CATALOGO[s.video_id]!.genero) ?? 0) + 1);
    const enListaPorGenero = (g: string) => activa.elementos.filter((e) => CATALOGO[e.video_id]?.genero === g).length;
    const candidato = [...porGenero.entries()]
      .filter(([g]) => g in NOMBRE_GENERO)
      .map(([g, n]) => ({ g, pedido: pct(n, solicitudes.length), lista: pct(enListaPorGenero(g), activa.elementos.length) }))
      .filter((x) => x.pedido >= 15 && x.lista < x.pedido / 2)
      .sort((a, b) => b.pedido - b.lista - (a.pedido - a.lista))[0];
    if (candidato) {
      salida.push({
        id: `genero-${candidato.g}`,
        tipo: 'genero_en_lista',
        titulo: `Sumar canciones de ${NOMBRE_GENERO[candidato.g]} a la lista de la casa`,
        base: `El ${candidato.pedido} % de los pedidos son de ${NOMBRE_GENERO[candidato.g]}; en «${activa.nombre}» es el ${candidato.lista} %.`,
        destino: 'listas',
        lista_id: activa.id,
      });
    }
  }

  // Un bloqueo vigente sobre algo que el público quería.
  for (const b of vigentes.filter((x) => x.duracion === 'indefinido')) {
    const veces = solicitudes.filter(({ s }) => s.video_id === b.video_id);
    const votos = sumar(veces.map(({ s }) => s.votos.length));
    if (veces.length >= 4) {
      const c = CATALOGO[b.video_id]!;
      salida.push({
        id: `bloqueo-${b.video_id}`,
        tipo: 'revisar_bloqueo',
        titulo: `Revisar si «${c.titulo}» debe seguir bloqueada`,
        base: `En estas noches la pidieron ${veces.length} veces y sumó ${votos} votos; está bloqueada hasta que alguien lo levante.`,
        destino: 'bloqueos',
        lista_id: null,
      });
      break;
    }
  }

  // El cierre: lo que llega en la última hora y se queda sin sonar.
  for (const dia of [6, 5]) {
    const delDia = noches.filter((n) => n.noche.dia_semana === dia && !n.noche.evento);
    if (delDia.length === 0) continue;
    const pedidas = delDia.flatMap((n) => n.solicitudes.map((s) => ({ s, n })));
    const ultimaHora = pedidas.filter(({ s, n }) => n.fin - s.pedida <= HORA);
    const sinSonar = pedidas.filter(({ s }) => s.estado === 'fuera_de_tiempo').length;
    const parte = pct(ultimaHora.length, pedidas.length);
    if (parte >= 12 && sinSonar >= 5) {
      const nombre = DIAS[dia]!.toLowerCase();
      salida.push({
        id: `horario-${dia}`,
        tipo: 'ajustar_horario',
        titulo: `Pensar en cerrar más tarde los ${nombre === 'sábado' ? 'sábados' : 'viernes'}`,
        base: `Los ${nombre === 'sábado' ? 'sábados' : 'viernes'}, en la última hora llega el ${parte} % de los pedidos y ${sinSonar} se quedaron sin sonar al cerrar (${delDia.length} ${delDia.length === 1 ? 'noche' : 'noches'}).`,
        destino: 'horario',
        lista_id: null,
      });
      break;
    }
  }
  return salida;
}
