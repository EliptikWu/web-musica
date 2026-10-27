import { describe, expect, it } from 'vitest';
import { ACCIONES, puede } from './permisos';

describe('matriz de permisos (D9, doc 04)', () => {
  it('el Operador controla la noche: saltar, pausar, retirar y bloquear hasta el fin de la noche', () => {
    for (const accion of ['verOperacion', 'saltar', 'pausarMusica', 'pausarSolicitudes', 'retirar', 'bloquearNoche'] as const) {
      expect(puede('operador', accion), accion).toBe(true);
    }
  });

  it('lo demás es solo del Administrador', () => {
    for (const accion of ['bloquearMas', 'desbloquear', 'mantenimiento', 'abrirCerrarNoche', 'gestion', 'bitacora', 'analitica'] as const) {
      expect(puede('operador', accion), accion).toBe(false);
      expect(puede('administrador', accion), accion).toBe(true);
    }
  });

  it('sin sesión no se puede nada', () => {
    for (const accion of Object.keys(ACCIONES) as (keyof typeof ACCIONES)[]) expect(puede(undefined, accion)).toBe(false);
  });
});
