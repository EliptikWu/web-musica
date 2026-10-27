import { z } from 'zod';
import { Id, Segundos } from './comunes.ts';

export const CuerpoSolicitud = z.object({ video_id: z.string().min(1) }).meta({ id: 'CuerpoSolicitud' });
export type CuerpoSolicitud = z.infer<typeof CuerpoSolicitud>;

/** Respuesta al pedir. Si la canción ya estaba pendiente, `resultado` es `votada` (RN-02). */
export const RespuestaSolicitud = z
  .object({
    resultado: z.enum(['creada', 'votada']),
    solicitud_id: Id,
    puesto: z.int().positive(),
    espera_seg: Segundos,
    pedidos_restantes: z.int().nonnegative(),
  })
  .meta({ id: 'RespuestaSolicitud' });
export type RespuestaSolicitud = z.infer<typeof RespuestaSolicitud>;

export const RespuestaVoto = z
  .object({
    solicitud_id: Id,
    votos_total: z.int().nonnegative(),
  })
  .meta({ id: 'RespuestaVoto' });
export type RespuestaVoto = z.infer<typeof RespuestaVoto>;

/** Mover un voto de una solicitud a otra en un solo paso (RF-014, Could). */
export const CuerpoCambioVoto = z.object({ desde: Id, hacia: Id }).meta({ id: 'CuerpoCambioVoto' });
export type CuerpoCambioVoto = z.infer<typeof CuerpoCambioVoto>;

export const ResolucionQr = z.object({ slug: z.string() }).meta({ id: 'ResolucionQr' });
export const QrPublico = z.object({ url: z.url() }).meta({ id: 'QrPublico' });
