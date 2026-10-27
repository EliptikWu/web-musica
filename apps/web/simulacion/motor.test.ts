import { Ahora, Destacada, Noche, Yo } from '@rockola/contrato';
import { describe, expect, it } from 'vitest';
import { crearEstado } from './datos/semilla';
import { recortar } from './almacen';
import {
  abrirSesionManual, activarMantenimiento, ahoraSim, avanzar, bloquear, buscar, cambiarModoQr, cerrarSesionManual, construirAhora,
  construirDestacadas, construirNoche, construirYo, desbloquear, pedirCancion, reportarPantalla, retirar, saltar, votar,
} from './motor';

const PERSONA = 'persona-0000';
const OTRO = 'otro-telefono';
const REAL = 1_000_000;
const ADMIN = { id: 'u-admin', nombre: 'Admin de prueba' };

function escena(nombre: Parameters<typeof crearEstado>[0] = 'momento-b') {
  const estado = crearEstado(nombre, PERSONA, REAL);
  const ahora = ahoraSim(estado, REAL);
  return { estado, ahora };
}

describe('la semilla reproduce los prototipos', () => {
  it('momento B: cola de 9, Monaco y La Bicicleta empatadas, Monaco antes', () => {
    const { estado, ahora } = escena();
    const foto = Ahora.parse(construirAhora(estado, ahora));
    expect(foto.reproduccion?.cancion.titulo).toBe('Rebelión');
    expect(foto.reproduccion?.posicion_seg).toBe(161);
    expect(foto.reproduccion?.votos).toBe(24);
    expect(foto.cola.map((i) => `${i.cancion.titulo} ${i.votos_total}`)).toEqual([
      'Cali Pachanguero 21', 'DtMF 18', 'Monaco 15', 'La Bicicleta 15', 'NUEVAYoL 14', 'TQG 9', 'Luna 7', 'Una aventura 4', 'Tití Me Preguntó 2',
    ]);
    expect(foto.cola.find((i) => i.cancion.titulo === 'Monaco')?.puesto_anterior).toBe(5);
    expect(foto.cola.find((i) => i.cancion.titulo === 'DtMF')?.puesto_anterior).toBe(3);
    expect(foto.servicio.estado).toBe('abierto');
    expect(foto.lista_casa).toEqual({ nombre: 'Salsa y tropical de la casa', total_canciones: 42 });
  });

  it('momento B, el teléfono de la persona: Monaco es suya, votó DtMF, le queda 1 de 3', () => {
    const { estado } = escena();
    const yo = Yo.parse(construirYo(estado, PERSONA));
    expect(yo.pedidos_restantes).toBe(1);
    expect(yo.solicitudes.map((s) => [s.cancion.titulo, s.estado, s.puesto])).toEqual([
      ['Monaco', 'pendiente', 3],
      ['Rebelión', 'reproduciendo', null],
    ]);
    expect(yo.votos).toContain('s-dtmf');
  });

  it('el panel cuadra con el prototipo: 48 solicitudes, 31 teléfonos, 162 votos, 11 min', () => {
    const { estado, ahora } = escena();
    const noche = Noche.parse(construirNoche(estado, ahora));
    expect(noche.metricas).toMatchObject({ solicitudes: 48, telefonos: 31, votos: 162, espera_promedio_seg: 660 });
    expect(noche.reproduccion?.pedida_por).toBe('7F3K');
    expect(noche.bloqueos.map((b) => b.cancion.titulo).sort()).toEqual(['Despechá', 'Gasolina', 'La Bachata']);
    expect(noche.registro.map((a) => a.tipo)).toEqual(['bloquear', 'bloquear', 'saltar', 'abrir_sesion']);
    expect(noche.lista_casa?.total_canciones).toBe(42);
  });

  it('momento A: le quedan 2 de 3 y lo más votado es Rebelión, Hips Don’t Lie y Cali Pachanguero', () => {
    const { estado } = escena('momento-a');
    expect(construirYo(estado, PERSONA).pedidos_restantes).toBe(2);
    const { destacadas } = construirDestacadas(estado);
    destacadas.forEach((d) => Destacada.parse(d));
    expect(destacadas.map((d) => [d.cancion.titulo, d.votos, d.contexto])).toEqual([
      ['Rebelión', 24, 'sonando'],
      ["Hips Don't Lie", 19, 'sono'],
      ['Cali Pachanguero', 15, 'en_cola'],
    ]);
  });
});

describe('buscar y pedir (RN-01 a RN-07)', () => {
  it('momento A: «bad bunny» separa lo que ya está en la cola de los resultados de YouTube', () => {
    const { estado, ahora } = escena('momento-a');
    const r = buscar(estado, 'bad bunny', ahora);
    if (!r.ok) throw new Error('debía responder');
    expect(r.cuerpo.en_cola.map((i) => i.cancion.titulo)).toEqual(['NUEVAYoL', 'DtMF']);
    const mix = r.cuerpo.resultados.find((x) => x.cancion.titulo === 'Mix Bad Bunny 2025');
    expect(mix).toMatchObject({ pedible: false, motivo: 'muy_larga' });
    expect(r.cuerpo.resultados.some((x) => x.cancion.titulo === 'Monaco' && x.pedible)).toBe(true);
  });

  it('momento A: Monaco entra de cuarta, suena en unos 14 min y queda 1 de 3', () => {
    const { estado, ahora } = escena('momento-a');
    const r = pedirCancion(estado, PERSONA, 'sim-monaco', ahora);
    expect(r).toMatchObject({ ok: true, estado: 201, cuerpo: { resultado: 'creada', puesto: 4, pedidos_restantes: 1 } });
    if (r.ok) expect(Math.floor(r.cuerpo.espera_seg / 60)).toBe(14);
  });

  it('pedir una que ya está en la cola cuenta como voto (RN-02) y no gasta cupo', () => {
    const { estado, ahora } = escena();
    const r = pedirCancion(estado, OTRO, 'sim-tqg', ahora);
    expect(r).toMatchObject({ ok: true, cuerpo: { resultado: 'votada' } });
    expect(construirYo(estado, OTRO).pedidos_restantes).toBe(3);
    expect(pedirCancion(estado, OTRO, 'sim-tqg', ahora)).toMatchObject({ ok: false, codigo: 'ya_votaste' });
  });

  it('con 3 pedidos en uso no deja pedir otro (RN-01)', () => {
    const { estado, ahora } = escena();
    expect(pedirCancion(estado, PERSONA, 'sim-provenza', ahora)).toMatchObject({ ok: true });
    expect(pedirCancion(estado, PERSONA, 'sim-classy-101', ahora)).toMatchObject({ ok: false, estado: 409, codigo: 'limite_pedidos' });
  });

  it('rechaza la bloqueada, la larga y la que sonó hace poco, con su motivo (RN-06, RN-07)', () => {
    const { estado, ahora } = escena();
    expect(pedirCancion(estado, OTRO, 'sim-gasolina', ahora)).toMatchObject({ ok: false, codigo: 'bloqueada' });
    expect(pedirCancion(estado, OTRO, 'sim-mix-bad-bunny-2025', ahora)).toMatchObject({ ok: false, codigo: 'muy_larga' });
    const r = pedirCancion(estado, OTRO, 'sim-rebelion', ahora);
    expect(r).toMatchObject({ ok: false, codigo: 'reproducida_hace_poco' });
    if (!r.ok) expect(r.detalle?.disponible_desde).toBeTypeOf('string');
  });

  it('un voto que empata no adelanta; uno que desempata sube y queda anotado', () => {
    const { estado, ahora } = escena();
    expect(votar(estado, OTRO, 's-bici', ahora)).toMatchObject({ ok: true, cuerpo: { votos_total: 16 } });
    const foto = construirAhora(estado, ahora);
    const bici = foto.cola.find((i) => i.solicitud_id === 's-bici')!;
    expect(bici.puesto).toBe(3);
    expect(bici.puesto_anterior).toBe(4);
    expect(votar(estado, OTRO, 's-bici', ahora)).toMatchObject({ ok: false, codigo: 'ya_votaste' });
  });

  it('en pausa no se pide ni se vota, pero la cola se ve', () => {
    const { estado, ahora } = escena('en-pausa');
    expect(construirAhora(estado, ahora).servicio.estado).toBe('en_pausa');
    expect(construirAhora(estado, ahora).cola).toHaveLength(9);
    expect(votar(estado, OTRO, 's-bici', ahora)).toMatchObject({ ok: false, estado: 423, codigo: 'servicio_no_disponible' });
  });
});

describe('la noche avanza sola (RF-019, RF-020, RN-08)', () => {
  it('al terminar Rebelión suena Cali Pachanguero, justo a su hora', () => {
    const { estado, ahora } = escena();
    avanzar(estado, ahora + 197_000);
    const foto = construirAhora(estado, ahora + 197_000);
    expect(foto.reproduccion?.cancion.titulo).toBe('Cali Pachanguero');
    expect(foto.reproduccion?.posicion_seg).toBe(0);
    expect(foto.cola).toHaveLength(8);
    expect(construirYo(estado, PERSONA).solicitudes.find((s) => s.solicitud_id === 's-rebelion')?.estado).toBe('reproducida');
  });

  it('sin pedidos suena la lista de la casa, y vuelve a los pedidos sin cortar la canción en curso', () => {
    const { estado, ahora } = escena('cola-vacia');
    avanzar(estado, ahora + 197_000);
    expect(construirAhora(estado, ahora + 197_000).reproduccion).toMatchObject({ origen: 'lista_casa', cancion: { titulo: 'Rebelión' } });
    const t = ahora + 200_000;
    expect(pedirCancion(estado, OTRO, 'sim-provenza', t)).toMatchObject({ ok: true });
    expect(construirAhora(estado, t).reproduccion?.origen).toBe('lista_casa');
    avanzar(estado, ahora + 197_000 + 358_000);
    expect(construirAhora(estado, ahora + 197_000 + 358_000).reproduccion?.cancion.titulo).toBe('PROVENZA');
  });

  it('sin lista activa y sin pedidos, no suena nada y el panel avisa', () => {
    const { estado, ahora } = escena('sin-lista-activa');
    avanzar(estado, ahora + 200_000);
    expect(construirAhora(estado, ahora + 200_000).reproduccion).toBeNull();
    expect(construirNoche(estado, ahora + 200_000).alertas.map((a) => a.tipo)).toContain('sin_lista_activa');
  });

  it('a las 2:00 a. m. se cierra la noche y lo pendiente expira', () => {
    const { estado, ahora } = escena();
    const dos = Date.parse('2026-10-23T02:00:30-05:00');
    avanzar(estado, dos);
    expect(estado.sesion).toBeNull();
    expect(construirAhora(estado, dos).servicio.estado).toBe('fuera_de_horario');
    expect(estado.solicitudes.filter((s) => s.estado === 'pendiente')).toHaveLength(0);
    expect(ahora).toBeLessThan(dos);
  });

  it('fuera de horario abre sola a las 8:00 p. m.', () => {
    const { estado } = escena('fuera-de-horario');
    const ocho = Date.parse('2026-10-22T20:00:10-05:00');
    expect(construirAhora(estado, ahoraSim(estado, REAL)).servicio.estado).toBe('fuera_de_horario');
    avanzar(estado, ocho);
    expect(estado.sesion?.origen).toBe('horario');
    expect(construirAhora(estado, ocho).servicio.estado).toBe('abierto');
  });

  it('el reporte de fin de la pantalla avanza; uno tardío no hace nada', () => {
    const { estado, ahora } = escena();
    const id = estado.reproduccion!.id;
    reportarPantalla(estado, id, 'fin', ahora + 1000, null);
    expect(construirAhora(estado, ahora + 1000).reproduccion?.cancion.titulo).toBe('Cali Pachanguero');
    const version = estado.version;
    reportarPantalla(estado, id, 'fin', ahora + 2000, null);
    expect(estado.version).toBe(version);
  });

  it('un error del reproductor marca la solicitud y queda en la bitácora', () => {
    const { estado, ahora } = escena();
    reportarPantalla(estado, estado.reproduccion!.id, 'error', ahora, 150);
    expect(construirYo(estado, PERSONA).solicitudes.find((s) => s.solicitud_id === 's-rebelion')?.estado).toBe('no_disponible');
    expect(estado.acciones.at(-1)).toMatchObject({ tipo: 'error_youtube', detalle: 'El reproductor dio el error 150' });
  });
});

describe('el panel (RF-038 a RF-043)', () => {
  it('saltar es idempotente', () => {
    const { estado, ahora } = escena();
    const id = estado.reproduccion!.id;
    saltar(estado, id, ADMIN, ahora);
    const version = estado.version;
    saltar(estado, id, ADMIN, ahora);
    expect(estado.version).toBe(version);
    expect(construirAhora(estado, ahora).reproduccion?.cancion.titulo).toBe('Cali Pachanguero');
  });

  it('bloquear la que suena la corta, y bloquear una de la cola la saca con su motivo', () => {
    const { estado, ahora } = escena();
    bloquear(estado, { video_id: 'sim-rebelion', motivo: 'Prueba', duracion: 'fin_sesion', horas: null }, ADMIN, ahora);
    expect(construirAhora(estado, ahora).reproduccion?.cancion.titulo).toBe('Cali Pachanguero');
    bloquear(estado, { video_id: 'sim-monaco', motivo: 'Repetida', duracion: 'horas', horas: 2 }, ADMIN, ahora);
    expect(construirYo(estado, PERSONA).solicitudes.find((s) => s.solicitud_id === 's-monaco')).toMatchObject({ estado: 'bloqueada', motivo: 'Repetida' });
    const b = estado.bloqueos.find((x) => x.video_id === 'sim-monaco')!;
    desbloquear(estado, b.id, ADMIN, ahora);
    expect(pedirCancion(estado, OTRO, 'sim-monaco', ahora)).toMatchObject({ ok: true });
  });

  it('un bloqueo por horas vence solo', () => {
    const { estado, ahora } = escena();
    bloquear(estado, { video_id: 'sim-provenza', motivo: 'Prueba', duracion: 'horas', horas: 1 }, ADMIN, ahora);
    expect(pedirCancion(estado, OTRO, 'sim-provenza', ahora)).toMatchObject({ ok: false, codigo: 'bloqueada' });
    expect(pedirCancion(estado, OTRO, 'sim-provenza', ahora + 3600_001)).toMatchObject({ ok: true });
  });

  it('retirar saca la canción con su motivo', () => {
    const { estado, ahora } = escena();
    retirar(estado, 's-titi', 'duplicada', ADMIN, ahora);
    expect(construirAhora(estado, ahora).cola).toHaveLength(8);
    expect(retirar(estado, 's-titi', 'duplicada', ADMIN, ahora)).toMatchObject({ ok: false, codigo: 'no_pendiente' });
  });

  it('mantenimiento cierra la noche y expira lo pendiente (RN-10); reanudar abre una nueva', () => {
    const { estado, ahora } = escena();
    activarMantenimiento(estado, ADMIN, ahora);
    expect(construirAhora(estado, ahora).servicio.estado).toBe('mantenimiento');
    expect(construirAhora(estado, ahora).cola).toHaveLength(0);
    expect(construirYo(estado, PERSONA).solicitudes.find((s) => s.solicitud_id === 's-monaco')).toBeUndefined();
    cambiarModoQr(estado, 'automatico', ADMIN, ahora + 1000);
    expect(estado.sesion).not.toBeNull();
    expect(construirAhora(estado, ahora + 1000).servicio.estado).toBe('abierto');
  });
});

describe('la noche abierta y cerrada a mano (barrido de la iteración 0)', () => {
  it('cerrada a mano dentro del horario no se reabre sola hasta que acaba la franja', () => {
    const { estado, ahora } = escena();
    cerrarSesionManual(estado, ADMIN, ahora);
    avanzar(estado, ahora + 60_000);
    expect(estado.sesion).toBeNull();
    // A las 8:00 p. m. del viernes abre la franja siguiente.
    const viernes = Date.parse('2026-10-23T20:00:30-05:00');
    avanzar(estado, viernes);
    expect(estado.sesion?.origen).toBe('horario');
  });

  it('abierta a mano fuera de horario se cierra si el servicio vuelve al horario', () => {
    const { estado } = escena('fuera-de-horario');
    const ahora = ahoraSim(estado, REAL);
    abrirSesionManual(estado, 'normal', ADMIN, ahora);
    expect(estado.modo_qr).toBe('forzado_activo');
    cambiarModoQr(estado, 'automatico', ADMIN, ahora + 1000);
    expect(estado.sesion).toBeNull();
    expect(construirAhora(estado, ahora + 1000).servicio.estado).toBe('fuera_de_horario');
  });
});

describe('el almacén no crece sin límite', () => {
  it('recorta historial, bitácora y solicitudes terminadas, pero no lo vigente', () => {
    const { estado, ahora } = escena();
    for (let i = 0; i < 900; i++) {
      estado.historial.push({ video_id: 'sim-rebelion', origen: 'lista_casa', solicitud_id: null, sesion_id: null, inicio: ahora, fin: ahora, resultado: 'completa' });
      estado.acciones.push({ id: `x-${i}`, fecha: ahora, usuario: null, tipo: 'saltar', objetivo: null, detalle: null });
      estado.solicitudes.push({ ...estado.solicitudes[0]!, id: `vieja-${i}`, estado: 'reproducida' });
    }
    recortar(estado, Date.now());
    expect(estado.historial.length).toBe(400);
    expect(estado.acciones.length).toBe(400);
    expect(estado.solicitudes.filter((s) => s.estado === 'pendiente')).toHaveLength(9);
    expect(estado.solicitudes.some((s) => s.id === 's-rebelion')).toBe(true);
  });
});
