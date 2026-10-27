import { CuerpoEntrar } from '@rockola/contrato';
import { http, HttpResponse } from 'msw';
import { conEstado } from '../almacen';
import {
  abrirSesionManual, activarMantenimiento, bloquear, bloqueoAContrato, cambiarModoQr, cerrarSesionManual, construirNoche, desbloquear,
  extenderBloqueo, pausarReproduccion, retirar, saltar,
} from '../motor';
import type { EstadoSim, UsuarioSim } from '../tipos';
import { COOKIE_PANEL, error, manejar, ok, type Peticion } from './comun';

/** El panel de la noche (doc 04, «Panel»). La gestión (usuarios, listas…) llega en la iteración 4. */

const INTENTOS_MAXIMOS = 5; // (propuesto)
const BLOQUEO_CUENTA_MS = 5 * 60_000; // (propuesto)

function usuarioAContrato(u: UsuarioSim) {
  return {
    id: u.id,
    nombre: u.nombre,
    correo: u.correo,
    rol: u.rol,
    estado: u.estado,
    debe_cambiar_clave: u.debe_cambiar_clave,
    ultimo_acceso: u.ultimo_acceso === null ? null : new Date(u.ultimo_acceso).toISOString(),
  };
}

function yoPanel(estado: EstadoSim, u: UsuarioSim) {
  return { usuario: usuarioAContrato(u), establecimiento: { slug: estado.bar.slug, nombre: estado.bar.nombre } };
}

function quien(p: Peticion) {
  return { id: p.personal!.id, nombre: p.personal!.nombre };
}

function ponerCookie(valor: string | null) {
  // En el navegador MSW no puede poner cookies httpOnly: se pone una normal (doc 03).
  document.cookie = valor ? `${COOKIE_PANEL}=${valor}; Path=/; SameSite=Lax` : `${COOKIE_PANEL}=; Path=/; Max-Age=0; SameSite=Lax`;
}

const entrar = http.post('/api/panel/sesion', async ({ request }) => {
  const datos = CuerpoEntrar.safeParse(await request.json().catch(() => undefined));
  if (!datos.success) return HttpResponse.json({ codigo: 'datos_invalidos', mensaje: 'Escribe tu correo y tu clave' }, { status: 400 });
  const r = await conEstado((estado, ahora) => {
    const u = estado.usuarios.find((x) => x.correo.toLowerCase() === datos.data.correo.toLowerCase());
    if (u?.bloqueado_hasta && u.bloqueado_hasta > ahora) {
      return { estado: 423, cuerpo: { codigo: 'cuenta_bloqueada', mensaje: 'Demasiados intentos', detalle: { reintentar_en: new Date(u.bloqueado_hasta).toISOString() } } };
    }
    if (!u || u.estado !== 'activo' || u.clave !== datos.data.clave) {
      if (u) {
        u.intentos++;
        if (u.intentos >= INTENTOS_MAXIMOS) {
          u.bloqueado_hasta = ahora + BLOQUEO_CUENTA_MS;
          u.intentos = 0;
        }
        estado.version++;
      }
      // Mensaje genérico: no dice si el correo existe (RF-021).
      return { estado: 401, cuerpo: { codigo: 'credenciales_invalidas', mensaje: 'Correo o clave incorrectos' } };
    }
    u.intentos = 0;
    u.bloqueado_hasta = null;
    u.ultimo_acceso = ahora;
    const token = crypto.randomUUID();
    estado.sesiones_panel[token] = u.id;
    estado.version++;
    return { estado: 200, cuerpo: yoPanel(estado, u), token };
  });
  if ('token' in r && r.token) ponerCookie(r.token);
  return HttpResponse.json(r.cuerpo, { status: r.estado });
});

const salir = http.delete('/api/panel/sesion', async ({ cookies }) => {
  const token = cookies[COOKIE_PANEL];
  if (token) {
    await conEstado((estado) => {
      if (estado.sesiones_panel[token]) {
        delete estado.sesiones_panel[token];
        estado.version++;
      }
    });
  }
  ponerCookie(null);
  return new HttpResponse(null, { status: 204 });
});

export const manejadoresPanel = [
  entrar,
  salir,
  manejar('yoPanel', ({ estado, personal }) => ok(yoPanel(estado, estado.usuarios.find((u) => u.id === personal!.id)!))),
  manejar('cambiarClave', ({ estado, personal, cuerpo }) => {
    const u = estado.usuarios.find((x) => x.id === personal!.id)!;
    const { actual, nueva } = cuerpo as { actual: string; nueva: string };
    if (u.clave !== actual) return error(401, 'credenciales_invalidas', 'La clave actual no coincide');
    if (nueva === actual) return error(400, 'datos_invalidos', 'La clave nueva tiene que ser distinta');
    u.clave = nueva;
    u.debe_cambiar_clave = false;
    estado.version++;
    return ok(null, 204);
  }),
  manejar('noche', ({ estado, ahora }) => ok(construirNoche(estado, ahora))),
  manejar('abrirSesion', (p) => abrirSesionManual(p.estado, (p.cuerpo as { tipo_evento: string }).tipo_evento, quien(p), p.ahora)),
  manejar('cerrarSesion', (p) => cerrarSesionManual(p.estado, quien(p), p.ahora)),
  manejar('cambiarServicio', (p) => {
    const modo = (p.cuerpo as { modo_qr: EstadoSim['modo_qr'] }).modo_qr;
    // Forzar la apertura y salir del mantenimiento son del Administrador (D9).
    if (p.personal!.rol !== 'administrador' && (modo === 'forzado_activo' || p.estado.mantenimiento)) {
      return error(403, 'sin_permiso', 'Tu rol no permite esta acción');
    }
    return cambiarModoQr(p.estado, modo, quien(p), p.ahora);
  }),
  manejar('mantenimiento', (p) => activarMantenimiento(p.estado, quien(p), p.ahora)),
  manejar('saltar', (p) => saltar(p.estado, p.parametros.id ?? '', quien(p), p.ahora)),
  manejar('pausarReproduccion', (p) => pausarReproduccion(p.estado, p.parametros.id ?? '', true, quien(p), p.ahora)),
  manejar('reanudarReproduccion', (p) => pausarReproduccion(p.estado, p.parametros.id ?? '', false, quien(p), p.ahora)),
  manejar('retirar', (p) => {
    const { motivo, detalle } = p.cuerpo as { motivo: string; detalle: string | null };
    const textos: Record<string, string> = { inapropiada: 'Inapropiada', duplicada: 'Duplicada', otro: 'Otro motivo' };
    return retirar(p.estado, p.parametros.id ?? '', detalle ? `${textos[motivo] ?? motivo}: ${detalle}` : (textos[motivo] ?? motivo), quien(p), p.ahora);
  }),
  manejar('bloqueos', ({ estado, ahora, consulta }) => {
    const vigencia = consulta.get('vigencia') ?? 'vigentes';
    const todos = estado.bloqueos.map((b) => bloqueoAContrato(estado, b, ahora)).sort((a, b) => b.inicio.localeCompare(a.inicio));
    return ok({ bloqueos: vigencia === 'todos' ? todos : todos.filter((b) => b.vigente === (vigencia === 'vigentes')) });
  }),
  manejar('bloquear', (p) => {
    const datos = p.cuerpo as Parameters<typeof bloquear>[1];
    // El Operador solo bloquea hasta el fin de la noche (D9).
    if (p.personal!.rol !== 'administrador' && datos.duracion !== 'fin_sesion') return error(403, 'sin_permiso', 'Tu rol solo puede bloquear hasta el fin de la noche');
    const r = bloquear(p.estado, datos, quien(p), p.ahora);
    return r.ok ? ok(bloqueoAContrato(p.estado, r.cuerpo, p.ahora), 201) : r;
  }),
  manejar('extenderBloqueo', (p) => {
    const { duracion, horas } = p.cuerpo as { duracion: 'fin_sesion' | 'horas' | 'indefinido'; horas: number | null };
    const r = extenderBloqueo(p.estado, p.parametros.id ?? '', duracion, horas, quien(p), p.ahora);
    return r.ok ? ok(bloqueoAContrato(p.estado, r.cuerpo, p.ahora)) : r;
  }),
  manejar('desbloquear', (p) => desbloquear(p.estado, p.parametros.id ?? '', quien(p), p.ahora)),
];
