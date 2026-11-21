import type { BarPlataforma, CuentaBar } from '@rockola/contrato';
import type { Resultado } from '../motor';
import type { EstadoSim, PlataformaSim } from '../tipos';
import { COOKIE_PLATAFORMA, error, manejar, ok, type Peticion } from './comun';

/** La plataforma en la simulación (RF-064). Cuenta de ejemplo: superadmin@rockola.demo. */

function plataformaDe(estado: EstadoSim): PlataformaSim {
  estado.plataforma ??= {
    superadmin: { id: 'sa-1', nombre: 'Superadmin de prueba', correo: 'superadmin@rockola.demo', clave: 'rockola-demo', debe_cambiar_clave: false },
    sesiones: [],
    bares: [],
  };
  return estado.plataforma;
}

function ponerCookie(valor: string | null) {
  // En el navegador MSW no puede poner cookies httpOnly: se pone una normal (doc 03).
  document.cookie = valor ? `${COOKIE_PLATAFORMA}=${valor}; Path=/; SameSite=Strict` : `${COOKIE_PLATAFORMA}=; Path=/; Max-Age=0; SameSite=Strict`;
}

function yo(p: PlataformaSim) {
  const { id, nombre, correo, debe_cambiar_clave } = p.superadmin;
  return { id, nombre, correo, debe_cambiar_clave };
}

function barDemo(estado: EstadoSim): BarPlataforma {
  return {
    id: 'bar-demo',
    slug: estado.bar.slug,
    nombre: estado.bar.nombre,
    zona_horaria: estado.bar.zona_horaria,
    activo: true,
    cuentas: estado.usuarios.map((u) => ({ id: u.id, nombre: u.nombre, correo: u.correo, rol: u.rol, estado: u.estado, debe_cambiar_clave: u.debe_cambiar_clave })),
  };
}

const conSesion = (p: Peticion, fn: (x: PlataformaSim) => Resultado<unknown>, exigirClaveNueva = true): Resultado<unknown> => {
  const x = plataformaDe(p.estado);
  if (!p.tokenPlataforma || !x.sesiones.includes(p.tokenPlataforma)) return error(401, 'no_autenticado', 'Inicia sesión en la plataforma');
  if (exigirClaveNueva && x.superadmin.debe_cambiar_clave) return error(403, 'debe_cambiar_clave', 'Debes cambiar tu clave antes de seguir');
  return fn(x);
};

function claveDeEjemplo(estado: EstadoSim) {
  return `demo-${(++estado.secuencia).toString().padStart(4, '0')}-clave`;
}

export const manejadoresPlataforma = [
  manejar('entrarPlataforma', (p) => {
    const x = plataformaDe(p.estado);
    const { correo, clave } = p.cuerpo as { correo: string; clave: string };
    if (correo.toLowerCase() !== x.superadmin.correo || clave !== x.superadmin.clave) return error(401, 'credenciales_invalidas', 'Correo o clave incorrectos');
    const token = `plataforma-${++p.estado.secuencia}`;
    x.sesiones.push(token);
    p.estado.version++;
    ponerCookie(token);
    return ok(yo(x));
  }),
  manejar('salirPlataforma', (p) => {
    const x = plataformaDe(p.estado);
    x.sesiones = x.sesiones.filter((s) => s !== p.tokenPlataforma);
    p.estado.version++;
    ponerCookie(null);
    return ok(null, 204);
  }),
  manejar('yoPlataforma', (p) => conSesion(p, (x) => ok(yo(x)), false)),
  manejar('cambiarClavePlataforma', (p) =>
    conSesion(
      p,
      (x) => {
        const { actual, nueva } = p.cuerpo as { actual: string; nueva: string };
        if (actual !== x.superadmin.clave) return error(401, 'credenciales_invalidas', 'La clave actual no coincide');
        if (nueva === actual) return error(400, 'datos_invalidos', 'La clave nueva tiene que ser distinta');
        x.superadmin.clave = nueva;
        x.superadmin.debe_cambiar_clave = false;
        p.estado.version++;
        return ok(null, 204);
      },
      false,
    ),
  ),
  manejar('baresPlataforma', (p) => conSesion(p, (x) => ok({ bares: [barDemo(p.estado), ...x.bares] }))),
  manejar('crearBarPlataforma', (p) =>
    conSesion(p, (x) => {
      const c = p.cuerpo as { nombre: string; slug: string; zona_horaria: string; administrador: { nombre: string; correo: string } };
      if (c.slug === p.estado.bar.slug || x.bares.some((b) => b.slug === c.slug)) return error(409, 'conflicto', 'Ya hay un bar con esa dirección');
      const cuenta: CuentaBar = { id: `c-${++p.estado.secuencia}`, nombre: c.administrador.nombre, correo: c.administrador.correo.toLowerCase(), rol: 'administrador', estado: 'activo', debe_cambiar_clave: true };
      const bar: BarPlataforma = { id: `bar-${++p.estado.secuencia}`, slug: c.slug, nombre: c.nombre, zona_horaria: c.zona_horaria, activo: true, cuentas: [cuenta] };
      x.bares.push(bar);
      p.estado.version++;
      return ok({ bar, clave_temporal: claveDeEjemplo(p.estado) }, 201);
    }),
  ),
  manejar('crearCuentaPlataforma', (p) =>
    conSesion(p, (x) => {
      const c = p.cuerpo as { nombre: string; correo: string; rol: 'operador' | 'administrador' };
      const correo = c.correo.toLowerCase();
      if (p.parametros.id === 'bar-demo') {
        if (p.estado.usuarios.some((u) => u.correo.toLowerCase() === correo)) return error(409, 'conflicto', 'Ya hay una cuenta con ese correo en este bar');
        const clave = claveDeEjemplo(p.estado);
        const nueva = { id: `u-${++p.estado.secuencia}`, nombre: c.nombre, correo, rol: c.rol, estado: 'activo' as const, clave, debe_cambiar_clave: true, ultimo_acceso: null, intentos: 0, bloqueado_hasta: null };
        p.estado.usuarios.push(nueva);
        p.estado.version++;
        return ok({ cuenta: { id: nueva.id, nombre: nueva.nombre, correo, rol: nueva.rol, estado: nueva.estado, debe_cambiar_clave: true }, clave_temporal: clave }, 201);
      }
      const bar = x.bares.find((b) => b.id === p.parametros.id);
      if (!bar) return error(404, 'no_encontrado', 'Ese bar no existe');
      if (bar.cuentas.some((u) => u.correo === correo)) return error(409, 'conflicto', 'Ya hay una cuenta con ese correo en este bar');
      const cuenta: CuentaBar = { id: `c-${++p.estado.secuencia}`, nombre: c.nombre, correo, rol: c.rol, estado: 'activo', debe_cambiar_clave: true };
      bar.cuentas.push(cuenta);
      p.estado.version++;
      return ok({ cuenta, clave_temporal: claveDeEjemplo(p.estado) }, 201);
    }),
  ),
  manejar('claveCuentaPlataforma', (p) =>
    conSesion(p, (x) => {
      const id = p.parametros.id;
      const delDemo = p.estado.usuarios.find((u) => u.id === id);
      const propia = x.bares.flatMap((b) => b.cuentas).find((u) => u.id === id);
      if (!delDemo && !propia) return error(404, 'no_encontrado', 'Esa cuenta no existe');
      const clave = claveDeEjemplo(p.estado);
      if (delDemo) {
        delDemo.clave = clave;
        delDemo.debe_cambiar_clave = true;
      }
      if (propia) propia.debe_cambiar_clave = true;
      p.estado.version++;
      return ok({ clave_temporal: clave });
    }),
  ),
];
