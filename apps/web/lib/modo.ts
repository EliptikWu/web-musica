/**
 * Modo de la API (doc 03): `simulado` mientras no hay backend, `real` para hablar con él.
 * Se fija al compilar con NEXT_PUBLIC_API_MODE. Fuera de aquí, de instrumentation-client.ts
 * y de simulacion/, ningún archivo sabe en qué modo está.
 */
export const MODO: 'simulado' | 'real' = process.env.NEXT_PUBLIC_API_MODE === 'real' ? 'real' : 'simulado';

declare global {
  // La promesa la crea instrumentation-client.ts antes de que React arranque.
  var __rockolaSimulacion: Promise<void> | undefined;
}

/** Espera a que la simulación esté lista para responder. En modo real no espera nada. */
export function esperarSimulacion(): Promise<void> {
  if (MODO === 'real' || typeof window === 'undefined') return Promise.resolve();
  return globalThis.__rockolaSimulacion ?? Promise.resolve();
}
