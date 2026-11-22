'use client';

import type { Rol } from '@rockola/contrato';
import { claseTecla, Selector } from '@rockola/ui';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useEffect, useRef } from 'react';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { PaginaGestion } from './PaginaGestion';

type Seccion = { id: string; titulo: string; para: Rol; pasos: string[]; imagen?: { archivo: string; texto: string } };

/**
 * El manual del panel (RNF-016, PB-42): pasos cortos con la captura de la pantalla de verdad.
 * Las capturas las genera `npm run capturas -w @rockola/web` (e2e/capturas.spec.ts) en
 * `public/manual/`, así se actualizan cuando cambia la interfaz. El Operador ve su parte.
 */
const SECCIONES: Seccion[] = [
  {
    id: 'entrar',
    titulo: 'Entrar al panel',
    para: 'operador',
    pasos: [
      'Abre el panel en el computador o el teléfono del bar y escribe tu correo y tu clave.',
      'La primera vez, o si el administrador restableció tu clave, el panel te pide elegir una nueva de al menos 10 caracteres.',
      'Si te equivocas cinco veces, espera unos minutos antes de volver a intentar.',
    ],
    imagen: { archivo: 'entrar.jpg', texto: 'La pantalla para entrar al panel.' },
  },
  {
    id: 'noche',
    titulo: 'Seguir la noche',
    para: 'operador',
    pasos: [
      'En «Operación», la franja de arriba dice si se reciben canciones: encendida, abierto; apagada, en pausa o cerrado.',
      'Debajo está lo que suena, con quién lo pidió y cuánto le falta, y la cola ordenada por votos.',
      'El registro de la noche muestra qué hizo el personal y a qué hora.',
    ],
    imagen: { archivo: 'operacion.jpg', texto: 'La operación de la noche.' },
  },
  {
    id: 'musica',
    titulo: 'Saltar o pausar la música',
    para: 'operador',
    pasos: [
      '«Saltar» corta la canción que suena y pasa a la siguiente de la cola (o a la lista de la casa).',
      '«Pausar música» detiene la pantalla del bar; «Reanudar música» la sigue desde donde iba.',
      'Todo queda en el registro con tu nombre.',
    ],
  },
  {
    id: 'pausa',
    titulo: 'Pausar las solicitudes',
    para: 'operador',
    pasos: [
      'Pulsa «Pausar solicitudes» en la franja de arriba. La franja se apaga.',
      'La cola sigue sonando, pero nadie puede pedir ni votar. Los teléfonos dicen que los pedidos están en pausa.',
      'Pulsa «Reanudar solicitudes» para volver a abrir.',
    ],
    imagen: { archivo: 'pausa.jpg', texto: 'La franja apagada: solicitudes en pausa.' },
  },
  {
    id: 'retirar',
    titulo: 'Retirar una solicitud',
    para: 'operador',
    pasos: [
      'En la cola, pulsa «Retirar» en la canción.',
      'Elige el motivo: inapropiada, duplicada u otro, y si quieres añade un detalle.',
      'Quien la pidió ve en su teléfono que el bar la retiró y por qué.',
    ],
    imagen: { archivo: 'retirar.jpg', texto: 'El diálogo para retirar una solicitud.' },
  },
  {
    id: 'bloquear',
    titulo: 'Bloquear una canción',
    para: 'operador',
    pasos: [
      'Pulsa «Bloquear» en lo que suena o en una canción de la cola.',
      'Escribe el motivo: lo verá el resto del personal.',
      'El Operador bloquea hasta el fin de la noche; el Administrador también puede bloquear por horas o hasta levantarlo.',
      'Si estaba sonando, se corta; si estaba en la cola, sale de ella. Nadie la podrá pedir mientras dure el bloqueo.',
    ],
    imagen: { archivo: 'bloquear.jpg', texto: 'El diálogo para bloquear una canción.' },
  },
  {
    id: 'horario',
    titulo: 'Programar el horario del QR',
    para: 'administrador',
    pasos: [
      'En «Horario», marca los días que abre el bar y la hora en que abre y cierra.',
      'Si cierra después de medianoche (por ejemplo, de 8:00 p. m. a 2:00 a. m.), el tramo cuenta para el día en que empieza.',
      'Pulsa «Guardar horario». Las solicitudes se abren y se cierran solas.',
    ],
    imagen: { archivo: 'horario.jpg', texto: 'El horario semanal.' },
  },
  {
    id: 'listas',
    titulo: 'Armar las listas de la casa',
    para: 'administrador',
    pasos: [
      'En «Listas de la casa», crea una lista con su nombre.',
      'Ábrela con «Editar», busca canciones y pulsa «Agregar». Ordénalas con las flechas.',
      'Vuelve a las listas y pulsa «Activar»: elige si suena en orden o al azar. Suena cuando la cola se vacía.',
    ],
    imagen: { archivo: 'listas.jpg', texto: 'El editor de una lista de la casa.' },
  },
  {
    id: 'bloqueos',
    titulo: 'Revisar los bloqueos',
    para: 'administrador',
    pasos: [
      'En «Bloqueos» están los vigentes y los vencidos, con quién los puso y por qué.',
      '«Desbloquear» la deja pedir otra vez; «Duración» cambia hasta cuándo dura.',
    ],
    imagen: { archivo: 'bloqueos.jpg', texto: 'Los bloqueos vigentes.' },
  },
  {
    id: 'personal',
    titulo: 'Dar acceso al personal',
    para: 'administrador',
    pasos: [
      'En «Personal», pulsa «Agregar a alguien» y escribe su nombre, su correo y su rol.',
      'El panel muestra una clave temporal una sola vez: dásela en persona. Al entrar tendrá que cambiarla.',
      '«Clave» la restablece; «Desactivar» le quita el acceso sin borrar su historial.',
    ],
    imagen: { archivo: 'personal.jpg', texto: 'El personal del bar.' },
  },
  {
    id: 'qr',
    titulo: 'Imprimir o cambiar el QR',
    para: 'administrador',
    pasos: [
      'En «Bar y QR», «Imprimir» abre una hoja lista para las mesas; «Descargar» guarda el código.',
      '«Regenerar» crea un QR nuevo: el que está en las mesas deja de funcionar al instante.',
      'Ahí mismo se cambian el nombre del bar y las reglas de los pedidos.',
    ],
    imagen: { archivo: 'bar-qr.jpg', texto: 'Los datos del bar y su QR.' },
  },
  {
    id: 'pantallas',
    titulo: 'Vincular la pantalla del bar',
    para: 'administrador',
    pasos: [
      'En el navegador de la TV, abre la dirección de la pantalla, que está en «Bar y QR», «Vincular una pantalla». La TV muestra un código de 6 letras y números.',
      'En «Bar y QR», «Vincular una pantalla», escribe ese código y un nombre, como «TV de la barra». En unos segundos la TV muestra la rockola: pulsa «Encender» en la TV.',
      'Sin una pantalla vinculada la música no avanza. «Desvincular» le quita el permiso a una TV que ya no se usa.',
    ],
    imagen: { archivo: 'pantalla-codigo.jpg', texto: 'La TV del bar mostrando su código.' },
  },
  {
    id: 'bitacora',
    titulo: 'Consultar la bitácora',
    para: 'administrador',
    pasos: ['En «Bitácora» está todo lo que hizo el personal y el sistema, también los errores de YouTube.', 'Filtra por fechas, por persona o por tipo de acción. No se puede editar.'],
    imagen: { archivo: 'bitacora.jpg', texto: 'La bitácora con sus filtros.' },
  },
  {
    id: 'eventos',
    titulo: 'Anotar los eventos del bar',
    para: 'administrador',
    pasos: ['En «Eventos», agrega karaokes, partidos o noches temáticas con su fecha y hora.', 'El tablero analítico compara esas noches con las normales.'],
    imagen: { archivo: 'eventos.jpg', texto: 'El calendario de eventos.' },
  },
  {
    id: 'tablero',
    titulo: 'Leer el tablero analítico',
    para: 'administrador',
    pasos: [
      'En «Tablero analítico» elige las noches arriba: todas, las últimas cuatro semanas, una noche o unas fechas. Cada noche entra al tablero cuando se cierra.',
      '«Resumen» dice cuánto se pidió, cuánto se esperó y cuánto sonó lo pedido frente a la lista de la casa. «Participación» muestra los géneros y las horas con más movimiento.',
      '«Diagnóstico» señala las canciones muy votadas que no sonaron, si pocos teléfonos deciden la cola y las noches flojas; los umbrales se cambian ahí mismo.',
      '«Recomendaciones» sugiere cambios con el dato en que se basa y te lleva a la sección donde decides. Nada se aplica solo.',
    ],
    imagen: { archivo: 'tablero.jpg', texto: 'El resumen del tablero analítico.' },
  },
  {
    id: 'mantenimiento',
    titulo: 'Cerrar la noche o ponerla en mantenimiento',
    para: 'administrador',
    pasos: [
      '«Cerrar la noche» cierra las solicitudes hasta la próxima franja del horario; lo que quedaba en la cola se descarta.',
      '«Modo mantenimiento» cierra la noche y nadie puede pedir hasta que pulses «Reanudar solicitudes». El historial se guarda.',
    ],
  },
];

/** El tema abierto vive en `?tema=` de la URL: se puede enlazar y el botón Atrás del navegador sirve. */
const rutaDe = (id: string) => `/panel/manual?tema=${id}`;

function IndiceGrupo({ titulo, temas, actual }: { titulo: string; temas: Seccion[]; actual: string }) {
  if (temas.length === 0) return null;
  return (
    <>
      <h2>{titulo}</h2>
      <ul>
        {temas.map((s) => (
          <li key={s.id}>
            <Link href={rutaDe(s.id)} prefetch={false} scroll={false} aria-current={s.id === actual ? 'page' : undefined}>
              {s.titulo}
            </Link>
          </li>
        ))}
      </ul>
    </>
  );
}

export function Manual() {
  const yo = useYoDelPanel();
  const esAdmin = yo.usuario.rol === 'administrador';
  const secciones = SECCIONES.filter((s) => esAdmin || s.para === 'operador');
  const delOperador = secciones.filter((s) => s.para === 'operador');
  const delAdmin = secciones.filter((s) => s.para === 'administrador');
  const router = useRouter();
  const pedido = useSearchParams().get('tema');
  // Un tema desconocido, o que este rol no ve, abre el primero.
  const posicion = Math.max(0, secciones.findIndex((s) => s.id === pedido));
  const tema = secciones[posicion]!;
  const anterior = secciones[posicion - 1];
  const siguiente = secciones[posicion + 1];

  const titulo = useRef<HTMLHeadingElement>(null);
  const temaPrevio = useRef(tema.id);
  const desdeSelector = useRef(false);
  // Al cambiar de tema se sube a su título y el foco va con él, salvo si se eligió en la lista
  // desplegable: ahí foco y vista se quedan para poder seguir recorriéndola con las flechas.
  useEffect(() => {
    if (temaPrevio.current === tema.id) return;
    temaPrevio.current = tema.id;
    if (!desdeSelector.current) {
      titulo.current?.closest('section')?.scrollIntoView({ block: 'start' });
      titulo.current?.focus({ preventScroll: true });
    }
    desdeSelector.current = false;
  }, [tema.id]);

  return (
    <PaginaGestion titulo="Manual de uso" nota={esAdmin ? 'Lo que hace el Operador y, después, lo que solo hace el Administrador.' : 'Lo que puedes hacer desde el panel en una noche.'}>
      <div className="manual">
        <div className="lg:hidden">
          <Selector
            etiqueta="Tema del manual"
            value={tema.id}
            onChange={(e) => {
              desdeSelector.current = true;
              // Sin entrada nueva en el historial: recorrer la lista no llena el botón Atrás.
              router.replace(rutaDe(e.target.value), { scroll: false });
            }}
          >
            {secciones.map((s) => (
              <option key={s.id} value={s.id}>
                {s.titulo}
              </option>
            ))}
          </Selector>
        </div>
        <nav className="caja manual-indice hidden lg:block" aria-label="Temas del manual">
          <IndiceGrupo titulo="En la noche" temas={delOperador} actual={tema.id} />
          <IndiceGrupo titulo="Solo el Administrador" temas={delAdmin} actual={tema.id} />
        </nav>
        <section key={tema.id} className="caja manual-seccion" aria-labelledby={`t-${tema.id}`}>
          <div className="caja__cabeza mb-0!">
            <h2 id={`t-${tema.id}`} ref={titulo} tabIndex={-1} className="focus:outline-none">
              {tema.titulo}
            </h2>
            {tema.para === 'administrador' ? <span>Solo el Administrador</span> : null}
          </div>
          <ol className="manual-pasos">
            {tema.pasos.map((p) => (
              <li key={p}>{p}</li>
            ))}
          </ol>
          {tema.imagen ? (
            <figure className="manual-figura">
              {/* eslint-disable-next-line @next/next/no-img-element -- captura estática generada por el script del manual */}
              <img src={`/manual/${tema.imagen.archivo}`} alt={tema.imagen.texto} width={1280} height={800} />
              <figcaption>{tema.imagen.texto}</figcaption>
            </figure>
          ) : null}
          <nav className="manual-paginas" aria-label="Páginas del manual">
            {anterior ? (
              <Link href={rutaDe(anterior.id)} prefetch={false} scroll={false} className={claseTecla('normal', 'chica')} aria-label={`Anterior: ${anterior.titulo}`}>
                Anterior
              </Link>
            ) : (
              <span />
            )}
            <p className="manual-paginas__posicion">
              {posicion + 1} de {secciones.length}
            </p>
            {siguiente ? (
              <Link href={rutaDe(siguiente.id)} prefetch={false} scroll={false} className={claseTecla('normal', 'chica')} aria-label={`Siguiente: ${siguiente.titulo}`}>
                Siguiente
              </Link>
            ) : (
              <span />
            )}
          </nav>
        </section>
      </div>
    </PaginaGestion>
  );
}
