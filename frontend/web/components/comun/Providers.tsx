'use client';

import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { type ReactNode, useState } from 'react';
import { ErrorApiCliente } from '@/lib/api/errores';
import { salida } from '@/lib/sesion-panel';

/** Un error 4xx no se arregla reintentando: se muestra. La red y el 5xx sí se reintentan (doc 03). */
function reintentar(fallos: number, error: unknown): boolean {
  if (error instanceof ErrorApiCliente && error.estado < 500) return false;
  return fallos < 2;
}

/**
 * Si el panel se queda sin sesión a mitad de la noche (o debe cambiar la clave), cualquier
 * consulta o acción que lo note lleva a la puerta: una sola regla para todo el panel, no un
 * caso en cada pantalla. Es una carga completa: se descarta todo lo guardado en memoria.
 */
function alErrorDeSesion(error: unknown) {
  if (typeof window === 'undefined' || !(error instanceof ErrorApiCliente)) return;
  const ruta = window.location.pathname;
  if (!ruta.startsWith('/panel') || ruta.startsWith('/panel/entrar') || salida.enCurso) return;
  // Carga completa a propósito: sin sesión no debe quedar nada del panel en memoria.
  // eslint-disable-next-line @next/next/no-location-assign-relative-destination
  if (error.codigo === 'no_autenticado') window.location.assign('/panel/entrar?motivo=caducada');
  // eslint-disable-next-line @next/next/no-location-assign-relative-destination
  else if (error.codigo === 'debe_cambiar_clave' && !ruta.startsWith('/panel/cambiar-clave')) window.location.assign('/panel/cambiar-clave');
}

export function Providers({ children }: { children: ReactNode }) {
  const [cliente] = useState(
    () =>
      new QueryClient({
        queryCache: new QueryCache({ onError: alErrorDeSesion }),
        mutationCache: new MutationCache({ onError: alErrorDeSesion }),
        defaultOptions: {
          queries: { retry: reintentar, staleTime: 5_000, refetchOnWindowFocus: true },
          mutations: { retry: false },
        },
      }),
  );
  return <QueryClientProvider client={cliente}>{children}</QueryClientProvider>;
}
