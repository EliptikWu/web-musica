import { endpoints, VinculoPantalla } from '@rockola/contrato';
import { http, HttpResponse } from 'msw';
import { conEstado } from '../almacen';
import { pedirCodigo, vinculoDe } from '../gestion';
import { reportarPantalla } from '../motor';
import { COOKIE_PANTALLA, manejar, ok } from './comun';

/** La placa del QR, los reportes del reproductor y la vinculación (RF-019, RF-035, RF-058). */

const pedirCodigoPantalla = http.post(`/api${endpoints.pedirCodigoPantalla.ruta.replace('{slug}', ':slug')}`, async ({ params }) => {
  const token = crypto.randomUUID();
  const r = await conEstado((estado, ahora) => (params.slug === estado.bar.slug ? pedirCodigo(estado, token, ahora) : null));
  if (!r) return HttpResponse.json({ codigo: 'no_encontrado', mensaje: 'Ese bar no existe' }, { status: 404 });
  // En el navegador MSW no puede poner cookies httpOnly: se pone una normal (doc 03). El backend
  // la pone httpOnly y el JavaScript de la pantalla nunca la lee.
  document.cookie = `${COOKIE_PANTALLA}=${token}; Path=/; SameSite=Strict; Max-Age=31536000`;
  // Va por fuera de `manejar` solo para poner la cookie: se valida igual contra el contrato.
  if (!VinculoPantalla.safeParse(r).success) console.error('[simulación] «pedirCodigoPantalla» respondió fuera del contrato', r);
  return HttpResponse.json(r, { status: 201 });
});

export const manejadoresPantalla = [
  manejar('qrPublico', ({ estado }) => ok({ url: `${location.origin}/q/${estado.qr.token}` })),
  manejar('vinculoPantalla', ({ estado, ahora, tokenPantalla }) => ok(vinculoDe(estado, tokenPantalla, ahora))),
  pedirCodigoPantalla,
  manejar('reportarInicio', ({ estado, ahora, parametros }) => reportarPantalla(estado, parametros.id ?? '', 'inicio', ahora, null)),
  manejar('reportarFin', ({ estado, ahora, parametros }) => reportarPantalla(estado, parametros.id ?? '', 'fin', ahora, null)),
  manejar('reportarError', ({ estado, ahora, parametros, cuerpo }) =>
    reportarPantalla(estado, parametros.id ?? '', 'error', ahora, (cuerpo as { codigo_youtube: number | null }).codigo_youtube),
  ),
];
