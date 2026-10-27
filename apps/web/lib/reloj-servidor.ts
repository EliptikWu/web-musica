'use client';

import { useSyncExternalStore } from 'react';

/**
 * Reloj del servidor (doc 04): cada foto trae `servidor_ahora`; la diferencia con el reloj
 * del teléfono se guarda para que el avance de la canción no dependa de la hora del teléfono.
 */
let desfaseMs = 0;
const oyentesDesfase = new Set<() => void>();

export function anotarHoraServidor(servidorAhoraIso: string): void {
  const nuevo = Date.parse(servidorAhoraIso) - Date.now();
  // Ignora variaciones pequeñas de la red: solo avisa si cambia más de medio segundo.
  if (Math.abs(nuevo - desfaseMs) > 500) {
    desfaseMs = nuevo;
    for (const o of oyentesDesfase) o();
  }
}

export function ahoraServidor(): number {
  return Date.now() + desfaseMs;
}

function suscribirDesfase(oyente: () => void) {
  oyentesDesfase.add(oyente);
  return () => oyentesDesfase.delete(oyente);
}

// Un solo segundero para toda la página, encendido mientras alguien lo mira.
let tic = 0;
let segundero: ReturnType<typeof setInterval> | null = null;
const oyentesTic = new Set<() => void>();

function suscribirTic(oyente: () => void) {
  oyentesTic.add(oyente);
  if (!segundero) {
    tic = Date.now();
    segundero = setInterval(() => {
      tic = Date.now();
      for (const o of oyentesTic) o();
    }, 1000);
  }
  return () => {
    oyentesTic.delete(oyente);
    if (oyentesTic.size === 0 && segundero) {
      clearInterval(segundero);
      segundero = null;
    }
  };
}

/**
 * Hora del servidor, al segundo. Devuelve 0 en el servidor y durante la hidratación: así los
 * dos pintan lo mismo, y quien la usa cae en `servidor_ahora` de la foto (las reglas de
 * espera.ts lo tratan como «sin avance»).
 */
export function useAhoraServidor(): number {
  const ahora = useSyncExternalStore(suscribirTic, () => tic, () => 0);
  const desfase = useSyncExternalStore(suscribirDesfase, () => desfaseMs, () => 0);
  return ahora === 0 ? 0 : ahora + desfase;
}

/** El reloj del teléfono, al segundo (0 en el servidor y en la hidratación). */
export function useTic(): number {
  return useSyncExternalStore(suscribirTic, () => tic, () => 0);
}
