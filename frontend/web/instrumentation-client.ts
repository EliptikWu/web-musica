/**
 * Corre antes de que React arranque. En modo simulado (doc 03) pone en marcha la simulación
 * de la API; el cliente de la API espera a esta promesa antes de su primera petición. En una
 * compilación real la condición es falsa y la simulación no se incluye.
 */
if (process.env.NEXT_PUBLIC_API_MODE !== 'real') {
  globalThis.__rockolaSimulacion = import('./simulacion/navegador')
    .then((m) => m.arrancar())
    .catch((error: unknown) => {
      console.error('[simulación] no arrancó', error);
      throw error;
    });
}
