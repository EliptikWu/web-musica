import type { Metadata } from 'next';
import { ConPermiso } from '@/components/gestion/PaginaGestion';
import { Listas } from '@/components/gestion/Listas';

export const metadata: Metadata = { title: 'Listas de la casa' };

export default function Pagina() {
  return (
    <ConPermiso>
      <Listas />
    </ConPermiso>
  );
}
