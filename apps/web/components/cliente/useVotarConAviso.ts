'use client';

import type { ItemCola } from '@rockola/contrato';
import { useState } from 'react';
import { codigoDe } from '@/lib/api/errores';
import { nuevaClave, useMoverVoto, useVotar } from '@/lib/api/hooks-cliente';
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

  return { alVotar, alMover, aviso, cerrarAviso: () => setAviso(null), anuncio, enviando };
}
