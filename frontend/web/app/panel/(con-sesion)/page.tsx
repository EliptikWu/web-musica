import type { Metadata } from 'next';
import { Operacion } from '@/components/panel/Operacion';

export const metadata: Metadata = { title: 'Operación' };

export default function PaginaOperacion() {
  return <Operacion />;
}
