import type { z } from 'zod';
import type { CodigoError } from './comunes.ts';
import type { Rol } from './panel.ts';

export type Metodo = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
export type Superficie = 'cliente' | 'pantalla' | 'panel' | 'plataforma';

/**
 * Un endpoint del contrato. Es la única definición: de aquí salen el cliente de la app, los
 * manejadores de la simulación y la documentación para el backend (doc 04).
 */
export type Endpoint = {
  metodo: Metodo;
  /** Ruta relativa a la base de la API, con parámetros entre llaves: `/bares/{slug}/ahora`. */
  ruta: string;
  resumen: string;
  superficie: Superficie;
  rf: readonly string[];
  consulta?: z.ZodObject;
  cuerpo?: z.ZodType;
  respuesta: z.ZodType;
  /** Código HTTP de éxito. */
  estado: 200 | 201 | 204;
  /** Lleva `Idempotency-Key`: la misma clave devuelve la misma respuesta (RNF-008). */
  idempotente?: boolean;
  /** Rol mínimo en el panel. Sin él, la ruta es pública. */
  rol?: Rol;
  /** Pide una pantalla vinculada (cookie `rockola_pantalla`, RF-058). */
  pantalla?: true;
  errores: readonly CodigoError[];
};

export function definir<const E extends Endpoint>(endpoint: E): E {
  return endpoint;
}

/** Arma la ruta concreta: `rutaDe('/bares/{slug}/ahora', { slug: 'demo' })` → `/bares/demo/ahora`. */
export function rutaDe(ruta: string, parametros: Record<string, string>): string {
  return ruta.replace(/\{(\w+)\}/g, (_, nombre: string) => {
    const valor = parametros[nombre];
    if (valor === undefined) throw new Error(`Falta el parámetro «${nombre}» para ${ruta}`);
    return encodeURIComponent(valor);
  });
}
