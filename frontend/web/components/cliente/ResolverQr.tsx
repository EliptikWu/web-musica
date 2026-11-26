'use client';

import { Arco, claseTecla, Ventana } from '@rockola/ui';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { AvisoPantalla } from '@/components/comun/AvisoPantalla';
import { pedir } from '@/lib/api/telefono';
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
  // RF-065: el instalador es para Android; en iPhone y en computador sigue la web.
  const apk = consulta.data?.destino === 'app' && /android/i.test(navigator.userAgent) ? consulta.data.apk_url : null;
  useEffect(() => {
    if (slug && !apk) router.replace(`/b/${slug}`);
  }, [apk, router, slug]);

  if (slug && apk) {
    return (
      <main className="madera flex min-h-dvh flex-col items-center px-4 pt-12 pb-8">
        <div className="flex w-full max-w-[26rem] flex-col">
          <Arco className="mt-6" aria-label="Rockola">
            <div className="px-[18px] pt-[104px] pb-[18px]">
              <Ventana className="px-4 pt-4 pb-[18px]">
                <h1 className="condensada text-[34px] leading-[.98]">{qr.app.titulo}</h1>
                <p className="mt-[10px] text-[15px] leading-[1.4] text-tinta-2">{qr.app.detalle}</p>
              </Ventana>
            </div>
          </Arco>
          <a href={apk} rel="noopener" className={claseTecla('cereza', 'grande', 'mt-6 w-full')}>
            {qr.app.descargar}
          </a>
          <Link href={`/b/${slug}`} className={claseTecla('normal', 'grande', 'mt-3 w-full')}>
            {qr.app.web}
          </Link>
        </div>
      </main>
    );
  }

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
