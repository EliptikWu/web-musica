'use client';

import { Campo, LuzEstado, Tecla } from '@rockola/ui';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { type FormEvent, useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { codigoDe } from '@/lib/api/errores';
import { entrada, errores, P } from '@/lib/textos/panel';

/** Cambio de clave obligatorio en el primer ingreso o tras restablecerla (RF-023, RF-025). */
export function CambiarClave() {
  const router = useRouter();
  const qc = useQueryClient();
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [repetir, setRepetir] = useState('');
  const [intento, setIntento] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const cambiar = useMutation({ mutationFn: () => pedir('cambiarClave', { cuerpo: { actual, nueva } }) });

  const errorNueva = intento && nueva.length < 10 ? entrada.nuevaAyuda : null;
  const errorRepetir = intento && repetir !== nueva ? entrada.noCoinciden : null;

  function enviar(e: FormEvent) {
    e.preventDefault();
    setIntento(true);
    setError(null);
    if (nueva.length < 10 || repetir !== nueva) return;
    cambiar.mutate(undefined, {
      onSuccess: () => {
        // Se descarta el «yo» guardado (aún dice debe_cambiar_clave): el panel lo vuelve a pedir.
        qc.removeQueries({ queryKey: claves.panel.yo });
        router.replace('/panel');
      },
      onError: (err) => {
        const codigo = codigoDe(err);
        if (codigo === 'no_autenticado') router.replace('/panel/entrar?motivo=caducada');
        else if (codigo === 'credenciales_invalidas') setError('La clave actual no coincide.');
        else if (codigo === 'datos_invalidos') setError('La clave nueva tiene que ser distinta y tener al menos 10 caracteres.');
        else setError(errores.generico);
      },
    });
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
        <form className="caja mt-6 flex flex-col gap-4 px-5 pt-5 pb-6" onSubmit={enviar} noValidate>
          <h1 className="titulo-seccion text-[20px]">{entrada.cambiarTitulo}</h1>
          <p className="secundario text-[15px]">{entrada.cambiarTexto}</p>
          {error ? <Aviso tipo="error" titulo={error} /> : null}
          <Campo etiqueta={entrada.actual} type="password" autoComplete="current-password" value={actual} onChange={(e) => setActual(e.target.value)} />
          <Campo etiqueta={entrada.nueva} ayuda={entrada.nuevaAyuda} type="password" autoComplete="new-password" value={nueva} onChange={(e) => setNueva(e.target.value)} error={errorNueva} />
          <Campo etiqueta={entrada.repetir} type="password" autoComplete="new-password" value={repetir} onChange={(e) => setRepetir(e.target.value)} error={errorRepetir} />
          <Tecla type="submit" variante="cereza" tamano="grande" ocupada={cambiar.isPending}>
            {entrada.guardar}
          </Tecla>
        </form>
      </div>
    </main>
  );
}
