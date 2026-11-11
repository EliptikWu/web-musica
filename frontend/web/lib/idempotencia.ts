/** Clave para que un reintento no duplique un voto o una solicitud (RNF-008). */
export function nuevaClave(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  // Fuera de un contexto seguro (http en la red local) no hay randomUUID: UUID v4 a mano.
  const b = crypto.getRandomValues(new Uint8Array(16));
  b[6] = (b[6]! & 0x0f) | 0x40;
  b[8] = (b[8]! & 0x3f) | 0x80;
  return formatoUuid(b);
}

/**
 * Clave fija para una operación que no debe contar dos veces aunque se repita (doble clic,
 * la TV que recarga): el mismo `texto` da siempre el mismo UUID, como pide el contrato.
 * Es un UUID v8 (RFC 9562) con 128 bits de FNV-1a: no es secreto, solo estable. Síncrono a
 * propósito: en http de la red local no hay `crypto.subtle`.
 */
export function claveDe(texto: string): string {
  const b = new Uint8Array(16);
  for (let parte = 0; parte < 4; parte++) {
    let h = 2166136261 ^ Math.imul(parte + 1, 0x9e3779b1);
    for (let i = 0; i < texto.length; i++) h = Math.imul(h ^ texto.charCodeAt(i), 16777619);
    for (let k = 0; k < 4; k++) b[parte * 4 + k] = (h >>> (k * 8)) & 0xff;
  }
  b[6] = (b[6]! & 0x0f) | 0x80;
  b[8] = (b[8]! & 0x3f) | 0x80;
  return formatoUuid(b);
}

function formatoUuid(b: Uint8Array): string {
  const h = [...b].map((x) => x.toString(16).padStart(2, '0')).join('');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}
