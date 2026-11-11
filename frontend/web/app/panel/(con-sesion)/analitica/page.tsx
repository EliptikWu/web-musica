import type { Metadata } from 'next';
import { Resumen } from '@/components/analitica/Resumen';

export const metadata: Metadata = { title: 'Tablero analítico' };

export default function Pagina() {
  return <Resumen />;
}
