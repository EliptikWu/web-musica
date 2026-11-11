import type { Metadata } from 'next';
import { ConPermiso } from '@/components/gestion/PaginaGestion';
import { Horario } from '@/components/gestion/Horario';

export const metadata: Metadata = { title: 'Horario del QR' };

export default function Pagina() {
  return (
    <ConPermiso>
      <Horario />
    </ConPermiso>
  );
}
