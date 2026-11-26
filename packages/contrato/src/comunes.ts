import * as z from 'zod/mini';

/** Fecha y hora ISO 8601 con zona, p. ej. `2026-10-22T22:45:00-05:00` (doc 04, «Convenciones»). */
export const Fecha = z.iso.datetime({ offset: true }).register(z.globalRegistry, { id: 'Fecha', description: 'Fecha y hora ISO 8601 con zona.' });

/** Hora del día en 24 h, `"20:00"`. */
export const HoraDelDia = z
  .string().check(z.regex(/^([01]\d|2[0-3]):[0-5]\d$/, 'Hora en formato HH:MM')).register(z.globalRegistry, { id: 'HoraDelDia', description: 'Hora local del establecimiento, 24 h (HH:MM).' });

export const Segundos = z.int().check(z.nonnegative()).register(z.globalRegistry, { id: 'Segundos', description: 'Duración o posición en segundos enteros.' });
export const Id = z.string().check(z.minLength(1)).register(z.globalRegistry, { id: 'Id', description: 'Identificador opaco generado por el backend.' });
export const Slug = z
  .string().check(z.regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, 'slug en minúsculas con guiones')).register(z.globalRegistry, { id: 'Slug', description: 'Identificador del establecimiento en las rutas.' });

export const CodigoError = z
  .enum([
    'datos_invalidos',
    'no_autenticado',
    'credenciales_invalidas',
    'sin_permiso',
    'debe_cambiar_clave',
    'no_encontrado',
    'limite_pedidos',
    'ya_votaste',
    'conflicto',
    'qr_reemplazado',
    'no_pendiente',
    'bloqueada',
    'no_disponible',
    'no_embebible',
    'muy_larga',
    'reproducida_hace_poco',
    'servicio_no_disponible',
    'cuenta_bloqueada',
    'youtube_no_disponible',
    'cuota_agotada',
    'error_interno',
  ]).register(z.globalRegistry, { id: 'CodigoError', description: 'Código estable del error; el cliente lo traduce a su texto (doc 04).' });
export type CodigoError = z.infer<typeof CodigoError>;

export const ErrorApi = z
  .object({
    codigo: CodigoError,
    mensaje: z.string(),
    detalle: z.optional(z.record(z.string(), z.unknown())),
  }).register(z.globalRegistry, { id: 'ErrorApi', description: 'Cuerpo de toda respuesta de error.' });
export type ErrorApi = z.infer<typeof ErrorApi>;

/** Respuesta sin cuerpo (204). */
export const SinContenido = z.null().register(z.globalRegistry, { id: 'SinContenido' });
