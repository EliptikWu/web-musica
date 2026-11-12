import type { Metadata, Viewport } from 'next';
import { Archivo, League_Gothic, Libre_Franklin, Outfit } from 'next/font/google';
import { headers } from 'next/headers';
import { connection } from 'next/server';
import type { ReactNode } from 'react';
import { Providers } from '@/components/comun/Providers';
import { SCRIPT_TEMA_PREVIO } from '@/lib/tema';
import './globals.css';

// Archivo variable con su eje de ancho: placas al 125 %, canciones al 75 % (ADR-08).
const archivo = Archivo({ subsets: ['latin', 'latin-ext'], axes: ['wdth'], variable: '--fuente-archivo', display: 'swap' });
// Fuentes del diseño Afiche (RF-059, ADR-25): sin precarga, solo las baja el bar que lo usa.
const leagueGothic = League_Gothic({ subsets: ['latin', 'latin-ext'], variable: '--fuente-cartel', display: 'swap', preload: false });
const libreFranklin = Libre_Franklin({ subsets: ['latin', 'latin-ext'], variable: '--fuente-franklin', display: 'swap', preload: false });
// Fuente del diseño Vidrio, igual: sin precarga.
const outfit = Outfit({ subsets: ['latin', 'latin-ext'], variable: '--fuente-outfit', display: 'swap', preload: false });

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
  const nonce = (await headers()).get('x-nonce') ?? undefined;
  return (
    // suppressHydrationWarning: el script previo (ADR-26) pone `data-tema` en <html> antes de hidratar.
    <html lang="es-CO" className={`${archivo.variable} ${leagueGothic.variable} ${libreFranklin.variable} ${outfit.variable}`} suppressHydrationWarning>
      <head>
        {/* ADR-26: el diseño del bar antes de pintar. Con el nonce de la CSP; no lee más que la URL y un tema guardado. */}
        <script nonce={nonce} dangerouslySetInnerHTML={{ __html: SCRIPT_TEMA_PREVIO }} />
      </head>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
