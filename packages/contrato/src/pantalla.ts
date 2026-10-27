import { z } from 'zod';
import { Segundos } from './comunes.ts';

/** Lo que reporta el reproductor de la pantalla del bar (RF-007, RF-019). */
export const ReportePantalla = z
  .object({
    posicion_seg: Segundos,
    codigo_youtube: z.int().nullable().describe('Código de error del reproductor de YouTube (2, 5, 100, 101, 150, 153)'),
  })
  .meta({ id: 'ReportePantalla' });
export type ReportePantalla = z.infer<typeof ReportePantalla>;
