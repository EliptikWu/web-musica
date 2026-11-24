import type { Metadata } from 'next';
import { CatalogoCanciones } from '@/components/gestion/CatalogoCanciones';
import { ConPermiso } from '@/components/gestion/PaginaGestion';

export const metadata: Metadata = { title: 'Canciones' };

export default function Pagina() {
  return (
    <ConPermiso permiso="gestion">
      <CatalogoCanciones />
    </ConPermiso>
  );
}
