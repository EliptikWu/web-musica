import { z } from 'zod';
import { Franja, Servicio } from './bar.ts';
import { Cancion } from './cancion.ts';
import { ItemCola, Reproduccion } from './cola.ts';
import { Fecha, Id, Segundos, Slug } from './comunes.ts';

// ── Sesión y usuarios (RF-021 a RF-026) ──────────────────────────────────────

export const Rol = z.enum(['operador', 'administrador']).meta({ id: 'Rol', description: 'En el modelo original: dj y admin' });
export type Rol = z.infer<typeof Rol>;

export const UsuarioPanel = z
  .object({
    id: Id,
    nombre: z.string().min(1),
    correo: z.email(),
    rol: Rol,
    estado: z.enum(['activo', 'inactivo']),
    debe_cambiar_clave: z.boolean(),
    ultimo_acceso: Fecha.nullable(),
  })
  .meta({ id: 'UsuarioPanel', description: 'Nunca lleva la contraseña ni su hash' });
export type UsuarioPanel = z.infer<typeof UsuarioPanel>;

export const YoPanel = z
  .object({
    usuario: UsuarioPanel,
    establecimiento: z.object({ slug: Slug, nombre: z.string(), zona_horaria: z.string().describe('IANA, p. ej. America/Bogota') }),
  })
  .meta({ id: 'YoPanel' });
export type YoPanel = z.infer<typeof YoPanel>;

export const CuerpoEntrar = z.object({ correo: z.email(), clave: z.string().min(1) }).meta({ id: 'CuerpoEntrar' });
export const CuerpoCambioClave = z
  .object({ actual: z.string().min(1), nueva: z.string().min(10, 'Al menos 10 caracteres') })
  .meta({ id: 'CuerpoCambioClave', description: 'Mínimo de 10 caracteres (propuesto)' });

export const CuerpoUsuario = z.object({ nombre: z.string().trim().min(1), correo: z.email(), rol: Rol }).meta({ id: 'CuerpoUsuario' });
export const CambioUsuario = CuerpoUsuario.partial().meta({ id: 'CambioUsuario' });
export const UsuarioCreado = z
  .object({ usuario: UsuarioPanel, clave_temporal: z.string().describe('Se muestra una sola vez; obliga a cambiarla al entrar') })
  .meta({ id: 'UsuarioCreado' });
export const ClaveTemporal = z.object({ clave_temporal: z.string() }).meta({ id: 'ClaveTemporal' });
export const ListaUsuarios = z.object({ usuarios: z.array(UsuarioPanel) }).meta({ id: 'ListaUsuarios' });

// ── Bitácora (RF-044, RN-11) ──────────────────────────────────────────────────

export const TipoAccion = z
  .enum([
    'saltar', 'bloquear', 'desbloquear', 'extender_bloqueo', 'retirar',
    'pausar_solicitudes', 'reanudar_solicitudes', 'forzar_apertura',
    'pausar_reproduccion', 'reanudar_reproduccion',
    'mantenimiento', 'abrir_sesion', 'cerrar_sesion',
    'editar_horario', 'editar_lista', 'activar_lista',
    'crear_usuario', 'editar_usuario', 'desactivar_usuario', 'reactivar_usuario', 'restablecer_clave',
    'editar_establecimiento', 'regenerar_qr', 'editar_evento',
    'error_youtube', 'intento_no_permitido',
  ])
  .meta({ id: 'TipoAccion' });
export type TipoAccion = z.infer<typeof TipoAccion>;

export const AccionAdmin = z
  .object({
    id: Id,
    fecha: Fecha,
    usuario: z.object({ id: Id, nombre: z.string() }).nullable().describe('null = lo hizo el sistema'),
    tipo: TipoAccion,
    objetivo: z
      .object({
        tipo: z.enum(['cancion', 'usuario', 'lista', 'horario', 'servicio', 'sesion', 'establecimiento', 'evento']),
        nombre: z.string(),
        detalle: z.string().nullable().describe('Para una canción, el artista'),
      })
      .nullable(),
    detalle: z.string().nullable().describe('Motivo, duración o mensaje de error'),
  })
  .meta({ id: 'AccionAdmin' });
export type AccionAdmin = z.infer<typeof AccionAdmin>;

export const PaginaBitacora = z
  .object({ acciones: z.array(AccionAdmin), siguiente: z.string().nullable() })
  .meta({ id: 'PaginaBitacora' });

// ── Bloqueos (RF-042, RF-043) ─────────────────────────────────────────────────

export const DuracionBloqueo = z.enum(['fin_sesion', 'horas', 'indefinido']).meta({ id: 'DuracionBloqueo' });
export type DuracionBloqueo = z.infer<typeof DuracionBloqueo>;

export const Bloqueo = z
  .object({
    id: Id,
    cancion: Cancion,
    motivo: z.string(),
    duracion: DuracionBloqueo,
    horas: z.int().positive().nullable(),
    inicio: Fecha,
    fin: Fecha.nullable().describe('null = hasta que alguien lo levante'),
    creado_por: z.string(),
    vigente: z.boolean(),
  })
  .meta({ id: 'Bloqueo' });
export type Bloqueo = z.infer<typeof Bloqueo>;

export const CuerpoBloqueo = z
  .object({
    video_id: z.string().min(1),
    motivo: z.string().trim().min(1, 'Escribe el motivo'),
    duracion: DuracionBloqueo,
    horas: z.int().min(1).max(168).nullable(),
  })
  .meta({ id: 'CuerpoBloqueo' });
export type CuerpoBloqueo = z.infer<typeof CuerpoBloqueo>;
export const CambioBloqueo = z.object({ duracion: DuracionBloqueo, horas: z.int().min(1).max(168).nullable() }).meta({ id: 'CambioBloqueo' });
export const ListaBloqueos = z.object({ bloqueos: z.array(Bloqueo) }).meta({ id: 'ListaBloqueos' });

export const CuerpoRetiro = z
  .object({ motivo: z.enum(['inapropiada', 'duplicada', 'otro']), detalle: z.string().trim().max(200).nullable() })
  .meta({ id: 'CuerpoRetiro' });
export type CuerpoRetiro = z.infer<typeof CuerpoRetiro>;

// ── La noche: todo lo de la operación en una sola respuesta ──────────────────

export const ListaCasaPanel = z
  .object({ id: Id, nombre: z.string(), total_canciones: z.int().nonnegative(), modo_rotacion: z.enum(['secuencial', 'aleatoria']) })
  .meta({ id: 'ListaCasaPanel' });

export const Alerta = z
  .object({
    tipo: z.enum(['sin_lista_activa', 'errores_youtube', 'sin_sesion']),
    detalle: z.string().nullable(),
  })
  .meta({ id: 'Alerta' });
export type Alerta = z.infer<typeof Alerta>;

export const Noche = z
  .object({
    version: z.int().nonnegative(),
    servidor_ahora: Fecha,
    sesion: z
      .object({ id: Id, apertura: Fecha, tipo_evento: z.string(), origen: z.enum(['horario', 'manual']) })
      .nullable(),
    servicio: Servicio,
    reproduccion: Reproduccion.extend({
      pedida_por: z.string().nullable().describe('Código corto del teléfono (p. ej. 7F3K)'),
      pedida_en: Fecha.nullable().describe('Cuándo se pidió («pedida hace 31 min»)'),
    }).nullable(),
    cola: z.array(ItemCola.extend({ pedida_por: z.string(), duracion_seg: Segundos })),
    metricas: z.object({
      desde: Fecha.nullable(),
      solicitudes: z.int().nonnegative(),
      telefonos: z.int().nonnegative(),
      votos: z.int().nonnegative(),
      espera_promedio_seg: Segundos.nullable(),
    }),
    registro: z.array(AccionAdmin).describe('Lo de esta noche, lo más reciente primero'),
    horario: z.array(Franja),
    lista_casa: ListaCasaPanel.nullable(),
    bloqueos: z.array(Bloqueo).describe('Solo los vigentes'),
    alertas: z.array(Alerta),
  })
  .meta({ id: 'Noche' });
export type Noche = z.infer<typeof Noche>;

export const CuerpoServicio = z.object({ modo_qr: z.enum(['automatico', 'forzado_activo', 'pausado']) }).meta({ id: 'CuerpoServicio' });
export const CuerpoSesion = z.object({ tipo_evento: z.string().trim().min(1) }).meta({ id: 'CuerpoSesion' });

// ── Horario (RF-036) ──────────────────────────────────────────────────────────

export const Horario = z.object({ zona_horaria: z.string(), franjas: z.array(Franja) }).meta({ id: 'Horario' });
export const CuerpoHorario = z.object({ franjas: z.array(Franja) }).meta({ id: 'CuerpoHorario' });

// ── Listas de la casa (RF-027 a RF-033) ───────────────────────────────────────

export const ModoRotacion = z.enum(['secuencial', 'aleatoria']).meta({ id: 'ModoRotacion', description: 'aleatoria = sin repetir hasta agotar la lista' });
export const ListaCasa = z
  .object({
    id: Id,
    nombre: z.string(),
    modo_rotacion: ModoRotacion,
    activa: z.boolean(),
    total_canciones: z.int().nonnegative(),
    duracion_total_seg: Segundos,
    actualizada_en: Fecha,
  })
  .meta({ id: 'ListaCasa' });
export type ListaCasa = z.infer<typeof ListaCasa>;
export const ElementoLista = z
  .object({ elemento_id: Id, cancion: Cancion, orden: z.int().positive(), veces_reproducida: z.int().nonnegative(), ultima_reproduccion: Fecha.nullable() })
  .meta({ id: 'ElementoLista' });
export const ListaCasaDetalle = ListaCasa.extend({ canciones: z.array(ElementoLista) }).meta({ id: 'ListaCasaDetalle' });
export type ListaCasaDetalle = z.infer<typeof ListaCasaDetalle>;
export const ListasCasa = z.object({ listas: z.array(ListaCasa) }).meta({ id: 'ListasCasa' });
export const CuerpoLista = z.object({ nombre: z.string().trim().min(1).max(60) }).meta({ id: 'CuerpoLista' });
export const CuerpoCancionLista = z.object({ video_id: z.string().min(1) }).meta({ id: 'CuerpoCancionLista' });
export const CuerpoOrden = z.object({ elementos: z.array(Id) }).meta({ id: 'CuerpoOrden' });
export const CuerpoActivacion = z.object({ modo_rotacion: ModoRotacion }).meta({ id: 'CuerpoActivacion' });
export const BusquedaPanel = z.object({ consulta: z.string(), resultados: z.array(Cancion) }).meta({ id: 'BusquedaPanel' });

// ── Establecimiento y QR (RF-034, RF-035) ─────────────────────────────────────

export const Establecimiento = z
  .object({
    id: Id,
    slug: Slug,
    nombre: z.string().min(1),
    direccion: z.string(),
    zona_horaria: z.string(),
    estado: z.enum(['activo', 'inactivo']),
    pedidos_maximos: z.int().min(1).max(10),
    duracion_max_seg: z.int().min(60).max(3600),
    ventana_no_repeticion_min: z.int().min(0).max(600),
  })
  .meta({ id: 'Establecimiento' });
export type Establecimiento = z.infer<typeof Establecimiento>;
export const CambioEstablecimiento = Establecimiento.omit({ id: true, slug: true, estado: true }).partial().meta({ id: 'CambioEstablecimiento' });
export const QrEstablecimiento = z.object({ url: z.url(), token: z.string(), generado_en: Fecha }).meta({ id: 'QrEstablecimiento' });

// ── Calendario de eventos (RF-045) ────────────────────────────────────────────

export const TipoEvento = z.enum(['normal', 'karaoke', 'partido', 'tematica', 'otro']).meta({ id: 'TipoEvento' });
export const EventoBar = z
  .object({ id: Id, nombre: z.string().min(1), tipo_evento: TipoEvento, inicio: Fecha, fin: Fecha, descripcion: z.string().nullable() })
  .meta({ id: 'EventoBar' });
export type EventoBar = z.infer<typeof EventoBar>;
export const CuerpoEvento = EventoBar.omit({ id: true }).meta({ id: 'CuerpoEvento' });
export type CuerpoEvento = z.infer<typeof CuerpoEvento>;
export const ListaEventos = z.object({ eventos: z.array(EventoBar) }).meta({ id: 'ListaEventos' });
