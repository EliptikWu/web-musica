import type { Resultado } from '../motor';
import {
  anotarAviso, buscar, cambiarVoto, construirAhora, construirDestacadas, construirYo, pedirCancion, registrarDispositivo, votar,
} from '../motor';
import { pedidasEnElBar } from '../analitica';
import { error, manejar, ok, type Peticion } from './comun';

/** Rutas del cliente y de la pantalla del bar (doc 04, «Cliente» y «Pantalla del bar»). */

function conDispositivo<T>(p: Peticion, fn: (dispositivo: string) => Resultado<T>): Resultado<T> {
  if (!p.dispositivo) return error(400, 'datos_invalidos', 'Falta X-Rockola-Dispositivo');
  return fn(p.dispositivo);
}

export const manejadoresCliente = [
  manejar('resolverQr', ({ estado, parametros }) => {
    if (parametros.token === estado.qr.token) return ok({ slug: estado.bar.slug });
    if (estado.qr.anteriores.includes(parametros.token ?? '')) return error(410, 'qr_reemplazado', 'Este QR ya no está en uso');
    return error(404, 'no_encontrado', 'Ese QR no existe');
  }),
  manejar('bar', ({ estado }) =>
    ok({
      slug: estado.bar.slug,
      nombre: estado.bar.nombre,
      zona_horaria: estado.bar.zona_horaria,
      pedidos_maximos: estado.bar.pedidos_maximos,
      duracion_max_seg: estado.bar.duracion_max_seg,
      ventana_no_repeticion_min: estado.bar.ventana_no_repeticion_min,
      horario: estado.horario,
    }),
  ),
  manejar('ahora', ({ estado, ahora }) => ok(construirAhora(estado, ahora))),
  manejar('destacadas', ({ estado }) => ok(construirDestacadas(estado))),
  manejar('registrarDispositivo', (p) =>
    conDispositivo(p, (d) => ok(registrarDispositivo(p.estado, d, (p.cuerpo as { tipo: string }).tipo, p.ahora))),
  ),
  manejar('yo', (p) => conDispositivo(p, (d) => ok(construirYo(p.estado, d)))),
  manejar('avisoVisto', (p) =>
    conDispositivo(p, (d) => {
      anotarAviso(p.estado, d, p.ahora);
      return ok(null, 204);
    }),
  ),
  manejar('buscar', ({ estado, ahora, consulta }) => buscar(estado, consulta.get('q') ?? '', ahora, pedidasEnElBar(estado, ahora))),
  manejar('pedir', (p) => conDispositivo(p, (d) => pedirCancion(p.estado, d, (p.cuerpo as { video_id: string }).video_id, p.ahora))),
  manejar('votar', (p) => conDispositivo(p, (d) => votar(p.estado, d, p.parametros.id ?? '', p.ahora))),
  manejar('cambiarVoto', (p) =>
    conDispositivo(p, (d) => {
      const { desde, hacia } = p.cuerpo as { desde: string; hacia: string };
      return cambiarVoto(p.estado, d, desde, hacia, p.ahora);
    }),
  ),
];
