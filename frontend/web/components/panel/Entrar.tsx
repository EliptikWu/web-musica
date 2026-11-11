'use client';

import { Campo, LuzEstado, Tecla } from '@rockola/ui';
import { useRouter, useSearchParams } from 'next/navigation';
import { type FormEvent, useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { codigoDe, ErrorApiCliente } from '@/lib/api/errores';
import { useEntrar } from '@/lib/api/hooks-panel';
import { formatoHora } from '@/lib/formato';
import { MODO } from '@/lib/modo';
import { entrada, errores, P } from '@/lib/textos/panel';

/** Solo se vuelve a rutas del panel: nada de redirigir a otro sitio con `?volver=`. */
function destinoSeguro(volver: string | null): string {
  return volver && /^\/panel(\/[a-z0-9-/]*)?$/.test(volver) && !volver.startsWith('/panel/entrar') ? volver : '/panel';
}

/**
 * Entrar al panel (RF-021, PB-10). El error es siempre el mismo para correo o clave
 * equivocados: no dice si el correo existe. Tras varios intentos, la cuenta espera unos minutos.
 */
export function Entrar() {
  const router = useRouter();
  const parametros = useSearchParams();
  const entrar = useEntrar();
  const [correo, setCorreo] = useState('');
  const [clave, setClave] = useState('');
  const [verClave, setVerClave] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function enviar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    entrar.mutate(
      { correo: correo.trim(), clave },
      {
        onSuccess: (yo) => router.replace(yo.usuario.debe_cambiar_clave ? '/panel/cambiar-clave' : destinoSeguro(parametros.get('volver'))),
        onError: (err) => {
          const codigo = codigoDe(err);
          if (codigo === 'credenciales_invalidas' || codigo === 'datos_invalidos') setError(entrada.credenciales);
          else if (codigo === 'cuenta_bloqueada') {
            const hasta = err instanceof ErrorApiCliente && typeof err.detalle?.reintentar_en === 'string' ? formatoHora(err.detalle.reintentar_en) : 'unos minutos';
            setError(entrada.bloqueada(hasta));
          } else setError(errores.generico);
        },
      },
    );
  }

  return (
    <main className="madera flex min-h-dvh flex-col items-center px-4 pt-14 pb-10">
      <div className="w-full max-w-[26rem]">
        <header className="text-center">
          <p className="placa text-[22px]">Rockola</p>
          <LuzEstado encendida className="mt-2">
            {P.panelDeLaRockola}
          </LuzEstado>
        </header>

        {parametros.get('motivo') === 'caducada' ? <Aviso tipo="info" className="mt-6" meta={entrada.caducada} /> : null}

        <form className="caja mt-6 flex flex-col gap-4 px-5 pt-5 pb-6" onSubmit={enviar} noValidate>
          <h1 className="titulo-seccion text-[20px]">{entrada.titulo}</h1>
          {error ? <Aviso tipo="error" titulo={error} /> : null}
          <Campo etiqueta={entrada.correo} type="email" autoComplete="username" inputMode="email" required value={correo} onChange={(e) => setCorreo(e.target.value)} />
          <div className="relative">
            <Campo
              etiqueta={entrada.clave}
              type={verClave ? 'text' : 'password'}
              autoComplete="current-password"
              required
              value={clave}
              onChange={(e) => setClave(e.target.value)}
            />
            <button
              type="button"
              className="absolute right-1 bottom-0 grid h-12 min-w-11 place-items-center px-2 text-[13px] font-semibold text-tinta-2 underline underline-offset-2"
              onClick={() => setVerClave((v) => !v)}
              aria-pressed={verClave}
            >
              {verClave ? 'Ocultar' : 'Mostrar'}
              <span className="solo-lector"> clave</span>
            </button>
          </div>
          <Tecla type="submit" variante="cereza" tamano="grande" ocupada={entrar.isPending}>
            {P.entrar}
          </Tecla>
          <p className="secundario text-center text-[14px]">{entrada.olvido}</p>
        </form>

        {MODO === 'simulado' ? <Aviso tipo="info" className="mt-4" meta={entrada.simulacion} /> : null}
      </div>
    </main>
  );
}
