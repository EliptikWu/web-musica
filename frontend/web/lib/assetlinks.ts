/** El paquete de la app Android (`applicationId` de `frontend/movil/app/build.gradle.kts`). */
export const PAQUETE_ANDROID = 'com.example.rokola';

const HUELLA_SHA256 = /^[0-9A-F]{2}(?::[0-9A-F]{2}){31}$/;

/**
 * Lo que Android lee para abrir el QR en la app sin preguntar (doc 04, pendiente 11). Las huellas
 * vienen de `ROCKOLA_ANDROID_SHA256`, separadas por coma; una mal escrita se ignora, porque
 * publicarla no serviría de nada. Sin huellas la lista va vacía y Android sigue preguntando.
 */
export function construirAssetlinks(huellasCrudas: string | undefined) {
  const huellas = [
    ...new Set(
      (huellasCrudas ?? '')
        .split(',')
        .map((huella) => huella.trim().toUpperCase())
        .filter((huella) => HUELLA_SHA256.test(huella)),
    ),
  ];
  if (huellas.length === 0) return [];
  return [
    {
      relation: ['delegate_permission/common.handle_all_urls'],
      target: { namespace: 'android_app', package_name: PAQUETE_ANDROID, sha256_cert_fingerprints: huellas },
    },
  ];
}
