import type { Metadata } from 'next';
import { ConPermiso } from '@/components/gestion/PaginaGestion';
import { Eventos } from '@/components/gestion/Eventos';

export const metadata: Metadata = { title: 'Eventos' };

export default function Pagina() {
  return (
    <ConPermiso>
      <Eventos />
    </ConPermiso>
  );
}
