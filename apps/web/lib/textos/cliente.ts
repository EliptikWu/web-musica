import type { EstadoServicio, EstadoSolicitud, MotivoNoPedible } from '@rockola/contrato';
import { conPunto, formatoEspera, formatoHora } from '../formato';

/**
 * Textos del cliente (doc 05, «Textos»), revisados con la skill de redacción. Vocabulario fijo:
 * «pedido» y «pedir» (no «solicitud», que es del panel), «voto», «cola», «lista de la casa».
 * Los errores dicen qué pasó y qué hacer; no se disculpan ni muestran códigos.
 */

export const T = {
  privacidadBreve: 'Sin registro ni descargas: tu teléfono queda como un código anónimo.',
  politica: 'Política de datos',
  entrar: 'Entrar y votar',
  enVivo: 'En vivo',
  loMasVotado: 'Lo más votado de la noche',
  sinVotosAun: 'Nadie ha votado todavía esta noche.',
  suenaAhora: 'Suena ahora',
  nadaSuena: 'No suena nada ahora',
  listaDeLaCasa: 'Lista de la casa',
  pideUnaCancion: 'Pide una canción',
  laCola: 'La cola',
  cargando: 'Cargando la rockola…',
  buscarEtiqueta: 'Buscar en YouTube',
  buscarMarcador: 'Canción o artista',
  borrarBusqueda: 'Borrar búsqueda',
  buscarVacio: 'Escribe el nombre de una canción o de un artista.',
  yaEnLaCola: 'Ya está en la cola',
  votaEnVez: 'Vota en vez de pedirla',
  resultadosYoutube: 'Resultados en YouTube',
  verCola: 'Ver cola',
  reintentar: 'Reintentar',
  verQueSuena: 'Ver qué está sonando',
  tuCancionVaDe: 'Tu canción va de',
  tuCancionSuena: 'Tu canción está sonando',
  tusPedidos: 'Tus pedidos de esta noche',
  sinPedidosPropios: 'Todavía no has pedido canciones esta noche.',
  pedirUna: 'Pedir una canción',
} as const;

export function cupo(restantes: number, maximo: number): string {
  if (restantes <= 0) return `Usaste tus ${maximo} pedidos`;
  return restantes === 1 ? `Te queda 1 de ${maximo} pedidos` : `Te quedan ${restantes} de ${maximo} pedidos`;
}

export const sinCupo = (maximo: number) => ({
  titulo: `Ya tienes ${maximo} canciones en la cola`,
  meta: 'Cuando suene una de las tuyas podrás pedir otra. Mientras tanto, vota.',
});

/** Motivo corto, en la columna de la tecla (76 px). */
export const MOTIVO_CORTO: Record<MotivoNoPedible | 'sin_cupo' | 'cerrado', string> = {
  bloqueada: 'El bar la bloqueó',
  no_disponible: 'No está disponible',
  no_embebible: 'No se puede poner aquí',
  muy_larga: 'Dura más de 10 min',
  reproducida_hace_poco: 'Sonó hace poco',
  sin_cupo: 'Sin pedidos libres',
  cerrado: 'Pedidos cerrados',
};

/** Motivo largo, cuando el rechazo llega al pulsar «Pedir». */
export function motivoLargo(motivo: MotivoNoPedible, disponibleDesde: string | null, zona?: string): string {
  switch (motivo) {
    case 'bloqueada':
      return 'el bar la bloqueó.';
    case 'no_disponible':
      return 'el video ya no está disponible en YouTube.';
    case 'no_embebible':
      return 'YouTube no deja reproducirla fuera de su página.';
    case 'muy_larga':
      return 'dura más de 10 minutos.';
    case 'reproducida_hace_poco':
      return disponibleDesde ? `sonó hace poco; podrás pedirla desde las ${formatoHora(disponibleDesde, zona)}.` : 'sonó hace poco.';
  }
}

export const busqueda = {
  sinResultados: (q: string) => ({ titulo: `No encontramos «${q}»`, meta: 'Revisa cómo se escribe o busca por el nombre del artista.' }),
  youtubeCaido: { titulo: 'YouTube no respondió', meta: 'La búsqueda falló, pero la música sigue sonando.' },
  sinRed: { titulo: 'No hay conexión', meta: 'Revisa tu señal o el wifi del bar e inténtalo de nuevo.' },
  desdeCache: 'YouTube no responde ahora: estos resultados son de búsquedas anteriores y pueden estar incompletos.',
};

export const pedido = {
  entro: (titulo: string, puesto: number, esperaSeg: number) => ({ titulo: `${titulo} entró a la cola`, meta: `Puesto #${puesto}, suena ${formatoEspera(esperaSeg)}` }),
  eraVoto: (titulo: string, puesto: number) => ({ titulo: `${titulo} ya estaba en la cola`, meta: `Le sumaste tu voto: va de #${puesto}.` }),
  rechazado: (titulo: string, motivo: string) => ({ titulo: `No se puede pedir ${titulo}`, meta: motivo.charAt(0).toUpperCase() + motivo.slice(1) }),
  cerrado: { titulo: 'Los pedidos se acaban de cerrar', meta: 'Puedes seguir viendo la cola.' },
  fallo: (titulo: string) => ({ titulo: `No pudimos pedir ${titulo}`, meta: 'Revisa tu conexión e inténtalo de nuevo.' }),
};

export const voto = {
  fallo: (titulo: string) => ({ titulo: `No se registró tu voto por ${titulo}`, meta: 'Revisa tu conexión e inténtalo de nuevo.' }),
  yaVotaste: (titulo: string) => ({ titulo: `Ya habías votado por ${titulo}`, meta: 'Cada teléfono vota una vez por canción.' }),
  yaNoEsta: (titulo: string) => ({ titulo: `${titulo} ya no está en la cola`, meta: 'Acaba de sonar o el bar la retiró.' }),
  cerrado: { titulo: 'Ahora no se puede votar', meta: 'Los pedidos y los votos están cerrados.' },
  etiqueta: (titulo: string) => `Votar por ${titulo}`,
  votaste: (titulo: string) => `Ya votaste por ${titulo}`,
  tuya: (titulo: string) => `${titulo} la pediste tú`,
};

export function estadoPedido(estado: EstadoSolicitud, puesto: number | null): string {
  switch (estado) {
    case 'pendiente':
      return puesto ? `En espera, puesto #${puesto}` : 'En espera';
    case 'reproduciendo':
      return 'Sonando ahora';
    case 'reproducida':
      return 'Ya sonó';
    case 'saltada':
      return 'Omitida por el bar';
    case 'no_disponible':
      return 'No se pudo reproducir';
    case 'bloqueada':
      return 'Bloqueada por el bar';
    case 'retirada':
      return 'Retirada por el bar';
    case 'expirada':
      return 'La noche cerró antes de que sonara';
  }
}

export const colaVacia = {
  conLista: { titulo: 'La cola está vacía.', meta: 'Mientras tanto suena la lista de la casa. Pide una canción y será la siguiente.' },
  sinLista: { titulo: 'La cola está vacía.', meta: 'Pide una canción y será la primera en sonar.' },
};

export function desempate(antes: string, despues: string) {
  return { regla: 'Con los mismos votos, suena primero la que se pidió antes.', caso: { antes, despues } };
}

export const ESPERAS_ESTIMADAS = 'Las esperas son aproximadas: cambian con los votos.';

/** El texto de la luz según el estado del servicio. */
export function luzServicio(estado: EstadoServicio): string {
  return { abierto: 'En vivo', fuera_de_horario: 'Pedidos cerrados', en_pausa: 'En pausa', mantenimiento: 'En mantenimiento' }[estado];
}

/** Aviso corto encima de la cola cuando no se puede votar. */
export function colaCerrada(estado: Exclude<EstadoServicio, 'abierto'>): string {
  return {
    en_pausa: 'Los pedidos están en pausa: la cola sigue sonando, pero por ahora no se puede votar.',
    mantenimiento: 'La rockola está en mantenimiento: por ahora no se puede pedir ni votar.',
    fuera_de_horario: 'Los pedidos están cerrados: puedes ver lo que suena, pero no votar.',
  }[estado];
}

export const falloCarga = { titulo: 'La rockola no cargó', meta: 'Revisa tu conexión e inténtalo de nuevo.' };

/** Lo que ve el cliente cuando no se reciben canciones (RF-003, PB-30). */
export function servicioCerrado(estado: Exclude<EstadoServicio, 'abierto'>, proximaApertura: string | null, zona: string, ahora: number) {
  const cuando = proximaApertura ? `${diaRelativo(proximaApertura, ahora, zona)} a las ${formatoHora(proximaApertura, zona)}` : null;
  switch (estado) {
    case 'fuera_de_horario':
      return {
        luz: luzServicio(estado),
        titulo: 'Por ahora no recibimos canciones',
        // La hora ya termina en punto («8:00 p. m.»): no se le pone otro.
        detalle: cuando ? `${conPunto(`Los pedidos abren ${cuando}`)} Mientras tanto suena la lista de la casa.` : 'Mientras tanto suena la lista de la casa.',
      };
    case 'en_pausa':
      return { luz: luzServicio(estado), titulo: 'Los pedidos están en pausa', detalle: 'El bar los pausó por un momento. La cola sigue sonando y puedes verla.' };
    case 'mantenimiento':
      return { luz: luzServicio(estado), titulo: 'La rockola está en mantenimiento', detalle: 'El bar está preparando la próxima noche. Vuelve a escanear el QR cuando abran.' };
  }
}

/** «hoy», «mañana» o «el jueves», en la zona del bar. */
export function diaRelativo(fecha: string, ahora: number, zona: string): string {
  const dia = (ms: number) => new Intl.DateTimeFormat('en-CA', { timeZone: zona, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(ms));
  const objetivo = Date.parse(fecha);
  if (dia(objetivo) === dia(ahora)) return 'hoy';
  if (dia(objetivo) === dia(ahora + 86_400_000)) return 'mañana';
  return `el ${new Intl.DateTimeFormat('es-CO', { timeZone: zona, weekday: 'long' }).format(new Date(objetivo))}`;
}

export const conexion = {
  luz: 'Sin conexión',
  reconectando: 'Se perdió la conexión en vivo. Reconectando…',
  desconectado: 'Sin conexión en vivo: lo que ves puede estar desactualizado.',
};

export const qr = {
  reemplazado: { titulo: 'Este QR ya no está en uso', detalle: 'El bar cambió su código. Escanea el QR que está ahora en tu mesa.' },
  abriendo: 'Abriendo la rockola del bar…',
};
