// Prueba de carga de una noche (doc 07, análisis del 2026-11-25, #7; doc 09, «Rendimiento»).
//
// Simula un bar lleno: muchos teléfonos que miran la cola y votan y quitan su voto, y el panel que
// recarga la noche. Se corre a mano contra un backend levantado para la prueba (nunca contra el de
// producción sin avisar), con k6 (https://k6.io):
//
//   k6 run -e BASE=http://localhost:18080 -e TELEFONOS=60 -e DURACION=2m scripts/carga/noche.js
//
// Antes: la base con `esquema-reconstruido.sql` y `scripts/carga/semilla.sql` (pedidos pendientes) y
// el backend con ROKOLA_AUTOREPRODUCCION_HABILITADA=false. El panel entra con la cuenta de la demo
// local; contra otro backend, con -e PANEL_CORREO=… -e PANEL_CLAVE=… (nunca escritas aquí).
//
// Lo que no cubre: las conexiones SSE abiertas (k6 no las mantiene sin una extensión), pedir y
// buscar (necesitan YouTube). Los umbrales salen de los requisitos: votar < 1 s (RF-012), acciones
// y propagación ≤ 3 s (RNF-002, RNF-009); el de `ahora` es *(propuesto)*.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:18080';
const BAR = __ENV.BAR || 'demo';
const TELEFONOS = Number(__ENV.TELEFONOS || 60);
const DURACION = __ENV.DURACION || '2m';
// Por defecto, la cuenta de la demo local (pública, doc CLAUDE.md). Contra otro backend, por variables de entorno.
const CORREO = __ENV.PANEL_CORREO || 'admin@rockola.demo';
const CLAVE = __ENV.PANEL_CLAVE || 'rockola-demo';

const CON_409 = http.expectedStatuses({ min: 200, max: 299 }, 409);

const tAhora = new Trend('rockola_ahora', true);
const tVotar = new Trend('rockola_votar', true);
const tQuitar = new Trend('rockola_quitar_voto', true);
const tNoche = new Trend('rockola_panel_noche', true);

export const options = {
  scenarios: {
    telefonos: { executor: 'constant-vus', exec: 'telefono', vus: TELEFONOS, duration: DURACION },
    panel: { executor: 'constant-vus', exec: 'panel', vus: 2, duration: DURACION },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    rockola_votar: ['p(95)<1000'], // RF-012
    rockola_quitar_voto: ['p(95)<1000'],
    rockola_panel_noche: ['p(95)<3000'], // RNF-002
    rockola_ahora: ['p(95)<500'], // (propuesto)
  },
};

/** Un UUID v4 fijo por teléfono virtual: cada uno es un dispositivo distinto. `LOTE` cambia todos (teléfonos nuevos). */
const LOTE = String(Number(__ENV.LOTE || 0)).padStart(4, '0');
function dispositivo() {
  const n = String(__VU).padStart(12, '0');
  return `00000000-${LOTE}-4000-8000-${n}`;
}

function clave() {
  const h = () => Math.floor(Math.random() * 0x10000).toString(16).padStart(4, '0');
  return `${h()}${h()}-${h()}-4${h().slice(1)}-a${h().slice(1)}-${h()}${h()}${h()}`;
}

export function telefono() {
  const cabeceras = { 'X-Rockola-Dispositivo': dispositivo(), Accept: 'application/json' };
  // Como la web al abrir el bar: el teléfono se registra una vez (RF-002).
  if (__ITER === 0) {
    const registro = http.post(`${BASE}/api/bares/${BAR}/dispositivo`, JSON.stringify({ tipo: 'movil' }), {
      headers: { ...cabeceras, 'Content-Type': 'application/json' },
      tags: { op: 'registrar' },
    });
    if (!check(registro, { 'registrar 2xx': (r) => r.status >= 200 && r.status < 300 })) console.warn(`registrar: ${registro.status} ${registro.error || ''} ${String(registro.body).slice(0, 160)}`);
    // Votar pide haber visto el aviso de datos (RF-004), como en la web.
    const aviso = http.post(`${BASE}/api/bares/${BAR}/dispositivo/aviso`, null, { headers: cabeceras, tags: { op: 'aviso' } });
    check(aviso, { 'aviso 2xx': (r) => r.status >= 200 && r.status < 300 });
  }
  const ahora = http.get(`${BASE}/api/bares/${BAR}/ahora`, { headers: cabeceras, tags: { op: 'ahora' } });
  tAhora.add(ahora.timings.duration);
  if (!check(ahora, { 'ahora 200': (r) => r.status === 200 })) return sleep(2);
  const cola = ahora.json('cola') || [];
  if (cola.length === 0) return sleep(2);
  const elegida = cola[Math.floor(Math.random() * cola.length)];
  const id = elegida.solicitud_id;
  const json = { ...cabeceras, 'Content-Type': 'application/json', 'Idempotency-Key': clave() };
  // 409 ya_votaste no es una falla de la carga: otro ciclo de este teléfono dejó su voto ahí.
  const voto = http.post(`${BASE}/api/bares/${BAR}/solicitudes/${id}/votos`, null, { headers: json, tags: { op: 'votar' }, responseCallback: CON_409 });
  tVotar.add(voto.timings.duration);
  check(voto, { 'votar 2xx o ya votado': (r) => (r.status >= 200 && r.status < 300) || r.status === 409 });
  sleep(1 + Math.random() * 2);
  const quitar = http.del(`${BASE}/api/bares/${BAR}/solicitudes/${id}/votos`, null, { headers: { ...cabeceras, 'Idempotency-Key': clave() }, tags: { op: 'quitar_voto' }, responseCallback: CON_409 });
  tQuitar.add(quitar.timings.duration);
  check(quitar, { 'quitar voto 2xx o 409': (r) => (r.status >= 200 && r.status < 300) || r.status === 409 });
  sleep(1 + Math.random() * 2);
}

// La cookie del panel es `Secure`: k6 no la devuelve por http en local, así que se manda a mano.
let cookiePanel = null;

export function panel() {
  // Cada usuario virtual entra una vez y recarga la noche cada 3 s, como el panel sin tiempo real (RNF-009).
  if (cookiePanel === null) {
    const entrar = http.post(`${BASE}/api/panel/sesion`, JSON.stringify({ correo: CORREO, clave: CLAVE }), {
      headers: { 'Content-Type': 'application/json' },
      tags: { op: 'entrar' },
    });
    check(entrar, { 'entrar 200': (r) => r.status === 200 });
    const valor = entrar.cookies.rockola_panel?.[0]?.value;
    if (!valor) return sleep(3);
    cookiePanel = `rockola_panel=${valor}`;
  }
  const noche = http.get(`${BASE}/api/panel/noche`, { headers: { Cookie: cookiePanel }, tags: { op: 'noche' } });
  tNoche.add(noche.timings.duration);
  check(noche, { 'noche 200': (r) => r.status === 200 });
  sleep(3);
}
