import type { Metadata } from 'next';
import { ConPermiso } from '@/components/gestion/PaginaGestion';
import { Usuarios } from '@/components/gestion/Usuarios';

export const metadata: Metadata = { title: 'Personal' };

export default function Pagina() {
  return (
    <ConPermiso>
      <Usuarios />
    </ConPermiso>
  );
}
