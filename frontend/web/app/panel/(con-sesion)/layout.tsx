import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { MarcoPanel } from '@/components/panel/MarcoPanel';
import '@rockola/ui/temas/afiche.css';
import '@rockola/ui/temas/vidrio.css';
import '@rockola/ui/temas/panel-afiche.css';
import '@rockola/ui/temas/panel-vidrio.css';

export const metadata: Metadata = { title: { default: 'Panel', template: '%s · Panel de la rockola' }, robots: { index: false } };

export default function LayoutPanel({ children }: { children: ReactNode }) {
  return <MarcoPanel>{children}</MarcoPanel>;
}
