'use client';

import type { Ahora, Bar, EstadoServicio } from '@rockola/contrato';
import { Arco, claseTecla, DiasSemana, LuzEstado, Ventana } from '@rockola/ui';
import Link from 'next/link';
import { Aviso } from '@/components/comun/Aviso';
import { AvisoPantalla } from '@/components/comun/AvisoPantalla';
import { DIAS, resumenHorario } from '@/lib/formato';
import { diasQueAbren, momentoLocal } from '@/lib/reglas/horario';
import { falloCarga, servicioCerrado, T } from '@/lib/textos/cliente';

/**
 * El bar no recibe canciones (prototipo 4, PB-30): el arco apagado con el motivo, el horario
 * de la semana y la salida a «Ver qué está sonando». Sirve para fuera de horario, pausa y
 * mantenimiento, cada uno con su texto.
 */
export function EstadoCerrado({ slug, bar, ahora }: { slug: string; bar: Bar; ahora: Ahora }) {
  const estado = ahora.servicio.estado as Exclude<EstadoServicio, 'abierto'>;
  const instante = Date.parse(ahora.servidor_ahora);
  const textos = servicioCerrado(estado, ahora.servicio.proxima_apertura, bar.zona_horaria, instante);
  const abren = diasQueAbren(bar.horario);
  const hoy = momentoLocal(new Date(instante), bar.zona_horaria).dia;
  const lineas = resumenHorario(bar.horario);
  return (
    <>
      <header className="text-center">
        <h1 className="placa text-[25px]">{bar.nombre}</h1>
        <LuzEstado encendida={false} className="mt-2">
          {textos.luz}
        </LuzEstado>
      </header>

      <Arco apagado className="mt-[34px]" aria-label="Estado de la rockola">
        <div className="flex min-h-[322px] flex-col justify-end px-[18px] pt-[104px] pb-[18px]">
          <Ventana className="px-4 pt-4 pb-[18px]">
            <h2 className="condensada text-[34px] leading-[.98]">{textos.titulo}</h2>
            <p className="mt-[10px] text-[15px] leading-[1.4] text-tinta-2">{textos.detalle}</p>
          </Ventana>
        </div>
      </Arco>

      {lineas.length > 0 ? (
        <section className="mt-9" aria-labelledby="t-horario">
          <h2 id="t-horario" className="titulo-seccion text-center">
            Horario de pedidos
          </h2>
          <DiasSemana className="mt-3" dias={DIAS.map((d, i) => ({ ...d, abre: abren.has(i + 1), hoy: hoy === i + 1 }))} />
          {lineas.map((l) => (
            <p key={l.dias} className="secundario mt-[14px] text-center text-[16px]">
              {l.dias}, <b className="font-bold text-crema">{l.horas}</b>
            </p>
          ))}
        </section>
      ) : null}

      <div className="mt-auto pt-10 text-center">
        <Link href={`/b/${slug}/cola`} className={claseTecla('cereza', 'grande')}>
          {T.verQueSuena}
        </Link>
        <EnlacePolitica slug={slug} className="mt-[10px]" />
      </div>
    </>
  );
}

export function EnlacePolitica({ slug, className }: { slug: string; className?: string }) {
  return (
    <p className={['text-center text-[14px]', className].filter(Boolean).join(' ')}>
      <Link href={`/b/${slug}/politica-de-datos`} className="inline-flex min-h-11 items-center font-semibold text-crema underline underline-offset-[3px]">
        {T.politica}
      </Link>
    </p>
  );
}

/** No llegó el bar o la foto tras los reintentos: se dice y se puede volver a intentar. */
export function FalloCarga({ onReintentar }: { onReintentar: () => void }) {
  return (
    <Aviso
      tipo="error"
      titulo={falloCarga.titulo}
      meta={falloCarga.meta}
      accion={
        <button type="button" onClick={onReintentar}>
          {T.reintentar}
        </button>
      }
    />
  );
}

/** Mientras llega la primera foto: la forma de la pantalla, sin texto inventado. */
export function CargandoCliente({ arco = true }: { arco?: boolean }) {
  return (
    <div aria-busy="true">
      <p className="solo-lector" role="status">
        {T.cargando}
      </p>
      <div className="esqueleto mx-auto h-7 w-48" />
      {arco ? <div className="esqueleto mt-6 h-[292px] rounded-t-[50%]" /> : null}
      <div className="mt-6 grid gap-2">
        <div className="esqueleto h-14" />
        <div className="esqueleto h-14" />
        <div className="esqueleto h-14" />
      </div>
    </div>
  );
}

export function BarNoEncontrado() {
  return (
    <AvisoPantalla
      titulo="Aquí no hay ninguna rockola"
      accion={
        <Link href="/" className={claseTecla('cereza', 'grande')}>
          Ir al inicio
        </Link>
      }
    >
      <p>La dirección no existe o el QR ya no está en uso. Escanea el QR que está en tu mesa.</p>
    </AvisoPantalla>
  );
}
