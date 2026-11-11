import type { Metadata } from 'next';
import { ConPermiso } from '@/components/gestion/PaginaGestion';
import { Establecimiento } from '@/components/gestion/Establecimiento';

export const metadata: Metadata = { title: 'Bar y QR' };

export default function Pagina() {
  return (
    <ConPermiso>
      <Establecimiento />
    </ConPermiso>
  );
}
