import * as z from 'zod/mini';
import { Servicio } from './bar.ts';
import { Cancion } from './cancion.ts';
import { Fecha, Id, Segundos } from './comunes.ts';

export const OrigenReproduccion = z.enum(['solicitud', 'lista_casa']).register(z.globalRegistry, { id: 'OrigenReproduccion' });
export type OrigenReproduccion = z.infer<typeof OrigenReproduccion>;

/** Lo que suena ahora (RF-016). El avance se calcula con `posicion_seg` y `servidor_ahora`. */
export const Reproduccion = z
  .object({
    id: Id,
    cancion: Cancion,
    origen: OrigenReproduccion,
    solicitud_id: z.nullable(Id),
    votos: z.nullable(z.int().check(z.nonnegative())).check(z.describe('Votos de la solicitud; null si suena la lista de la casa')),
    inicio: Fecha,
    posicion_seg: Segundos.check(z.describe('Segundos reproducidos en el momento de servidor_ahora')),
    estado: z.enum(['sonando', 'pausada']),
  }).register(z.globalRegistry, { id: 'Reproduccion' });
export type Reproduccion = z.infer<typeof Reproduccion>;

/** Una canción en la cola, ya ordenada por RN-05. */
export const ItemCola = z
  .object({
    solicitud_id: Id,
    cancion: Cancion,
    votos_total: z.int().check(z.nonnegative()),
    fecha_solicitud: Fecha,
    puesto: z.int().check(z.positive()),
    puesto_anterior: z.nullable(z.int().check(z.positive())).check(z.describe('Puesto antes del último reordenamiento; alimenta «Subió 2 puestos»')),
    espera_seg: Segundos.check(z.describe('Espera estimada en servidor_ahora: lo que le falta a lo que suena más lo que duran las de delante (RF-018)')),
  }).register(z.globalRegistry, { id: 'ItemCola' });
export type ItemCola = z.infer<typeof ItemCola>;

export const ListaCasaResumen = z
  .object({
    nombre: z.string(),
    total_canciones: z.int().check(z.nonnegative()),
  }).register(z.globalRegistry, { id: 'ListaCasaResumen' });

/** RF-062: volumen de la pantalla del bar, de 0 (silencio) a 100. Sin dato, 80. */
export const Volumen = z._default(z.int().check(z.minimum(0)).check(z.maximum(100)), 80).register(z.globalRegistry, { id: 'Volumen' });

/** La foto de la noche: lo que todas las pantallas muestran y lo que viaja por tiempo real. */
export const Ahora = z
  .object({
    version: z.int().check(z.nonnegative()).check(z.describe('Crece con cada cambio; un salto obliga a volver a pedir la foto')),
    servidor_ahora: Fecha,
    servicio: Servicio,
    reproduccion: z.nullable(Reproduccion),
    cola: z.array(ItemCola),
    lista_casa: z.nullable(ListaCasaResumen).check(z.describe('Lista activa, la que suena si la cola se vacía (RN-08)')),
    volumen: Volumen.check(z.describe('RF-062: volumen de la pantalla del bar, lo fija el panel')),
  }).register(z.globalRegistry, { id: 'Ahora' });
export type Ahora = z.infer<typeof Ahora>;

/** «Lo más votado de la noche» (bienvenida). */
export const Destacada = z
  .object({
    cancion: Cancion,
    votos: z.int().check(z.nonnegative()),
    contexto: z.enum(['sonando', 'sono', 'en_cola']),
    hora: z.nullable(Fecha).check(z.describe('Cuándo sonó, si ya sonó')),
  }).register(z.globalRegistry, { id: 'Destacada' });
export type Destacada = z.infer<typeof Destacada>;
