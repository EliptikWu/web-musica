'use client';

import { useEffect } from 'react';
import { quitarTema } from '@/lib/tema';

/** Para una pantalla que no pertenece a un bar: quita el diseño que haya puesto el script previo. */
export function SinTema() {
  useEffect(quitarTema, []);
  return null;
}
