/** Claves de TanStack Query: un solo lugar para que invalidar no dependa de escribirlas igual. */
export const claves = {
  bar: (slug: string) => ['bar', slug] as const,
  ahora: (slug: string) => ['ahora', slug] as const,
  destacadas: (slug: string) => ['destacadas', slug] as const,
  yo: (slug: string) => ['yo', slug] as const,
  busqueda: (slug: string, q: string) => ['busqueda', slug, q] as const,
  qrPublico: (slug: string) => ['qr-publico', slug] as const,
  panel: {
    todo: ['panel'] as const,
    yo: ['panel', 'yo'] as const,
    noche: ['panel', 'noche'] as const,
    bloqueos: (vigencia: string) => ['panel', 'bloqueos', vigencia] as const,
    horario: ['panel', 'horario'] as const,
    listas: ['panel', 'listas'] as const,
    lista: (id: string) => ['panel', 'listas', id] as const,
    usuarios: ['panel', 'usuarios'] as const,
    establecimiento: ['panel', 'establecimiento'] as const,
    qr: ['panel', 'establecimiento', 'qr'] as const,
    bitacora: ['panel', 'bitacora'] as const,
    eventos: ['panel', 'eventos'] as const,
    busqueda: (q: string) => ['panel', 'busqueda', q] as const,
  },
};
