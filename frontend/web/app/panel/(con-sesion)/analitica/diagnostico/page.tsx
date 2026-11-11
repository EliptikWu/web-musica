import type { Metadata } from 'next';
import { Diagnostico } from '@/components/analitica/Diagnostico';

export const metadata: Metadata = { title: 'Diagnóstico' };

export default function Pagina() {
  return <Diagnostico />;
}
