'use client';

import type { Establecimiento as DatosBar } from '@rockola/contrato';
import { Campo, claseTecla, Icono, LuzEstado, Selector, Tecla } from '@rockola/ui';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { QRCodeSVG } from 'qrcode.react';
import { useRef, useState } from 'react';
import { Aviso } from '@/components/comun/Aviso';
import { DialogoConfirmar } from '@/components/panel/Dialogos';
import { useYoDelPanel } from '@/components/panel/MarcoPanel';
import { claves } from '@/lib/api/claves';
import { pedir } from '@/lib/api/cliente';
import { useAccionGestion } from '@/lib/api/hooks-panel';
import { fechaYHora } from '@/lib/formato';
import { Cargando, mensajeDeError, PaginaGestion } from './PaginaGestion';

// Nombres que el personal reconoce; el valor es el identificador IANA del contrato.
const ZONAS: [string, string][] = [
  ['America/Bogota', 'Colombia (Bogotá)'],
  ['America/Lima', 'Perú (Lima)'],
  ['America/Mexico_City', 'México (Ciudad de México)'],
  ['America/Caracas', 'Venezuela (Caracas)'],
  ['America/Santiago', 'Chile (Santiago)'],
  ['America/Argentina/Buenos_Aires', 'Argentina (Buenos Aires)'],
];

/** Lee un color de tokens.css (los atributos del SVG del QR no resuelven `var()`). */
export function colorDeToken(nombre: string, respaldo: string): string {
  if (typeof window === 'undefined') return respaldo;
  return getComputedStyle(document.documentElement).getPropertyValue(nombre).trim() || respaldo;
}

/**
 * El bar y su QR (RF-034, RF-035; PB-01, PB-02, PB-26): datos y límites del servicio, el QR
 * para imprimir o descargar, y regenerarlo (el anterior deja de funcionar).
 */
export function Establecimiento() {
  const bar = useQuery({ queryKey: claves.panel.establecimiento, queryFn: ({ signal }) => pedir('establecimiento', { signal }) });
  const qr = useQuery({ queryKey: claves.panel.qr, queryFn: ({ signal }) => pedir('qrEstablecimiento', { signal }) });
  // El aviso y la acción viven aquí: al guardar, el formulario se vuelve a montar con los datos
  // nuevos, y React Query no avisa del resultado a un componente que ya no está.
  const [aviso, setAviso] = useState<Mensaje | null>(null);
  // El nombre y la zona del bar salen también en la cabecera y en las fechas: `yo` se recarga.
  const guardar = useAccionGestion('editarEstablecimiento', [claves.panel.establecimiento, claves.panel.yo, ['bar']]);
  return (
    <PaginaGestion titulo="Bar y QR" nota="Los datos del establecimiento, las reglas de los pedidos y el código QR de las mesas.">
      <div className="gestion-rejilla gestion-rejilla--dos">
        <Cargando consulta={bar}>{() => <FormularioBar key={JSON.stringify(bar.data)} bar={bar.data!} guardar={guardar} aviso={aviso} setAviso={setAviso} />}</Cargando>
        <Cargando consulta={qr}>{() => <PlacaQrGestion url={qr.data!.url} generado={qr.data!.generado_en} />}</Cargando>
      </div>
    </PaginaGestion>
  );
}

type Mensaje = { tipo: 'luz' | 'error'; texto: string };

function FormularioBar({
  bar,
  guardar,
  aviso,
  setAviso,
}: {
  bar: DatosBar;
  guardar: ReturnType<typeof useAccionGestion<'editarEstablecimiento'>>;
  aviso: Mensaje | null;
  setAviso: (a: Mensaje | null) => void;
}) {
  const [datos, setDatos] = useState({
    nombre: bar.nombre,
    direccion: bar.direccion,
    zona_horaria: bar.zona_horaria,
    pedidos_maximos: String(bar.pedidos_maximos),
    duracion_max_min: String(Math.round(bar.duracion_max_seg / 60)),
    ventana_no_repeticion_min: String(bar.ventana_no_repeticion_min),
  });
  const entero = (v: string, min: number, max: number) => Number.isInteger(Number(v)) && Number(v) >= min && Number(v) <= max;
  const errores = {
    nombre: datos.nombre.trim() ? null : 'Escribe el nombre del bar.',
    pedidos: entero(datos.pedidos_maximos, 1, 10) ? null : 'De 1 a 10.',
    duracion: entero(datos.duracion_max_min, 1, 60) ? null : 'De 1 a 60 minutos.',
    ventana: entero(datos.ventana_no_repeticion_min, 0, 600) ? null : 'De 0 a 600 minutos.',
  };
  const valido = Object.values(errores).every((e) => e === null);
  const poner = (clave: keyof typeof datos) => (e: { target: { value: string } }) => {
    setAviso(null);
    setDatos((d) => ({ ...d, [clave]: e.target.value }));
  };
  return (
    <form
      className="caja flex flex-col gap-4"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        if (!valido) return;
        guardar.mutate(
          {
            cuerpo: {
              nombre: datos.nombre.trim(),
              direccion: datos.direccion.trim(),
              zona_horaria: datos.zona_horaria,
              pedidos_maximos: Number(datos.pedidos_maximos),
              duracion_max_seg: Number(datos.duracion_max_min) * 60,
              ventana_no_repeticion_min: Number(datos.ventana_no_repeticion_min),
            },
          },
          { onSuccess: () => setAviso({ tipo: 'luz', texto: 'Datos del bar guardados.' }), onError: (err) => setAviso({ tipo: 'error', texto: mensajeDeError(err) }) },
        );
      }}
    >
      <div className="caja__cabeza mb-0!">
        <h2>Datos del bar</h2>
        <LuzEstado encendida={bar.estado === 'activo'}>{bar.estado === 'activo' ? 'Activo' : 'Inactivo'}</LuzEstado>
      </div>
      <Campo etiqueta="Nombre" value={datos.nombre} onChange={poner('nombre')} error={errores.nombre} maxLength={80} />
      <Campo etiqueta="Dirección" value={datos.direccion} onChange={poner('direccion')} maxLength={160} />
      <Selector etiqueta="Zona horaria" value={datos.zona_horaria} onChange={poner('zona_horaria')}>
        {ZONAS.map(([valor, texto]) => (
          <option key={valor} value={valor}>
            {texto}
          </option>
        ))}
      </Selector>
      <div className="formulario-fila">
        <Campo etiqueta="Pedidos por teléfono" ayuda="Canciones de un mismo teléfono en la cola a la vez." type="number" min={1} max={10} value={datos.pedidos_maximos} onChange={poner('pedidos_maximos')} error={errores.pedidos} />
        <Campo etiqueta="Duración máxima (min)" ayuda="Las canciones más largas no se pueden pedir." type="number" min={1} max={60} value={datos.duracion_max_min} onChange={poner('duracion_max_min')} error={errores.duracion} />
        <Campo etiqueta="Sin repetir (min)" ayuda="Después de sonar, nadie la puede pedir durante este tiempo." type="number" min={0} max={600} value={datos.ventana_no_repeticion_min} onChange={poner('ventana_no_repeticion_min')} error={errores.ventana} />
      </div>
      {aviso ? <Aviso tipo={aviso.tipo} titulo={aviso.texto} /> : null}
      <Tecla type="submit" variante="cereza" tamano="ancha" className="self-start" ocupada={guardar.isPending}>
        Guardar datos
      </Tecla>
    </form>
  );
}

function PlacaQrGestion({ url, generado }: { url: string; generado: string }) {
  const zona = useYoDelPanel().establecimiento.zona_horaria;
  const regenerar = useAccionGestion('regenerarQr', [claves.panel.qr, ['qr-publico']]);
  const [confirmar, setConfirmar] = useState(false);
  const [aviso, setAviso] = useState<{ tipo: 'luz' | 'error'; texto: string } | null>(null);
  const svg = useRef<HTMLDivElement>(null);

  function descargar() {
    const elemento = svg.current?.querySelector('svg');
    if (!elemento) return;
    // XMLSerializer pone el xmlns que outerHTML omite: sin él, el archivo no abre como imagen.
    const contenido = new XMLSerializer().serializeToString(elemento);
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(new Blob([contenido], { type: 'image/svg+xml' }));
    enlace.download = 'rockola-qr.svg';
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(enlace.href), 1000);
  }

  return (
    <section className="caja flex flex-col gap-4 self-start" aria-labelledby="t-qr">
      <div className="caja__cabeza mb-0!">
        <h2 id="t-qr">Código QR de las mesas</h2>
        <p>
          Generado el {fechaYHora(generado, zona)}
        </p>
      </div>
      <div className="qr-grande">
        <div className="qr-grande__codigo" ref={svg}>
          <QRCodeSVG value={url} size={172} level="M" marginSize={0} fgColor={colorDeToken('--tinta', 'black')} bgColor={colorDeToken('--qr-fondo', 'white')} title="Código QR del bar" />
        </div>
        <div className="min-w-0 flex-1">
          <p className="secundario text-[14px]">Lleva a</p>
          <p className="qr-grande__url">{url.replace(/^https?:\/\//, '')}</p>
        </div>
      </div>
      {aviso ? <Aviso tipo={aviso.tipo} titulo={aviso.texto} /> : null}
      <div className="flex flex-wrap gap-3">
        <Tecla tamano="ancha" onClick={descargar}>
          <Icono nombre="descargar" />
          Descargar
        </Tecla>
        <Link href="/panel/establecimiento/qr/imprimir" className={claseTecla('normal', 'ancha')}>
          <Icono nombre="imprimir" />
          Imprimir
        </Link>
        <Tecla tamano="ancha" onClick={() => setConfirmar(true)}>
          Regenerar
        </Tecla>
      </div>
      <DialogoConfirmar
        abierto={confirmar}
        titulo="¿Regenerar el código QR?"
        texto="El QR que está impreso en las mesas deja de funcionar ahora mismo. Tendrás que imprimir el nuevo."
        confirmar="Regenerar QR"
        ocupado={regenerar.isPending}
        onCerrar={() => setConfirmar(false)}
        onConfirmar={() =>
          regenerar.mutate(
            {},
            {
              onSuccess: () => setAviso({ tipo: 'luz', texto: 'QR nuevo listo. El anterior ya no abre la rockola.' }),
              onError: (e) => setAviso({ tipo: 'error', texto: mensajeDeError(e) }),
              onSettled: () => setConfirmar(false),
            },
          )
        }
      />
    </section>
  );
}

/** La hoja para imprimir y pegar en las mesas (RF-035). */
export function QrParaImprimir() {
  const qr = useQuery({ queryKey: claves.panel.qr, queryFn: ({ signal }) => pedir('qrEstablecimiento', { signal }) });
  const bar = useQuery({ queryKey: claves.panel.establecimiento, queryFn: ({ signal }) => pedir('establecimiento', { signal }) });
  return (
    <div className="mx-auto max-w-[34rem] py-6 text-center">
      <div className="no-imprimir mb-6 flex justify-center gap-3">
        <Link href="/panel/establecimiento" className={claseTecla('normal', 'ancha')}>
          <Icono nombre="atras" />
          Volver
        </Link>
        <Tecla variante="cereza" tamano="ancha" onClick={() => window.print()}>
          <Icono nombre="imprimir" />
          Imprimir
        </Tecla>
      </div>
      {qr.isError || bar.isError ? (
        <Cargando consulta={qr.isError ? qr : bar}>{() => null}</Cargando>
      ) : qr.data && bar.data ? (
        <div className="ventana mx-auto flex flex-col items-center gap-4 px-8 py-8">
          <h1 className="placa text-[34px]">Escanea y vota</h1>
          <p className="text-[18px] text-tinta-2">Pide la música de {bar.data.nombre} desde tu teléfono. Sin descargas ni registro.</p>
          <div className="qr-grande__codigo h-[300px]! w-[300px]!">
            <QRCodeSVG value={qr.data.url} size={276} level="M" marginSize={0} fgColor={colorDeToken('--tinta', 'black')} bgColor={colorDeToken('--qr-fondo', 'white')} title="Código QR del bar" />
          </div>
          <p className="text-[15px] font-semibold">{qr.data.url.replace(/^https?:\/\//, '')}</p>
        </div>
      ) : (
        <p className="secundario" role="status">
          Preparando el QR…
        </p>
      )}
    </div>
  );
}
