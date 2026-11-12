'use client';

import type { ItemCola } from '@rockola/contrato';
import { useState } from 'react';
import { codigoDe } from '@/lib/api/errores';
import { nuevaClave, useMoverVoto, useQuitarVoto, useRetirarPedido, useVotar } from '@/lib/api/hooks-cliente';
import { enfocarCuandoExista } from '@/lib/enfocar';
import { voto } from '@/lib/textos/cliente';

export type AvisoVoto = { tipo: 'luz' | 'error' | 'info'; titulo: string; meta: string } | null;

/**
 * Votar desde una lista: la tecla se hunde al instante (optimista) y, si el servidor lo
 * rechaza, la pantalla dice por qué. Un toque más mientras se envía no hace nada (RNF-008).
 */
export function useVotarConAviso(slug: string) {
  const votar = useVotar(slug);
  const moverVoto = useMoverVoto(slug);
  const quitarVoto = useQuitarVoto(slug);
  const retirarPedido = useRetirarPedido(slug);
  const [aviso, setAviso] = useState<AvisoVoto>(null);
  const [anuncio, setAnuncio] = useState('');
  const [enviando, setEnviando] = useState<string | null>(null);

  function alVotar(item: ItemCola) {
    if (enviando) return;
    setAviso(null);
    setEnviando(item.solicitud_id);
    setAnuncio(`Votaste por ${item.cancion.titulo}`);
    // «+1» desaparece: el foco pasa a «Votaste» en la misma fila para no perderse (WCAG 2.4.3).
    const conFoco = document.activeElement?.closest('[data-solicitud]') === document.querySelector(`[data-solicitud="${item.solicitud_id}"]`);
    if (conFoco) enfocarCuandoExista(`[data-solicitud="${item.solicitud_id}"] [data-tecla-fija]`);
    votar.mutate(
      { solicitudId: item.solicitud_id, clave: nuevaClave() },
      {
        onError: (error) => {
          const titulo = item.cancion.titulo;
          const codigo = codigoDe(error);
          setAnuncio('');
          if (codigo === 'ya_votaste') setAviso({ tipo: 'info', ...voto.yaVotaste(titulo) });
          else if (codigo === 'no_pendiente') setAviso({ tipo: 'info', ...voto.yaNoEsta(titulo) });
          else if (codigo === 'servicio_no_disponible') setAviso({ tipo: 'info', ...voto.cerrado });
          else setAviso({ tipo: 'error', ...voto.fallo(titulo) });
        },
        onSettled: () => setEnviando(null),
      },
    );
  }

  /** Mueve el voto de `desde` a `hacia` (RF-014); el foco queda en la nueva «Votaste». */
  function alMover(desde: ItemCola, hacia: ItemCola) {
    if (enviando) {
      setAviso({ tipo: 'info', ...voto.enCamino });
      return;
    }
    setAviso(null);
    setEnviando(hacia.solicitud_id);
    setAnuncio(`Tu voto pasó de ${desde.cancion.titulo} a ${hacia.cancion.titulo}`);
    enfocarCuandoExista(`[data-solicitud="${hacia.solicitud_id}"] [data-tecla-fija]`);
    moverVoto.mutate(
      { desde: desde.solicitud_id, hacia: hacia.solicitud_id, clave: nuevaClave() },
      {
        onSuccess: () => setAviso({ tipo: 'luz', ...voto.movido(desde.cancion.titulo, hacia.cancion.titulo) }),
        onError: (error) => {
          const codigo = codigoDe(error);
          setAnuncio('');
          if (codigo === 'ya_votaste') setAviso({ tipo: 'info', ...voto.yaVotaste(hacia.cancion.titulo) });
          else if (codigo === 'no_pendiente') setAviso({ tipo: 'info', ...voto.yaNoEsta(`${desde.cancion.titulo} o ${hacia.cancion.titulo}`) });
          else if (codigo === 'servicio_no_disponible') setAviso({ tipo: 'info', ...voto.cerrado });
          else if (codigo === 'datos_invalidos') setAviso({ tipo: 'info', ...voto.sinVotoQueMover(desde.cancion.titulo) });
          else setAviso({ tipo: 'error', ...voto.noMovido(desde.cancion.titulo) });
        },
        onSettled: () => setEnviando(null),
      },
    );
  }

  /** Quita el voto (RF-060); «Votaste» vuelve a ser «+1» y el foco queda en ella. */
  function alQuitar(item: ItemCola) {
    if (enviando) {
      setAviso({ tipo: 'info', ...voto.enCamino });
      return;
    }
    const titulo = item.cancion.titulo;
    setAviso(null);
    setEnviando(item.solicitud_id);
    setAnuncio(`Ya no votas por ${titulo}`);
    enfocarCuandoExista(`[data-solicitud="${item.solicitud_id}"] button`);
    quitarVoto.mutate(
      { solicitudId: item.solicitud_id, clave: nuevaClave() },
      {
        onSuccess: () => setAviso({ tipo: 'luz', ...voto.quitado(titulo) }),
        onError: (error) => {
          const codigo = codigoDe(error);
          setAnuncio('');
          if (codigo === 'no_pendiente') setAviso({ tipo: 'info', ...voto.yaNoEsta(titulo) });
          else if (codigo === 'servicio_no_disponible') setAviso({ tipo: 'info', ...voto.cerrado });
          else setAviso({ tipo: 'error', ...voto.noQuitado(titulo) });
        },
        onSettled: () => setEnviando(null),
      },
    );
  }

  /** Retira el pedido (RF-061); la fila desaparece y el aviso dice que ya se puede pedir otra. */
  function alRetirar(item: ItemCola) {
    if (enviando) {
      setAviso({ tipo: 'info', ...voto.enCamino });
      return;
    }
    const titulo = item.cancion.titulo;
    setAviso(null);
    setEnviando(item.solicitud_id);
    setAnuncio(`${titulo} salió de la cola`);
    // La fila desaparece con su tecla: el foco pasa a la siguiente canción, o al aviso (WCAG 2.4.3).
    const siguiente = document.querySelector(`[data-solicitud="${item.solicitud_id}"]`)?.nextElementSibling?.getAttribute('data-solicitud');
    enfocarCuandoExista(siguiente ? `[data-solicitud="${siguiente}"] button` : '.aviso button');
    retirarPedido.mutate(
      { solicitudId: item.solicitud_id, clave: nuevaClave() },
      {
        onSuccess: () => setAviso({ tipo: 'luz', ...voto.retirado(titulo) }),
        onError: (error) => {
          const codigo = codigoDe(error);
          setAnuncio('');
          if (codigo === 'no_pendiente') setAviso({ tipo: 'info', ...voto.yaNoSeRetira(titulo) });
          else if (codigo === 'servicio_no_disponible') setAviso({ tipo: 'info', ...voto.cerrado });
          else setAviso({ tipo: 'error', ...voto.noRetirado(titulo) });
        },
        onSettled: () => setEnviando(null),
      },
    );
  }

  return { alVotar, alMover, alQuitar, alRetirar, aviso, cerrarAviso: () => setAviso(null), anuncio, enviando };
}
