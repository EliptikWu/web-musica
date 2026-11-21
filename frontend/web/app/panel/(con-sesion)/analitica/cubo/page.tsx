import type { Metadata } from 'next';
import { CuboKpi } from '@/components/analitica/CuboKpi';

export const metadata: Metadata = { title: 'Cubo de KPI' };

export default function Pagina() {
  return <CuboKpi />;
}
