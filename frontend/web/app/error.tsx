'use client';

import { Tecla } from '@rockola/ui';
import { useEffect } from 'react';
import { AvisoPantalla } from '@/components/comun/AvisoPantalla';

export default function ErrorDePagina({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);
  return (
    <AvisoPantalla
      titulo="Esta pantalla no cargó"
      accion={
        <Tecla variante="cereza" tamano="grande" onClick={() => retry()}>
          Reintentar
        </Tecla>
      }
    >
      <p>Revisa tu conexión y vuelve a intentarlo. La música del bar sigue sonando.</p>
    </AvisoPantalla>
  );
}
