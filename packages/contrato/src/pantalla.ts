import * as z from 'zod/mini';
import { Fecha, Id, Segundos } from './comunes.ts';

/** Lo que reporta el reproductor de la pantalla del bar (RF-007, RF-019). */
export const ReportePantalla = z
  .object({
    posicion_seg: Segundos,
    codigo_youtube: z.nullable(z.int()).check(z.describe('Código de error del reproductor de YouTube (2, 5, 100, 101, 150, 153)')),
  }).register(z.globalRegistry, { id: 'ReportePantalla' });
export type ReportePantalla = z.infer<typeof ReportePantalla>;

// ── Vinculación de la pantalla (RF-058, ADR-23) ───────────────────────────────

/** 6 caracteres sin los que se confunden de lejos (0, O, 1, I). */
export const CodigoPantalla = z
  .string().check(z.regex(/^[A-HJ-NP-Z2-9]{6}$/)).register(z.globalRegistry, { id: 'CodigoPantalla', description: 'Seis caracteres en mayúscula, sin 0, O, 1 ni I' });

/** Cómo está esta pantalla según su cookie `rockola_pantalla`. */
export const VinculoPantalla = z
  .discriminatedUnion('estado', [
    z.object({ estado: z.literal('sin_vincular') }),
    z.object({ estado: z.literal('esperando'), codigo: CodigoPantalla, expira_en: Fecha }),
    z.object({ estado: z.literal('vinculada'), nombre: z.string() }),
  ]).register(z.globalRegistry, { id: 'VinculoPantalla' });
export type VinculoPantalla = z.infer<typeof VinculoPantalla>;

/** Una pantalla vinculada, como la ve el panel. */
export const PantallaVinculada = z
  .object({
    id: Id,
    nombre: z.string(),
    vinculada_en: Fecha,
    vinculada_por: z.object({ id: Id, nombre: z.string() }),
    ultimo_reporte: z.nullable(Fecha).check(z.describe('Último inicio, fin o error que reportó; null si todavía no')),
  }).register(z.globalRegistry, { id: 'PantallaVinculada' });
export type PantallaVinculada = z.infer<typeof PantallaVinculada>;

export const ListaPantallas = z.object({ pantallas: z.array(PantallaVinculada) }).register(z.globalRegistry, { id: 'ListaPantallas' });
export type ListaPantallas = z.infer<typeof ListaPantallas>;

export const CuerpoVincularPantalla = z
  .object({ codigo: CodigoPantalla, nombre: z.string().check(z.trim()).check(z.minLength(1)).check(z.maxLength(40)) }).register(z.globalRegistry, { id: 'CuerpoVincularPantalla' });
export type CuerpoVincularPantalla = z.infer<typeof CuerpoVincularPantalla>;
