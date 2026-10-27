import type { AccionAdmin, DuracionBloqueo, EventoBar, Franja, ModoQr, Rol, TipoAccion, UmbralesAnalitica } from '@rockola/contrato';

/**
 * Estado interno de la simulación: se parece más a la base de datos que al contrato. Las
 * respuestas se construyen a partir de él (motor.ts). Fechas en milisegundos de la hora
 * simulada; se pasan a ISO solo al responder.
 */

export type CancionSim = {
  video_id: string;
  titulo: string;
  artista: string;
  duracion_seg: number;
  embebible: boolean;
  disponibilidad: 'disponible' | 'no_disponible' | 'restringido';
  genero: string;
};

export type EstadoSolicitudSim = 'pendiente' | 'reproduciendo' | 'reproducida' | 'saltada' | 'no_disponible' | 'bloqueada' | 'retirada' | 'expirada';

export type SolicitudSim = {
  id: string;
  sesion_id: string;
  video_id: string;
  dispositivo: string;
  fecha: number;
  votos: string[];
  estado: EstadoSolicitudSim;
  inicio: number | null;
  fin: number | null;
  motivo: string | null;
  /** Lo que subió en el último reordenamiento por votos, y cuándo. */
  subida: { puestos: number; en: number } | null;
};

export type ReproduccionSim = {
  id: string;
  video_id: string;
  origen: 'solicitud' | 'lista_casa';
  solicitud_id: string | null;
  inicio: number;
  /** Segundos ya reproducidos antes de la última reanudación. */
  acumulado_seg: number;
  /** Desde cuándo suena sin pausa; null = en pausa. */
  reanudada_en: number | null;
};

export type HistorialSim = {
  video_id: string;
  origen: 'solicitud' | 'lista_casa';
  solicitud_id: string | null;
  sesion_id: string | null;
  inicio: number;
  fin: number;
  resultado: 'completa' | 'saltada_admin' | 'error_video';
};

export type ListaSim = {
  id: string;
  nombre: string;
  modo_rotacion: 'secuencial' | 'aleatoria';
  activa: boolean;
  actualizada_en: number;
  elementos: { elemento_id: string; video_id: string; veces: number; ultima: number | null }[];
  /** Próximo índice (secuencial) o lo que queda por sonar en esta vuelta (aleatoria). */
  indice: number;
  pendientes_aleatorios: string[];
};

export type BloqueoSim = {
  id: string;
  video_id: string;
  motivo: string;
  duracion: DuracionBloqueo;
  horas: number | null;
  inicio: number;
  fin: number | null;
  creado_por: string;
  levantado: boolean;
};

export type UsuarioSim = {
  id: string;
  nombre: string;
  correo: string;
  rol: Rol;
  estado: 'activo' | 'inactivo';
  clave: string;
  debe_cambiar_clave: boolean;
  ultimo_acceso: number | null;
  intentos: number;
  bloqueado_hasta: number | null;
};

export type SesionNocheSim = { id: string; apertura: number; tipo_evento: string; origen: 'horario' | 'manual' };

export type AccionSim = Omit<AccionAdmin, 'fecha'> & { fecha: number };

export type EstadoSim = {
  esquema: 1;
  escenario: string;
  version: number;
  /** Reloj virtual: hora simulada = ancla_sim + (ahora real − ancla_real) × velocidad. */
  reloj: { ancla_sim: number; ancla_real: number; velocidad: number };
  bar: {
    id: string;
    slug: string;
    nombre: string;
    direccion: string;
    zona_horaria: string;
    estado: 'activo' | 'inactivo';
    pedidos_maximos: number;
    duracion_max_seg: number;
    ventana_no_repeticion_min: number;
  };
  modo_qr: ModoQr;
  mantenimiento: boolean;
  horario: Franja[];
  sesion: SesionNocheSim | null;
  /** Si el personal cerró la noche a mano dentro del horario, no se reabre sola hasta esta hora. */
  sin_apertura_automatica_hasta: number | null;
  catalogo: Record<string, CancionSim>;
  solicitudes: SolicitudSim[];
  reproduccion: ReproduccionSim | null;
  historial: HistorialSim[];
  listas: ListaSim[];
  bloqueos: BloqueoSim[];
  acciones: AccionSim[];
  usuarios: UsuarioSim[];
  sesiones_panel: Record<string, string>;
  dispositivos: Record<string, { codigo: string; tipo: string; primer_acceso: number; aviso: number | null }>;
  idempotencia: Record<string, { estado: number; cuerpo: unknown; en: number }>;
  eventos: EventoBar[];
  /** Umbrales del diagnóstico del tablero (RF-052); sin ellos, los de `analitica.ts`. */
  umbrales_analitica?: UmbralesAnalitica;
  qr: { token: string; generado_en: number; anteriores: string[] };
  /** Contadores de la noche (datos de ejemplo al sembrar; suben con cada acción). */
  metricas: { solicitudes: number; telefonos: number; votos: number; espera_seg_suma: number; espera_n: number };
  /** Conmutadores de escenario para probar fallos. */
  fallos: { youtube: 'bien' | 'caido' | 'cuota'; conexion: 'bien' | 'cortada' };
  secuencia: number;
};

export type Contexto = { ahora: number; estado: EstadoSim };
export type { TipoAccion };
