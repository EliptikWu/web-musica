import type { Metadata } from 'next';
import { CambiarClave } from '@/components/panel/CambiarClave';

export const metadata: Metadata = { title: 'Cambia tu clave', robots: { index: false } };

export default function PaginaCambiarClave() {
  return <CambiarClave />;
}
