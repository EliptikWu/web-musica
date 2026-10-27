'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { type ReactNode, useState } from 'react';
import { ErrorApiCliente } from '@/lib/api/errores';

/** Un error 4xx no se arregla reintentando: se muestra. La red y el 5xx sí se reintentan (doc 03). */
function reintentar(fallos: number, error: unknown): boolean {
  if (error instanceof ErrorApiCliente && error.estado < 500) return false;
  return fallos < 2;
}

export function Providers({ children }: { children: ReactNode }) {
  const [cliente] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { retry: reintentar, staleTime: 5_000, refetchOnWindowFocus: true },
          mutations: { retry: false },
        },
      }),
  );
  return <QueryClientProvider client={cliente}>{children}</QueryClientProvider>;
}
