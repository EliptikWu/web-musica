'use client';

import { type ReactNode, useEffect } from 'react';
import { useBar, useTiempoRealBar } from '@/lib/api/hooks-cliente';
import { claveTema } from '@/lib/tema';

/**
 * Mantiene abierta la conexión en vivo del bar mientras se está en cualquiera de sus pantallas
 * (teléfono y TV) y les pone el diseño que eligió el bar: `data-tema` en `<html>` (RF-059,
 * ADR-25). Lo recuerda para la próxima carga; la primera vez, mientras llegan los datos del
 * bar, se ve Rockola.
 */
export function VivoBar({ slug, children }: { slug: string; children: ReactNode }) {
  useTiempoRealBar(slug);
  const tema = useBar(slug).data?.tema;
  // Al salir del bar se quita siempre, también si el script previo lo puso y el bar no llegó a cargar.
  useEffect(
    () => () => {
      delete document.documentElement.dataset.tema;
    },
    [],
  );
  useEffect(() => {
    if (!tema) return;
    document.documentElement.dataset.tema = tema;
    try {
      localStorage.setItem(claveTema(slug), tema);
    } catch {
      // Sin almacenamiento (modo privado): solo se pierde el aviso previo del diseño.
    }
  }, [slug, tema]);
  return children;
}
