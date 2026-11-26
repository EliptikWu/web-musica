import * as z from 'zod/mini';
import { Segundos } from './comunes.ts';

/** Un video de YouTube tal como lo ve la interfaz (diccionario: Cancion). */
export const Cancion = z
  .object({
    video_id: z.string().check(z.minLength(1)).check(z.describe('ID del video en YouTube')),
    titulo: z.string().check(z.minLength(1)),
    artista: z.string().check(z.describe('Derivado del canal o del título (TBD: cómo)')),
    duracion_seg: Segundos,
    miniatura_url: z.nullable(z.url()).check(z.describe('Miniatura de i.ytimg.com')),
  }).register(z.globalRegistry, { id: 'Cancion' });
export type Cancion = z.infer<typeof Cancion>;
