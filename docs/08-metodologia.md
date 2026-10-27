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

| It. | Fechas | Qué se entregó | Verificado | Pendiente |
|---|---|---|---|---|
| 0 Cimientos | 2026-10-26 | Repositorio y documentación (CLAUDE.md, docs 01–10, fuentes y prototipos); `@rockola/ui` portado de los prototipos con galería en `/muestras`; `@rockola/contrato` (59 operaciones → OpenAPI); app con CSP con nonce, proxy a la API, cliente validado, tiempo real SSE; simulación (motor puro, escenarios, sincronía entre pestañas); `npm run check` | 53 pruebas de lógica, 5 de contrato, 5 e2e con axe. Comprobado en Edge: eje de ancho de Archivo, SSE simulado entre pestañas en < 3 s, CSP de producción con el Service Worker, idempotencia | WebKit (iOS) sin probar todavía: falta instalar su navegador de Playwright |
| 1 Cliente | 2026-10-26 | Bienvenida, buscar/pedir/votar, cola con «Tu canción», espera por canción, desempate y «Tus pedidos», estados cerrados (fuera de horario, pausa, mantenimiento), cupo agotado, YouTube caído, resultados de caché, política de datos, entrada por QR, banner de desconexión con sondeo de respaldo. PB-03, 04, 06–09, 13–15, 17–19, 21, 22, 30 | 65 pruebas de lógica; 22 e2e con axe en cada estado y objetivos de 44 px; capturas a 360, 390 y 768 comparadas con el prototipo; crítica de diseño y revisión de accesibilidad hechas y lo encontrado corregido | Miniaturas reales sin probar (la simulación no tiene IDs de YouTube verificados) |
| 2 Pantalla del bar | 2026-10-26 | TV con el video en el arco (YouTube IFrame API o reproductor simulado), lo que suena con su origen, lo que viene con ↑N y espera, placa del QR, estados cerrado/mantenimiento/pausa/cola vacía/sin conexión, «Encender la pantalla», lienzo escalado. PB-17–20, 38, 02 | 8 e2e (axe, sincronía teléfono → TV < 3 s, avance al reportar el fin, el QR cabe a 1280 × 720, 1366 × 768, 1920 × 1080 y 3840 × 2160); crítica de diseño; barrido con 10 correcciones (sobre todo del reproductor de YouTube) | El reproductor de YouTube real no se probó con videos de verdad (faltan IDs verificados); la clave de la pantalla sigue `TBD` |
| 3 Panel operativo | 2026-10-26 | Entrar (error genérico, bloqueo por intentos, cambio de clave forzado, sesión caducada), operación de la noche fiel al prototipo (franja en 4 estados, lo que suena con Saltar/Pausar/Bloquear, cola con Retirar/Bloquear, registro, métricas, horario, lista de la casa, bloqueadas con Desbloquear), abrir/cerrar la noche, mantenimiento, vista del Operador, diseño a 360 px. PB-10–12, 25, 27, 29, 31–34, 38, 40 | 79 pruebas de lógica; 17 e2e del panel con axe (incluidos los 403 del Operador y su anotación en la bitácora, salir y sesión vencida); barrido con 10 correcciones (salida sin confirmar, sesión que caducaba en silencio, diálogos que arrastraban el motivo, acciones del sistema atribuidas a quien mira) | Valores de bloqueo de cuenta e inactividad *(propuestos)*; matriz D9 sin confirmar |
| 4 Gestión | 2026-10-26 | Horario (tramos que cruzan medianoche, solapes en la fila), listas de la casa con editor (buscar y agregar, ordenar con flechas y teclado, activar con rotación, no borrar la activa), bloqueos vigentes y vencidos, bitácora con filtros y «Cargar más», personal (clave temporal una vez, último Administrador protegido), bar y QR (descargar SVG, hoja para imprimir, regenerar invalida el anterior), eventos, manual por rol con capturas generadas; pestañas de secciones por rol. PB-01, 02, 23, 24, 26, 28, 34–37, 39–42 | Lógica en verde (incluidas fechas en la zona del bar con horario de verano y el desempate de la bitácora); 8 e2e de gestión con axe (foco con teclado en el editor, el Operador no dispara pedidos en rutas ajenas); capturas a 360/768/1440; barrido con 12 hallazgos: bucle de 403 del Operador, eventos y filtros de la bitácora fijos a UTC−5, `yo` sin recargar al cambiar el bar, hoja del QR sin error ni permiso, SVG descargado sin `xmlns`, el tiempo real recargaba 8 recursos por voto, fechas fuera de la zona del bar, doble envío al agregar, avisos que se perdían al guardar, diálogos sin centrar | Quién crea establecimientos; zonas horarias admitidas *(propuesto: las de la lista)* |
| 5 Analítica | 2026-10-27 | Tablero analítico del Administrador: resumen de la noche o el rango (cifras, esperas, lo pedido frente a la lista de la casa, lo que sonó), géneros y franjas horarias, diagnóstico con umbrales configurables, comparación por noches, días o tipos de evento, y recomendaciones que llevan a donde se decide. Contrato propuesto (8 operaciones) y datos de ejemplo de 20 noches simuladas con una cola de verdad. PB-45 a PB-49 | Lógica en verde (las cifras cuadran con los registros, un voto por teléfono, zona del bar, umbrales); 6 e2e del tablero con axe (teclado en las columnas, filtros, umbrales en la bitácora, el Operador no pide nada); paleta validada con la skill dataviz; capturas a 360/1440; barrido con 12 hallazgos (fechas a mano que no se mostraban, filtro de día escondido, votos repetidos, zona fija, bloqueos vencidos, claves repetidas, espera sin dato dibujada como cero, entre otros) | Origen del género (RF-051); umbrales por defecto *(propuestos)*; el pipeline (RF-047, RF-048) es del backend |
| 6 Extras y cierre | 2026-10-27 | Mover el voto desde «Votaste» (PB-50), filtros de la búsqueda por género, duración y popularidad (PB-51), aviso de resultados guardados (PB-54, ya estaba); medición de rendimiento; guía de entrega al backend (doc 11). PB-52 y PB-53 no tienen pantalla | Lógica en verde (bordes de los tramos de duración); 2 e2e nuevos con axe y objetivos de 44 px (foco tras mover el voto, filtros que se combinan y se quitan); compilación real sin la simulación; barrido con 10 hallazgos (filtro de género que se perdía, foco al cancelar, voto en camino, error engañoso, popularidad sin la noche en curso, bordes de duración, anuncio que no sonaba, doc 04) | JS inicial del teléfono por encima de lo propuesto (doc 07); origen del género y la popularidad (doc 04) |
