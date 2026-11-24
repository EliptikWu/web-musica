import { z } from 'zod';
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
    origen: z.enum(['registrado', 'ejemplo']).describe('`ejemplo`: datos inventados de la simulación; la interfaz lo rotula'),
    actualizado_en: Fecha.nullable().describe('Última carga del pipeline; `null` si todavía no ha corrido'),
    noches: z.int().nonnegative().describe('Noches que entran en el cálculo'),
    desde: Fecha.nullable().describe('Inicio de la primera noche'),
    hasta: Fecha.nullable().describe('Inicio de la última noche (su fecha es la de la noche)'),
    fuente: z.enum(['operativa', 'pipeline']).optional().describe('Solo el cubo en vivo (RF-067): `operativa` = calculado ahora desde las tablas de la operación'),
    en_vivo: z.boolean().optional().describe('`true` en el cubo en vivo: incluye la noche abierta y `actualizado_en` es la hora de la consulta'),
    sesiones_abiertas: z.int().nonnegative().optional().describe('Solo en vivo: noches abiertas ahora mismo'),
  })
  .meta({ id: 'OrigenDatos' });
export type OrigenDatos = z.infer<typeof OrigenDatos>;

/** Qué se consulta: una noche, o un rango de fechas (`YYYY-MM-DD`, en la zona del bar). */
export const FiltroAnalitica = z.object({
  noche: z.string().optional(),
  desde: z.string().optional(),
  hasta: z.string().optional(),
  /** 1 = lunes … 7 = domingo (RF-051). */
  dia_semana: z.string().optional(),
});

/** Una noche cerrada: la sesión de servicio, con su evento del calendario si lo hubo. */
export const NocheAnalitica = z
  .object({
    id: Id,
    inicio: Fecha,
    fin: Fecha,
    dia_semana: z.int().min(1).max(7),
    evento: z.object({ id: Id, nombre: z.string(), tipo_evento: TipoEvento }).nullable(),
  })
  .meta({ id: 'NocheAnalitica' });
export type NocheAnalitica = z.infer<typeof NocheAnalitica>;

export const ListaNoches = z.object({ datos: OrigenDatos, noches: z.array(NocheAnalitica) }).meta({ id: 'ListaNoches' });

/** RF-049 y RF-050: lo que pasó en la noche (o el rango) elegido. */
export const ResumenAnalitica = z
  .object({
    datos: OrigenDatos,
    solicitudes: z.int().nonnegative(),
    telefonos: z.int().nonnegative().describe('Dispositivos únicos (anonimizados en el modelo, RF-048)'),
    votos: z.int().nonnegative(),
    mas_popular: z.object({ cancion: Cancion, votos: z.int().nonnegative(), solicitudes: z.int().nonnegative() }).nullable(),
    espera: z.object({ promedio_seg: Segundos, maxima_seg: Segundos }).nullable().describe('De la solicitud a que empezó a sonar'),
    tiempo: z.object({ pedidos_seg: Segundos, lista_casa_seg: Segundos }).describe('Tiempo que sonó cada origen'),
    no_sonaron: z.int().nonnegative().describe('Solicitudes que nunca llegaron a reproducirse'),
    /** Solo cuando se consulta una noche: lo que sonó, en orden. */
    historial: z.array(z.object({ inicio: Fecha, cancion: Cancion, origen: OrigenReproduccion, votos: z.int().nonnegative() })),
  })
  .meta({ id: 'ResumenAnalitica' });
export type ResumenAnalitica = z.infer<typeof ResumenAnalitica>;

/** RF-051: géneros y franjas horarias. */
export const ParticipacionAnalitica = z
  .object({
    datos: OrigenDatos,
    generos: z.array(z.object({ genero: z.string().nullable().describe('`null`: sin clasificar'), solicitudes: z.int().nonnegative(), votos: z.int().nonnegative() })),
    franjas: z.array(z.object({ hora: z.int().min(0).max(23), solicitudes: z.int().nonnegative(), votos: z.int().nonnegative(), telefonos: z.int().nonnegative() })),
  })
  .meta({ id: 'ParticipacionAnalitica' });
export type ParticipacionAnalitica = z.infer<typeof ParticipacionAnalitica>;

/** RF-052: umbrales documentados y configurables por el Administrador. */
export const UmbralesAnalitica = z
  .object({
    votos_minimos: z.int().min(1).max(500).describe('Desde cuántos votos una canción cuenta como «muy pedida»'),
    concentracion_pct: z.int().min(10).max(100).describe('Alerta si el 10 % de los teléfonos que más votan reúne más de este porcentaje de los votos'),
    participacion_minima: z.int().min(1).max(1000).describe('Teléfonos únicos por noche por debajo de los cuales la noche se marca'),
  })
  .meta({ id: 'UmbralesAnalitica' });
export type UmbralesAnalitica = z.infer<typeof UmbralesAnalitica>;

export const CausaNoSono = z.enum(['omitida', 'bloqueada', 'fuera_de_tiempo', 'retirada', 'no_disponible']).meta({ id: 'CausaNoSono' });
export type CausaNoSono = z.infer<typeof CausaNoSono>;

export const DiagnosticoAnalitica = z
  .object({
    datos: OrigenDatos,
    umbrales: UmbralesAnalitica,
    no_sonaron: z.array(z.object({ solicitud_id: Id, cancion: Cancion, votos: z.int().nonnegative(), noche_id: Id, noche_inicio: Fecha, causa: CausaNoSono })),
    concentracion: z
      .object({
        telefonos: z.int().nonnegative(),
        votos: z.int().nonnegative(),
        pct_votos_del_10: z.int().min(0).max(100).describe('Porcentaje de los votos que dio el 10 % de los teléfonos que más votaron'),
      })
      .describe('Se mide por noche; en un rango, el promedio por noche'),
    noches: z.array(
      z.object({ noche_id: Id, inicio: Fecha, telefonos: z.int().nonnegative(), solicitudes: z.int().nonnegative(), pct_votos_del_10: z.int().min(0).max(100) }),
    ),
  })
  .meta({ id: 'DiagnosticoAnalitica' });
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
        noches: z.int().nonnegative(),
        solicitudes: z.number().nonnegative(),
        telefonos: z.number().nonnegative(),
        votos: z.number().nonnegative(),
        espera_promedio_seg: Segundos.nullable(),
        pedidos_seg: z.number().nonnegative().describe('Lo reproducido por solicitudes'),
        lista_casa_seg: z.number().nonnegative().describe('Lo planeado: la lista de la casa'),
      }),
    ),
  })
  .meta({ id: 'ComparacionAnalitica' });
export type ComparacionAnalitica = z.infer<typeof ComparacionAnalitica>;

/** RF-054: sugerencias con el dato en que se basan; nunca se aplican solas. */
export const Recomendacion = z
  .object({
    id: Id,
    tipo: z.enum(['agregar_a_lista', 'genero_en_lista', 'revisar_bloqueo', 'ajustar_horario']),
    titulo: z.string(),
    base: z.string().describe('El dato en que se basa, en palabras'),
    destino: z.enum(['listas', 'bloqueos', 'horario']).describe('La sección del panel donde se decide'),
    lista_id: Id.nullable(),
  })
  .meta({ id: 'Recomendacion' });
export type Recomendacion = z.infer<typeof Recomendacion>;

export const ListaRecomendaciones = z.object({ datos: OrigenDatos, recomendaciones: z.array(Recomendacion) }).meta({ id: 'ListaRecomendaciones' });

/** El miembro «Todos» de cada eje del cubo. */
export const TODOS_CUBO = '*';

/**
 * RF-063: una celda del cubo con los KPI del Entregable 6. Usuarios únicos, esperas y canción
 * más popular se calculan desde los hechos en cada celda: no se obtienen sumando otras.
 */
export const CeldaCubo = z
  .object({
    genero: z.string().describe('`*`: todos'),
    franja: z.string().describe('`*`: todas'),
    tercero: z.string().describe('Día o tipo de evento; `*`: todos'),
    solicitudes: z.int().nonnegative(),
    votos: z.int().nonnegative(),
    telefonos: z.int().nonnegative().describe('Usuarios únicos de la celda'),
    no_sonaron: z.int().nonnegative(),
    espera_promedio_seg: Segundos.nullable(),
    espera_maxima_seg: Segundos.nullable(),
    pedidos_seg: z.number().nonnegative(),
    lista_casa_seg: z.number().nonnegative(),
    popular: z.object({ cancion: Cancion, votos: z.int().nonnegative(), solicitudes: z.int().nonnegative() }).nullable(),
  })
  .meta({ id: 'CeldaCubo' });
export type CeldaCubo = z.infer<typeof CeldaCubo>;

export const CuboAnalitica = z
  .object({
    datos: OrigenDatos,
    eje: z.enum(['dia', 'evento']).describe('Qué es el tercer eje'),
    miembros: z.object({ genero: z.array(z.string()), franja: z.array(z.string()), tercero: z.array(z.string()) }),
    celdas: z.array(CeldaCubo),
  })
  .meta({ id: 'CuboAnalitica' });
export type CuboAnalitica = z.infer<typeof CuboAnalitica>;

/** Fuente del cubo (RF-067): `vivo` lo calcula el backend al consultar; `historico`, el pipeline. */
export const FuenteCubo = z.enum(['vivo', 'historico']).meta({ id: 'FuenteCubo' });
export type FuenteCubo = z.infer<typeof FuenteCubo>;

// ── Catálogo de canciones y género (RF-068) ───────────────────────────────────

/**
 * Quién le puso el género a la canción. Los tres primeros son la clasificación automática con
 * los metadatos de YouTube; `MANUAL` es la corrección del panel, que la automática no pisa.
 * `null`: sin género.
 */
export const OrigenCategoria = z.enum(['ETIQUETAS', 'TITULO', 'DESCRIPCION', 'MANUAL']).nullable().meta({ id: 'OrigenCategoria' });
export type OrigenCategoria = z.infer<typeof OrigenCategoria>;

export const CancionCatalogo = z
  .object({
    id: Id,
    youtube_video_id: z.string(),
    titulo: z.string(),
    artista: z.string().nullable(),
    categoria: z.string().nullable().describe('El género; `null` = sin clasificar'),
    origen_categoria: OrigenCategoria,
    pedidos: z.int().nonnegative().describe('Solicitudes de la canción en este bar'),
  })
  .meta({ id: 'CancionCatalogo' });
export type CancionCatalogo = z.infer<typeof CancionCatalogo>;

export const CuerpoCategoria = z.object({ categoria: z.string().min(1).describe('Un género de `GET /panel/generos`') }).meta({ id: 'CuerpoCategoria' });

export const CategoriaCorregida = z
  .object({ id: Id, categoria: z.string(), origen_categoria: OrigenCategoria })
  .meta({ id: 'CategoriaCorregida' });
export type CategoriaCorregida = z.infer<typeof CategoriaCorregida>;
