import type {
  Ahora, Busqueda, CodigoError, Destacada, EstadoServicio, ItemCola, MiSolicitud, MotivoNoPedible, Noche, RespuestaSolicitud,
  RespuestaVoto, Servicio, Yo,
} from '@rockola/contrato';
import { abiertoPorHorario, cierreDeFranja, franjaVigente, proximaApertura } from '../lib/reglas/horario';
import type { AccionSim, BloqueoSim, CancionSim, EstadoSim, ReproduccionSim, SolicitudSim, TipoAccion } from './tipos';

/**
 * Motor de la simulación: funciones puras sobre `EstadoSim`. Hace en el navegador lo que hará
 * el backend (doc 04) para que la interfaz se pueda usar sin él. Lo que muta llama a `tocar`,
 * que sube la versión: así las pestañas saben que hay una foto nueva.
 */

export type Resultado<T> =
  | { ok: true; estado: 200 | 201 | 204; cuerpo: T }
  | { ok: false; estado: number; codigo: CodigoError; mensaje: string; detalle?: Record<string, unknown> };

const bien = <T>(cuerpo: T, estado: 200 | 201 | 204 = 200): Resultado<T> => ({ ok: true, estado, cuerpo });
const mal = (estado: number, codigo: CodigoError, mensaje: string, detalle?: Record<string, unknown>): Resultado<never> => ({ ok: false, estado, codigo, mensaje, detalle });

const VIGENCIA_SUBIDA_MS = 10 * 60_000; // «Subió N puestos» se ve 10 min (propuesto)
const iso = (ms: number) => new Date(ms).toISOString();

// ── Reloj ────────────────────────────────────────────────────────────────────

export function ahoraSim(estado: EstadoSim, ahoraReal: number): number {
  const { ancla_sim, ancla_real, velocidad } = estado.reloj;
  return Math.floor(ancla_sim + (ahoraReal - ancla_real) * velocidad);
}

export function tocar(estado: EstadoSim): void {
  estado.version++;
}

function nuevoId(estado: EstadoSim, prefijo: string): string {
  estado.secuencia++;
  return `${prefijo}-${estado.secuencia}`;
}

// ── Consultas internas ───────────────────────────────────────────────────────

export function cancion(estado: EstadoSim, videoId: string): CancionSim {
  return estado.catalogo[videoId] ?? { video_id: videoId, titulo: videoId, artista: '', duracion_seg: 0, embebible: false, disponibilidad: 'no_disponible', genero: 'sin_clasificar' };
}

function aContrato(c: CancionSim) {
  return { video_id: c.video_id, titulo: c.titulo, artista: c.artista, duracion_seg: c.duracion_seg, miniatura_url: null };
}

export function bloqueoVigente(estado: EstadoSim, videoId: string, ahora: number): BloqueoSim | null {
  return estado.bloqueos.find((b) => b.video_id === videoId && !b.levantado && b.inicio <= ahora && (b.fin === null || b.fin > ahora)) ?? null;
}

function deLaSesion(estado: EstadoSim): SolicitudSim[] {
  const id = estado.sesion?.id;
  return id ? estado.solicitudes.filter((s) => s.sesion_id === id) : [];
}

/** Pendientes ordenadas por RN-05. */
export function pendientes(estado: EstadoSim): SolicitudSim[] {
  return deLaSesion(estado)
    .filter((s) => s.estado === 'pendiente')
    .sort((a, b) => b.votos.length - a.votos.length || a.fecha - b.fecha);
}

/** Cupo usado: las pendientes y la que suena (TBD con el backend, doc 04). */
function cupoUsado(estado: EstadoSim, dispositivo: string): number {
  return deLaSesion(estado).filter((s) => s.dispositivo === dispositivo && (s.estado === 'pendiente' || s.estado === 'reproduciendo')).length;
}

export function posicionSeg(rep: ReproduccionSim, ahora: number): number {
  return rep.acumulado_seg + (rep.reanudada_en === null ? 0 : (ahora - rep.reanudada_en) / 1000);
}

function restanteSeg(estado: EstadoSim, ahora: number): number {
  const rep = estado.reproduccion;
  if (!rep) return 0;
  return Math.max(0, cancion(estado, rep.video_id).duracion_seg - posicionSeg(rep, ahora));
}

/** Código corto del teléfono para el panel («7F3K»). Seudónimo: no deja ver el ID. */
export function codigoTelefono(estado: EstadoSim, dispositivo: string): string {
  const registrado = estado.dispositivos[dispositivo]?.codigo;
  if (registrado) return registrado;
  const alfabeto = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let h = 2166136261;
  for (const c of dispositivo) h = Math.imul(h ^ c.charCodeAt(0), 16777619);
  let codigo = '';
  for (let i = 0; i < 4; i++) {
    codigo += alfabeto[(h >>> (i * 5)) & 31];
  }
  return codigo;
}

function accion(estado: EstadoSim, ahora: number, usuario: { id: string; nombre: string } | null, tipo: TipoAccion, objetivo: AccionSim['objetivo'], detalle: string | null = null) {
  estado.acciones.push({ id: nuevoId(estado, 'a'), fecha: ahora, usuario, tipo, objetivo, detalle });
}

const objetivoCancion = (c: CancionSim): AccionSim['objetivo'] => ({ tipo: 'cancion', nombre: c.titulo, detalle: c.artista });

// ── Servicio y sesión (RF-003, RF-036 a RF-039, RN-09, RN-10) ────────────────

export function evaluarServicio(estado: EstadoSim, ahora: number): Servicio {
  const zona = estado.bar.zona_horaria;
  const instante = new Date(ahora);
  const cierre = cierreDeFranja(estado.horario, instante, zona);
  const proxima = proximaApertura(estado.horario, instante, zona);
  const ultima = (tipos: TipoAccion[]) => [...estado.acciones].reverse().find((a) => tipos.includes(a.tipo))?.fecha ?? null;
  let tipo: EstadoServicio;
  let desde: number | null;
  if (estado.mantenimiento) {
    tipo = 'mantenimiento';
    desde = ultima(['mantenimiento']);
  } else if (estado.modo_qr === 'pausado') {
    tipo = 'en_pausa';
    desde = ultima(['pausar_solicitudes']);
  } else if (estado.modo_qr === 'forzado_activo' || abiertoPorHorario(estado.horario, instante, zona)) {
    tipo = 'abierto';
    desde = estado.sesion?.apertura ?? null;
  } else {
    tipo = 'fuera_de_horario';
    desde = null;
  }
  return {
    estado: tipo,
    modo_qr: estado.modo_qr,
    desde: desde === null ? null : iso(desde),
    hasta: tipo === 'abierto' && estado.modo_qr !== 'forzado_activo' && cierre ? iso(cierre.getTime()) : null,
    proxima_apertura: tipo === 'abierto' || !proxima ? null : iso(proxima.getTime()),
  };
}

function abrirSesionInterna(estado: EstadoSim, en: number, origen: 'horario' | 'manual', tipoEvento = 'normal', usuario: { id: string; nombre: string } | null = null) {
  estado.sesion = { id: nuevoId(estado, 'n'), apertura: en, tipo_evento: tipoEvento, origen };
  estado.metricas = { solicitudes: 0, telefonos: 0, votos: 0, espera_seg_suma: 0, espera_n: 0 };
  accion(estado, en, usuario, 'abrir_sesion', { tipo: 'sesion', nombre: 'Noche', detalle: null }, origen);
}

function cerrarSesionInterna(estado: EstadoSim, en: number, usuario: { id: string; nombre: string } | null, tipo: TipoAccion = 'cerrar_sesion') {
  if (!estado.sesion) return;
  for (const s of deLaSesion(estado)) if (s.estado === 'pendiente') s.estado = 'expirada';
  for (const b of estado.bloqueos) if (b.duracion === 'fin_sesion' && b.fin === null && !b.levantado) b.fin = en;
  accion(estado, en, usuario, tipo, { tipo: 'sesion', nombre: 'Noche', detalle: null }, usuario ? 'manual' : 'horario');
  estado.sesion = null;
}

// ── Reproducción (RF-019, RF-020, RN-08) ─────────────────────────────────────

function terminar(estado: EstadoSim, fin: number, resultado: 'completa' | 'saltada_admin' | 'error_video') {
  const rep = estado.reproduccion;
  if (!rep) return;
  estado.historial.push({ video_id: rep.video_id, origen: rep.origen, solicitud_id: rep.solicitud_id, sesion_id: estado.sesion?.id ?? null, inicio: rep.inicio, fin, resultado });
  const s = rep.solicitud_id ? estado.solicitudes.find((x) => x.id === rep.solicitud_id) : undefined;
  if (s) {
    s.estado = resultado === 'completa' ? 'reproducida' : resultado === 'saltada_admin' ? 'saltada' : 'no_disponible';
    s.fin = fin;
  }
  estado.reproduccion = null;
}

/** Número pseudoaleatorio reproducible a partir de la secuencia del estado. */
function aleatorio(estado: EstadoSim): number {
  estado.secuencia = (Math.imul(estado.secuencia, 1103515245) + 12345) & 0x7fffffff;
  return estado.secuencia / 0x7fffffff;
}

function siguienteDeLista(estado: EstadoSim, en: number): string | null {
  const lista = estado.listas.find((l) => l.activa);
  if (!lista || lista.elementos.length === 0) return null;
  for (let intento = 0; intento < lista.elementos.length; intento++) {
    let elemento;
    if (lista.modo_rotacion === 'secuencial') {
      elemento = lista.elementos[lista.indice % lista.elementos.length]!;
      lista.indice = (lista.indice + 1) % lista.elementos.length;
    } else {
      if (lista.pendientes_aleatorios.length === 0) lista.pendientes_aleatorios = lista.elementos.map((e) => e.elemento_id);
      const i = Math.floor(aleatorio(estado) * lista.pendientes_aleatorios.length);
      const [id] = lista.pendientes_aleatorios.splice(i, 1);
      elemento = lista.elementos.find((e) => e.elemento_id === id)!;
    }
    if (bloqueoVigente(estado, elemento.video_id, en)) continue;
    elemento.veces++;
    elemento.ultima = en;
    return elemento.video_id;
  }
  return null;
}

function iniciarSiguiente(estado: EstadoSim, en: number) {
  const siguiente = pendientes(estado)[0];
  if (siguiente) {
    siguiente.estado = 'reproduciendo';
    siguiente.inicio = en;
    estado.metricas.espera_seg_suma += Math.max(0, (en - siguiente.fecha) / 1000);
    estado.metricas.espera_n++;
    estado.reproduccion = { id: nuevoId(estado, 'r'), video_id: siguiente.video_id, origen: 'solicitud', solicitud_id: siguiente.id, inicio: en, acumulado_seg: 0, reanudada_en: en };
    return;
  }
  const deLista = siguienteDeLista(estado, en);
  estado.reproduccion = deLista
    ? { id: nuevoId(estado, 'r'), video_id: deLista, origen: 'lista_casa', solicitud_id: null, inicio: en, acumulado_seg: 0, reanudada_en: en }
    : null;
}

/**
 * Lleva el estado hasta `ahora`: abre o cierra la sesión según el horario, termina las
 * canciones que ya acabaron y empieza las siguientes sin silencios. Devuelve si cambió algo.
 */
export function avanzar(estado: EstadoSim, ahora: number): boolean {
  const antes = estado.version;
  const zona = estado.bar.zona_horaria;
  const instante = new Date(ahora);

  // Sesión según el horario (RN-09).
  const enHorario = abiertoPorHorario(estado.horario, instante, zona);
  const vetada = estado.sin_apertura_automatica_hasta !== null && ahora < estado.sin_apertura_automatica_hasta;
  if (!vetada) estado.sin_apertura_automatica_hasta = null;
  if (!estado.sesion && !estado.mantenimiento && !vetada && (enHorario || estado.modo_qr === 'forzado_activo')) {
    const franja = franjaVigente(estado.horario, instante, zona);
    const cierre = cierreDeFranja(estado.horario, instante, zona);
    const inicio = franja && cierre ? Math.max(ahora - 24 * 3600_000, cierre.getTime() - duracionMs(franja)) : ahora;
    abrirSesionInterna(estado, Math.min(inicio, ahora), enHorario ? 'horario' : 'manual');
    tocar(estado);
  } else if (estado.sesion && !enHorario && estado.modo_qr !== 'forzado_activo') {
    // Fuera de horario y sin apertura forzada, la noche se cierra, la haya abierto el horario o el personal.
    cerrarSesionInterna(estado, ahora, null);
    tocar(estado);
  }

  // Reproducción: lo que ya terminó, termina; empieza lo siguiente justo a su hora.
  let vueltas = 0;
  while (estado.reproduccion && estado.reproduccion.reanudada_en !== null && vueltas < 500) {
    const rep = estado.reproduccion;
    const fin = rep.reanudada_en! + (cancion(estado, rep.video_id).duracion_seg - rep.acumulado_seg) * 1000;
    if (fin > ahora) break;
    terminar(estado, fin, 'completa');
    iniciarSiguiente(estado, fin);
    vueltas++;
    tocar(estado);
  }
  if (vueltas >= 500 && estado.reproduccion) {
    // Pasaron días sin abrir la simulación: se retoma desde ahora.
    estado.reproduccion.inicio = ahora;
    estado.reproduccion.reanudada_en = ahora;
    estado.reproduccion.acumulado_seg = 0;
  }
  if (!estado.reproduccion && (pendientes(estado).length > 0 || estado.listas.some((l) => l.activa && l.elementos.length > 0))) {
    iniciarSiguiente(estado, ahora);
    if (estado.reproduccion) tocar(estado);
  }
  return estado.version !== antes;
}

function duracionMs(f: { hora_inicio: string; hora_fin: string }): number {
  const m = (t: string) => Number(t.slice(0, 2)) * 60 + Number(t.slice(3, 5));
  return (((m(f.hora_fin) - m(f.hora_inicio) + 1440) % 1440) || 1440) * 60_000;
}

// ── Lo que ven el cliente y la pantalla ──────────────────────────────────────

function itemCola(estado: EstadoSim, s: SolicitudSim, puesto: number, ahora: number): ItemCola {
  const vigente = s.subida && ahora - s.subida.en < VIGENCIA_SUBIDA_MS ? s.subida : null;
  return {
    solicitud_id: s.id,
    cancion: aContrato(cancion(estado, s.video_id)),
    votos_total: s.votos.length,
    fecha_solicitud: iso(s.fecha),
    puesto,
    puesto_anterior: vigente ? puesto + vigente.puestos : null,
  };
}

export function construirAhora(estado: EstadoSim, ahora: number): Ahora {
  const rep = estado.reproduccion;
  const solicitudActual = rep?.solicitud_id ? estado.solicitudes.find((s) => s.id === rep.solicitud_id) : undefined;
  const lista = estado.listas.find((l) => l.activa);
  return {
    version: estado.version,
    servidor_ahora: iso(ahora),
    servicio: evaluarServicio(estado, ahora),
    reproduccion: rep
      ? {
          id: rep.id,
          cancion: aContrato(cancion(estado, rep.video_id)),
          origen: rep.origen,
          solicitud_id: rep.solicitud_id,
          votos: solicitudActual ? solicitudActual.votos.length : null,
          inicio: iso(rep.inicio),
          posicion_seg: Math.floor(Math.min(posicionSeg(rep, ahora), cancion(estado, rep.video_id).duracion_seg)),
          estado: rep.reanudada_en === null ? 'pausada' : 'sonando',
        }
      : null,
    cola: pendientes(estado).map((s, i) => itemCola(estado, s, i + 1, ahora)),
    lista_casa: lista ? { nombre: lista.nombre, total_canciones: lista.elementos.length } : null,
  };
}

export function construirDestacadas(estado: EstadoSim): { destacadas: Destacada[] } {
  const contexto = { reproduciendo: 'sonando', reproducida: 'sono', pendiente: 'en_cola' } as const;
  const destacadas = deLaSesion(estado)
    .filter((s): s is SolicitudSim & { estado: keyof typeof contexto } => s.estado in contexto)
    .sort((a, b) => b.votos.length - a.votos.length || a.fecha - b.fecha)
    .slice(0, 3)
    .map((s) => ({
      cancion: aContrato(cancion(estado, s.video_id)),
      votos: s.votos.length,
      contexto: contexto[s.estado],
      hora: s.estado === 'reproducida' && s.inicio !== null ? iso(s.inicio) : null,
    }));
  return { destacadas };
}

export function construirYo(estado: EstadoSim, dispositivo: string): Yo {
  const cola = pendientes(estado);
  const propias = deLaSesion(estado)
    .filter((s) => s.dispositivo === dispositivo)
    .sort((a, b) => b.fecha - a.fecha);
  const solicitudes: MiSolicitud[] = propias.map((s) => ({
    solicitud_id: s.id,
    cancion: aContrato(cancion(estado, s.video_id)),
    estado: s.estado,
    fecha_solicitud: iso(s.fecha),
    votos_total: s.votos.length,
    puesto: s.estado === 'pendiente' ? cola.findIndex((x) => x.id === s.id) + 1 : null,
    motivo: s.motivo,
  }));
  return {
    pedidos_maximos: estado.bar.pedidos_maximos,
    pedidos_restantes: Math.max(0, estado.bar.pedidos_maximos - cupoUsado(estado, dispositivo)),
    solicitudes,
    votos: deLaSesion(estado)
      .filter((s) => (s.estado === 'pendiente' || s.estado === 'reproduciendo') && s.votos.includes(dispositivo))
      .map((s) => s.id),
    aviso_visto_en: estado.dispositivos[dispositivo]?.aviso ? iso(estado.dispositivos[dispositivo]!.aviso!) : null,
  };
}

export function registrarDispositivo(estado: EstadoSim, dispositivo: string, tipo: string, ahora: number): Yo {
  if (!estado.dispositivos[dispositivo]) {
    estado.dispositivos[dispositivo] = { codigo: codigoTelefono(estado, dispositivo), tipo, primer_acceso: ahora, aviso: null };
    if (estado.sesion) estado.metricas.telefonos++;
    tocar(estado);
  }
  return construirYo(estado, dispositivo);
}

export function anotarAviso(estado: EstadoSim, dispositivo: string, ahora: number): void {
  const d = estado.dispositivos[dispositivo];
  if (d && !d.aviso) {
    d.aviso = ahora;
    tocar(estado);
  }
}

// ── Buscar, pedir y votar (RF-005 a RF-012, RN-01 a RN-07) ───────────────────

export function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().replace(/[^a-z0-9ñ ]+/g, ' ').replace(/\s+/g, ' ').trim();
}

function motivoNoPedible(estado: EstadoSim, c: CancionSim, ahora: number): { motivo: MotivoNoPedible; desde: number | null } | null {
  if (bloqueoVigente(estado, c.video_id, ahora)) return { motivo: 'bloqueada', desde: null };
  if (c.disponibilidad !== 'disponible') return { motivo: 'no_disponible', desde: null };
  if (!c.embebible) return { motivo: 'no_embebible', desde: null };
  if (c.duracion_seg > estado.bar.duracion_max_seg) return { motivo: 'muy_larga', desde: null };
  const ventana = estado.bar.ventana_no_repeticion_min * 60_000;
  const sono = deLaSesion(estado)
    .filter((s) => s.video_id === c.video_id && s.inicio !== null && (s.estado === 'reproduciendo' || s.estado === 'reproducida' || s.estado === 'saltada'))
    .map((s) => s.inicio!)
    .sort((a, b) => b - a)[0];
  if (sono !== undefined && ahora - sono < ventana) return { motivo: 'reproducida_hace_poco', desde: sono + ventana };
  return null;
}

export function buscar(estado: EstadoSim, consulta: string, ahora: number): Resultado<Busqueda> {
  if (estado.fallos.youtube === 'caido') return mal(503, 'youtube_no_disponible', 'YouTube no respondió');
  const tokens = normalizar(consulta).split(' ').filter(Boolean);
  if (tokens.length === 0) return mal(400, 'datos_invalidos', 'Consulta vacía');
  const coincide = (c: CancionSim) => {
    const texto = normalizar(`${c.titulo} ${c.artista}`);
    return tokens.every((t) => texto.includes(t));
  };
  const cola = construirAhora(estado, ahora).cola;
  const enCola = cola.filter((i) => coincide(cancion(estado, i.cancion.video_id)));
  const idsEnCola = new Set(cola.map((i) => i.cancion.video_id));
  const resultados = Object.values(estado.catalogo)
    .filter((c) => coincide(c) && !idsEnCola.has(c.video_id))
    .sort((a, b) => Number(normalizar(b.titulo).startsWith(tokens[0]!)) - Number(normalizar(a.titulo).startsWith(tokens[0]!)))
    .slice(0, 8)
    .map((c) => {
      const no = motivoNoPedible(estado, c, ahora);
      return { cancion: aContrato(c), pedible: no === null, motivo: no?.motivo ?? null, disponible_desde: no?.desde ? iso(no.desde) : null };
    });
  return bien({ consulta, en_cola: enCola, resultados, desde_cache: estado.fallos.youtube === 'cuota' });
}

/** Reordena la cola alrededor de un cambio de votos y anota quién subió. */
function conSubidas(estado: EstadoSim, ahora: number, cambio: () => void) {
  const antes = pendientes(estado).map((s) => s.id);
  cambio();
  const despues = pendientes(estado).map((s) => s.id);
  despues.forEach((id, nuevo) => {
    const viejo = antes.indexOf(id);
    if (viejo === -1 || viejo === nuevo) return;
    const s = estado.solicitudes.find((x) => x.id === id)!;
    s.subida = nuevo < viejo ? { puestos: viejo - nuevo, en: ahora } : null;
  });
}

function aceptaPedidos(estado: EstadoSim, ahora: number): boolean {
  return evaluarServicio(estado, ahora).estado === 'abierto' && estado.sesion !== null;
}

export function pedirCancion(estado: EstadoSim, dispositivo: string, videoId: string, ahora: number): Resultado<RespuestaSolicitud> {
  if (!aceptaPedidos(estado, ahora)) return mal(423, 'servicio_no_disponible', 'Ahora no se reciben canciones');
  const c = estado.catalogo[videoId];
  if (!c) return mal(422, 'no_disponible', 'El video no existe o no está disponible');
  const yaEnCola = pendientes(estado).find((s) => s.video_id === videoId);
  if (yaEnCola) {
    // RN-02: pedir una que ya está pendiente es votar por ella.
    const voto = votar(estado, dispositivo, yaEnCola.id, ahora);
    if (!voto.ok) return voto;
    const puesto = pendientes(estado).findIndex((s) => s.id === yaEnCola.id) + 1;
    return bien({ resultado: 'votada', solicitud_id: yaEnCola.id, puesto, espera_seg: esperaDe(estado, yaEnCola.id, ahora), pedidos_restantes: construirYo(estado, dispositivo).pedidos_restantes }, 201);
  }
  const no = motivoNoPedible(estado, c, ahora);
  if (no) return mal(422, no.motivo, 'No se puede pedir', no.desde ? { disponible_desde: iso(no.desde) } : undefined);
  if (cupoUsado(estado, dispositivo) >= estado.bar.pedidos_maximos) return mal(409, 'limite_pedidos', 'Ya tienes todos tus pedidos en la cola');

  const s: SolicitudSim = { id: nuevoId(estado, 's'), sesion_id: estado.sesion!.id, video_id: videoId, dispositivo, fecha: ahora, votos: [dispositivo], estado: 'pendiente', inicio: null, fin: null, motivo: null, subida: null };
  conSubidas(estado, ahora, () => estado.solicitudes.push(s));
  estado.metricas.solicitudes++;
  estado.metricas.votos++;
  tocar(estado);
  avanzar(estado, ahora); // si no sonaba nada, empieza ya
  const puesto = pendientes(estado).findIndex((x) => x.id === s.id) + 1;
  return bien({ resultado: 'creada', solicitud_id: s.id, puesto: Math.max(1, puesto), espera_seg: Math.round(esperaDe(estado, s.id, ahora)), pedidos_restantes: construirYo(estado, dispositivo).pedidos_restantes }, 201);
}

function esperaDe(estado: EstadoSim, solicitudId: string, ahora: number): number {
  let total = restanteSeg(estado, ahora);
  for (const s of pendientes(estado)) {
    if (s.id === solicitudId) break;
    total += cancion(estado, s.video_id).duracion_seg;
  }
  return Math.round(total);
}

export function votar(estado: EstadoSim, dispositivo: string, solicitudId: string, ahora: number): Resultado<RespuestaVoto> {
  if (!aceptaPedidos(estado, ahora)) return mal(423, 'servicio_no_disponible', 'Ahora no se puede votar');
  const s = deLaSesion(estado).find((x) => x.id === solicitudId);
  if (!s || s.estado !== 'pendiente') return mal(410, 'no_pendiente', 'Esa canción ya no está en la cola');
  if (s.votos.includes(dispositivo)) return mal(409, 'ya_votaste', 'Ya votaste por esta canción');
  conSubidas(estado, ahora, () => s.votos.push(dispositivo));
  estado.metricas.votos++;
  tocar(estado);
  return bien({ solicitud_id: s.id, votos_total: s.votos.length }, 201);
}

export function cambiarVoto(estado: EstadoSim, dispositivo: string, desde: string, hacia: string, ahora: number): Resultado<RespuestaVoto> {
  if (!aceptaPedidos(estado, ahora)) return mal(423, 'servicio_no_disponible', 'Ahora no se puede votar');
  const origen = deLaSesion(estado).find((x) => x.id === desde);
  const destino = deLaSesion(estado).find((x) => x.id === hacia);
  if (!origen || !destino || origen.estado !== 'pendiente' || destino.estado !== 'pendiente') return mal(410, 'no_pendiente', 'Esa canción ya no está en la cola');
  if (!origen.votos.includes(dispositivo)) return mal(400, 'datos_invalidos', 'No habías votado por esa canción');
  if (origen.dispositivo === dispositivo) return mal(400, 'datos_invalidos', 'El voto de tu propia canción no se mueve');
  if (destino.votos.includes(dispositivo)) return mal(409, 'ya_votaste', 'Ya votaste por esa canción');
  conSubidas(estado, ahora, () => {
    origen.votos = origen.votos.filter((d) => d !== dispositivo);
    destino.votos.push(dispositivo);
  });
  tocar(estado);
  return bien({ solicitud_id: destino.id, votos_total: destino.votos.length });
}

// ── La pantalla del bar reporta (RF-007, RF-019) ─────────────────────────────

export function reportarPantalla(estado: EstadoSim, reproduccionId: string, tipo: 'inicio' | 'fin' | 'error', ahora: number, codigoYoutube: number | null): Resultado<null> {
  const rep = estado.reproduccion;
  if (!rep || rep.id !== reproduccionId) return bien(null, 204); // ya avanzó: el reporte llega tarde y no hace nada
  if (tipo === 'inicio') return bien(null, 204);
  const c = cancion(estado, rep.video_id);
  if (tipo === 'error') {
    accion(estado, ahora, null, 'error_youtube', objetivoCancion(c), `El reproductor dio el error ${codigoYoutube ?? 'desconocido'}`);
    terminar(estado, ahora, 'error_video');
  } else {
    terminar(estado, ahora, 'completa');
  }
  iniciarSiguiente(estado, ahora);
  tocar(estado);
  return bien(null, 204);
}

// ── El panel (RF-037 a RF-044) ───────────────────────────────────────────────

export type Personal = { id: string; nombre: string };

export function saltar(estado: EstadoSim, reproduccionId: string, quien: Personal, ahora: number): Resultado<null> {
  const rep = estado.reproduccion;
  if (!rep || rep.id !== reproduccionId) return bien(null, 204); // idempotente: ya no suena
  accion(estado, ahora, quien, 'saltar', objetivoCancion(cancion(estado, rep.video_id)));
  terminar(estado, ahora, 'saltada_admin');
  iniciarSiguiente(estado, ahora);
  tocar(estado);
  return bien(null, 204);
}

export function pausarReproduccion(estado: EstadoSim, reproduccionId: string, pausar: boolean, quien: Personal, ahora: number): Resultado<null> {
  const rep = estado.reproduccion;
  if (!rep || rep.id !== reproduccionId) return mal(404, 'no_encontrado', 'Esa canción ya no suena');
  const c = cancion(estado, rep.video_id);
  if (pausar && rep.reanudada_en !== null) {
    rep.acumulado_seg = posicionSeg(rep, ahora);
    rep.reanudada_en = null;
    accion(estado, ahora, quien, 'pausar_reproduccion', objetivoCancion(c));
    tocar(estado);
  } else if (!pausar && rep.reanudada_en === null) {
    rep.reanudada_en = ahora;
    accion(estado, ahora, quien, 'reanudar_reproduccion', objetivoCancion(c));
    tocar(estado);
  }
  return bien(null, 204);
}

export function retirar(estado: EstadoSim, solicitudId: string, motivo: string, quien: Personal, ahora: number): Resultado<null> {
  const s = deLaSesion(estado).find((x) => x.id === solicitudId);
  if (!s || s.estado !== 'pendiente') return mal(410, 'no_pendiente', 'Esa canción ya no está en la cola');
  s.estado = 'retirada';
  s.motivo = motivo;
  accion(estado, ahora, quien, 'retirar', objetivoCancion(cancion(estado, s.video_id)), motivo);
  tocar(estado);
  return bien(null, 204);
}

export function bloquear(
  estado: EstadoSim,
  datos: { video_id: string; motivo: string; duracion: BloqueoSim['duracion']; horas: number | null },
  quien: Personal,
  ahora: number,
): Resultado<BloqueoSim> {
  const c = estado.catalogo[datos.video_id];
  if (!c) return mal(404, 'no_encontrado', 'Ese video no está en el catálogo');
  const existente = bloqueoVigente(estado, datos.video_id, ahora);
  if (existente) return bien(existente, 201);
  const b: BloqueoSim = {
    id: nuevoId(estado, 'b'),
    video_id: datos.video_id,
    motivo: datos.motivo,
    duracion: datos.duracion,
    horas: datos.duracion === 'horas' ? datos.horas : null,
    inicio: ahora,
    fin: datos.duracion === 'horas' && datos.horas ? ahora + datos.horas * 3600_000 : null,
    creado_por: quien.nombre,
    levantado: false,
  };
  estado.bloqueos.push(b);
  // Si suena, se corta; si está en la cola, sale con su motivo.
  if (estado.reproduccion?.video_id === datos.video_id) {
    accion(estado, ahora, quien, 'saltar', objetivoCancion(c));
    terminar(estado, ahora, 'saltada_admin');
    iniciarSiguiente(estado, ahora);
  }
  for (const s of deLaSesion(estado)) {
    if (s.video_id === datos.video_id && s.estado === 'pendiente') {
      s.estado = 'bloqueada';
      s.motivo = datos.motivo;
    }
  }
  accion(estado, ahora, quien, 'bloquear', objetivoCancion(c), textoDuracion(b));
  tocar(estado);
  return bien(b, 201);
}

export function textoDuracion(b: Pick<BloqueoSim, 'duracion' | 'horas'>): string {
  if (b.duracion === 'fin_sesion') return 'Hasta el fin de la noche';
  if (b.duracion === 'indefinido') return 'Hasta que alguien lo levante';
  return b.horas === 1 ? 'Por 1 hora' : `Por ${b.horas} horas`;
}

export function desbloquear(estado: EstadoSim, bloqueoId: string, quien: Personal, ahora: number): Resultado<null> {
  const b = estado.bloqueos.find((x) => x.id === bloqueoId);
  if (!b) return mal(404, 'no_encontrado', 'Ese bloqueo no existe');
  if (!b.levantado) {
    b.levantado = true;
    b.fin = ahora;
    accion(estado, ahora, quien, 'desbloquear', objetivoCancion(cancion(estado, b.video_id)));
    tocar(estado);
  }
  return bien(null, 204);
}

export function extenderBloqueo(estado: EstadoSim, bloqueoId: string, duracion: BloqueoSim['duracion'], horas: number | null, quien: Personal, ahora: number): Resultado<BloqueoSim> {
  const b = estado.bloqueos.find((x) => x.id === bloqueoId);
  if (!b) return mal(404, 'no_encontrado', 'Ese bloqueo no existe');
  b.duracion = duracion;
  b.horas = duracion === 'horas' ? horas : null;
  b.fin = duracion === 'horas' && horas ? ahora + horas * 3600_000 : null;
  b.levantado = false;
  accion(estado, ahora, quien, 'extender_bloqueo', objetivoCancion(cancion(estado, b.video_id)), textoDuracion(b));
  tocar(estado);
  return bien(b);
}

export function cambiarModoQr(estado: EstadoSim, modo: EstadoSim['modo_qr'], quien: Personal, ahora: number): Resultado<null> {
  const salirDeMantenimiento = estado.mantenimiento;
  if (estado.modo_qr === modo && !salirDeMantenimiento) return bien(null, 204);
  estado.modo_qr = modo;
  estado.mantenimiento = false;
  const tipo: TipoAccion = modo === 'pausado' ? 'pausar_solicitudes' : modo === 'forzado_activo' ? 'forzar_apertura' : 'reanudar_solicitudes';
  accion(estado, ahora, quien, tipo, { tipo: 'servicio', nombre: 'Solicitudes', detalle: null });
  tocar(estado);
  avanzar(estado, ahora);
  return bien(null, 204);
}

export function activarMantenimiento(estado: EstadoSim, quien: Personal, ahora: number): Resultado<null> {
  if (estado.mantenimiento) return bien(null, 204);
  estado.mantenimiento = true;
  cerrarSesionInterna(estado, ahora, quien, 'mantenimiento');
  tocar(estado);
  return bien(null, 204);
}

export function abrirSesionManual(estado: EstadoSim, tipoEvento: string, quien: Personal, ahora: number): Resultado<null> {
  if (estado.sesion) return mal(409, 'conflicto', 'Ya hay una noche abierta');
  estado.mantenimiento = false;
  estado.sin_apertura_automatica_hasta = null;
  abrirSesionInterna(estado, ahora, 'manual', tipoEvento, quien);
  if (!abiertoPorHorario(estado.horario, new Date(ahora), estado.bar.zona_horaria) && estado.modo_qr === 'automatico') estado.modo_qr = 'forzado_activo';
  tocar(estado);
  return bien(null, 204);
}

export function cerrarSesionManual(estado: EstadoSim, quien: Personal, ahora: number): Resultado<null> {
  if (!estado.sesion) return mal(409, 'conflicto', 'No hay una noche abierta');
  cerrarSesionInterna(estado, ahora, quien);
  if (estado.modo_qr === 'forzado_activo') estado.modo_qr = 'automatico';
  // Cerrada a mano dentro del horario: no se reabre sola hasta que acabe esta franja.
  const cierre = cierreDeFranja(estado.horario, new Date(ahora), estado.bar.zona_horaria);
  estado.sin_apertura_automatica_hasta = cierre ? cierre.getTime() : null;
  tocar(estado);
  return bien(null, 204);
}

export function construirNoche(estado: EstadoSim, ahora: number): Noche {
  const ahoraFoto = construirAhora(estado, ahora);
  const rep = estado.reproduccion;
  const solicitudActual = rep?.solicitud_id ? estado.solicitudes.find((s) => s.id === rep.solicitud_id) : undefined;
  const lista = estado.listas.find((l) => l.activa);
  const inicioNoche = estado.sesion?.apertura ?? ahora - 12 * 3600_000;
  const alertas: Noche['alertas'] = [];
  if (!lista) alertas.push({ tipo: 'sin_lista_activa', detalle: 'Si la cola se vacía, no sonará nada.' });
  if (!estado.sesion && ahoraFoto.servicio.estado === 'abierto') alertas.push({ tipo: 'sin_sesion', detalle: null });
  const errores = estado.acciones.filter((a) => a.tipo === 'error_youtube' && a.fecha > ahora - 30 * 60_000).length;
  if (errores > 0) alertas.push({ tipo: 'errores_youtube', detalle: `${errores} en la última media hora` });
  const m = estado.metricas;
  return {
    version: estado.version,
    servidor_ahora: iso(ahora),
    sesion: estado.sesion ? { id: estado.sesion.id, apertura: iso(estado.sesion.apertura), tipo_evento: estado.sesion.tipo_evento, origen: estado.sesion.origen } : null,
    servicio: ahoraFoto.servicio,
    reproduccion: ahoraFoto.reproduccion ? { ...ahoraFoto.reproduccion, pedida_por: solicitudActual ? codigoTelefono(estado, solicitudActual.dispositivo) : null } : null,
    cola: ahoraFoto.cola.map((item) => {
      const s = estado.solicitudes.find((x) => x.id === item.solicitud_id)!;
      return { ...item, pedida_por: codigoTelefono(estado, s.dispositivo), duracion_seg: item.cancion.duracion_seg };
    }),
    metricas: {
      desde: estado.sesion ? iso(estado.sesion.apertura) : null,
      solicitudes: m.solicitudes,
      telefonos: m.telefonos,
      votos: m.votos,
      espera_promedio_seg: m.espera_n > 0 ? Math.round(m.espera_seg_suma / m.espera_n) : null,
    },
    registro: estado.acciones
      .filter((a) => a.fecha >= inicioNoche)
      .sort((a, b) => b.fecha - a.fecha)
      .slice(0, 20)
      .map((a) => ({ ...a, fecha: iso(a.fecha) })),
    horario: estado.horario,
    lista_casa: lista ? { id: lista.id, nombre: lista.nombre, total_canciones: lista.elementos.length, modo_rotacion: lista.modo_rotacion } : null,
    bloqueos: estado.bloqueos
      .filter((b) => bloqueoVigente(estado, b.video_id, ahora)?.id === b.id)
      .map((b) => bloqueoAContrato(estado, b, ahora)),
    alertas,
  };
}

export function bloqueoAContrato(estado: EstadoSim, b: BloqueoSim, ahora: number) {
  return {
    id: b.id,
    cancion: aContrato(cancion(estado, b.video_id)),
    motivo: b.motivo,
    duracion: b.duracion,
    horas: b.horas,
    inicio: iso(b.inicio),
    fin: b.fin === null ? null : iso(b.fin),
    creado_por: b.creado_por,
    vigente: bloqueoVigente(estado, b.video_id, ahora)?.id === b.id,
  };
}
