# 04 — Modelo de datos y contrato con el backend

El modelo de datos lo definió el equipo (`fuente/extraido/prototipos-arquitectura-y-modelado-de-datos.md`,
§2). Este doc recoge **lo que el frontend necesita del backend**: qué entidades y estados
espera, qué endpoints llama y qué eventos escucha. Es una **propuesta**: se acuerda con
el equipo de backend y, cuando ellos publiquen su OpenAPI, manda el suyo y se ajusta
`packages/contrato`.

- Fuente exacta (tipos y validaciones): `packages/contrato/src/`.
- Versión para el backend: `packages/contrato/generado/openapi.json` y
  `generado/endpoints.md`, regenerados con `npm run generar -w @rockola/contrato`.

## Convenciones

| Tema | Regla |
|---|---|
| Campos | `snake_case` en español, como el diccionario de datos |
| Fechas | ISO 8601 con zona (`2026-10-22T22:45:00-05:00`); el servidor manda `servidor_ahora` para que el cliente corrija su reloj |
| Duraciones | Segundos enteros (`duracion_seg`, `posicion_seg`) |
| Zona horaria | La del establecimiento (`America/Bogota`); el cliente formatea con ella |
| Teléfono | Cabecera `X-Rockola-Dispositivo: <uuid>` en las rutas del cliente. El servidor guarda solo su SHA-256 (RF-002) |
| Reintentos | `Idempotency-Key: <uuid>` en `POST` de solicitudes y votos: la misma clave devuelve la misma respuesta (RNF-008) |
| Errores | `{ "codigo": "...", "mensaje": "...", "detalle": {...}? }` con los códigos de abajo; el cliente traduce `codigo` a su texto |
| Sesión del panel | Cookie httpOnly que pone el backend; `401 no_autenticado` si falta o caducó |
| Pantalla del bar | Cookie httpOnly `rockola_pantalla` que pone el backend al pedir el código (RF-058, ADR-23); los reportes responden `401 no_autenticado` si falta, no está vinculada o se desvinculó. *(Propuesto:* `Secure`, `SameSite=Strict`, `Path=/api/bares/{slug}/pantalla`, un año, renovable*)* |
| Tiempo real | SSE en `GET /bares/{slug}/eventos` (ADR-07) |

## Entidades que ve el frontend

Nombres del diccionario; solo se listan los campos que la interfaz usa.

| Entidad | Campos | Notas |
|---|---|---|
| `Cancion` | `video_id`, `titulo`, `artista`, `duracion_seg`, `miniatura_url` | `artista` se deriva del canal o del título: `TBD` cómo |
| `Bar` (público) | `slug`, `nombre`, `zona_horaria`, `pedidos_maximos` (3), `duracion_max_seg` (600), `ventana_no_repeticion_min` (60), `tema` (`rockola`), `horario[]` | De `Establecimiento` y `Horario`. `tema`: el diseño con que se ve el teléfono y la TV (RF-059) |
| `Franja` (horario) | `dia_semana` (1 = lunes … 7 = domingo), `hora_inicio`, `hora_fin` (`"20:00"`, `"02:00"`), `activo` | Una franja que cruza medianoche pertenece al día en que empieza |
| `Servicio` | `estado`, `modo_qr`, `desde`, `hasta`, `proxima_apertura` | Ver «Estado del servicio» |
| `Reproduccion` | `id`, `cancion`, `origen` (`solicitud` \| `lista_casa`), `solicitud_id`, `votos`, `inicio`, `posicion_seg`, `estado` (`sonando` \| `pausada`) | El avance se calcula con `posicion_seg` y el reloj del servidor |
| `ItemCola` | `solicitud_id`, `cancion`, `votos_total`, `fecha_solicitud`, `puesto`, `puesto_anterior`, `espera_seg` | Ya ordenada (RN-05). `puesto_anterior` alimenta «Subió 2 puestos» igual en todas las pantallas. `espera_seg`: espera estimada en `servidor_ahora`, lo que le falta a lo que suena más lo que duran las de delante; el cliente le descuenta lo que avanza lo que suena (RF-018, ADR-20) |
| `Ahora` | `version`, `servidor_ahora`, `servicio`, `reproduccion`, `cola[]`, `lista_casa`, `volumen` | La foto que se envía por tiempo real. `volumen` (0–100, RF-062) lo aplica la TV |
| `MiSolicitud` | `solicitud_id`, `cancion`, `estado`, `fecha_solicitud`, `votos_total`, `puesto`, `motivo` | Para «Tus pedidos» (RF-013). `retirada` sin `motivo` = la retiró ese teléfono (RF-061); el bar siempre da motivo (RF-041) |
| `Yo` (teléfono) | `pedidos_maximos`, `pedidos_restantes`, `solicitudes[]`, `votos[]` (ids), `aviso_visto_en` | |
| `UsuarioPanel` | `id`, `nombre`, `correo`, `rol`, `estado`, `debe_cambiar_clave`, `ultimo_acceso` | Nunca contraseñas ni hashes |
| `YoPanel` | `usuario`, `establecimiento` (`slug`, `nombre`, `zona_horaria`) | La zona formatea todas las horas del panel |
| `Noche` | `sesion`, `servicio`, `reproduccion` (+ `pedida_por`, `pedida_en`), `cola` (+ `pedida_por`, `duracion_seg`), `metricas`, `registro`, `horario`, `lista_casa`, `bloqueos`, `alertas` | Una sola respuesta para la operación; `pedida_en` da «pedida hace 31 min» |
| `Bloqueo` | `id`, `cancion`, `motivo`, `duracion` (`fin_sesion` \| `horas` \| `indefinido`), `inicio`, `fin`, `creado_por` | |
| `AccionAdmin` | `id`, `fecha`, `usuario` (o `null` = sistema), `tipo`, `objetivo`, `detalle` | La interfaz arma la frase («Bloqueaste «La Bachata», de Manuel Turizo») |
| `ListaCasa` | `id`, `nombre`, `modo_rotacion` (`secuencial` \| `aleatoria`), `activa`, `total_canciones`, `duracion_total_seg`, `canciones[]` | `aleatoria` = sin repetir hasta agotar la lista |
| `Establecimiento` | `id`, `slug`, `nombre`, `direccion`, `zona_horaria`, `estado`, límites, `tema` | `tema`: `rockola` \| `vidrio` \| `afiche`; por defecto `rockola`. Se cambia con `PATCH /panel/establecimiento` (solo Administrador) y queda en la bitácora como `editar_establecimiento` (RF-059) |
| `EventoBar` | `id`, `nombre`, `tipo_evento`, `inicio`, `fin`, `descripcion` | Calendario (RF-045) |

### Correcciones propuestas al modelo

| Qué | Por qué |
|---|---|
| Estado `retirada` en `Solicitud`, con `motivo_retiro` | RF-041 (retirar con motivo) y RF-013 lo necesitan; el modelo no lo tiene |
| `no_disponible` también desde `pendiente` | RF-007 pide revalidar antes de reproducir |
| `estado` de la reproducción: `sonando` \| `pausada` | RF-040 pide pausar y reanudar |
| Roles `administrador` y `operador` | El modelo dice `admin`/`dj`; los requisitos, Administrador/Operador |
| `puesto_anterior` en la cola | Para que «Subió 2 puestos» sea igual en el teléfono, la TV y el panel |
| `AccionAdmin.tipo` ampliado | Añade `retirar`, `pausar_reproduccion`, `reanudar_reproduccion`, `abrir_sesion`, `cerrar_sesion`, `activar_lista`, `crear_usuario`, `editar_usuario`, `editar_establecimiento`, `regenerar_qr`, `error_youtube`, `intento_no_permitido` |

## Estados de una solicitud

| Estado | Llega desde | Causa | Lo que ve el cliente |
|---|---|---|---|
| `pendiente` | creación | Se aceptó la solicitud | «En espera», con puesto y tiempo |
| `reproduciendo` | pendiente | Es la siguiente | «Sonando» |
| `reproducida` | reproduciendo | El video terminó | «Ya sonó» |
| `saltada` | reproduciendo | El personal la saltó | «Omitida» |
| `no_disponible` | pendiente, reproduciendo | El video dejó de estar disponible o el reproductor dio error | «No se pudo reproducir» |
| `bloqueada` | pendiente | El personal bloqueó la canción | «Bloqueada por el bar» |
| `retirada` | pendiente | El personal la retiró con un motivo | «Retirada por el bar» |
| `expirada` | pendiente | Mantenimiento o cierre de la sesión | «La noche cerró antes de que sonara» |

Los textos definitivos salen de `design:ux-copy` y viven en `frontend/web/lib/textos`.

## Estado del servicio

| `estado` | Cuándo | Cliente | Pantalla del bar |
|---|---|---|---|
| `abierto` | Dentro del horario o forzado activo | Pide y vota | Luz encendida |
| `fuera_de_horario` | Fuera del horario | Ve lo que suena y el horario; no pide ni vota | Luz apagada, horario y QR |
| `en_pausa` | El personal pausó las solicitudes (RF-039) | Ve la cola; no pide ni vota | Luz apagada, «En pausa» |
| `mantenimiento` | Modo mantenimiento (RF-038) | Aviso; no pide ni vota | Aviso |

`modo_qr` (`automatico` \| `forzado_activo` \| `pausado`) es lo que el Administrador elige;
`estado` es el resultado de combinarlo con el horario (RN-09).

## Permisos

Propuesta D9, pendiente de confirmar. El servidor aplica la matriz; la interfaz la usa
para ocultar lo que no se puede (`frontend/web/lib/permisos.ts`).

| Acción | Operador | Administrador |
|---|---|---|
| Ver la operación de la noche (cola, lo que suena, registro, métricas) | Sí | Sí |
| Saltar, pausar y reanudar la reproducción; volumen de la pantalla (RF-062) | Sí | Sí |
| Pausar y reanudar las solicitudes | Sí | Sí |
| Retirar una solicitud | Sí | Sí |
| Bloquear hasta el fin de la sesión | Sí | Sí |
| Bloquear por horas o indefinidamente; desbloquear; extender | No | Sí |
| Modo mantenimiento; abrir y cerrar la sesión a mano | No | Sí |
| Horario, listas de la casa, usuarios, establecimiento y QR, eventos | No | Sí |
| Vincular y desvincular pantallas del bar | No | Sí |
| Bitácora completa y tablero analítico | No | Sí |
| Manual de uso | Su parte | Completo |

## Endpoints

Rutas relativas a la base de la API. En el navegador siempre van por `/api` del mismo
origen. El detalle de cuerpos y respuestas está en `generado/endpoints.md`.

### Cliente (público, con `X-Rockola-Dispositivo`)

| Método y ruta | Para qué | RF |
|---|---|---|
| `GET /qr/{token}` | Resolver el QR al `slug` del bar (`410 qr_reemplazado` si se regeneró) | 001, 035 |
| `GET /bares/{slug}` | Datos públicos del bar y su horario | 001, 003 |
| `GET /bares/{slug}/ahora` | Foto actual: servicio, lo que suena, cola | 003, 015–018 |
| `GET /bares/{slug}/destacadas` | «Lo más votado de la noche»: **las tres primeras** (con más, la bienvenida obligaba a deslizar; corrección del 2026-11-12) | 015 |
| `POST /bares/{slug}/dispositivo` | Registrar el teléfono (`tipo` por ancho) y devolver `Yo` | 002 |
| `GET /bares/{slug}/dispositivo` | `Yo`: cupo, mis solicitudes, mis votos | 011, 013 |
| `POST /bares/{slug}/dispositivo/aviso` | Registrar que vio el aviso de datos | 004 |
| `GET /bares/{slug}/busqueda?q=` | Resultados de YouTube + coincidencias en la cola. Cada resultado trae `genero` (`null` = sin clasificar) y `pedidas_en_el_bar` (veces pedida en el bar en las últimas cuatro semanas) para los filtros, que el teléfono aplica sin volver a buscar *(propuesto)* | 005–008, 055 |
| `POST /bares/{slug}/solicitudes` | Pedir (`{ video_id }`); si ya está, cuenta como voto (RN-02) | 009 |
| `POST /bares/{slug}/solicitudes/{id}/votos` | Votar | 010–012 |
| `PUT /bares/{slug}/dispositivo/voto` | Mover un voto (`{ desde, hacia }`): las dos en espera, la de origen votada y no propia, la de destino sin votar. Errores: `no_pendiente`, `ya_votaste`, `datos_invalidos`, `servicio_no_disponible`. El backend lo registra como evento de cambio de voto (RF-046) | 014 |
| `DELETE /bares/{slug}/solicitudes/{id}/votos` | Quitar mi voto de una en espera que pidió otro. Idempotente. Errores: `no_pendiente`, `datos_invalidos` (es tu propio pedido), `servicio_no_disponible`. Evento `quitar_voto` | 060 |
| `DELETE /bares/{slug}/solicitudes/{id}` | Retirar mi pedido si no suena: queda `retirada` **sin motivo**, sus votos se desactivan y vuelve el cupo; responde `{ solicitud_id, pedidos_restantes }`. Idempotente. Errores: `sin_permiso` (no es tuyo), `no_pendiente`, `servicio_no_disponible`. Evento `retirar_pedido` | 061 |
| `GET /bares/{slug}/eventos` | Tiempo real (SSE) | 017 |

### Pantalla del bar

| Método y ruta | Para qué | RF |
|---|---|---|
| `GET /bares/{slug}/ahora` | La misma foto | 015–018 |
| `GET /bares/{slug}/qr-publico` | URL que codifica el QR de la placa | 035 |
| `POST /bares/{slug}/pantalla/reproducciones/{id}/inicio` · `/fin` · `/error` | Reportar el reproductor (`posicion_seg`, `codigo_youtube`). **Piden una pantalla vinculada** | 007, 019, 020, 058 |
| `GET /bares/{slug}/pantalla/vinculo` | Cómo está esta pantalla según su cookie: `sin_vincular`, `esperando` (con `codigo` y `expira_en`) o `vinculada` (con su `nombre`). La TV pregunta cada 2 s mientras espera y cada 10 s ya vinculada, por si la desvinculan | 058 |
| `POST /bares/{slug}/pantalla/vinculo` | Pedir un código nuevo: pone la cookie `rockola_pantalla` y responde `esperando`. Un código nuevo reemplaza al anterior de esa pantalla | 058 |

Vinculación (RF-058, ADR-23): la TV pide un código de 6 caracteres sin los que se
confunden (`0`, `O`, `1`, `I`), que vence a los 10 min *(propuesto)*; el Administrador lo
escribe en el panel y la cookie de esa TV queda vinculada al bar, solo para estos
reportes. Varias pantallas pueden estar vinculadas a la vez: un reporte con un `id` que ya
no es el vigente no hace nada, así que dos pantallas o un reintento no saltan dos
canciones. Para el backend: un reporte cuyo `{slug}` no es el bar de esa pantalla responde
`401`, y un código solo se confirma desde el panel de su mismo bar; la sesión de la pantalla
se comprueba antes de responder desde la caché de `Idempotency-Key`, y esa caché se separa
por operación (la TV usa claves previsibles, `{id}-{tipo}`). *(Propuesto:* limitar cuántos
códigos pide una misma IP por minuto*)*.

### Panel (cookie de sesión)

| Método y ruta | Para qué | RF |
|---|---|---|
| `POST /panel/sesion` · `DELETE /panel/sesion` | Entrar y salir | 021 |
| `GET /panel/yo` · `PUT /panel/yo/clave` | Usuario actual; cambiar la clave | 021, 023 |
| `GET /panel/noche` | Todo lo de la operación: sesión, servicio, lo que suena, cola con detalle, métricas, registro, horario, lista activa, bloqueos, alertas | 037, 044, 049 |
| `POST /panel/sesiones` · `POST /panel/sesiones/actual/cierre` | Abrir y cerrar la noche a mano | 037 |
| `PUT /panel/servicio` (`{ modo_qr }`) · `POST /panel/servicio/mantenimiento` | Pausar/reanudar solicitudes; mantenimiento | 036, 038, 039 |
| `POST /panel/reproduccion/{id}/salto` · `/pausa` · `/reanudacion` | Control de la reproducción | 040 |
| `PUT /panel/volumen` | Volumen de la pantalla (`{ volumen }`, 0 a 100). Va a la bitácora como `cambiar_volumen` con el valor; sale en `Ahora.volumen` y `Noche.volumen` (80 sin dato) | 062 |
| `POST /panel/solicitudes/{id}/retiro` (`{ motivo, detalle }`) | Retirar de la cola | 041 |
| `GET /panel/bloqueos` · `POST` · `PATCH /{id}` · `DELETE /{id}` | Bloquear, extender, desbloquear | 042, 043 |
| `GET /panel/horario` · `PUT /panel/horario` | Horario semanal | 036 |
| `GET/POST /panel/listas` · `GET/PATCH/DELETE /panel/listas/{id}` · `POST/DELETE …/canciones` · `PUT …/orden` · `POST …/activacion` | Listas de la casa | 027–033 |
| `GET /panel/busqueda?q=` | Buscar para armar listas | 031 |
| `GET/POST /panel/usuarios` · `PATCH /panel/usuarios/{id}` · `POST …/clave-temporal` · `…/desactivacion` · `…/reactivacion` | Personal | 023–026 |
| `GET/PATCH /panel/establecimiento` · `GET …/qr` · `POST …/qr/regeneracion` | Establecimiento y QR | 034, 035 |
| `GET/POST /panel/pantallas` · `DELETE /panel/pantallas/{id}` | Pantallas vinculadas; vincular con `{ codigo, nombre }` (`404 no_encontrado` si el código no existe o venció); desvincular. Quedan en la bitácora (`vincular_pantalla`, `desvincular_pantalla`) | 058 |
| `GET /panel/bitacora?desde&hasta&usuario&tipo&cursor` | Bitácora (incluye errores de YouTube) | 044, 008 |
| `GET/POST /panel/eventos` · `PATCH/DELETE /panel/eventos/{id}` | Calendario de eventos | 045 |
| `GET /panel/analitica/noches` | Noches cerradas que tiene el tablero, con su evento del calendario | 049, 053 |
| `GET /panel/analitica/resumen?noche\|desde&hasta` | Solicitudes, teléfonos únicos, votos, la más votada, esperas (promedio y máxima), tiempo de pedidos frente a lista de la casa, solicitudes que no sonaron; de una noche, el historial | 049, 050 |
| `GET /panel/analitica/participacion?noche\|desde&hasta&dia_semana` | Géneros por solicitudes y votos (`null` = sin clasificar) y franjas horarias con solicitudes, votos y teléfonos | 051 |
| `GET /panel/analitica/diagnostico?…` | Muy votadas que no sonaron con su causa probable, concentración de votos (por noche) y participación por noche, con los umbrales | 052 |
| `GET/PUT /panel/analitica/umbrales` | Umbrales del diagnóstico (`votos_minimos`, `concentracion_pct`, `participacion_minima`); el cambio queda en la bitácora (`editar_umbrales`) | 052 |
| `GET /panel/analitica/comparacion?agrupar=noche\|dia\|evento&noches=…\|desde&hasta` | Indicadores en promedio por noche, por noche, día de la semana o tipo de evento; lo planeado (lista de la casa) frente a lo pedido | 053 |
| `GET /panel/analitica/recomendaciones?…` | Sugerencias con el dato en que se basan y la sección donde se decide; **nunca se aplican solas** | 054 |
| `GET /panel/analitica/cubo?eje=dia\|evento` | Cubo de KPI por género, franja y día o tipo de evento. Cada celda trae solicitudes, votos, usuarios únicos, no sonaron, espera promedio y máxima, segundos de pedidos y de lista de la casa, y la canción más popular. `*` es el miembro «Todos» | 063 |

**Tablero analítico** *(propuesto)*. Todas sus respuestas traen `datos: OrigenDatos`
(`origen` `registrado` o `ejemplo`, `actualizado_en`, `noches`, `desde`, `hasta`), que la
interfaz muestra siempre (RNF-015). Las cifras salen del modelo analítico (RF-047, RF-048):
la noche en curso entra cuando se cierra. Las fechas de los filtros son `AAAA-MM-DD` en la
zona del bar. La concentración de votos se mide **por noche** (en un rango, el promedio):
sumar noches juntaría a los habituales y daría una alerta falsa. Umbrales por defecto
*(propuestos)*: 6 votos, 40 % y 15 teléfonos.

## Eventos de tiempo real

| `tipo` | Contenido | Cuándo |
|---|---|---|
| `ahora` | La foto `Ahora` completa (con `version`) | Cualquier cambio en la cola, los votos, lo que suena o el servicio |
| `panel` | `{ recursos: [...] }` (`noche`, `yo`, `bitacora`, `bloqueos`, `listas`, `horario`, `usuarios`, `establecimiento`, `eventos`, `pantallas`) | Cambió algo del panel; el panel vuelve a pedir **solo** esos recursos. Nombra únicamente los que cambiaron: un voto no hace recargar el personal, y un 403 (que deja rastro en la bitácora) no hace volver a pedir lo rechazado. `yo` cambia con el nombre o la zona del bar y con los datos o el rol de quien mira. Sin datos privados |
| `ping` | `{ servidor_ahora }` | Cada 15 s, para detectar la desconexión |

Al abrir la conexión, el servidor manda **primero una foto `ahora`**: el cliente solo se
considera conectado cuando llegan datos (un canal que acepta y corta no cuenta) y así se
resincroniza al reconectar.

## Códigos de error

| Código | HTTP | Cuándo |
|---|---|---|
| `datos_invalidos` | 400 | El cuerpo o los parámetros no cumplen el esquema |
| `no_autenticado` | 401 | Panel sin sesión o sesión caducada; pantalla sin vincular o desvinculada |
| `credenciales_invalidas` | 401 | Correo o clave incorrectos (mensaje genérico, RF-021) |
| `sin_permiso` | 403 | El rol no permite la acción (queda en la bitácora) |
| `debe_cambiar_clave` | 403 | Primer ingreso o clave restablecida |
| `no_encontrado` | 404 | Bar, solicitud, lista o usuario inexistente |
| `limite_pedidos` | 409 | Ya tiene 3 solicitudes pendientes o sonando (RN-01) |
| `ya_votaste` | 409 | Ya votó por esa solicitud (RN-04) |
| `conflicto` | 409 | Otro cambio llegó antes (nombre de lista repetido, horario solapado…) |
| `qr_reemplazado` | 410 | El QR se regeneró |
| `no_pendiente` | 410 | La solicitud ya sonó, se retiró o expiró |
| `bloqueada` · `no_disponible` · `no_embebible` · `muy_larga` · `reproducida_hace_poco` | 422 | Motivos de RN-06 y RN-07 |
| `servicio_no_disponible` | 423 | Fuera de horario, en pausa o en mantenimiento |
| `cuenta_bloqueada` | 423 | Demasiados intentos fallidos (`detalle.reintentar_en`) |
| `youtube_no_disponible` · `cuota_agotada` | 503 | La búsqueda falló (RF-008) |
| `error_interno` | 500 | Cualquier otro |

## Pendiente del backend (`TBD`)

1. ~~Transporte de tiempo real~~ Resuelto el 2026-11-10: SSE (doc 03, «Tiempo real»).
2. Cuota de YouTube: `search.list` cuesta 100 unidades de 10 000 diarias; el
   autocompletado necesita caché en el servidor y espera entre teclas (el cliente espera
   300 ms). RF-008 y PB-54. El 2026-11-12 la caché pasó de 30 min a 6 h, configurable con
   `youtube.cache.horas` *(propuesto, por confirmar)*; ver doc 07, «Mediciones».
3. Cómo se deriva `artista` y de dónde sale el género (la categoría de YouTube casi
   siempre es «Música»), para RF-051 y los filtros de RF-055; y si la popularidad de los
   filtros son las veces pedida en el bar (lo propuesto) u otra medida.
4. ~~Autenticación de la pantalla del bar~~ Resuelto el 2026-11-10: vinculación con código (RF-058, ADR-23).
5. Matriz del Operador (D9) y estado `retirada`.
6. Valores de bloqueo de cuenta e inactividad del panel.
7. Si la lista de la casa suena fuera de horario sin sesión abierta.
8. Quién crea establecimientos y su primer Administrador.
9. ~~Si la canción que suena cuenta para el cupo de 3~~ Resuelto el 2026-11-10: cuenta hasta
   que termina (RN-01), como en los prototipos y la simulación.
10. Cuánto dura «Subió N puestos» (`puesto_anterior`): la simulación lo mantiene 10 min
    *(propuesto)*.
11. Dominio público de la web: el QR codifica `https://<dominio>/q/<token>` y no se puede
    cambiar una vez impreso, así que tiene que ser estable y con HTTPS. Lo publica el equipo
    de frontend junto con `/.well-known/assetlinks.json` (paquete y huella SHA-256 de la firma
    de la app), que sirve Next, no el backend (ADR-19).
12. Migración de `api_idempotencia`. El código del backend (pedir, votar, mover el voto y los
    reportes de la pantalla) usa esta tabla, pero no estaba en la base ni en ninguna migración,
    y esas operaciones respondían 500. El 2026-11-10 el equipo de frontend la creó a mano en la
    base de Railway con la forma que pide el código, para poder integrar. Falta que el backend
    la adopte como migración de Flyway (el repositorio todavía no tiene ninguna):

    ```sql
    CREATE TABLE IF NOT EXISTS public.api_idempotencia (
        id_establecimiento BIGINT       NOT NULL REFERENCES public.establecimiento (id_establecimiento) ON DELETE CASCADE,
        device_hash        VARCHAR(64)  NOT NULL,
        clave              UUID         NOT NULL,
        operacion          VARCHAR(40)  NOT NULL,
        huella_peticion    VARCHAR(64)  NOT NULL,
        respuesta_http     INTEGER      NOT NULL,
        respuesta_json     TEXT         NOT NULL,
        fecha_creacion     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (id_establecimiento, device_hash, clave)
    );
    CREATE INDEX IF NOT EXISTS idx_api_idempotencia_fecha ON public.api_idempotencia (fecha_creacion);
    ```

    `fecha_creacion` y su índice sirven para borrar las claves viejas; cuánto tiempo se
    guardan es `TBD`.
13. Panel mínimo hecho por el equipo de frontend en `rokolaBackend/compat/` (2026-11-10), para
    poder probar RF-040: `POST/DELETE /panel/sesion`, `GET /panel/yo`, `GET /panel/noche` y
    `POST /panel/reproduccion/{id}/salto`. Las demás rutas de `/panel` responden
    `404 no_encontrado` (o `401` sin sesión). Lo que el backend tiene que revisar o decidir:
    - La sesión vive en memoria (reiniciar el backend cierra todas). Duración 12 h, 4 h sin uso,
      bloqueo de 15 min tras 5 intentos *(propuestos, punto 6)*.
    - Las contraseñas de los 5 usuarios de la base no son bcrypt válido (64 caracteres; bcrypt
      tiene 60): nadie puede entrar hasta que se regeneren.
    - Falta `PUT /panel/yo/clave`: un usuario con `cambiar_password` puede entrar pero no
      pasa de ahí. Quien lo haga debe cerrar las demás sesiones de ese usuario.
    - `noche.sesion.origen` sale siempre `manual` y los bloqueos «hasta el fin de la noche»
      salen como horas o indefinido: el modelo de datos no guarda esa diferencia.
    - De paso se corrigió `GET /bares/{slug}/busqueda`: `en_cola` no traía `espera_seg` y la
      interfaz rechazaba la respuesta cuando la búsqueda coincidía con algo de la cola.
14. Campo `tema` (RF-059, ADR-25). **Hecho por el equipo de frontend el 2026-11-12** (D14),
    sin poder verificarlo contra la base de Railway (apagada y sin acceso):
    - Migración `V2__tema_establecimiento.sql`, la primera del repositorio: columna
      `establecimiento.tema` (`rockola` | `vidrio` | `afiche`, por defecto `rockola`, con CHECK).
      La base existente queda como versión 1 (`baseline-on-migrate`).
    - `GET /api/bares/{slug}` devuelve `tema`.
    - `GET/PATCH /api/panel/establecimiento` (`ContratoEstablecimientoService`), solo
      Administrador y solo su bar; valida con los límites del contrato y anota
      `editar_establecimiento` en `accion_admin`. **`TBD`:** si `accion_admin.tipo` tiene un
      CHECK en la base real, tiene que admitir `editar_establecimiento`; si no, el PATCH falla.
15. Corrección del 2026-11-12, **hecha por el equipo de frontend** (D14, mismo método que el
    punto 14): pausa de la reproducción (RF-040). El backend no guardaba la pausa y siempre
    informaba `sonando`, así que la barra del teléfono y de la TV seguía corriendo. Migración
    `V3__pausa_reproduccion.sql` (`reproduccion.pausada_desde`, `segundos_en_pausa`), el reloj
    en `ReproduccionReloj`, `estado: pausada` en `Reproduccion` y
    `POST /panel/reproduccion/{id}/pausa` y `/reanudacion` (`ContratoPausaService`). Cuando la
    TV reporta su posición, la pausa acumulada vuelve a cero. También: `destacadas` devuelve
    tres, y `DELETE` de voto y de solicitud (RF-060, RF-061).
16. Volumen de la pantalla (RF-062), **hecho por el equipo de frontend el 2026-11-12** (D14):
    migración `V4__volumen_pantalla.sql` (`configuracion_servicio.volumen`, 80 por defecto,
    CHECK 0–100), `volumen` en `ahora` y `noche`, y `PUT /panel/volumen`
    (`ContratoVolumenService`). Se avisa a la TV por el canal en vivo de la noche abierta.
17. Gestión del panel, **hecha por el equipo de frontend el 2026-11-21** (D14): horario,
    usuarios y `PUT /panel/yo/clave`, bloqueos, listas de la casa y `GET /panel/busqueda`, con
    las reglas de la simulación (doc 11 tiene los archivos). Migración
    `V5__gestion_panel.sql`, **ya aplicada en Railway**. Para que lo revise el backend:
    - `TBD`: que `accion_admin.tipo` admita `editar_horario`, `crear_usuario`,
      `editar_usuario`, `desactivar_usuario`, `reactivar_usuario`, `restablecer_clave`,
      `bloquear`, `extender_bloqueo`, `desbloquear`, `editar_lista` y `activar_lista`.
    - El nombre de un usuario o de una lista va en `accion_admin.detalle->>'objetivo_nombre'`:
      la bitácora lo muestra aunque después se renombre o se borre.
    - «Hasta el fin de la noche» no tiene columna propia en el modelo original: `fecha_fin` es el
      cierre de la franja vigente (o 12 h sin franja, *propuesto*) y `bloqueo_contenido.duracion`
      (V5) guarda lo elegido.
    - `crearUsuario` no es idempotente en el servidor (la respuesta lleva la clave temporal y no
      se guarda en `api_idempotencia`); repetirla responde 409 `conflicto`.
    - La clave temporal tiene 12 caracteres al azar; la clave propia, de 10 caracteres a
      72 bytes (lo que usa bcrypt) *(propuesto)*.
    - Sigue en el 404 «aún no existe»: retirar con motivo. El QR del panel se hizo en el punto 25.
    - Quedan de la revisión de código: la búsqueda del panel gasta el mismo presupuesto diario de
      YouTube que la de los clientes; cambiar la clave propia no cierra las otras sesiones del
      usuario ni limita los intentos con la clave actual.
18. Servicio, mantenimiento y bitácora, **hechos por el equipo de frontend el 2026-11-21** (D14):
    `PUT /panel/servicio`, `POST /panel/servicio/mantenimiento` y `GET /panel/bitacora`
    (`ContratoServicioService`, `ContratoBitacora`). Para que lo revise el backend:
    - **El backend no abre ni cierra noches solo**: ningún código escribe en `sesion`. Por eso
      «Reanudar solicitudes» abre una noche si no hay ninguna (bitácora `abrir_sesion`, detalle
      «al reanudar»); si no, salir de mantenimiento dejaría el bar sin poder recibir canciones.
      `TBD`: la apertura y el cierre automáticos por horario (RF-037).
    - Mantenimiento, como la simulación: corta lo que suena (`saltada`), expira los pedidos
      pendientes (`expirada`, motivo «Mantenimiento») y cierra la noche. El modo queda en
      `configuracion_servicio.estado_manual`.
    - `TBD`: `forzado_activo` no abre fuera del horario. `estado_manual` solo admite
      `disponible`, `pausado` y `mantenimiento`, y hoy se trata como `automatico`.
    - La bitácora va de 25 en 25 con un cursor que es el id de la última acción, y filtra por
      fechas, persona (`sistema` = sin persona) y tipo. Solo muestra los tipos que el contrato
      conoce.
    - Cambio del contrato: `cambiarServicio` y `bitacora` agregan `datos_invalidos` a sus errores
      (un `modo_qr` desconocido, o una fecha, tipo o cursor mal formados); `generado/` regenerado.
19. Abrir y cerrar la noche a mano y calendario de eventos, **hechos por el equipo de frontend
    el 2026-11-21** (D14): `POST /panel/sesiones`, `POST /panel/sesiones/actual/cierre` y
    `/panel/eventos` (`ContratoServicioService`, `ContratoEventosService`). Para que lo revise el
    backend:
    - Cerrar la noche corta lo que suena, expira lo pendiente (motivo «Noche cerrada») y no
      cambia el modo del QR. Abrirla con una abierta es 409; si el bar estaba en mantenimiento,
      abrirla lo saca de él.
    - `servicio.estado` sin noche abierta: fuera del horario ahora es `fuera_de_horario` (con
      `proxima_apertura`), como la simulación; antes era siempre `en_pausa`. Dentro del horario
      sigue `en_pausa` (`ContratoEstadoService`).
    - `evento_calendario` guarda día y horas en la zona del bar: si la hora de fin es menor, el
      evento termina al día siguiente. Por eso un evento dura menos de 24 h *(propuesto)*.
      `tipo` guarda el `tipo_evento` del contrato.
20. Eventos que cambian el horario y noches ligadas a su evento, **hechos por el equipo de
    frontend el 2026-11-21** (D14), a pedido del usuario. Para que lo revise el backend:
    - **Un evento suma horas al horario y nunca quita** (decisión del usuario, 2026-11-21): si
      cae dentro del horario de siempre no cambia nada; si empieza antes o termina después, el bar
      abre antes o cierra más tarde ese día. Las ventanas que se pisan se unen. Lo calcula `HorarioEfectivoService` (paquete
      `acceso`), que usan `ahora` (estado, `hasta` y `proxima_apertura`), pedir y la
      reproducción automática (`HorarioActivoService` ahora delega en él) y «hasta el fin de la
      noche» de los bloqueos. `GET /bares/{slug}` sigue mandando el horario semanal sin eventos.
    - **Cada noche se liga al evento de hoy**: el de la ventana abierta ahora (aunque el evento
      empiece más tarde esa noche) o, si no hay, el próximo que empieza hoy (fecha del bar)
      *(propuesto)*. La noche toma su `tipo_evento` aunque se abra
      como «normal». Al crear, editar o eliminar un evento se religa la noche abierta; si deja
      de haber evento hoy, vuelve a «normal». Un tipo puesto a mano sin evento se respeta.
21. Capa analítica (RF-047 a RF-054, RF-063, ADR-27), **hecha por el equipo de frontend el
    2026-11-20** (D15) y probada solo con el esquema reconstruido y noches de ejemplo:
    - Migración `V6__modelo_analitico.sql` (esquemas `analitica` y `analitica_stg`), pipeline
      en `rokolaBackend/pipeline/` y las 9 rutas de `/panel/analitica/*`.
    - **`TBD`:** el género sale de `cancion.categoria`; hay que confirmar que el backend la
      llena con un género y no con «Música». Si no, todo queda «Sin clasificar» (punto 3).
    - **`TBD`:** quién programa el pipeline al cerrar cada noche. Hoy se ejecuta a mano.
    - La fecha de `evento_analitico` es `timestamp_utc` (comprobado en Railway el 2026-11-21).
      **`TBD`:** las demás columnas marcadas como supuesto en el esquema reconstruido.
    - La noche 1 se excluye con `--excluir 1`: quedó abierta del 11 al 21 de noviembre.
    - `actualizado_en` de `OrigenDatos` pasa a admitir `null`: todavía no ha corrido ninguna carga.
22. ~~`pantalla_vinculo` y `codigo_qr.url_publica` sin migración~~ Resuelto el 2026-11-21 con
    `V7__pantalla_y_noche_manual.sql`, con `IF NOT EXISTS` por si la base real ya las tiene.
    **`TBD`:** comparar sus columnas con las de Railway cuando vuelva a estar disponible.
23. La noche por horario (RF-036 a RF-039, RN-09), **hecha por el equipo de frontend el
    2026-11-21** (D14). La migración V8 cita este punto como «pendiente 20». Se unió con los
    puntos 18 y 19 en una sola implementación (`ContratoServicioService`, rutas en
    `ContratoGestionController`). Esto reemplaza lo que esos puntos dicen de que el backend no
    abre noches solo y de que `forzado_activo` no abre fuera del horario.
    - Un ciclo cada 15 s abre la noche al entrar en una franja y la cierra al salir. La ventana es
      la del horario efectivo (franjas más eventos, punto 20).
    - `configuracion_servicio.forzado_abierto` (V8): atiende fuera del horario hasta que la
      cierren (`modo_qr: forzado_activo`). Pausar y reanudar no le quita lo forzado.
    - `sin_apertura_hasta` (V8): cerrada a mano dentro de una franja, no se reabre sola hasta
      que termine esa ventana del horario efectivo. `sesion.origen` (V8) dice cómo se abrió.
    - Forzar la apertura, quitarla (cierra la noche) y salir de mantenimiento son solo del
      Administrador (D9).
    - Al cerrar, lo que suena queda `saltada` con los segundos que alcanzó y lo pendiente pasa a
      `expirada`. Sin noche abierta, `version` es la última emitida (antes 0: los clientes
      descartaban esa foto y no se enteraban del cierre).
    - **`TBD`:** que `accion_admin.tipo` admita `abrir_sesion`, `cerrar_sesion`,
      `pausar_solicitudes`, `reanudar_solicitudes`, `forzar_apertura` y `mantenimiento`; que
      `accion_admin.id_usuario` admita `NULL` (lo que hace el horario, sin persona); . Las columnas
      de `evento_calendario` (`fecha`, `hora_inicio`, `hora_fin`, `tipo`, `descripcion`) las asegura
      `V9`, sin cambiar nada si ya existen.
    - Con una ventana abierta, la noche toma solo el evento de esa ventana. Un tipo elegido a mano
      y distinto del evento se respeta, y un evento ya empezado no se desliga al editar el calendario.

24. Plataforma (RF-064, ADR-29), **hecha por el equipo de frontend el 2026-11-21** (D16): tabla
    `superadmin` (V10) y rutas `/plataforma/*`, con su cookie `rockola_plataforma`.
    - La primera cuenta sale de `ROCKOLA_SUPERADMIN_CORREO` y `ROCKOLA_SUPERADMIN_CLAVE`, solo si
      no hay ninguna, y debe cambiar la clave. Claves de al menos 12 caracteres *(propuesto)*.
    - Crear un bar es todo o nada: bar, configuración (3 pedidos, 600 s, 60 min), QR y primer
      Administrador. El bar nace sin horario: su Administrador lo define.
    - Si una clave temporal se pierde, la plataforma da otra: cierra las sesiones de esa cuenta.
    - Durante el bloqueo (5 fallos, 15 min) todo intento responde igual, con clave buena o mala.
    - **`TBD`:** desactivar bares y editar cuentas desde la plataforma.
25. QR del panel y su destino (RF-035, RF-065, ADR-30), **hechos por el equipo de frontend el
    2026-11-21** (D18): `GET /panel/establecimiento/qr`, `POST …/regeneracion` y `PUT …/destino`, con
    `establecimiento.destino_qr` (V11).
    - Solo se guarda la huella del token: un QR sin `url_publica` no se puede dibujar y responde
      `url: null`. Su destino se puede cambiar igual; generar otro invalida el impreso.
    - Con `ROKOLA_WEB_URL`, el `/q/{token}` del backend redirige a la web, que aplica el destino.
      Si se retira el instalador, al arrancar los bares en `app` vuelven a `web`.
    - **`TBD`:** publicar el APK firmado y definir `ROKOLA_APK_URL` y `ROKOLA_WEB_URL`.
    - **`TBD`:** el dominio de la app y `/.well-known/assetlinks.json`, para que el QR abra la app
      ya instalada sin pasar por la pantalla de descarga.