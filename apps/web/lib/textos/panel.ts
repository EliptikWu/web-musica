import type { AccionAdmin, EstadoServicio, Rol, TipoAccion } from '@rockola/contrato';
import { conPunto, formatoHora } from '../formato';

/**
 * Textos del panel (doc 05, «Textos»). El panel es del personal: «solicitudes» (no «pedidos»),
 * frases cortas, verbos en pasado para lo que ya pasó. La bitácora habla en segunda persona
 * cuando la acción es de quien mira («Bloqueaste…») y en tercera cuando es de otro.
 */

export const ROL: Record<Rol, string> = { administrador: 'Administrador', operador: 'Operador' };

export const P = {
  panelDeLaRockola: 'Panel de la rockola',
  entrar: 'Entrar',
  salir: 'Salir',
  operacion: 'Operación',
  suenaAhora: 'Suena ahora',
  cola: 'Cola',
  registro: 'Registro de acciones',
  estaNoche: 'Esta noche',
  metricas: 'Métricas de la noche',
  horario: 'Horario del QR',
  listaCasa: 'Lista de la casa',
  suenaSiVacia: 'Suena si la cola se vacía',
  bloqueadas: 'Canciones bloqueadas',
  sinBloqueos: 'No hay canciones bloqueadas.',
  colaVacia: 'No hay solicitudes en la cola.',
  nadaSuena: 'No suena nada.',
  sinListaActiva: 'No hay una lista de la casa activa: si la cola se vacía, no sonará nada.',
  sinRegistro: 'Todavía no hay acciones esta noche.',
  saltar: 'Saltar',
  bloquear: 'Bloquear',
  retirar: 'Retirar',
  pausarMusica: 'Pausar música',
  reanudarMusica: 'Reanudar música',
  desbloquear: 'Desbloquear',
  cancelar: 'Cancelar',
  datosDeEjemplo: 'Datos de ejemplo',
} as const;

/** La franja de estado (prototipo panel-admin y su variante #pausa). */
export function franjaEstado(estado: EstadoServicio, hasta: string | null, desde: string | null, proxima: string | null, forzado: boolean, zona: string) {
  const h = (f: string | null) => (f ? formatoHora(f, zona) : null);
  switch (estado) {
    case 'abierto':
      return {
        titulo: 'Abierto, recibiendo canciones',
        sub: forzado
          ? 'Abierto a mano, fuera del horario del QR.'
          : [h(desde) && conPunto(`Desde las ${h(desde)}`), h(hasta) && `Cierra a las ${h(hasta)} según el horario del QR.`].filter(Boolean).join(' '),
      };
    case 'en_pausa':
      return { titulo: 'En pausa, no se reciben canciones nuevas', sub: 'La cola sigue sonando. Quien escanee el QR verá que las solicitudes están en pausa.' };
    case 'fuera_de_horario':
      return { titulo: 'Fuera de horario', sub: h(proxima) ? `Las solicitudes abren a las ${h(proxima)} según el horario del QR.` : 'No hay un horario activo.' };
    case 'mantenimiento':
      return { titulo: 'En mantenimiento', sub: 'La noche está cerrada. Nadie puede pedir hasta que reanudes las solicitudes.' };
  }
}

export const acciones = {
  pausarSolicitudes: 'Pausar solicitudes',
  reanudarSolicitudes: 'Reanudar solicitudes',
  mantenimiento: 'Modo mantenimiento',
  abrirNoche: 'Abrir la noche',
  cerrarNoche: 'Cerrar la noche',
};

export const dialogos = {
  mantenimiento: (enCola: number) => ({
    titulo: '¿Poner la rockola en mantenimiento?',
    texto: `Se cierra la noche: ${enCola === 1 ? 'la canción que está en la cola se descarta' : `las ${enCola} canciones de la cola se descartan`} y nadie puede pedir hasta que reanudes las solicitudes. El historial y las métricas se guardan.`,
    confirmar: 'Poner en mantenimiento',
  }),
  cerrarNoche: (enCola: number) => ({
    titulo: '¿Cerrar la noche?',
    texto: `${enCola === 0 ? 'La cola está vacía.' : enCola === 1 ? 'La canción que está en la cola se descarta.' : `Las ${enCola} canciones de la cola se descartan.`} Las solicitudes no se abren otra vez hasta la próxima franja del horario.`,
    confirmar: 'Cerrar la noche',
  }),
  bloquear: (titulo: string) => ({
    titulo: `Bloquear «${titulo}»`,
    texto: 'Nadie podrá pedirla mientras dure el bloqueo. Si está sonando, se corta; si está en la cola, sale de ella.',
    motivo: 'Motivo',
    motivoAyuda: 'Lo verá el resto del personal en la bitácora.',
    duracion: 'Duración',
    horas: 'Horas',
    confirmar: 'Bloquear',
  }),
  retirar: (titulo: string) => ({
    titulo: `Retirar «${titulo}» de la cola`,
    texto: 'Quien la pidió verá que el bar la retiró y el motivo.',
    motivo: 'Motivo',
    detalle: 'Detalle (opcional)',
    confirmar: 'Retirar',
  }),
};

export const DURACIONES = [
  { valor: 'fin_sesion', texto: 'Hasta el fin de la noche' },
  { valor: 'horas', texto: 'Por horas' },
  { valor: 'indefinido', texto: 'Hasta levantarlo' },
] as const;

export const MOTIVOS_RETIRO = [
  { valor: 'inapropiada', texto: 'Inapropiada' },
  { valor: 'duplicada', texto: 'Duplicada' },
  { valor: 'otro', texto: 'Otro' },
] as const;

export const errores = {
  generico: 'No se pudo hacer. Revisa tu conexión e inténtalo de nuevo.',
  sinPermiso: 'Tu rol no permite esta acción.',
  noPendiente: 'Esa canción ya no está en la cola.',
  motivoVacio: 'Escribe el motivo.',
  horas: 'Escribe cuántas horas, de 1 a 168.',
};

export const entrada = {
  titulo: 'Entrar al panel',
  correo: 'Correo',
  clave: 'Clave',
  mostrar: 'Mostrar clave',
  ocultar: 'Ocultar clave',
  credenciales: 'Correo o clave incorrectos.',
  bloqueada: (hora: string) => `Demasiados intentos. Vuelve a intentarlo a las ${hora}.`,
  caducada: 'Tu sesión terminó. Entra de nuevo.',
  olvido: '¿Olvidaste tu clave? Pídele al administrador que la restablezca.',
  simulacion: 'Simulación: entra con admin@rockola.demo (Administrador) u operador@rockola.demo (Operador), clave rockola-demo.',
  cambiarTitulo: 'Cambia tu clave',
  cambiarTexto: 'Es tu primer ingreso o tu clave se restableció: elige una nueva para seguir.',
  actual: 'Clave actual (la temporal)',
  nueva: 'Clave nueva',
  nuevaAyuda: 'Al menos 10 caracteres.',
  repetir: 'Repite la clave nueva',
  noCoinciden: 'Las dos claves nuevas no coinciden.',
  guardar: 'Guardar y entrar',
  sinPermisoTitulo: 'Esta sección es del Administrador',
  sinPermisoTexto: 'Tu rol de Operador controla la música y la cola de la noche.',
  volver: 'Volver a la operación',
};

// ── Bitácora: la frase de cada acción ────────────────────────────────────────

const VERBO: Partial<Record<TipoAccion, [tu: string, otro: string]>> = {
  saltar: ['Saltaste', 'saltó'],
  bloquear: ['Bloqueaste', 'bloqueó'],
  desbloquear: ['Desbloqueaste', 'desbloqueó'],
  retirar: ['Retiraste', 'retiró'],
  pausar_reproduccion: ['Pausaste la música', 'pausó la música'],
  reanudar_reproduccion: ['Reanudaste la música', 'reanudó la música'],
  pausar_solicitudes: ['Pausaste las solicitudes', 'pausó las solicitudes'],
  reanudar_solicitudes: ['Reanudaste las solicitudes', 'reanudó las solicitudes'],
  forzar_apertura: ['Abriste las solicitudes fuera del horario', 'abrió las solicitudes fuera del horario'],
  mantenimiento: ['Pusiste la rockola en mantenimiento', 'puso la rockola en mantenimiento'],
  abrir_sesion: ['Abriste la noche', 'abrió la noche'],
  cerrar_sesion: ['Cerraste la noche', 'cerró la noche'],
  editar_horario: ['Cambiaste el horario del QR', 'cambió el horario del QR'],
  editar_lista: ['Editaste la lista', 'editó la lista'],
  activar_lista: ['Activaste la lista', 'activó la lista'],
  crear_usuario: ['Creaste el usuario', 'creó el usuario'],
  editar_usuario: ['Editaste el usuario', 'editó el usuario'],
  desactivar_usuario: ['Desactivaste el usuario', 'desactivó el usuario'],
  reactivar_usuario: ['Reactivaste el usuario', 'reactivó el usuario'],
  restablecer_clave: ['Restableciste la clave de', 'restableció la clave de'],
  editar_establecimiento: ['Editaste los datos del bar', 'editó los datos del bar'],
  regenerar_qr: ['Regeneraste el QR', 'regeneró el QR'],
  editar_evento: ['Editaste el evento', 'editó el evento'],
  extender_bloqueo: ['Cambiaste el bloqueo de', 'cambió el bloqueo de'],
};

function objetivoTexto(a: AccionAdmin): string {
  if (!a.objetivo) return '';
  if (a.objetivo.tipo === 'cancion') return ` «${a.objetivo.nombre}»${a.objetivo.detalle ? `, de ${a.objetivo.detalle}` : ''}`;
  if (['usuario', 'lista', 'evento'].includes(a.objetivo.tipo)) return ` «${a.objetivo.nombre}»`;
  return '';
}

/** La frase de una acción, desde el punto de vista de quien mira el panel. */
export function fraseAccion(a: AccionAdmin, yoId: string | undefined): string {
  if (a.tipo === 'abrir_sesion' && !a.usuario) return 'Se abrieron las solicitudes con el horario del QR';
  if (a.tipo === 'cerrar_sesion' && !a.usuario) return 'Se cerraron las solicitudes con el horario del QR';
  if (a.tipo === 'error_youtube') return `YouTube no pudo reproducir${objetivoTexto(a)}${a.detalle ? `: ${a.detalle}` : ''}`;
  if (a.tipo === 'intento_no_permitido') return `${a.usuario?.nombre ?? 'Alguien'} intentó algo que su rol no permite${a.detalle ? `: ${a.detalle.toLowerCase()}` : ''}`;
  const [tu, otro] = VERBO[a.tipo] ?? ['Hiciste un cambio', 'hizo un cambio'];
  const sujeto = !a.usuario ? `El sistema ${otro}` : a.usuario.id !== yoId ? `${a.usuario.nombre} ${otro}` : tu;
  const cola = a.tipo === 'retirar' && a.detalle ? ` de la cola: ${a.detalle}` : a.tipo === 'extender_bloqueo' && a.detalle ? `: ${a.detalle.toLowerCase()}` : '';
  return `${sujeto}${objetivoTexto(a)}${cola}`;
}

/**
 * Las líneas del registro, de la más reciente a la más vieja. Saltar y bloquear la misma
 * canción en menos de un minuto, por la misma persona, es una sola línea («Saltaste
 * «Gasolina», de Daddy Yankee, y la bloqueaste»), como en el prototipo.
 */
export function lineasRegistro(accionesRecientes: readonly AccionAdmin[], yoId: string | undefined): { id: string; fecha: string; texto: string }[] {
  const lineas: { id: string; fecha: string; texto: string }[] = [];
  for (let i = 0; i < accionesRecientes.length; i++) {
    const a = accionesRecientes[i]!;
    const siguiente = accionesRecientes[i + 1];
    const juntas =
      a.tipo === 'bloquear' &&
      siguiente?.tipo === 'saltar' &&
      siguiente.usuario?.id === a.usuario?.id &&
      siguiente.objetivo?.nombre === a.objetivo?.nombre &&
      Math.abs(Date.parse(a.fecha) - Date.parse(siguiente.fecha)) < 60_000;
    if (juntas) {
      const propia = a.usuario !== null && a.usuario.id === yoId;
      lineas.push({ id: a.id, fecha: siguiente!.fecha, texto: `${fraseAccion(siguiente!, yoId)}, y ${propia ? 'la bloqueaste' : 'la bloqueó'}` });
      i++;
    } else lineas.push({ id: a.id, fecha: a.fecha, texto: fraseAccion(a, yoId) });
  }
  return lineas;
}
