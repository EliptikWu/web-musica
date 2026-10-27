'use client';

import type { Ahora, Busqueda, Yo } from '@rockola/contrato';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { tipoDispositivo } from '../dispositivo';
import { nuevaClave } from '../idempotencia';
import { renumerar } from '../reglas/cola';
import { anotarHoraServidor } from '../reloj-servidor';
import { conectarSse } from '../tiempo-real/fuente';
import { anotarConexion, useConexion } from '../tiempo-real/estado';
import { claves } from './claves';
import { pedir } from './cliente';
import { ErrorRed } from './errores';

/**
 * Datos del cliente y de la pantalla del bar (doc 03, «Datos»). Todo se pide desde el
 * navegador, se valida con el contrato y se mantiene al día con el tiempo real.
 */

export function useBar(slug: string) {
  return useQuery({ queryKey: claves.bar(slug), queryFn: ({ signal }) => pedir('bar', { parametros: { slug }, signal }), staleTime: 5 * 60_000 });
}

export function useAhora(slug: string) {
  const { estado } = useConexion();
  const consulta = useQuery({
    queryKey: claves.ahora(slug),
    queryFn: ({ signal }) => pedir('ahora', { parametros: { slug }, signal }),
    // Con la conexión en vivo, las fotos llegan solas; sin ella, se pregunta cada 3 s (RNF-009).
    refetchInterval: estado === 'conectado' ? false : 3000,
    staleTime: estado === 'conectado' ? Infinity : 2000,
  });
  const servidorAhora = consulta.data?.servidor_ahora;
  useEffect(() => {
    if (servidorAhora) anotarHoraServidor(servidorAhora);
  }, [servidorAhora]);
  return consulta;
}

export function useDestacadas(slug: string) {
  return useQuery({ queryKey: claves.destacadas(slug), queryFn: ({ signal }) => pedir('destacadas', { parametros: { slug }, signal }) });
}

/** Bares en los que este teléfono ya quedó registrado en esta visita (solo cuenta si salió bien). */
const registrados = new Set<string>();

/**
 * Lo de este teléfono. La primera vez en la visita se registra (RF-002) y después solo se
 * consulta, monte la pantalla que monte. Si el registro falla, el reintento vuelve a registrar.
 */
export function useYo(slug: string) {
  return useQuery({
    queryKey: claves.yo(slug),
    queryFn: async ({ signal }) => {
      if (registrados.has(slug)) return pedir('yo', { parametros: { slug }, signal });
      const yo = await pedir('registrarDispositivo', { parametros: { slug }, cuerpo: { tipo: tipoDispositivo(window.innerWidth) }, signal });
      registrados.add(slug);
      return yo;
    },
  });
}

/** Busca mientras se escribe (RF-005): desde 2 letras y 300 ms después de la última tecla. */
export function useBusqueda(slug: string, consulta: string) {
  const q = consulta.trim();
  return useQuery({
    queryKey: claves.busqueda(slug, q.toLowerCase()),
    queryFn: ({ signal }) => pedir('buscar', { parametros: { slug }, consulta: { q }, signal }),
    enabled: q.length >= 2,
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    retry: false,
  });
}

export function useAvisoVisto(slug: string, yo: Yo | undefined) {
  const enviado = useRef(false);
  useEffect(() => {
    if (!yo || yo.aviso_visto_en || enviado.current) return;
    enviado.current = true;
    pedir('avisoVisto', { parametros: { slug } }).catch((error: unknown) => console.error('[aviso] no se pudo registrar', error));
  }, [slug, yo]);
}

/** Reintenta una sola vez y solo si no hubo respuesta: con la misma clave no se duplica (RNF-008). */
const reintentarSoloRed = (fallos: number, error: unknown) => fallos < 1 && error instanceof ErrorRed;

type VariablesVoto = { solicitudId: string; clave: string };

/** Votar con respuesta inmediata (RF-012): la tecla se hunde ya y se revierte si falla. */
export function useVotar(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ solicitudId, clave }: VariablesVoto) => pedir('votar', { parametros: { slug, id: solicitudId }, idempotencia: clave }),
    retry: reintentarSoloRed,
    onMutate: async ({ solicitudId }) => {
      // También `yo`: una respuesta vieja en vuelo borraría el voto recién puesto.
      await Promise.all([qc.cancelQueries({ queryKey: claves.ahora(slug) }), qc.cancelQueries({ queryKey: claves.yo(slug) })]);
      const antes = { ahora: qc.getQueryData<Ahora>(claves.ahora(slug)), yo: qc.getQueryData<Yo>(claves.yo(slug)) };
      const sumar = <T extends { solicitud_id: string; votos_total: number }>(i: T): T => (i.solicitud_id === solicitudId ? { ...i, votos_total: i.votos_total + 1 } : i);
      qc.setQueryData<Ahora>(claves.ahora(slug), (a) => (a ? { ...a, cola: renumerar(a.cola.map(sumar)) } : a));
      qc.setQueryData<Yo>(claves.yo(slug), (y) => (y && !y.votos.includes(solicitudId) ? { ...y, votos: [...y.votos, solicitudId] } : y));
      qc.setQueriesData<Busqueda>({ queryKey: ['busqueda', slug] }, (b) => (b ? { ...b, en_cola: b.en_cola.map(sumar) } : b));
      return antes;
    },
    onError: (_error, { solicitudId }, antes) => {
      // Se deshace solo si no llegó nada más nuevo; si llegó, se pregunta al servidor.
      const ahora = qc.getQueryData<Ahora>(claves.ahora(slug));
      if (antes?.ahora && ahora && ahora.version === antes.ahora.version) qc.setQueryData(claves.ahora(slug), antes.ahora);
      else void qc.invalidateQueries({ queryKey: claves.ahora(slug) });
      qc.setQueryData<Yo>(claves.yo(slug), (y) => (y ? { ...y, votos: y.votos.filter((id) => id !== solicitudId || antes?.yo?.votos.includes(id)) } : y));
    },
    onSettled: () => {
      void qc.invalidateQueries({ queryKey: claves.yo(slug) });
      void qc.invalidateQueries({ queryKey: ['busqueda', slug] });
    },
  });
}

export function usePedir(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ videoId, clave }: { videoId: string; clave: string }) => pedir('pedir', { parametros: { slug }, cuerpo: { video_id: videoId }, idempotencia: clave }),
    retry: reintentarSoloRed,
    onSettled: () => {
      void qc.invalidateQueries({ queryKey: claves.yo(slug) });
      void qc.invalidateQueries({ queryKey: claves.ahora(slug) });
      void qc.invalidateQueries({ queryKey: ['busqueda', slug] });
    },
  });
}

export { nuevaClave };

/**
 * Conexión en vivo del bar: cada foto nueva reemplaza la guardada y, si cambió qué suena o
 * quién está en la cola, se vuelve a pedir lo de este teléfono (sus pedidos pudieron cambiar
 * de estado). Al reconectar, se vuelve a pedir todo (RNF-012).
 */
export function useTiempoRealBar(slug: string) {
  const qc = useQueryClient();
  useEffect(() => {
    let firma = '';
    let estadoPrevio = '';
    let estuvoConectado = false;
    return conectarSse(
      slug,
      (evento) => {
        if (evento.tipo === 'ahora') {
          const guardada = qc.getQueryData<Ahora>(claves.ahora(slug));
          if (guardada && evento.ahora.version < guardada.version) return;
          qc.setQueryData(claves.ahora(slug), evento.ahora);
          anotarHoraServidor(evento.ahora.servidor_ahora);
          const nueva = `${evento.ahora.reproduccion?.id ?? '-'}|${evento.ahora.cola.map((i) => i.solicitud_id).join(',')}|${evento.ahora.servicio.estado}`;
          if (firma && nueva !== firma) {
            void qc.invalidateQueries({ queryKey: claves.yo(slug) });
            void qc.invalidateQueries({ queryKey: claves.destacadas(slug) });
          }
          firma = nueva;
        } else if (evento.tipo === 'ping') {
          anotarHoraServidor(evento.servidor_ahora);
        } else {
          // Solo lo que el servidor dice que cambió (nunca la sesión, que no viaja por aquí).
          for (const recurso of evento.recursos) void qc.invalidateQueries({ queryKey: ['panel', recurso] });
        }
      },
      (estado) => {
        anotarConexion(estado);
        // Solo al RE-conectar se vuelve a pedir todo (RNF-012); la primera conexión ya trae la foto.
        if (estado === 'conectado') {
          if (estuvoConectado && estadoPrevio !== 'conectado') void qc.invalidateQueries();
          estuvoConectado = true;
        }
        estadoPrevio = estado;
      },
    );
  }, [qc, slug]);
}
