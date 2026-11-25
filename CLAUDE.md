# Rockola — instrucciones para Claude

Rockola es una **rockola digital colaborativa para bares**: el cliente escanea un QR sin
instalar ni registrarse, busca en YouTube, pide canciones y vota; la cola se ordena por votos,
la pantalla del bar las reproduce y el personal administra desde un panel. Proyecto nuclear de
Ingeniería de Software (Universidad Alexander von Humboldt, 2026).

**Este equipo hace los frontends:** la web (`frontend/web`, Next.js) y la app Android
(`frontend/movil`, Kotlin + Compose, con su propio `CLAUDE.md`). El backend lo hace otro equipo en
`rokolaBackend/`; por D14 este equipo también hace ahí lo que el frontend necesita, y cada cambio
va al doc 11 y a «Pendiente del backend» del doc 04 para que lo revisen. Sin la API, la web y la
app corren contra su **simulación**, con el contrato del doc 04. Integrar es apuntar a la API real.

## Estado actual

- Iteraciones: `docs/08-metodologia.md`, «Registro de iteraciones».
- Backlog: docs/README.md («Lo que queda»)
- Las iteraciones van seguidas (usuario, 2026-10-26). Cada una cierra con capturas, revisión de
  diseño y accesibilidad, `npm run check` en verde, barrido y un **commit local**. No se sube
  nada a ningún remoto sin pedirlo.
- Rockola corre en el puerto **3200** (el 3000 lo usan otros proyectos del equipo).

| Para | Comando (desde la raíz) |
|---|---|
| Arrancar en local (simulado) | `npm run dev` → `http://localhost:3200` |
| Todo lo del CI | `npm run check` (o `-- --sin-e2e`) |
| Una parte | `npm run lint\|typecheck\|test\|build\|e2e -w @rockola/web` |
| Regenerar el contrato | `npm run generar` (el chequeo falla si `packages/contrato/generado/` no está al día) |
| Capturas del manual | `npm run capturas -w @rockola/web` (rehacerlas si cambia una pantalla que el manual muestra) |
| Texto de las fuentes | `python -I scripts/extraer-fuentes.py` |
| Android | desde `frontend/movil`: `./gradlew testDebugUnitTest lintDebug` (fuera de `npm run check`) |
| Backend | desde `rokolaBackend`: `./gradlew test` (la prueba de arranque solo corre con `DB_URL`; doc 11) |

- Los e2e usan el Edge instalado y la app compilada (`npm run build -w @rockola/web`).
- Simulación: `?escenario=<nombre>&reloj=fijo|rapido&reiniciar`; panel con `admin@rockola.demo`
  u `operador@rockola.demo` y clave `rockola-demo`.
- `prototipos/` es referencia: nada de `frontend/` lo importa.

## La documentación manda

`docs/` es la fuente de verdad: léela antes de proponer o cambiar algo (índice y decisiones en
`docs/README.md`). Los originales del equipo están en `docs/fuente/` (texto en `extraido/`): si
chocan con un doc numerado, se corrige el doc numerado y se anota por qué.

| Si vas a tocar… | Lee primero |
|---|---|
| Alcance, roles, glosario | `docs/01-alcance-y-definiciones.md` |
| Cualquier funcionalidad | `docs/02-requisitos.md` (criterio completo en `docs/fuente/extraido/`) |
| Stack, carpetas, simulación, tiempo real, seguridad, decisiones | `docs/03-arquitectura.md` |
| Datos, estados, endpoints o eventos | `docs/04-modelo-de-datos-y-contrato.md` y `packages/contrato` |
| Una pantalla, componente, token o texto | `docs/05-ui-ux.md` y `prototipos/` |
| Qué significa «hecho» | `docs/06-calidad.md` |
| Rendimiento | `docs/07-optimizaciones.md` |
| Iteraciones, commits, entregas | `docs/08-metodologia.md` |
| Pruebas | `docs/09-pruebas-qa.md` |
| Un error | `docs/10-correccion-de-errores.md` |
| La API del backend | `docs/11-entrega-al-backend.md` |

- Un requisito nuevo recibe su ID en el doc 02 **antes** de implementarse.
- Una decisión técnica es una fila nueva en el doc 03, con motivo y alternativa descartada. Una
  decisión aceptada no se edita: se reemplaza.
- La documentación afectada se actualiza en el mismo cambio que altera el comportamiento.
- Un cambio de `packages/contrato` va al doc 04 y regenera `generado/` (lo lee el backend).

## Prohibido inventar

- Lo que depende del backend o de YouTube (campos, límites, errores) se verifica en la fuente;
  lo no verificado es `TBD` y va a «Pendiente del backend» del doc 04.
- Metas con número sin medición: *(propuesto)*.
- Canciones y artistas de ejemplo son reales; votos, horas, métricas e historial de la
  simulación son **datos de ejemplo** y la interfaz lo dice donde se puedan confundir (analítica).
- Lo que falta del equipo va entre corchetes o `TBD`: nombre del bar, texto legal, IDs de video.

## Seguridad — no negociable

- Cero credenciales en código, scripts, pruebas o docs: todo por variables de entorno
  (`.env.example` lista las que existen, sin valores reales).
- La llave de YouTube **nunca** llega al navegador: las búsquedas pasan por la API.
- El panel solo confía en la sesión del backend (cookie httpOnly). La interfaz oculta lo que el
  rol no permite, pero **decide el servidor**; matriz en `frontend/web/lib/permisos.ts` y doc 04.
- La CSP con nonce de `frontend/web/proxy.ts` no se relaja para «que funcione»: nada de
  `style=""` en el HTML del servidor ni scripts en línea sin nonce.
- El identificador del teléfono es seudónimo (Ley 1581, RF-002, RNF-007): no se registra ni se
  muestra completo.
- Lo que toque sesión, permisos, la CSP, el proxy hacia la API o datos del teléfono se revisa dos
  veces (`security-review` y `code-review`).

## Diseño — que no parezca una plantilla

La identidad completa (dirección Rockola, tokens, tipografía Archivo y sus anchos, acento escaso,
los tres diseños Rockola, Vidrio y Afiche) está en `docs/05-ui-ux.md`. Lo que más se rompe:

- **Solo tokens**, en un único lugar: `packages/ui/src/tokens.css` (Vidrio y Afiche bajo
  `[data-tema=…]`, formas en `packages/ui/src/temas/`). Ningún color, tipografía o radio suelto.
- **Sin recursos de plantilla** en Rockola: nada de titular centrado con tres tarjetas, degradados
  morados, vidrio esmerilado, píldoras ni emojis como iconos (Vidrio sí usa los suyos, fiel a
  `prototipos/vidrio/`). Mismo marcado y mismas reglas de accesibilidad para los tres diseños.
- **Toda pantalla** parte de un prototipo o se diseña en el sistema antes de programarla, y se
  comprueba en el navegador a 360, 768 y 1440 px (TV a 1280 × 720 y 1920 × 1080), con sus
  estados vacío, cargando y error.

### Las skills de diseño son obligatorias

**Ningún trabajo de diseño se hace de memoria**, tampoco un cambio pequeño: antes de diseñar,
maquetar o retocar una pantalla, componente, token o texto se cargan con Skill las del paso.

| Cuándo | Skill |
|---|---|
| Dirección visual, composición o tipografía | `frontend-design:frontend-design` |
| Tokens o componentes compartidos (`packages/ui`) | `design:design-system` |
| Comparar variantes abiertas | `playground:playground` |
| Textos de interfaz, vacíos y errores | `design:ux-copy` |
| Antes de dar por buena una pantalla | `design:design-critique` y `design:accessibility-review` |
| Documentar una pantalla para el equipo | `design:design-handoff` |
| Una gráfica (analítica) | `dataviz` |
| Pruebas con usuarios | `design:user-research` y `design:research-synthesis` |

- Las skills dan método; la identidad la fijan los prototipos y el doc 05 (si chocan, gana el 05).
- Si una skill no está en la sesión, se dice y se pregunta antes de seguir sin ella.
- Un rework del prototipo justificado por la crítica se anota en el doc 05 con qué y por qué.
- Al informar se nombran las skills usadas y las pantallas revisadas con captura.

## Skills: no solo las de diseño

Antes de empezar un trabajo se mira si hay una skill que aplique y se usa. Si no hay, se busca, se
comprueba quién la revisó y qué añade (ganchos, servidores, credenciales) y se pregunta antes de
instalar lo que no sean solo skills. Manda la lista que enseña la sesión, no esta tabla.

| Cuándo | Skill |
|---|---|
| Lo que «se revisa dos veces» | `security-review` y `code-review` |
| El barrido (doc 10) | `code-review`; `simplify` para lo que sobre |
| Comprobar la app en marcha | `run` |
| Leer o regenerar las fuentes | `anthropic-skills:docx`, `anthropic-skills:xlsx` |
| Investigar algo externo con varias fuentes | `anthropic-skills:deep-research` |
| Revisar si los docs siguen al día | `docs-ingenieria` |

## Procedimiento de ingeniería

1. **Explorar** el doc que aplica y el código, y mirar si hay una skill.
2. **Diagnosticar** la causa raíz con archivo, línea y por qué, no el síntoma.
3. **Diseñar a la altitud correcta:** lo de una capa compartida (`packages/ui`,
   `packages/contrato`, `lib/`) se corrige en esa capa. Reutilizar antes de crear.
4. **Implementar** con el estilo del código vecino. KISS y YAGNI: lo que no está en el 02 no se
   construye.
5. **Verificar** con pruebas, tipos y el navegador (definición de hecho en el doc 06).
6. **Barrer** lo recién hecho antes de darlo por terminado (doc 10, «El barrido»).
7. **Informar con exactitud:** qué se hizo, qué se verificó, qué salió en el barrido y qué no se
   verificó.

## Convenciones

- Docs, comentarios, textos de interfaz y commits en español (Colombia). **Identificadores del
  dominio también en español** (`solicitud`, `ordenarCola`, `Tecla`); en inglés solo lo que impone
  el framework (`page.tsx`, `useQuery`). ADR-05 del doc 03.
- Campos del contrato en `snake_case` español (`votos_total`, `fecha_solicitud`).
- Componentes en `PascalCase.tsx`; el resto en `kebab-case.ts`; pruebas junto a su fuente
  (`*.test.ts`).
- Commits con prefijo (`feat:`, `fix(área):`, `docs:`, `test:`, `chore:`), con el porqué y los ID
  (PB-xx, RF-xxx). Un cambio que solo mueve archivos va en su propio commit.
