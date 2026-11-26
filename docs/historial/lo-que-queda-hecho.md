# Lo que queda: hecho

| Fecha | Qué | Dónde |
|---|---|---|
| 2026-11-25 | Puntos 2 a 20 de la lista de errores del equipo | Commits `76bed1e` a `4892463`; doc 08, iteración 34 |
| 2026-11-25 | Escáner de Android al abrir la app, «Cambiar de bar» y simulación con QR | Commits `2fe079d`, `f038eae`; doc 08, iteraciones 32 y 33 |
| 2026-11-25 | Auditoría de los docs: el doc 04 decía pendiente lo ya resuelto (`retirada`, D16, D24) y el teléfono Android prometía la lista de la casa fuera de horario | Este commit |
| 2026-11-25 | T1: la prueba de arranque del backend se salta sin `DB_URL` y pasa contra Postgres 16 (Flyway llega a V14) | `RokolaBackendApplicationTests.kt` |
| 2026-11-25 | T2: los avisos de lint de `hooks-panel.ts` (el `eslint-disable` estaba en la línea equivocada) | `frontend/web/lib/api/hooks-panel.ts` |
| 2026-11-25 | T3: docs 05 y 08 dentro del presupuesto; lo cerrado en `historial/` | `08-iteraciones-cerradas.md`, `05-reworks-anteriores.md` |
| 2026-11-25 | T4: el backend contra Postgres local: V14 guarda el último ingreso, el volumen no escribe la bitácora, reclasificar responde con la forma del contrato y guardar el horario responde 200 | Doc 11 |
| 2026-11-25 | T5: el doc 04 dentro del presupuesto (los puntos 13 a 26 de «Pendiente del backend», con su detalle, en `historial/04-hecho-para-el-backend.md`; los números se conservan). `CLAUDE.md` de ~3 500 a ~2 700 tokens: la identidad visual remite al doc 05, sin quitar ninguna regla | Con visto bueno del usuario |
| 2026-11-25 | Persona #5: la rama `correcciones-del-equipo` quedó fusionada (PR #16) | `4208524` |
| 2026-11-25 | Segunda lista del equipo (12 puntos) | Commits `f9a6b0b` a `d8f6ba2`; doc 08, iteración 35 |
| 2026-11-25 | T6: el ícono de la app Android con el logo (la R en el arco de luz) | `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`; doc 05 |
| 2026-11-25 | Prueba de carga con k6 y el arreglo del pool de conexiones de los avisos SSE (parte de T8 y T10) | `scripts/carga/`, `ContratoSseService.kt`; doc 07 |
| 2026-11-25 | T9: migración V15 de `api_idempotencia` | Doc 11 |
