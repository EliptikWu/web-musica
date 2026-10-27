import type { Metadata } from 'next';
import { Cola } from '@/components/cliente/Cola';

export const metadata: Metadata = { title: 'La cola' };

export default async function PaginaCola({ params }: { params: Promise<{ bar: string }> }) {
  const { bar } = await params;
  return <Cola slug={bar} />;
}
