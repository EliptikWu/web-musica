import { z } from 'zod';
import { Cancion } from './cancion.ts';
import { Fecha, Id } from './comunes.ts';

/** Estados de una solicitud (doc 04). `retirada` es una corrección propuesta al modelo. */
export const EstadoSolicitud = z
  .enum(['pendiente', 'reproduciendo', 'reproducida', 'saltada', 'no_disponible', 'bloqueada', 'retirada', 'expirada'])
  .meta({ id: 'EstadoSolicitud' });
export type EstadoSolicitud = z.infer<typeof EstadoSolicitud>;

export const TipoDispositivo = z.enum(['movil', 'tableta', 'otro']).meta({ id: 'TipoDispositivo', description: 'Según el ancho de pantalla, no el modelo' });
export type TipoDispositivo = z.infer<typeof TipoDispositivo>;

export const MiSolicitud = z
  .object({
    solicitud_id: Id,
    cancion: Cancion,
    estado: EstadoSolicitud,
    fecha_solicitud: Fecha,
    votos_total: z.int().nonnegative(),
    puesto: z.int().positive().nullable().describe('Solo si está pendiente'),
    motivo: z.string().nullable().describe('Motivo del retiro o del bloqueo, si lo hay'),
  })
  .meta({ id: 'MiSolicitud' });
export type MiSolicitud = z.infer<typeof MiSolicitud>;

/** Lo que sabe el servidor de este teléfono en la sesión actual (RF-011, RF-013). */
export const Yo = z
  .object({
    pedidos_maximos: z.int().positive(),
    pedidos_restantes: z.int().nonnegative(),
    solicitudes: z.array(MiSolicitud),
    votos: z.array(Id).describe('IDs de las solicitudes por las que votó este teléfono'),
    aviso_visto_en: Fecha.nullable(),
  })
  .meta({ id: 'Yo' });
export type Yo = z.infer<typeof Yo>;

export const CuerpoDispositivo = z.object({ tipo: TipoDispositivo }).meta({ id: 'CuerpoDispositivo' });
