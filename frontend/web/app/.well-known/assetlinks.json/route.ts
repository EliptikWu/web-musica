import { construirAssetlinks } from '@/lib/assetlinks';

// La huella se define en el servicio desplegado: no puede quedar fija al compilar.
export const dynamic = 'force-dynamic';

export function GET() {
  return Response.json(construirAssetlinks(process.env.ROCKOLA_ANDROID_SHA256), {
    headers: { 'Cache-Control': 'public, max-age=300' },
  });
}
