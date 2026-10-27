import type { CodigoError, CuerpoEvento, EventoBar, Franja, ListaCasaDetalle, TipoAccion } from '@rockola/contrato';
import { validarFranjas } from '../lib/reglas/horario';
import { bloqueoVigente, cancion, normalizar, type Personal, recientesPrimero, type Resultado, tocar } from './motor';
import type { AccionSim, EstadoSim, ListaSim, UsuarioSim } from './tipos';

/**
 * La gestión del panel en la simulación (iteración 4): usuarios, establecimiento y QR, horario,
 * listas de la casa, bitácora y eventos. Funciones puras sobre `EstadoSim`, como el motor.
 */

const bien = <T>(cuerpo: T, estado: 200 | 201 | 204 = 200): Resultado<T> => ({ ok: true, estado, cuerpo });
const mal = (estado: number, codigo: CodigoError, mensaje: string): Resultado<never> => ({ ok: false, estado, codigo, mensaje });
const iso = (ms: number) => new Date(ms).toISOString();

function siguiente(estado: EstadoSim, prefijo: string) {
  estado.secuencia++;
  return `${prefijo}-${estado.secuencia}`;
}

export function anotar(estado: EstadoSim, ahora: number, quien: Personal, tipo: TipoAccion, objetivo: AccionSim['objetivo'], detalle: string | null = null) {
  estado.acciones.push({ id: siguiente(estado, 'a'), fecha: ahora, usuario: quien, tipo, objetivo, detalle });
  tocar(estado);
}

/** Clave temporal legible para dictarla en voz alta; no es un secreto real (solo simulación). */
function claveTemporal(estado: EstadoSim): string {
  const palabras = ['salsa', 'cumbia', 'bolero', 'vallenato', 'son', 'mambo', 'merengue', 'porro'];
  estado.secuencia++;
  return `${palabras[estado.secuencia % palabras.length]}-${1000 + ((estado.secuencia * 7919) % 9000)}`;
}

// ── Usuarios (RF-023 a RF-026) ───────────────────────────────────────────────

export function usuarioAContrato(u: UsuarioSim) {
  return { id: u.id, nombre: u.nombre, correo: u.correo, rol: u.rol, estado: u.estado, debe_cambiar_clave: u.debe_cambiar_clave, ultimo_acceso: u.ultimo_acceso === null ? null : iso(u.ultimo_acceso) };
}

export function listarUsuarios(estado: EstadoSim, filtros: { q?: string | null; rol?: string | null; estado?: string | null }) {
  const q = normalizar(filtros.q ?? '');
  return estado.usuarios
    .filter((u) => (!q || normalizar(`${u.nombre} ${u.correo}`).includes(q)) && (!filtros.rol || u.rol === filtros.rol) && (!filtros.estado || u.estado === filtros.estado))
    .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
    .map(usuarioAContrato);
}

export function crearUsuario(estado: EstadoSim, datos: { nombre: string; correo: string; rol: UsuarioSim['rol'] }, quien: Personal, ahora: number) {
  if (estado.usuarios.some((u) => u.correo.toLowerCase() === datos.correo.toLowerCase())) return mal(409, 'conflicto', 'Ya hay un usuario con ese correo');
  const clave = claveTemporal(estado);
  const u: UsuarioSim = { id: siguiente(estado, 'u'), nombre: datos.nombre.trim(), correo: datos.correo.trim().toLowerCase(), rol: datos.rol, estado: 'activo', clave, debe_cambiar_clave: true, ultimo_acceso: null, intentos: 0, bloqueado_hasta: null };
  estado.usuarios.push(u);
  anotar(estado, ahora, quien, 'crear_usuario', { tipo: 'usuario', nombre: u.nombre, detalle: null });
  return bien({ usuario: usuarioAContrato(u), clave_temporal: clave }, 201);
}

const administradoresActivos = (estado: EstadoSim) => estado.usuarios.filter((u) => u.rol === 'administrador' && u.estado === 'activo');

export function editarUsuario(estado: EstadoSim, id: string, cambios: { nombre?: string; correo?: string; rol?: UsuarioSim['rol'] }, quien: Personal, ahora: number) {
  const u = estado.usuarios.find((x) => x.id === id);
  if (!u) return mal(404, 'no_encontrado', 'Ese usuario no existe');
  if (cambios.correo && estado.usuarios.some((x) => x.id !== id && x.correo.toLowerCase() === cambios.correo!.toLowerCase())) return mal(409, 'conflicto', 'Ya hay un usuario con ese correo');
  // RF-025: no se puede dejar al bar sin Administrador.
  if (cambios.rol === 'operador' && u.rol === 'administrador' && administradoresActivos(estado).length === 1) return mal(409, 'conflicto', 'Es el último Administrador');
  if (cambios.nombre) u.nombre = cambios.nombre.trim();
  if (cambios.correo) u.correo = cambios.correo.trim().toLowerCase();
  if (cambios.rol) u.rol = cambios.rol;
  anotar(estado, ahora, quien, 'editar_usuario', { tipo: 'usuario', nombre: u.nombre, detalle: null });
  return bien(usuarioAContrato(u));
}

export function restablecerClave(estado: EstadoSim, id: string, quien: Personal, ahora: number) {
  const u = estado.usuarios.find((x) => x.id === id);
  if (!u) return mal(404, 'no_encontrado', 'Ese usuario no existe');
  u.clave = claveTemporal(estado);
  u.debe_cambiar_clave = true;
  u.intentos = 0;
  u.bloqueado_hasta = null;
  // Sus sesiones abiertas se cierran: tiene que entrar con la temporal.
  for (const [token, uid] of Object.entries(estado.sesiones_panel)) if (uid === id) delete estado.sesiones_panel[token];
  anotar(estado, ahora, quien, 'restablecer_clave', { tipo: 'usuario', nombre: u.nombre, detalle: null });
  return bien({ clave_temporal: u.clave });
}

export function cambiarEstadoUsuario(estado: EstadoSim, id: string, activo: boolean, quien: Personal, ahora: number) {
  const u = estado.usuarios.find((x) => x.id === id);
  if (!u) return mal(404, 'no_encontrado', 'Ese usuario no existe');
  if (!activo && u.id === quien.id) return mal(409, 'conflicto', 'No puedes desactivarte a ti mismo');
  if (!activo && u.rol === 'administrador' && administradoresActivos(estado).length === 1) return mal(409, 'conflicto', 'Es el último Administrador');
  u.estado = activo ? 'activo' : 'inactivo';
  if (!activo) for (const [token, uid] of Object.entries(estado.sesiones_panel)) if (uid === id) delete estado.sesiones_panel[token];
  anotar(estado, ahora, quien, activo ? 'reactivar_usuario' : 'desactivar_usuario', { tipo: 'usuario', nombre: u.nombre, detalle: null });
  return bien(usuarioAContrato(u));
}

// ── Establecimiento y QR (RF-034, RF-035) ────────────────────────────────────

export function establecimientoAContrato(estado: EstadoSim) {
  const b = estado.bar;
  return { id: b.id, slug: b.slug, nombre: b.nombre, direccion: b.direccion, zona_horaria: b.zona_horaria, estado: b.estado, pedidos_maximos: b.pedidos_maximos, duracion_max_seg: b.duracion_max_seg, ventana_no_repeticion_min: b.ventana_no_repeticion_min };
}

export function editarEstablecimiento(estado: EstadoSim, cambios: Partial<EstadoSim['bar']>, quien: Personal, ahora: number) {
  const permitidos = ['nombre', 'direccion', 'zona_horaria', 'pedidos_maximos', 'duracion_max_seg', 'ventana_no_repeticion_min'] as const;
  for (const clave of permitidos) if (cambios[clave] !== undefined) (estado.bar as Record<string, unknown>)[clave] = cambios[clave];
  anotar(estado, ahora, quien, 'editar_establecimiento', { tipo: 'establecimiento', nombre: estado.bar.nombre, detalle: null });
  return bien(establecimientoAContrato(estado));
}

export function qrAContrato(estado: EstadoSim, origen: string) {
  return { url: `${origen}/q/${estado.qr.token}`, token: estado.qr.token, generado_en: iso(estado.qr.generado_en) };
}

export function regenerarQr(estado: EstadoSim, origen: string, quien: Personal, ahora: number) {
  estado.qr.anteriores.push(estado.qr.token);
  estado.qr = { token: `demo-qr-${estado.qr.anteriores.length + 1}`, generado_en: ahora, anteriores: estado.qr.anteriores };
  anotar(estado, ahora, quien, 'regenerar_qr', { tipo: 'establecimiento', nombre: estado.bar.nombre, detalle: null });
  return bien(qrAContrato(estado, origen));
}

// ── Horario (RF-036) ─────────────────────────────────────────────────────────

export function guardarHorario(estado: EstadoSim, franjas: Franja[], quien: Personal, ahora: number) {
  const problemas = validarFranjas(franjas);
  if (problemas.some((p) => p.tipo === 'misma_hora')) return mal(400, 'datos_invalidos', 'Una franja empieza y termina a la misma hora');
  if (problemas.some((p) => p.tipo === 'solapada')) return mal(409, 'conflicto', 'Hay franjas que se pisan');
  estado.horario = franjas.map((f) => ({ ...f }));
  anotar(estado, ahora, quien, 'editar_horario', { tipo: 'horario', nombre: 'Horario', detalle: null });
  return bien({ zona_horaria: estado.bar.zona_horaria, franjas: estado.horario });
}

// ── Listas de la casa (RF-027 a RF-033) ──────────────────────────────────────

export function listaResumen(estado: EstadoSim, l: ListaSim) {
  return {
    id: l.id,
    nombre: l.nombre,
    modo_rotacion: l.modo_rotacion,
    activa: l.activa,
    total_canciones: l.elementos.length,
    duracion_total_seg: l.elementos.reduce((t, e) => t + cancion(estado, e.video_id).duracion_seg, 0),
    actualizada_en: iso(l.actualizada_en),
  };
}

export function listaDetalle(estado: EstadoSim, l: ListaSim): ListaCasaDetalle {
  return {
    ...listaResumen(estado, l),
    canciones: l.elementos.map((e, i) => {
      const c = cancion(estado, e.video_id);
      return {
        elemento_id: e.elemento_id,
        cancion: { video_id: c.video_id, titulo: c.titulo, artista: c.artista, duracion_seg: c.duracion_seg, miniatura_url: null },
        orden: i + 1,
        veces_reproducida: e.veces,
        ultima_reproduccion: e.ultima === null ? null : iso(e.ultima),
      };
    }),
  };
}

function buscarLista(estado: EstadoSim, id: string) {
  return estado.listas.find((l) => l.id === id);
}

export function crearLista(estado: EstadoSim, nombre: string, quien: Personal, ahora: number) {
  if (estado.listas.some((l) => normalizar(l.nombre) === normalizar(nombre))) return mal(409, 'conflicto', 'Ya hay una lista con ese nombre');
  const l: ListaSim = { id: siguiente(estado, 'l'), nombre: nombre.trim(), modo_rotacion: 'secuencial', activa: false, actualizada_en: ahora, elementos: [], indice: 0, pendientes_aleatorios: [] };
  estado.listas.push(l);
  anotar(estado, ahora, quien, 'editar_lista', { tipo: 'lista', nombre: l.nombre, detalle: 'creada' });
  return bien(listaDetalle(estado, l), 201);
}

export function renombrarLista(estado: EstadoSim, id: string, nombre: string, quien: Personal, ahora: number) {
  const l = buscarLista(estado, id);
  if (!l) return mal(404, 'no_encontrado', 'Esa lista no existe');
  if (estado.listas.some((x) => x.id !== id && normalizar(x.nombre) === normalizar(nombre))) return mal(409, 'conflicto', 'Ya hay una lista con ese nombre');
  l.nombre = nombre.trim();
  l.actualizada_en = ahora;
  anotar(estado, ahora, quien, 'editar_lista', { tipo: 'lista', nombre: l.nombre, detalle: 'renombrada' });
  return bien(listaDetalle(estado, l));
}

export function eliminarLista(estado: EstadoSim, id: string, quien: Personal, ahora: number) {
  const l = buscarLista(estado, id);
  if (!l) return mal(404, 'no_encontrado', 'Esa lista no existe');
  if (l.activa) return mal(409, 'conflicto', 'No se puede eliminar la lista activa');
  estado.listas = estado.listas.filter((x) => x.id !== id);
  anotar(estado, ahora, quien, 'editar_lista', { tipo: 'lista', nombre: l.nombre, detalle: 'eliminada' });
  return bien(null, 204);
}

export function agregarALista(estado: EstadoSim, id: string, videoId: string, quien: Personal, ahora: number) {
  const l = buscarLista(estado, id);
  if (!l) return mal(404, 'no_encontrado', 'Esa lista no existe');
  const c = estado.catalogo[videoId];
  if (!c || c.disponibilidad !== 'disponible' || !c.embebible) return mal(422, 'no_disponible', 'Ese video no está disponible');
  if (bloqueoVigente(estado, videoId, ahora)) return mal(422, 'bloqueada', 'Esa canción está bloqueada');
  if (l.elementos.some((e) => e.video_id === videoId)) return mal(409, 'conflicto', 'Esa canción ya está en la lista');
  l.elementos.push({ elemento_id: siguiente(estado, `${l.id}-e`), video_id: videoId, veces: 0, ultima: null });
  l.actualizada_en = ahora;
  l.pendientes_aleatorios = [];
  anotar(estado, ahora, quien, 'editar_lista', { tipo: 'lista', nombre: l.nombre, detalle: `agregó ${c.titulo}` });
  return bien(listaDetalle(estado, l), 201);
}

export function quitarDeLista(estado: EstadoSim, id: string, elementoId: string, quien: Personal, ahora: number) {
  const l = buscarLista(estado, id);
  if (!l) return mal(404, 'no_encontrado', 'Esa lista no existe');
  const e = l.elementos.find((x) => x.elemento_id === elementoId);
  if (!e) return mal(404, 'no_encontrado', 'Esa canción no está en la lista');
  // RF-032: la lista activa no se puede quedar vacía.
  if (l.activa && l.elementos.length === 1) return mal(409, 'conflicto', 'La lista activa no puede quedar vacía');
  l.elementos = l.elementos.filter((x) => x.elemento_id !== elementoId);
  l.indice = l.elementos.length === 0 ? 0 : l.indice % l.elementos.length;
  l.actualizada_en = ahora;
  l.pendientes_aleatorios = [];
  anotar(estado, ahora, quien, 'editar_lista', { tipo: 'lista', nombre: l.nombre, detalle: `quitó ${cancion(estado, e.video_id).titulo}` });
  return bien(listaDetalle(estado, l));
}

export function ordenarLista(estado: EstadoSim, id: string, elementos: string[], quien: Personal, ahora: number) {
  const l = buscarLista(estado, id);
  if (!l) return mal(404, 'no_encontrado', 'Esa lista no existe');
  const actuales = new Set(l.elementos.map((e) => e.elemento_id));
  if (elementos.length !== actuales.size || !elementos.every((e) => actuales.has(e))) return mal(400, 'datos_invalidos', 'El orden no corresponde a las canciones de la lista');
  l.elementos = elementos.map((idE) => l.elementos.find((e) => e.elemento_id === idE)!);
  l.actualizada_en = ahora;
  anotar(estado, ahora, quien, 'editar_lista', { tipo: 'lista', nombre: l.nombre, detalle: 'reordenada' });
  return bien(listaDetalle(estado, l));
}

export function activarLista(estado: EstadoSim, id: string, modo: ListaSim['modo_rotacion'], quien: Personal, ahora: number) {
  const l = buscarLista(estado, id);
  if (!l) return mal(404, 'no_encontrado', 'Esa lista no existe');
  if (l.elementos.length === 0) return mal(409, 'conflicto', 'Una lista vacía no se puede activar');
  for (const x of estado.listas) x.activa = x.id === id;
  l.modo_rotacion = modo;
  l.pendientes_aleatorios = [];
  l.actualizada_en = ahora;
  anotar(estado, ahora, quien, 'activar_lista', { tipo: 'lista', nombre: l.nombre, detalle: modo });
  return bien(listaDetalle(estado, l));
}

export function buscarCatalogo(estado: EstadoSim, consulta: string) {
  const tokens = normalizar(consulta).split(' ').filter(Boolean);
  return Object.values(estado.catalogo)
    .filter((c) => c.duracion_seg <= estado.bar.duracion_max_seg && tokens.every((t) => normalizar(`${c.titulo} ${c.artista}`).includes(t)))
    .slice(0, 12)
    .map((c) => ({ video_id: c.video_id, titulo: c.titulo, artista: c.artista, duracion_seg: c.duracion_seg, miniatura_url: null }));
}

// ── Bitácora (RF-044) ────────────────────────────────────────────────────────

const POR_PAGINA = 25;

export function bitacora(estado: EstadoSim, filtros: { desde?: string | null; hasta?: string | null; usuario?: string | null; tipo?: string | null; cursor?: string | null }) {
  const desde = filtros.desde ? Date.parse(filtros.desde) : -Infinity;
  const hasta = filtros.hasta ? Date.parse(filtros.hasta) : Infinity;
  const todas = recientesPrimero(
    estado.acciones
      .filter((a) => a.fecha >= desde && a.fecha <= hasta)
      .filter((a) => !filtros.usuario || (filtros.usuario === 'sistema' ? a.usuario === null : a.usuario?.id === filtros.usuario))
      .filter((a) => !filtros.tipo || a.tipo === filtros.tipo),
  );
  const inicio = filtros.cursor ? Math.max(0, Number(filtros.cursor)) : 0;
  const pagina = todas.slice(inicio, inicio + POR_PAGINA);
  return { acciones: pagina.map((a) => ({ ...a, fecha: iso(a.fecha) })), siguiente: inicio + POR_PAGINA < todas.length ? String(inicio + POR_PAGINA) : null };
}

// ── Calendario de eventos (RF-045) ───────────────────────────────────────────

function validarEvento(datos: Partial<CuerpoEvento>): string | null {
  if (datos.inicio && datos.fin && Date.parse(datos.fin) <= Date.parse(datos.inicio)) return 'El evento tiene que terminar después de empezar';
  return null;
}

export function crearEvento(estado: EstadoSim, datos: CuerpoEvento, quien: Personal, ahora: number) {
  const problema = validarEvento(datos);
  if (problema) return mal(400, 'datos_invalidos', problema);
  const e: EventoBar = { id: siguiente(estado, 'ev'), ...datos };
  estado.eventos.push(e);
  anotar(estado, ahora, quien, 'editar_evento', { tipo: 'evento', nombre: e.nombre, detalle: 'creado' });
  return bien(e, 201);
}

export function editarEvento(estado: EstadoSim, id: string, cambios: Partial<CuerpoEvento>, quien: Personal, ahora: number) {
  const e = estado.eventos.find((x) => x.id === id);
  if (!e) return mal(404, 'no_encontrado', 'Ese evento no existe');
  // RF-045: de un evento pasado solo se corrige la descripción.
  const pasado = Date.parse(e.fin) < ahora;
  if (pasado && Object.keys(cambios).some((k) => k !== 'descripcion')) return mal(409, 'conflicto', 'De un evento pasado solo se puede cambiar la descripción');
  const nuevo = { ...e, ...cambios };
  const problema = validarEvento(nuevo);
  if (problema) return mal(400, 'datos_invalidos', problema);
  Object.assign(e, cambios);
  anotar(estado, ahora, quien, 'editar_evento', { tipo: 'evento', nombre: e.nombre, detalle: 'editado' });
  return bien(e);
}

export function eliminarEvento(estado: EstadoSim, id: string, quien: Personal, ahora: number) {
  const e = estado.eventos.find((x) => x.id === id);
  if (!e) return mal(404, 'no_encontrado', 'Ese evento no existe');
  if (Date.parse(e.inicio) < ahora) return mal(409, 'conflicto', 'Un evento que ya empezó no se elimina');
  estado.eventos = estado.eventos.filter((x) => x.id !== id);
  anotar(estado, ahora, quien, 'editar_evento', { tipo: 'evento', nombre: e.nombre, detalle: 'eliminado' });
  return bien(null, 204);
}

export function listarEventos(estado: EstadoSim) {
  return [...estado.eventos].sort((a, b) => a.inicio.localeCompare(b.inicio));
}
