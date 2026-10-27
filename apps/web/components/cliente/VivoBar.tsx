'use client';

import type { ReactNode } from 'react';
import { useTiempoRealBar } from '@/lib/api/hooks-cliente';

/** Mantiene abierta la conexión en vivo del bar mientras se está en cualquiera de sus pantallas. */
export function VivoBar({ slug, children }: { slug: string; children: ReactNode }) {
  useTiempoRealBar(slug);
  return children;
}
