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
| Tiempo real | SSE en `GET /bares/{slug}/eventos` (ADR-07) |

## Entidades que ve el frontend

Nombres del diccionario; solo se listan los campos que la interfaz usa.

| Entidad | Campos | Notas |
|---|---|---|
| `Cancion` | `video_id`, `titulo`, `artista`, `duracion_seg`, `miniatura_url` | `artista` se deriva del canal o del título: `TBD` cómo |
| `Bar` (público) | `slug`, `nombre`, `zona_horaria`, `pedidos_maximos` (3), `duracion_max_seg` (600), `ventana_no_repeticion_min` (60), `horario[]` | De `Establecimiento` y `Horario` |
| `Franja` (horario) | `dia_semana` (1 = lunes … 7 = domingo), `hora_inicio`, `hora_fin` (`"20:00"`, `"02:00"`), `activo` | Una franja que cruza medianoche pertenece al día en que empieza |
| `Servicio` | `estado`, `modo_qr`, `desde`, `hasta`, `proxima_apertura` | Ver «Estado del servicio» |
| `Reproduccion` | `id`, `cancion`, `origen` (`solicitud` \| `lista_casa`), `solicitud_id`, `votos`, `inicio`, `posicion_seg`, `estado` (`sonando` \| `pausada`) | El avance se calcula con `posicion_seg` y el reloj del servidor |
| `ItemCola` | `solicitud_id`, `cancion`, `votos_total`, `fecha_solicitud`, `puesto`, `puesto_anterior` | Ya ordenada (RN-05). `puesto_anterior` alimenta «Subió 2 puestos» igual en todas las pantallas |
| `Ahora` | `version`, `servidor_ahora`, `servicio`, `reproduccion`, `cola[]`, `lista_casa` | La foto que se envía por tiempo real |
| `MiSolicitud` | `solicitud_id`, `cancion`, `estado`, `fecha_solicitud`, `votos_total`, `puesto`, `motivo` | Para «Tus pedidos» (RF-013) |
| `Yo` (teléfono) | `pedidos_maximos`, `pedidos_restantes`, `solicitudes[]`, `votos[]` (ids), `aviso_visto_en` | |
| `UsuarioPanel` | `id`, `nombre`, `correo`, `rol`, `estado`, `debe_cambiar_clave`, `ultimo_acceso` | Nunca contraseñas ni hashes |
| `Bloqueo` | `id`, `cancion`, `motivo`, `duracion` (`fin_sesion` \| `horas` \| `indefinido`), `inicio`, `fin`, `creado_por` | |
| `AccionAdmin` | `id`, `fecha`, `usuario` (o `null` = sistema), `tipo`, `objetivo`, `detalle` | La interfaz arma la frase («Bloqueaste «La Bachata», de Manuel Turizo») |
| `ListaCasa` | `id`, `nombre`, `modo_rotacion` (`secuencial` \| `aleatoria`), `activa`, `total_canciones`, `duracion_total_seg`, `canciones[]` | `aleatoria` = sin repetir hasta agotar la lista |
| `Establecimiento` | `id`, `slug`, `nombre`, `direccion`, `zona_horaria`, `estado`, límites | |
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

Los textos definitivos salen de `design:ux-copy` y viven en `apps/web/lib/textos`.

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
para ocultar lo que no se puede (`apps/web/lib/permisos.ts`).

| Acción | Operador | Administrador |
|---|---|---|
| Ver la operación de la noche (cola, lo que suena, registro, métricas) | Sí | Sí |
| Saltar, pausar y reanudar la reproducción | Sí | Sí |
| Pausar y reanudar las solicitudes | Sí | Sí |
| Retirar una solicitud | Sí | Sí |
| Bloquear hasta el fin de la sesión | Sí | Sí |
| Bloquear por horas o indefinidamente; desbloquear; extender | No | Sí |
| Modo mantenimiento; abrir y cerrar la sesión a mano | No | Sí |
| Horario, listas de la casa, usuarios, establecimiento y QR, eventos | No | Sí |
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
| `GET /bares/{slug}/destacadas` | «Lo más votado de la noche» | 015 |
| `POST /bares/{slug}/dispositivo` | Registrar el teléfono (`tipo` por ancho) y devolver `Yo` | 002 |
| `GET /bares/{slug}/dispositivo` | `Yo`: cupo, mis solicitudes, mis votos | 011, 013 |
| `POST /bares/{slug}/dispositivo/aviso` | Registrar que vio el aviso de datos | 004 |
| `GET /bares/{slug}/busqueda?q=` | Resultados de YouTube + coincidencias en la cola | 005–008 |
| `POST /bares/{slug}/solicitudes` | Pedir (`{ video_id }`); si ya está, cuenta como voto (RN-02) | 009 |
| `POST /bares/{slug}/solicitudes/{id}/votos` | Votar | 010–012 |
| `PUT /bares/{slug}/dispositivo/voto` | Cambiar un voto (`{ desde, hacia }`) | 014 |
| `GET /bares/{slug}/eventos` | Tiempo real (SSE) | 017 |

### Pantalla del bar

| Método y ruta | Para qué | RF |
|---|---|---|
| `GET /bares/{slug}/ahora` | La misma foto | 015–018 |
| `GET /bares/{slug}/qr-publico` | URL que codifica el QR de la placa | 035 |
| `POST /bares/{slug}/pantalla/reproducciones/{id}/inicio` · `/fin` · `/error` | Reportar el reproductor (`posicion_seg`, `codigo_youtube`) | 007, 019, 020 |

`TBD`: cómo se autentica la pantalla (propuesta: `Authorization: Pantalla <clave>`
emparejada desde el panel), para que un cliente no pueda reportar «fin» y saltar canciones.

### Panel (cookie de sesión)

| Método y ruta | Para qué | RF |
|---|---|---|
| `POST /panel/sesion` · `DELETE /panel/sesion` | Entrar y salir | 021 |
| `GET /panel/yo` · `PUT /panel/yo/clave` | Usuario actual; cambiar la clave | 021, 023 |
| `GET /panel/noche` | Todo lo de la operación: sesión, servicio, lo que suena, cola con detalle, métricas, registro, horario, lista activa, bloqueos, alertas | 037, 044, 049 |
| `POST /panel/sesiones` · `POST /panel/sesiones/actual/cierre` | Abrir y cerrar la noche a mano | 037 |
| `PUT /panel/servicio` (`{ modo_qr }`) · `POST /panel/servicio/mantenimiento` | Pausar/reanudar solicitudes; mantenimiento | 036, 038, 039 |
| `POST /panel/reproduccion/{id}/salto` · `/pausa` · `/reanudacion` | Control de la reproducción | 040 |
| `POST /panel/solicitudes/{id}/retiro` (`{ motivo, detalle }`) | Retirar de la cola | 041 |
| `GET /panel/bloqueos` · `POST` · `PATCH /{id}` · `DELETE /{id}` | Bloquear, extender, desbloquear | 042, 043 |
| `GET /panel/horario` · `PUT /panel/horario` | Horario semanal | 036 |
| `GET/POST /panel/listas` · `GET/PATCH/DELETE /panel/listas/{id}` · `POST/DELETE …/canciones` · `PUT …/orden` · `POST …/activacion` | Listas de la casa | 027–033 |
| `GET /panel/busqueda?q=` | Buscar para armar listas | 031 |
| `GET/POST /panel/usuarios` · `PATCH /panel/usuarios/{id}` · `POST …/clave-temporal` · `…/desactivacion` · `…/reactivacion` | Personal | 023–026 |
| `GET/PATCH /panel/establecimiento` · `GET …/qr` · `POST …/qr/regeneracion` | Establecimiento y QR | 034, 035 |
| `GET /panel/bitacora?desde&hasta&usuario&tipo&cursor` | Bitácora (incluye errores de YouTube) | 044, 008 |
| `GET/POST /panel/eventos` · `PATCH/DELETE /panel/eventos/{id}` | Calendario de eventos | 045 |
| `GET /panel/analitica/…` | Tablero (se define en la iteración 5) | 049–054 |

## Eventos de tiempo real

| `tipo` | Contenido | Cuándo |
|---|---|---|
| `ahora` | La foto `Ahora` completa (con `version`) | Cualquier cambio en la cola, los votos, lo que suena o el servicio |
| `panel` | `{ recursos: [...] }` (`noche`, `bitacora`, `bloqueos`, `listas`…) | Cambió algo del panel; el panel vuelve a pedir esos recursos. Sin datos privados |
| `ping` | `{ servidor_ahora }` | Cada 15 s, para detectar la desconexión |

Al abrir la conexión, el servidor manda **primero una foto `ahora`**: el cliente solo se
considera conectado cuando llegan datos (un canal que acepta y corta no cuenta) y así se
resincroniza al reconectar.

## Códigos de error

| Código | HTTP | Cuándo |
|---|---|---|
| `datos_invalidos` | 400 | El cuerpo o los parámetros no cumplen el esquema |
| `no_autenticado` | 401 | Panel sin sesión o sesión caducada |
| `credenciales_invalidas` | 401 | Correo o clave incorrectos (mensaje genérico, RF-021) |
| `sin_permiso` | 403 | El rol no permite la acción (queda en la bitácora) |
| `debe_cambiar_clave` | 403 | Primer ingreso o clave restablecida |
| `no_encontrado` | 404 | Bar, solicitud, lista o usuario inexistente |
| `limite_pedidos` | 409 | Ya tiene 3 solicitudes pendientes (RN-01) |
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

1. Transporte de tiempo real (SSE propuesto) y su autenticación.
2. Cuota de YouTube: `search.list` cuesta 100 unidades de 10 000 diarias; el
   autocompletado necesita caché en el servidor y espera entre teclas (el cliente espera
   300 ms). RF-008 y PB-54.
3. Cómo se deriva `artista` y de dónde sale el género (la categoría de YouTube casi
   siempre es «Música»), para RF-051.
4. Autenticación de la pantalla del bar.
5. Matriz del Operador (D9) y estado `retirada`.
6. Valores de bloqueo de cuenta e inactividad del panel.
7. Si la lista de la casa suena fuera de horario sin sesión abierta.
8. Quién crea establecimientos y su primer Administrador.
9. Si la canción que **suena** cuenta para el cupo de 3 (RN-01 dice «pendientes»). Los
   prototipos solo cuadran si cuenta: en el momento A el teléfono tiene «2 de 3» libres y
   ninguna pendiente, porque suya es la que suena. La simulación la cuenta.
10. Cuánto dura «Subió N puestos» (`puesto_anterior`): la simulación lo mantiene 10 min
    *(propuesto)*.
