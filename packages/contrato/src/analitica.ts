import * as z from 'zod/mini';
import { Cancion } from './cancion.ts';
import { OrigenReproduccion } from './cola.ts';
import { Fecha, Id, Segundos } from './comunes.ts';
import { TipoEvento } from './panel.ts';

/**
 * Tablero analítico (RF-049 a RF-054; PB-45 a PB-49). *Propuesta para el backend*: el
 * pipeline (RF-047, RF-048) carga cada noche al cerrarse; el panel solo consulta.
 */

/** De dónde salen las cifras y de cuándo son (RNF-015). */
export const OrigenDatos = z
  .object({
    origen: z.enum(['registrado', 'ejemplo']).check(z.describe('`ejemplo`: datos inventados de la simulación; la interfaz lo rotula')),
    actualizado_en: z.nullable(Fecha).check(z.describe('Última carga del pipeline; `null` si todavía no ha corrido')),
    noches: z.int().check(z.nonnegative()).check(z.describe('Noches que entran en el cálculo')),
    desde: z.nullable(Fecha).check(z.describe('Inicio de la primera noche')),
    hasta: z.nullable(Fecha).check(z.describe('Inicio de la última noche (su fecha es la de la noche)')),
    fuente: z.optional(z.enum(['operativa', 'pipeline'])).check(z.describe('Solo el cubo en vivo (RF-067): `operativa` = calculado ahora desde las tablas de la operación')),
    en_vivo: z.optional(z.boolean()).check(z.describe('`true` en el cubo en vivo: incluye la noche abierta y `actualizado_en` es la hora de la consulta')),
    sesiones_abiertas: z.optional(z.int().check(z.nonnegative())).check(z.describe('Solo en vivo: noches abiertas ahora mismo')),
  }).register(z.globalRegistry, { id: 'OrigenDatos' });
export type OrigenDatos = z.infer<typeof OrigenDatos>;

/** Qué se consulta: una noche, o un rango de fechas (`YYYY-MM-DD`, en la zona del bar). */
export const FiltroAnalitica = z.object({
  noche: z.optional(z.string()),
  desde: z.optional(z.string()),
  hasta: z.optional(z.string()),
  /** 1 = lunes … 7 = domingo (RF-051). */
  dia_semana: z.optional(z.string()),
});

/** Una noche cerrada: la sesión de servicio, con su evento del calendario si lo hubo. */
export const NocheAnalitica = z
  .object({
    id: Id,
    inicio: Fecha,
    fin: Fecha,
    dia_semana: z.int().check(z.minimum(1)).check(z.maximum(7)),
    evento: z.nullable(z.object({ id: Id, nombre: z.string(), tipo_evento: TipoEvento })),
  }).register(z.globalRegistry, { id: 'NocheAnalitica' });
export type NocheAnalitica = z.infer<typeof NocheAnalitica>;

export const ListaNoches = z.object({ datos: OrigenDatos, noches: z.array(NocheAnalitica) }).register(z.globalRegistry, { id: 'ListaNoches' });

/** RF-049 y RF-050: lo que pasó en la noche (o el rango) elegido. */
export const ResumenAnalitica = z
  .object({
    datos: OrigenDatos,
    solicitudes: z.int().check(z.nonnegative()),
    telefonos: z.int().check(z.nonnegative()).check(z.describe('Dispositivos únicos (anonimizados en el modelo, RF-048)')),
    votos: z.int().check(z.nonnegative()),
    mas_popular: z.nullable(z.object({ cancion: Cancion, votos: z.int().check(z.nonnegative()), solicitudes: z.int().check(z.nonnegative()) })),
    espera: z.nullable(z.object({ promedio_seg: Segundos, maxima_seg: Segundos })).check(z.describe('De la solicitud a que empezó a sonar')),
    tiempo: z.object({ pedidos_seg: Segundos, lista_casa_seg: Segundos }).register(z.globalRegistry, { description: 'Tiempo que sonó cada origen' }),
    no_sonaron: z.int().check(z.nonnegative()).check(z.describe('Solicitudes que nunca llegaron a reproducirse')),
    /** Solo cuando se consulta una noche: lo que sonó, en orden. */
    historial: z.array(z.object({ inicio: Fecha, cancion: Cancion, origen: OrigenReproduccion, votos: z.int().check(z.nonnegative()) })),
  }).register(z.globalRegistry, { id: 'ResumenAnalitica' });
export type ResumenAnalitica = z.infer<typeof ResumenAnalitica>;

/** RF-051: géneros y franjas horarias. */
export const ParticipacionAnalitica = z
  .object({
    datos: OrigenDatos,
    generos: z.array(z.object({ genero: z.nullable(z.string()).check(z.describe('`null`: sin clasificar')), solicitudes: z.int().check(z.nonnegative()), votos: z.int().check(z.nonnegative()) })),
    franjas: z.array(
      z.object({
        hora: z.int().check(z.minimum(0)).check(z.maximum(23)),
        solicitudes: z.int().check(z.nonnegative()),
        votos: z.int().check(z.nonnegative()),
        telefonos: z.int().check(z.nonnegative()),
        espera_promedio_seg: z.nullable(Segundos).check(z.describe('Espera promedio de lo pedido a esa hora hasta que sonó; `null` si nada de esa hora sonó (2026-11-25)')),
      }),
    ),
    mas_pedidas: z
      .array(z.object({ cancion: Cancion, solicitudes: z.int().check(z.positive()), votos: z.int().check(z.nonnegative()) })).check(z.maxLength(10)).check(z.describe('Las 10 canciones más pedidas, de más a menos; en empate, más votos (2026-11-25)')),
    artistas: z
      .array(z.object({ artista: z.string(), solicitudes: z.int().check(z.positive()), votos: z.int().check(z.nonnegative()) })).check(z.maxLength(10)).check(z.describe('Los 10 artistas más pedidos, igual que `mas_pedidas` (2026-11-25)')),
  }).register(z.globalRegistry, { id: 'ParticipacionAnalitica' });
export type ParticipacionAnalitica = z.infer<typeof ParticipacionAnalitica>;

/** RF-052: umbrales documentados y configurables por el Administrador. */
export const UmbralesAnalitica = z
  .object({
    votos_minimos: z.int().check(z.minimum(1)).check(z.maximum(500)).check(z.describe('Desde cuántos votos una canción cuenta como «muy pedida»')),
    concentracion_pct: z.int().check(z.minimum(10)).check(z.maximum(100)).check(z.describe('Alerta si el 10 % de los teléfonos que más votan reúne más de este porcentaje de los votos')),
    participacion_minima: z.int().check(z.minimum(1)).check(z.maximum(1000)).check(z.describe('Teléfonos únicos por noche por debajo de los cuales la noche se marca')),
  }).register(z.globalRegistry, { id: 'UmbralesAnalitica' });
export type UmbralesAnalitica = z.infer<typeof UmbralesAnalitica>;

export const CausaNoSono = z.enum(['omitida', 'bloqueada', 'fuera_de_tiempo', 'retirada', 'no_disponible']).register(z.globalRegistry, { id: 'CausaNoSono' });
export type CausaNoSono = z.infer<typeof CausaNoSono>;

export const DiagnosticoAnalitica = z
  .object({
    datos: OrigenDatos,
    umbrales: UmbralesAnalitica,
    no_sonaron: z.array(z.object({ solicitud_id: Id, cancion: Cancion, votos: z.int().check(z.nonnegative()), noche_id: Id, noche_inicio: Fecha, causa: CausaNoSono })),
    concentracion: z
      .object({
        telefonos: z.int().check(z.nonnegative()),
        votos: z.int().check(z.nonnegative()),
        pct_votos_del_10: z.int().check(z.minimum(0)).check(z.maximum(100)).check(z.describe('Porcentaje de los votos que dio el 10 % de los teléfonos que más votaron')),
      }).register(z.globalRegistry, { description: 'Se mide por noche; en un rango, el promedio por noche' }),
    noches: z.array(
      z.object({
        noche_id: Id,
        inicio: Fecha,
        telefonos: z.int().check(z.nonnegative()),
        solicitudes: z.int().check(z.nonnegative()),
        pct_votos_del_10: z.int().check(z.minimum(0)).check(z.maximum(100)),
        no_sonaron: z.int().check(z.nonnegative()).check(z.describe('Pedidos de esa noche que no sonaron enteros: saltados, bloqueados, retirados, no disponibles o fuera de tiempo (2026-11-25)')),
      }),
    ),
  }).register(z.globalRegistry, { id: 'DiagnosticoAnalitica' });
export type DiagnosticoAnalitica = z.infer<typeof DiagnosticoAnalitica>;

/** RF-053: indicadores por noche, día de la semana o tipo de evento (promedio por noche). */
export const ComparacionAnalitica = z
  .object({
    datos: OrigenDatos,
    agrupar: z.enum(['noche', 'dia', 'evento']),
    grupos: z.array(
      z.object({
        clave: z.string(),
        etiqueta: z.string(),
        noches: z.int().check(z.nonnegative()),
        solicitudes: z.number().check(z.nonnegative()),
        telefonos: z.number().check(z.nonnegative()),
        votos: z.number().check(z.nonnegative()),
        espera_promedio_seg: z.nullable(Segundos),
        pedidos_seg: z.number().check(z.nonnegative()).check(z.describe('Lo reproducido por solicitudes')),
        lista_casa_seg: z.number().check(z.nonnegative()).check(z.describe('Lo planeado: la lista de la casa')),
      }),
    ),
  }).register(z.globalRegistry, { id: 'ComparacionAnalitica' });
export type ComparacionAnalitica = z.infer<typeof ComparacionAnalitica>;

/** RF-054: sugerencias con el dato en que se basan; nunca se aplican solas. */
export const Recomendacion = z
  .object({
    id: Id,
    tipo: z.enum(['agregar_a_lista', 'genero_en_lista', 'revisar_bloqueo', 'ajustar_horario']),
    titulo: z.string(),
    base: z.string().check(z.describe('El dato en que se basa, en palabras')),
    destino: z.enum(['listas', 'bloqueos', 'horario']).check(z.describe('La sección del panel donde se decide')),
    lista_id: z.nullable(Id),
  }).register(z.globalRegistry, { id: 'Recomendacion' });
export type Recomendacion = z.infer<typeof Recomendacion>;

export const ListaRecomendaciones = z.object({ datos: OrigenDatos, recomendaciones: z.array(Recomendacion) }).register(z.globalRegistry, { id: 'ListaRecomendaciones' });

/** El miembro «Todos» de cada eje del cubo. */
export const TODOS_CUBO = '*';

/**
 * RF-063: una celda del cubo con los KPI del Entregable 6. Usuarios únicos, esperas y canción
 * más popular se calculan desde los hechos en cada celda: no se obtienen sumando otras.
 */
export const CeldaCubo = z
  .object({
    genero: z.string().check(z.describe('`*`: todos')),
    franja: z.string().check(z.describe('`*`: todas')),
    tercero: z.string().check(z.describe('Día o tipo de evento; `*`: todos')),
    solicitudes: z.int().check(z.nonnegative()),
    votos: z.int().check(z.nonnegative()),
    telefonos: z.int().check(z.nonnegative()).check(z.describe('Usuarios únicos de la celda')),
    no_sonaron: z.int().check(z.nonnegative()),
    espera_promedio_seg: z.nullable(Segundos),
    espera_maxima_seg: z.nullable(Segundos),
    pedidos_seg: z.number().check(z.nonnegative()),
    lista_casa_seg: z.number().check(z.nonnegative()),
    popular: z.nullable(z.object({ cancion: Cancion, votos: z.int().check(z.nonnegative()), solicitudes: z.int().check(z.nonnegative()) })),
  }).register(z.globalRegistry, { id: 'CeldaCubo' });
export type CeldaCubo = z.infer<typeof CeldaCubo>;

export const CuboAnalitica = z
  .object({
    datos: OrigenDatos,
    eje: z.enum(['dia', 'evento']).check(z.describe('Qué es el tercer eje')),
    miembros: z.object({ genero: z.array(z.string()), franja: z.array(z.string()), tercero: z.array(z.string()) }),
    celdas: z.array(CeldaCubo),
  }).register(z.globalRegistry, { id: 'CuboAnalitica' });
export type CuboAnalitica = z.infer<typeof CuboAnalitica>;

/** Fuente del cubo (RF-067): `vivo` lo calcula el backend al consultar; `historico`, el pipeline. */
export const FuenteCubo = z.enum(['vivo', 'historico']).register(z.globalRegistry, { id: 'FuenteCubo' });
export type FuenteCubo = z.infer<typeof FuenteCubo>;

// ── Catálogo de canciones y género (RF-068) ───────────────────────────────────

/**
 * Quién le puso el género a la canción. Los tres primeros son la clasificación automática con
 * los metadatos de YouTube; `MANUAL` es la corrección del panel, que la automática no pisa.
 * `null`: sin género.
 */
export const OrigenCategoria = z.nullable(z.enum(['ETIQUETAS', 'TITULO', 'DESCRIPCION', 'MANUAL'])).register(z.globalRegistry, { id: 'OrigenCategoria' });
export type OrigenCategoria = z.infer<typeof OrigenCategoria>;

export const CancionCatalogo = z
  .object({
    id: Id,
    youtube_video_id: z.string(),
    titulo: z.string(),
    artista: z.nullable(z.string()),
    categoria: z.nullable(z.string()).check(z.describe('El género; `null` = sin clasificar')),
    origen_categoria: OrigenCategoria,
    pedidos: z.int().check(z.nonnegative()).check(z.describe('Solicitudes de la canción en este bar')),
  }).register(z.globalRegistry, { id: 'CancionCatalogo' });
export type CancionCatalogo = z.infer<typeof CancionCatalogo>;

export const CuerpoCategoria = z.object({ categoria: z.string().check(z.minLength(1)).check(z.describe('Un género de `GET /panel/generos`')) }).register(z.globalRegistry, { id: 'CuerpoCategoria' });

export const CategoriaCorregida = z
  .object({ id: Id, categoria: z.string(), origen_categoria: OrigenCategoria }).register(z.globalRegistry, { id: 'CategoriaCorregida' });
export type CategoriaCorregida = z.infer<typeof CategoriaCorregida>;

/**
 * `POST /panel/canciones/reclasificar` (RF-068): vuelve a pasar por el clasificador hasta 10
 * canciones del bar sin género, desde `despuesDe`. Nunca inventa: sin pistas en el video queda en
 * `sin_coincidencia`. `interrumpida`: YouTube no respondió o se acabó la cuota; se puede reintentar
 * desde `siguiente_desde_id`.
 */
export const Reclasificacion = z
  .object({
    consultadas: z.int().check(z.nonnegative()),
    clasificadas: z.int().check(z.nonnegative()),
    clasificadas_por_origen: z.object({ ETIQUETAS: z.int(), TITULO: z.int(), DESCRIPCION: z.int() }),
    sin_coincidencia: z.int().check(z.nonnegative()),
    no_disponibles: z.int().check(z.nonnegative()),
    siguiente_desde_id: z.int().check(z.nonnegative()),
    hay_mas: z.boolean(),
    interrumpida: z.nullable(z.enum(['cuota_agotada', 'youtube_no_disponible'])),
  }).register(z.globalRegistry, { id: 'Reclasificacion' });
export type Reclasificacion = z.infer<typeof Reclasificacion>;
