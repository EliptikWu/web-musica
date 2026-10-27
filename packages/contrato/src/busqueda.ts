import { z } from 'zod';
import { Cancion } from './cancion.ts';
import { ItemCola } from './cola.ts';
import { Fecha } from './comunes.ts';

/** Por qué no se puede pedir un resultado (RN-06, RN-07). El cliente ve el motivo, no un botón gris. */
export const MotivoNoPedible = z
  .enum(['bloqueada', 'no_disponible', 'no_embebible', 'muy_larga', 'reproducida_hace_poco'])
  .meta({ id: 'MotivoNoPedible' });
export type MotivoNoPedible = z.infer<typeof MotivoNoPedible>;

export const ResultadoBusqueda = z
  .object({
    cancion: Cancion,
    pedible: z.boolean(),
    motivo: MotivoNoPedible.nullable(),
    disponible_desde: Fecha.nullable().describe('Con reproducida_hace_poco: cuándo se podrá pedir otra vez'),
    genero: z.string().nullable().describe('Para filtrar (RF-055). `null` = sin clasificar. TBD: de dónde sale (doc 04)'),
    pedidas_en_el_bar: z.int().nonnegative().describe('Veces que se pidió en este bar en las últimas cuatro semanas: la popularidad de los filtros (propuesto)'),
  })
  .meta({ id: 'ResultadoBusqueda' });
export type ResultadoBusqueda = z.infer<typeof ResultadoBusqueda>;

export const Busqueda = z
  .object({
    consulta: z.string(),
    en_cola: z.array(ItemCola).describe('Coincidencias que ya están en la cola: se votan en vez de pedirse (RN-02)'),
    resultados: z.array(ResultadoBusqueda),
    desde_cache: z.boolean().describe('Resultados guardados porque YouTube no respondió o se agotó la cuota (PB-54)'),
  })
  .meta({ id: 'Busqueda' });
export type Busqueda = z.infer<typeof Busqueda>;
