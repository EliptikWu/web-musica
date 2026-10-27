import { NextResponse, type NextRequest } from 'next/server';
import { construirCsp, esRutaPantalla } from './lib/csp';
import { COOKIE_PANEL, RUTAS_PANEL_PUBLICAS } from './lib/sesion-panel';

/**
 * Antes de cada página: su política de contenido con un nonce nuevo y, en el panel, quien no
 * trae cookie de sesión va a la puerta. Que la sesión valga de verdad lo decide la API.
 */
export function proxy(request: NextRequest) {
  const nonce = btoa(crypto.randomUUID());
  const { pathname } = request.nextUrl;
  const csp = construirCsp({
    nonce,
    desarrollo: process.env.NODE_ENV !== 'production',
    https: request.nextUrl.protocol === 'https:',
    pantalla: esRutaPantalla(pathname),
  });

  const enPanel = pathname === '/panel' || pathname.startsWith('/panel/');
  if (enPanel && !RUTAS_PANEL_PUBLICAS.includes(pathname) && !request.cookies.has(COOKIE_PANEL)) {
    const puerta = request.nextUrl.clone();
    puerta.pathname = '/panel/entrar';
    puerta.search = pathname === '/panel' ? '' : `?volver=${encodeURIComponent(pathname)}`;
    const respuesta = NextResponse.redirect(puerta);
    respuesta.headers.set('Content-Security-Policy', csp);
    return respuesta;
  }

  // Next toma el nonce de la política que viaja en la petición y lo pone en sus scripts.
  const cabeceras = new Headers(request.headers);
  cabeceras.set('x-nonce', nonce);
  cabeceras.set('Content-Security-Policy', csp);
  const respuesta = NextResponse.next({ request: { headers: cabeceras } });
  respuesta.headers.set('Content-Security-Policy', csp);
  return respuesta;
}

export const config = {
  // Todo menos la API, los estáticos y los archivos con extensión (incluido mockServiceWorker.js).
  // `\\.` y no `\.`: dentro de un string de JS, `\.` es un punto cualquiera y `/b/pacifico`
  // (termina en «ico») se saltaría la CSP.
  matcher: ['/((?!api/|_next/static|_next/image|.*\\.(?:js|svg|png|jpg|ico|woff2|txt|webmanifest)$).*)'],
};
