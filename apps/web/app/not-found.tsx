import Link from 'next/link';
import { claseTecla } from '@rockola/ui';
import { AvisoPantalla } from '@/components/comun/AvisoPantalla';

export default function NoEncontrada() {
  return (
    <AvisoPantalla
      titulo="Aquí no hay ninguna rockola"
      accion={
        <Link href="/" className={claseTecla('cereza', 'grande')}>
          Ir al inicio
        </Link>
      }
    >
      <p>La dirección no existe o el QR ya no está en uso. Escanea el QR que está en tu mesa.</p>
    </AvisoPantalla>
  );
}
