'use client';

import { useEffect } from 'react';
import './globals.css';

/** Si falla hasta el layout raíz: sin componentes compartidos, solo lo imprescindible. */
export default function ErrorGlobal({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);
  return (
    <html lang="es-CO">
      <body className="madera madera--apagada flex min-h-dvh items-center justify-center p-6">
        <main className="max-w-sm text-center">
          <h1 className="placa text-[25px]">Rockola no cargó</h1>
          <p className="secundario mt-3">Revisa tu conexión y vuelve a intentarlo.</p>
          <button type="button" className="tecla tecla--cereza tecla--grande mt-8" onClick={() => retry()}>
            Reintentar
          </button>
        </main>
      </body>
    </html>
  );
}
