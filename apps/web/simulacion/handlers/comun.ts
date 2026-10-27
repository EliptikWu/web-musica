import { type CodigoError, endpoints, type NombreEndpoint, type Rol } from '@rockola/contrato';
import { http, HttpResponse } from 'msw';
import { conEstado } from '../almacen';
import type { Personal, Resultado } from '../motor';
import type { EstadoSim } from '../tipos';

export const COOKIE_PANEL = 'rockola_panel';

export type Peticion = {
  estado: EstadoSim;
  ahora: number;
  parametros: Record<string, string>;
  consulta: URLSearchParams;
  cuerpo: unknown;
  dispositivo: string | null;
  personal: (Personal & { rol: Rol }) | null;
};

/** `/bares/{slug}/ahora` → `/api/bares/:slug/ahora` */
function rutaMsw(ruta: string) {
  return `/api${ruta.replace(/\{(\w+)\}/g, ':$1')}`;
}

const NIVEL: Record<Rol, number> = { operador: 1, administrador: 2 };

function responder(r: Resultado<unknown>) {
  if (!r.ok) return HttpResponse.json({ codigo: r.codigo, mensaje: r.mensaje, ...(r.detalle ? { detalle: r.detalle } : {}) }, { status: r.estado });
  if (r.estado === 204) return new HttpResponse(null, { status: 204 });
  return HttpResponse.json(r.cuerpo as Record<string, unknown>, { status: r.estado });
}

/**
 * Manejador de MSW para un endpoint del contrato. Hace lo que hará el backend en la
 * frontera: valida el cuerpo, comprueba el bar, la sesión y el rol, y respeta la
 * `Idempotency-Key`. La respuesta se valida contra el contrato para cazar errores de la
 * propia simulación.
 */
export function manejar<N extends NombreEndpoint>(nombre: N, fn: (p: Peticion) => Resultado<unknown>) {
  const e = endpoints[nombre];
  const metodo = e.metodo.toLowerCase() as 'get' | 'post' | 'put' | 'patch' | 'delete';
  return http[metodo](rutaMsw(e.ruta), async ({ request, params, cookies }) => {
    const url = new URL(request.url);
    let cuerpo: unknown = undefined;
    if ('cuerpo' in e && e.cuerpo) {
      const crudo: unknown = await request.json().catch(() => undefined);
      const validado = e.cuerpo.safeParse(crudo);
      if (!validado.success) {
        return responder({ ok: false, estado: 400, codigo: 'datos_invalidos', mensaje: 'Datos inválidos', detalle: { problemas: validado.error.issues.map((i) => ({ campo: i.path.join('.'), mensaje: i.message })) } });
      }
      cuerpo = validado.data;
    }
    const parametros = Object.fromEntries(Object.entries(params).map(([k, v]) => [k, String(v)]));
    const clave = request.headers.get('Idempotency-Key');
    const dispositivo = request.headers.get('X-Rockola-Dispositivo');
    const token = cookies[COOKIE_PANEL];

    const resultado = await conEstado((estado, ahora) => {
      if (clave && estado.idempotencia[clave]) {
        const guardada = estado.idempotencia[clave]!;
        return guardada.estado >= 400
          ? ({ ok: false, estado: guardada.estado, ...(guardada.cuerpo as { codigo: never; mensaje: string }) } as Resultado<unknown>)
          : ({ ok: true, estado: guardada.estado as 200, cuerpo: guardada.cuerpo } as Resultado<unknown>);
      }
      if ('slug' in parametros && parametros.slug !== estado.bar.slug) {
        return { ok: false, estado: 404, codigo: 'no_encontrado', mensaje: 'Ese bar no existe' } as const;
      }
      const usuario = token ? estado.usuarios.find((u) => u.id === estado.sesiones_panel[token]) : undefined;
      const personal = usuario && usuario.estado === 'activo' ? { id: usuario.id, nombre: usuario.nombre, rol: usuario.rol } : null;
      if ('rol' in e && e.rol) {
        if (!personal) return { ok: false, estado: 401, codigo: 'no_autenticado', mensaje: 'Tu sesión no es válida' } as const;
        if (usuario?.debe_cambiar_clave && nombre !== 'cambiarClave' && nombre !== 'yoPanel') {
          return { ok: false, estado: 403, codigo: 'debe_cambiar_clave', mensaje: 'Primero cambia tu clave' } as const;
        }
        if (NIVEL[personal.rol] < NIVEL[e.rol]) {
          estado.acciones.push({ id: `a-${++estado.secuencia}`, fecha: ahora, usuario: { id: personal.id, nombre: personal.nombre }, tipo: 'intento_no_permitido', objetivo: null, detalle: e.resumen });
          estado.version++;
          return { ok: false, estado: 403, codigo: 'sin_permiso', mensaje: 'Tu rol no permite esta acción' } as const;
        }
      }
      const r = fn({ estado, ahora, parametros, consulta: url.searchParams, cuerpo, dispositivo, personal });
      if (clave && 'idempotente' in e && e.idempotente) {
        estado.idempotencia[clave] = { estado: r.estado, cuerpo: r.ok ? r.cuerpo : { codigo: r.codigo, mensaje: r.mensaje, detalle: r.detalle }, en: Date.now() };
        estado.version++;
      }
      return r;
    });

    if (resultado.ok && resultado.estado !== 204) {
      const valida = e.respuesta.safeParse(resultado.cuerpo);
      if (!valida.success) console.error(`[simulación] «${nombre}» respondió fuera del contrato`, valida.error.issues);
    }
    return responder(resultado);
  });
}

export const ok = <T>(cuerpo: T, estado: 200 | 201 | 204 = 200): Resultado<T> => ({ ok: true, estado, cuerpo });
export const error = (estado: number, codigo: CodigoError, mensaje: string): Resultado<never> => ({ ok: false, estado, codigo, mensaje });
