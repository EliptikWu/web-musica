import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { MarcoPanel } from '@/components/panel/MarcoPanel';

export const metadata: Metadata = { title: { default: 'Panel', template: '%s · Panel de la rockola' }, robots: { index: false } };

export default function LayoutPanel({ children }: { children: ReactNode }) {
  return <MarcoPanel>{children}</MarcoPanel>;
}
