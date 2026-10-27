import type { Franja } from '@rockola/contrato';
import type { AccionSim, BloqueoSim, EstadoSim, ListaSim, SolicitudSim } from '../tipos';
import { type Escenario } from '../escenarios';
import { CATALOGO, LISTA_SALSA_Y_TROPICAL, LISTA_VIERNES_CROSSOVER } from './catalogo';

export { ESCENARIOS, esEscenario, type Escenario } from '../escenarios';

/**
 * Las noches de ejemplo (prototipos/herramientas/datos-de-ejemplo.md). Votos, horas y
 * métricas son **datos de ejemplo**: la interfaz los rotula donde puedan confundirse.
 *
 * El teléfono de quien abre la simulación es «el de la persona» de los prototipos: pidió
 * Rebelión (la que suena, «teléfono 7F3K» en el panel) y Monaco, y votó por DtMF. Por eso le
 * quedan 2 de 3 pedidos antes de pedir Monaco: en la simulación el cupo cuenta las pendientes
 * y la que suena (TBD con el backend, doc 04).
 */

/** Hora de Bogotá (UTC−5, sin horario de verano) a milisegundos. */
const bogota = (fecha: string, hora: string) => Date.parse(`${fecha}T${hora}-05:00`);
const JUEVES = '2026-10-22';
const MIN = 60_000;

export const CODIGO_PERSONA = '7F3K';

const HORARIO: Franja[] = [4, 5, 6].map((dia) => ({ dia_semana: dia, hora_inicio: '20:00', hora_fin: '02:00', activo: true }));

/** Votantes de relleno: teléfonos de otras mesas. */
const otros = (n: number, desde = 0) => Array.from({ length: n }, (_, i) => `tel-${String(desde + i + 1).padStart(3, '0')}`);

function solicitud(id: string, video: string, dispositivo: string, fecha: number, votos: string[], extra: Partial<SolicitudSim> = {}): SolicitudSim {
  return { id, sesion_id: 'n-2026-10-22', video_id: `sim-${video}`, dispositivo, fecha, votos, estado: 'pendiente', inicio: null, fin: null, motivo: null, subida: null, ...extra };
}

function lista(id: string, nombre: string, videos: string[], activa: boolean, actualizada: number): ListaSim {
  return {
    id,
    nombre,
    modo_rotacion: 'secuencial',
    activa,
    actualizada_en: actualizada,
    elementos: videos.map((video_id, i) => ({ elemento_id: `${id}-e${i + 1}`, video_id, veces: 0, ultima: null })),
    indice: 0,
    pendientes_aleatorios: [],
  };
}

/** Momento B: jueves 10:45 p. m. (cola, TV y panel de los prototipos). */
function momentoB(persona: string): { estado: EstadoSim; ancla: number } {
  const ahora = bogota(JUEVES, '22:45:00');
  const hace = (min: number) => ahora - min * MIN;
  const p = persona;

  const solicitudes: SolicitudSim[] = [
    // Sonó a las 9:40 p. m. (para «Lo más votado de la noche»).
    solicitud('s-hips', 'hips-dont-lie', 'tel-090', bogota(JUEVES, '21:05:00'), ['tel-090', ...otros(18, 60)], { estado: 'reproducida', inicio: bogota(JUEVES, '21:40:00'), fin: bogota(JUEVES, '21:43:38') }),
    // Saltada y bloqueada a las 10:32 p. m.
    solicitud('s-gasolina', 'gasolina', 'tel-091', bogota(JUEVES, '22:05:00'), otros(6, 70), { estado: 'saltada', inicio: bogota(JUEVES, '22:31:10'), fin: bogota(JUEVES, '22:32:00') }),
    // La que suena: la pidió la persona hace 31 min.
    solicitud('s-rebelion', 'rebelion', p, hace(31), [p, ...otros(23)], { estado: 'reproduciendo', inicio: ahora - 161_000 }),
    solicitud('s-cali', 'cali-pachanguero', 'tel-030', hace(26), ['tel-030', ...otros(20, 30)]),
    solicitud('s-dtmf', 'dtmf', 'tel-041', hace(31), ['tel-041', p, ...otros(16, 41)], { subida: { puestos: 1, en: hace(1) } }),
    solicitud('s-monaco', 'monaco', p, hace(14), [p, ...otros(14, 2)], { subida: { puestos: 2, en: hace(1) } }),
    solicitud('s-bici', 'la-bicicleta', 'tel-050', hace(9), ['tel-050', ...otros(14, 50)]),
    solicitud('s-nuevayol', 'nuevayol', 'tel-020', hace(22), ['tel-020', ...otros(13, 20)]),
    solicitud('s-tqg', 'tqg', 'tel-060', hace(7), ['tel-060', ...otros(8, 60)]),
    solicitud('s-luna', 'luna', 'tel-070', hace(5), ['tel-070', ...otros(6, 70)]),
    solicitud('s-aventura', 'una-aventura', 'tel-080', hace(4), ['tel-080', ...otros(3, 80)]),
    solicitud('s-titi', 'titi-me-pregunto', 'tel-085', hace(2), ['tel-085', 'tel-086']),
  ];

  const admin = 'Admin de prueba';
  const bloqueos: BloqueoSim[] = [
    { id: 'b-bachata', video_id: 'sim-la-bachata', motivo: 'No va con la noche de salsa', duracion: 'fin_sesion', horas: null, inicio: bogota(JUEVES, '22:41:00'), fin: null, creado_por: admin, levantado: false },
    { id: 'b-gasolina', video_id: 'sim-gasolina', motivo: 'Pedida tres veces seguidas', duracion: 'fin_sesion', horas: null, inicio: bogota(JUEVES, '22:32:05'), fin: null, creado_por: admin, levantado: false },
    { id: 'b-despecha', video_id: 'sim-despecha', motivo: 'Fuera del estilo del bar', duracion: 'indefinido', horas: null, inicio: bogota('2026-10-16', '23:10:00'), fin: null, creado_por: admin, levantado: false },
  ];

  const usuarioAdmin = { id: 'u-admin', nombre: admin };
  const acciones: AccionSim[] = [
    { id: 'a-1', fecha: bogota(JUEVES, '20:00:00'), usuario: null, tipo: 'abrir_sesion', objetivo: { tipo: 'sesion', nombre: 'Noche del jueves', detalle: null }, detalle: 'horario' },
    { id: 'a-2', fecha: bogota(JUEVES, '22:32:00'), usuario: usuarioAdmin, tipo: 'saltar', objetivo: { tipo: 'cancion', nombre: 'Gasolina', detalle: 'Daddy Yankee' }, detalle: null },
    { id: 'a-3', fecha: bogota(JUEVES, '22:32:05'), usuario: usuarioAdmin, tipo: 'bloquear', objetivo: { tipo: 'cancion', nombre: 'Gasolina', detalle: 'Daddy Yankee' }, detalle: 'Hasta el fin de la noche' },
    { id: 'a-4', fecha: bogota(JUEVES, '22:41:00'), usuario: usuarioAdmin, tipo: 'bloquear', objetivo: { tipo: 'cancion', nombre: 'La Bachata', detalle: 'Manuel Turizo' }, detalle: 'Hasta el fin de la noche' },
  ];

  const casa = lista('l-salsa', 'Salsa y tropical de la casa', LISTA_SALSA_Y_TROPICAL, true, bogota('2026-10-15', '18:20:00'));
  const crossover = lista('l-crossover', 'Viernes crossover', LISTA_VIERNES_CROSSOVER, false, bogota('2026-10-09', '17:05:00'));

  const estado: EstadoSim = {
    esquema: 1,
    escenario: 'momento-b',
    version: 1,
    reloj: { ancla_sim: ahora, ancla_real: 0, velocidad: 1 },
    bar: {
      id: 'e-demo',
      slug: 'demo',
      nombre: 'Bar de demostración',
      direccion: 'Dirección de ejemplo, Armenia (Quindío)',
      zona_horaria: 'America/Bogota',
      estado: 'activo',
      pedidos_maximos: 3,
      duracion_max_seg: 600,
      ventana_no_repeticion_min: 60,
    },
    modo_qr: 'automatico',
    mantenimiento: false,
    horario: HORARIO,
    sesion: { id: 'n-2026-10-22', apertura: bogota(JUEVES, '20:00:00'), tipo_evento: 'normal', origen: 'horario' },
    sin_apertura_automatica_hasta: null,
    catalogo: structuredClone(CATALOGO),
    solicitudes,
    reproduccion: { id: 'r-rebelion', video_id: 'sim-rebelion', origen: 'solicitud', solicitud_id: 's-rebelion', inicio: ahora - 161_000, acumulado_seg: 0, reanudada_en: ahora - 161_000 },
    historial: [
      { video_id: 'sim-hips-dont-lie', origen: 'solicitud', solicitud_id: 's-hips', sesion_id: 'n-2026-10-22', inicio: bogota(JUEVES, '21:40:00'), fin: bogota(JUEVES, '21:43:38'), resultado: 'completa' },
      { video_id: 'sim-gasolina', origen: 'solicitud', solicitud_id: 's-gasolina', sesion_id: 'n-2026-10-22', inicio: bogota(JUEVES, '22:31:10'), fin: bogota(JUEVES, '22:32:00'), resultado: 'saltada_admin' },
    ],
    listas: [casa, crossover],
    bloqueos,
    acciones,
    usuarios: [
      { id: 'u-admin', nombre: admin, correo: 'admin@rockola.demo', rol: 'administrador', estado: 'activo', clave: 'rockola-demo', debe_cambiar_clave: false, ultimo_acceso: bogota(JUEVES, '19:55:00'), intentos: 0, bloqueado_hasta: null },
      { id: 'u-operador', nombre: 'Operador de prueba', correo: 'operador@rockola.demo', rol: 'operador', estado: 'activo', clave: 'rockola-demo', debe_cambiar_clave: false, ultimo_acceso: bogota(JUEVES, '20:10:00'), intentos: 0, bloqueado_hasta: null },
      { id: 'u-nuevo', nombre: 'Mesero nuevo', correo: 'nuevo@rockola.demo', rol: 'operador', estado: 'activo', clave: 'temporal-demo', debe_cambiar_clave: true, ultimo_acceso: null, intentos: 0, bloqueado_hasta: null },
    ],
    sesiones_panel: {},
    dispositivos: { [p]: { codigo: CODIGO_PERSONA, tipo: 'movil', primer_acceso: hace(40), aviso: hace(40) } },
    idempotencia: {},
    // Eventos de ejemplo (genéricos: no son eventos reales del bar).
    eventos: [
      { id: 'ev-1', nombre: 'Karaoke de los miércoles', tipo_evento: 'karaoke', inicio: '2026-10-21T20:00:00-05:00', fin: '2026-10-22T01:00:00-05:00', descripcion: 'La rockola se usa para el karaoke.' },
      { id: 'ev-2', nombre: 'Transmisión del partido', tipo_evento: 'partido', inicio: '2026-10-18T15:00:00-05:00', fin: '2026-10-18T18:00:00-05:00', descripcion: null },
      { id: 'ev-3', nombre: 'Noche de salsa en vivo', tipo_evento: 'tematica', inicio: '2026-10-30T20:00:00-05:00', fin: '2026-10-31T02:00:00-05:00', descripcion: 'Orquesta en vivo de 10:00 p. m. a 12:00 a. m.' },
    ],
    qr: { token: 'demo-qr-1', generado_en: bogota('2026-10-01', '17:00:00'), anteriores: [] },
    // Lo que ya pasó desde las 8:00 p. m. Cuadra con el panel del prototipo: 48, 31, 162 y 11 min.
    metricas: { solicitudes: 48, telefonos: 31, votos: 162, espera_seg_suma: 38 * 660, espera_n: 38 },
    fallos: { youtube: 'bien', conexion: 'bien' },
    secuencia: 100,
  };
  return { estado, ancla: ahora };
}

/** Momento A: jueves 10:24 p. m. (buscar de los prototipos; también la bienvenida). */
function momentoA(persona: string): { estado: EstadoSim; ancla: number } {
  const { estado } = momentoB(persona);
  const ahora = bogota(JUEVES, '22:24:00');
  const hace = (min: number) => ahora - min * MIN;
  const p = persona;
  const inicioRebelion = ahora - (358 - 98) * 1000; // le quedan 1:38
  estado.solicitudes = [
    estado.solicitudes.find((s) => s.id === 's-hips')!,
    solicitud('s-rebelion', 'rebelion', p, hace(10), [p, ...otros(23)], { estado: 'reproduciendo', inicio: inicioRebelion }),
    solicitud('s-cali', 'cali-pachanguero', 'tel-030', hace(5), ['tel-030', ...otros(14, 30)]),
    solicitud('s-nuevayol', 'nuevayol', 'tel-020', hace(1), ['tel-020', ...otros(13, 20)]),
    solicitud('s-dtmf', 'dtmf', 'tel-041', hace(10), ['tel-041', p, ...otros(11, 41)]),
  ];
  estado.reproduccion = { id: 'r-rebelion', video_id: 'sim-rebelion', origen: 'solicitud', solicitud_id: 's-rebelion', inicio: inicioRebelion, acumulado_seg: 0, reanudada_en: inicioRebelion };
  estado.historial = estado.historial.filter((h) => h.solicitud_id === 's-hips');
  estado.bloqueos = estado.bloqueos.filter((b) => b.id === 'b-despecha');
  estado.acciones = estado.acciones.filter((a) => a.id === 'a-1');
  estado.metricas = { solicitudes: 39, telefonos: 27, votos: 131, espera_seg_suma: 33 * 640, espera_n: 33 };
  estado.reloj.ancla_sim = ahora;
  return { estado, ancla: ahora };
}

/** Crea el estado de un escenario. `persona` es el ID del teléfono que abre la simulación. */
export function crearEstado(escenario: Escenario, persona: string, ahoraReal: number, velocidad = 1): EstadoSim {
  const base = escenario === 'momento-a' ? momentoA(persona) : momentoB(persona);
  const estado = base.estado;
  estado.escenario = escenario;
  estado.reloj = { ancla_sim: base.ancla, ancla_real: ahoraReal, velocidad };

  const pendientesFuera = () => {
    for (const s of estado.solicitudes) if (s.estado === 'pendiente') s.estado = 'expirada';
  };

  switch (escenario) {
    case 'fuera-de-horario': {
      // Jueves 6:30 p. m.: aún no abre. Suena la lista de la casa.
      const ahora = bogota(JUEVES, '18:30:00');
      estado.reloj.ancla_sim = ahora;
      estado.sesion = null;
      estado.solicitudes = estado.solicitudes.filter(() => false);
      estado.historial = [];
      estado.acciones = [];
      estado.bloqueos = estado.bloqueos.filter((b) => b.id === 'b-despecha');
      estado.reproduccion = { id: 'r-casa-1', video_id: 'sim-gotas-de-lluvia', origen: 'lista_casa', solicitud_id: null, inicio: ahora - 70_000, acumulado_seg: 0, reanudada_en: ahora - 70_000 };
      estado.listas[0]!.indice = 4;
      estado.metricas = { solicitudes: 0, telefonos: 0, votos: 0, espera_seg_suma: 0, espera_n: 0 };
      break;
    }
    case 'en-pausa': {
      estado.modo_qr = 'pausado';
      const ahora = estado.reloj.ancla_sim;
      estado.acciones.push({ id: 'a-5', fecha: ahora - MIN, usuario: { id: 'u-admin', nombre: 'Admin de prueba' }, tipo: 'pausar_solicitudes', objetivo: { tipo: 'servicio', nombre: 'Solicitudes', detalle: null }, detalle: null });
      break;
    }
    case 'mantenimiento': {
      estado.mantenimiento = true;
      estado.sesion = null;
      pendientesFuera();
      break;
    }
    case 'cola-vacia': {
      for (const s of estado.solicitudes) if (s.estado === 'pendiente') s.estado = 'reproducida';
      break;
    }
    case 'sin-pedidos': {
      // La persona ya tiene la que suena y dos más en la cola: 0 de 3.
      for (const id of ['s-tqg', 's-luna']) {
        const s = estado.solicitudes.find((x) => x.id === id)!;
        s.dispositivo = persona;
        s.votos[0] = persona;
      }
      break;
    }
    case 'sin-lista-activa': {
      for (const l of estado.listas) l.activa = false;
      for (const s of estado.solicitudes) if (s.estado === 'pendiente') s.estado = 'reproducida';
      break;
    }
    case 'youtube-caido':
      estado.fallos.youtube = 'caido';
      break;
    case 'cuota-agotada':
      estado.fallos.youtube = 'cuota';
      break;
    case 'desconexion':
      estado.fallos.conexion = 'cortada';
      break;
    default:
      break;
  }
  return estado;
}
