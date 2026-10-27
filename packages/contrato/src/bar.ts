import { z } from 'zod';
import { Fecha, HoraDelDia, Slug } from './comunes.ts';

/** Una franja del horario semanal. Si cruza medianoche (20:00–02:00) pertenece al día en que empieza. */
export const Franja = z
  .object({
    dia_semana: z.int().min(1).max(7).describe('1 = lunes … 7 = domingo (ISO 8601)'),
    hora_inicio: HoraDelDia,
    hora_fin: HoraDelDia,
    activo: z.boolean(),
  })
  .meta({ id: 'Franja' });
export type Franja = z.infer<typeof Franja>;

export const EstadoServicio = z
  .enum(['abierto', 'fuera_de_horario', 'en_pausa', 'mantenimiento'])
  .meta({ id: 'EstadoServicio', description: 'Resultado de combinar el horario con el modo del QR (RN-09).' });
export type EstadoServicio = z.infer<typeof EstadoServicio>;

export const ModoQr = z
  .enum(['automatico', 'forzado_activo', 'pausado'])
  .meta({ id: 'ModoQr', description: 'Lo que elige el personal: seguir el horario, forzar abierto o pausar (RF-036, RF-039).' });
export type ModoQr = z.infer<typeof ModoQr>;

export const Servicio = z
  .object({
    estado: EstadoServicio,
    modo_qr: ModoQr,
    desde: Fecha.nullable().describe('Desde cuándo está en este estado'),
    hasta: Fecha.nullable().describe('Cuándo termina el estado actual según el horario (p. ej. cierra a las 2:00 a. m.)'),
    proxima_apertura: Fecha.nullable().describe('Cuándo vuelven a abrir los pedidos, si está cerrado'),
  })
  .meta({ id: 'Servicio' });
export type Servicio = z.infer<typeof Servicio>;

/** Lo público de un establecimiento (RF-001, RF-003). */
export const Bar = z
  .object({
    slug: Slug,
    nombre: z.string().min(1),
    zona_horaria: z.string().describe('IANA, p. ej. America/Bogota'),
    pedidos_maximos: z.int().positive().describe('RN-01: solicitudes pendientes por teléfono y sesión'),
    duracion_max_seg: z.int().positive().describe('RN-06: 600 = 10 minutos'),
    ventana_no_repeticion_min: z.int().nonnegative().describe('RN-07'),
    horario: z.array(Franja),
  })
  .meta({ id: 'Bar' });
export type Bar = z.infer<typeof Bar>;
