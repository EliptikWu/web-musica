import type { Metadata, Viewport } from 'next';
import { Archivo } from 'next/font/google';
import { connection } from 'next/server';
import type { ReactNode } from 'react';
import { Providers } from '@/components/comun/Providers';
import './globals.css';

// Archivo variable con su eje de ancho: placas al 125 %, canciones al 75 % (ADR-08).
const archivo = Archivo({ subsets: ['latin', 'latin-ext'], axes: ['wdth'], variable: '--fuente-archivo', display: 'swap' });

export const metadata: Metadata = {
  title: { default: 'Rockola', template: '%s · Rockola' },
  description: 'Pide y vota la música del bar desde tu teléfono, sin descargar nada.',
  applicationName: 'Rockola',
};

export const viewport: Viewport = {
  themeColor: '#2A1711',
  colorScheme: 'dark',
  width: 'device-width',
  initialScale: 1,
  viewportFit: 'cover',
  interactiveWidget: 'resizes-content',
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  // Cada página se genera por petición: la CSP lleva un nonce nuevo cada vez y Next solo
  // puede ponerlo en sus scripts si la página no es estática (doc 03).
  await connection();
  return (
    <html lang="es-CO" className={archivo.variable}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
