import type { ReactNode } from 'react';
import { MarcoAnalitica } from '@/components/analitica/Comun';
import { ConPermiso } from '@/components/gestion/PaginaGestion';

/** El tablero analítico es solo del Administrador (RF-049): la puerta va antes de pedir nada. */
export default function Layout({ children }: { children: ReactNode }) {
  return (
    <ConPermiso permiso="analitica">
      <MarcoAnalitica>{children}</MarcoAnalitica>
    </ConPermiso>
  );
}
