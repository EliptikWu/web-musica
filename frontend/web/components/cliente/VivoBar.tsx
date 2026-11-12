'use client';

import { type ReactNode, useEffect } from 'react';
import { useBar, useTiempoRealBar } from '@/lib/api/hooks-cliente';

/**
 * Mantiene abierta la conexión en vivo del bar mientras se está en cualquiera de sus pantallas
 * (teléfono y TV) y les pone el diseño que eligió el bar: `data-tema` en `<html>` (RF-059,
 * ADR-25). Mientras llegan los datos del bar se ve Rockola, el diseño por defecto.
 */
export function VivoBar({ slug, children }: { slug: string; children: ReactNode }) {
  useTiempoRealBar(slug);
  const tema = useBar(slug).data?.tema;
  useEffect(() => {
    if (!tema) return;
    const raiz = document.documentElement;
    raiz.dataset.tema = tema;
    return () => {
      delete raiz.dataset.tema;
    };
  }, [tema]);
  return children;
}
