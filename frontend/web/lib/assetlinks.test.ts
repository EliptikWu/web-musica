import { describe, expect, it } from 'vitest';
import { construirAssetlinks, PAQUETE_ANDROID } from './assetlinks';

const HUELLA = Array.from({ length: 32 }, () => 'AB').join(':');
const OTRA = Array.from({ length: 32 }, () => '0F').join(':');

describe('construirAssetlinks', () => {
  it('sin huellas publica una lista vacía', () => {
    expect(construirAssetlinks(undefined)).toEqual([]);
    expect(construirAssetlinks('  ')).toEqual([]);
  });

  it('declara el paquete y la huella de la firma', () => {
    expect(construirAssetlinks(HUELLA)).toEqual([
      {
        relation: ['delegate_permission/common.handle_all_urls'],
        target: { namespace: 'android_app', package_name: PAQUETE_ANDROID, sha256_cert_fingerprints: [HUELLA] },
      },
    ]);
  });

  it('acepta varias, las pasa a mayúsculas y quita las repetidas', () => {
    const [enlace] = construirAssetlinks(`${HUELLA.toLowerCase()}, ${OTRA} ,${HUELLA}`);
    expect(enlace?.target.sha256_cert_fingerprints).toEqual([HUELLA, OTRA]);
  });

  it('ignora lo que no es una huella SHA-256', () => {
    expect(construirAssetlinks('AB:CD')).toEqual([]);
    expect(construirAssetlinks(HUELLA.replaceAll(':', ''))).toEqual([]);
    expect(construirAssetlinks(`${HUELLA}:AB`)).toEqual([]);
  });
});
