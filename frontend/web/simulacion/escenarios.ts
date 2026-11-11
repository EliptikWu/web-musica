/**
 * Los escenarios de la simulación (doc 03). Viven aparte de la semilla para que la portada
 * los liste sin cargar los datos de ejemplo.
 */
export const ESCENARIOS = [
  { id: 'momento-b', nombre: 'Noche en curso (10:45 p. m.)' },
  { id: 'momento-a', nombre: 'Antes de pedir Monaco (10:24 p. m.)' },
  { id: 'fuera-de-horario', nombre: 'Fuera de horario' },
  { id: 'en-pausa', nombre: 'Solicitudes en pausa' },
  { id: 'mantenimiento', nombre: 'Mantenimiento' },
  { id: 'cola-vacia', nombre: 'Cola vacía' },
  { id: 'sin-pedidos', nombre: 'Sin pedidos libres' },
  { id: 'sin-lista-activa', nombre: 'Sin lista de la casa' },
  { id: 'youtube-caido', nombre: 'YouTube no responde' },
  { id: 'cuota-agotada', nombre: 'Cuota de YouTube agotada' },
  { id: 'desconexion', nombre: 'Sin conexión en vivo' },
] as const;

export type Escenario = (typeof ESCENARIOS)[number]['id'];

export function esEscenario(valor: string | null | undefined): valor is Escenario {
  return ESCENARIOS.some((e) => e.id === valor);
}
