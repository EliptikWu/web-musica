import type { Metadata } from 'next';
import { Politica } from '@/components/cliente/Politica';

export const metadata: Metadata = { title: 'Política de datos' };

export default async function PaginaPolitica({ params }: { params: Promise<{ bar: string }> }) {
  const { bar } = await params;
  return <Politica slug={bar} />;
}
