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
  vincular: {
    titulo: 'Vincula esta pantalla',
    luz: 'Sin vincular',
    etiquetaCodigo: 'Código de esta pantalla',
    porQue: 'Solo una pantalla que el bar vinculó pone la música y mueve la cola.',
    pasos: [
      'En el panel de Rockola, entra a «Bar y QR».',
      'En «Pantallas del bar», escribe este código y ponle un nombre, como «TV de la barra».',
      'La rockola aparece aquí sola, en unos segundos.',
    ],
    vence: (minutos: number) => (minutos < 1 ? 'El código cambia en menos de 1 min. Si vence, aquí sale uno nuevo.' : `El código cambia en ${minutos} min. Si vence, aquí sale uno nuevo.`),
    pidiendo: 'Pidiendo un código…',
    noPidio: 'No se pudo pedir el código. Revisa la conexión del televisor; la pantalla lo vuelve a intentar sola.',
    desvinculada: 'Esta pantalla se desvinculó desde el panel. Para que vuelva a poner la música, vincúlala otra vez.',
  },
  encender: {
    titulo: 'Encender la pantalla',
    detalle: 'El navegador necesita un toque para poner la música con sonido. Después la cola avanza sola.',
    boton: 'Encender',
  },
};
