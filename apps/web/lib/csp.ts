/**
 * Política de contenido (doc 03, «Seguridad»): estricta y con un nonce por petición.
 * El reproductor de YouTube solo se permite en la pantalla del bar.
 */
export function construirCsp({ nonce, desarrollo, https, pantalla }: { nonce: string; desarrollo: boolean; https: boolean; pantalla: boolean }): string {
  return [
    "default-src 'self'",
    // En desarrollo el empaquetador evalúa código y mete estilos sueltos al recargar.
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${desarrollo ? " 'unsafe-eval'" : ''}`,
    desarrollo ? "style-src 'self' 'unsafe-inline'" : `style-src 'self' 'nonce-${nonce}'`,
    "img-src 'self' data: blob: https://i.ytimg.com",
    "font-src 'self'",
    `connect-src 'self'${desarrollo ? ' ws:' : ''}`,
    // El Service Worker de la simulación: con 'strict-dynamic', 'self' de script-src no cuenta.
    "worker-src 'self'",
    pantalla ? 'frame-src https://www.youtube-nocookie.com https://www.youtube.com' : "frame-src 'none'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
    ...(https ? ['upgrade-insecure-requests'] : []),
  ].join('; ');
}

/** ¿Es la pantalla del bar? `/b/<slug>/pantalla`, con o sin barra final. */
export function esRutaPantalla(ruta: string): boolean {
  return /^\/b\/[^/]+\/pantalla\/?$/.test(ruta);
}
