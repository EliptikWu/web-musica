'use client';

import type { NombreEndpoint, Noche, Respuesta } from '@rockola/contrato';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { olvidarTemaPanel } from '@/lib/tema';
import { salida } from '../sesion-panel';
import { useConexion } from '../tiempo-real/estado';
import { claves } from './claves';
import { type OpcionesPedido, pedir } from './cliente';
import { ErrorApiCliente } from './errores';

/** Datos del panel (doc 03). La sesión es una cookie httpOnly del backend: aquí solo se pregunta. */

const sinReintentoEn4xx = (fallos: number, error: unknown) => !(error instanceof ErrorApiCliente && error.estado < 500) && fallos < 2;

export function useYoPanel() {
  return useQuery({ queryKey: claves.panel.yo, queryFn: ({ signal }) => pedir('yoPanel', { signal }), retry: sinReintentoEn4xx, staleTime: 60_000 });
}

export function useNoche() {
  const { estado } = useConexion();
  return useQuery({
    queryKey: claves.panel.noche,
    queryFn: ({ signal }) => pedir('noche', { signal }),
    retry: sinReintentoEn4xx,
    // Con la conexión en vivo, el aviso «panel» la refresca; sin ella, cada 3 s (RNF-009).
    refetchInterval: estado === 'conectado' ? false : 3000,
  });
}

/**
 * Una acción del panel. Al terminar, bien o mal, se vuelve a pedir lo del panel: la verdad
 * es la del servidor. Sin reintentos automáticos: el personal ve el error y decide.
 *
 * `optimista` (pausar y reanudar la música) cambia la noche en pantalla al pulsar, sin esperar
 * la ida y vuelta, y la devuelve como estaba si el servidor dice que no. La tecla sigue ocupada
 * hasta que llega la noche nueva: así no reaparece un instante el botón de antes (lista del
 * equipo, 2026-11-25).
 */
export function useAccionPanel<N extends NombreEndpoint>(nombre: N, optimista?: (noche: Noche) => Noche) {
  const qc = useQueryClient();
  return useMutation<Respuesta<N>, unknown, OpcionesPedido<N>, { antes?: Noche }>({
    mutationFn: (opciones) => (pedir as (n: N, o: OpcionesPedido<N>) => Promise<Respuesta<N>>)(nombre, opciones),
    onMutate: async () => {
      if (!optimista) return {};
      // Una recarga en curso traería la noche de antes y taparía el cambio.
      await qc.cancelQueries({ queryKey: claves.panel.noche });
      const antes = qc.getQueryData<Noche>(claves.panel.noche);
      if (antes) qc.setQueryData(claves.panel.noche, optimista(antes));
      return { antes };
    },
    onError: (_e, _o, contexto) => {
      if (contexto?.antes) qc.setQueryData(claves.panel.noche, contexto.antes);
    },
    onSettled: () => Promise.all([qc.invalidateQueries({ queryKey: claves.panel.noche }), qc.invalidateQueries({ queryKey: ['ahora'] })]),
  });
}

export function useEntrar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (datos: { correo: string; clave: string }) => pedir('entrar', { cuerpo: datos }),
    onSuccess: (yo) => qc.setQueryData(claves.panel.yo, yo),
  });
}

/**
 * Salir. Solo cuando el servidor confirma que cerró la sesión: si falla, la cookie sigue
 * valiendo y el panel lo dice. Se sale con carga completa, que descarta todo lo de memoria.
 */
export function useSalir() {
  return useMutation({
    mutationFn: () => pedir('salir'),
    onMutate: () => {
      salida.enCurso = true;
    },
    onError: () => {
      salida.enCurso = false;
    },
    onSuccess: () => {
      olvidarTemaPanel();
      // Carga completa a propósito: al salir no queda nada del panel en memoria.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.location.assign('/panel/entrar');
    },
  });
}

/**
 * Una acción de gestión: invalida justo lo que cambia (y la noche, que resume parte de ello).
 * El tiempo real lo confirma a las demás pantallas abiertas.
 */
export function useAccionGestion<N extends NombreEndpoint>(nombre: N, invalidar: readonly (readonly string[])[]) {
  const qc = useQueryClient();
  return useMutation<Respuesta<N>, unknown, OpcionesPedido<N>>({
    mutationFn: (opciones) => (pedir as (n: N, o: OpcionesPedido<N>) => Promise<Respuesta<N>>)(nombre, opciones),
    // Se espera la recarga: la acción sigue «ocupada» hasta que se ven los datos nuevos, y los
    // `onSuccess` de quien la llama ya encuentran la pantalla al día (p. ej., para mover el foco).
    onSettled: () => Promise.all([...invalidar, claves.panel.noche].map((clave) => qc.invalidateQueries({ queryKey: clave }))),
  });
}
