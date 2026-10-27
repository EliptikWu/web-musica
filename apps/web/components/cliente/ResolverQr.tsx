'use client';

import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { AvisoPantalla } from '@/components/comun/AvisoPantalla';
import { pedir } from '@/lib/api/cliente';
import { codigoDe } from '@/lib/api/errores';
import { qr } from '@/lib/textos/cliente';
import { BarNoEncontrado } from './Estados';

/**
 * Entrada por QR (RF-001, RF-035): el QR lleva un token, no el bar, para poder invalidarlo al
 * regenerarlo. Se resuelve y se reemplaza la dirección por la del bar.
 */
export function ResolverQr({ token }: { token: string }) {
  const router = useRouter();
  const consulta = useQuery({ queryKey: ['qr', token], queryFn: ({ signal }) => pedir('resolverQr', { parametros: { token }, signal }), retry: 1 });
  const slug = consulta.data?.slug;
  useEffect(() => {
    if (slug) router.replace(`/b/${slug}`);
  }, [router, slug]);

  const codigo = codigoDe(consulta.error);
  if (codigo === 'qr_reemplazado') {
    return (
      <AvisoPantalla titulo={qr.reemplazado.titulo}>
        <p>{qr.reemplazado.detalle}</p>
      </AvisoPantalla>
    );
  }
  if (codigo === 'no_encontrado') return <BarNoEncontrado />;
  return (
    <main className="madera flex min-h-dvh items-center justify-center p-6">
      <p className="secundario text-center text-[15px]" role="status">
        {consulta.isError ? 'No se pudo abrir la rockola. Revisa tu conexión y vuelve a escanear el QR.' : qr.abriendo}
      </p>
    </main>
  );
}
