import type { Metadata } from 'next';
import { ResolverQr } from '@/components/cliente/ResolverQr';

export const metadata: Metadata = { title: 'Abriendo la rockola' };

export default async function PaginaQr({ params }: { params: Promise<{ token: string }> }) {
  const { token } = await params;
  return <ResolverQr token={token} />;
}
