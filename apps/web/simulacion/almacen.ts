import { idDispositivo } from '../lib/dispositivo';
import { crearEstado, type Escenario, esEscenario } from './datos/semilla';
import { ahoraSim, avanzar } from './motor';
import type { EstadoSim } from './tipos';

/**
 * Dónde vive la simulación: un JSON en localStorage, compartido por todas las pestañas del
 * navegador. Cada cambio se hace con un candado (`navigator.locks`) y se anuncia por
 * `BroadcastChannel`, así cliente, TV y panel abiertos en pestañas se ven en vivo (doc 03).
 */
const CLAVE = 'rockola.sim.v1';
const CANAL = 'rockola.sim';

const oyentes = new Set<() => void>();
let canal: BroadcastChannel | null = null;

function abrirCanal() {
  if (canal || typeof BroadcastChannel === 'undefined') return;
  canal = new BroadcastChannel(CANAL);
  canal.onmessage = () => avisar();
}

function avisar() {
  for (const oyente of oyentes) oyente();
}

/** Avisa cuando la simulación cambia, en esta pestaña o en otra. */
export function alCambiar(oyente: () => void): () => void {
  abrirCanal();
  oyentes.add(oyente);
  return () => oyentes.delete(oyente);
}

function leer(): EstadoSim | null {
  try {
    const texto = localStorage.getItem(CLAVE);
    if (!texto) return null;
    const estado = JSON.parse(texto) as EstadoSim;
    return estado.esquema === 1 ? estado : null;
  } catch (error) {
    console.error('[simulación] estado ilegible; se vuelve a sembrar', error);
    return null;
  }
}

const MAXIMO_HISTORIAL = 400;
const MAXIMO_ACCIONES = 400;

/**
 * Recorta lo que crece con el tiempo: idempotencia de más de 24 h, historial, bitácora y
 * solicitudes ya terminadas. Lo vigente (pendientes y la que suena) no se toca.
 */
export function recortar(estado: EstadoSim, ahoraReal: number, proporcion = 1): void {
  const limite = ahoraReal - 24 * 3600_000;
  for (const [clave, valor] of Object.entries(estado.idempotencia)) if (valor.en < limite) delete estado.idempotencia[clave];
  const tope = (n: number) => Math.ceil(n * proporcion);
  estado.historial = estado.historial.slice(-tope(MAXIMO_HISTORIAL));
  estado.acciones = estado.acciones.slice(-tope(MAXIMO_ACCIONES));
  const terminadas = estado.solicitudes.filter((s) => s.estado !== 'pendiente' && s.estado !== 'reproduciendo');
  if (terminadas.length > tope(MAXIMO_HISTORIAL)) {
    const quitar = new Set(terminadas.slice(0, terminadas.length - tope(MAXIMO_HISTORIAL)).map((s) => s.id));
    estado.solicitudes = estado.solicitudes.filter((s) => !quitar.has(s.id));
  }
}

function guardar(estado: EstadoSim) {
  recortar(estado, Date.now());
  try {
    localStorage.setItem(CLAVE, JSON.stringify(estado));
  } catch (error) {
    // Almacenamiento lleno: se recorta a la cuarta parte y se intenta una vez más.
    console.warn('[simulación] almacenamiento lleno; se recorta el historial', error);
    recortar(estado, Date.now(), 0.25);
    localStorage.setItem(CLAVE, JSON.stringify(estado));
  }
}

async function conCandado<T>(fn: () => T): Promise<T> {
  if (typeof navigator !== 'undefined' && navigator.locks) return navigator.locks.request('rockola.sim.almacen', async () => fn());
  return fn();
}

/** Prepara el escenario pedido en la URL (`?escenario=…&reloj=fijo|rapido`) o conserva el guardado. */
export async function preparar(parametros: URLSearchParams): Promise<void> {
  const pedido = parametros.get('escenario');
  const reloj = parametros.get('reloj');
  const velocidad = reloj === 'fijo' ? 0 : reloj === 'rapido' ? 10 : 1;
  await conCandado(() => {
    const actual = leer();
    const reiniciar = parametros.has('reiniciar');
    if (actual && !reiniciar && (!esEscenario(pedido) || actual.escenario === pedido) && (reloj === null || actual.reloj.velocidad === velocidad)) return;
    const escenario: Escenario = esEscenario(pedido) ? pedido : (actual?.escenario as Escenario | undefined) ?? 'momento-b';
    const nuevo = crearEstado(escenario, idDispositivo(), Date.now(), velocidad);
    nuevo.version = (actual?.version ?? 0) + 1;
    guardar(nuevo);
    canal?.postMessage({ version: nuevo.version });
  });
  abrirCanal();
  avisar();
}

export async function reiniciar(escenario: Escenario, velocidad = 1): Promise<void> {
  const parametros = new URLSearchParams({ escenario, reiniciar: '1' });
  if (velocidad === 0) parametros.set('reloj', 'fijo');
  if (velocidad === 10) parametros.set('reloj', 'rapido');
  await preparar(parametros);
}

/**
 * Corre `fn` con el estado al día: primero se avanza el reloj y después se aplica el cambio.
 * Si algo cambió, se guarda y se avisa a las demás pestañas.
 */
export async function conEstado<T>(fn: (estado: EstadoSim, ahora: number) => T): Promise<T> {
  let cambio = false;
  const resultado = await conCandado(() => {
    const estado = leer() ?? crearEstado('momento-b', idDispositivo(), Date.now());
    const antes = estado.version;
    const ahora = ahoraSim(estado, Date.now());
    avanzar(estado, ahora);
    const r = fn(estado, ahora);
    if (estado.version !== antes || !localStorage.getItem(CLAVE)) {
      guardar(estado);
      cambio = true;
    }
    return r;
  });
  if (cambio) {
    canal?.postMessage({ changed: true });
    avisar();
  }
  return resultado;
}

/** Lectura sin cambios (no toma el candado si no hace falta avanzar). */
export function versionGuardada(): number {
  return leer()?.version ?? 0;
}
