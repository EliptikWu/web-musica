import { endpointsCliente } from '@rockola/contrato';
import { crearPedir } from './pedido';

/**
 * `pedir` del teléfono del cliente: solo sus endpoints, para que la ruta `/b/[bar]` no descargue
 * los esquemas del panel, la analítica y la plataforma (doc 07, análisis del 2026-11-25, #3; T7).
 * Pedir aquí un endpoint de otra superficie no compila.
 */
export const pedir = crearPedir(endpointsCliente);
