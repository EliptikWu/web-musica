import type { Metadata } from 'next';
import { ConPermiso } from '@/components/gestion/PaginaGestion';
import { ListaEditor } from '@/components/gestion/ListaEditor';

export const metadata: Metadata = { title: 'Lista de la casa' };

export default async function Pagina({ params }: { params: Promise<{ lista: string }> }) {
  const { lista } = await params;
  return (
    <ConPermiso>
      <ListaEditor id={lista} />
    </ConPermiso>
  );
}
