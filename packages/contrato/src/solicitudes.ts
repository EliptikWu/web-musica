import * as z from 'zod/mini';
import { DestinoQr } from './bar.ts';
import { Id, Segundos } from './comunes.ts';

export const CuerpoSolicitud = z.object({ video_id: z.string().check(z.minLength(1)) }).register(z.globalRegistry, { id: 'CuerpoSolicitud' });
export type CuerpoSolicitud = z.infer<typeof CuerpoSolicitud>;

/** Respuesta al pedir. Si la canción ya estaba pendiente, `resultado` es `votada` (RN-02). */
export const RespuestaSolicitud = z
  .object({
    resultado: z.enum(['creada', 'votada']),
    solicitud_id: Id,
    puesto: z.int().check(z.positive()),
    espera_seg: Segundos,
    pedidos_restantes: z.int().check(z.nonnegative()),
  }).register(z.globalRegistry, { id: 'RespuestaSolicitud' });
export type RespuestaSolicitud = z.infer<typeof RespuestaSolicitud>;

export const RespuestaVoto = z
  .object({
    solicitud_id: Id,
    votos_total: z.int().check(z.nonnegative()),
  }).register(z.globalRegistry, { id: 'RespuestaVoto' });
export type RespuestaVoto = z.infer<typeof RespuestaVoto>;

/** Retirar mi pedido (RF-061): sale de la cola y el teléfono recupera ese pedido. */
export const RespuestaRetiroPedido = z
  .object({
    solicitud_id: Id,
    pedidos_restantes: z.int().check(z.nonnegative()),
  }).register(z.globalRegistry, { id: 'RespuestaRetiroPedido' });
export type RespuestaRetiroPedido = z.infer<typeof RespuestaRetiroPedido>;

/** Mover un voto de una solicitud a otra en un solo paso (RF-014, Could). */
export const CuerpoCambioVoto = z.object({ desde: Id, hacia: Id }).register(z.globalRegistry, { id: 'CuerpoCambioVoto' });
export type CuerpoCambioVoto = z.infer<typeof CuerpoCambioVoto>;

export const ResolucionQr = z
  .object({
    slug: z.string(),
    destino: z._default(DestinoQr, 'web').check(z.describe('RF-065: `app` ofrece descargar la app')),
    apk_url: z._default(z.nullable(z.url()), null).check(z.describe('Solo con destino `app`')),
  }).register(z.globalRegistry, { id: 'ResolucionQr' });
export const QrPublico = z.object({ url: z.url(), destino: z._default(DestinoQr, 'web') }).register(z.globalRegistry, { id: 'QrPublico' });
