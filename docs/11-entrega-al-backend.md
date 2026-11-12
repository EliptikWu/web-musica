# 11 — Entrega al equipo de backend

Qué recibe el equipo de backend, cómo se conecta el frontend a su API y qué falta decidir.
El detalle del contrato está en el [doc 04](04-modelo-de-datos-y-contrato.md); este documento
es la puerta de entrada.

## Qué se entrega

| Pieza | Dónde | Para qué |
|---|---|---|
| Contrato propuesto (fuente) | `packages/contrato/src/*.ts` | Esquemas zod de cada cuerpo y respuesta, y el registro de las 67 operaciones (`endpoints.ts`) |
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

## Lo que el frontend no hace

- Llamar a YouTube: la búsqueda pasa por el backend (cuota, caché y claves, RF-008, PB-54).
- El pipeline de analítica (RF-047, RF-048) ni los agregados del tablero (ADR-18).
- Sonido del local (PB-52) y respaldo de las más votadas (PB-53): no tienen pantalla.
- Registrar los eventos de uso (RF-046), incluido el cambio de voto.

## Pendientes que necesitan respuesta

Están numerados en el doc 04, «Pendiente del backend». Los que más afectan a la interfaz:
matriz del Operador (D9), estado `retirada`, origen del género y de la popularidad, y los
valores de bloqueo de cuenta. El tiempo real (SSE), la pantalla del bar (RF-058) y el cupo
(RN-01) quedaron resueltos el 2026-11-10.
