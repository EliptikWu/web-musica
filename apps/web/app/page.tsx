import { claseTecla, LuzEstado, TarjetaTitulo } from '@rockola/ui';
import type { Metadata } from 'next';
import Link from 'next/link';
import { AvisoPantalla } from '@/components/comun/AvisoPantalla';
import { MODO } from '@/lib/modo';
import { ESCENARIOS } from '@/simulacion/escenarios';

export const metadata: Metadata = { title: 'Rockola' };

const SLUG = 'demo';

const SUPERFICIES = [
  { href: `/b/${SLUG}`, titulo: 'Cliente', detalle: 'Lo que abre el teléfono al escanear el QR', boton: 'Abrir' },
  { href: `/b/${SLUG}/pantalla`, titulo: 'Pantalla del bar', detalle: 'La TV: lo que suena, lo que viene y el QR', boton: 'Abrir' },
  { href: '/panel', titulo: 'Panel', detalle: 'admin@rockola.demo u operador@rockola.demo, clave rockola-demo', boton: 'Entrar' },
];


/**
 * Portada. En modo simulado es el tablero para abrir las tres superficies y cambiar de
 * escenario; con la API real, quien llega aquí sin QR solo necesita saber que lo escanee.
 */
export default function Portada() {
  if (MODO === 'real') {
    return (
      <AvisoPantalla titulo="Escanea el QR de tu mesa">
        <p>Rockola se abre desde el código QR del bar. Sin descargas ni registro.</p>
      </AvisoPantalla>
    );
  }
  return (
    <main className="madera px-4 pt-[22px] pb-12">
      <div className="mx-auto max-w-[34rem]">
        <header className="text-center">
          <h1 className="placa text-[25px]">Rockola</h1>
          <LuzEstado encendida className="mt-2">
            Simulación en este navegador
          </LuzEstado>
          <p className="secundario mx-auto mt-4 max-w-[30rem] text-[15px]">
            Todavía no hay backend: las tres pantallas corren contra una simulación de la API. Ábrelas en pestañas de este
            navegador y verás cómo un voto llega a la TV y al panel.
          </p>
        </header>

        <section aria-labelledby="t-abrir" className="mt-8">
          <h2 id="t-abrir" className="titulo-seccion mb-3">
            Abrir una pantalla
          </h2>
          <ul className="filas">
            {SUPERFICIES.map((s) => (
              <li key={s.href} className="fila">
                <TarjetaTitulo titulo={s.titulo} artista={s.detalle} />
                <Link href={s.href} className={claseTecla('cereza')} aria-label={`${s.boton}: ${s.titulo}`}>
                  {s.boton}
                </Link>
              </li>
            ))}
          </ul>
        </section>

        <section aria-labelledby="t-escenarios" className="mt-9">
          <h2 id="t-escenarios" className="titulo-seccion">
            Cambiar de escenario
          </h2>
          <p className="secundario mt-1 mb-3 text-[14px]">
            Rehace la noche de ejemplo en todas las pestañas. Los votos, horas y métricas son datos de ejemplo.
          </p>
          <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {ESCENARIOS.map((e) => (
              <li key={e.id}>
                {/* Enlace normal, no <Link>: el escenario se prepara al cargar el documento. */}
                <a href={`/b/${SLUG}?escenario=${e.id}&reiniciar`} className={claseTecla('normal', 'chica', 'w-full justify-start! normal-case! tracking-normal!')}>
                  {e.nombre}
                </a>
              </li>
            ))}
          </ul>
        </section>

        <p className="secundario mt-9 text-center text-[14px]">
          <Link href="/muestras" className="font-semibold text-crema underline underline-offset-4">
            Ver los componentes del sistema de diseño
          </Link>
        </p>
      </div>
    </main>
  );
}
