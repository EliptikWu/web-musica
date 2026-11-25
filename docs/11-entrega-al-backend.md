# 11 — Entrega al equipo de backend

Qué recibe el equipo de backend, cómo se conecta el frontend a su API y qué falta decidir.
El detalle del contrato está en el [doc 04](04-modelo-de-datos-y-contrato.md); este documento
es la puerta de entrada.

## Qué se entrega

| Pieza | Dónde | Para qué |
|---|---|---|
| Contrato propuesto (fuente) | `packages/contrato/src/*.ts` | Esquemas zod de cada cuerpo y respuesta, y el registro de las 88 operaciones (`endpoints.ts`) |
| OpenAPI generado | `packages/contrato/generado/openapi.json` | Lo mismo en formato estándar, para generar servidor, clientes o pruebas |
| Resumen legible | `packages/contrato/generado/endpoints.md` | Una tabla por superficie: método, ruta, rol y errores |
| Reglas y decisiones | Doc 04 (entidades, estados, permisos, eventos, errores) y doc 03 (ADR) | Por qué el contrato es así |
| Pendientes | Doc 04, «Pendiente del backend», y README, «Decisiones abiertas» | Lo que el frontend no puede decidir solo |

Todo es **propuesta**: lo que el backend cambie se corrige en `packages/contrato`, se
regenera con `npm run generar` y la compilación y las pruebas dicen qué se rompió en la
interfaz.

## Cómo habla el frontend con la API

- **Mismo origen.** El navegador solo llama a `/api/*` de la propia app. La ruta
  `frontend/web/app/api/[...path]/route.ts` reenvía a `ROCKOLA_API_URL` (por ejemplo,
  `/api/bares/demo/ahora` → `${ROCKOLA_API_URL}/bares/demo/ahora`), con el método, el cuerpo,
  la consulta y las cabeceras. La CSP solo permite `connect-src 'self'`.
- **Cabeceras propias.** `X-Rockola-Dispositivo` (UUID anónimo del teléfono) en todo lo del
  cliente; `Idempotency-Key` en las operaciones marcadas `idempotente` (pedir, votar, mover el
  voto, los reportes de la pantalla y, en el panel, saltar, retirar, bloquear y crear un
  usuario): la misma clave tiene que devolver la misma respuesta.
- **Sesión del panel.** Cookie `httpOnly` que pone el backend al entrar (el frontend no la
  lee). Al ir por el mismo origen, es de primera parte. Un 401 en cualquier pantalla lleva a
  `/panel/entrar?motivo=caducada`.
- **Errores.** Siempre `{ codigo, mensaje, detalle? }` con los códigos del doc 04; la
  interfaz decide el texto por `codigo`, nunca muestra `mensaje` al público.
- **Fechas.** ISO 8601 con zona. Cada foto en vivo trae `servidor_ahora` para corregir el
  reloj del teléfono.
- **Tiempo real.** SSE en `GET /bares/{slug}/eventos`: primero una foto `ahora` completa,
  después `ahora`, `panel` (los recursos que cambiaron) y `ping` cada 15 s. Sin SSE, la
  interfaz sondea cada 3 s.
- **Validación.** El cliente valida cada respuesta con el esquema del contrato: una
  respuesta que no cumple es un error en la interfaz, no un dato a medias.

## Cómo se conecta

1. Compilar con `NEXT_PUBLIC_API_MODE=real` (se fija al compilar: la simulación queda fuera
   del paquete; medido el 2026-10-27, no queda nada de MSW en los archivos).
2. Arrancar con `ROCKOLA_API_URL` apuntando a la API. Si delante hay un proxy de confianza
   que reescribe `X-Forwarded-*`, añadir `ROCKOLA_CONFIAR_PROXY=1`; si no, esas cabeceras
   no se reenvían y el backend no debe usar la IP para limitar intentos.
3. Probar a mano los recorridos del doc 09 (pedir, votar, mover el voto, la TV, el panel).
   Las pruebas automáticas de extremo a extremo corren contra la simulación (usan sus
   escenarios por URL): sirven para la interfaz, no para la API.
4. Para comprobar la API contra el contrato, validar sus respuestas con los esquemas de
   `packages/contrato` o con el OpenAPI.

## La app nativa (Android)

Además de la web hay una app Android para el cliente (ADR-19), en `frontend/movil` (ADR-24). Usa
las mismas operaciones de la superficie «Cliente» de `endpoints.md` y valida cada respuesta
con los mismos esquemas, así que para el backend no cambia nada, salvo esto:

- **Llama directo a la API**, sin el proxy de Next: la URL base se fija al compilar
  (`-Prockola.apiUrl=https://…/api`). Con HTTPS en producción; HTTP solo en depuración.
- **Mismas cabeceras:** `X-Rockola-Dispositivo` (UUID que guarda la app, fuera de las copias de
  seguridad) e `Idempotency-Key` en lo idempotente. El SSE va sin cabeceras propias, como
  `EventSource` en la web.
- **Una conexión SSE por teléfono**, compartida por todas sus pantallas; si se corta, la app
  reintenta (1, 2, 4, 8 y 15 s) y mientras tanto pregunta `GET …/ahora` cada 3 s.
- **El QR:** la app se abre con `https://<dominio>/q/<token>` y `/b/<slug>`; si no está
  instalada, abre la web, que funciona igual (sin instalar ni registrarse). El dominio y
  `assetlinks.json` los publica el frontend (doc 04, «Pendiente del backend», punto 11).
- **Espera estimada:** cada canción de la cola trae `espera_seg` (ADR-20).

## La pantalla del bar (RF-058)

La TV ya no reporta sin permiso. Lo que el backend tiene que construir (doc 04, «Pantalla
del bar», y `endpoints.md`):

- `GET` y `POST /bares/{slug}/pantalla/vinculo`: el `POST` pone la cookie httpOnly
  `rockola_pantalla` y devuelve un código de 6 caracteres (sin `0`, `O`, `1`, `I`) que vence
  a los 10 min *(propuesto)*.
- `GET/POST /panel/pantallas` y `DELETE /panel/pantallas/{id}`, solo para el Administrador,
  con `vincular_pantalla` y `desvincular_pantalla` en la bitácora y `pantallas` en el
  evento `panel`.
- Los tres reportes de reproducción responden `401 no_autenticado` sin una pantalla vinculada
  a ese bar, y la sesión se comprueba antes de la caché de `Idempotency-Key`.

## Lo que el equipo de frontend hizo en el backend (D14)

La base de Railway está apagada y sin acceso. Estos cambios se probaron con pruebas
unitarias y levantando el backend contra un Postgres 16 en Docker cargado con
`src/test/resources/esquema-reconstruido.sql`: un esquema **reconstruido del código** (cada
supuesto lleva su comentario) que puede no coincidir con la base real. Revisarlos antes de
desplegar. Para repetir la prueba: cargar el esquema, poner las claves demo con
`UPDATE public.usuario SET password_hash = crypt('rockola-demo', gen_salt('bf', 10))`
(extensión `pgcrypto`) y arrancar con `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y
`ROKOLA_DEVICE_HMAC_SECRET` apuntando a esa base. En una base vacía la V2 falla: Flyway solo
hace la línea base sobre una base que ya tiene tablas.

| Fecha | Qué | Archivos | Para revisar |
|---|---|---|---|
| 2026-11-12 | `tema` del bar (RF-059) y edición del establecimiento (RF-034) | `db/migration/V2__tema_establecimiento.sql`, `compat/ContratoEstablecimientoService.kt`, `compat/ContratoPanelController.kt`, `compat/ContratoQrBarController.kt` | Que `accion_admin.tipo` admita `editar_establecimiento` y que `detalle` sea JSONB; que la base real no tenga ya otra V2 de Flyway. Los teléfonos y la TV abiertos no reciben aviso del cambio: lo toman al recargar |
| 2026-11-12 | Pausa de lo que suena (RF-040), quitar mi voto (RF-060), retirar mi pedido (RF-061) y top 3 en `destacadas` | `db/migration/V3__pausa_reproduccion.sql`, `reproduccion/ReproduccionReloj.kt`, `reproduccion/ReproduccionConsultaRepository.kt`, `compat/ContratoPausaService.kt`, `compat/ContratoParticipacionService.kt` y su controlador, `compat/ContratoEstadoService.kt`, `compat/ContratoPantallaService.kt`, `config/SecurityConfig.kt` | Que `accion_admin.tipo` admita `pausar_reproduccion` y `reanudar_reproduccion`, y `evento_analitico.nombre_evento` los eventos `quitar_voto` y `retirar_pedido`. Se cambió `ReproduccionConsultaRepository`, que usa también el flujo antiguo |
| 2026-11-12 | Volumen de la pantalla desde el panel (RF-062) | `db/migration/V4__volumen_pantalla.sql`, `compat/ContratoVolumenService.kt`, `compat/ContratoPanelController.kt`, `compat/ContratoEstadoService.kt`, `compat/ContratoNocheService.kt` | Que `accion_admin.tipo` admita `cambiar_volumen`. El volumen sale en la misma consulta del bar de `ahora`; un evento `volumen` no despierta la reproducción automática (`ReproduccionAutomaticaService`) |
| 2026-11-12 | Rapidez de la búsqueda: consultas en lote, la foto de la cola solo si coincide, caché de 6 h (`youtube.cache.horas`) | `compat/ContratoBusquedaService.kt`, `youtube/YoutubeCacheRepository.kt` | Confirmar las 6 h *(propuesto)* frente a la cuota de YouTube; el autocompletado sigue en 30 min. Medición en el doc 07 |
| 2026-11-21 | Gestión del panel: horario (RF-036), usuarios y clave propia (RF-023 a RF-026), bloqueos (RF-042, RF-043), listas de la casa y búsqueda del panel (RF-027 a RF-033). 21 rutas que antes caían en el 404 «aún no existe» | `db/migration/V5__gestion_panel.sql`, `compat/ContratoGestionController.kt`, `compat/ContratoHorarioService.kt`, `compat/ContratoUsuariosService.kt`, `compat/ContratoBloqueosService.kt`, `compat/ContratoListasService.kt`, `compat/ContratoCanciones.kt`, `compat/ContratoBitacora.kt`, `compat/ContratoCuerpo.kt`; cambios en `ContratoNocheService.kt` (bloqueos y nombre del objetivo en la bitácora), `ContratoPanelSesiones.kt` (cerrar las sesiones de un usuario) y `ContratoError.kt` | **La V5 ya está aplicada en Railway** (el 2026-11-21 la aplicó por error `RokolaBackendApplicationTests`, que arranca la aplicación con el `.env`): no se puede cambiar el archivo, un ajuste va en una V6. La V5 deja borrar un elemento de lista que ya sonó (`reproduccion.id_elemento` pasa a `ON DELETE SET NULL` y el CHECK de un solo origen se afloja) y guarda la duración del bloqueo. «Hasta el fin de la noche» termina al cierre de la franja vigente, o a las 12 h sin franja *(propuesto)*. `crearUsuario` no guarda la respuesta en `api_idempotencia` (llevaría la clave temporal en claro): repetir la petición da 409. `ultimo_acceso` sigue en `null` (la tabla no lo guarda) |
| 2026-11-21 | Pausar y reanudar solicitudes (RF-036, RF-039), modo mantenimiento (RF-038) y bitácora completa con filtros y páginas (RF-044) | `compat/ContratoServicioService.kt`, `compat/ContratoBitacora.kt` (ahora también lee), `compat/ContratoGestionController.kt`, `compat/ContratoNocheService.kt` (usa la misma lectura) | Sin migración. **Reanudar abre una noche si no hay** (ningún código del backend abre ni cierra noches; doc 04, punto 18). Que `accion_admin.tipo` admita `pausar_solicitudes`, `reanudar_solicitudes`, `forzar_apertura`, `mantenimiento` y `abrir_sesion`. `forzado_activo` no abre fuera del horario |
| 2026-11-21 | Abrir y cerrar la noche a mano (RF-037) y calendario de eventos (RF-045) | `compat/ContratoServicioService.kt`, `compat/ContratoEventosService.kt`, `compat/ContratoGestionController.kt`, `compat/ContratoEstadoService.kt` | Sin migración. Que `accion_admin.tipo` admita `cerrar_sesion` y `editar_evento`. **Cambio de comportamiento en `ahora`:** sin noche abierta y fuera del horario, `servicio.estado` es `fuera_de_horario` y ya no `en_pausa` (doc 04, punto 19). Un evento dura menos de 24 h: lo que cabe en `evento_calendario` |
| 2026-11-21 | Los eventos cambian el horario de su día y cada noche se liga a su evento (RF-045, RF-036, RF-037) | `acceso/HorarioEfectivoService.kt` (nuevo), `acceso/HorarioActivoService.kt` (delega en él), `compat/ContratoEstadoService.kt`, `compat/ContratoServicioService.kt`, `compat/ContratoEventosService.kt`, `compat/ContratoBloqueosService.kt`, `compat/ContratoHorarioService.kt` | Sin migración. **Cambia cuándo el bar está «en horario»** para pedir y para la reproducción automática: un evento suma horas y nunca quita (doc 04, punto 20). Se reemplazó la lógica de `HorarioActivoService`, que usa también el acceso por QR antiguo |

El 2026-11-21 la base de Railway ya estaba en línea. La gestión del panel se probó contra una
**copia de su estructura** (`pg_dump --schema-only`, sin datos) en un Postgres 18 de Docker:
124 comprobaciones HTTP con los dos roles y dos bares (permisos, reglas, bitácora, que un bar
no vea lo del otro) y 29 respuestas validadas con los esquemas zod del contrato. La estructura
real tiene índices únicos que el esquema reconstruido no tenía (por ejemplo, una sola lista
activa por bar): probar contra la copia real los encontró. Ojo: `./gradlew test` arranca la aplicación con el `.env`; para no tocar
la base real, correr las pruebas con `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` apuntando a una
base local (las variables de entorno ganan al `.env`).
| 2026-11-20 | Capa de Big Data (D15, ADR-27): pipeline con PySpark, modelo `analitica`, las 9 rutas del tablero y el cubo de KPI (RF-063) | `pipeline/`, `db/migration/V6__modelo_analitico.sql`, `compat/ContratoAnaliticaService.kt`, `compat/ContratoAnaliticaController.kt`, `compat/ContratoError.kt` | Que `cancion.categoria` traiga el género, quién programa el pipeline al cerrar la noche y dónde se guarda `ROCKOLA_SEUDONIMO_CLAVE`. Doc 04, pendiente 18 |
| 2026-11-21 | Migración de la pantalla del bar y del QR; abrir y cerrar la noche a mano (RF-037) | `db/migration/V7__pantalla_y_noche_manual.sql`, `compat/ContratoServicioService.kt`, `compat/ContratoPanelController.kt`, `compat/ContratoEstadoService.kt`, `acceso/HorarioActivoService.kt` y quienes lo llaman | Que las columnas de `pantalla_vinculo` coincidan con Railway y que `accion_admin.tipo` admita `abrir_sesion` y `cerrar_sesion`. El horario se consulta ahora con `atiende()`, que también acepta una noche abierta a mano. Doc 04, pendientes 19 y 20 |
| 2026-11-21 | Ciclo de la noche por horario, pausa de solicitudes y mantenimiento (RF-036 a RF-039) | `compat/ContratoServicioService.kt`, `compat/ContratoVersionService.kt`, `tiemporeal/ContratoSseService.kt`, `compat/ContratoNocheService.kt`, `db/migration/V8__ciclo_de_la_noche.sql` | Con varias instancias del backend el ciclo correría en cada una: el candado por bar evita noches dobles. Doc 04, pendiente 20 |
| 2026-11-21 | Plataforma de superadmin (RF-064, ADR-29) | `db/migration/V10__superadmin.sql`, `compat/ContratoPlataformaService.kt`, `compat/ContratoPlataformaController.kt`, `config/SecurityConfig.kt` | Definir en Railway `ROCKOLA_SUPERADMIN_CORREO` y `ROCKOLA_SUPERADMIN_CLAVE` antes del primer ingreso; crear un bar exige `ROKOLA_QR_BASE_URL`. Las sesiones viven en memoria: reiniciar las cierra |
| 2026-11-21 | QR del panel y destino web o app (RF-035, RF-065, ADR-30) | `db/migration/V11__destino_qr.sql`, `compat/ContratoQrPanel.kt`, `compat/ContratoQrBarController.kt`, `compat/ContratoPantallaService.kt` | Publicar el APK y definir `ROKOLA_APK_URL` (https) y `ROKOLA_WEB_URL` (la web, sin ruta). `ROKOLA_QR_BASE_URL` debería ser también la web |
| 2026-11-21 | Dirección y horario de cada bar en la lista de la plataforma (RF-064) | `compat/ContratoPlataformaService.kt` | Sin migración y solo lectura. Compilado, sin probar contra una base. Lee las columnas que ya usan `ContratoEstablecimientoService` y `ContratoHorarioService` |
| 2026-11-24 | Errores con forma de contrato otra vez en la analítica, la plataforma y el QR del panel (D21) | `compat/ContratoError.kt` | Los PR #11 a #14 cambiaron la lista de `ContratoErrorAdvice` y sacaron `ContratoAnaliticaController`, `ContratoPlataformaController` y `ContratoQrPanelController`: sus 400, 401 y 403 salían sin `{ codigo, mensaje }` y el panel no sabía que la sesión había vencido. Se devolvieron a la lista junto a `ContratoGenerosController`. Compilado, sin probar contra una base. Doc 04, pendiente 26 |
| 2026-11-25 | El volumen ya no va a la bitácora; el error de YouTube nombra la canción (RF-062, RF-019) | `compat/ContratoVolumenService.kt`, `compat/ContratoBitacora.kt`, `compat/ContratoPantallaService.kt`, `compat/ContratoNocheService.kt` | Sin migración. El volumen no escribe `accion_admin` y la bitácora oculta `cambiar_volumen` sin filtro. El reporte `error` de la TV anota `error_youtube` con la canción, y la alerta `errores_youtube` del panel trae hasta tres títulos en `detalle`. Prueba `ErroresYoutubeTest` |
| 2026-11-25 | La música sigue con las solicitudes en pausa (D24, RN-08) | `reproduccion/ReproduccionAutomaticaService.kt` | La reproducción automática corre con `estado_manual` `disponible` o `pausado` (antes solo `disponible`): pausar solicitudes ya no callaba la cola ni la lista de la casa. Mantenimiento y fuera de horario siguen sin música |

## Lo que el frontend no hace

- Llamar a YouTube: la búsqueda pasa por el backend (cuota, caché y claves, RF-008, PB-54).
- Sonido del local (PB-52) y respaldo de las más votadas (PB-53): no tienen pantalla.
- Registrar los eventos de uso (RF-046), incluido el cambio de voto.

## Pendientes que necesitan respuesta

Están numerados en el doc 04, «Pendiente del backend». Los que más afectan a la interfaz:
matriz del Operador (D9), estado `retirada`, origen del género y de la popularidad, y los
valores de bloqueo de cuenta. El tiempo real (SSE), la pantalla del bar (RF-058) y el cupo
(RN-01) quedaron resueltos el 2026-11-10.
