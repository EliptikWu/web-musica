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
| `CancionCatalogo` (RF-068) | `id`, `youtube_video_id`, `titulo`, `artista`, `categoria` (`null` = sin género), `origen_categoria` (`ETIQUETAS`, `TITULO`, `DESCRIPCION`, `MANUAL` o `null`), `pedidos` | La canción en la vista «Canciones». `MANUAL`: corregida en el panel; la clasificación automática no la pisa |
| `Bar` (público) | `slug`, `nombre`, `zona_horaria`, `pedidos_maximos` (3), `duracion_max_seg` (600), `ventana_no_repeticion_min` (60), `tema` (`rockola`), `horario[]` | De `Establecimiento` y `Horario`. `tema`: el diseño con que se ven el teléfono, la TV y el panel con sesión (RF-059, D19, ADR-31) |
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
| ~~Estado `retirada` en `Solicitud`, con `motivo_retiro`~~ | Resuelto: el backend lo tiene, con el motivo en `motivo_estado` |
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
La apertura forzada dura solo mientras está fuera del horario: si la noche se abrió a mano antes
de la hora, cuando empieza el horario el servidor vuelve a `automatico` y manda el horario (avisa
y cierra a su hora). El horario es el efectivo: franjas más eventos (RF-045).

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
| `POST /panel/canciones/reclasificar` | Vuelve a clasificar hasta 10 canciones del bar sin género desde `despuesDe` (`Reclasificacion`); nunca toca las `MANUAL`. La web la llama de 10 en 10 desde «Canciones» (hasta 100 por vez) | 068 |
| `PUT /panel/volumen` | Volumen de la pantalla (`{ volumen }`, 0 a 100). No va a la bitácora desde el 2026-11-25 (`cambiar_volumen` sigue en el contrato para las líneas viejas, que tampoco se listan sin filtro); sale en `Ahora.volumen` y `Noche.volumen` (80 sin dato) | 062 |
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
| `GET /panel/analitica/cubo?eje=dia\|evento&fuente=vivo\|historico` | Cubo de KPI por género, franja y día o tipo de evento. Cada celda trae solicitudes, votos, usuarios únicos, no sonaron, espera promedio y máxima, segundos de pedidos y de lista de la casa, y la canción más popular. `*` es el miembro «Todos». Una combinación sin hechos **no trae celda** (no es un 0). Sin `fuente`, `vivo`: lo calcula el backend al consultar, con la noche abierta, todos los géneros y los siete días; `datos` trae además `fuente: operativa`, `en_vivo: true` y `sesiones_abiertas`, y `actualizado_en` es la hora de la consulta. `historico`: lo del pipeline (tres géneros y «Otros») | 063, 067 |
| `GET /panel/generos` | Los géneros que se pueden asignar (`string[]`, del clasificador del backend) | 068 |
| `GET /panel/canciones?soloSinCategoria&q&limite` | Canciones del bar (pedidas, en listas, bloqueadas o que sonaron) con su género: primero las sin género, luego las más pedidas. `soloSinCategoria` va en camelCase porque así lo nombra el backend; sin él vale `true`. `limite` de 1 a 100 (50); **no pagina** | 068 |
| `PATCH /panel/canciones/{id}/categoria` | `{ categoria }` → `{ id, categoria, origen_categoria: MANUAL }`. 400 `datos_invalidos` si el género no está en el catálogo; 404 si la canción no es del bar. Queda en la bitácora (`editar_categoria`) | 068 |

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
| `panel` | `{ recursos: [...] }` (`noche`, `yo`, `bitacora`, `bloqueos`, `listas`, `horario`, `usuarios`, `establecimiento`, `eventos`, `pantallas`, `analitica`) | Cambió algo del panel; el panel vuelve a pedir **solo** esos recursos. Nombra únicamente los que cambiaron: un voto no hace recargar el personal, y un 403 (que deja rastro en la bitácora) no hace volver a pedir lo rechazado. `yo` cambia con el nombre o la zona del bar y con los datos o el rol de quien mira. Sin datos privados |
| `ping` | `{ servidor_ahora }` | Cada 15 s, para detectar la desconexión |

`analitica` (RF-067) llega con cada cambio de la cola y con cada género corregido: el panel
vuelve a pedir **solo el cubo en vivo**, y junta los avisos seguidos (ADR-33).

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

Los shorts (RN-06, 2026-11-25) no tienen motivo propio: YouTube no marca qué video es un short, y el
backend lo reconoce por ser vertical (`player.embedHeight > embedWidth`, pedido con `maxWidth`; la
referencia de `videos.list` lo da solo si la proporción del video se conoce). Se tratan como un video no
embebible: no salen en la búsqueda y, si uno quedó en la caché de 6 h o en una lista de la casa, al
pedirlo o al sonar se rechaza como `no_disponible`. La simulación no tiene shorts en su catálogo.

## Pendiente del backend (`TBD`)

1. ~~Transporte de tiempo real~~ Resuelto el 2026-11-10: SSE (doc 03, «Tiempo real»).
2. Cuota de YouTube: `search.list` cuesta 100 unidades de 10 000 diarias; el
   autocompletado necesita caché en el servidor y espera entre teclas (el cliente espera
   300 ms). RF-008 y PB-54. El 2026-11-12 la caché pasó de 30 min a 6 h, configurable con
   `youtube.cache.horas` *(propuesto, por confirmar)*; ver doc 07, «Mediciones».
3. Cómo se deriva `artista` y ~~de dónde sale el género~~ (resuelto el 2026-11-24: el backend
   lo clasifica con las etiquetas, el título y la descripción del video y se corrige a mano en
   «Canciones», RF-068), para RF-051 y los filtros de RF-055; y si la popularidad de los
   filtros son las veces pedida en el bar (lo propuesto) u otra medida.
4. ~~Autenticación de la pantalla del bar~~ Resuelto el 2026-11-10: vinculación con código (RF-058, ADR-23).
5. Matriz del Operador (D9). ~~Estado `retirada`~~ Resuelto: el backend lo tiene (`EstadoSolicitud.RETIRADA`, `motivo_estado`, `compat/ContratoRetiroService.kt`).
6. Valores de bloqueo de cuenta e inactividad del panel.
7. ~~Si la lista de la casa suena fuera de horario sin sesión abierta~~ Resuelto el 2026-11-25 (D24): no; suena con la noche abierta, también con las solicitudes en pausa.
8. ~~Quién crea establecimientos y su primer Administrador~~ Resuelto el 2026-11-21 (D16, RF-064): el superadmin, desde `/plataforma`.
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
13. Panel mínimo (sesión, `yo`, `noche`, salto), 2026-11-10: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p13).
14. Campo `tema` y migración V2, 2026-11-12: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p14).
15. Pausa de la reproducción (V3), destacadas y `DELETE` de voto y solicitud, 2026-11-12: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p15).
16. Volumen de la pantalla (V4), 2026-11-12: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p16).
17. Gestión del panel: horario, usuarios, bloqueos, listas (V5), 2026-11-21: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p17).
18. Servicio, mantenimiento y bitácora, 2026-11-21: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p18).
19. Abrir y cerrar la noche a mano y calendario de eventos, 2026-11-21: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p19).
20. Eventos que cambian el horario y noches ligadas a su evento, 2026-11-21: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p20).
21. Capa analítica (V6, pipeline y rutas del tablero), 2026-11-20: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p21).
22. ~~`pantalla_vinculo` y `codigo_qr.url_publica` sin migración~~ Resuelto el 2026-11-21 con
    `V7__pantalla_y_noche_manual.sql`, con `IF NOT EXISTS` por si la base real ya las tiene.
    **`TBD`:** comparar sus columnas con las de Railway cuando vuelva a estar disponible.
23. La noche por horario (RF-036 a RF-039), 2026-11-21: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p23).
24. Plataforma (RF-064), 2026-11-21: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p24).
25. QR del panel y su destino (RF-035, RF-065), 2026-11-21: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p25).
26. Cubo en vivo y géneros (RF-067, RF-068), del equipo de backend, 2026-11-24: hecho. Detalle y lo que queda por revisar en [historial](historial/04-hecho-para-el-backend.md#p26).
27. Rapidez de los botones de operación (lista del equipo, 2026-11-25), *(propuesto, sin medir contra el backend real)*: los oyentes SSE de `ContratoSseService` arman la foto y escriben a cada cliente dentro de la misma petición, antes del 204; y al saltar, `ReproduccionAutomaticaService` consulta YouTube (`obtenerVideoPorId`, sin caché) antes de poner la siguiente. La web ya muestra pausar y reanudar al pulsar (optimista). Decidir si el envío pasa a un hilo aparte sin desordenar las versiones, y si la verificación del video se adelanta a cuando la canción queda primera en la cola. Ver doc 07, «Análisis 2026-11-25».
