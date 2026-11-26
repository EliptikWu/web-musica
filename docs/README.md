# Documentación de Rockola (frontend)

Fuente de verdad del proyecto. Los documentos numerados son la versión de trabajo; los
originales del equipo, con los que se entregó el curso, están en `fuente/`.

**Estado (2026-11-25):** cerrados los puntos 2 a 20 de la lista de errores del equipo, el escáner
de Android (iteraciones 32 a 34 del doc 08) y las tandas T1 a T5. Todo está en la rama
`correcciones-del-equipo`, sin fusionar. Lo pendiente está en «Lo que queda», abajo.

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
| [historial/](historial/README.md) | Lo cerrado de los registros vivos |
| `fuente/` | Requisitos, historias, prototipos/arquitectura/modelo y backlog originales (.docx, .xlsx) |
| `fuente/extraido/` | Su texto en Markdown (`python -I scripts/extraer-fuentes.py`) |

## Decisiones tomadas

| ID | Fecha | Decisión | Quién |
|---|---|---|---|
| D1 | 2026-10-26 | Este repositorio es solo el frontend; el backend es de otro equipo. *Reemplazada en parte por D14: el backend vive en `rokolaBackend/` y este equipo le hace lo que el frontend necesita* | Usuario |
| D2 | 2026-10-26 | Se implementa solo la dirección visual **Rockola** (Tema #2); Vidrio y Afiche se descartan. *Reemplazada por D13* | Usuario |
| D3 | 2026-10-26 | Next.js 16 + React 19 + Tailwind 4 + TypeScript estricto, en un monorepo con npm workspaces | Usuario |
| D4 | 2026-10-26 | Una sola app (`apps/web`) con tres superficies: cliente, pantalla del bar y panel | Usuario |
| D5 | 2026-10-26 | Sin backend, la app corre contra una simulación en el navegador que cumple el contrato propuesto | Usuario |
| D6 | 2026-10-26 | Las iteraciones van seguidas; cada una cierra con capturas, barrido y un commit local, sin subir a remotos | Usuario |
| D7 | 2026-10-26 | Las ingenierías de lk-crm (CLAUDE.md, docs 06, 08, 09, 10) se adaptan a este proyecto | Usuario |
| D8 | 2026-10-26 | Las skills de diseño son obligatorias y se permiten reworks de los prototipos si la crítica lo justifica | Usuario |
| D9 | 2026-10-26 | Matriz del Operador *(propuesto, hasta que el equipo la confirme)*: saltar, pausar/reanudar la reproducción, pausar/reanudar solicitudes, retirar de la cola y bloquear hasta el fin de la sesión. Lo demás, solo el Administrador | Claude, por contradicción entre RF-022 y RF-039/042 |
| D10 | 2026-11-10 | Respuesta al backend: el teléfono sigue con UUID propio (ADR-21), la espera la calcula el backend (ADR-20) y la canción que suena cuenta para el cupo (RN-01) | Usuario |
| D11 | 2026-11-10 | La TV muestra el código y el Administrador lo escribe en el panel (ADR-23, reemplaza a ADR-22) | Usuario |
| D12 | 2026-11-10 | La app Android entra al repositorio en `frontend/movil`, y la web pasa a `frontend/web` (ADR-24) | Usuario |
| D13 | 2026-11-11 | Reemplaza a D2. Se implementan también **Vidrio** y **Afiche**, fieles a sus prototipos, y el Administrador elige el de su bar (RF-059, ADR-25). El diseño cambia el teléfono y la TV; el panel sigue en Rockola. La app Android, por ahora, sigue en Rockola | Usuario |
| D14 | 2026-11-12 | El equipo de frontend hace en `rokolaBackend/` lo que el frontend necesita y el backend no tiene (empezando por `tema` y la edición del establecimiento), con su prueba y anotado en el doc 11 y el doc 04 para que el equipo de backend lo revise. La base de Railway está apagada: se prueba contra un esquema reconstruido del código | Usuario |
| D15 | 2026-11-20 | Este equipo hace también la capa de Big Data del Entregable 6: pipeline con Spark, rutas de analítica en el backend, tablero con datos reales y un cubo OLAP en el sistema Rockola cuyas celdas son los KPI del entregable (RF-063, ADR-27) | Usuario |
| D16 | 2026-11-21 | Resuelve «Alta de establecimientos»: un superadmin crea los bares y sus cuentas desde `/plataforma` (RF-064, ADR-29). Sin registro público | Usuario |
| D17 | 2026-11-21 | Reemplaza la parte de D13 sobre Android: la app también muestra **Vidrio** y **Afiche**, según el `tema` del bar (RF-059, ADR-25) | Usuario |
| D18 | 2026-11-21 | Cada bar elige si su QR abre la web o ofrece descargar la app, con un interruptor y sin reimprimir (RF-065, ADR-30) | Usuario |
| D19 | 2026-11-21 | Reemplaza la parte de D13 sobre el panel: el panel con sesión y el tablero también muestran el diseño del bar, para que todas las vistas sean coherentes (RF-059, ADR-31). Entrar, cambiar la clave y `/plataforma` siguen en Rockola porque no pertenecen a un bar | Usuario |
| D20 | 2026-11-21 | La app Android escanea el QR del bar con la cámara, con permiso pedido en contexto (RF-066, ADR-32) | Usuario |
| D21 | 2026-11-24 | El equipo de backend entrega el cubo en vivo y la corrección manual del género; el frontend los integra: cubo en vivo por defecto y vista «Canciones» (RF-067, RF-068, ADR-33). Se arregla en el backend el manejador de errores que dejó sin forma de contrato la analítica, la plataforma y el QR del panel (D14) | Usuario |
| D22 | 2026-11-24 | Reemplaza la parte de D20 sobre cuándo se pide la cámara: recién instalada y sin bar recordado, la app abre el escáner sola, para escanear el QR y entrar al bar sin otro paso (RF-066). Y «Cambiar de bar» en la bienvenida olvida el bar recordado y vuelve a la cámara, para irse a otro bar la misma noche (RF-069). | Usuario |
| D23 | 2026-11-24 | La simulación de Android ya no entra sola al bar «demo»: pide el QR como la real (se abre el escáner, que acepta el QR de la web en simulación, ADR-34) y ofrece «Entrar al bar de demostración». Así el APK de prueba muestra el mismo recorrido que el de producción (RF-066) | Usuario |
| D24 | 2026-11-25 | Resuelve «Lista de la casa fuera de horario»: la cola y la lista de la casa suenan con la noche abierta, también con las solicitudes en pausa. Sin noche (fuera de horario) o en mantenimiento no suena nada, en el backend y en la simulación (RN-08) | Usuario |
| D25 | 2026-11-25 | Reemplaza D1 y D14: el equipo hace el frontend **y** el backend. Lo que estaba «pendiente del backend» pasa al backlog técnico como cualquier tanda; el doc 11 sigue siendo el registro de lo que cambia en `rokolaBackend/` (archivos, migración, prueba), ya sin esperar la revisión de otro equipo | Usuario |

## Decisiones abiertas

| Tema | Qué falta decidir | Quién |
|---|---|---|
| Matriz del Operador | Confirmar D9 | Equipo |
| Bloqueo de cuenta e inactividad | Número de intentos, minutos de bloqueo y de inactividad | Equipo |

## Pendiente de recibir

- Nombre y slug reales del bar (los prototipos dicen `[Nombre del bar]`; la simulación
  usa «Bar de demostración», rotulado).
- Texto legal de la política de tratamiento de datos (Ley 1581 de 2012).
- IDs de YouTube verificados de las canciones de ejemplo.
- El documento de analítica que citan las historias PB-43 y PB-44 («hallazgos») y la
  sección 5.1 (EventoAnalitico), que no están en `fuente/`.

## Lo que queda

El backlog del proyecto (lo lee `/next-task`). Lo cerrado va a
[historial/lo-que-queda-hecho.md](historial/lo-que-queda-hecho.md).

### Lo que hace una persona

| # | Qué | Por qué |
|---|---|---|
| 1 | Desplegar la web y el backend con lo de esta semana (la lista del equipo del 2026-11-25 vio en lo desplegado el menú sin grupos y el volumen en la bitácora, ya arreglados en `main`): el arreglo de errores de D21, la migración V14, la música con solicitudes en pausa (D24) y el error de YouTube con la canción | «No carga superadmin» y «Todavía no ha entrado» vienen de un backend desplegado viejo; nada de esto se probó contra la base real |
| 2 | Probar la TV una noche con YouTube real y un televisor: que no se silencie ni se detenga al cambiar de canción o de pestaña (ADR-35) | La simulación no usa videos de verdad; solo se revisó en código y con pruebas unitarias |
| 3 | Probar el escáner de la app en un teléfono: abrir la app recién instalada y escanear el QR de la TV de `npm run dev` (D22, D23) | El emulador no puede apuntar la cámara a un QR |
| 4 | Confirmar la matriz del Operador (D9) y los valores de bloqueo de cuenta e inactividad | Decisiones abiertas del equipo |

### Lo aplazado

| Qué | Quién lo aplazó |
|---|---|
| Dominio real de la web y huella del APK firmado para `assetlinks.json` (`TBD`) | Equipo, hasta tener hosting |
| Lo de «Pendiente de recibir», abajo | Equipo |

### Lo técnico, por tandas

| Tanda | Qué (IDs) | Depende de | Apetito | Estado |
|---|---|---|---|---|
| T7 | Bajar el JS del teléfono hacia 180 kB *(propuesto)*: partir `endpoints` del contrato por superficie y, si no alcanza, `zod/mini` (doc 07, análisis del 2026-11-25, #3) | — | Mediano | Abierta |
| T8 | Medir contra el backend desplegado lo que quedó sin medir del análisis del doc 07 (#4, #5, #6) y repetir allá la prueba de carga de `scripts/carga/` (en local ya se hizo: doc 07) | El backend desplegado (persona #1) | Mediano | Abierta |
| T10 | Saltar más rápido en el backend (doc 04, punto 27): verificar el video cuando queda primero en la cola, no al saltar (los avisos SSE ya salen en su propio hilo). Medir antes y después | — | Pequeño | Abierta |
| T11 | La simulación de Android (`MotorSimulado.kt`) al día con la web: eventos en el horario y la apertura forzada que termina al empezar el horario (RN-09, RF-045) | — | Pequeño | Abierta |

## Reglas de estos documentos

- Prohibido inventar: lo no verificado queda `TBD`, con quién lo resuelve.
- Las metas con número sin medición se marcan *(propuesto)*.
- Un requisito nuevo recibe ID en el doc 02 antes de implementarse.
- Se actualizan en el mismo cambio que altera el comportamiento.
