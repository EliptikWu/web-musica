import type { Metadata } from 'next';
import { ConPermiso } from '@/components/gestion/PaginaGestion';
import { QrParaImprimir } from '@/components/gestion/Establecimiento';

export const metadata: Metadata = { title: 'Imprimir el QR' };

export default function Pagina() {
  return (
    <ConPermiso>
      <QrParaImprimir />
    </ConPermiso>
  );
}
