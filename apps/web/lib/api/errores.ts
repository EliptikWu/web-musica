import type { CodigoError } from '@rockola/contrato';

/** La API respondió con un error del contrato (doc 04, «Códigos de error»). */
export class ErrorApiCliente extends Error {
  constructor(
    readonly estado: number,
    readonly codigo: CodigoError,
    mensaje: string,
    readonly detalle?: Record<string, unknown>,
  ) {
    super(mensaje);
    this.name = 'ErrorApiCliente';
  }
}

/** No hubo respuesta: sin conexión, tiempo agotado o la petición se canceló. */
export class ErrorRed extends Error {
  constructor(readonly causa: unknown, readonly agotado = false) {
    super(agotado ? 'La API tardó demasiado en responder' : 'No hubo conexión con la API');
    this.name = 'ErrorRed';
  }
}

/** La API respondió algo que no cumple el contrato: mejor fallar que pintar datos rotos. */
export class ErrorContrato extends Error {
  constructor(readonly operacion: string, readonly problemas: unknown) {
    super(`La respuesta de «${operacion}» no cumple el contrato`);
    this.name = 'ErrorContrato';
  }
}

export function codigoDe(error: unknown): CodigoError | 'red' | 'contrato' | 'desconocido' {
  if (error instanceof ErrorApiCliente) return error.codigo;
  if (error instanceof ErrorRed) return 'red';
  if (error instanceof ErrorContrato) return 'contrato';
  return 'desconocido';
}
