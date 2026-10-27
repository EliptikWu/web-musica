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
    actualizado_en: Fecha.describe('Última carga del pipeline'),
    noches: z.int().nonnegative().describe('Noches que entran en el cálculo'),
    desde: Fecha.nullable().describe('Inicio de la primera noche'),
    hasta: Fecha.nullable().describe('Inicio de la última noche (su fecha es la de la noche)'),
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
