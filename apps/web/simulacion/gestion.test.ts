import { EventoBar, ListaCasaDetalle, PaginaBitacora, UsuarioPanel } from '@rockola/contrato';
import { describe, expect, it } from 'vitest';
import { crearEstado } from './datos/semilla';
import {
  activarLista, agregarALista, bitacora, cambiarEstadoUsuario, crearEvento, crearLista, crearUsuario, editarEvento, editarUsuario,
  eliminarEvento, eliminarLista, guardarHorario, listaDetalle, listarUsuarios, ordenarLista, quitarDeLista, regenerarQr, restablecerClave,
} from './gestion';
import { ahoraSim } from './motor';

const ADMIN = { id: 'u-admin', nombre: 'Admin de prueba' };
function escena() {
  const estado = crearEstado('momento-b', 'persona', 1_000_000);
  return { estado, ahora: ahoraSim(estado, 1_000_000) };
}

describe('usuarios (RF-023 a RF-026)', () => {
  it('crear: clave temporal y cambio obligatorio; el correo no se repite', () => {
    const { estado, ahora } = escena();
    const r = crearUsuario(estado, { nombre: 'Barra 2', correo: 'Barra2@rockola.demo', rol: 'operador' }, ADMIN, ahora);
    if (!r.ok) throw new Error(r.mensaje);
    UsuarioPanel.parse(r.cuerpo.usuario);
    expect(r.cuerpo.usuario).toMatchObject({ correo: 'barra2@rockola.demo', debe_cambiar_clave: true });
    expect(r.cuerpo.clave_temporal.length).toBeGreaterThan(5);
    expect(crearUsuario(estado, { nombre: 'Otro', correo: 'barra2@rockola.demo', rol: 'operador' }, ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
  });

  it('no se puede quitar el último Administrador ni desactivarse a sí mismo', () => {
    const { estado, ahora } = escena();
    expect(editarUsuario(estado, 'u-admin', { rol: 'operador' }, ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
    expect(cambiarEstadoUsuario(estado, 'u-admin', false, ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
    expect(cambiarEstadoUsuario(estado, 'u-operador', false, ADMIN, ahora)).toMatchObject({ ok: true, cuerpo: { estado: 'inactivo' } });
  });

  it('restablecer la clave cierra sus sesiones abiertas', () => {
    const { estado, ahora } = escena();
    estado.sesiones_panel.tok = 'u-operador';
    const r = restablecerClave(estado, 'u-operador', ADMIN, ahora);
    expect(r.ok).toBe(true);
    expect(estado.sesiones_panel.tok).toBeUndefined();
    expect(estado.usuarios.find((u) => u.id === 'u-operador')?.debe_cambiar_clave).toBe(true);
  });

  it('buscar y filtrar', () => {
    const { estado } = escena();
    expect(listarUsuarios(estado, { q: 'oper' }).map((u) => u.nombre)).toEqual(['Operador de prueba']);
    expect(listarUsuarios(estado, { rol: 'operador' })).toHaveLength(2);
  });
});

describe('horario (RF-036)', () => {
  it('rechaza franjas que se pisan, también cruzando medianoche', () => {
    const { estado, ahora } = escena();
    const franjas = [
      { dia_semana: 5, hora_inicio: '20:00', hora_fin: '03:00', activo: true },
      { dia_semana: 6, hora_inicio: '02:00', hora_fin: '06:00', activo: true },
    ];
    expect(guardarHorario(estado, franjas, ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
    expect(guardarHorario(estado, [{ dia_semana: 1, hora_inicio: '20:00', hora_fin: '20:00', activo: true }], ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'datos_invalidos' });
    expect(guardarHorario(estado, [franjas[0]!], ADMIN, ahora)).toMatchObject({ ok: true });
  });
});

describe('listas de la casa (RF-027 a RF-033)', () => {
  it('crear, armar, ordenar y activar', () => {
    const { estado, ahora } = escena();
    const creada = crearLista(estado, 'Boleros', ADMIN, ahora);
    if (!creada.ok) throw new Error(creada.mensaje);
    const id = creada.cuerpo.id;
    expect(crearLista(estado, 'boleros', ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
    expect(activarLista(estado, id, 'secuencial', ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
    agregarALista(estado, id, 'sim-idilio', ADMIN, ahora);
    agregarALista(estado, id, 'sim-decisiones', ADMIN, ahora);
    expect(agregarALista(estado, id, 'sim-idilio', ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
    expect(agregarALista(estado, id, 'sim-gasolina', ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'bloqueada' });
    const lista = ListaCasaDetalle.parse(listaDetalle(estado, estado.listas.find((l) => l.id === id)!));
    const r = ordenarLista(estado, id, [...lista.canciones].reverse().map((c) => c.elemento_id), ADMIN, ahora);
    if (!r.ok) throw new Error(r.mensaje);
    expect(r.cuerpo.canciones.map((c) => c.cancion.titulo)).toEqual(['Decisiones', 'Idilio']);
    expect(activarLista(estado, id, 'aleatoria', ADMIN, ahora)).toMatchObject({ ok: true, cuerpo: { activa: true, modo_rotacion: 'aleatoria' } });
    expect(estado.listas.filter((l) => l.activa).map((l) => l.id)).toEqual([id]);
  });

  it('la activa no se elimina ni se queda vacía', () => {
    const { estado, ahora } = escena();
    expect(eliminarLista(estado, 'l-salsa', ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
    expect(eliminarLista(estado, 'l-crossover', ADMIN, ahora)).toMatchObject({ ok: true });
    const l = estado.listas.find((x) => x.id === 'l-salsa')!;
    l.elementos = l.elementos.slice(0, 1);
    expect(quitarDeLista(estado, 'l-salsa', l.elementos[0]!.elemento_id, ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
  });
});

describe('QR (RF-035)', () => {
  it('regenerar deja al anterior como reemplazado', () => {
    const { estado, ahora } = escena();
    const r = regenerarQr(estado, 'http://localhost:3200', ADMIN, ahora);
    expect(r).toMatchObject({ ok: true, cuerpo: { token: 'demo-qr-2', url: 'http://localhost:3200/q/demo-qr-2' } });
    expect(estado.qr.anteriores).toContain('demo-qr-1');
  });
});

describe('bitácora (RF-044)', () => {
  it('filtra por tipo y por el sistema, y pagina de 25 en 25', () => {
    const { estado, ahora } = escena();
    for (let i = 0; i < 30; i++) estado.acciones.push({ id: `x-${i}`, fecha: ahora - i * 1000, usuario: ADMIN, tipo: 'saltar', objetivo: null, detalle: null });
    const primera = PaginaBitacora.parse(bitacora(estado, { tipo: 'saltar' }));
    expect(primera.acciones).toHaveLength(25);
    expect(primera.siguiente).toBe('25');
    expect(bitacora(estado, { tipo: 'saltar', cursor: '25' }).acciones).toHaveLength(6);
    expect(bitacora(estado, { usuario: 'sistema' }).acciones.map((a) => a.tipo)).toEqual(['abrir_sesion']);
  });

  it('con la misma hora, la última acción va arriba (el reloj quieto de la simulación)', () => {
    const { estado, ahora } = escena();
    for (const [id, tipo] of [['a-9', 'pausar_solicitudes'], ['a-10', 'reanudar_solicitudes']] as const) estado.acciones.push({ id, fecha: ahora + 1, usuario: ADMIN, tipo, objetivo: null, detalle: null });
    expect(bitacora(estado, {}).acciones.slice(0, 2).map((a) => a.tipo)).toEqual(['reanudar_solicitudes', 'pausar_solicitudes']);
  });
});

describe('eventos (RF-045)', () => {
  it('de un evento pasado solo se cambia la descripción; uno que ya empezó no se elimina', () => {
    const { estado, ahora } = escena();
    expect(editarEvento(estado, 'ev-2', { nombre: 'Otro' }, ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
    expect(editarEvento(estado, 'ev-2', { descripcion: 'Se vio en la pantalla grande' }, ADMIN, ahora)).toMatchObject({ ok: true });
    expect(eliminarEvento(estado, 'ev-2', ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'conflicto' });
    expect(eliminarEvento(estado, 'ev-3', ADMIN, ahora)).toMatchObject({ ok: true });
  });

  it('el fin va después del inicio', () => {
    const { estado, ahora } = escena();
    const malo = { nombre: 'X', tipo_evento: 'otro' as const, inicio: '2026-11-01T22:00:00-05:00', fin: '2026-11-01T21:00:00-05:00', descripcion: null };
    expect(crearEvento(estado, malo, ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'datos_invalidos' });
    const bueno = crearEvento(estado, { ...malo, fin: '2026-11-02T01:00:00-05:00' }, ADMIN, ahora);
    if (!bueno.ok) throw new Error(bueno.mensaje);
    EventoBar.parse(bueno.cuerpo);
  });
});
