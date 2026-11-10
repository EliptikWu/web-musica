import { z } from 'zod';
import { Fecha, Id, Segundos } from './comunes.ts';

/** Lo que reporta el reproductor de la pantalla del bar (RF-007, RF-019). */
export const ReportePantalla = z
  .object({
    posicion_seg: Segundos,
    codigo_youtube: z.int().nullable().describe('Código de error del reproductor de YouTube (2, 5, 100, 101, 150, 153)'),
  })
  .meta({ id: 'ReportePantalla' });
export type ReportePantalla = z.infer<typeof ReportePantalla>;

// ── Vinculación de la pantalla (RF-058, ADR-23) ───────────────────────────────

/** 6 caracteres sin los que se confunden de lejos (0, O, 1, I). */
export const CodigoPantalla = z
  .string()
  .regex(/^[A-HJ-NP-Z2-9]{6}$/)
  .meta({ id: 'CodigoPantalla', description: 'Seis caracteres en mayúscula, sin 0, O, 1 ni I' });

/** Cómo está esta pantalla según su cookie `rockola_pantalla`. */
export const VinculoPantalla = z
  .discriminatedUnion('estado', [
    z.object({ estado: z.literal('sin_vincular') }),
    z.object({ estado: z.literal('esperando'), codigo: CodigoPantalla, expira_en: Fecha }),
    z.object({ estado: z.literal('vinculada'), nombre: z.string() }),
  ])
  .meta({ id: 'VinculoPantalla' });
export type VinculoPantalla = z.infer<typeof VinculoPantalla>;

/** Una pantalla vinculada, como la ve el panel. */
export const PantallaVinculada = z
  .object({
    id: Id,
    nombre: z.string(),
    vinculada_en: Fecha,
    vinculada_por: z.object({ id: Id, nombre: z.string() }),
    ultimo_reporte: Fecha.nullable().describe('Último inicio, fin o error que reportó; null si todavía no'),
  })
  .meta({ id: 'PantallaVinculada' });
export type PantallaVinculada = z.infer<typeof PantallaVinculada>;

export const ListaPantallas = z.object({ pantallas: z.array(PantallaVinculada) }).meta({ id: 'ListaPantallas' });
export type ListaPantallas = z.infer<typeof ListaPantallas>;

export const CuerpoVincularPantalla = z
  .object({ codigo: CodigoPantalla, nombre: z.string().trim().min(1).max(40) })
  .meta({ id: 'CuerpoVincularPantalla' });
export type CuerpoVincularPantalla = z.infer<typeof CuerpoVincularPantalla>;
