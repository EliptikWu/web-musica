# Endpoints del contrato

> Generado desde `packages/contrato/src/endpoints.ts` con `npm run generar -w @rockola/contrato`. No se edita a mano.
> Esquemas completos en `openapi.json`. Errores: `{ codigo, mensaje, detalle? }` (`ErrorApi`).

## Cliente

| Operación | Método y ruta | Cuerpo → respuesta | Rol | Errores | RF |
|---|---|---|---|---|---|
| `resolverQr` | `GET /qr/{token}` | `ResolucionQr` | público | `qr_reemplazado`, `no_encontrado` | RF-001, RF-035 |
| `bar` | `GET /bares/{slug}` | `Bar` | público | `no_encontrado` | RF-001, RF-003 |
| `ahora` | `GET /bares/{slug}/ahora` | `Ahora` | público | `no_encontrado` | RF-003, RF-015, RF-016, RF-017, RF-018 |
| `destacadas` | `GET /bares/{slug}/destacadas` | `RespuestaDestacadas` | público | `no_encontrado` | RF-015 |
| `registrarDispositivo` | `POST /bares/{slug}/dispositivo` | `CuerpoDispositivo` → `Yo` | público | `datos_invalidos`, `no_encontrado` | RF-002 |
| `yo` | `GET /bares/{slug}/dispositivo` | `Yo` | público | `no_encontrado` | RF-011, RF-013 |
| `avisoVisto` | `POST /bares/{slug}/dispositivo/aviso` | 204 | público | `no_encontrado` | RF-004 |
| `buscar` | `GET /bares/{slug}/busqueda?q` | `Busqueda` | público | `datos_invalidos`, `youtube_no_disponible`, `cuota_agotada` | RF-005, RF-006, RF-007, RF-008 |
| `pedir` (idempotente) | `POST /bares/{slug}/solicitudes` | `CuerpoSolicitud` → `RespuestaSolicitud` | público | `limite_pedidos`, `ya_votaste`, `bloqueada`, `no_disponible`, `no_embebible`, `muy_larga`, `reproducida_hace_poco`, `servicio_no_disponible` | RF-009, RF-007 |
| `votar` (idempotente) | `POST /bares/{slug}/solicitudes/{id}/votos` | `RespuestaVoto` | público | `ya_votaste`, `no_pendiente`, `servicio_no_disponible` | RF-010, RF-011, RF-012 |
| `cambiarVoto` (idempotente) | `PUT /bares/{slug}/dispositivo/voto` | `CuerpoCambioVoto` → `RespuestaVoto` | público | `no_pendiente`, `ya_votaste`, `servicio_no_disponible` | RF-014 |

## Pantalla

| Operación | Método y ruta | Cuerpo → respuesta | Rol | Errores | RF |
|---|---|---|---|---|---|
| `qrPublico` | `GET /bares/{slug}/qr-publico` | `QrPublico` | público | `no_encontrado` | RF-035 |
| `reportarInicio` (idempotente) | `POST /bares/{slug}/pantalla/reproducciones/{id}/inicio` | `ReportePantalla` → 204 | público | `no_encontrado` | RF-019 |
| `reportarFin` (idempotente) | `POST /bares/{slug}/pantalla/reproducciones/{id}/fin` | `ReportePantalla` → 204 | público | `no_encontrado` | RF-019, RF-020 |
| `reportarError` (idempotente) | `POST /bares/{slug}/pantalla/reproducciones/{id}/error` | `ReportePantalla` → 204 | público | `no_encontrado` | RF-007 |

## Panel

| Operación | Método y ruta | Cuerpo → respuesta | Rol | Errores | RF |
|---|---|---|---|---|---|
| `entrar` | `POST /panel/sesion` | `CuerpoEntrar` → `YoPanel` | público | `credenciales_invalidas`, `cuenta_bloqueada`, `datos_invalidos` | RF-021 |
| `salir` | `DELETE /panel/sesion` | 204 | público | — | RF-021 |
| `yoPanel` | `GET /panel/yo` | `YoPanel` | operador | `no_autenticado` | RF-021, RF-022 |
| `cambiarClave` | `PUT /panel/yo/clave` | `CuerpoCambioClave` → 204 | operador | `no_autenticado`, `credenciales_invalidas`, `datos_invalidos` | RF-023, RF-025 |
| `noche` | `GET /panel/noche` | `Noche` | operador | `no_autenticado`, `sin_permiso` | RF-037, RF-044, RF-049 |
| `abrirSesion` | `POST /panel/sesiones` | `CuerpoSesion` → 204 | administrador | `no_autenticado`, `sin_permiso`, `conflicto` | RF-037 |
| `cerrarSesion` | `POST /panel/sesiones/actual/cierre` | 204 | administrador | `no_autenticado`, `sin_permiso`, `conflicto` | RF-037 |
| `cambiarServicio` | `PUT /panel/servicio` | `CuerpoServicio` → 204 | operador | `no_autenticado`, `sin_permiso` | RF-036, RF-039 |
| `mantenimiento` | `POST /panel/servicio/mantenimiento` | 204 | administrador | `no_autenticado`, `sin_permiso` | RF-038 |
| `saltar` (idempotente) | `POST /panel/reproduccion/{id}/salto` | 204 | operador | `no_autenticado`, `sin_permiso`, `no_encontrado` | RF-040 |
| `pausarReproduccion` | `POST /panel/reproduccion/{id}/pausa` | 204 | operador | `no_autenticado`, `sin_permiso`, `no_encontrado` | RF-040 |
| `reanudarReproduccion` | `POST /panel/reproduccion/{id}/reanudacion` | 204 | operador | `no_autenticado`, `sin_permiso`, `no_encontrado` | RF-040 |
| `retirar` (idempotente) | `POST /panel/solicitudes/{id}/retiro` | `CuerpoRetiro` → 204 | operador | `no_autenticado`, `sin_permiso`, `no_pendiente` | RF-041 |
| `bloqueos` | `GET /panel/bloqueos?vigencia` | `ListaBloqueos` | operador | `no_autenticado`, `sin_permiso` | RF-043 |
| `bloquear` (idempotente) | `POST /panel/bloqueos` | `CuerpoBloqueo` → `Bloqueo` | operador | `no_autenticado`, `sin_permiso`, `datos_invalidos` | RF-042 |
| `extenderBloqueo` | `PATCH /panel/bloqueos/{id}` | `CambioBloqueo` → `Bloqueo` | administrador | `no_autenticado`, `sin_permiso`, `no_encontrado` | RF-043 |
| `desbloquear` | `DELETE /panel/bloqueos/{id}` | 204 | administrador | `no_autenticado`, `sin_permiso`, `no_encontrado` | RF-043 |
| `horario` | `GET /panel/horario` | `Horario` | administrador | `no_autenticado`, `sin_permiso` | RF-036 |
| `guardarHorario` | `PUT /panel/horario` | `CuerpoHorario` → `Horario` | administrador | `no_autenticado`, `sin_permiso`, `datos_invalidos`, `conflicto` | RF-036 |
| `listas` | `GET /panel/listas?q` | `ListasCasa` | administrador | `no_autenticado`, `sin_permiso` | RF-028 |
| `crearLista` | `POST /panel/listas` | `CuerpoLista` → `ListaCasaDetalle` | administrador | `no_autenticado`, `sin_permiso`, `conflicto` | RF-027 |
| `lista` | `GET /panel/listas/{id}` | `ListaCasaDetalle` | administrador | `no_autenticado`, `sin_permiso`, `no_encontrado` | RF-028 |
| `renombrarLista` | `PATCH /panel/listas/{id}` | `CuerpoLista` → `ListaCasaDetalle` | administrador | `no_autenticado`, `sin_permiso`, `conflicto`, `no_encontrado` | RF-029 |
| `eliminarLista` | `DELETE /panel/listas/{id}` | 204 | administrador | `no_autenticado`, `sin_permiso`, `conflicto`, `no_encontrado` | RF-030 |
| `agregarALista` | `POST /panel/listas/{id}/canciones` | `CuerpoCancionLista` → `ListaCasaDetalle` | administrador | `no_autenticado`, `sin_permiso`, `conflicto`, `bloqueada`, `no_disponible` | RF-031 |
| `quitarDeLista` | `DELETE /panel/listas/{id}/canciones/{elemento}` | `ListaCasaDetalle` | administrador | `no_autenticado`, `sin_permiso`, `conflicto` | RF-032 |
| `ordenarLista` | `PUT /panel/listas/{id}/orden` | `CuerpoOrden` → `ListaCasaDetalle` | administrador | `no_autenticado`, `sin_permiso`, `datos_invalidos` | RF-032 |
| `activarLista` | `POST /panel/listas/{id}/activacion` | `CuerpoActivacion` → `ListaCasaDetalle` | administrador | `no_autenticado`, `sin_permiso`, `conflicto` | RF-033 |
| `buscarPanel` | `GET /panel/busqueda?q` | `BusquedaPanel` | administrador | `no_autenticado`, `sin_permiso`, `youtube_no_disponible`, `cuota_agotada` | RF-031 |
| `usuarios` | `GET /panel/usuarios?q&rol&estado` | `ListaUsuarios` | administrador | `no_autenticado`, `sin_permiso` | RF-024 |
| `crearUsuario` (idempotente) | `POST /panel/usuarios` | `CuerpoUsuario` → `UsuarioCreado` | administrador | `no_autenticado`, `sin_permiso`, `conflicto`, `datos_invalidos` | RF-023 |
| `editarUsuario` | `PATCH /panel/usuarios/{id}` | `CambioUsuario` → `UsuarioPanel` | administrador | `no_autenticado`, `sin_permiso`, `conflicto`, `no_encontrado` | RF-025 |
| `claveTemporal` | `POST /panel/usuarios/{id}/clave-temporal` | `ClaveTemporal` | administrador | `no_autenticado`, `sin_permiso`, `no_encontrado` | RF-025 |
| `desactivarUsuario` | `POST /panel/usuarios/{id}/desactivacion` | `UsuarioPanel` | administrador | `no_autenticado`, `sin_permiso`, `conflicto`, `no_encontrado` | RF-026 |
| `reactivarUsuario` | `POST /panel/usuarios/{id}/reactivacion` | `UsuarioPanel` | administrador | `no_autenticado`, `sin_permiso`, `no_encontrado` | RF-026 |
| `establecimiento` | `GET /panel/establecimiento` | `Establecimiento` | administrador | `no_autenticado`, `sin_permiso` | RF-034 |
| `editarEstablecimiento` | `PATCH /panel/establecimiento` | `CambioEstablecimiento` → `Establecimiento` | administrador | `no_autenticado`, `sin_permiso`, `datos_invalidos` | RF-034 |
| `qrEstablecimiento` | `GET /panel/establecimiento/qr` | `QrEstablecimiento` | administrador | `no_autenticado`, `sin_permiso` | RF-035 |
| `regenerarQr` | `POST /panel/establecimiento/qr/regeneracion` | `QrEstablecimiento` | administrador | `no_autenticado`, `sin_permiso` | RF-035 |
| `bitacora` | `GET /panel/bitacora?desde&hasta&usuario&tipo&cursor` | `PaginaBitacora` | administrador | `no_autenticado`, `sin_permiso` | RF-044, RF-008 |
| `eventos` | `GET /panel/eventos` | `ListaEventos` | administrador | `no_autenticado`, `sin_permiso` | RF-045 |
| `crearEvento` | `POST /panel/eventos` | `CuerpoEvento` → `EventoBar` | administrador | `no_autenticado`, `sin_permiso`, `datos_invalidos`, `conflicto` | RF-045 |
| `editarEvento` | `PATCH /panel/eventos/{id}` | `CuerpoEditarEvento` → `EventoBar` | administrador | `no_autenticado`, `sin_permiso`, `datos_invalidos`, `no_encontrado` | RF-045 |
| `eliminarEvento` | `DELETE /panel/eventos/{id}` | 204 | administrador | `no_autenticado`, `sin_permiso`, `conflicto`, `no_encontrado` | RF-045 |

## Tiempo real

`GET /bares/{slug}/eventos` (SSE). Cada mensaje: `event: <tipo>`, `data: <EventoTiempoReal en JSON>`, `id: <version>`.

Al abrir la conexión, el servidor manda primero una foto `ahora`; después, una cada vez que algo cambia y un `ping` cada 15 s.
