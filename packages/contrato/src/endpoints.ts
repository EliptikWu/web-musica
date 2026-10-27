import { z } from 'zod';
import { Bar } from './bar.ts';
import { Busqueda } from './busqueda.ts';
import { Ahora, Destacada } from './cola.ts';
import { type CodigoError, SinContenido } from './comunes.ts';
import { CuerpoDispositivo, Yo } from './dispositivo.ts';
import {
  BusquedaPanel, CambioBloqueo, CambioEstablecimiento, CambioUsuario, ClaveTemporal, CuerpoActivacion, CuerpoBloqueo,
  CuerpoCambioClave, CuerpoCancionLista, CuerpoEntrar, CuerpoEvento, CuerpoHorario, CuerpoLista, CuerpoOrden, CuerpoRetiro,
  CuerpoServicio, CuerpoSesion, CuerpoUsuario, Establecimiento, EventoBar, Horario, ListaBloqueos, ListaCasaDetalle,
  ListaEventos, ListasCasa, ListaUsuarios, Noche, PaginaBitacora, QrEstablecimiento, type Rol, UsuarioCreado, UsuarioPanel,
  YoPanel, Bloqueo,
} from './panel.ts';
import { ReportePantalla } from './pantalla.ts';
import { CuerpoCambioVoto, CuerpoSolicitud, QrPublico, ResolucionQr, RespuestaSolicitud, RespuestaVoto } from './solicitudes.ts';

export type Metodo = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
export type Superficie = 'cliente' | 'pantalla' | 'panel';

/**
 * Un endpoint del contrato. Es la única definición: de aquí salen el cliente de la app, los
 * manejadores de la simulación y la documentación para el backend (doc 04).
 */
export type Endpoint = {
  metodo: Metodo;
  /** Ruta relativa a la base de la API, con parámetros entre llaves: `/bares/{slug}/ahora`. */
  ruta: string;
  resumen: string;
  superficie: Superficie;
  rf: readonly string[];
  consulta?: z.ZodObject;
  cuerpo?: z.ZodType;
  respuesta: z.ZodType;
  /** Código HTTP de éxito. */
  estado: 200 | 201 | 204;
  /** Lleva `Idempotency-Key`: la misma clave devuelve la misma respuesta (RNF-008). */
  idempotente?: boolean;
  /** Rol mínimo en el panel. Sin él, la ruta es pública. */
  rol?: Rol;
  errores: readonly CodigoError[];
};

function definir<const E extends Endpoint>(endpoint: E): E {
  return endpoint;
}

const PANEL_ERR = ['no_autenticado', 'sin_permiso'] as const;

export const endpoints = {
  // ── Cliente ────────────────────────────────────────────────────────────────
  resolverQr: definir({ metodo: 'GET', ruta: '/qr/{token}', resumen: 'Resolver el QR al bar', superficie: 'cliente', rf: ['RF-001', 'RF-035'], respuesta: ResolucionQr, estado: 200, errores: ['qr_reemplazado', 'no_encontrado'] }),
  bar: definir({ metodo: 'GET', ruta: '/bares/{slug}', resumen: 'Datos públicos del bar y su horario', superficie: 'cliente', rf: ['RF-001', 'RF-003'], respuesta: Bar, estado: 200, errores: ['no_encontrado'] }),
  ahora: definir({ metodo: 'GET', ruta: '/bares/{slug}/ahora', resumen: 'Foto actual: servicio, lo que suena y la cola', superficie: 'cliente', rf: ['RF-003', 'RF-015', 'RF-016', 'RF-017', 'RF-018'], respuesta: Ahora, estado: 200, errores: ['no_encontrado'] }),
  destacadas: definir({ metodo: 'GET', ruta: '/bares/{slug}/destacadas', resumen: 'Lo más votado de la noche', superficie: 'cliente', rf: ['RF-015'], respuesta: z.object({ destacadas: z.array(Destacada) }), estado: 200, errores: ['no_encontrado'] }),
  registrarDispositivo: definir({ metodo: 'POST', ruta: '/bares/{slug}/dispositivo', resumen: 'Registrar el teléfono y devolver lo suyo', superficie: 'cliente', rf: ['RF-002'], cuerpo: CuerpoDispositivo, respuesta: Yo, estado: 200, errores: ['datos_invalidos', 'no_encontrado'] }),
  yo: definir({ metodo: 'GET', ruta: '/bares/{slug}/dispositivo', resumen: 'Cupo, solicitudes y votos de este teléfono', superficie: 'cliente', rf: ['RF-011', 'RF-013'], respuesta: Yo, estado: 200, errores: ['no_encontrado'] }),
  avisoVisto: definir({ metodo: 'POST', ruta: '/bares/{slug}/dispositivo/aviso', resumen: 'Registrar que vio el aviso de datos', superficie: 'cliente', rf: ['RF-004'], respuesta: SinContenido, estado: 204, errores: ['no_encontrado'] }),
  buscar: definir({ metodo: 'GET', ruta: '/bares/{slug}/busqueda', resumen: 'Buscar en YouTube y en la cola', superficie: 'cliente', rf: ['RF-005', 'RF-006', 'RF-007', 'RF-008'], consulta: z.object({ q: z.string().min(2) }), respuesta: Busqueda, estado: 200, errores: ['datos_invalidos', 'youtube_no_disponible', 'cuota_agotada'] }),
  pedir: definir({ metodo: 'POST', ruta: '/bares/{slug}/solicitudes', resumen: 'Pedir una canción (si ya está, cuenta como voto)', superficie: 'cliente', rf: ['RF-009', 'RF-007'], cuerpo: CuerpoSolicitud, respuesta: RespuestaSolicitud, estado: 201, idempotente: true, errores: ['limite_pedidos', 'ya_votaste', 'bloqueada', 'no_disponible', 'no_embebible', 'muy_larga', 'reproducida_hace_poco', 'servicio_no_disponible'] }),
  votar: definir({ metodo: 'POST', ruta: '/bares/{slug}/solicitudes/{id}/votos', resumen: 'Votar por una solicitud', superficie: 'cliente', rf: ['RF-010', 'RF-011', 'RF-012'], respuesta: RespuestaVoto, estado: 201, idempotente: true, errores: ['ya_votaste', 'no_pendiente', 'servicio_no_disponible'] }),
  cambiarVoto: definir({ metodo: 'PUT', ruta: '/bares/{slug}/dispositivo/voto', resumen: 'Mover un voto a otra solicitud', superficie: 'cliente', rf: ['RF-014'], cuerpo: CuerpoCambioVoto, respuesta: RespuestaVoto, estado: 200, idempotente: true, errores: ['no_pendiente', 'ya_votaste', 'servicio_no_disponible'] }),

  // ── Pantalla del bar ───────────────────────────────────────────────────────
  qrPublico: definir({ metodo: 'GET', ruta: '/bares/{slug}/qr-publico', resumen: 'URL que codifica el QR de la placa', superficie: 'pantalla', rf: ['RF-035'], respuesta: QrPublico, estado: 200, errores: ['no_encontrado'] }),
  reportarInicio: definir({ metodo: 'POST', ruta: '/bares/{slug}/pantalla/reproducciones/{id}/inicio', resumen: 'El reproductor empezó la canción', superficie: 'pantalla', rf: ['RF-019'], cuerpo: ReportePantalla, respuesta: SinContenido, estado: 204, idempotente: true, errores: ['no_encontrado'] }),
  reportarFin: definir({ metodo: 'POST', ruta: '/bares/{slug}/pantalla/reproducciones/{id}/fin', resumen: 'El reproductor terminó la canción', superficie: 'pantalla', rf: ['RF-019', 'RF-020'], cuerpo: ReportePantalla, respuesta: SinContenido, estado: 204, idempotente: true, errores: ['no_encontrado'] }),
  reportarError: definir({ metodo: 'POST', ruta: '/bares/{slug}/pantalla/reproducciones/{id}/error', resumen: 'El reproductor no pudo reproducir la canción', superficie: 'pantalla', rf: ['RF-007'], cuerpo: ReportePantalla, respuesta: SinContenido, estado: 204, idempotente: true, errores: ['no_encontrado'] }),

  // ── Panel: sesión ──────────────────────────────────────────────────────────
  entrar: definir({ metodo: 'POST', ruta: '/panel/sesion', resumen: 'Entrar al panel', superficie: 'panel', rf: ['RF-021'], cuerpo: CuerpoEntrar, respuesta: YoPanel, estado: 200, errores: ['credenciales_invalidas', 'cuenta_bloqueada', 'datos_invalidos'] }),
  salir: definir({ metodo: 'DELETE', ruta: '/panel/sesion', resumen: 'Salir del panel', superficie: 'panel', rf: ['RF-021'], respuesta: SinContenido, estado: 204, errores: [] }),
  yoPanel: definir({ metodo: 'GET', ruta: '/panel/yo', resumen: 'Usuario actual y su establecimiento', superficie: 'panel', rf: ['RF-021', 'RF-022'], respuesta: YoPanel, estado: 200, rol: 'operador', errores: ['no_autenticado'] }),
  cambiarClave: definir({ metodo: 'PUT', ruta: '/panel/yo/clave', resumen: 'Cambiar la clave propia', superficie: 'panel', rf: ['RF-023', 'RF-025'], cuerpo: CuerpoCambioClave, respuesta: SinContenido, estado: 204, rol: 'operador', errores: ['no_autenticado', 'credenciales_invalidas', 'datos_invalidos'] }),

  // ── Panel: operación de la noche ───────────────────────────────────────────
  noche: definir({ metodo: 'GET', ruta: '/panel/noche', resumen: 'Todo lo de la operación de esta noche', superficie: 'panel', rf: ['RF-037', 'RF-044', 'RF-049'], respuesta: Noche, estado: 200, rol: 'operador', errores: [...PANEL_ERR] }),
  abrirSesion: definir({ metodo: 'POST', ruta: '/panel/sesiones', resumen: 'Abrir la noche a mano', superficie: 'panel', rf: ['RF-037'], cuerpo: CuerpoSesion, respuesta: SinContenido, estado: 204, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto'] }),
  cerrarSesion: definir({ metodo: 'POST', ruta: '/panel/sesiones/actual/cierre', resumen: 'Cerrar la noche a mano', superficie: 'panel', rf: ['RF-037'], respuesta: SinContenido, estado: 204, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto'] }),
  cambiarServicio: definir({ metodo: 'PUT', ruta: '/panel/servicio', resumen: 'Pausar o reanudar solicitudes, o forzar abierto', superficie: 'panel', rf: ['RF-036', 'RF-039'], cuerpo: CuerpoServicio, respuesta: SinContenido, estado: 204, rol: 'operador', errores: [...PANEL_ERR] }),
  mantenimiento: definir({ metodo: 'POST', ruta: '/panel/servicio/mantenimiento', resumen: 'Modo mantenimiento: cierra la sesión y expira lo pendiente', superficie: 'panel', rf: ['RF-038'], respuesta: SinContenido, estado: 204, rol: 'administrador', errores: [...PANEL_ERR] }),
  saltar: definir({ metodo: 'POST', ruta: '/panel/reproduccion/{id}/salto', resumen: 'Saltar la canción que suena', superficie: 'panel', rf: ['RF-040'], respuesta: SinContenido, estado: 204, rol: 'operador', idempotente: true, errores: [...PANEL_ERR, 'no_encontrado'] }),
  pausarReproduccion: definir({ metodo: 'POST', ruta: '/panel/reproduccion/{id}/pausa', resumen: 'Pausar la reproducción', superficie: 'panel', rf: ['RF-040'], respuesta: SinContenido, estado: 204, rol: 'operador', errores: [...PANEL_ERR, 'no_encontrado'] }),
  reanudarReproduccion: definir({ metodo: 'POST', ruta: '/panel/reproduccion/{id}/reanudacion', resumen: 'Reanudar la reproducción', superficie: 'panel', rf: ['RF-040'], respuesta: SinContenido, estado: 204, rol: 'operador', errores: [...PANEL_ERR, 'no_encontrado'] }),
  retirar: definir({ metodo: 'POST', ruta: '/panel/solicitudes/{id}/retiro', resumen: 'Retirar una solicitud con motivo', superficie: 'panel', rf: ['RF-041'], cuerpo: CuerpoRetiro, respuesta: SinContenido, estado: 204, rol: 'operador', idempotente: true, errores: [...PANEL_ERR, 'no_pendiente'] }),

  // ── Panel: bloqueos ────────────────────────────────────────────────────────
  bloqueos: definir({ metodo: 'GET', ruta: '/panel/bloqueos', resumen: 'Bloqueos vigentes y vencidos', superficie: 'panel', rf: ['RF-043'], consulta: z.object({ vigencia: z.enum(['vigentes', 'vencidos', 'todos']).optional() }), respuesta: ListaBloqueos, estado: 200, rol: 'operador', errores: [...PANEL_ERR] }),
  bloquear: definir({ metodo: 'POST', ruta: '/panel/bloqueos', resumen: 'Bloquear un video', superficie: 'panel', rf: ['RF-042'], cuerpo: CuerpoBloqueo, respuesta: Bloqueo, estado: 201, rol: 'operador', idempotente: true, errores: [...PANEL_ERR, 'datos_invalidos'] }),
  extenderBloqueo: definir({ metodo: 'PATCH', ruta: '/panel/bloqueos/{id}', resumen: 'Cambiar la duración de un bloqueo', superficie: 'panel', rf: ['RF-043'], cuerpo: CambioBloqueo, respuesta: Bloqueo, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'no_encontrado'] }),
  desbloquear: definir({ metodo: 'DELETE', ruta: '/panel/bloqueos/{id}', resumen: 'Levantar un bloqueo', superficie: 'panel', rf: ['RF-043'], respuesta: SinContenido, estado: 204, rol: 'administrador', errores: [...PANEL_ERR, 'no_encontrado'] }),

  // ── Panel: gestión ─────────────────────────────────────────────────────────
  horario: definir({ metodo: 'GET', ruta: '/panel/horario', resumen: 'Horario semanal', superficie: 'panel', rf: ['RF-036'], respuesta: Horario, estado: 200, rol: 'administrador', errores: [...PANEL_ERR] }),
  guardarHorario: definir({ metodo: 'PUT', ruta: '/panel/horario', resumen: 'Guardar el horario semanal', superficie: 'panel', rf: ['RF-036'], cuerpo: CuerpoHorario, respuesta: Horario, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'datos_invalidos', 'conflicto'] }),
  listas: definir({ metodo: 'GET', ruta: '/panel/listas', resumen: 'Listas de la casa', superficie: 'panel', rf: ['RF-028'], consulta: z.object({ q: z.string().optional() }), respuesta: ListasCasa, estado: 200, rol: 'administrador', errores: [...PANEL_ERR] }),
  crearLista: definir({ metodo: 'POST', ruta: '/panel/listas', resumen: 'Crear una lista', superficie: 'panel', rf: ['RF-027'], cuerpo: CuerpoLista, respuesta: ListaCasaDetalle, estado: 201, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto'] }),
  lista: definir({ metodo: 'GET', ruta: '/panel/listas/{id}', resumen: 'Una lista con sus canciones', superficie: 'panel', rf: ['RF-028'], respuesta: ListaCasaDetalle, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'no_encontrado'] }),
  renombrarLista: definir({ metodo: 'PATCH', ruta: '/panel/listas/{id}', resumen: 'Renombrar una lista', superficie: 'panel', rf: ['RF-029'], cuerpo: CuerpoLista, respuesta: ListaCasaDetalle, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto', 'no_encontrado'] }),
  eliminarLista: definir({ metodo: 'DELETE', ruta: '/panel/listas/{id}', resumen: 'Eliminar una lista (no la activa)', superficie: 'panel', rf: ['RF-030'], respuesta: SinContenido, estado: 204, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto', 'no_encontrado'] }),
  agregarALista: definir({ metodo: 'POST', ruta: '/panel/listas/{id}/canciones', resumen: 'Agregar una canción', superficie: 'panel', rf: ['RF-031'], cuerpo: CuerpoCancionLista, respuesta: ListaCasaDetalle, estado: 201, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto', 'bloqueada', 'no_disponible'] }),
  quitarDeLista: definir({ metodo: 'DELETE', ruta: '/panel/listas/{id}/canciones/{elemento}', resumen: 'Quitar una canción', superficie: 'panel', rf: ['RF-032'], respuesta: ListaCasaDetalle, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto'] }),
  ordenarLista: definir({ metodo: 'PUT', ruta: '/panel/listas/{id}/orden', resumen: 'Reordenar las canciones', superficie: 'panel', rf: ['RF-032'], cuerpo: CuerpoOrden, respuesta: ListaCasaDetalle, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'datos_invalidos'] }),
  activarLista: definir({ metodo: 'POST', ruta: '/panel/listas/{id}/activacion', resumen: 'Activar una lista y su rotación', superficie: 'panel', rf: ['RF-033'], cuerpo: CuerpoActivacion, respuesta: ListaCasaDetalle, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto'] }),
  buscarPanel: definir({ metodo: 'GET', ruta: '/panel/busqueda', resumen: 'Buscar en YouTube para armar listas', superficie: 'panel', rf: ['RF-031'], consulta: z.object({ q: z.string().min(2) }), respuesta: BusquedaPanel, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'youtube_no_disponible', 'cuota_agotada'] }),
  usuarios: definir({ metodo: 'GET', ruta: '/panel/usuarios', resumen: 'Personal', superficie: 'panel', rf: ['RF-024'], consulta: z.object({ q: z.string().optional(), rol: z.enum(['operador', 'administrador']).optional(), estado: z.enum(['activo', 'inactivo']).optional() }), respuesta: ListaUsuarios, estado: 200, rol: 'administrador', errores: [...PANEL_ERR] }),
  crearUsuario: definir({ metodo: 'POST', ruta: '/panel/usuarios', resumen: 'Crear un usuario', superficie: 'panel', rf: ['RF-023'], cuerpo: CuerpoUsuario, respuesta: UsuarioCreado, estado: 201, rol: 'administrador', idempotente: true, errores: [...PANEL_ERR, 'conflicto', 'datos_invalidos'] }),
  editarUsuario: definir({ metodo: 'PATCH', ruta: '/panel/usuarios/{id}', resumen: 'Editar un usuario', superficie: 'panel', rf: ['RF-025'], cuerpo: CambioUsuario, respuesta: UsuarioPanel, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto', 'no_encontrado'] }),
  claveTemporal: definir({ metodo: 'POST', ruta: '/panel/usuarios/{id}/clave-temporal', resumen: 'Restablecer la clave', superficie: 'panel', rf: ['RF-025'], respuesta: ClaveTemporal, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'no_encontrado'] }),
  desactivarUsuario: definir({ metodo: 'POST', ruta: '/panel/usuarios/{id}/desactivacion', resumen: 'Desactivar un usuario', superficie: 'panel', rf: ['RF-026'], respuesta: UsuarioPanel, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto', 'no_encontrado'] }),
  reactivarUsuario: definir({ metodo: 'POST', ruta: '/panel/usuarios/{id}/reactivacion', resumen: 'Reactivar un usuario', superficie: 'panel', rf: ['RF-026'], respuesta: UsuarioPanel, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'no_encontrado'] }),
  establecimiento: definir({ metodo: 'GET', ruta: '/panel/establecimiento', resumen: 'Datos del establecimiento', superficie: 'panel', rf: ['RF-034'], respuesta: Establecimiento, estado: 200, rol: 'administrador', errores: [...PANEL_ERR] }),
  editarEstablecimiento: definir({ metodo: 'PATCH', ruta: '/panel/establecimiento', resumen: 'Editar el establecimiento', superficie: 'panel', rf: ['RF-034'], cuerpo: CambioEstablecimiento, respuesta: Establecimiento, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'datos_invalidos'] }),
  qrEstablecimiento: definir({ metodo: 'GET', ruta: '/panel/establecimiento/qr', resumen: 'QR vigente', superficie: 'panel', rf: ['RF-035'], respuesta: QrEstablecimiento, estado: 200, rol: 'administrador', errores: [...PANEL_ERR] }),
  regenerarQr: definir({ metodo: 'POST', ruta: '/panel/establecimiento/qr/regeneracion', resumen: 'Regenerar el QR (el anterior deja de funcionar)', superficie: 'panel', rf: ['RF-035'], respuesta: QrEstablecimiento, estado: 200, rol: 'administrador', errores: [...PANEL_ERR] }),
  bitacora: definir({ metodo: 'GET', ruta: '/panel/bitacora', resumen: 'Bitácora de acciones', superficie: 'panel', rf: ['RF-044', 'RF-008'], consulta: z.object({ desde: z.string().optional(), hasta: z.string().optional(), usuario: z.string().optional(), tipo: z.string().optional(), cursor: z.string().optional() }), respuesta: PaginaBitacora, estado: 200, rol: 'administrador', errores: [...PANEL_ERR] }),
  eventos: definir({ metodo: 'GET', ruta: '/panel/eventos', resumen: 'Calendario de eventos', superficie: 'panel', rf: ['RF-045'], respuesta: ListaEventos, estado: 200, rol: 'administrador', errores: [...PANEL_ERR] }),
  crearEvento: definir({ metodo: 'POST', ruta: '/panel/eventos', resumen: 'Crear un evento', superficie: 'panel', rf: ['RF-045'], cuerpo: CuerpoEvento, respuesta: EventoBar, estado: 201, rol: 'administrador', errores: [...PANEL_ERR, 'datos_invalidos', 'conflicto'] }),
  editarEvento: definir({ metodo: 'PATCH', ruta: '/panel/eventos/{id}', resumen: 'Editar un evento', superficie: 'panel', rf: ['RF-045'], cuerpo: CuerpoEvento.partial(), respuesta: EventoBar, estado: 200, rol: 'administrador', errores: [...PANEL_ERR, 'datos_invalidos', 'no_encontrado'] }),
  eliminarEvento: definir({ metodo: 'DELETE', ruta: '/panel/eventos/{id}', resumen: 'Eliminar un evento futuro', superficie: 'panel', rf: ['RF-045'], respuesta: SinContenido, estado: 204, rol: 'administrador', errores: [...PANEL_ERR, 'conflicto', 'no_encontrado'] }),
} as const satisfies Record<string, Endpoint>;

export type Endpoints = typeof endpoints;
export type NombreEndpoint = keyof Endpoints;

/** Parámetros de ruta de un endpoint: `{slug}` → `{ slug: string }`. */
type ParametrosDe<R extends string> = R extends `${string}{${infer P}}${infer Resto}` ? { [K in P | keyof ParametrosDe<Resto>]: string } : Record<never, never>;
export type Parametros<N extends NombreEndpoint> = ParametrosDe<Endpoints[N]['ruta']>;
export type Respuesta<N extends NombreEndpoint> = z.infer<Endpoints[N]['respuesta']>;
export type Cuerpo<N extends NombreEndpoint> = Endpoints[N] extends { cuerpo: infer C extends z.ZodType } ? z.infer<C> : never;
export type Consulta<N extends NombreEndpoint> = Endpoints[N] extends { consulta: infer C extends z.ZodType } ? z.input<C> : never;

/** Arma la ruta concreta: `rutaDe('/bares/{slug}/ahora', { slug: 'demo' })` → `/bares/demo/ahora`. */
export function rutaDe(ruta: string, parametros: Record<string, string>): string {
  return ruta.replace(/\{(\w+)\}/g, (_, nombre: string) => {
    const valor = parametros[nombre];
    if (valor === undefined) throw new Error(`Falta el parámetro «${nombre}» para ${ruta}`);
    return encodeURIComponent(valor);
  });
}
