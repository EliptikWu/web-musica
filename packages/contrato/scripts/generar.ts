/**
 * Genera lo que lee el equipo de backend (doc 04):
 *   generado/openapi.json  — OpenAPI 3.0 con todos los endpoints y esquemas
 *   generado/endpoints.md  — la misma lista, legible
 * Uso: npm run generar -w @rockola/contrato. El chequeo falla si lo generado no está al día.
 */
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { z } from 'zod';
import { ErrorApi } from '../src/comunes.ts';
import { type Endpoint, endpoints } from '../src/endpoints.ts';
import '../src/index.ts';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..');
const salida = join(raiz, 'generado');

const ESTADO_DE: Record<string, number> = {
  datos_invalidos: 400, no_autenticado: 401, credenciales_invalidas: 401, sin_permiso: 403, debe_cambiar_clave: 403,
  no_encontrado: 404, limite_pedidos: 409, ya_votaste: 409, conflicto: 409, qr_reemplazado: 410, no_pendiente: 410,
  bloqueada: 422, no_disponible: 422, no_embebible: 422, muy_larga: 422, reproducida_hace_poco: 422,
  servicio_no_disponible: 423, cuenta_bloqueada: 423, youtube_no_disponible: 503, cuota_agotada: 503, error_interno: 500,
};

const nombrePascal = (nombre: string) => nombre.charAt(0).toUpperCase() + nombre.slice(1);

/** Todo esquema de un endpoint queda registrado con un id, para referenciarlo con $ref. */
function idDe(esquema: z.ZodType, sugerido: string): string {
  const existente = z.globalRegistry.get(esquema)?.id;
  if (typeof existente === 'string') return existente;
  z.globalRegistry.add(esquema, { id: sugerido });
  return sugerido;
}

const lista = Object.entries(endpoints) as [string, Endpoint][];
const ids = new Map<string, { cuerpo?: string; respuesta: string }>();
for (const [nombre, e] of lista) {
  ids.set(nombre, {
    cuerpo: e.cuerpo ? idDe(e.cuerpo, `Cuerpo${nombrePascal(nombre)}`) : undefined,
    respuesta: idDe(e.respuesta, `Respuesta${nombrePascal(nombre)}`),
  });
}

const { schemas } = z.toJSONSchema(z.globalRegistry, {
  target: 'openapi-3.0',
  uri: (id) => `#/components/schemas/${id}`,
  unrepresentable: 'any',
  // Para el backend: puede añadir campos (el cliente ignora los que no conoce) y los enteros
  // no tienen el máximo artificial de JavaScript.
  override: ({ jsonSchema }) => {
    const s = jsonSchema as Record<string, unknown>;
    if (s.additionalProperties === false) delete s.additionalProperties;
    if (s.maximum === Number.MAX_SAFE_INTEGER) delete s.maximum;
    if (s.minimum === Number.MIN_SAFE_INTEGER) delete s.minimum;
  },
});
const componentes = Object.fromEntries(
  Object.entries(schemas)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([id, s]) => {
      const { $schema: _omitido, $id: _ref, id: _id, ...resto } = s as Record<string, unknown>;
      return [id, resto];
    }),
);

const ref = (id: string) => ({ $ref: `#/components/schemas/${id}` });
const paths: Record<string, Record<string, unknown>> = {};
for (const [nombre, e] of lista) {
  const { cuerpo, respuesta } = ids.get(nombre)!;
  const parametrosRuta = [...e.ruta.matchAll(/\{(\w+)\}/g)].map((m) => ({ name: m[1], in: 'path', required: true, schema: { type: 'string' } }));
  const parametrosConsulta = e.consulta
    ? Object.entries(e.consulta.shape).map(([clave, esquema]) => ({
        name: clave,
        in: 'query',
        required: !(esquema as z.ZodType).safeParse(undefined).success,
        schema: { type: 'string' },
      }))
    : [];
  const cabeceras = [
    ...(e.superficie === 'cliente' ? [{ name: 'X-Rockola-Dispositivo', in: 'header', required: true, schema: { type: 'string', format: 'uuid' }, description: 'UUID del teléfono; el servidor guarda solo su SHA-256' }] : []),
    ...(e.idempotente ? [{ name: 'Idempotency-Key', in: 'header', required: true, schema: { type: 'string', format: 'uuid' } }] : []),
  ];
  const errores = Object.fromEntries(
    [...new Set(e.errores.map((c) => ESTADO_DE[c] ?? 500))].map((estado) => [
      String(estado),
      { description: e.errores.filter((c) => ESTADO_DE[c] === estado).join(', '), content: { 'application/json': { schema: ref('ErrorApi') } } },
    ]),
  );
  paths[e.ruta] ??= {};
  paths[e.ruta]![e.metodo.toLowerCase()] = {
    operationId: nombre,
    summary: e.resumen,
    tags: [e.superficie],
    description: `Requisitos: ${e.rf.join(', ')}.${e.rol ? ` Rol mínimo: ${e.rol}.` : ''}`,
    ...(e.rol ? { security: [{ sesionPanel: [] }] } : {}),
    parameters: [...parametrosRuta, ...parametrosConsulta, ...cabeceras],
    ...(cuerpo ? { requestBody: { required: true, content: { 'application/json': { schema: ref(cuerpo) } } } } : {}),
    responses: {
      [String(e.estado)]: e.estado === 204 ? { description: 'Sin contenido' } : { description: 'Correcto', content: { 'application/json': { schema: ref(respuesta) } } },
      ...errores,
    },
  };
}

const openapi = {
  openapi: '3.0.3',
  info: {
    title: 'Rockola — contrato propuesto por el frontend',
    version: '0.1.0',
    description: 'Propuesta para acordar con el equipo de backend (docs/04-modelo-de-datos-y-contrato.md). Generado desde packages/contrato; no se edita a mano.',
  },
  servers: [{ url: '/api' }],
  components: {
    securitySchemes: { sesionPanel: { type: 'apiKey', in: 'cookie', name: 'rockola_panel', description: 'Cookie httpOnly que pone el backend al entrar' } },
    schemas: componentes,
  },
  paths,
};

const md: string[] = [
  '# Endpoints del contrato',
  '',
  '> Generado desde `packages/contrato/src/endpoints.ts` con `npm run generar -w @rockola/contrato`. No se edita a mano.',
  '> Esquemas completos en `openapi.json`. Errores: `{ codigo, mensaje, detalle? }` (`ErrorApi`).',
  '',
];
for (const superficie of ['cliente', 'pantalla', 'panel'] as const) {
  md.push(`## ${nombrePascal(superficie)}`, '', '| Operación | Método y ruta | Cuerpo → respuesta | Rol | Errores | RF |', '|---|---|---|---|---|---|');
  for (const [nombre, e] of lista.filter(([, x]) => x.superficie === superficie)) {
    const { cuerpo, respuesta } = ids.get(nombre)!;
    const consulta = e.consulta ? `?${Object.keys(e.consulta.shape).join('&')}` : '';
    md.push(
      `| \`${nombre}\`${e.idempotente ? ' (idempotente)' : ''} | \`${e.metodo} ${e.ruta}${consulta}\` | ${cuerpo ? `\`${cuerpo}\` → ` : ''}${e.estado === 204 ? '204' : `\`${respuesta}\``} | ${e.rol ?? 'público'} | ${e.errores.map((c) => `\`${c}\``).join(', ') || '—'} | ${e.rf.join(', ')} |`,
    );
  }
  md.push('');
}
md.push('## Tiempo real', '', '`GET /bares/{slug}/eventos` (SSE). Cada mensaje: `event: <tipo>`, `data: <EventoTiempoReal en JSON>`, `id: <version>`.', '', 'Al abrir la conexión, el servidor manda primero una foto `ahora`; después, una cada vez que algo cambia y un `ping` cada 15 s.', '');

// ErrorApi se referencia aunque ningún endpoint lo nombre directamente.
void ErrorApi;
mkdirSync(salida, { recursive: true });
writeFileSync(join(salida, 'openapi.json'), `${JSON.stringify(openapi, null, 2)}\n`);
writeFileSync(join(salida, 'endpoints.md'), `${md.join('\n')}`);
console.log(`generado/openapi.json: ${Object.keys(componentes).length} esquemas, ${lista.length} operaciones`);
