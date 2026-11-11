'use client';

import type { Cancion, DuracionBloqueo } from '@rockola/contrato';
import { Campo, Dialogo, GrupoTeclas, Tecla } from '@rockola/ui';
import { useState } from 'react';
import { DURACIONES, dialogos, errores, MOTIVOS_RETIRO, P } from '@/lib/textos/panel';

/**
 * Bloquear un video (RF-042): motivo obligatorio y duración. El Operador solo bloquea hasta
 * el fin de la noche (D9); el resto de opciones no se le ofrecen.
 */
export function DialogoBloquear({
  cancion,
  soloNoche,
  ocupado,
  onConfirmar,
  onCerrar,
}: {
  cancion: Cancion | null;
  soloNoche: boolean;
  ocupado: boolean;
  onConfirmar: (datos: { motivo: string; duracion: DuracionBloqueo; horas: number | null }) => void;
  onCerrar: () => void;
}) {
  const [motivo, setMotivo] = useState('');
  const [duracion, setDuracion] = useState<DuracionBloqueo>('fin_sesion');
  const [horas, setHoras] = useState('2');
  const [intento, setIntento] = useState(false);
  const textos = dialogos.bloquear(cancion?.titulo ?? '');
  const horasNum = Number(horas);
  const errorMotivo = intento && !motivo.trim() ? errores.motivoVacio : null;
  const errorHoras = intento && duracion === 'horas' && !(Number.isInteger(horasNum) && horasNum >= 1 && horasNum <= 168) ? errores.horas : null;

  function cerrar() {
    setMotivo('');
    setDuracion('fin_sesion');
    setIntento(false);
    onCerrar();
  }

  return (
    <Dialogo abierto={cancion !== null} titulo={textos.titulo} onCerrar={cerrar}>
      <form
        className="flex flex-col gap-4"
        onSubmit={(e) => {
          e.preventDefault();
          setIntento(true);
          if (!motivo.trim() || (duracion === 'horas' && !(Number.isInteger(horasNum) && horasNum >= 1 && horasNum <= 168))) return;
          onConfirmar({ motivo: motivo.trim(), duracion, horas: duracion === 'horas' ? horasNum : null });
        }}
        noValidate
      >
        <p className="dialogo__texto">{textos.texto}</p>
        <Campo etiqueta={textos.motivo} ayuda={textos.motivoAyuda} value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={140} error={errorMotivo} autoFocus />
        {soloNoche ? (
          <p className="campo__ayuda">Tu rol bloquea hasta el fin de la noche.</p>
        ) : (
          <GrupoTeclas leyenda={textos.duracion} nombre="duracion" opciones={[...DURACIONES]} valor={duracion} onCambio={setDuracion} />
        )}
        {!soloNoche && duracion === 'horas' ? (
          <Campo etiqueta={textos.horas} type="number" inputMode="numeric" min={1} max={168} value={horas} onChange={(e) => setHoras(e.target.value)} error={errorHoras} />
        ) : null}
        <div className="dialogo__acciones">
          <Tecla type="button" onClick={cerrar}>
            {P.cancelar}
          </Tecla>
          <Tecla type="submit" variante="cereza" ocupada={ocupado}>
            {textos.confirmar}
          </Tecla>
        </div>
      </form>
    </Dialogo>
  );
}

/** Retirar una solicitud (RF-041): con motivo, que verá quien la pidió. */
export function DialogoRetirar({
  titulo,
  ocupado,
  onConfirmar,
  onCerrar,
}: {
  titulo: string | null;
  ocupado: boolean;
  onConfirmar: (datos: { motivo: 'inapropiada' | 'duplicada' | 'otro'; detalle: string | null }) => void;
  onCerrar: () => void;
}) {
  const [motivo, setMotivo] = useState<'inapropiada' | 'duplicada' | 'otro'>('duplicada');
  const [detalle, setDetalle] = useState('');
  const textos = dialogos.retirar(titulo ?? '');
  function cerrar() {
    setMotivo('duplicada');
    setDetalle('');
    onCerrar();
  }
  return (
    <Dialogo abierto={titulo !== null} titulo={textos.titulo} onCerrar={cerrar}>
      <form
        className="flex flex-col gap-4"
        onSubmit={(e) => {
          e.preventDefault();
          onConfirmar({ motivo, detalle: detalle.trim() || null });
        }}
      >
        <p className="dialogo__texto">{textos.texto}</p>
        <GrupoTeclas leyenda={textos.motivo} nombre="motivo" opciones={[...MOTIVOS_RETIRO]} valor={motivo} onCambio={setMotivo} />
        <Campo etiqueta={textos.detalle} value={detalle} onChange={(e) => setDetalle(e.target.value)} maxLength={200} />
        <div className="dialogo__acciones">
          <Tecla type="button" onClick={cerrar}>
            {P.cancelar}
          </Tecla>
          <Tecla type="submit" variante="cereza" ocupada={ocupado}>
            {textos.confirmar}
          </Tecla>
        </div>
      </form>
    </Dialogo>
  );
}

/** Confirmación de algo que no se deshace (mantenimiento, cerrar la noche). */
export function DialogoConfirmar({
  abierto,
  titulo,
  texto,
  confirmar,
  ocupado,
  onConfirmar,
  onCerrar,
}: {
  abierto: boolean;
  titulo: string;
  texto: string;
  confirmar: string;
  ocupado: boolean;
  onConfirmar: () => void;
  onCerrar: () => void;
}) {
  return (
    <Dialogo abierto={abierto} titulo={titulo} onCerrar={onCerrar}>
      <p className="dialogo__texto">{texto}</p>
      <div className="dialogo__acciones">
        <Tecla type="button" onClick={onCerrar} autoFocus>
          {P.cancelar}
        </Tecla>
        <Tecla type="button" variante="cereza" ocupada={ocupado} onClick={onConfirmar}>
          {confirmar}
        </Tecla>
      </div>
    </Dialogo>
  );
}
