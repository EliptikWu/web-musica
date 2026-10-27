import { notFound } from 'next/navigation';
import type { ReactNode } from 'react';
import { VivoBar } from '@/components/cliente/VivoBar';

const SLUG = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

export default async function LayoutBar({ children, params }: { children: ReactNode; params: Promise<{ bar: string }> }) {
  const { bar } = await params;
  if (!SLUG.test(bar)) notFound();
  return <VivoBar slug={bar}>{children}</VivoBar>;
}
