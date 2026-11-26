import * as z from 'zod/mini';
import { DestinoQr, Franja, Servicio, Tema } from './bar.ts';
import { Cancion } from './cancion.ts';
import { ItemCola, Reproduccion, Volumen } from './cola.ts';
import { Fecha, Id, Segundos, Slug } from './comunes.ts';

// ── Sesión y usuarios (RF-021 a RF-026) ──────────────────────────────────────

export const Rol = z.enum(['operador', 'administrador']).register(z.globalRegistry, { id: 'Rol', description: 'En el modelo original: dj y admin' });
export type Rol = z.infer<typeof Rol>;

export const UsuarioPanel = z
  .object({
    id: Id,
    nombre: z.string().check(z.minLength(1)),
    correo: z.email(),
    rol: Rol,
    estado: z.enum(['activo', 'inactivo']),
    debe_cambiar_clave: z.boolean(),
    ultimo_acceso: z.nullable(Fecha),
  }).register(z.globalRegistry, { id: 'UsuarioPanel', description: 'Nunca lleva la contraseña ni su hash' });
export type UsuarioPanel = z.infer<typeof UsuarioPanel>;

export const YoPanel = z
  .object({
    usuario: UsuarioPanel,
    establecimiento: z.object({ slug: Slug, nombre: z.string(), zona_horaria: z.string().check(z.describe('IANA, p. ej. America/Bogota')) }),
  }).register(z.globalRegistry, { id: 'YoPanel' });
export type YoPanel = z.infer<typeof YoPanel>;

export const CuerpoEntrar = z.object({ correo: z.email(), clave: z.string().check(z.minLength(1)) }).register(z.globalRegistry, { id: 'CuerpoEntrar' });
export const CuerpoCambioClave = z
  .object({ actual: z.string().check(z.minLength(1)), nueva: z.string().check(z.minLength(10, 'Al menos 10 caracteres')) }).register(z.globalRegistry, { id: 'CuerpoCambioClave', description: 'Mínimo de 10 caracteres (propuesto)' });

export const CuerpoUsuario = z.object({ nombre: z.string().check(z.trim()).check(z.minLength(1)), correo: z.email(), rol: Rol }).register(z.globalRegistry, { id: 'CuerpoUsuario' });
export const CambioUsuario = z.partial(CuerpoUsuario).register(z.globalRegistry, { id: 'CambioUsuario' });
export const UsuarioCreado = z
  .object({ usuario: UsuarioPanel, clave_temporal: z.string().check(z.describe('Se muestra una sola vez; obliga a cambiarla al entrar')) }).register(z.globalRegistry, { id: 'UsuarioCreado' });
export const ClaveTemporal = z.object({ clave_temporal: z.string() }).register(z.globalRegistry, { id: 'ClaveTemporal' });
export const ListaUsuarios = z.object({ usuarios: z.array(UsuarioPanel) }).register(z.globalRegistry, { id: 'ListaUsuarios' });

// ── Bitácora (RF-044, RN-11) ──────────────────────────────────────────────────

export const TipoAccion = z
  .enum([
    'saltar', 'bloquear', 'desbloquear', 'extender_bloqueo', 'retirar',
    'pausar_solicitudes', 'reanudar_solicitudes', 'forzar_apertura',
    'pausar_reproduccion', 'reanudar_reproduccion', 'cambiar_volumen',
    'mantenimiento', 'abrir_sesion', 'cerrar_sesion',
    'editar_horario', 'editar_lista', 'activar_lista',
    'crear_usuario', 'editar_usuario', 'desactivar_usuario', 'reactivar_usuario', 'restablecer_clave',
    'editar_establecimiento', 'regenerar_qr', 'editar_evento', 'editar_umbrales',
    'vincular_pantalla', 'desvincular_pantalla', 'editar_categoria',
    'error_youtube', 'intento_no_permitido',
  ]).register(z.globalRegistry, { id: 'TipoAccion' });
export type TipoAccion = z.infer<typeof TipoAccion>;

export const AccionAdmin = z
  .object({
    id: Id,
    fecha: Fecha,
    usuario: z.nullable(z.object({ id: Id, nombre: z.string() })).check(z.describe('null = lo hizo el sistema')),
    tipo: TipoAccion,
    objetivo: z.nullable(z
      .object({
        tipo: z.enum(['cancion', 'usuario', 'lista', 'horario', 'servicio', 'sesion', 'establecimiento', 'evento', 'pantalla']),
        nombre: z.string(),
        detalle: z.nullable(z.string()).check(z.describe('Para una canción, el artista')),
      })),
    detalle: z.nullable(z.string()).check(z.describe('Motivo, duración o mensaje de error')),
  }).register(z.globalRegistry, { id: 'AccionAdmin' });
export type AccionAdmin = z.infer<typeof AccionAdmin>;

export const PaginaBitacora = z
  .object({ acciones: z.array(AccionAdmin), siguiente: z.nullable(z.string()) }).register(z.globalRegistry, { id: 'PaginaBitacora' });

// ── Bloqueos (RF-042, RF-043) ─────────────────────────────────────────────────

export const DuracionBloqueo = z.enum(['fin_sesion', 'horas', 'indefinido']).register(z.globalRegistry, { id: 'DuracionBloqueo' });
export type DuracionBloqueo = z.infer<typeof DuracionBloqueo>;

export const Bloqueo = z
  .object({
    id: Id,
    cancion: Cancion,
    motivo: z.string(),
    duracion: DuracionBloqueo,
    horas: z.nullable(z.int().check(z.positive())),
    inicio: Fecha,
    fin: z.nullable(Fecha).check(z.describe('null = hasta que alguien lo levante')),
    creado_por: z.string(),
    vigente: z.boolean(),
  }).register(z.globalRegistry, { id: 'Bloqueo' });
export type Bloqueo = z.infer<typeof Bloqueo>;

export const CuerpoBloqueo = z
  .object({
    video_id: z.string().check(z.minLength(1)),
    motivo: z.string().check(z.trim()).check(z.minLength(1, 'Escribe el motivo')),
    duracion: DuracionBloqueo,
    horas: z.nullable(z.int().check(z.minimum(1)).check(z.maximum(168))),
  }).register(z.globalRegistry, { id: 'CuerpoBloqueo' });
export type CuerpoBloqueo = z.infer<typeof CuerpoBloqueo>;
export const CambioBloqueo = z.object({ duracion: DuracionBloqueo, horas: z.nullable(z.int().check(z.minimum(1)).check(z.maximum(168))) }).register(z.globalRegistry, { id: 'CambioBloqueo' });
export const ListaBloqueos = z.object({ bloqueos: z.array(Bloqueo) }).register(z.globalRegistry, { id: 'ListaBloqueos' });

export const CuerpoRetiro = z
  .object({ motivo: z.enum(['inapropiada', 'duplicada', 'otro']), detalle: z.nullable(z.string().check(z.trim()).check(z.maxLength(200))) }).register(z.globalRegistry, { id: 'CuerpoRetiro' });
export type CuerpoRetiro = z.infer<typeof CuerpoRetiro>;

// ── La noche: todo lo de la operación en una sola respuesta ──────────────────

export const ListaCasaPanel = z
  .object({ id: Id, nombre: z.string(), total_canciones: z.int().check(z.nonnegative()), modo_rotacion: z.enum(['secuencial', 'aleatoria']) }).register(z.globalRegistry, { id: 'ListaCasaPanel' });

export const Alerta = z
  .object({
    tipo: z.enum(['sin_lista_activa', 'errores_youtube', 'sin_sesion']),
    detalle: z.nullable(z.string()),
  }).register(z.globalRegistry, { id: 'Alerta' });
export type Alerta = z.infer<typeof Alerta>;

export const Noche = z
  .object({
    version: z.int().check(z.nonnegative()),
    servidor_ahora: Fecha,
    sesion: z.nullable(z
      .object({ id: Id, apertura: Fecha, tipo_evento: z.string(), origen: z.enum(['horario', 'manual']) })),
    servicio: Servicio,
    reproduccion: z.nullable(z.extend(Reproduccion, {
      pedida_por: z.nullable(z.string()).check(z.describe('Código corto del teléfono (p. ej. 7F3K)')),
      pedida_en: z.nullable(Fecha).check(z.describe('Cuándo se pidió («pedida hace 31 min»)')),
    })),
    cola: z.array(z.extend(ItemCola, { pedida_por: z.string(), duracion_seg: Segundos })),
    volumen: Volumen,
    metricas: z.object({
      desde: z.nullable(Fecha),
      solicitudes: z.int().check(z.nonnegative()),
      telefonos: z.int().check(z.nonnegative()),
      votos: z.int().check(z.nonnegative()),
      espera_promedio_seg: z.nullable(Segundos),
    }),
    registro: z.array(AccionAdmin).check(z.describe('Lo de esta noche, lo más reciente primero')),
    horario: z.array(Franja),
    lista_casa: z.nullable(ListaCasaPanel),
    bloqueos: z.array(Bloqueo).check(z.describe('Solo los vigentes')),
    alertas: z.array(Alerta),
  }).register(z.globalRegistry, { id: 'Noche' });
export type Noche = z.infer<typeof Noche>;

export const CuerpoServicio = z.object({ modo_qr: z.enum(['automatico', 'forzado_activo', 'pausado']) }).register(z.globalRegistry, { id: 'CuerpoServicio' });
/** RF-062. Sin `.default()`: el cuerpo tiene que traer el volumen. */
export const CuerpoVolumen = z.object({ volumen: z.int().check(z.minimum(0)).check(z.maximum(100)) }).register(z.globalRegistry, { id: 'CuerpoVolumen' });
export const CuerpoSesion = z.object({ tipo_evento: z.string().check(z.trim()).check(z.minLength(1)) }).register(z.globalRegistry, { id: 'CuerpoSesion' });

// ── Horario (RF-036) ──────────────────────────────────────────────────────────

export const Horario = z.object({ zona_horaria: z.string(), franjas: z.array(Franja) }).register(z.globalRegistry, { id: 'Horario' });
export const CuerpoHorario = z.object({ franjas: z.array(Franja) }).register(z.globalRegistry, { id: 'CuerpoHorario' });

// ── Listas de la casa (RF-027 a RF-033) ───────────────────────────────────────

export const ModoRotacion = z.enum(['secuencial', 'aleatoria']).register(z.globalRegistry, { id: 'ModoRotacion', description: 'aleatoria = sin repetir hasta agotar la lista' });
export const ListaCasa = z
  .object({
    id: Id,
    nombre: z.string(),
    modo_rotacion: ModoRotacion,
    activa: z.boolean(),
    total_canciones: z.int().check(z.nonnegative()),
    duracion_total_seg: Segundos,
    actualizada_en: Fecha,
  }).register(z.globalRegistry, { id: 'ListaCasa' });
export type ListaCasa = z.infer<typeof ListaCasa>;
export const ElementoLista = z
  .object({ elemento_id: Id, cancion: Cancion, orden: z.int().check(z.positive()), veces_reproducida: z.int().check(z.nonnegative()), ultima_reproduccion: z.nullable(Fecha) }).register(z.globalRegistry, { id: 'ElementoLista' });
export const ListaCasaDetalle = z.extend(ListaCasa, { canciones: z.array(ElementoLista) }).register(z.globalRegistry, { id: 'ListaCasaDetalle' });
export type ListaCasaDetalle = z.infer<typeof ListaCasaDetalle>;
export const ListasCasa = z.object({ listas: z.array(ListaCasa) }).register(z.globalRegistry, { id: 'ListasCasa' });
export const CuerpoLista = z.object({ nombre: z.string().check(z.trim()).check(z.minLength(1)).check(z.maxLength(60)) }).register(z.globalRegistry, { id: 'CuerpoLista' });
export const CuerpoCancionLista = z.object({ video_id: z.string().check(z.minLength(1)) }).register(z.globalRegistry, { id: 'CuerpoCancionLista' });
export const CuerpoOrden = z.object({ elementos: z.array(Id) }).register(z.globalRegistry, { id: 'CuerpoOrden' });
export const CuerpoActivacion = z.object({ modo_rotacion: ModoRotacion }).register(z.globalRegistry, { id: 'CuerpoActivacion' });
export const BusquedaPanel = z.object({ consulta: z.string(), resultados: z.array(Cancion) }).register(z.globalRegistry, { id: 'BusquedaPanel' });

// ── Establecimiento y QR (RF-034, RF-035) ─────────────────────────────────────

export const Establecimiento = z
  .object({
    id: Id,
    slug: Slug,
    nombre: z.string().check(z.minLength(1)),
    direccion: z.string(),
    zona_horaria: z.string(),
    estado: z.enum(['activo', 'inactivo']),
    pedidos_maximos: z.int().check(z.minimum(1)).check(z.maximum(10)),
    duracion_max_seg: z.int().check(z.minimum(60)).check(z.maximum(3600)),
    ventana_no_repeticion_min: z.int().check(z.minimum(0)).check(z.maximum(600)),
    tema: z._default(Tema, 'rockola').check(z.describe('RF-059: diseño del teléfono, la TV y el panel; solo lo cambia el Administrador')),
  }).register(z.globalRegistry, { id: 'Establecimiento' });
export type Establecimiento = z.infer<typeof Establecimiento>;
// `tema` sin valor por defecto: un PATCH que no lo trae no debe devolver el bar a Rockola.
export const CambioEstablecimiento = z.partial(z.extend(z.omit(Establecimiento, { id: true, slug: true, estado: true }), { tema: Tema })).register(z.globalRegistry, { id: 'CambioEstablecimiento' });
export const QrEstablecimiento = z
  .object({
    url: z.nullable(z.url()).check(z.describe('null: el bar no tiene un QR que se pueda dibujar y hay que generarlo')),
    token: z.nullable(z.string()),
    generado_en: z.nullable(Fecha),
    destino: DestinoQr.check(z.describe('RF-065: lo que pasa de verdad al escanear')),
    apk_disponible: z.boolean().check(z.describe('Si hay un instalador publicado para elegir `app`')),
  }).register(z.globalRegistry, { id: 'QrEstablecimiento' });
export type QrEstablecimiento = z.infer<typeof QrEstablecimiento>;
export const CuerpoDestinoQr = z.object({ destino: DestinoQr }).register(z.globalRegistry, { id: 'CuerpoDestinoQr' });

// ── Calendario de eventos (RF-045) ────────────────────────────────────────────

export const TipoEvento = z.enum(['normal', 'karaoke', 'partido', 'tematica', 'otro']).register(z.globalRegistry, { id: 'TipoEvento' });
export const EventoBar = z
  .object({ id: Id, nombre: z.string().check(z.minLength(1)), tipo_evento: TipoEvento, inicio: Fecha, fin: Fecha, descripcion: z.nullable(z.string()) }).register(z.globalRegistry, { id: 'EventoBar' });
export type EventoBar = z.infer<typeof EventoBar>;
export const CuerpoEvento = z.omit(EventoBar, { id: true }).register(z.globalRegistry, { id: 'CuerpoEvento' });
export type CuerpoEvento = z.infer<typeof CuerpoEvento>;
export const ListaEventos = z.object({ eventos: z.array(EventoBar) }).register(z.globalRegistry, { id: 'ListaEventos' });
