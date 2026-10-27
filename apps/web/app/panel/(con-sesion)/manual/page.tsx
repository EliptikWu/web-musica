import type { Metadata } from 'next';
import { ConPermiso } from '@/components/gestion/PaginaGestion';
import { Manual } from '@/components/gestion/Manual';

export const metadata: Metadata = { title: 'Manual de uso' };

export default function Pagina() {
  return (
    <ConPermiso permiso="verOperacion">
      <Manual />
    </ConPermiso>
  );
}
