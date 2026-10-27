import { z } from 'zod';
import { Servicio } from './bar.ts';
import { Cancion } from './cancion.ts';
import { Fecha, Id, Segundos } from './comunes.ts';

export const OrigenReproduccion = z.enum(['solicitud', 'lista_casa']).meta({ id: 'OrigenReproduccion' });
export type OrigenReproduccion = z.infer<typeof OrigenReproduccion>;

/** Lo que suena ahora (RF-016). El avance se calcula con `posicion_seg` y `servidor_ahora`. */
export const Reproduccion = z
  .object({
    id: Id,
    cancion: Cancion,
    origen: OrigenReproduccion,
    solicitud_id: Id.nullable(),
    votos: z.int().nonnegative().nullable().describe('Votos de la solicitud; null si suena la lista de la casa'),
    inicio: Fecha,
    posicion_seg: Segundos.describe('Segundos reproducidos en el momento de servidor_ahora'),
    estado: z.enum(['sonando', 'pausada']),
  })
  .meta({ id: 'Reproduccion' });
export type Reproduccion = z.infer<typeof Reproduccion>;

/** Una canción en la cola, ya ordenada por RN-05. */
export const ItemCola = z
  .object({
    solicitud_id: Id,
    cancion: Cancion,
    votos_total: z.int().nonnegative(),
    fecha_solicitud: Fecha,
    puesto: z.int().positive(),
    puesto_anterior: z.int().positive().nullable().describe('Puesto antes del último reordenamiento; alimenta «Subió 2 puestos»'),
  })
  .meta({ id: 'ItemCola' });
export type ItemCola = z.infer<typeof ItemCola>;

export const ListaCasaResumen = z
  .object({
    nombre: z.string(),
    total_canciones: z.int().nonnegative(),
  })
  .meta({ id: 'ListaCasaResumen' });

/** La foto de la noche: lo que todas las pantallas muestran y lo que viaja por tiempo real. */
export const Ahora = z
  .object({
    version: z.int().nonnegative().describe('Crece con cada cambio; un salto obliga a volver a pedir la foto'),
    servidor_ahora: Fecha,
    servicio: Servicio,
    reproduccion: Reproduccion.nullable(),
    cola: z.array(ItemCola),
    lista_casa: ListaCasaResumen.nullable().describe('Lista activa, la que suena si la cola se vacía (RN-08)'),
  })
  .meta({ id: 'Ahora' });
export type Ahora = z.infer<typeof Ahora>;

/** «Lo más votado de la noche» (bienvenida). */
export const Destacada = z
  .object({
    cancion: Cancion,
    votos: z.int().nonnegative(),
    contexto: z.enum(['sonando', 'sono', 'en_cola']),
    hora: Fecha.nullable().describe('Cuándo sonó, si ya sonó'),
  })
  .meta({ id: 'Destacada' });
export type Destacada = z.infer<typeof Destacada>;
