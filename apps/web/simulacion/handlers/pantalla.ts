import { reportarPantalla } from '../motor';
import { manejar, ok } from './comun';

/** La placa del QR y los reportes del reproductor (RF-019, RF-035). */
export const manejadoresPantalla = [
  manejar('qrPublico', ({ estado }) => ok({ url: `${location.origin}/q/${estado.qr.token}` })),
  manejar('reportarInicio', ({ estado, ahora, parametros }) => reportarPantalla(estado, parametros.id ?? '', 'inicio', ahora, null)),
  manejar('reportarFin', ({ estado, ahora, parametros }) => reportarPantalla(estado, parametros.id ?? '', 'fin', ahora, null)),
  manejar('reportarError', ({ estado, ahora, parametros, cuerpo }) =>
    reportarPantalla(estado, parametros.id ?? '', 'error', ahora, (cuerpo as { codigo_youtube: number | null }).codigo_youtube),
  ),
];
