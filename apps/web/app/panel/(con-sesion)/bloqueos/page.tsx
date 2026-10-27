import type { Metadata } from 'next';
import { ConPermiso } from '@/components/gestion/PaginaGestion';
import { Bloqueos } from '@/components/gestion/Bloqueos';

export const metadata: Metadata = { title: 'Bloqueos' };

export default function Pagina() {
  return (
    <ConPermiso permiso="desbloquear">
      <Bloqueos />
    </ConPermiso>
  );
}
