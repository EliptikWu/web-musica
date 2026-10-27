import type { Metadata } from 'next';
import { Bienvenida } from '@/components/cliente/Bienvenida';

export const metadata: Metadata = { title: 'Bienvenida' };

export default async function PaginaBienvenida({ params }: { params: Promise<{ bar: string }> }) {
  const { bar } = await params;
  return <Bienvenida slug={bar} />;
}
