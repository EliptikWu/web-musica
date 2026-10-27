import type { Metadata } from 'next';
import { Comparar } from '@/components/analitica/Comparar';

export const metadata: Metadata = { title: 'Comparar noches' };

export default function Pagina() {
  return <Comparar />;
}
