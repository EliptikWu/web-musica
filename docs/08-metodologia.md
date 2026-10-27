# 08 — Metodología

## Proceso

Iterativo e incremental. Las iteraciones del doc 01 reemplazan a los sprints del backlog
original para el frontend; la equivalencia está en el doc 02, «Historias por iteración».

Ciclo de cada iteración:

```
Planear → Diseñar → Construir → Verificar → Barrer → Registrar → Commit
```

1. **Planear:** qué historias entran y qué pantallas necesitan diseño nuevo.
2. **Diseñar:** con las skills del CLAUDE.md; las pantallas sin prototipo se diseñan en el
   sistema Rockola antes de programarlas.
3. **Construir:** de lo compartido (`packages/`, `lib/`) a lo particular (páginas).
4. **Verificar:** pruebas, capturas a 360/768/1440 (+ TV), `design-critique` y
   `accessibility-review`.
5. **Barrer:** los cinco pasos del doc 10, con `code-review` y `simplify`.
6. **Registrar:** la fila de la iteración abajo y los docs afectados.
7. **Commit local** (D6).

Las iteraciones van seguidas por decisión del usuario (D6). Si una tarea parece pedir
trabajo de otra iteración, se anota y se hace en la suya.

## Estados de una tarea

`Pendiente → Lista → En curso → En verificación → Hecha`. «Lista» exige ID de requisito,
criterio de aceptación y diseño (prototipo o diseño nuevo aprobado en la crítica).

## Control de versiones

- Rama `main`. Sin remoto por ahora: no se sube nada sin pedirlo.
- Commits con prefijo (`feat:`, `fix(área):`, `docs:`, `test:`, `chore:`), en español,
  explicando el porqué y citando PB/RF.
- Un cambio que solo mueve archivos va en su propio commit.
- Cuando haya remoto: ramas cortas (`feat/…`, `fix/…`, `docs/…`) fusionadas por pull
  request con el chequeo en verde.

## Integración con el backend

1. El equipo de backend revisa el doc 04 y `packages/contrato/generado/openapi.json`.
2. Lo que cambien se corrige en `packages/contrato` y se regenera; las pruebas dicen qué
   se rompió en la interfaz.
3. Se configura `NEXT_PUBLIC_API_MODE=real` y `ROCKOLA_API_URL` (doc 03).
4. Se repasan los `TBD` del doc 04 y se corre la suite e2e contra su entorno.

## Herramientas

| Para qué | Herramienta |
|---|---|
| Código y documentación | Este repositorio |
| Backlog original | `docs/fuente/Gestión Backlog.xlsx` |
| Diseño | `prototipos/` + skills de diseño |
| Chequeo completo | `npm run check` |

## Registro de iteraciones

| It. | Fechas | Qué se entregó | Verificado | Pendiente |
|---|---|---|---|---|
| 0 Cimientos | 2026-10-26 | Repositorio y documentación (CLAUDE.md, docs 01–10, fuentes y prototipos); `@rockola/ui` portado de los prototipos con galería en `/muestras`; `@rockola/contrato` (59 operaciones → OpenAPI); app con CSP con nonce, proxy a la API, cliente validado, tiempo real SSE; simulación (motor puro, escenarios, sincronía entre pestañas); `npm run check` | 53 pruebas de lógica, 5 de contrato, 5 e2e con axe. Comprobado en Edge: eje de ancho de Archivo, SSE simulado entre pestañas en < 3 s, CSP de producción con el Service Worker, idempotencia | WebKit (iOS) sin probar todavía: falta instalar su navegador de Playwright |
