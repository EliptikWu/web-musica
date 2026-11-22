'use client';

import { useEffect } from 'react';
import { useBar } from '@/lib/api/hooks-cliente';
import { quitarTema } from '@/lib/tema';

/**
 * Pone en `<html>` el diseño que eligió el bar (RF-059, ADR-25, D19) y lo recuerda en `clave` para
 * pintarlo antes de la próxima carga. La primera vez, mientras llegan los datos, se ve Rockola.
 */
export function useTemaDelBar(slug: string, clave: string) {
  const tema = useBar(slug).data?.tema;
  // Al salir se quita siempre, también si el script previo lo puso y el bar no llegó a cargar.
  useEffect(() => quitarTema, []);
  useEffect(() => {
    if (!tema) return;
    document.documentElement.dataset.tema = tema;
    try {
      localStorage.setItem(clave, tema);
    } catch {
      // Sin almacenamiento (modo privado): solo se pierde el aviso previo del diseño.
    }
  }, [clave, tema]);
}
