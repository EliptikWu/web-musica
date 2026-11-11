import {
  Arco, Avance, DiasSemana, Fichas, Icono, Insignia, LuzEstado, MiniAvance, NOMBRES_ICONO, TarjetaTitulo, Tecla, TeclaFija, Ventana,
} from '@rockola/ui';
import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import type { ReactNode } from 'react';
import { MODO } from '@/lib/modo';

export const metadata: Metadata = { title: 'Componentes', robots: { index: false } };

/** Galería del sistema de diseño (doc 05). Solo existe en modo simulado. */
export default function Muestras() {
  if (MODO === 'real') notFound();
  return (
    <main className="madera px-4 pt-[22px] pb-16">
      <div className="mx-auto max-w-[44rem]">
        <h1 className="placa text-center text-[25px]">Componentes Rockola</h1>
        <p className="secundario mt-2 text-center text-[14px]">Cada pieza de packages/ui en sus estados. Los tokens viven en tokens.css.</p>

        <Muestra titulo="Colores">
          <ul className="grid grid-cols-3 gap-3 sm:grid-cols-6">
            {[
              ['nogal', 'bg-nogal'], ['madera', 'bg-madera'], ['madera-clara', 'bg-madera-clara'], ['crema', 'bg-crema'], ['papel-apagado', 'bg-papel-apagado'],
              ['cereza', 'bg-cereza'], ['cereza-hondo', 'bg-cereza-hondo'], ['ámbar', 'bg-ambar'], ['ámbar-claro', 'bg-ambar-claro'], ['tinta', 'bg-tinta'],
              ['tinta-2', 'bg-tinta-2'], ['gris-apagado', 'bg-gris-apagado'],
            ].map(([nombre, clase]) => (
              <li key={nombre} className="text-[12px]">
                <span className={`block h-12 rounded-tarjeta ${clase} shadow-[inset_0_0_0_1px_var(--borde)]`} />
                <span className="mt-1 block">{nombre}</span>
              </li>
            ))}
          </ul>
        </Muestra>

        <Muestra titulo="Tipografía">
          <p className="placa text-[25px]">Placa, 125 %</p>
          <p className="titulo-seccion mt-2">Título de sección, 112 %</p>
          <p className="condensada mt-2 text-[40px] leading-none">Rebelión · DtMF · NUEVAYoL</p>
          <p className="mt-2 text-[15px]">Texto corrido al 100 %: sin registro ni descargas.</p>
        </Muestra>

        <Muestra titulo="Teclas">
          <div className="flex flex-wrap items-center gap-4">
            <Tecla aria-label="Votar por NUEVAYoL">+1</Tecla>
            <TeclaFija variante="hundida">Votaste</TeclaFija>
            <TeclaFija variante="tuya">Tuya</TeclaFija>
            <Tecla variante="cereza">Pedir</Tecla>
            <Tecla variante="cereza" ocupada>
              Pedir
            </Tecla>
            <Tecla tamano="chica">Bloquear</Tecla>
          </div>
          <Tecla variante="cereza" tamano="grande" className="mt-5">
            Entrar y votar
          </Tecla>
        </Muestra>

        <Muestra titulo="Tarjetas de título">
          <ul className="filas">
            <li className="fila">
              <TarjetaTitulo puesto={2} titulo="NUEVAYoL" artista="Bad Bunny" votos={14} />
              <Tecla aria-label="Votar por NUEVAYoL">+1</Tecla>
            </li>
            <li className="fila">
              <TarjetaTitulo puesto={3} titulo="Monaco" artista="Bad Bunny" votos={15} variante="tuya" />
              <TeclaFija variante="tuya">Tuya</TeclaFija>
            </li>
            <li className="fila">
              <TarjetaTitulo titulo="Tití Me Preguntó" artista="Bad Bunny" duracion="4:03" />
              <Tecla variante="cereza">Pedir</Tecla>
            </li>
            <li className="fila">
              <TarjetaTitulo titulo="Mix Bad Bunny 2025" artista="Grandes éxitos" duracion="1:12:40" variante="apagada" />
              <p className="motivo">Dura más de 10 min</p>
            </li>
          </ul>
        </Muestra>

        <Muestra titulo="Arco de luz">
          <div className="grid gap-6 sm:grid-cols-2">
            <Arco aria-label="Suena ahora">
              <div className="h-[292px] px-5 pt-24 pb-5">
                <Ventana className="px-4 pt-3 pb-[14px]">
                  <p className="ventana__etiqueta">Suena ahora</p>
                  <p className="condensada mt-1 text-[40px] leading-none">Rebelión</p>
                  <p className="mt-1 text-[16px]">Joe Arroyo</p>
                  <Insignia className="mt-2">24 votos</Insignia>
                  <Avance valor={161} maximo={358} textoInicio="2:41" textoFin="5:58" className="mt-[10px] text-tinta-2" />
                </Ventana>
              </div>
            </Arco>
            <Arco apagado aria-label="Pedidos cerrados">
              <div className="h-[292px] px-[18px] pt-[104px] pb-[18px]">
                <Ventana className="px-4 pt-4 pb-[18px]">
                  <p className="condensada text-[30px] leading-[.98]">Por ahora no recibimos canciones</p>
                </Ventana>
              </div>
            </Arco>
          </div>
        </Muestra>

        <Muestra titulo="Estado, fichas y avance">
          <div className="flex flex-wrap items-center gap-6">
            <LuzEstado encendida>En vivo</LuzEstado>
            <LuzEstado encendida={false}>Pedidos cerrados</LuzEstado>
            <p className="flex items-center gap-[10px] text-[14px]">
              <Fichas libres={2} total={3} />
              <span className="secundario">Te quedan 2 de 3 pedidos</span>
            </p>
            <Insignia>
              <Icono nombre="subir" />
              Subió 2 puestos
            </Insignia>
          </div>
          <div className="mt-5 max-w-[22rem]">
            <MiniAvance fraccion={0.45} />
          </div>
          <DiasSemana
            className="mt-5"
            dias={[
              { corto: 'Lun', largo: 'lunes', abre: false }, { corto: 'Mar', largo: 'martes', abre: false }, { corto: 'Mié', largo: 'miércoles', abre: false },
              { corto: 'Jue', largo: 'jueves', abre: true, hoy: true }, { corto: 'Vie', largo: 'viernes', abre: true }, { corto: 'Sáb', largo: 'sábado', abre: true },
              { corto: 'Dom', largo: 'domingo', abre: false },
            ]}
          />
        </Muestra>

        <Muestra titulo="Avisos">
          <div className="aviso" role="status">
            <div className="aviso__texto">
              <p className="aviso__titulo">Monaco entró a la cola</p>
              <p className="aviso__meta">Puesto #4, suena en unos 14 min</p>
            </div>
            <a href="#avisos">Ver cola</a>
          </div>
          <div className="aviso aviso--error mt-3" role="alert">
            <div className="aviso__texto">
              <p className="aviso__titulo">No pudimos registrar tu voto</p>
              <p className="aviso__meta">Revisa tu conexión e inténtalo de nuevo.</p>
            </div>
          </div>
          <div className="aviso aviso--info mt-3">
            <div className="aviso__texto">
              <p className="aviso__meta">Resultados guardados: pueden estar incompletos.</p>
            </div>
          </div>
          <p className="vacio mt-3">
            <b>La cola está vacía.</b> Mientras tanto suena la lista de la casa.
          </p>
          <div className="mt-3 grid gap-2">
            <div className="esqueleto h-14" />
            <div className="esqueleto h-14" />
          </div>
        </Muestra>

        <Muestra titulo="Iconos">
          <ul className="grid grid-cols-4 gap-3 sm:grid-cols-8">
            {NOMBRES_ICONO.map((nombre) => (
              <li key={nombre} className="flex flex-col items-center gap-1 text-[11px]">
                <Icono nombre={nombre} className="h-6 w-6" />
                {nombre}
              </li>
            ))}
          </ul>
        </Muestra>
      </div>
    </main>
  );
}

function Muestra({ titulo, children }: { titulo: string; children: ReactNode }) {
  const id = `m-${titulo.toLowerCase().replace(/[^a-z]+/g, '-')}`;
  return (
    <section aria-labelledby={id} className="mt-10" id={titulo === 'Avisos' ? 'avisos' : undefined}>
      <h2 id={id} className="titulo-seccion mb-4 border-b border-borde pb-2">
        {titulo}
      </h2>
      {children}
    </section>
  );
}
