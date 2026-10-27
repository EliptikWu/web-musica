import { z } from 'zod';
import { Segundos } from './comunes.ts';

/** Un video de YouTube tal como lo ve la interfaz (diccionario: Cancion). */
export const Cancion = z
  .object({
    video_id: z.string().min(1).describe('ID del video en YouTube'),
    titulo: z.string().min(1),
    artista: z.string().describe('Derivado del canal o del título (TBD: cómo)'),
    duracion_seg: Segundos,
    miniatura_url: z.url().nullable().describe('Miniatura de i.ytimg.com'),
  })
  .meta({ id: 'Cancion' });
export type Cancion = z.infer<typeof Cancion>;
