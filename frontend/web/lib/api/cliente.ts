import { endpoints } from '@rockola/contrato';
import { crearPedir } from './pedido';

export { BASE_API, type OpcionesPedido } from './pedido';

/**
 * `pedir` con el registro completo del contrato: el panel, la TV y la plataforma. El teléfono usa
 * `lib/api/telefono.ts`, que solo trae sus endpoints (doc 07, análisis del 2026-11-25, #3).
 */
export const pedir = crearPedir(endpoints);
