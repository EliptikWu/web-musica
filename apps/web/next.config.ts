import path from 'node:path';
import type { NextConfig } from 'next';

const cabecerasComunes = [
  { key: 'X-Content-Type-Options', value: 'nosniff' },
  { key: 'X-Frame-Options', value: 'DENY' },
  // Los navegadores solo la atienden por https; en local no hace nada.
  { key: 'Strict-Transport-Security', value: 'max-age=63072000; includeSubDomains' },
];

const config: NextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  devIndicators: false,
  transpilePackages: ['@rockola/ui', '@rockola/contrato'],
  // La raíz es el repositorio: los paquetes compartidos viven fuera de esta carpeta.
  turbopack: { root: path.resolve(import.meta.dirname, '..', '..') },
  images: {
    // Miniaturas de YouTube, sin pasar por el optimizador (son pequeñas y ya vienen en jpg).
    unoptimized: true,
    remotePatterns: [{ protocol: 'https', hostname: 'i.ytimg.com' }],
  },
  // La política de contenido va en proxy.ts: lleva un nonce distinto en cada petición.
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          ...cabecerasComunes,
          { key: 'Referrer-Policy', value: 'same-origin' },
          { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=(), autoplay=(self)' },
        ],
      },
      {
        // La pantalla del bar embebe YouTube: sin referente, YouTube rechaza el video (error 153).
        source: '/b/:bar/pantalla',
        headers: [
          { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
          { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=(), autoplay=(self "https://www.youtube-nocookie.com"), encrypted-media=(self "https://www.youtube-nocookie.com"), fullscreen=(self "https://www.youtube-nocookie.com")' },
        ],
      },
    ];
  },
};

export default config;
