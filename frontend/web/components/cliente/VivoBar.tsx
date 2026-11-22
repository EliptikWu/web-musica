'use client';

import type { ReactNode } from 'react';
import { useTemaDelBar } from '@/components/comun/useTemaDelBar';
import { useTiempoRealBar } from '@/lib/api/hooks-cliente';
import { claveTema } from '@/lib/tema';

/**
 * Mantiene abierta la conexión en vivo del bar mientras se está en cualquiera de sus pantallas
 * (teléfono y TV) y les pone el diseño que eligió el bar (RF-059, ADR-25).
 */
export function VivoBar({ slug, children }: { slug: string; children: ReactNode }) {
  useTiempoRealBar(slug);
  useTemaDelBar(slug, claveTema(slug));
  return children;
}
