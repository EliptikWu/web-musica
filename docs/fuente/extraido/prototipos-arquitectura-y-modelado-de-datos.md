# Prototipos, Arquitectura y Modelado de datos

> Texto extraído de `Prototipos, Arquitectura y Modelado de datos .docx` con `scripts/extraer-fuentes.py`. El original manda: las imágenes (maquetas y diagramas) solo están en el .docx.

### PROTOTIPADO, MODELADO DE DATOS Y ARQUITECTURA

SAMUEL ALZATE BERRIO

DERLY ELENA QUEJADA PEREA

JUAN DIEGO TÁMARA

LI KUN WU LI

Corporación Universitaria Empresarial Alexander von Humboldt

Facultad de Ingenierías y Ciencias Básicas

Ingeniería De Software

Armenia, Quindío

2026

### Prototipos

Cabe aclarar que se hicieron 3 prototipos basados en la idea principal de un reproductor de música:

#### Tema #1 -

La primera propuesta sigue el estilo de las aplicaciones de música de iOS. Usa un fondo oscuro con manchas de color difuminadas y, encima, tarjetas que simulan vidrio esmerilado. La intención es transmitir un ambiente moderno e íntimo, parecido al de un bar con luz baja. El color de acento, un degradado entre durazno, rosa y lavanda, se reservó para los elementos más importantes: el botón principal, la canción del usuario y el estado del bar. Así la atención se dirige sin esfuerzo a lo que está sonando y al lugar de cada pedido en la cola.

Figura 3. Interfaz del cliente

Figura 4. Vista pública en la pantalla del local

Pensada para verse a distancia en una TV: reproductor de YouTube embebido que pasa solo a la siguiente canción, canción actual con votos y progreso, las siguientes de la cola con su número de votos en grande y su tiempo estimado, una etiqueta que indica si suenan pedidos del público o la lista por defecto, y un recuadro con el QR del establecimiento para invitar a participar. Se actualiza sin recargar (RF-015, RF-016, RF-017, RF-018, RF-019, RF-020, RF-035 / CU-12, CU-13, CU-14).

Figura 5. Panel de administración

#### Tema #2

La segunda propuesta parte del objeto que le da nombre al producto. La idea es que el teléfono funcione como el panel de una rockola. La canción que suena aparece dentro de un arco de luz, como en las máquinas clásicas, y cada canción se muestra como una tarjeta de título similar a las tiras de papel que identificaban los discos. Votar equivale a presionar una tecla, que queda hundida cuando ya se votó. Los tres pedidos disponibles se representan como fichas, en referencia a las monedas de las rockolas de tienda de barrio. La luz también comunica el estado del sistema: encendida, el bar recibe canciones; apagada, está cerrado o en pausa. Esta propuesta busca una sensación cálida y nostálgica, cercana a la tradición de la rockola en cantinas y tiendas colombianas.

Figura 3. Interfaz del cliente

Figura 4. Vista pública en la pantalla del local

Pensada para verse a distancia en una TV: reproductor de YouTube embebido que pasa solo a la siguiente canción, canción actual con votos y progreso, las siguientes de la cola con su número de votos en grande y su tiempo estimado, una etiqueta que indica si suenan pedidos del público o la lista por defecto, y un recuadro con el QR del establecimiento para invitar a participar. Se actualiza sin recargar (RF-015, RF-016, RF-017, RF-018, RF-019, RF-020, RF-035 / CU-12, CU-13, CU-14).

Figura 5. Panel de administración

#### Tema #3

La tercera propuesta se inspira en los carteles callejeros que anuncian conciertos de salsa y verbenas. Combina letras grandes y condensadas, tinta negra sobre papel y fondos fluorescentes en amarillo, naranja y rosa. La jerarquía se construye con el tamaño de la letra: lo que está sonando es siempre lo más grande de la pantalla. Los estados de la interfaz se expresan con recursos propios de la imprenta: el voto disponible es una casilla, el voto ya emitido es un sello, la canción propia aparece resaltada y la acción de pedir es un bloque de tinta negra. Cuando el bar no recibe canciones, el cartel pierde el color fluorescente. Esta propuesta transmite un tono festivo, popular y latino.

Figura 3. Interfaz del cliente

Figura 4. Vista pública en la pantalla del local

Pensada para verse a distancia en una TV: reproductor de YouTube embebido que pasa solo a la siguiente canción, canción actual con votos y progreso, las siguientes de la cola con su número de votos en grande y su tiempo estimado, una etiqueta que indica si suenan pedidos del público o la lista por defecto, y un recuadro con el QR del establecimiento para invitar a participar. Se actualiza sin recargar (RF-015, RF-016, RF-017, RF-018, RF-019, RF-020, RF-035 / CU-12, CU-13, CU-14).

Figura 5. Panel de administración

| Pantalla | Qué resuelve | RF / CU |
|---|---|---|
| 1. Bienvenida | Entrada por QR sin descargas ni registro; identificador anónimo del teléfono; estado del servicio (“En vivo”); aviso breve de tratamiento de datos con enlace a la política, consultable desde cualquier pantalla; un solo botón “Entrar” | RF-001, RF-002, RF-003, RF-004 / CU-01, CU-02, CU-03, CU-04 |
| 2. Buscar | Búsqueda con autocompletado; resultados con título, artista, duración y miniatura de YouTube; canciones ya en la cola para votar sin repetir la solicitud; estados Pedir, Pedida, Votar, Votada y No disponible con motivo; mensaje claro si la búsqueda falla; confirmación inmediata con puesto y tiempo estimado | RF-005, RF-006, RF-007, RF-008, RF-009, RF-010, RF-011, RF-012 / CU-05 a CU-09 |
| 3. Cola | Canción sonando con progreso; tarjeta “Tu canción” con estado, puesto, votos y tiempo estimado; lista ordenada por votos con botón Votar, que se actualiza sola; canciones ya votadas marcadas con ícono y texto, no solo con color; regla de desempate explicada | RF-010, RF-012, RF-013, RF-015, RF-016, RF-017, RF-018 / CU-09, CU-10, CU-12 |
| 4. Fuera de horario | Aviso cuando el servicio está fuera de horario, en pausa o en mantenimiento, con el horario programado; no permite pedir ni votar, pero sí ver lo que está sonando | RF-003, RF-036 / CU-03 |

| Zona | Qué resuelve | RF / CU |
|---|---|---|
| Inicio de sesión y usuario | Acceso con credenciales, usuario y rol visibles, cerrar sesión; el Operador solo ve el control de reproducción y la cola | RF-021, RF-022 / CU-15 |
| Estado del servicio | Estado del QR (por horario, en pausa o en mantenimiento), botones Pausar/Reanudar solicitudes y Modo mantenimiento | RF-003, RF-036, RF-038, RF-039 / CU-21, CU-23, CU-24 |
| Sonando en el local | Canción actual con botones Pausar/Reanudar, Saltar y Bloquear | RF-040, RF-042 / CU-25, CU-27 |
| Esta noche | Resumen de la sesión abierta (solicitudes, teléfonos, votos, espera promedio) y enlace al tablero analítico | RF-037, RF-049, RF-050 / CU-22, CU-32 |
| Cola de solicitudes | Cola ordenada por votos con duración, antigüedad y acciones Retirar (con motivo) y Bloquear | RF-015, RF-041, RF-042 / CU-26, CU-27 |
| Registro de acciones | Quién saltó, bloqueó, pausó o cambió algo, y a qué hora, más los errores de la API de YouTube | RF-008, RF-044 / CU-07, CU-28 |
| Horario del QR, lista por defecto y canciones bloqueadas | Programación semanal, lista activa con su modo de rotación y bloqueos vigentes con opción de desbloquear o extender | RF-033, RF-036, RF-043 / CU-18, CU-21, CU-27 |
| Menú de gestión (pantallas secundarias) | Usuarios del personal, listas por defecto, establecimiento y código QR, y calendario de eventos | RF-023 a RF-035, RF-045 / CU-16, CU-17, CU-19, CU-20, CU-29 |
| Tablero analítico | Indicadores descriptivos, diagnósticos, comparativos y recomendaciones | RF-049 a RF-054 / CU-32 a CU-35 |

### Modelo de datos

#### 2.1 Diagrama entidad-relación

Las seis entidades exigidas por la guía son Cancion, Solicitud, Voto, Sesion, Dispositivo y ListaDefecto (en azul). Las demás apoyan la operación: Establecimiento y Horario (configuración y QR), ElementoLista (canciones de la lista por defecto), Reproduccion (historial de lo que sonó), Bloqueo y AccionAdmin (registro de acciones del personal), UsuarioAdmin, BusquedaCache (ahorro de cuota) y EventoAnalitico (registro para el pipeline).

Sesion se refiere a la sesión de operación del establecimiento (una noche de servicio, desde que se activa el QR hasta el modo mantenimiento). La participación de cada teléfono en una sesión se obtiene de sus solicitudes, votos y eventos.

#### 2.2 Diccionario de datos

Cancion (catálogo de videos de YouTube ya consultados)

| Campo | Tipo | Descripción |
|---|---|---|
| video_id | texto (PK) | Identificador del video en YouTube |
| titulo | texto | Título del video |
| artista | texto | Derivado del canal o del título |
| canal | texto | Nombre del canal de YouTube |
| duracion_seg | número | Duración en segundos (convertida del formato ISO 8601) |
| miniatura_url | texto | URL de la miniatura |
| categoria_id | texto | Categoría de YouTube (para análisis por género) |
| embebible | sí/no | Si el video permite reproducción embebida |
| disponibilidad | lista: disponible, no_disponible, restringido | Último estado conocido |
| fecha_validacion | fecha | Última consulta a videos.list |
| bloqueada_hasta | fecha (opcional) | Si tiene valor futuro, no se puede pedir |

Solicitud (una canción en la cola de una sesión)

| Campo | Tipo | Descripción |
|---|---|---|
| id | PK | Generado por Bubble |
| sesion | Sesion (FK) | Sesión de operación |
| cancion | Cancion (FK) | Canción pedida |
| dispositivo | Dispositivo (FK) | Quién la pidió primero |
| fecha_solicitud | fecha | Fecha y hora de creación (desempate) |
| votos_total | número | Votos activos, incluido el del solicitante |
| estado | lista | pendiente, reproduciendo, reproducida, saltada, bloqueada, no_disponible, expirada |
| fecha_inicio, fecha_fin | fecha | Inicio y fin de su reproducción |

Voto

| Campo | Tipo | Descripción |
|---|---|---|
| id | PK | Generado por Bubble |
| solicitud | Solicitud (FK) | Canción votada |
| dispositivo | Dispositivo (FK) | Quién vota |
| sesion | Sesion (FK) | Facilita consultas y limpieza |
| fecha | fecha | Momento del voto |
| activo | sí/no | Falso si el voto se cambia a otra canción (RF-19, opcional) |
| Restricción | — | Un solo voto activo por (dispositivo, solicitud), validado en el workflow |

Sesion (sesión de operación)

| Campo | Tipo | Descripción |
|---|---|---|
| id | PK | Generado por Bubble |
| establecimiento | Establecimiento (FK) | — |
| fecha_apertura, fecha_cierre | fecha | Inicio y fin del servicio |
| tipo_evento | texto | Normal, karaoke, partido, temática, etc. (del calendario del establecimiento) |
| estado | lista | abierta, cerrada |
| origen_actual | lista | solicitudes, lista_defecto |
| reproduccion_actual | Reproduccion (FK) | Lo que suena ahora |
| indice_lista_defecto | número | Posición de la rotación de la lista por defecto |

Dispositivo

| Campo | Tipo | Descripción |
|---|---|---|
| id | PK | Generado por Bubble |
| id_hash | texto (único) | SHA-256 del UUID generado en el navegador; nunca se guarda el UUID |
| tipo | lista | movil, tableta, otro (según el ancho de pantalla, no el modelo) |
| primer_acceso, ultimo_acceso | fecha | — |
| aviso_privacidad_fecha | fecha | Cuándo aceptó el aviso |

ListaDefecto y ElementoLista

| Campo | Tipo | Descripción |
|---|---|---|
| ListaDefecto.id | PK | — |
| ListaDefecto.nombre | texto | Ej.: “Viernes crossover” |
| ListaDefecto.modo_rotacion | lista | secuencial, aleatoria sin repetición |
| ListaDefecto.activa | sí/no | Solo una activa por establecimiento |
| ElementoLista.lista_defecto | ListaDefecto (FK) | — |
| ElementoLista.cancion | Cancion (FK) | — |
| ElementoLista.orden | número | Posición en modo secuencial |
| ElementoLista.veces_reproducida, ultima_reproduccion | número, fecha | Para la rotación y el indicador de cumplimiento |

Entidades de apoyo

| Entidad | Campos principales |
|---|---|
| Establecimiento | nombre, slug, qr_url, zona_horaria (America/Bogota), estado_qr_manual (automatico, forzado_activo, pausado), max_pendientes_dispositivo (3), duracion_max_seg (600), ventana_no_repeticion_min (60), lista_defecto_activa |
| Horario | establecimiento, dia_semana, hora_inicio, hora_fin (admite cruce de medianoche, ej. 20:00–02:00), activo |
| Reproduccion | sesion, cancion, origen (solicitud, lista_defecto), solicitud (opcional), fecha_inicio, fecha_fin, segundos_reproducidos, resultado (completa, saltada_admin, error_video) |
| Bloqueo | cancion, usuario_admin, motivo, fecha_inicio, fecha_fin |
| AccionAdmin | usuario_admin, tipo (saltar, bloquear, desbloquear, pausar_qr, reanudar_qr, mantenimiento, editar_horario, editar_lista), objetivo, detalle, fecha |
| UsuarioAdmin | usuario de Bubble con correo y rol (admin, dj) |
| BusquedaCache | consulta_normalizada, resultados (lista de Cancion), fecha, aciertos |
| EventoAnalitico | ver sección 5.1 |

#### 2.3 Reglas de negocio

| ID | Regla |
|---|---|
| RN-01 | Un dispositivo puede tener como máximo 3 solicitudes pendientes por sesión (configurable). |
| RN-02 | Si se pide una canción que ya está pendiente en la sesión, no se duplica: se registra como voto. |
| RN-03 | Quien pide una canción le da automáticamente su primer voto. |
| RN-04 | Cada dispositivo tiene como máximo un voto activo por solicitud. |
| RN-05 | La cola se ordena por votos_total de mayor a menor y, en empate, por fecha_solicitud de la más antigua a la más reciente. |
| RN-06 | No se acepta una canción bloqueada, no disponible, que no permita embeber o que dure más de 10 minutos; el cliente ve el motivo. |
| RN-07 | Una canción ya reproducida en la sesión no se puede volver a pedir durante 60 minutos (configurable). |
| RN-08 | Sin solicitudes pendientes suena la lista por defecto; cuando aparece una solicitud, se termina la canción en curso y se vuelve a las solicitudes. |
| RN-09 | El QR está activo según el horario, salvo que el administrador lo pause o lo fuerce a activo. |
| RN-10 | El modo mantenimiento cierra la sesión y deja las solicitudes pendientes como expiradas; la siguiente activación abre una sesión nueva. |
| RN-11 | Todas las acciones del personal (saltar, bloquear, pausar, mantenimiento) quedan registradas con usuario y fecha. |

#### 2.4 Estados de una solicitud

| Estado | Llega desde | Causa |
|---|---|---|
| pendiente | (creación) | WF-03 acepta la solicitud |
| reproduciendo | pendiente | WF-05 la elige como siguiente |
| reproducida | reproduciendo | El video terminó |
| saltada | reproduciendo | El DJ pulsó Saltar |
| no_disponible | reproduciendo | El reproductor reportó error |
| bloqueada | pendiente | El DJ bloqueó la canción |
| expirada | pendiente | Modo mantenimiento o cierre de sesión |

### Arquitectura

La misma aplicación (acceso por QR, búsqueda, votación, cola pública, administración y eventos analíticos) se construye dos veces, una por enfoque. La versión A responde a la intervención de Tendencias, que pide lógica sin desarrollo tradicional de código; la versión B responde a los RDA1 y RDA2 de Construcción de Aplicaciones Móviles, que piden Android Studio.

| Aspecto | Versión A: web en Bubble | Versión B: nativa en Kotlin |
|---|---|---|
| Estilo | Monolito cliente-servidor low-code | Monolito en capas tradicional (MVVM) |
| Capas | Vista lógica: la plataforma las genera y oculta | Explícitas en el código: UI, ViewModel, dominio, datos |
| Cómo se construye | Se modela: datos, reglas y pantallas | Se programa en Kotlin con Android Studio |
| Backend | Bubble (base de datos y workflows integrados) | Backend compartido, por definir |
| Acceso del cliente | QR abre la web, sin instalar | QR lleva a instalar la app |

#### Versión Bubble

Es una aplicación monolítica cliente-servidor sobre la plataforma low-code Bubble. Monolítica porque una sola app contiene las interfaces, los workflows y la base de datos; cliente-servidor porque el navegador ejecuta la interfaz y el servidor de Bubble guarda los datos y ejecuta la lógica sensible.

En Bubble las capas no se programan: se modelan los datos, las reglas y las pantallas, y la plataforma genera y ejecuta el resto, como describen Cabot (2020) y Sahay et al. (2020) para el desarrollo low-code. Por eso la organización en capas de la sección 3 es una vista lógica, no una estructura de código. La plataforma incluso permite mezclar capas, por ejemplo un workflow pegado a un botón.

Para que esa vista lógica se cumpla en la práctica, el equipo adopta tres convenciones:

- Lógica en workflows reutilizables. Las reglas críticas (crear voto, avanzar la cola, pasar a la lista por defecto) viven en eventos personalizados o workflows de backend, no repetidas en cada botón.
- Acceso a datos por reglas de privacidad. Todo filtrado por establecimiento y rol se define en las reglas de privacidad, no solo en las búsquedas de cada pantalla.
- Integraciones solo por el API Connector. Ninguna página llama a servicios externos directamente; la llave de YouTube queda en el servidor.

#### Trade offs Bubble

| Plataforma | A favor frente a Bubble | En contra para este reto |
|---|---|---|
| Glide | Se aprende en días; plan gratuito más generoso para publicar | El componente de video no expone el fin del video, así que la cola no puede avanzar sola; tiempo real menos inmediato |
| Adalo | Genera apps móviles nativas sin código | Tiempo real débil (la cola necesitaría recargas); su app nativa no se construye en Android Studio, así que no evidencia el RDA1 |
| Microsoft Power Apps | Integración con Microsoft 365 y buenos formularios de gestión | Exige que el usuario inicie sesión con cuenta Microsoft, lo que rompe el acceso sin registro por QR; conectores externos de pago |
| Google AppSheet | Construcción rápida sobre Google Sheets | Normalmente pide inicio de sesión; sincronización por intervalos en vez de tiempo real; interfaz poco personalizable para baja iluminación |
| Bubble (elegido) | Interfaz libre, datos en vivo sin recargar, API Connector, Data API para extraer los eventos | Curva de aprendizaje más alta; el plan gratuito no publica en vivo; consumo por unidades de carga |

#### Versión Nativa

Es un monolito en capas en sentido tradicional: un solo APK construido en Android Studio con Kotlin, cuyas capas sí existen en el código y siguen el patrón MVVM recomendado por Android. La regla es de dependencia hacia abajo: cada capa solo llama a la inmediatamente inferior, y la interfaz nunca accede a la red directamente.

La capa de datos se conecta a un backend compartido, por definir, que guarda las entidades de la sección 4 y notifica los cambios a la app en tiempo real; sin él, cada teléfono vería su propia cola. La búsqueda llama a la API de YouTube con Retrofit, con la llave restringida al paquete de la app en Google Cloud.

## Imágenes incluidas en el original

- `word/media/image1.png`
- `word/media/image10.png`
- `word/media/image11.png`
- `word/media/image12.png`
- `word/media/image2.png`
- `word/media/image3.png`
- `word/media/image4.png`
- `word/media/image5.png`
- `word/media/image6.png`
- `word/media/image7.png`
- `word/media/image8.png`
- `word/media/image9.png`
