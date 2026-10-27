import type { CambioEstablecimiento, CuerpoEvento, Franja } from '@rockola/contrato';
import type { z } from 'zod';
import {
  activarLista, agregarALista, bitacora, buscarCatalogo, cambiarEstadoUsuario, crearEvento, crearLista, crearUsuario, editarEstablecimiento,
  editarEvento, editarUsuario, eliminarEvento, eliminarLista, establecimientoAContrato, guardarHorario, listaDetalle, listaResumen,
  listarEventos, listarUsuarios, ordenarLista, qrAContrato, quitarDeLista, regenerarQr, renombrarLista, restablecerClave,
} from '../gestion';
import { error, manejar, ok, type Peticion } from './comun';

/** La gestión del panel (doc 04, «Panel»): usuarios, bar y QR, horario, listas, bitácora y eventos. */

const quien = (p: Peticion) => ({ id: p.personal!.id, nombre: p.personal!.nombre });
const cuerpo = <T>(p: Peticion) => p.cuerpo as T;

export const manejadoresGestion = [
  // Horario
  manejar('horario', ({ estado }) => ok({ zona_horaria: estado.bar.zona_horaria, franjas: estado.horario })),
  manejar('guardarHorario', (p) => guardarHorario(p.estado, cuerpo<{ franjas: Franja[] }>(p).franjas, quien(p), p.ahora)),

  // Listas de la casa
  manejar('listas', ({ estado, consulta }) => {
    const q = (consulta.get('q') ?? '').toLowerCase();
    return ok({ listas: estado.listas.filter((l) => !q || l.nombre.toLowerCase().includes(q)).map((l) => listaResumen(estado, l)) });
  }),
  manejar('crearLista', (p) => crearLista(p.estado, cuerpo<{ nombre: string }>(p).nombre, quien(p), p.ahora)),
  manejar('lista', ({ estado, parametros }) => {
    const l = estado.listas.find((x) => x.id === parametros.id);
    return l ? ok(listaDetalle(estado, l)) : error(404, 'no_encontrado', 'Esa lista no existe');
  }),
  manejar('renombrarLista', (p) => renombrarLista(p.estado, p.parametros.id ?? '', cuerpo<{ nombre: string }>(p).nombre, quien(p), p.ahora)),
  manejar('eliminarLista', (p) => eliminarLista(p.estado, p.parametros.id ?? '', quien(p), p.ahora)),
  manejar('agregarALista', (p) => agregarALista(p.estado, p.parametros.id ?? '', cuerpo<{ video_id: string }>(p).video_id, quien(p), p.ahora)),
  manejar('quitarDeLista', (p) => quitarDeLista(p.estado, p.parametros.id ?? '', p.parametros.elemento ?? '', quien(p), p.ahora)),
  manejar('ordenarLista', (p) => ordenarLista(p.estado, p.parametros.id ?? '', cuerpo<{ elementos: string[] }>(p).elementos, quien(p), p.ahora)),
  manejar('activarLista', (p) => activarLista(p.estado, p.parametros.id ?? '', cuerpo<{ modo_rotacion: 'secuencial' | 'aleatoria' }>(p).modo_rotacion, quien(p), p.ahora)),
  manejar('buscarPanel', ({ estado, consulta }) => {
    if (estado.fallos.youtube === 'caido') return error(503, 'youtube_no_disponible', 'YouTube no respondió');
    const q = consulta.get('q') ?? '';
    return ok({ consulta: q, resultados: buscarCatalogo(estado, q) });
  }),

  // Usuarios
  manejar('usuarios', ({ estado, consulta }) => ok({ usuarios: listarUsuarios(estado, { q: consulta.get('q'), rol: consulta.get('rol'), estado: consulta.get('estado') }) })),
  manejar('crearUsuario', (p) => crearUsuario(p.estado, cuerpo<{ nombre: string; correo: string; rol: 'operador' | 'administrador' }>(p), quien(p), p.ahora)),
  manejar('editarUsuario', (p) => editarUsuario(p.estado, p.parametros.id ?? '', cuerpo<{ nombre?: string; correo?: string; rol?: 'operador' | 'administrador' }>(p), quien(p), p.ahora)),
  manejar('claveTemporal', (p) => restablecerClave(p.estado, p.parametros.id ?? '', quien(p), p.ahora)),
  manejar('desactivarUsuario', (p) => cambiarEstadoUsuario(p.estado, p.parametros.id ?? '', false, quien(p), p.ahora)),
  manejar('reactivarUsuario', (p) => cambiarEstadoUsuario(p.estado, p.parametros.id ?? '', true, quien(p), p.ahora)),

  // Establecimiento y QR
  manejar('establecimiento', ({ estado }) => ok(establecimientoAContrato(estado))),
  manejar('editarEstablecimiento', (p) => editarEstablecimiento(p.estado, cuerpo<z.infer<typeof CambioEstablecimiento>>(p), quien(p), p.ahora)),
  manejar('qrEstablecimiento', ({ estado }) => ok(qrAContrato(estado, location.origin))),
  manejar('regenerarQr', (p) => regenerarQr(p.estado, location.origin, quien(p), p.ahora)),

  // Bitácora
  manejar('bitacora', ({ estado, consulta }) =>
    ok(bitacora(estado, { desde: consulta.get('desde'), hasta: consulta.get('hasta'), usuario: consulta.get('usuario'), tipo: consulta.get('tipo'), cursor: consulta.get('cursor') })),
  ),

  // Eventos
  manejar('eventos', ({ estado }) => ok({ eventos: listarEventos(estado) })),
  manejar('crearEvento', (p) => crearEvento(p.estado, cuerpo<CuerpoEvento>(p), quien(p), p.ahora)),
  manejar('editarEvento', (p) => editarEvento(p.estado, p.parametros.id ?? '', cuerpo<Partial<CuerpoEvento>>(p), quien(p), p.ahora)),
  manejar('eliminarEvento', (p) => eliminarEvento(p.estado, p.parametros.id ?? '', quien(p), p.ahora)),
];
