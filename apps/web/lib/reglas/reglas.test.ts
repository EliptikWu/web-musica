import { describe, expect, it } from 'vitest';
import { duracionCola, ejemploDesempate, ordenarCola, puestosSubidos, renumerar } from './cola';
import { esperaHasta, posicionActual, restanteActual } from './espera';
import { abiertoPorHorario, cierreDeFranja, diasQueAbren, momentoLocal, proximaApertura, validarFranjas, type FranjaHorario } from './horario';
import { aceptaPedidos, estadoTecla } from './pedidos';
import { formatoEspera, formatoResumenCola, minutosEnteros } from '../formato';

// Momento B de prototipos/herramientas/datos-de-ejemplo.md: jueves, 10:45 p. m. en Bogotá.
const AHORA = '2026-10-22T22:45:00-05:00';
const hace = (min: number) => new Date(Date.parse(AHORA) - min * 60000).toISOString();
const item = (id: string, titulo: string, dur: number, votos: number, pedidaHace: number) => ({
  solicitud_id: id,
  cancion: { titulo, duracion_seg: dur },
  votos_total: votos,
  fecha_solicitud: hace(pedidaHace),
  puesto: 0,
  puesto_anterior: null as number | null,
});
const COLA_B = [
  item('s-cali', 'Cali Pachanguero', 352, 21, 26),
  item('s-dtmf', 'DtMF', 237, 18, 31),
  item('s-bici', 'La Bicicleta', 227, 15, 9),
  item('s-monaco', 'Monaco', 267, 15, 14),
  item('s-nuevayol', 'NUEVAYoL', 183, 14, 22),
  item('s-tqg', 'TQG', 197, 9, 7),
  item('s-luna', 'Luna', 196, 7, 5),
  item('s-aventura', 'Una aventura', 285, 4, 4),
  item('s-titi', 'Tití Me Preguntó', 243, 2, 2),
];
const REBELION = { posicion_seg: 161, estado: 'sonando' as const, cancion: { duracion_seg: 358 } };

describe('orden de la cola (RN-05)', () => {
  it('con los mismos votos va primero la que se pidió antes: Monaco antes que La Bicicleta', () => {
    const titulos = ordenarCola(COLA_B).map((i) => i.cancion.titulo);
    expect(titulos.slice(0, 5)).toEqual(['Cali Pachanguero', 'DtMF', 'Monaco', 'La Bicicleta', 'NUEVAYoL']);
  });

  it('no cambia la cola original', () => {
    const copia = [...COLA_B];
    ordenarCola(COLA_B);
    expect(COLA_B).toEqual(copia);
  });

  it('renumera y recuerda el puesto anterior para «Subió N puestos»', () => {
    const numerada = renumerar(COLA_B.map((x, i) => ({ ...x, puesto: i + 1 })));
    const monaco = numerada.find((x) => x.solicitud_id === 's-monaco')!;
    expect(monaco.puesto).toBe(3);
    expect(monaco.puesto_anterior).toBe(4);
    expect(puestosSubidos(monaco)).toBe(1);
    expect(puestosSubidos({ puesto: 3, puesto_anterior: 5 })).toBe(2);
    expect(puestosSubidos({ puesto: 5, puesto_anterior: 3 })).toBe(0);
    expect(puestosSubidos({ puesto: 1, puesto_anterior: null })).toBe(0);
  });

  it('explica el desempate con la canción propia si está empatada', () => {
    const ordenada = ordenarCola(COLA_B);
    const par = ejemploDesempate(ordenada, new Set(['s-monaco']));
    expect(par?.antes.cancion.titulo).toBe('Monaco');
    expect(par?.despues.cancion.titulo).toBe('La Bicicleta');
    expect(ejemploDesempate([ordenada[0]!])).toBeNull();
  });
});

describe('espera estimada (RF-018)', () => {
  const ahoraMs = Date.parse(AHORA);

  it('la cola de 9 canciones dura unos 36 min', () => {
    expect(formatoResumenCola(9, duracionCola(COLA_B))).toBe('9 canciones, unos 36 min');
  });

  it('Monaco, en el puesto 3, suena en unos 13 min', () => {
    const ordenada = ordenarCola(COLA_B);
    const restante = restanteActual(REBELION, AHORA, ahoraMs);
    expect(restante).toBe(197);
    expect(formatoEspera(esperaHasta(ordenada, 2, restante))).toBe('en unos 13 min');
  });

  it('momento A: Monaco entra de cuarto y suena en unos 14 min (14:30 no redondea a 15)', () => {
    const cola = [{ cancion: { duracion_seg: 352 } }, { cancion: { duracion_seg: 183 } }, { cancion: { duracion_seg: 237 } }];
    const espera = esperaHasta(cola, 3, 98);
    expect(espera).toBe(870);
    expect(minutosEnteros(espera)).toBe(14);
  });

  it('el avance sigue el reloj del servidor y se detiene si la reproducción está en pausa', () => {
    expect(posicionActual(REBELION, AHORA, ahoraMs + 10_000)).toBe(171);
    expect(posicionActual({ ...REBELION, estado: 'pausada' }, AHORA, ahoraMs + 10_000)).toBe(161);
    expect(posicionActual(REBELION, AHORA, ahoraMs + 999_000)).toBe(358);
    expect(restanteActual(null, AHORA, ahoraMs)).toBe(0);
  });

  it('dice «en menos de 1 min» y no «unos 0 min»', () => {
    expect(formatoEspera(45)).toBe('en menos de 1 min');
  });
});

describe('horario que cruza medianoche (RF-036, RN-09)', () => {
  const zona = 'America/Bogota';
  const franjas: FranjaHorario[] = [4, 5, 6].map((d) => ({ dia_semana: d, hora_inicio: '20:00', hora_fin: '02:00', activo: true }));

  it('el viernes a la 1:30 a. m. sigue abierta la franja del jueves', () => {
    expect(abiertoPorHorario(franjas, new Date('2026-10-23T01:30:00-05:00'), zona)).toBe(true);
  });

  it('a las 2:00 a. m. ya está cerrado', () => {
    expect(abiertoPorHorario(franjas, new Date('2026-10-23T02:00:00-05:00'), zona)).toBe(false);
  });

  it('el jueves a las 7:59 p. m. está cerrado y abre a las 8:00 p. m.', () => {
    const antes = new Date('2026-10-22T19:59:00-05:00');
    expect(abiertoPorHorario(franjas, antes, zona)).toBe(false);
    expect(proximaApertura(franjas, antes, zona)?.toISOString()).toBe(new Date('2026-10-22T20:00:00-05:00').toISOString());
  });

  it('a las 10:45 p. m. del jueves cierra a las 2:00 a. m.', () => {
    expect(cierreDeFranja(franjas, new Date(AHORA), zona)?.toISOString()).toBe(new Date('2026-10-23T02:00:00-05:00').toISOString());
  });

  it('el domingo por la noche la próxima apertura es el jueves', () => {
    const domingo = new Date('2026-10-25T21:00:00-05:00');
    expect(momentoLocal(domingo, zona).dia).toBe(7);
    expect(proximaApertura(franjas, domingo, zona)?.toISOString()).toBe(new Date('2026-10-29T20:00:00-05:00').toISOString());
  });

  it('una franja inactiva no abre', () => {
    expect(abiertoPorHorario([{ ...franjas[0]!, activo: false }], new Date(AHORA), zona)).toBe(false);
    expect(proximaApertura([], new Date(AHORA), zona)).toBeNull();
  });

  it('detecta franjas solapadas, también a través de medianoche, y las de duración cero', () => {
    expect(validarFranjas(franjas)).toEqual([]);
    const solapada: FranjaHorario = { dia_semana: 5, hora_inicio: '01:00', hora_fin: '03:00', activo: true };
    // Viernes 1:00–3:00 a. m. pisa la franja del jueves (que acaba el viernes a las 2:00), no la del viernes por la noche.
    expect(validarFranjas([...franjas, solapada])).toEqual([{ tipo: 'solapada', indices: [0, 3] }]);
    expect(validarFranjas([{ dia_semana: 1, hora_inicio: '20:00', hora_fin: '20:00', activo: true }])).toEqual([{ tipo: 'misma_hora', indices: [0] }]);
  });

  it('dice qué días abren', () => {
    expect([...diasQueAbren(franjas)]).toEqual([4, 5, 6]);
  });
});

describe('tecla de cada canción (RN-03, RN-04)', () => {
  const yo = { votos: ['s-dtmf', 's-monaco'], solicitudes: [{ solicitud_id: 's-monaco', estado: 'pendiente' }] };

  it('la propia es «Tuya» aunque cuente con su voto automático', () => {
    expect(estadoTecla('s-monaco', yo)).toBe('tuya');
  });

  it('la ya votada es «Votaste» y el resto «+1»', () => {
    expect(estadoTecla('s-dtmf', yo)).toBe('votaste');
    expect(estadoTecla('s-cali', yo)).toBe('votar');
    expect(estadoTecla('s-cali', null)).toBe('votar');
  });

  it('solo se pide y se vota con el servicio abierto', () => {
    expect(aceptaPedidos('abierto')).toBe(true);
    expect(aceptaPedidos('en_pausa')).toBe(false);
    expect(aceptaPedidos('fuera_de_horario')).toBe(false);
  });
});
