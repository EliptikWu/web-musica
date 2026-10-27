'use client';

import type { Franja } from '@rockola/contrato';
import { Campo, Tecla } from '@rockola/ui';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { useAccionGestion } from '@/lib/api/hooks-panel';
import { capitalizar, DIAS, resumenHorario } from '@/lib/formato';
import { validarFranjas } from '@/lib/reglas/horario';
import { Cargando, mensajeDeError, PaginaGestion } from './PaginaGestion';

type Dia = { abre: boolean; inicio: string; fin: string };

/**
 * Horario del QR (RF-036, PB-28): un tramo por día; si cruza medianoche (20:00 a 02:00) es del
 * día en que empieza. El QR se abre y se cierra solo con él, salvo pausa o apertura forzada.
 */
export function Horario() {
  const horario = useQuery({ queryKey: claves.panel.horario, queryFn: ({ signal }) => pedir('horario', { signal }) });
  // El aviso y la acción viven aquí: al guardar, el editor se vuelve a montar con el horario
  // nuevo, y React Query no avisa del resultado a un componente que ya no está.
  const [aviso, setAviso] = useState<Mensaje | null>(null);
  const guardar = useAccionGestion('guardarHorario', [claves.panel.horario]);
  return (
    <PaginaGestion titulo="Horario del QR" nota="Las solicitudes se abren y se cierran solas con este horario. Si un tramo pasa de medianoche, cuenta para el día en que empieza.">
      <Cargando consulta={horario}>
        {() => <EditorHorario key={JSON.stringify(horario.data!.franjas)} franjas={horario.data!.franjas} guardar={guardar} aviso={aviso} setAviso={setAviso} />}
      </Cargando>
    </PaginaGestion>
  );
}

type Mensaje = { tipo: 'luz' | 'error'; texto: string };

function aDias(franjas: Franja[]): Dia[] {
  return DIAS.map((_, i) => {
    const f = franjas.find((x) => x.dia_semana === i + 1);
    return f ? { abre: f.activo, inicio: f.hora_inicio, fin: f.hora_fin } : { abre: false, inicio: '20:00', fin: '02:00' };
  });
}

function aFranjas(dias: Dia[]): Franja[] {
  return dias.map((d, i) => ({ dia_semana: i + 1, hora_inicio: d.inicio, hora_fin: d.fin, activo: d.abre })).filter((f) => f.activo);
}

function EditorHorario({
  franjas,
  guardar,
  aviso,
  setAviso,
}: {
  franjas: Franja[];
  guardar: ReturnType<typeof useAccionGestion<'guardarHorario'>>;
  aviso: Mensaje | null;
  setAviso: (a: Mensaje | null) => void;
}) {
  const [dias, setDias] = useState<Dia[]>(() => aDias(franjas));
  const nuevas = aFranjas(dias);
  const problemas = validarFranjas(nuevas);
  const solapadas = new Set(problemas.filter((p) => p.tipo === 'solapada').flatMap((p) => p.indices.map((i) => nuevas[i]!.dia_semana)));
  const vacias = new Set(problemas.filter((p) => p.tipo === 'misma_hora').flatMap((p) => p.indices.map((i) => nuevas[i]!.dia_semana)));
  const resumen = resumenHorario(nuevas);

  const cambiar = (i: number, cambio: Partial<Dia>) => {
    setAviso(null);
    setDias((d) => d.map((x, j) => (j === i ? { ...x, ...cambio } : x)));
  };

  return (
    <form
      className="caja mt-4 max-w-[52rem]"
      onSubmit={(e) => {
        e.preventDefault();
        if (problemas.length > 0) return;
        guardar.mutate(
          { cuerpo: { franjas: nuevas } },
          {
            onSuccess: () => setAviso({ tipo: 'luz', texto: 'Horario guardado. El QR ya sigue el nuevo horario.' }),
            onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e, { conflicto: 'Hay tramos que se pisan.', datos_invalidos: 'Un tramo empieza y termina a la misma hora.' }) }),
          },
        );
      }}
    >
      <ul className="horario-dias">
        {dias.map((d, i) => {
          const dia = i + 1;
          const error = solapadas.has(dia) ? 'Se pisa con el tramo de otro día.' : vacias.has(dia) ? 'Empieza y termina a la misma hora.' : null;
          return (
            <li key={DIAS[i]!.corto} className="horario-dia">
              <label className="interruptor">
                <input type="checkbox" checked={d.abre} onChange={(e) => cambiar(i, { abre: e.target.checked })} />
                {capitalizar(DIAS[i]!.largo)}
              </label>
              {d.abre ? (
                <div>
                  <div className="horario-dia__horas">
                    <Campo etiqueta="Abre" type="time" claseEntrada="campo__entrada--hora" value={d.inicio} onChange={(e) => cambiar(i, { inicio: e.target.value })} required invalido={Boolean(error)} />
                    <Campo etiqueta="Cierra" type="time" claseEntrada="campo__entrada--hora" value={d.fin} onChange={(e) => cambiar(i, { fin: e.target.value })} required invalido={Boolean(error)} />
                  </div>
                  {error ? <p className="campo__error mt-2">{error}</p> : null}
                </div>
              ) : (
                <p className="secundario text-[14px]">Cerrado: no se reciben canciones.</p>
              )}
            </li>
          );
        })}
      </ul>
      <p className="secundario mt-4 text-[15px]" aria-live="polite">
        {resumen.length === 0
          ? 'Sin días abiertos: el QR solo se abre a mano.'
          : resumen.map((l) => `${l.dias}, ${l.horas}`).join('. ')}
      </p>
      {aviso ? <Aviso tipo={aviso.tipo} className="mt-4" titulo={aviso.texto} /> : null}
      <div className="mt-4 flex flex-wrap gap-3">
        <Tecla type="submit" variante="cereza" tamano="ancha" ocupada={guardar.isPending}>
          Guardar horario
        </Tecla>
        <Tecla
          type="button"
          tamano="ancha"
          onClick={() => {
            setAviso(null);
            setDias(aDias(franjas));
          }}
        >
          Deshacer cambios
        </Tecla>
      </div>
    </form>
  );
}
