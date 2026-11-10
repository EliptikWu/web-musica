'use client';

import type { VinculoPantalla } from '@rockola/contrato';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useState } from 'react';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';

const CADA_MS = 2_000; // RNF-009: la TV pasa a la rockola en 3 s o menos, con margen.
// Ya vinculada, se revisa de vez en cuando: si la desvinculan, deja de poner música aunque no reporte.
const VINCULADA_CADA_MS = 10_000;
const REINTENTO_MS = 10_000;

/**
 * La vinculación de la pantalla del bar (RF-058, ADR-23). Sin vincular, pide un código y lo
 * muestra; pregunta cada 2 s hasta que el Administrador lo escribe en el panel, y pide otro
 * cuando vence. Vinculada, revisa cada 10 s por si la desvincularon (también lo sabe por un
 * 401 al reportar). La credencial es la cookie httpOnly que pone el backend: aquí no se toca.
 */
export function useVinculoPantalla(slug: string) {
  const qc = useQueryClient();
  const clave = claves.vinculoPantalla(slug);
  const vinculo = useQuery({
    queryKey: clave,
    queryFn: ({ signal }) => pedir('vinculoPantalla', { parametros: { slug }, signal }),
    refetchInterval: (q) => (q.state.data?.estado === 'vinculada' ? VINCULADA_CADA_MS : CADA_MS),
  });
  const codigo = useMutation({
    mutationFn: () => pedir('pedirCodigoPantalla', { parametros: { slug } }),
    onSuccess: async (datos: VinculoPantalla) => {
      // Una consulta que salió antes de tener la cookie respondería «sin vincular» y pediría otro código.
      await qc.cancelQueries({ queryKey: clave });
      qc.setQueryData(clave, datos);
    },
  });
  // Un reporte respondió 401: alguien la desvinculó desde el panel.
  const [desvinculada, setDesvinculada] = useState(false);

  const estado = vinculo.data?.estado;
  // Pasó de vinculada a sin vincular: la desvincularon (se guarda el estado anterior en el render,
  // como recomienda React, en vez de un efecto).
  const [anterior, setAnterior] = useState(estado);
  if (estado !== anterior) {
    if (anterior === 'vinculada' && estado === 'sin_vincular') setDesvinculada(true);
    setAnterior(estado);
  }
  const { mutate, reset, isPending, isError } = codigo;
  useEffect(() => {
    if (estado !== 'sin_vincular' || isPending) return;
    if (isError) {
      // Sin red, no se insiste en cada consulta: se espera y se vuelve a pedir.
      const t = setTimeout(reset, REINTENTO_MS);
      return () => clearTimeout(t);
    }
    mutate();
  }, [estado, isPending, isError, mutate, reset]);

  const alPerderVinculo = useCallback(() => {
    setDesvinculada(true);
    void qc.invalidateQueries({ queryKey: claves.vinculoPantalla(slug) });
  }, [qc, slug]);

  // El aviso de «se desvinculó» solo tiene sentido mientras no vuelva a estar vinculada.
  return { vinculo, sinRed: codigo.isError, desvinculada: desvinculada && estado !== 'vinculada', alPerderVinculo };
}
