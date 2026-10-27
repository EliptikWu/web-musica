# 09 — Pruebas y QA

## Niveles

| Nivel | Herramienta | Qué cubre | Dónde |
|---|---|---|---|
| Unidad | Vitest (entorno node) | Reglas puras: orden de la cola, esperas, horario, cupo, motivos, formato; motor de simulación; cliente de API; esquemas del contrato | `apps/web/lib/**/*.test.ts`, `apps/web/simulacion/**/*.test.ts`, `packages/contrato/**/*.test.ts` |
| Contrato | Vitest + generador | Los datos de ejemplo cumplen los esquemas; `generado/` está al día | `packages/contrato` |
| Extremo a extremo | Playwright (modo simulado, `next start`) | Flujos críticos por superficie y la sincronía entre ellas | `apps/web/e2e/*.spec.ts` |
| Accesibilidad | axe-core dentro de cada e2e | wcag2a, wcag2aa, wcag21aa en **cada estado** visitado | `e2e/support.ts` |
| Objetivos táctiles | Prueba propia | Todo control de la superficie del cliente mide ≥ 44 × 44 px | `e2e/cliente.spec.ts` |
| Visual | Capturas de Playwright | Cada pantalla × estado × ancho, comparadas con `prototipos/capturas/` | `e2e/capturas.spec.ts` → `capturas/` |
| Aceptación | Manual | Criterios de las historias de la iteración | Doc 08 |

Lo visual no se prueba en Vitest: lo cubren e2e y capturas.

## Flujos de extremo a extremo

| Flujo | Qué comprueba |
|---|---|
| Sincronía | Cliente, TV y panel en tres páginas: un voto en el teléfono se ve en la TV y el panel en ≤ 3 s (RNF-009) |
| Pedir y votar | Pedir, «Te quedan N de 3», voto optimista, «Votaste», límite, motivos de rechazo |
| Estados del servicio | Abierto, pausa, mantenimiento, fuera de horario en cliente y TV |
| Reproducción | La TV avanza a la siguiente y pasa a la lista de la casa sin silencios |
| Panel | Saltar, pausar, bloquear, retirar y que el cliente lo vea |
| Permisos | Cada acción del Operador fuera de su matriz → oculta en la interfaz y `403` del servidor (simulado) |
| Desconexión | Corte de la conexión → banner → reconexión → datos al día |

## Matriz de permisos

La matriz del doc 04 se prueba entera: cada acción × rol (Operador, Administrador, sin
sesión). La prueba se genera desde `apps/web/lib/permisos.ts` para que no se desfase.

## Puertas de calidad

| Momento | Qué tiene que pasar |
|---|---|
| Antes de cada commit | Lint, tipos y pruebas de unidad en verde |
| Antes de cerrar una iteración | `npm run check` completo (incluye e2e y `npm audit`), capturas revisadas, barrido hecho |
| Antes de integrar con el backend | Contrato revisado por el equipo de backend; e2e contra su entorno |

## Lista de cierre de iteración

- [ ] Historias de la iteración con su criterio cumplido.
- [ ] `npm run check` en verde.
- [ ] Capturas de cada pantalla y estado a 360, 768 y 1440 (+ TV).
- [ ] `design-critique` y `accessibility-review` hechos y lo encontrado corregido o anotado.
- [ ] Barrido (doc 10) con `code-review`.
- [ ] Docs afectados y registro del doc 08 al día.
- [ ] Commit local.
