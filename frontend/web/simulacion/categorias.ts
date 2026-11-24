import type { CancionCatalogo, CategoriaCorregida, OrigenCategoria } from '@rockola/contrato';
import { nombreGenero } from '../lib/formato';
import { anotar } from './gestion';
import { cancion, normalizar, type Personal, type Resultado } from './motor';
import type { EstadoSim } from './tipos';

/**
 * El género de las canciones en la simulación (RF-068). La clasificación automática se imita
 * con el género del catálogo; la corrección del panel queda en `estado.categorias` y gana
 * siempre, como `origen_categoria = 'MANUAL'` en el backend.
 */

/**
 * Los géneros que el panel puede asignar: los nombres de `ClasificadorGeneroService.generos()`
 * del backend (rokolaBackend/.../cancion/ClasificadorGeneroService.kt, 2026-11-24).
 */
export const GENEROS_CATALOGO = [
  'Salsa', 'Urbano', 'Pop', 'Tropical', 'Vallenato', 'Bachata', 'Rock', 'Electrónica', 'Merengue', 'Cumbia',
  'Champeta', 'Porro', 'Música Popular', 'Música Llanera', 'Música Andina', 'Folclor Colombiano', 'Bolero', 'Balada',
  'Tango', 'Samba', 'Bossa Nova', 'Forró', 'Zouk', 'Kizomba', 'Regional Mexicano', 'Banda', 'Norteño', 'Corridos',
  'Ranchera', 'Grupera', 'Reggae', 'Dancehall', 'Hip Hop', 'R&B', 'Soul', 'Funk', 'Disco', 'Jazz', 'Blues', 'Country',
  'Folk', 'Afrobeats', 'Amapiano', 'K-Pop', 'J-Pop', 'Metal', 'Punk', 'Indie', 'Góspel', 'Música Cristiana',
  'Música Clásica', 'Instrumental', 'Lo-Fi', 'Phonk', 'House', 'Techno', 'Trance', 'Dubstep', 'Drum and Bass',
  'Dembow', 'Flamenco',
];

const AUTOMATICOS: OrigenCategoria[] = ['ETIQUETAS', 'ETIQUETAS', 'ETIQUETAS', 'TITULO', 'DESCRIPCION'];

function huella(texto: string): number {
  let h = 2166136261;
  for (const c of texto) h = Math.imul(h ^ c.charCodeAt(0), 16777619);
  return h >>> 0;
}

/**
 * El género de una canción y quién se lo puso. Una de cada siete que el catálogo sí clasifica
 * queda sin género, como pasa con los videos de YouTube sin etiquetas útiles: así el filtro
 * «Sin género» tiene qué mostrar.
 */
export function categoriaDe(estado: EstadoSim, videoId: string): { categoria: string | null; origen: OrigenCategoria } {
  const manual = estado.categorias?.[videoId];
  if (manual) return { categoria: manual, origen: 'MANUAL' };
  const genero = cancion(estado, videoId).genero;
  const h = huella(videoId);
  if (genero === 'sin_clasificar' || h % 7 === 0) return { categoria: null, origen: null };
  return { categoria: nombreGenero(genero), origen: AUTOMATICOS[h % AUTOMATICOS.length]! };
}

/** Las canciones que el bar usó: pedidas, en una lista de la casa, bloqueadas o que sonaron. */
function cancionesDelBar(estado: EstadoSim, pedidasAntes: Map<string, number>): Map<string, number> {
  const pedidos = new Map(pedidasAntes);
  for (const s of estado.solicitudes) pedidos.set(s.video_id, (pedidos.get(s.video_id) ?? 0) + 1);
  const usar = (v: string) => pedidos.set(v, pedidos.get(v) ?? 0);
  for (const l of estado.listas) for (const e of l.elementos) usar(e.video_id);
  for (const b of estado.bloqueos) usar(b.video_id);
  for (const h of estado.historial) usar(h.video_id);
  return pedidos;
}

export type FiltroCatalogo = { soloSinCategoria: boolean; q: string; limite: number };

/** Igual que el backend: primero las que no tienen género, luego las más pedidas. */
export function listarCatalogo(estado: EstadoSim, pedidasAntes: Map<string, number>, f: FiltroCatalogo): CancionCatalogo[] {
  const q = normalizar(f.q.slice(0, 100));
  const filas: CancionCatalogo[] = [];
  for (const [video, pedidos] of cancionesDelBar(estado, pedidasAntes)) {
    const c = cancion(estado, video);
    const { categoria, origen } = categoriaDe(estado, video);
    if (f.soloSinCategoria && categoria !== null) continue;
    if (q && !normalizar(`${c.titulo} ${c.artista}`).includes(q)) continue;
    filas.push({ id: video, youtube_video_id: video, titulo: c.titulo, artista: c.artista || null, categoria, origen_categoria: origen, pedidos });
  }
  filas.sort((a, b) => Number(a.categoria !== null) - Number(b.categoria !== null) || b.pedidos - a.pedidos || a.titulo.localeCompare(b.titulo, 'es'));
  return filas.slice(0, Math.min(100, Math.max(1, f.limite)));
}

export function corregirCategoria(estado: EstadoSim, pedidasAntes: Map<string, number>, ahora: number, quien: Personal, id: string, ingresada: unknown): Resultado<CategoriaCorregida> {
  const buscada = typeof ingresada === 'string' ? normalizar(ingresada) : '';
  const categoria = GENEROS_CATALOGO.find((g) => normalizar(g) === buscada);
  if (!categoria) return { ok: false, estado: 400, codigo: 'datos_invalidos', mensaje: 'Selecciona un género válido del catálogo' };
  if (!cancionesDelBar(estado, pedidasAntes).has(id)) return { ok: false, estado: 404, codigo: 'no_encontrado', mensaje: 'Canción no encontrada en el bar' };
  estado.categorias = { ...estado.categorias, [id]: categoria };
  const c = cancion(estado, id);
  anotar(estado, ahora, quien, 'editar_categoria', { tipo: 'cancion', nombre: c.titulo, detalle: c.artista || null }, `Asignó género: ${categoria}`);
  return { ok: true, estado: 200, cuerpo: { id, categoria, origen_categoria: 'MANUAL' } };
}
