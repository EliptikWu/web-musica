import { z } from 'zod';
import { Ahora } from './cola.ts';
import { Fecha } from './comunes.ts';

export const RecursoPanel = z
  .enum(['noche', 'yo', 'bitacora', 'bloqueos', 'listas', 'horario', 'usuarios', 'establecimiento', 'eventos'])
  .meta({ id: 'RecursoPanel' });
export type RecursoPanel = z.infer<typeof RecursoPanel>;

/**
 * Eventos del canal de tiempo real (`GET /bares/{slug}/eventos`, SSE). El nombre del evento
 * SSE es el `tipo`; `data` es el JSON del evento entero.
 */
export const EventoTiempoReal = z
  .discriminatedUnion('tipo', [
    z.object({ tipo: z.literal('ahora'), ahora: Ahora }),
    z.object({ tipo: z.literal('panel'), recursos: z.array(RecursoPanel) }),
    z.object({ tipo: z.literal('ping'), servidor_ahora: Fecha }),
  ])
  .meta({ id: 'EventoTiempoReal' });
export type EventoTiempoReal = z.infer<typeof EventoTiempoReal>;
