# Lo hecho en el backend para el frontend: detalle

Los puntos 13 a 26 de «Pendiente del backend» del [doc 04](../04-modelo-de-datos-y-contrato.md),
con todo su detalle. Están hechos (D14, D15, D16, D21); lo que el equipo de backend tiene que
revisar sigue aquí, marcado `TBD` o «Para que lo revise el backend». Los archivos de cada uno
están en el [doc 11](../11-entrega-al-backend.md). Movidos el 2026-11-25 para que el doc 04
cueste poco de leer.
<a id="p13"></a>

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

<a id="p14"></a>

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

<a id="p15"></a>

15. Corrección del 2026-11-12, **hecha por el equipo de frontend** (D14, mismo método que el
    punto 14): pausa de la reproducción (RF-040). El backend no guardaba la pausa y siempre
    informaba `sonando`, así que la barra del teléfono y de la TV seguía corriendo. Migración
    `V3__pausa_reproduccion.sql` (`reproduccion.pausada_desde`, `segundos_en_pausa`), el reloj
    en `ReproduccionReloj`, `estado: pausada` en `Reproduccion` y
    `POST /panel/reproduccion/{id}/pausa` y `/reanudacion` (`ContratoPausaService`). Cuando la
    TV reporta su posición, la pausa acumulada vuelve a cero. También: `destacadas` devuelve
    tres, y `DELETE` de voto y de solicitud (RF-060, RF-061).

<a id="p16"></a>

16. Volumen de la pantalla (RF-062), **hecho por el equipo de frontend el 2026-11-12** (D14):
    migración `V4__volumen_pantalla.sql` (`configuracion_servicio.volumen`, 80 por defecto,
    CHECK 0–100), `volumen` en `ahora` y `noche`, y `PUT /panel/volumen`
    (`ContratoVolumenService`). Se avisa a la TV por el canal en vivo de la noche abierta.

<a id="p17"></a>

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

<a id="p18"></a>

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

<a id="p19"></a>

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

<a id="p20"></a>

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

<a id="p21"></a>

21. Capa analítica (RF-047 a RF-054, RF-063, ADR-27), **hecha por el equipo de frontend el
    2026-11-20** (D15) y probada solo con el esquema reconstruido y noches de ejemplo:
    - Migración `V6__modelo_analitico.sql` (esquemas `analitica` y `analitica_stg`), pipeline
      en `rokolaBackend/pipeline/` y las 9 rutas de `/panel/analitica/*`.
    - ~~**`TBD`:** el género sale de `cancion.categoria`~~ Resuelto el 2026-11-24: el backend la
      llena con un género del clasificador (V12, V13, `origen_categoria`) y la deja `null` si no
      lo encuentra.
    - **`TBD`:** quién programa el pipeline al cerrar cada noche. Hoy se ejecuta a mano.
    - La fecha de `evento_analitico` es `timestamp_utc` (comprobado en Railway el 2026-11-21).
      **`TBD`:** las demás columnas marcadas como supuesto en el esquema reconstruido.
    - La noche 1 se excluye con `--excluir 1`: quedó abierta del 11 al 21 de noviembre.
    - `actualizado_en` de `OrigenDatos` pasa a admitir `null`: todavía no ha corrido ninguna carga.

<a id="p23"></a>

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

<a id="p24"></a>

24. Plataforma (RF-064, ADR-29), **hecha por el equipo de frontend el 2026-11-21** (D16): tabla
    `superadmin` (V10) y rutas `/plataforma/*`, con su cookie `rockola_plataforma`.
    - La primera cuenta sale de `ROCKOLA_SUPERADMIN_CORREO` y `ROCKOLA_SUPERADMIN_CLAVE`, solo si
      no hay ninguna, y debe cambiar la clave. Claves de al menos 12 caracteres *(propuesto)*.
    - Crear un bar es todo o nada: bar, configuración (3 pedidos, 600 s, 60 min), QR y primer
      Administrador. El bar nace sin horario: su Administrador lo define.
    - `GET /plataforma/bares` devuelve también `direccion` y `horario` de cada bar (2026-11-21).
      En el contrato tienen valor por defecto para que un backend anterior no tumbe la lista.
    - Si una clave temporal se pierde, la plataforma da otra: cierra las sesiones de esa cuenta.
    - Durante el bloqueo (5 fallos, 15 min) todo intento responde igual, con clave buena o mala.
    - **`TBD`:** desactivar bares y editar cuentas desde la plataforma.

<a id="p25"></a>

25. QR del panel y su destino (RF-035, RF-065, ADR-30), **hechos por el equipo de frontend el
    2026-11-21** (D18): `GET /panel/establecimiento/qr`, `POST …/regeneracion` y `PUT …/destino`, con
    `establecimiento.destino_qr` (V11).
    - Solo se guarda la huella del token: un QR sin `url_publica` no se puede dibujar y responde
      `url: null`. Su destino se puede cambiar igual; generar otro invalida el impreso.
    - Con `ROKOLA_WEB_URL`, el `/q/{token}` del backend redirige a la web, que aplica el destino.
      Si se retira el instalador, al arrancar los bares en `app` vuelven a `web`.
    - **`TBD`:** publicar el APK firmado y definir `ROKOLA_APK_URL` y `ROKOLA_WEB_URL`.
    - La web ya sirve `/.well-known/assetlinks.json` con las huellas de `ROCKOLA_ANDROID_SHA256`
      (servicio web). Sin esa variable publica una lista vacía y Android pregunta con qué abrir.
    - **`TBD`:** la huella de la firma y compilar el APK con el dominio definitivo de la web.

<a id="p26"></a>

26. Cubo en vivo y géneros (RF-067, RF-068, D21), **hechos por el equipo de backend el 2026-11-24**
    (PR #11 a #14) e integrados por el frontend:
    - `panel: ["analitica"]` sale por el canal **público** con cada pedido o voto (y con cada
      género corregido, a todos los bares que usan esa canción). No lleva datos, pero sí tráfico
      a los teléfonos, que lo ignoran. **`TBD`:** mandarlo solo a quien tiene sesión del panel.
    - El cubo en vivo recorre todas las noches del bar con `CUBE` en cada consulta. **`TBD`:**
      medir cuánto tarda con meses de datos (doc 07).
    - `POST /panel/canciones/reclasificar` ya está en el contrato y la usa «Canciones» (punto 18
      del equipo, 2026-11-25). `GET /panel/generos/estado-analitica` y `GET /panel/generos/resumen`
      siguen sin uso en el frontend (no hay requisito que los pida).
    - El frontend arregló en `ContratoError.kt` la lista del manejador de errores: el cambio
      había sacado la analítica, la plataforma y el QR del panel, y sus errores perdían
      `{ codigo, mensaje }` (D14, doc 11).
    - **`TBD`:** probado solo contra la simulación; falta contra una base real.
