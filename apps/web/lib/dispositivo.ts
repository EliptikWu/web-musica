import { nuevaClave } from './idempotencia';

/**
 * Identificador seudónimo del teléfono (RF-002, RNF-007): un UUID que se crea en el
 * navegador y se guarda en él. El servidor guarda solo su SHA-256; aquí nunca se muestra.
 * En incógnito o si se borran los datos del sitio, el teléfono cuenta como nuevo (doc 01).
 */
const CLAVE = 'rockola.dispositivo';
let enMemoria: string | null = null;

export function idDispositivo(): string {
  if (enMemoria) return enMemoria;
  try {
    const guardado = localStorage.getItem(CLAVE);
    if (guardado) return (enMemoria = guardado);
    const nuevo = nuevaClave();
    localStorage.setItem(CLAVE, nuevo);
    return (enMemoria = nuevo);
  } catch {
    // Sin almacenamiento (algunos modos privados): vale para esta visita.
    return (enMemoria = nuevaClave());
  }
}

/** Tipo según el ancho de la pantalla, no el modelo (diccionario de datos). */
export function tipoDispositivo(ancho: number): 'movil' | 'tableta' | 'otro' {
  if (ancho < 600) return 'movil';
  if (ancho < 1024) return 'tableta';
  return 'otro';
}
