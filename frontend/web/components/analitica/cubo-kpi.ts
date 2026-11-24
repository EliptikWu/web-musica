import { type CeldaCubo, type CuboAnalitica, type FuenteCubo, TODOS_CUBO } from '@rockola/contrato';

/** Lógica del cubo de KPI (RF-063), sin React: qué se puede leer y qué no. */

export type Dim = 'genero' | 'franja' | 'tercero';
export const DIMS: Dim[] = ['genero', 'franja', 'tercero'];
export type Seleccion = Record<Dim, string[]>;
export const SIN_SELECCION: Seleccion = { genero: [], franja: [], tercero: [] };

export type ClaveKpi = 'solicitudes' | 'votos' | 'telefonos' | 'popular' | 'espera_promedio' | 'espera_maxima' | 'reproduccion' | 'lista_casa' | 'no_sonaron';

const entero = new Intl.NumberFormat('es-CO');
const minutos = (seg: number) => `${entero.format(Math.round(seg / 60))} min`;

export type Kpi = {
  clave: ClaveKpi;
  nombre: string;
  /** Lo que se lee de una celda ya calculada por el pipeline. */
  valor: (c: CeldaCubo) => number | null;
  formato: (n: number) => string;
  /** Cómo se combinan varias celdas en un dice; `null`: no se puede sin volver a los hechos. */
  combinar: ((celdas: CeldaCubo[]) => number | null) | null;
};

const suma = (f: (c: CeldaCubo) => number) => (celdas: CeldaCubo[]) => celdas.reduce((s, c) => s + f(c), 0);
const partePct = (casa: number, pedidos: number) => (casa + pedidos > 0 ? Math.round((casa / (casa + pedidos)) * 100) : null);

export const KPIS: Kpi[] = [
  { clave: 'solicitudes', nombre: 'Solicitudes', valor: (c) => c.solicitudes, formato: (n) => entero.format(n), combinar: suma((c) => c.solicitudes) },
  { clave: 'votos', nombre: 'Votos', valor: (c) => c.votos, formato: (n) => entero.format(n), combinar: suma((c) => c.votos) },
  { clave: 'telefonos', nombre: 'Usuarios únicos', valor: (c) => c.telefonos, formato: (n) => entero.format(n), combinar: null },
  { clave: 'popular', nombre: 'Votos de la más popular', valor: (c) => c.popular?.votos ?? null, formato: (n) => entero.format(n), combinar: null },
  { clave: 'espera_promedio', nombre: 'Espera promedio', valor: (c) => c.espera_promedio_seg, formato: minutos, combinar: null },
  {
    clave: 'espera_maxima', nombre: 'Espera máxima', valor: (c) => c.espera_maxima_seg, formato: minutos,
    combinar: (celdas) => {
      const v = celdas.map((c) => c.espera_maxima_seg).filter((x): x is number => x !== null);
      return v.length > 0 ? Math.max(...v) : null;
    },
  },
  // RF-067: lo que sonó en la celda, pedido o de la lista de la casa.
  { clave: 'reproduccion', nombre: 'Tiempo de reproducción', valor: (c) => c.pedidos_seg + c.lista_casa_seg, formato: minutos, combinar: suma((c) => c.pedidos_seg + c.lista_casa_seg) },
  {
    clave: 'lista_casa', nombre: 'Lista de la casa', valor: (c) => partePct(c.lista_casa_seg, c.pedidos_seg), formato: (n) => `${n} %`,
    combinar: (celdas) => partePct(suma((c) => c.lista_casa_seg)(celdas), suma((c) => c.pedidos_seg)(celdas)),
  },
  { clave: 'no_sonaron', nombre: 'No sonaron', valor: (c) => c.no_sonaron, formato: (n) => entero.format(n), combinar: suma((c) => c.no_sonaron) },
];

/**
 * Cuánto ámbar lleva una celda según su peso (0 a 1). Entre 41 % y 62 % de ámbar sobre la madera
 * ni la crema ni la tinta llegan a 4,5:1 (medido, doc 05): la escala salta esa franja.
 */
export function luzDe(peso: number): { brillo: number; encendido: boolean } {
  const p = Math.max(0, Math.min(1, peso));
  return p <= 0.5 ? { brillo: (p / 0.5) * 0.4, encendido: false } : { brillo: 0.63 + ((p - 0.5) / 0.5) * 0.37, encendido: true };
}

export const claveCelda =(g: string, f: string, t: string) => `${g}|${f}|${t}`;
export const indiceCeldas = (celdas: CeldaCubo[]) => new Map(celdas.map((c) => [claveCelda(c.genero, c.franja, c.tercero), c]));

const miembrosDe = (lista: string[]) => (lista.length > 0 ? lista : [TODOS_CUBO]);

/** Las celdas que cubre una coordenada: sin miembros en un eje, su «Todos». */
export function celdasDe(indice: Map<string, CeldaCubo>, s: Seleccion): CeldaCubo[] {
  const salida: CeldaCubo[] = [];
  for (const g of miembrosDe(s.genero)) for (const f of miembrosDe(s.franja)) for (const t of miembrosDe(s.tercero)) {
    const c = indice.get(claveCelda(g, f, t));
    if (c) salida.push(c);
  }
  return salida;
}

/** Ninguna celda con hechos: no es lo mismo que un 0, que es una celda con hechos que suma cero. */
export const SIN_DATOS = 'sin-datos';
export type Lectura = number | null | typeof SIN_DATOS;

/**
 * El valor de un KPI en una coordenada. Con un solo miembro por eje es la celda calculada.
 * Con varios (dice), solo los KPI que se pueden combinar: sumar usuarios únicos o promedios
 * daría una cifra falsa, y entonces devuelve `null` (no aplica). Si ninguna celda tiene
 * hechos, `SIN_DATOS`: el backend no manda las combinaciones vacías.
 */
export function leerKpi(indice: Map<string, CeldaCubo>, kpi: Kpi, s: Seleccion): Lectura {
  const unica = DIMS.every((d) => s[d].length <= 1);
  const celdas = celdasDe(indice, s);
  if (celdas.length === 0) return SIN_DATOS;
  if (unica) return kpi.valor(celdas[0]!);
  return kpi.combinar ? kpi.combinar(celdas) : null;
}

/** Cuántos géneros se ven de entrada: con más, el cubo deja de leerse (RF-067). */
export const GENEROS_A_LA_VISTA = 5;

/**
 * Qué miembros dibuja el cubo en cada eje. Con filtro, solo los elegidos. Sin filtro, todos,
 * salvo en género: los `GENEROS_A_LA_VISTA` con más pedidos y votos, en el orden de la API,
 * a menos que se pidan todos. La tabla de cruce sigue mostrando todos.
 */
export function miembrosVisibles(datos: Pick<CuboAnalitica, 'miembros' | 'celdas'>, s: Seleccion, todosLosGeneros: boolean): Record<Dim, string[]> {
  const ejes = {} as Record<Dim, string[]>;
  for (const d of DIMS) ejes[d] = s[d].length > 0 ? datos.miembros[d].filter((m) => s[d].includes(m)) : datos.miembros[d];
  if (s.genero.length === 0 && !todosLosGeneros && datos.miembros.genero.length > GENEROS_A_LA_VISTA) {
    const actividad = new Map(
      datos.celdas.filter((c) => c.franja === TODOS_CUBO && c.tercero === TODOS_CUBO && c.genero !== TODOS_CUBO).map((c) => [c.genero, c.solicitudes + c.votos]),
    );
    const mas = new Set([...datos.miembros.genero].sort((a, b) => (actividad.get(b) ?? 0) - (actividad.get(a) ?? 0)).slice(0, GENEROS_A_LA_VISTA));
    ejes.genero = datos.miembros.genero.filter((g) => mas.has(g));
  }
  return ejes;
}

export type Operacion = { nombre: string; detalle: string };

export function operacion(s: Seleccion, fuente: FuenteCubo = 'historico'): Operacion {
  if (DIMS.some((d) => s[d].length > 1)) return { nombre: 'Dice', detalle: 'Un bloque del cubo: varios valores en uno o más ejes.' };
  const fijos = DIMS.filter((d) => s[d].length === 1).length;
  if (fijos === 0) {
    const noches = fuente === 'vivo' ? 'Todas las noches, también la que está abierta,' : 'Todas las noches que cargó el pipeline,';
    return { nombre: 'Cubo completo', detalle: `${noches} sin recortar ningún eje.` };
  }
  if (fijos === 1) return { nombre: 'Slice', detalle: 'Una capa del cubo: un eje queda fijo.' };
  if (fijos === 2) return { nombre: 'Fila', detalle: 'El cruce de dos capas: queda una línea de celdas.' };
  return { nombre: 'Celda', detalle: 'Los tres ejes fijos: un solo punto del cubo.' };
}

/** En qué orden se fueron fijando los ejes: conserva los que siguen y agrega los nuevos al final. */
export function ordenEjes(orden: Dim[], s: Seleccion): Dim[] {
  const siguen = orden.filter((d) => s[d].length > 0);
  return [...siguen, ...DIMS.filter((d) => s[d].length > 0 && !siguen.includes(d))];
}

/** Roll-up: suelta el último eje fijado, para que deshaga el drill-down anterior. */
export function rollUp(s: Seleccion, orden: Dim[] = DIMS): Seleccion {
  const d = [...ordenEjes(orden, s)].pop();
  return d ? { ...s, [d]: [] } : s;
}

/** Drill-down: baja un nivel en el primer eje libre, a su primer miembro. */
export function drillDown(s: Seleccion, miembros: Record<Dim, string[]>): Seleccion {
  const d = DIMS.find((x) => s[x].length === 0 && miembros[x].length > 0);
  return d ? { ...s, [d]: [miembros[d]![0]!] } : s;
}

export function alternar(s: Seleccion, d: Dim, miembro: string): Seleccion {
  return { ...s, [d]: s[d].includes(miembro) ? s[d].filter((m) => m !== miembro) : [...s[d], miembro] };
}

export function dentro(s: Seleccion, g: string, f: string, t: string): boolean {
  const en = (d: Dim, m: string) => s[d].length === 0 || s[d].includes(m);
  return en('genero', g) && en('franja', f) && en('tercero', t);
}

/** Qué ejes van en filas y columnas: primero los que no están fijos en un solo miembro. */
export function ejesDeTabla(s: Seleccion, pivot: boolean): { filas: Dim; columnas: Dim; resto: Dim } {
  const orden = [...DIMS].sort((a, b) => Number(s[a].length === 1) - Number(s[b].length === 1));
  const [filas, columnas, resto] = orden as [Dim, Dim, Dim];
  return pivot ? { filas: columnas, columnas: filas, resto } : { filas, columnas, resto };
}
