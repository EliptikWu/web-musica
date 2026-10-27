import type { Metadata } from 'next';
import { PantallaBar } from '@/components/pantalla/PantallaBar';

export const metadata: Metadata = { title: 'Pantalla del bar' };

export default async function PaginaPantalla({ params }: { params: Promise<{ bar: string }> }) {
  const { bar } = await params;
  return <PantallaBar slug={bar} />;
}
