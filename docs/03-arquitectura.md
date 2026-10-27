# 03 — Arquitectura del frontend

## Vista general

```
 Teléfono (cliente)      TV del bar (pantalla)      Computador / teléfono (panel)
        │                        │                              │
        └────────────── apps/web (Next.js 16) ──────────────────┘
                 │ páginas: /b/[bar]  /b/[bar]/pantalla  /panel
                 │ lib/api/cliente.ts  ──►  /api/*  (mismo origen)
                 │
     modo "simulado"                          modo "real"
   Service Worker de MSW               app/api/[...path]/route.ts
   + motor de simulación                 reenvía a ROCKOLA_API_URL
   (localStorage + BroadcastChannel)      (API del equipo de backend)
```

- **El navegador nunca habla con YouTube Data API ni con el backend directamente.** Todo
  pasa por `/api/*` del mismo origen: en modo real, la ruta de Next lo reenvía al backend
  (así la cookie de sesión es del mismo sitio y la CSP solo permite `connect-src 'self'`).
- La única conexión a terceros es el **reproductor embebido de YouTube** en la pantalla
  del bar (`youtube-nocookie.com`), y las miniaturas (`i.ytimg.com`).
- El modo se elige con `NEXT_PUBLIC_API_MODE=simulado|real`. Fuera de `lib/modo.ts`,
  `instrumentation-client.ts` y `simulacion/`, ningún archivo sabe en qué modo está.

## Stack

| Pieza | Versión | Para qué |
|---|---|---|
| Node | ≥ 24 | Herramientas y servidor de Next |
| Next.js | 16.4.3 | App Router, render en servidor del armazón, `proxy.ts`, rutas de API |
| React | 19.3.1 | Interfaz |
| TypeScript | 5.9.3 | Tipado estricto (ADR-03) |
| Tailwind CSS | 4.3.3 | Utilidades sobre los tokens Rockola únicamente |
| zod | 4.6.7 | Esquemas del contrato y validación en la frontera |
| TanStack Query | 5.105.0 | Caché de datos del servidor, mutaciones optimistas |
| MSW | 3.0.3 (solo desarrollo) | Simulación de la API en el navegador |
| qrcode.react | 4.2.0 | QR del establecimiento (placa de la TV, descarga, impresión) |
| Vitest | 5.0.5 | Pruebas de lógica pura |
| Playwright + axe-core | 1.64.1 / 4.14.0 | Pruebas de extremo a extremo, accesibilidad y capturas |
| ESLint + eslint-config-next | 9.39.5 / 16.4.3 | Estilo y reglas de Next/React |

Todas las versiones van fijas (sin `^`). Se suben a propósito, con su fila en el registro.

## Repositorio

```
CLAUDE.md  README.md  package.json (workspaces)  .env.example
docs/            esta documentación + fuente/ (originales y texto extraído)
prototipos/      prototipos Rockola (referencia; nada de apps/ los importa)
scripts/         check.mjs (lo que correría el CI), extraer-fuentes.py
packages/
  ui/            @rockola/ui: tokens.css, theme.css, componentes Rockola, iconos
  contrato/      @rockola/contrato: esquemas zod, endpoints.ts, generado/ (OpenAPI, JSON Schema)
apps/
  web/           @rockola/web: la app (Next.js)
```

### `apps/web`

| Carpeta | Qué va | Regla |
|---|---|---|
| `app/` | Rutas: `b/[bar]/(movil)/…`, `b/[bar]/pantalla`, `panel/…`, `q/[token]`, `api/[...path]` | Delgadas: componen y piden datos, sin reglas |
| `components/{cliente,pantalla,panel,gestion,analitica,comun}/` | Componentes con datos (usan hooks) | PascalCase |
| `lib/` | Cliente de API, tiempo real, dispositivo, reglas puras, permisos, formato, CSP | kebab-case; prueba al lado (`*.test.ts`) |
| `simulacion/` | Motor, almacén, reloj, escenarios, datos de ejemplo, manejadores de MSW | Solo se carga en modo simulado |
| `e2e/` | Pruebas de Playwright y tomas de capturas | — |

Los paquetes internos exportan su `src` sin compilar y la app los transpila
(`transpilePackages`), como lk-crm.

## Capas y dependencias

```
página / componente  →  hook (TanStack Query)  →  lib/api/cliente.ts  →  /api/*
                                   ↑
                         lib/reglas (puras)      packages/contrato (esquemas)
```

- Los componentes no llaman a `fetch`: usan hooks de `lib/api`.
- `lib/reglas` no importa nada de React ni de red: ordenar la cola, esperas, horario,
  cupo de pedidos. Es lo que más se prueba.
- `packages/ui` no conoce datos ni rutas: recibe props. Es el sistema de diseño.
- `packages/contrato` no conoce la app: es lo que se comparte con el backend.

## Datos

- **Todo dato en vivo se pide desde el cliente** (TanStack Query). Las páginas del servidor
  solo dan el armazón. Motivos: la simulación (Service Worker) no alcanza las peticiones
  del servidor, y las horas se formatean en la zona del bar (America/Bogota) en un solo
  sitio.
- Cliente de API (`lib/api/cliente.ts`): arma la ruta desde `endpoints.ts`, envía
  `X-Rockola-Dispositivo` e `Idempotency-Key` cuando toca, corta a los 8 s
  *(propuesto)* y valida la respuesta con el esquema zod del endpoint. Si el backend
  responde algo que no cumple el contrato, falla con `ErrorContrato` en vez de pintar
  datos rotos.
- Errores: `{ codigo, mensaje, detalle? }` con los códigos del doc 04. Los textos que ve
  la persona salen de `lib/textos`, nunca del servidor ni de una traza.
- Mutaciones: reintento único y solo por error de red, con la **misma**
  `Idempotency-Key` (RNF-008). El voto es optimista con reversión (RF-012, < 1 s).

## Tiempo real

- Interfaz `FuenteTiempoReal` (`lib/tiempo-real/`): `conectar(slug, alEvento, alEstado)`.
  Estados `conectando | conectado | reconectando | desconectado`.
- Implementación: **SSE** sobre `/api/bares/{slug}/eventos`. El proxy de Next no puede
  reenviar WebSocket; SSE sí pasa. Si el backend elige WebSocket, se escribe otra
  implementación de la misma interfaz (y se abre `connect-src` a su dominio).
- Eventos: `ahora` (foto completa de lo que suena, la cola y el estado del servicio, con
  `version`), `panel` (aviso de que cambió algo del panel; sin datos privados) y `ping`.
- Al reconectar o volver a la pestaña se vuelve a pedir la foto (resync, RNF-012). Si la
  `version` salta, también. Sin SSE, se consulta cada 3 s.
- Banner «Sin conexión» cuando pasan 5 s sin conexión *(propuesto)*.

## Simulación

Cumple el contrato en el navegador para trabajar sin backend (D5).

- **MSW** arranca desde `instrumentation-client.ts` solo si `NEXT_PUBLIC_API_MODE=simulado`;
  en una compilación real ese código no se incluye.
- **Estado compartido:** un JSON en `localStorage` (`rockola.sim.v1`). Cada cambio se hace
  dentro de `navigator.locks` y se anuncia por `BroadcastChannel`: cliente, TV y panel en
  pestañas del mismo navegador se ven en vivo.
- **Motor puro** (`simulacion/motor.ts`): avanza la noche según el reloj (fin de canción →
  siguiente por RN-05 o lista de la casa por RN-08), expira bloqueos, aplica el horario
  (RN-09) y el mantenimiento (RN-10), y registra acciones y eventos.
- **Reloj virtual** con ancla y velocidad, para capturas deterministas y para acelerar.
- **Escenarios** por `?escenario=` (por ejemplo `momento-b`, `fuera-de-horario`,
  `en-pausa`, `cola-vacia`, `youtube-caido`, `desconexion`).
- Los datos salen de `prototipos/herramientas/datos-de-ejemplo.md`. Las métricas e
  historial son de ejemplo y se rotulan así.
- Límite: sincroniza pestañas de un navegador, no dispositivos distintos (doc 01).

## Seguridad

- `proxy.ts` (Next 16) pone en cada respuesta una **CSP con nonce**:
  `default-src 'self'; script-src 'self' 'nonce-…' 'strict-dynamic'; style-src 'self' 'nonce-…';
  img-src 'self' data: blob: https://i.ytimg.com; font-src 'self'; connect-src 'self';
  worker-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self';
  frame-ancestors 'none'`, más `frame-src` de YouTube **solo** en la ruta de la pantalla.
- Cabeceras: `nosniff`, `X-Frame-Options: DENY`, `Permissions-Policy`, HSTS en producción.
  `Referrer-Policy: same-origin`, salvo la pantalla del bar
  (`strict-origin-when-cross-origin`: YouTube rechaza el embebido sin referente).
- Sin `style=""` en el HTML del servidor: los valores dinámicos (avance de una canción) se
  ponen con una ref o con atributos SVG.
- Panel: el backend pone una cookie httpOnly; `proxy.ts` solo comprueba que exista para
  mandar a `/panel/entrar`; la verdad es `GET /panel/yo`. Los permisos se aplican en el
  servidor; la interfaz solo oculta.
- El QR codifica `/q/[token]`, no la ruta del bar, para que regenerarlo invalide el
  anterior (RF-035).

## Registro de decisiones (ADR)

| ID | Fecha | Decisión | Motivo | Alternativa descartada |
|---|---|---|---|---|
| ADR-01 | 2026-10-26 | Next.js 16 + React 19 + Tailwind 4 (D3) | Render en servidor del armazón para abrir rápido desde el QR (RF-001); proxy a la API en el mismo origen; mismo stack que lk-crm | Vite + React SPA: sin servidor propio para el proxy ni la CSP con nonce |
| ADR-02 | 2026-10-26 | Monorepo con npm workspaces; una app y dos paquetes (D4) | KISS; el contrato y el sistema de diseño se reutilizan y se pueden compartir | Turborepo/pnpm: no hace falta con un solo app; tres apps: config triplicada |
| ADR-03 | 2026-10-26 | TypeScript 5.9.3 y ESLint 9.39.5 | `typescript-eslint` exige TypeScript < 6.1 y los plugins de `eslint-config-next` 16.4 piden ESLint 9 | TS 7.0 / ESLint 10: el lint no instala o no corre |
| ADR-04 | 2026-10-26 | Simulación con MSW en el navegador + estado en localStorage y BroadcastChannel (D5) | El front avanza sin backend y cliente, TV y panel se ven sincronizados; integrar es cambiar una variable | Servidor simulado en Node: es código de backend dentro del repo |
| ADR-05 | 2026-10-26 | Identificadores del dominio en español | El diccionario de datos, los prototipos (`.tecla`, `.tarjeta`) y el equipo están en español; traducir en cada capa invita a errores | Inglés como lk-crm: obliga a mantener un glosario de traducción |
| ADR-06 | 2026-10-26 | Contrato en zod (`packages/contrato`) con campos snake_case en español; se exporta OpenAPI con `z.toJSONSchema` | Una sola fuente para cliente, simulación y documentación; el backend recibe un OpenAPI legible | Tipos a mano sin validación: no detecta que el backend cambie un campo |
| ADR-07 | 2026-10-26 | Tiempo real por SSE detrás de una interfaz | Pasa por el proxy de Next; reconexión nativa | WebSocket por el proxy: Next no reenvía el upgrade |
| ADR-08 | 2026-10-26 | Archivo variable (ejes `wght` y `wdth` 62–125) con `next/font/google` | La identidad depende del ancho; `next/font` la sirve desde el propio dominio (sin peticiones a Google) | CSS de Google Fonts: petición externa y CSP más abierta |
| ADR-09 | 2026-10-26 | `scripts/check.mjs` en Node en lugar de `check.py` | El repositorio es solo JavaScript; sin Python para el día a día | Python como lk-crm: dependencia extra sin backend Python |
| ADR-10 | 2026-10-26 | El QR codifica `/q/[token]` | RF-035 pide que el QR regenerado invalide el anterior | QR a `/b/[bar]`: no se puede invalidar |
| ADR-11 | 2026-10-26 | Toda página se genera por petición (`connection()` en el layout raíz) | Next solo pone el nonce de la CSP en páginas dinámicas; una estática quedaría sin scripts | Páginas estáticas: más rápidas, pero la CSP estricta las rompe |
| ADR-12 | 2026-10-26 | MSW en `dependencies` (no en `devDependencies`) | Una demo en modo simulado se puede desplegar con `npm ci --omit=dev`; en modo real el código no se incluye | Solo en desarrollo: una demo desplegada no arrancaría |
| ADR-13 | 2026-10-26 | Puerto 3200 para la app | El 3000 lo usan otros proyectos del equipo en la misma máquina | 3000: choque al tener dos proyectos abiertos |
| ADR-14 | 2026-10-26 | La TV es un lienzo de 1280 × 720 ampliado con `zoom` (puesto desde una ref) | Las medidas del prototipo se usan tal cual y el texto queda nítido a 1920 × 1080 y 4K | Unidades de viewport en cada medida: reescribir el prototipo entero; `transform: scale`: texto borroso en algunas TV |
| ADR-15 | 2026-10-26 | A la TV se llega siempre con carga completa (`EnlacePantalla`), nunca con navegación suave | La CSP se fija con el documento: solo la de la TV deja embeber YouTube | Permitir YouTube en la CSP de todas las rutas: abre un permiso que el resto no necesita |
| ADR-17 | 2026-10-26 | La sesión caducada se maneja en un solo sitio (las cachés de TanStack Query) y con carga completa a `/panel/entrar?motivo=caducada`; salir también recarga | Una regla para todo el panel y nada del panel queda en memoria sin sesión (barrido de la iteración 3) | Revisar el 401 en cada pantalla: se olvidaba en alguna y el panel quedaba congelado |
| ADR-16 | 2026-10-26 | Playwright nunca reutiliza un servidor en marcha | Uno viejo en el 3200 sirvió una compilación anterior y las pruebas miraron otra cosa (barrido de la iteración 1) | Reutilizarlo: más rápido, pero da resultados falsos |
