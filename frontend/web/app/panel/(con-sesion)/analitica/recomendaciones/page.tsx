import type { Metadata } from 'next';
import { Recomendaciones } from '@/components/analitica/Recomendaciones';

export const metadata: Metadata = { title: 'Recomendaciones' };

export default function Pagina() {
  return <Recomendaciones />;
}
