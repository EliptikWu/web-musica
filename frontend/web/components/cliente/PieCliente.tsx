'use client';

import { Icono, MiniAvance, type NombreIcono } from '@rockola/ui';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAhora } from '@/lib/api/hooks-cliente';
import { posicionActual } from '@/lib/reglas/espera';
import { useAhoraServidor } from '@/lib/reloj-servidor';
import { T } from '@/lib/textos/cliente';

/**
 * Pie del teléfono: lo que suena (sin play ni pausa: la música la controla el bar) y las
 * pestañas Inicio, Buscar y Cola, como en los prototipos 2 y 3.
 */
export function PieCliente({ slug }: { slug: string }) {
  const { data: ahora } = useAhora(slug);
  const instante = useAhoraServidor();
  const rep = ahora?.reproduccion ?? null;
  const posicion = rep && ahora ? posicionActual(rep, ahora.servidor_ahora, instante || Date.parse(ahora.servidor_ahora)) : 0;
  const ruta = usePathname();
  const base = `/b/${slug}`;
  const pestanas: { href: string; texto: string; icono: NombreIcono }[] = [
    { href: base, texto: 'Inicio', icono: 'inicio' },
    { href: `${base}/buscar`, texto: 'Buscar', icono: 'buscar' },
    { href: `${base}/cola`, texto: 'Cola', icono: 'cola' },
  ];
  return (
    <footer className="pie">
      <section className="mini" aria-label={T.suenaAhora}>
        <span className={rep ? 'mini__luz' : 'mini__luz mini__luz--apagada'} aria-hidden="true" />
        <div className="mini__texto">
          <p className="mini__etiqueta">{rep ? T.suenaAhora : T.nadaSuena}</p>
          {rep ? (
            <p className="mini__titulo condensada">
              {rep.cancion.titulo} <span>{rep.cancion.artista}</span>
            </p>
          ) : null}
        </div>
        {rep && rep.votos !== null ? (
          <p className="mini__votos">
            <b>{rep.votos}</b>
            {rep.votos === 1 ? 'voto' : 'votos'}
          </p>
        ) : rep ? (
          <p className="mini__votos max-w-[5.5rem] leading-tight">{T.listaDeLaCasa}</p>
        ) : null}
      </section>
      <MiniAvance fraccion={rep ? posicion / Math.max(1, rep.cancion.duracion_seg) : 0} />
      <nav className="pestanas" aria-label="Secciones">
        {pestanas.map((p) => (
          <Link key={p.href} href={p.href} className="pestana" aria-current={ruta === p.href ? 'page' : undefined}>
            <Icono nombre={p.icono} />
            {p.texto}
          </Link>
        ))}
      </nav>
    </footer>
  );
}
