import type { Metadata } from 'next';
import { Plataforma } from '@/components/plataforma/Plataforma';

export const metadata: Metadata = { title: 'Plataforma', robots: { index: false, follow: false } };

export default function PaginaPlataforma() {
  return <Plataforma />;
}
