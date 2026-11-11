'use client';

import type { Bloqueo, DuracionBloqueo } from '@rockola/contrato';
import { Campo, Dialogo, GrupoTeclas, TarjetaTitulo, Tecla } from '@rockola/ui';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { useAccionGestion } from '@/lib/api/hooks-panel';
import { conPunto, fechaYHora, formatoHora } from '@/lib/formato';
import { DURACIONES, errores } from '@/lib/textos/panel';
import { Cargando, mensajeDeError, PaginaGestion } from './PaginaGestion';

const VIGENCIAS = [
  { valor: 'vigentes', texto: 'Vigentes' },
  { valor: 'vencidos', texto: 'Vencidos' },
] as const;

function textoDuracion(b: Bloqueo, zona: string): string {
  if (b.duracion === 'fin_sesion') return b.fin ? `Hasta el fin de la noche (${formatoHora(b.fin, zona)})` : 'Hasta el fin de la noche';
  if (b.duracion === 'indefinido') return 'Hasta que alguien lo levante';
  return `Por ${b.horas} ${b.horas === 1 ? 'hora' : 'horas'}${b.fin ? `, hasta las ${formatoHora(b.fin, zona)}` : ''}`;
}

/**
 * Contenido bloqueado (RF-043, PB-34): los bloqueos vigentes y los vencidos, con quién y por
 * qué; levantar uno antes de tiempo o cambiar su duración.
 */
export function Bloqueos() {
  const zona = useYoDelPanel().establecimiento.zona_horaria;
  const [vigencia, setVigencia] = useState<'vigentes' | 'vencidos'>('vigentes');
  const bloqueos = useQuery({ queryKey: claves.panel.bloqueos(vigencia), queryFn: ({ signal }) => pedir('bloqueos', { consulta: { vigencia }, signal }) });
  const desbloquear = useAccionGestion('desbloquear', [['panel', 'bloqueos']]);
  const extender = useAccionGestion('extenderBloqueo', [['panel', 'bloqueos']]);
  const [aviso, setAviso] = useState<{ tipo: 'luz' | 'error'; texto: string } | null>(null);
  const [editando, setEditando] = useState<Bloqueo | null>(null);
  const [duracion, setDuracion] = useState<DuracionBloqueo>('fin_sesion');
  const [horas, setHoras] = useState('2');

  return (
    <PaginaGestion titulo="Bloqueos" nota="Mientras un bloqueo está vigente, nadie puede pedir esa canción.">
      <div className="mt-4">
        <GrupoTeclas leyenda="Mostrar" nombre="vigencia" opciones={[...VIGENCIAS]} valor={vigencia} onCambio={setVigencia} />
      </div>
      {aviso ? <Aviso tipo={aviso.tipo} className="mt-4" titulo={aviso.texto} /> : null}
      <section className="caja mt-4" aria-label={vigencia === 'vigentes' ? 'Bloqueos vigentes' : 'Bloqueos vencidos'}>
        <Cargando consulta={bloqueos}>
          {() =>
            bloqueos.data!.bloqueos.length === 0 ? (
              <p className="secundario">{vigencia === 'vigentes' ? 'No hay canciones bloqueadas.' : 'No hay bloqueos vencidos.'}</p>
            ) : (
              <ul className="lista-gestion">
                {bloqueos.data!.bloqueos.map((b) => (
                  <li key={b.id}>
                    <div className="w-full max-w-[18rem]">
                      <TarjetaTitulo titulo={b.cancion.titulo} artista={b.cancion.artista} variante="apagada" />
                    </div>
                    <div className="lista-gestion__info">
                      <p className="lista-gestion__titulo">{b.motivo}</p>
                      <p className="lista-gestion__meta">
                        {conPunto(textoDuracion(b, zona))} {conPunto(`Lo puso ${b.creado_por} el ${fechaYHora(b.inicio, zona)}`)}
                      </p>
                    </div>
                    {b.vigente ? (
                      <div className="lista-gestion__acciones">
                        <Tecla
                          tamano="chica"
                          aria-label={`Cambiar la duración del bloqueo de ${b.cancion.titulo}`}
                          onClick={() => {
                            setDuracion(b.duracion);
                            setHoras(String(b.horas ?? 2));
                            setEditando(b);
                          }}
                        >
                          Duración
                        </Tecla>
                        <Tecla
                          tamano="chica"
                          aria-label={`Desbloquear ${b.cancion.titulo}`}
                          ocupada={desbloquear.isPending}
                          onClick={() =>
                            desbloquear.mutate(
                              { parametros: { id: b.id } },
                              { onSuccess: () => setAviso({ tipo: 'luz', texto: `«${b.cancion.titulo}» se puede pedir otra vez.` }), onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e) }) },
                            )
                          }
                        >
                          Desbloquear
                        </Tecla>
                      </div>
                    ) : null}
                  </li>
                ))}
              </ul>
            )
          }
        </Cargando>
      </section>

      <Dialogo abierto={editando !== null} titulo={`Duración del bloqueo de «${editando?.cancion.titulo ?? ''}»`} onCerrar={() => setEditando(null)}>
        <form
          className="flex flex-col gap-4"
          onSubmit={(e) => {
            e.preventDefault();
            const h = Number(horas);
            if (duracion === 'horas' && !(Number.isInteger(h) && h >= 1 && h <= 168)) return;
            if (!editando) return;
            extender.mutate(
              { parametros: { id: editando.id }, cuerpo: { duracion, horas: duracion === 'horas' ? h : null } },
              {
                onSuccess: () => setAviso({ tipo: 'luz', texto: 'Duración cambiada.' }),
                onError: (err) => setAviso({ tipo: 'error', texto: mensajeDeError(err) }),
                onSettled: () => setEditando(null),
              },
            );
          }}
        >
          <p className="dialogo__texto">Cuenta desde ahora.</p>
          <GrupoTeclas leyenda="Duración" nombre="duracion-bloqueo" opciones={[...DURACIONES]} valor={duracion} onCambio={setDuracion} />
          {duracion === 'horas' ? (
            <Campo
              etiqueta="Horas"
              type="number"
              min={1}
              max={168}
              value={horas}
              onChange={(e) => setHoras(e.target.value)}
              error={!(Number.isInteger(Number(horas)) && Number(horas) >= 1 && Number(horas) <= 168) ? errores.horas : null}
            />
          ) : null}
          <div className="dialogo__acciones">
            <Tecla type="button" onClick={() => setEditando(null)}>
              Cancelar
            </Tecla>
            <Tecla type="submit" variante="cereza" ocupada={extender.isPending}>
              Guardar
            </Tecla>
          </div>
        </form>
      </Dialogo>
    </PaginaGestion>
  );
}
