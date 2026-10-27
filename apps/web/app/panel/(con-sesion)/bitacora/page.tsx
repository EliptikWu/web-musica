import type { Metadata } from 'next';
import { ConPermiso } from '@/components/gestion/PaginaGestion';
import { Bitacora } from '@/components/gestion/Bitacora';

export const metadata: Metadata = { title: 'Bitácora' };

export default function Pagina() {
  return (
    <ConPermiso permiso="bitacora">
      <Bitacora />
    </ConPermiso>
  );
}
