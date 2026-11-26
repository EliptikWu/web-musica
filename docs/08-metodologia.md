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

La guía para el equipo de backend es el [doc 11](11-entrega-al-backend.md). En resumen:

1. El equipo de backend revisa el doc 04 y `packages/contrato/generado/openapi.json`.
2. Lo que cambien se corrige en `packages/contrato` y se regenera; la compilación y las
   pruebas dicen qué se rompió en la interfaz.
3. Se compila con `NEXT_PUBLIC_API_MODE=real` y se arranca con `ROCKOLA_API_URL` (doc 03).
4. Se repasan los `TBD` del doc 04 y se prueban a mano los recorridos del doc 09: la suite
   e2e corre contra la simulación (usa sus escenarios), no contra la API.

## Herramientas

| Para qué | Herramienta |
|---|---|
| Código y documentación | Este repositorio |
| Backlog original | `docs/fuente/Gestión Backlog.xlsx` |
| Diseño | `prototipos/` + skills de diseño |
| Chequeo completo | `npm run check` |

## Registro de iteraciones

_Las iteraciones 0 a 28 están en [historial/08-iteraciones-cerradas.md](historial/08-iteraciones-cerradas.md)._

| It. | Fechas | Qué se entregó | Verificado | Pendiente |
|---|---|---|---|---|
| 29 Escáner de QR en Android | 2026-11-21 | D20, RF-066, ADR-32. La app pide el permiso de cámara, lee el QR en el teléfono y entra al bar; rechaza los QR que no son de Rockola | Pruebas unitarias y lint de Android, y recorrido en emulador | Sin probar en un teléfono físico ni con un QR real impreso |
| 30 Logo y ajustes del panel | 2026-11-21 | Logo de la app Android. RF-064: bares plegados con dirección y horario. RNF-016: manual con un tema por página. Las fechas del diálogo de eventos ya no se montan | Lint, pruebas y lanzador en emulador; e2e y capturas a 360, 768 y 1440 px | La consulta nueva de la plataforma no se probó contra una base; ícono monocromo sin probar en el sistema |
| 31 Cubo en vivo y géneros | 2026-11-24 | D21, RF-067, RF-068, ADR-33. Se trae lo que hizo el equipo de backend. Cubo en vivo por defecto, que se actualiza con el aviso `analitica` del tiempo real y junta las ráfagas; 5 géneros a la vista, tamaño automático, vacío distinto de cero y KPI de tiempo de reproducción. Vista «Canciones» para corregir el género. En el backend, el manejador de errores vuelve a cubrir la analítica, la plataforma y el QR | Pruebas unitarias (limitador, cubo en vivo de la simulación, vacío frente a cero, catálogo), e2e (un voto mueve el cubo en ≤ 3 s; cinco votos seguidos, ≤ 3 peticiones; corregir un género), capturas a 360, 768 y 1440 px en Rockola, Vidrio y Afiche, y `npm run check` | Sin probar contra una base real ni con el backend corriendo: el SSE `analitica` y el PATCH solo se vieron en la simulación |
| 32 Escáner al abrir la app y cambiar de bar | 2026-11-24 | D22, RF-066, RF-069. Corrección pedida por el usuario: tras instalar la app, abrirla no llevaba a la cámara sino a «Escanea el QR de tu mesa». Ahora, sin bar recordado, abre el escáner sola una vez; si se cierra, queda la tecla. No se abre en teléfonos sin cámara. Y como la app recuerda el bar aunque se cierre, «Cambiar de bar» reemplaza a «Escanear otro QR»: olvida el bar y abre la cámara | Prueba unitaria de `EntradaViewModel` y lint de Android | Sin probar en emulador ni teléfono. Con el APK de depuración en simulación no se ve: entra al bar «demo» sin pedir QR |
| 33 Escáner en la simulación de Android | 2026-11-24 | D23, ADR-34, RF-066. Punto 5 de la lista del equipo: el APK de prueba entraba directo al bar «demo» y nunca mostraba la cámara. Ahora, sin bar recordado, abre el escáner también en la simulación, acepta el QR que muestra la web en `npm run dev` y ofrece «Entrar al bar de demostración». Las capturas con `--es pantalla|escenario|tema` siguen entrando directo | Pruebas unitarias del lector (rutas aceptadas y rechazadas en los dos modos) y de la entrada, lint y recorrido en el emulador | Sin probar un QR real con la cámara: el emulador no puede apuntar a uno. Queda probarlo en un teléfono con la TV de `npm run dev` |
| 34 Lista de errores del equipo | 2026-11-25 | Puntos 2 a 20 de la lista del equipo, en commits por grupo: volumen, bitácora y errores de YouTube; TV que no se silencia ni se detiene y lista de la casa (D24, ADR-35); aviso de cierre y horario al día (RF-070); menú del panel en grupos, Bar y QR, Canciones (orden, «Clasificar las que faltan», diccionario), Personal (V14), eventos; Plataforma; gráficas del Diagnóstico; manual | Pruebas unitarias de la web y del backend, lint, typecheck y e2e del panel, la TV, el cliente, la analítica y la plataforma | Sin probar contra la base real ni con YouTube real: el reproductor que no se desmonta y el regreso a la pestaña solo se revisaron en código y con pruebas unitarias. «No carga superadmin» depende de desplegar el backend con el arreglo de D21 |
| 35 Segunda lista del equipo | 2026-11-25 | Doce puntos vistos en lo desplegado. Ya estaban en `main`: menú por grupos y volumen fuera de la bitácora. Hecho: «Bar y QR» en dos columnas sin huecos; pausar y reanudar al pulsar y acciones sin parpadeo; aviso de YouTube de 5 min que se desvanece; «fuera de horario» que ya no se queda en «a mano» al empezar el horario (RN-09) y la simulación con los eventos en el horario; mostrar la clave en todos los logins (`CampoClave`); eventos pasados editables enteros (RF-045 cambiado); sin shorts (RN-06); cuatro gráficas nuevas (RF-051, RF-052); pruebas de rendimiento y análisis en el doc 07. Backlog: T6 ícono con logo, T7 JS del teléfono, T8 mediciones contra el backend | `npm run check` completo (158 unitarias, 91 e2e) y, tras el barrido, e2e del panel y de rendimiento; `./gradlew test`; `security-review` sin hallazgos; capturas a 360, 768 y 1440 px en Rockola, Vidrio y Afiche de Bar y QR y de la analítica | Nada probado contra el backend desplegado ni con YouTube real: el filtro de shorts, el aviso de 5 min, el cambio de forzado y las consultas nuevas de analítica. La app Android (`MotorSimulado.kt`) no tiene aún los eventos en el horario ni el cambio de forzado |
| 36 Front y back, carga y logo | 2026-11-25 | D25: el equipo hace el frontend y el backend; lo pendiente del backend pasa al backlog (T9–T11). Prueba de carga con k6 (`scripts/carga/`), que encontró que el backend se congelaba 30 s con 10 votos a la vez (los avisos SSE trababan el pool de conexiones); arreglado. Logo de la app Android (T6) | k6 con 60 y 200 teléfonos contra el backend local y Postgres 16 en Docker; volcado de hilos antes del arreglo; `ContratoSseHiloTest` (falla con el código anterior); `./gradlew test`; lint y pruebas de Android; el ícono en el lanzador del emulador (API 37) | La carga no se corrió contra el backend desplegado, ni con conexiones SSE abiertas ni pidiendo y buscando (YouTube). El ícono temático de una tinta no se vio en el sistema |
| 37 Backlog técnico | 2026-11-25 | T9: migración V15 de `api_idempotencia`. T10: al saltar, el pedido recién validado suena sin esperar a YouTube. T11: cerrada sin cambios (la simulación de Android no tiene panel ni eventos). T7 en parte: el registro del contrato partido por superficie (ADR-36), `/b/[bar]` de 208 a 201 kB en la compilación real | V15 contra Postgres 16 sin la tabla (Flyway y la prueba de arranque); T10 contra Postgres sin llave de YouTube, en los dos casos (validación reciente y vieja); `./gradlew test`; OpenAPI generado sin cambios; JS por ruta medido en la compilación real antes y después; `npm run check` completo | T8 sigue esperando el backend desplegado. Para 180 kB falta `zod/mini`. La ganancia de T10 contra el YouTube real no se midió |
