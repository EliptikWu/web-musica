import type { Metadata } from 'next';
import { Suspense } from 'react';
import { Entrar } from '@/components/panel/Entrar';

export const metadata: Metadata = { title: 'Entrar al panel', robots: { index: false } };

export default function PaginaEntrar() {
  return (
    <Suspense>
      <Entrar />
    </Suspense>
  );
}
