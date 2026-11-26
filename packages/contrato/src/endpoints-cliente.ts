import * as z from 'zod/mini';
import { definir, type Endpoint } from './endpoint-base.ts';
import { Bar } from './bar.ts';
import { Busqueda } from './busqueda.ts';
import { Ahora, Destacada } from './cola.ts';
import { SinContenido } from './comunes.ts';
import { CuerpoDispositivo, Yo } from './dispositivo.ts';
import { CuerpoCambioVoto, CuerpoSolicitud, ResolucionQr, RespuestaRetiroPedido, RespuestaSolicitud, RespuestaVoto } from './solicitudes.ts';

/**
 * Los endpoints del teléfono del cliente. Van aparte para que la ruta del teléfono no descargue
 * los esquemas del panel, la analítica y la plataforma (doc 07, análisis del 2026-11-25, #3; T7).
 * `endpoints.ts` los junta con los demás: el registro completo y el OpenAPI no cambian.
 */
export const endpointsCliente = {
  // ── Cliente ────────────────────────────────────────────────────────────────
  resolverQr: definir({ metodo: 'GET', ruta: '/qr/{token}', resumen: 'Resolver el QR al bar', superficie: 'cliente', rf: ['RF-001', 'RF-035'], respuesta: ResolucionQr, estado: 200, errores: ['qr_reemplazado', 'no_encontrado'] }),
  bar: definir({ metodo: 'GET', ruta: '/bares/{slug}', resumen: 'Datos públicos del bar y su horario', superficie: 'cliente', rf: ['RF-001', 'RF-003'], respuesta: Bar, estado: 200, errores: ['no_encontrado'] }),
  ahora: definir({ metodo: 'GET', ruta: '/bares/{slug}/ahora', resumen: 'Foto actual: servicio, lo que suena y la cola', superficie: 'cliente', rf: ['RF-003', 'RF-015', 'RF-016', 'RF-017', 'RF-018'], respuesta: Ahora, estado: 200, errores: ['no_encontrado'] }),
  destacadas: definir({ metodo: 'GET', ruta: '/bares/{slug}/destacadas', resumen: 'Lo más votado de la noche: las tres primeras', superficie: 'cliente', rf: ['RF-015'], respuesta: z.object({ destacadas: z.array(Destacada).check(z.describe('Hasta 3, de más a menos votos')) }), estado: 200, errores: ['no_encontrado'] }),
  registrarDispositivo: definir({ metodo: 'POST', ruta: '/bares/{slug}/dispositivo', resumen: 'Registrar el teléfono y devolver lo suyo', superficie: 'cliente', rf: ['RF-002'], cuerpo: CuerpoDispositivo, respuesta: Yo, estado: 200, errores: ['datos_invalidos', 'no_encontrado'] }),
  yo: definir({ metodo: 'GET', ruta: '/bares/{slug}/dispositivo', resumen: 'Cupo, solicitudes y votos de este teléfono', superficie: 'cliente', rf: ['RF-011', 'RF-013'], respuesta: Yo, estado: 200, errores: ['no_encontrado'] }),
  avisoVisto: definir({ metodo: 'POST', ruta: '/bares/{slug}/dispositivo/aviso', resumen: 'Registrar que vio el aviso de datos', superficie: 'cliente', rf: ['RF-004'], respuesta: SinContenido, estado: 204, errores: ['no_encontrado'] }),
  buscar: definir({ metodo: 'GET', ruta: '/bares/{slug}/busqueda', resumen: 'Buscar en YouTube y en la cola', superficie: 'cliente', rf: ['RF-005', 'RF-006', 'RF-007', 'RF-008'], consulta: z.object({ q: z.string().check(z.minLength(2)) }), respuesta: Busqueda, estado: 200, errores: ['datos_invalidos', 'youtube_no_disponible', 'cuota_agotada'] }),
  pedir: definir({ metodo: 'POST', ruta: '/bares/{slug}/solicitudes', resumen: 'Pedir una canción (si ya está, cuenta como voto)', superficie: 'cliente', rf: ['RF-009', 'RF-007'], cuerpo: CuerpoSolicitud, respuesta: RespuestaSolicitud, estado: 201, idempotente: true, errores: ['limite_pedidos', 'ya_votaste', 'bloqueada', 'no_disponible', 'no_embebible', 'muy_larga', 'reproducida_hace_poco', 'servicio_no_disponible'] }),
  votar: definir({ metodo: 'POST', ruta: '/bares/{slug}/solicitudes/{id}/votos', resumen: 'Votar por una solicitud', superficie: 'cliente', rf: ['RF-010', 'RF-011', 'RF-012'], respuesta: RespuestaVoto, estado: 201, idempotente: true, errores: ['ya_votaste', 'no_pendiente', 'servicio_no_disponible'] }),
  cambiarVoto: definir({ metodo: 'PUT', ruta: '/bares/{slug}/dispositivo/voto', resumen: 'Mover un voto a otra solicitud', superficie: 'cliente', rf: ['RF-014'], cuerpo: CuerpoCambioVoto, respuesta: RespuestaVoto, estado: 200, idempotente: true, errores: ['no_pendiente', 'ya_votaste', 'servicio_no_disponible'] }),
  // RF-060: sin el voto, la canción baja; el voto de tu propio pedido no se quita (RF-061).
  quitarVoto: definir({ metodo: 'DELETE', ruta: '/bares/{slug}/solicitudes/{id}/votos', resumen: 'Quitar mi voto de una solicitud', superficie: 'cliente', rf: ['RF-060'], respuesta: RespuestaVoto, estado: 200, idempotente: true, errores: ['no_pendiente', 'datos_invalidos', 'servicio_no_disponible'] }),
  // RF-061: solo quien la pidió y solo si no suena todavía; queda `retirada`.
  retirarPedido: definir({ metodo: 'DELETE', ruta: '/bares/{slug}/solicitudes/{id}', resumen: 'Retirar mi pedido de la cola', superficie: 'cliente', rf: ['RF-061'], respuesta: RespuestaRetiroPedido, estado: 200, idempotente: true, errores: ['no_pendiente', 'sin_permiso', 'servicio_no_disponible'] }),
} as const satisfies Record<string, Endpoint>;

export type EndpointsCliente = typeof endpointsCliente;
export type NombreEndpointCliente = keyof EndpointsCliente;
