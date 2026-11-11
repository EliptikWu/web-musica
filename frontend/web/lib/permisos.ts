import type { Rol } from '@rockola/contrato';

/**
 * Matriz rol × acción del panel (doc 04, «Permisos»; D9, *propuesta* hasta que el equipo la
 * confirme). La interfaz la usa para ocultar lo que el rol no puede hacer; **quien decide es
 * el servidor**, que responde 403 y lo anota en la bitácora.
 */
export const ACCIONES = {
  verOperacion: ['operador', 'administrador'],
  saltar: ['operador', 'administrador'],
  pausarMusica: ['operador', 'administrador'],
  pausarSolicitudes: ['operador', 'administrador'],
  retirar: ['operador', 'administrador'],
  bloquearNoche: ['operador', 'administrador'],
  bloquearMas: ['administrador'],
  desbloquear: ['administrador'],
  mantenimiento: ['administrador'],
  abrirCerrarNoche: ['administrador'],
  gestion: ['administrador'],
  bitacora: ['administrador'],
  analitica: ['administrador'],
} as const satisfies Record<string, readonly Rol[]>;

export type Accion = keyof typeof ACCIONES;

export function puede(rol: Rol | undefined, accion: Accion): boolean {
  return rol !== undefined && (ACCIONES[accion] as readonly Rol[]).includes(rol);
}
