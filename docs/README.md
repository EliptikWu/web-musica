# Documentación de Rockola (frontend)

Fuente de verdad del proyecto. Los documentos numerados son la versión de trabajo; los
originales del equipo, con los que se entregó el curso, están en `fuente/`.

## Índice

| Doc | De qué trata |
|---|---|
| [01 Alcance y definiciones](01-alcance-y-definiciones.md) | Qué es, qué no es, roles, superficies, iteraciones, glosario |
| [02 Requisitos](02-requisitos.md) | RF, RNF y reglas de negocio con su historia (PB) y su iteración |
| [03 Arquitectura](03-arquitectura.md) | Stack, carpetas, capas, simulación, tiempo real, seguridad, registro de decisiones (ADR) |
| [04 Modelo de datos y contrato](04-modelo-de-datos-y-contrato.md) | Entidades, estados, endpoints, eventos, errores y lo pendiente del backend |
| [05 UI/UX](05-ui-ux.md) | Identidad Rockola, tokens, componentes, pantallas, estados, responsive, accesibilidad, textos |
| [06 Calidad](06-calidad.md) | Atributos priorizados, principios, reglas de código, definición de hecho |
| [07 Optimizaciones](07-optimizaciones.md) | Presupuestos de carga y respuesta |
| [08 Metodología](08-metodologia.md) | Iteraciones, git, entregas, registro de iteraciones |
| [09 Pruebas y QA](09-pruebas-qa.md) | Niveles de prueba, matriz de permisos, puertas de calidad |
| [10 Corrección de errores](10-correccion-de-errores.md) | El barrido, severidad, flujo de un error |
| [11 Entrega al backend](11-entrega-al-backend.md) | Qué recibe el equipo de backend, cómo se conecta el frontend a su API y qué falta decidir |
| `fuente/` | Requisitos, historias, prototipos/arquitectura/modelo y backlog originales (.docx, .xlsx) |
| `fuente/extraido/` | Su texto en Markdown (`python -I scripts/extraer-fuentes.py`) |

## Decisiones tomadas

| ID | Fecha | Decisión | Quién |
|---|---|---|---|
| D1 | 2026-10-26 | Este repositorio es solo el frontend; el backend es de otro equipo | Usuario |
| D2 | 2026-10-26 | Se implementa solo la dirección visual **Rockola** (Tema #2); Vidrio y Afiche se descartan | Usuario |
| D3 | 2026-10-26 | Next.js 16 + React 19 + Tailwind 4 + TypeScript estricto, en un monorepo con npm workspaces | Usuario |
| D4 | 2026-10-26 | Una sola app (`apps/web`) con tres superficies: cliente, pantalla del bar y panel | Usuario |
| D5 | 2026-10-26 | Sin backend, la app corre contra una simulación en el navegador que cumple el contrato propuesto | Usuario |
| D6 | 2026-10-26 | Las iteraciones van seguidas; cada una cierra con capturas, barrido y un commit local, sin subir a remotos | Usuario |
| D7 | 2026-10-26 | Las ingenierías de lk-crm (CLAUDE.md, docs 06, 08, 09, 10) se adaptan a este proyecto | Usuario |
| D8 | 2026-10-26 | Las skills de diseño son obligatorias y se permiten reworks de los prototipos si la crítica lo justifica | Usuario |
| D9 | 2026-10-26 | Matriz del Operador *(propuesto, hasta que el equipo la confirme)*: saltar, pausar/reanudar la reproducción, pausar/reanudar solicitudes, retirar de la cola y bloquear hasta el fin de la sesión. Lo demás, solo el Administrador | Claude, por contradicción entre RF-022 y RF-039/042 |

## Decisiones abiertas

| Tema | Qué falta decidir | Quién |
|---|---|---|
| Transporte de tiempo real | SSE (propuesto, pasa por el proxy) o WebSocket; y su autenticación | Equipo de backend |
| Matriz del Operador | Confirmar D9 | Equipo |
| Estado `retirada` | El modelo no lo tiene y RF-041/RF-013 lo necesitan (doc 04) | Equipo de backend |
| Lista de la casa fuera de horario | ¿Suena aunque no haya sesión abierta? El prototipo dice que sí | Equipo |
| Alta de establecimientos | ¿Quién crea el establecimiento y su primer Administrador? (RF-034 vs RF-023) | Equipo |
| Bloqueo de cuenta e inactividad | Número de intentos, minutos de bloqueo y de inactividad | Equipo |
| Emparejar la TV | Clave para que solo la pantalla del bar reporte el fin de una canción | Equipo de backend |

## Pendiente de recibir

- Nombre y slug reales del bar (los prototipos dicen `[Nombre del bar]`; la simulación
  usa «Bar de demostración», rotulado).
- Texto legal de la política de tratamiento de datos (Ley 1581 de 2012).
- IDs de YouTube verificados de las canciones de ejemplo.
- El documento de analítica que citan las historias PB-43 y PB-44 («hallazgos») y la
  sección 5.1 (EventoAnalitico), que no están en `fuente/`.

## Reglas de estos documentos

- Prohibido inventar: lo no verificado queda `TBD`, con quién lo resuelve.
- Las metas con número sin medición se marcan *(propuesto)*.
- Un requisito nuevo recibe ID en el doc 02 antes de implementarse.
- Se actualizan en el mismo cambio que altera el comportamiento.
