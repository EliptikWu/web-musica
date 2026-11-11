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

/** Días que abren (1–7), según las franjas activas. */
export function diasQueAbren(franjas: readonly FranjaHorario[]): Set<number> {
  return new Set(franjas.filter((f) => f.activo && duracionFranja(f) > 0).map((f) => f.dia_semana));
}
