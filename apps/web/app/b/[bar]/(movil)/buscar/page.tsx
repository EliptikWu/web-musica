import type { Metadata } from 'next';
import { Buscar } from '@/components/cliente/Buscar';

export const metadata: Metadata = { title: 'Pide una canción' };

export default async function PaginaBuscar({ params }: { params: Promise<{ bar: string }> }) {
  const { bar } = await params;
  return <Buscar slug={bar} />;
}
