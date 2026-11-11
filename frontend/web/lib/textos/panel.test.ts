import type { AccionAdmin } from '@rockola/contrato';
import { describe, expect, it } from 'vitest';
import { fraseAccion, franjaEstado, lineasRegistro } from './panel';

const admin = { id: 'u-admin', nombre: 'Admin de prueba' };
const accion = (id: string, hora: string, tipo: AccionAdmin['tipo'], nombre?: string, detalle?: string, usuario: AccionAdmin['usuario'] = admin): AccionAdmin => ({
  id,
  fecha: `2026-10-22T${hora}:00-05:00`,
  usuario,
  tipo,
  objetivo: nombre ? { tipo: 'cancion', nombre, detalle: detalle ?? null } : null,
  detalle: null,
});

// El registro del prototipo panel-admin, de la más reciente a la más vieja.
const NOCHE: AccionAdmin[] = [
  accion('a-4', '22:41', 'bloquear', 'La Bachata', 'Manuel Turizo'),
  { ...accion('a-3', '22:32', 'bloquear', 'Gasolina', 'Daddy Yankee'), fecha: '2026-10-22T22:32:05-05:00' },
  accion('a-2', '22:32', 'saltar', 'Gasolina', 'Daddy Yankee'),
  { ...accion('a-1', '20:00', 'abrir_sesion'), usuario: null, objetivo: { tipo: 'sesion', nombre: 'Noche', detalle: null } },
];

describe('registro de acciones', () => {
  it('dice las tres líneas del prototipo, en segunda persona para quien las hizo', () => {
    expect(lineasRegistro(NOCHE, 'u-admin').map((l) => l.texto)).toEqual([
      'Bloqueaste «La Bachata», de Manuel Turizo',
      'Saltaste «Gasolina», de Daddy Yankee, y la bloqueaste',
      'Se abrieron las solicitudes con el horario del QR',
    ]);
  });

  it('en tercera persona para otro miembro del personal', () => {
    expect(lineasRegistro(NOCHE, 'u-operador')[1]?.texto).toBe('Admin de prueba saltó «Gasolina», de Daddy Yankee, y la bloqueó');
  });

  it('un retiro dice su motivo; un intento sin permiso, quién fue', () => {
    expect(fraseAccion({ ...accion('x', '23:00', 'retirar', 'TQG', 'Karol G y Shakira'), detalle: 'Duplicada' }, 'u-admin')).toBe('Retiraste «TQG», de Karol G y Shakira de la cola: Duplicada');
    expect(fraseAccion({ ...accion('y', '23:00', 'intento_no_permitido'), usuario: { id: 'u-op', nombre: 'Operador de prueba' }, detalle: 'Modo mantenimiento' }, 'u-admin')).toBe(
      'Operador de prueba intentó algo que su rol no permite: modo mantenimiento',
    );
  });
});

describe('acciones del sistema (barrido de la iteración 3)', () => {
  it('se dicen en impersonal, no como si las hubiera hecho quien mira', () => {
    const sistema = { ...accion('s', '23:00', 'saltar', 'Luna', 'Feid y ATL Jacob'), usuario: null };
    expect(fraseAccion(sistema, 'u-admin')).toBe('El sistema saltó «Luna», de Feid y ATL Jacob');
    const bloqueo = { ...accion('b', '23:00', 'bloquear', 'Luna', 'Feid y ATL Jacob'), usuario: null };
    expect(lineasRegistro([bloqueo, sistema], 'u-admin')[0]?.texto).toBe('El sistema saltó «Luna», de Feid y ATL Jacob, y la bloqueó');
  });
});

describe('franja de estado', () => {
  const normal = (s: string) => s.replace(/[  ]/g, ' ');
  it('abierta, como en el prototipo', () => {
    const f = franjaEstado('abierto', '2026-10-23T02:00:00-05:00', '2026-10-22T20:00:00-05:00', null, false, 'America/Bogota');
    expect(f.titulo).toBe('Abierto, recibiendo canciones');
    expect(normal(f.sub)).toBe('Desde las 8:00 p. m. Cierra a las 2:00 a. m. según el horario del QR.');
  });

  it('en pausa, como en el prototipo', () => {
    expect(franjaEstado('en_pausa', null, null, null, false, 'America/Bogota').titulo).toBe('En pausa, no se reciben canciones nuevas');
  });
});
