import type { Metadata } from 'next';
import { Participacion } from '@/components/analitica/Participacion';

export const metadata: Metadata = { title: 'Participación' };

export default function Pagina() {
  return <Participacion />;
}
