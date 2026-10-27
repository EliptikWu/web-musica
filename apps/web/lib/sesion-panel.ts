/** Nombre de la cookie de sesión del panel. La pone el backend (httpOnly); el proxy solo mira si existe. */
export const COOKIE_PANEL = 'rockola_panel';

/** Rutas del panel que se ven sin sesión. */
export const RUTAS_PANEL_PUBLICAS = ['/panel/entrar'];

/**
 * Mientras se cierra la sesión a propósito, un 401 de una consulta que llega tarde no es una
 * sesión caducada: no hay que decir «Tu sesión terminó» a quien acaba de salir.
 */
export const salida = { enCurso: false };
