/**
 * Textos de la pantalla del bar (doc 05, «Textos»). Se leen de lejos: frases cortas, sin
 * tecnicismos. Mismo vocabulario que el cliente: «pedir», «cola», «lista de la casa».
 */
export const TV = {
  cargando: 'Encendiendo la rockola…',
  noCargo: 'La pantalla no pudo cargar. Revisa la conexión del televisor.',
  barNoEncontrado: 'Aquí no hay ninguna rockola',
  nadaSuena: 'No suena nada ahora',
  suenaPedida: 'Suena ahora, pedida por el público',
  suenaCasa: 'Suena ahora, de la lista de la casa',
  listaDeLaCasa: 'Lista de la casa',
  loQueViene: 'Lo que viene',
  desempate: 'Con los mismos votos, va primero la que se pidió antes.',
  yMas: (n: number) => (n === 1 ? 'Y 1 más en la cola.' : `Y ${n} más en la cola.`),
  enPausa: 'Los pedidos están en pausa.',
  colaVacia: {
    titulo: 'La cola está vacía',
    conLista: 'Escanea el QR y pide la primera canción. Mientras tanto suena la lista de la casa.',
    sinLista: 'Escanea el QR y pide la primera canción.',
  },
  cerrado: { titulo: 'Por ahora no recibimos canciones', detalle: 'Los pedidos se abren en el horario del bar. Mientras tanto suena la lista de la casa.' },
  mantenimiento: { titulo: 'La rockola está en mantenimiento', detalle: 'El bar está preparando la próxima noche.' },
  escaneaYVota: 'Escanea y vota',
  sinDescargas: 'Sin descargas ni registro',
  qrEtiqueta: 'Código QR para pedir y votar desde el teléfono',
  encender: {
    titulo: 'Encender la pantalla',
    detalle: 'El navegador necesita un toque para poner la música con sonido. Después la cola avanza sola.',
    boton: 'Encender',
  },
};
