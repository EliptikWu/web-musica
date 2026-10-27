import type { NextRequest } from 'next/server';

/**
 * Reenvío a la API del backend (doc 03). Todo el navegador habla con /api del mismo origen:
 * así la cookie de sesión es del mismo sitio y la CSP solo permite `connect-src 'self'`.
 * En modo simulado esta ruta no se usa: responde el Service Worker de la simulación.
 */
export const dynamic = 'force-dynamic';

const SIN_REENVIAR = new Set(['host', 'connection', 'content-length', 'transfer-encoding', 'keep-alive', 'upgrade', 'accept-encoding']);
// Las cabeceras de reenvío las puede inventar cualquiera: solo pasan si delante hay un proxy de
// confianza que las reescribe (la plataforma de despliegue), y así se declara con
// ROCKOLA_CONFIAR_PROXY=1. Sin eso, el backend no debe usar la IP para limitar intentos.
const DE_REENVIO = /^(x-forwarded-|x-real-ip$|forwarded$)/i;

async function reenviar(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  const base = process.env.ROCKOLA_API_URL;
  if (process.env.NEXT_PUBLIC_API_MODE !== 'real' || !base) {
    return Response.json(
      { codigo: 'error_interno', mensaje: 'No hay API configurada: en modo simulado responde la simulación del navegador.' },
      { status: 503 },
    );
  }
  const { path } = await params;
  if (path.some((parte) => parte === '..' || parte === '.' || parte.includes('/'))) {
    return Response.json({ codigo: 'datos_invalidos', mensaje: 'Ruta inválida' }, { status: 400 });
  }
  const destino = new URL(`${base.replace(/\/$/, '')}/${path.map(encodeURIComponent).join('/')}`);
  destino.search = request.nextUrl.search;

  const cabeceras = new Headers();
  const confiarProxy = process.env.ROCKOLA_CONFIAR_PROXY === '1';
  request.headers.forEach((valor, clave) => {
    if (SIN_REENVIAR.has(clave.toLowerCase())) return;
    if (DE_REENVIO.test(clave) && !confiarProxy) return;
    cabeceras.set(clave, valor);
  });
  const conCuerpo = !['GET', 'HEAD'].includes(request.method);
  try {
    const respuesta = await fetch(destino, {
      method: request.method,
      headers: cabeceras,
      body: conCuerpo ? request.body : undefined,
      redirect: 'manual',
      // @ts-expect-error: necesario en Node para reenviar un cuerpo en flujo.
      duplex: conCuerpo ? 'half' : undefined,
      cache: 'no-store',
    });
    const salida = new Headers(respuesta.headers);
    salida.delete('content-encoding');
    salida.delete('content-length');
    // El tiempo real (SSE) pasa tal cual, sin almacenarse.
    if (salida.get('content-type')?.includes('text/event-stream')) salida.set('Cache-Control', 'no-cache, no-transform');
    return new Response(respuesta.body, { status: respuesta.status, headers: salida });
  } catch (causa) {
    console.error('[api] el backend no respondió', destino.pathname, causa);
    return Response.json({ codigo: 'error_interno', mensaje: 'La API no respondió' }, { status: 502 });
  }
}

export { reenviar as GET, reenviar as POST, reenviar as PUT, reenviar as PATCH, reenviar as DELETE };
