/**
 * El horario semanal (RF-036, RN-09). Una franja que cruza medianoche (20:00–02:00)
 * pertenece al día en que empieza: el viernes a la 1:30 a. m. sigue abierta la del jueves.
 * Todo se calcula en la zona horaria del bar.
 */

export type FranjaHorario = { dia_semana: number; hora_inicio: string; hora_fin: string; activo: boolean };

const MIN_SEMANA = 7 * 24 * 60;
const DIA_INGLES: Record<string, number> = { Mon: 1, Tue: 2, Wed: 3, Thu: 4, Fri: 5, Sat: 6, Sun: 7 };
const formatos = new Map<string, Intl.DateTimeFormat>();

function aMinutos(hhmm: string): number {
  const [h = 0, m = 0] = hhmm.split(':').map(Number);
  return h * 60 + m;
}

/** Día (1–7) y minuto del día en la zona del bar. */
export function momentoLocal(instante: Date, zona: string): { dia: number; minuto: number } {
  let formato = formatos.get(zona);
  if (!formato) {
    formato = new Intl.DateTimeFormat('en-US', { timeZone: zona, weekday: 'short', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' });
    formatos.set(zona, formato);
  }
  const partes = Object.fromEntries(formato.formatToParts(instante).map((p) => [p.type, p.value]));
  return { dia: DIA_INGLES[partes.weekday ?? 'Mon'] ?? 1, minuto: Number(partes.hour) * 60 + Number(partes.minute) };
}

/** Duración de una franja en minutos; 0 si empieza y termina a la misma hora (no válida). */
export function duracionFranja(f: FranjaHorario): number {
  return (aMinutos(f.hora_fin) - aMinutos(f.hora_inicio) + 1440) % 1440;
}

function inicioEnSemana(f: FranjaHorario): number {
  return (f.dia_semana - 1) * 1440 + aMinutos(f.hora_inicio);
}

function minutoDeSemana(instante: Date, zona: string): number {
  const { dia, minuto } = momentoLocal(instante, zona);
  return (dia - 1) * 1440 + minuto;
}

/** La franja activa que contiene el instante, si hay una. */
export function franjaVigente(franjas: readonly FranjaHorario[], instante: Date, zona: string): FranjaHorario | null {
  const m = minutoDeSemana(instante, zona);
  return (
    franjas.find((f) => f.activo && duracionFranja(f) > 0 && (m - inicioEnSemana(f) + MIN_SEMANA) % MIN_SEMANA < duracionFranja(f)) ?? null
  );
}

export function abiertoPorHorario(franjas: readonly FranjaHorario[], instante: Date, zona: string): boolean {
  return franjaVigente(franjas, instante, zona) !== null;
}

/** Cuándo termina la franja vigente. */
export function cierreDeFranja(franjas: readonly FranjaHorario[], instante: Date, zona: string): Date | null {
  const f = franjaVigente(franjas, instante, zona);
  if (!f) return null;
  const m = minutoDeSemana(instante, zona);
  const dentro = (m - inicioEnSemana(f) + MIN_SEMANA) % MIN_SEMANA;
  return redondearAlMinuto(new Date(instante.getTime() + (duracionFranja(f) - dentro) * 60000));
}

/** Cuándo abre la próxima franja (si ahora está cerrado). */
export function proximaApertura(franjas: readonly FranjaHorario[], instante: Date, zona: string): Date | null {
  const activas = franjas.filter((f) => f.activo && duracionFranja(f) > 0);
  if (activas.length === 0) return null;
  const m = minutoDeSemana(instante, zona);
  const faltan = Math.min(...activas.map((f) => (inicioEnSemana(f) - m + MIN_SEMANA) % MIN_SEMANA || MIN_SEMANA));
  return redondearAlMinuto(new Date(instante.getTime() + faltan * 60000));
}

/**
 * El horario que vale de verdad (RF-036, RF-045): las franjas **más** los eventos del calendario.
 * Un evento suma horas y nunca quita; las ventanas que se tocan o se pisan cuentan como una sola.
 * Igual que `HorarioEfectivoService` del backend (antes la simulación ignoraba los eventos y un
 * evento fuera del horario semanal salía «Fuera de horario»; lista del equipo, 2026-11-25).
 */
export type EventoHorario = { inicio: string; fin: string };

function eventosEn(eventos: readonly EventoHorario[], ms: number): EventoHorario[] {
  return eventos.filter((e) => Date.parse(e.inicio) <= ms && ms < Date.parse(e.fin));
}

export function abiertoEfectivo(franjas: readonly FranjaHorario[], eventos: readonly EventoHorario[], instante: Date, zona: string): boolean {
  return abiertoPorHorario(franjas, instante, zona) || eventosEn(eventos, instante.getTime()).length > 0;
}

/** Cuándo cierra la ventana abierta ahora (franjas y eventos unidos), o null si está cerrado. */
export function cierreEfectivo(franjas: readonly FranjaHorario[], eventos: readonly EventoHorario[], instante: Date, zona: string): Date | null {
  if (!abiertoEfectivo(franjas, eventos, instante, zona)) return null;
  let fin = instante.getTime();
  // Cada vuelta salta al final de lo que sigue abierto en ese momento; una semana de ventanas pegadas como mucho.
  for (let vuelta = 0; vuelta < 50; vuelta++) {
    const deFranja = cierreDeFranja(franjas, new Date(fin), zona)?.getTime() ?? fin;
    const siguiente = Math.max(deFranja, ...eventosEn(eventos, fin).map((e) => Date.parse(e.fin)));
    if (siguiente <= fin) break;
    fin = siguiente;
  }
  return new Date(fin);
}

/** Cuándo abre lo próximo, franja o evento (si ahora está cerrado). */
export function proximaAperturaEfectiva(franjas: readonly FranjaHorario[], eventos: readonly EventoHorario[], instante: Date, zona: string): Date | null {
  const candidatas = [proximaApertura(franjas, instante, zona)?.getTime(), ...eventos.map((e) => Date.parse(e.inicio)).filter((ms) => ms > instante.getTime())];
  const validas = candidatas.filter((ms): ms is number => ms !== undefined);
  return validas.length > 0 ? new Date(Math.min(...validas)) : null;
}

function redondearAlMinuto(fecha: Date): Date {
  return new Date(Math.floor(fecha.getTime() / 60000) * 60000);
}

export type ProblemaHorario = { tipo: 'misma_hora' | 'solapada'; indices: number[] };

/** Franjas que empiezan y terminan a la misma hora, o que se pisan entre sí. */
export function validarFranjas(franjas: readonly FranjaHorario[]): ProblemaHorario[] {
  const problemas: ProblemaHorario[] = [];
  franjas.forEach((f, i) => {
    if (duracionFranja(f) === 0) problemas.push({ tipo: 'misma_hora', indices: [i] });
  });
  for (let i = 0; i < franjas.length; i++) {
    for (let j = i + 1; j < franjas.length; j++) {
      const a = franjas[i]!;
      const b = franjas[j]!;
      if (!a.activo || !b.activo || duracionFranja(a) === 0 || duracionFranja(b) === 0) continue;
      const desdeA = (inicioEnSemana(b) - inicioEnSemana(a) + MIN_SEMANA) % MIN_SEMANA;
      const desdeB = (inicioEnSemana(a) - inicioEnSemana(b) + MIN_SEMANA) % MIN_SEMANA;
      if (desdeA < duracionFranja(a) || desdeB < duracionFranja(b)) problemas.push({ tipo: 'solapada', indices: [i, j] });
    }
  }
  return problemas;
}

/**
 * El día (1–7) de la noche que corre: el de la franja abierta, que pertenece al día en que empieza
 * (el sábado a la 1:00 a. m. todavía es la noche del viernes); sin franja abierta, el del calendario.
 */
export function diaDeLaNoche(franjas: readonly FranjaHorario[], ahoraMs: number, zona: string): number {
  const instante = new Date(ahoraMs);
  return franjaVigente(franjas, instante, zona)?.dia_semana ?? momentoLocal(instante, zona).dia;
}

/** Días que abren (1–7), según las franjas activas. */
export function diasQueAbren(franjas: readonly FranjaHorario[]): Set<number> {
  return new Set(franjas.filter((f) => f.activo && duracionFranja(f) > 0).map((f) => f.dia_semana));
}

/** Cuánto antes del cierre por horario se avisa en el teléfono y el panel (*propuesto*). */
export const AVISO_CIERRE_MIN = 15;

/**
 * Minutos que faltan para que el horario cierre las solicitudes, si son [AVISO_CIERRE_MIN] o
 * menos (punto 6 del equipo); null si falta más, ya pasó o no hay cierre. `hasta` es el
 * `servicio.hasta` de la foto. Se redondea hacia arriba: a 30 s del cierre dice «1 min».
 */
export function minutosParaCerrar(hasta: string | null, ahoraMs: number): number | null {
  if (!hasta || !ahoraMs) return null;
  const faltan = Date.parse(hasta) - ahoraMs;
  if (!(faltan > 0) || faltan > AVISO_CIERRE_MIN * 60_000) return null;
  return Math.ceil(faltan / 60_000);
}
