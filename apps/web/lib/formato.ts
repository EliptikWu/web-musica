/**
 * Formatos de la interfaz (doc 05, «Textos»): horas de 12 h como en Colombia, duraciones
 * m:ss y esperas redondeadas hacia abajo. Siempre en la zona horaria del bar.
 */

export const ZONA_POR_DEFECTO = 'America/Bogota';

const formatosHora = new Map<string, Intl.DateTimeFormat>();

/** «8:00 p. m.». El espacio antes de «p. m.» no se parte al final de una línea. */
export function formatoHora(fecha: Date | string | number, zona = ZONA_POR_DEFECTO): string {
  let formato = formatosHora.get(zona);
  if (!formato) {
    formato = new Intl.DateTimeFormat('es-CO', { hour: 'numeric', minute: '2-digit', hour12: true, timeZone: zona });
    formatosHora.set(zona, formato);
  }
  return formato
    .format(new Date(fecha))
    .replace(/[  ]/g, ' ')
    .replace(/ ([ap])\. m\./, ' $1. m.');
}

/** «4:27», «1:12:40». */
export function formatoDuracion(segundos: number): string {
  const total = Math.max(0, Math.floor(segundos));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = String(total % 60).padStart(2, '0');
  return h > 0 ? `${h}:${String(m).padStart(2, '0')}:${s}` : `${m}:${s}`;
}

/** Minutos enteros hacia abajo: 14:30 de espera es «unos 14 min», no 15 (doc 05). */
export function minutosEnteros(segundos: number): number {
  return Math.floor(Math.max(0, segundos) / 60);
}

/** «unos 13 min», «en menos de 1 min», «más de 2 h». */
export function formatoEspera(segundos: number): string {
  const minutos = minutosEnteros(segundos);
  if (minutos < 1) return 'en menos de 1 min';
  if (minutos >= 120) return 'en más de 2 h';
  return `en unos ${minutos} min`;
}

/** Forma corta para las listas: «en 3 min» (que es estimada se dice una vez, aparte). */
export function formatoEsperaCorta(segundos: number): string {
  const minutos = minutosEnteros(segundos);
  if (minutos < 1) return 'en menos de 1 min';
  if (minutos >= 120) return 'en más de 2 h';
  return `en ${minutos} min`;
}

/** «9 canciones, unos 36 min». */
export function formatoResumenCola(canciones: number, segundos: number): string {
  if (canciones === 0) return 'Sin canciones en la cola';
  const minutos = minutosEnteros(segundos);
  const nombre = canciones === 1 ? 'canción' : 'canciones';
  return minutos < 1 ? `${canciones} ${nombre}, menos de 1 min` : `${canciones} ${nombre}, unos ${minutos} min`;
}

/** «hace 31 min», «hace un momento». */
export function formatoHace(desde: Date | string, ahora: Date | number): string {
  const minutos = Math.floor((new Date(ahora).getTime() - new Date(desde).getTime()) / 60000);
  if (minutos < 1) return 'hace un momento';
  if (minutos < 60) return `hace ${minutos} min`;
  const horas = Math.floor(minutos / 60);
  return horas === 1 ? 'hace 1 h' : `hace ${horas} h`;
}

/** «31 min» (columna «Pedida hace» del panel). */
export function formatoMinutos(segundos: number): string {
  return `${minutosEnteros(segundos)} min`;
}

export const DIAS = [
  { corto: 'Lun', largo: 'lunes' },
  { corto: 'Mar', largo: 'martes' },
  { corto: 'Mié', largo: 'miércoles' },
  { corto: 'Jue', largo: 'jueves' },
  { corto: 'Vie', largo: 'viernes' },
  { corto: 'Sáb', largo: 'sábado' },
  { corto: 'Dom', largo: 'domingo' },
] as const;

/** 1 = lunes … 7 = domingo. */
export function nombreDia(dia: number): string {
  return DIAS[dia - 1]?.largo ?? '';
}

/** «8:00 p. m.» a partir de «20:00». */
export function formatoHoraDelDia(hhmm: string): string {
  const [h = 0, m = 0] = hhmm.split(':').map(Number);
  const periodo = h < 12 ? 'a. m.' : 'p. m.';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${String(m).padStart(2, '0')} ${periodo}`;
}

/** Primera letra en mayúscula: «jueves» → «Jueves». */
export function capitalizar(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

type FranjaResumible = { dia_semana: number; hora_inicio: string; hora_fin: string; activo: boolean };

/** Días seguidos como rango («jueves a sábado»); sueltos, en lista («lunes y jueves»). */
function nombrarDias(dias: number[]): string {
  const tramos: number[][] = [];
  for (const d of [...dias].sort((a, b) => a - b)) {
    const ultimo = tramos.at(-1);
    if (ultimo && ultimo.at(-1) === d - 1) ultimo.push(d);
    else tramos.push([d]);
  }
  const partes = tramos.map((t) => (t.length >= 3 ? `${nombreDia(t[0]!)} a ${nombreDia(t.at(-1)!)}` : t.map(nombreDia).join(' y ')));
  return partes.length > 1 ? `${partes.slice(0, -1).join(', ')} y ${partes.at(-1)}` : (partes[0] ?? '');
}

/**
 * El horario en una línea, como en el prototipo: «Jueves a sábado» + «8:00 p. m. a 2:00 a. m.».
 * Si los días tienen horas distintas, una línea por grupo de horas.
 */
export function resumenHorario(franjas: readonly FranjaResumible[]): { dias: string; horas: string }[] {
  const porHoras = new Map<string, number[]>();
  for (const f of franjas.filter((x) => x.activo && x.hora_inicio !== x.hora_fin)) {
    const clave = `${f.hora_inicio}-${f.hora_fin}`;
    porHoras.set(clave, [...(porHoras.get(clave) ?? []), f.dia_semana]);
  }
  return [...porHoras.entries()]
    .sort(([, a], [, b]) => Math.min(...a) - Math.min(...b))
    .map(([clave, dias]) => {
      const [inicio = '', fin = ''] = clave.split('-');
      return { dias: capitalizar(nombrarDias(dias)), horas: `${formatoHoraDelDia(inicio)} a ${formatoHoraDelDia(fin)}` };
    });
}

/** Cierra una frase con punto, sin duplicarlo cuando ya termina en «p. m.» o «a. m.». */
export function conPunto(frase: string): string {
  return frase.endsWith('.') ? frase : `${frase}.`;
}
