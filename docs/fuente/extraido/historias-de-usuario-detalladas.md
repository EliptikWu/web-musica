# Historias de usuario detalladas

> Texto extraído de `Historias de usuario detalladas.docx` con `scripts/extraer-fuentes.py`. El original manda: las imágenes (maquetas y diagramas) solo están en el .docx.

HISTORIAS DE USUARIOS DETALLADAS

SAMUEL ALZATE BERRIO

DERLY ELENA QUEJADA PEREA

JUAN DIEGO TÁMARA

LI KUN WU LI

Corporación Universitaria Empresarial Alexander von Humboldt

Facultad de Ingenierías y Ciencias Básicas

Ingeniería De Software

Armenia, Quindío

2026

## Sprint 1

12 historias de usuario.

### PB-01 · Registrar mi establecimiento

| Prioridad: Must | Sprint: Sprint 1 | Depende de: — | Requisitos: RF-034, RNF-010 |
|---|---|---|---|

| Nombre: Como administrador, quiero registrar mi establecimiento en la aplicación, para que el servicio quede asociado a mi bar y sus datos estén separados de otros. | Atributos o entradas al sistema: -Nombre del establecimiento. -Dirección. -Zona horaria. -Usuario administrador que lo registra. |
|---|---|
| Caso de uso: El administrador entra al panel y abre el formulario de registro del establecimiento. Diligencia nombre, dirección y zona horaria y pulsa «Guardar». El sistema valida los campos, crea el establecimiento con un identificador propio y lo deja asociado al administrador. Desde ese momento, colas, listas, usuarios, sesiones y analítica pertenecen a ese bar, y su nombre aparece en el encabezado del panel. | Criterios de aceptación: -Se puede registrar un establecimiento con nombre, dirección y zona horaria. (RF-034) -Los datos de un establecimiento no son visibles para otro. (RF-034) -Los cambios quedan registrados en la bitácora. (RF-034) |

Pantalla del mockup

Diagrama de flujo

### PB-02 · Generar y descargar el QR del establecimiento

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-01 | Requisitos: RF-035 |
|---|---|---|---|

| Nombre: Como administrador, quiero descargar el código QR de mi bar, para imprimirlo y que los clientes entren con su teléfono. | Atributos o entradas al sistema: -Establecimiento registrado y activo. -Solicitud de generar, descargar o regenerar el QR. -Confirmación al regenerar. |
|---|---|
| Caso de uso: El administrador abre la sección del código QR. El sistema genera un QR único que apunta a la app de su bar y le permite descargarlo en formato imprimible o mostrarlo en pantalla. Ese QR aparece en la pantalla pública con el texto «Escanea y vota». Si el administrador decide regenerarlo, el sistema pide confirmación y el QR anterior deja de funcionar. | Criterios de aceptación: -El QR es único por establecimiento y abre su aplicación. (RF-035) -Se puede descargar en un formato imprimible. (RF-035) -Al regenerar el QR, el anterior deja de funcionar y se pide confirmación. (RF-035) -El QR permanece operativo mientras el establecimiento esté activo y en horario. (RF-035) |

Pantalla del mockup

Diagrama de flujo

### PB-03 · Entrar a la app escaneando el QR

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-02 | Requisitos: RF-001, RNF-001, RNF-002, RNF-004 |
|---|---|---|---|

| Nombre: Como cliente, quiero escanear el QR y entrar a la app del bar sin descargar nada ni registrarme, para pedir música en segundos. | Atributos o entradas al sistema: -Código QR escaneado con la cámara. -Identificador del establecimiento contenido en el QR. |
|---|---|
| Caso de uso: El cliente escanea con la cámara el QR de la mesa o de la pantalla del bar. El navegador abre la pantalla «Bienvenida» con el nombre del bar, la canción que suena ahora y «Lo más votado de la noche». No hay descarga, instalación ni registro: el cliente pulsa «Entrar y votar» y empieza a usar la app. | Criterios de aceptación: -Al escanear el QR vigente, la aplicación del establecimiento correcto abre en el navegador del teléfono. (RF-001) -No se solicita descarga, instalación, registro ni inicio de sesión. (RF-001) -La pantalla inicial carga en menos de 2 segundos con conexión móvil estándar. (RF-001) -Un QR de un establecimiento distinto no muestra datos de otro establecimiento. (RF-001) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-04 · Ser reconocido sin crear cuenta

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-03 | Requisitos: RF-002, RNF-007 |
|---|---|---|---|

| Nombre: Como cliente, quiero que la app me reconozca sin pedirme datos personales, para poder votar sin registrarme. | Atributos o entradas al sistema: -Primer acceso del navegador a la app. -Identificador de dispositivo guardado en el navegador (visitas siguientes). |
|---|---|
| Caso de uso: Cuando el cliente entra por primera vez, el sistema genera un identificador único y lo guarda en su navegador, sin pedir nombre, correo ni teléfono. En la «Bienvenida» se le explica que su teléfono queda como un código anónimo. Si recarga la página o vuelve más tarde esa noche, el sistema lo reconoce con el mismo identificador, que luego se usa para controlar los votos y para los eventos de uso. | Criterios de aceptación: -Al primer ingreso se genera un identificador y se conserva en el dispositivo. (RF-002) -El mismo dispositivo conserva su identificador entre sesiones de la misma noche. (RF-002) -No se solicita nombre, correo, teléfono ni otro dato personal. (RF-002) -El identificador queda disponible para los eventos de uso (RF-046) y para el control de votos (RF-011). (RF-002) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-05 · Registrar los eventos de acceso y búsqueda

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-04 | Requisitos: RF-046, RF-004, RF-005, RF-008, RNF-008, RNF-014 |
|---|---|---|---|

| Nombre: Como administrador, quiero que el ingreso por QR, la consulta del aviso de datos, las búsquedas y los errores de YouTube queden registrados desde el primer sprint, para tener datos de uso desde la primera prueba. | Atributos o entradas al sistema: -Acción del cliente o del sistema: acceso, aviso, búsqueda o error de YouTube. -Identificador de dispositivo. -Establecimiento y sesión vigente. -Fecha y hora. |
|---|---|
| Caso de uso: Es un proceso del sistema, sin pantalla propia. Cada vez que el cliente entra por QR, ve el aviso de datos o busca una canción, y cada vez que falla la API de YouTube, el backend crea un evento con tipo, fecha y hora, sesión, establecimiento y dispositivo, y lo guarda de forma asíncrona para no retrasar la respuesta. Si la acción falla, el evento se guarda igual indicando el resultado. Los eventos quedan disponibles para el pipeline analítico. | Criterios de aceptación: -Cada evento guarda tipo, fecha y hora, sesión, establecimiento, canción (si aplica) e identificador de dispositivo. (RF-046) -Los eventos se guardan aunque la acción del cliente falle de forma parcial, indicando el resultado. (RF-046) -El registro no retrasa la respuesta al cliente de forma perceptible. (RF-046) -Los eventos quedan disponibles para el pipeline analítico. (RF-046) -La visualización del aviso queda registrada como evento. (RF-004) -Cada búsqueda queda registrada como evento de uso. (RF-005) -Cada error se registra con tipo, fecha y hora, y el administrador puede consultarlo. (RF-008) |

| Pantalla del mockup Sin pantalla en los mockups. Proceso interno del sistema, sin pantalla. Se dispara desde «Bienvenida» y «Buscar, pedir y votar». | Diagrama de flujo |
|---|---|

### PB-06 · Conocer cómo se usan mis datos

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-03, PB-05 | Requisitos: RF-004, RNF-007 |
|---|---|---|---|

| Nombre: Como cliente, quiero ver un aviso breve de qué se registra de mi dispositivo, para saber que mi privacidad está cuidada. | Atributos o entradas al sistema: -Primer ingreso del dispositivo. -Clic en «Política de datos». |
|---|---|
| Caso de uso: En el primer ingreso, la «Bienvenida» muestra un aviso breve que explica qué se registra (identificador de dispositivo, solicitudes y votos), para qué y bajo qué política, conforme a la Ley 1581 de 2012. El aviso no impide usar la app. Después, el cliente puede volver a leerlo desde el enlace «Política de datos», disponible en cualquier pantalla. | Criterios de aceptación: -El aviso aparece en el primer ingreso sin impedir el uso inmediato de la aplicación. (RF-004) -El texto indica qué se registra, para qué se usa y que no se piden datos personales. (RF-004) -El cliente puede volver a consultarlo desde cualquier pantalla. (RF-004) -La visualización del aviso queda registrada como evento. (RF-004) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-07 · Recibir sugerencias mientras escribo

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-03, PB-05 | Requisitos: RF-005, RNF-002, RNF-011 |
|---|---|---|---|

| Nombre: Como cliente, quiero recibir sugerencias mientras escribo el nombre de una canción, para encontrarla rápido. | Atributos o entradas al sistema: -Texto que escribe el cliente (canción o artista). -Respuesta de la API de YouTube. |
|---|---|
| Caso de uso: En «Pide una canción», el cliente escribe en la caja de búsqueda (por ejemplo, «bad bunny»). Mientras escribe, la app consulta YouTube y muestra sugerencias sin que tenga que enviar la búsqueda, separadas en «Ya está en la cola» y «Resultados en YouTube». Si la búsqueda está vacía o no hay resultados, se muestra un mensaje claro. | Criterios de aceptación: -Al escribir, aparecen sugerencias sin que el cliente tenga que enviar la búsqueda. (RF-005) -La respuesta de la búsqueda tarda menos de 2 segundos en condiciones normales. (RF-005) -Una búsqueda vacía o sin resultados muestra un mensaje claro y no genera error. (RF-005) -Cada búsqueda queda registrada como evento de uso. (RF-005) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-08 · Ver título, artista, duración y miniatura

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-07 | Requisitos: RF-006, RNF-001, RNF-011 |
|---|---|---|---|

| Nombre: Como cliente, quiero ver cada resultado con título, artista, duración y miniatura, para elegir la canción correcta. | Atributos o entradas al sistema: -Resultados de la búsqueda. -Metadatos de YouTube: título, artista, duración y miniatura. |
|---|---|
| Caso de uso: Cada resultado de «Resultados en YouTube» se muestra en una tarjeta con miniatura, título, artista y duración, tomados de la API y no de una lista fija. La lista se puede recorrer y operar con una sola mano, y cada tarjeta tiene el botón «Pedir» para iniciar la solicitud. | Criterios de aceptación: -Cada resultado muestra los cuatro datos: título, artista, duración y miniatura. (RF-006) -Los datos provienen de la API de YouTube, no de una lista fija. (RF-006) -La lista es legible y operable con una sola mano en pantalla de teléfono. (RF-006) -Desde un resultado se puede iniciar la solicitud (RF-009). (RF-006) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-09 · Recibir mensajes claros cuando YouTube falla

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-07, PB-05 | Requisitos: RF-008, RNF-011, RNF-012 |
|---|---|---|---|

| Nombre: Como cliente, quiero un mensaje claro y poder reintentar si la búsqueda falla, para no quedarme sin saber qué hacer. | Atributos o entradas al sistema: -Error de la API: cuota agotada, servicio no disponible, tiempo de espera o respuesta inválida. -Acción de reintento del cliente. |
|---|---|
| Caso de uso: Si al buscar la API de YouTube falla, el cliente ve un mensaje comprensible con la opción de reintentar, y la app no se bloquea. La cola existente se conserva y la música en curso y la lista por defecto siguen sonando. El error queda registrado con su tipo, fecha y hora, y el administrador puede consultarlo en la bitácora. | Criterios de aceptación: -Ante un error de la API, el cliente ve un mensaje comprensible y puede reintentar. (RF-008) -La aplicación no se bloquea ni pierde la cola existente. (RF-008) -La música en curso y la lista por defecto siguen reproduciéndose aunque la API falle. (RF-008) -Cada error se registra con tipo, fecha y hora, y el administrador puede consultarlo. (RF-008) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-10 · Entrar al panel con usuario y contraseña

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-01 | Requisitos: RF-021, RNF-006 |
|---|---|---|---|

| Nombre: Como administrador, quiero iniciar sesión en el panel, para que solo el personal autorizado gestione el servicio. | Atributos o entradas al sistema: -Correo y contraseña del personal. -Acción de cerrar sesión. -Tiempo de inactividad. |
|---|---|
| Caso de uso: El administrador u operador entra al panel con su correo y contraseña. Si las credenciales son incorrectas, ve un mensaje genérico; tras varios intentos fallidos seguidos, la cuenta se bloquea temporalmente. Con la sesión iniciada llega al «Panel de la rockola», con su usuario en la esquina superior derecha, desde donde puede cerrar sesión. Por inactividad prolongada la sesión termina sola. | Criterios de aceptación: -Sin sesión iniciada no se puede acceder a ninguna pantalla del panel. (RF-021) -Credenciales incorrectas muestran un mensaje genérico, sin indicar cuál dato falló. (RF-021) -Tras varios intentos fallidos consecutivos la cuenta se bloquea temporalmente. (RF-021) -Al cerrar sesión o por inactividad prolongada, la sesión termina y se pide autenticarse de nuevo. (RF-021) -Las contraseñas se almacenan cifradas, nunca en texto plano. (RF-021) |

Pantalla del mockup

Diagrama de flujo

### PB-11 · Limitar el panel según el rol

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-10 | Requisitos: RF-022, RNF-006 |
|---|---|---|---|

| Nombre: Como administrador, quiero que el Operador solo controle la reproducción y la cola, para evitar cambios indebidos. | Atributos o entradas al sistema: -Rol del usuario autenticado (Administrador u Operador). -Función del panel solicitada. |
|---|---|
| Caso de uso: Al entrar al panel, el sistema muestra solo lo que permite el rol. El Administrador ve todo: tablero analítico, horario del QR, lista de la casa, usuarios y modo mantenimiento. El Operador (mesero, bartender o DJ) solo controla la reproducción y la cola: saltar, pausar y retirar solicitudes. Si intenta abrir una función no permitida, se rechaza y queda en la bitácora. | Criterios de aceptación: -Un Operador no puede gestionar usuarios, listas por defecto, horarios ni establecimientos. (RF-022) -Un Operador sí puede saltar canciones, pausar y retirar solicitudes inapropiadas. (RF-022) -Un intento de acceso a una función no permitida se rechaza y se registra en la bitácora. (RF-022) -Los permisos de cada rol son visibles para el Administrador al crear o editar un usuario. (RF-022) |

Pantalla del mockup

El mockup muestra la vista del Administrador; el Operador ve una versión reducida del panel.

Diagrama de flujo

### PB-12 · Abrir y cerrar la sesión de la noche manualmente

| Prioridad: Must | Sprint: Sprint 1 | Depende de: PB-10, PB-05 | Requisitos: RF-037, RNF-008, RNF-014 |
|---|---|---|---|

| Nombre: Como administrador, quiero abrir y cerrar desde el panel la sesión de la noche, para que todo lo que pase quede asociado a esa noche desde la primera prueba. | Atributos o entradas al sistema: -Acción del administrador: abrir o cerrar sesión. -Establecimiento. -Fecha y hora. -Tipo de evento del calendario, si existe. |
|---|---|
| Caso de uso: Al empezar la noche, el administrador abre la sesión desde el panel y la barra superior cambia a «Abierto, recibiendo canciones». Desde ese momento, cada solicitud, voto y reproducción queda asociada a esa sesión. Al terminar, la cierra y la sesión conserva fecha, hora de inicio y fin y el tipo de evento del calendario. Solo puede haber una sesión abierta por establecimiento. | Criterios de aceptación: -La sesión se abre y cierra manualmente por el Administrador. (RF-037) -Cada evento de uso se asocia a la sesión vigente. (RF-037) -Al cerrar, la sesión conserva su fecha, hora de inicio y fin, y el tipo de evento del calendario, si lo hay. (RF-037) -Hay una sola sesión abierta por establecimiento a la vez. (RF-037) |

Pantalla del mockup

La barra de estado del panel muestra la sesión abierta; el botón manual de abrir y cerrar sesión no aparece en el mockup.

Diagrama de flujo

## Sprint 2

15 historias de usuario.

### PB-13 · Pedir una canción

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-08, PB-04 | Requisitos: RF-009 |
|---|---|---|---|

| Nombre: Como cliente, quiero pedir una canción con un toque, para que entre a la lista de espera. | Atributos o entradas al sistema: -Canción elegida (identificador del video). -Identificador de dispositivo. -Estado del servicio. -Lista de contenido bloqueado. |
|---|---|
| Caso de uso: En los «Resultados en YouTube», el cliente pulsa «Pedir». Si la canción no está en la cola, entra con estado «en espera» y un voto inicial y aparece en la cola pública. Si ya estaba, no se duplica: se convierte en un voto. No se puede pedir con el servicio cerrado o en pausa, ni una canción bloqueada. La pantalla indica cuántos pedidos le quedan («Te quedan 2 de 3 pedidos»). | Criterios de aceptación: -Al solicitar, la canción aparece en la cola pública con estado «en espera» y un voto inicial. (RF-009) -Si la canción ya estaba en la cola, no se duplica: se suma un voto (sujeto a RF-011). (RF-009) -No se puede solicitar contenido bloqueado (RF-042) ni con el servicio no disponible (RF-003). (RF-009) -Cada solicitud genera un evento con canción, dispositivo (seudonimizado), sesión y hora. (RF-009) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-14 · Votar por una canción y ver la confirmación

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-13 | Requisitos: RF-010, RF-012, RNF-001, RNF-008 |
|---|---|---|---|

| Nombre: Como cliente, quiero votar por canciones ya pedidas y ver que mi voto quedó registrado, para ayudar a que suene mi favorita. | Atributos o entradas al sistema: -Canción de la cola elegida. -Identificador de dispositivo. |
|---|---|
| Caso de uso: En «La cola» o en «Ya está en la cola», el cliente pulsa «+1» sobre una canción en espera. El voto suma uno al contador, la cola se reordena y en menos de un segundo el botón cambia a «Votaste» para confirmar. Las canciones que ya votó se distinguen de las demás con texto, no solo con color. Si el voto falla, aparece un mensaje y el contador no cambia. | Criterios de aceptación: -Desde la cola, el cliente puede votar por cualquier canción en espera. (RF-010) -Cada voto válido suma uno al contador de la canción. (RF-010) -La posición de la canción en la cola se recalcula tras cada voto. (RF-010) -Cada voto genera un evento de uso. (RF-010) -Tras votar, el botón cambia de estado y aparece una confirmación en menos de 1 segundo. (RF-012) -Las canciones votadas por el dispositivo se distinguen de las demás. (RF-012) -Si el voto falla, se muestra un mensaje de error y el contador no cambia. (RF-012) -La confirmación no depende solo del color (accesibilidad). (RF-012) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-15 · Votar una sola vez por canción

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-14, PB-04 | Requisitos: RF-011, RNF-007, RNF-008 |
|---|---|---|---|

| Nombre: Como administrador, quiero que cada dispositivo vote una sola vez por canción, para que la cola sea justa. | Atributos o entradas al sistema: -Identificador de dispositivo. -Canción votada. -Votos previos del dispositivo en la sesión. |
|---|---|
| Caso de uso: Cuando el cliente vota, el sistema revisa con el identificador de dispositivo si ya votó por esa canción. Si es así, rechaza el segundo voto con un mensaje; si no, lo acepta. El mismo teléfono puede votar por canciones distintas, y el control se mantiene aunque recargue la página o reabra el navegador. Con muchos teléfonos votando a la vez, no se pierden ni se duplican votos. | Criterios de aceptación: -Un segundo voto del mismo dispositivo por la misma canción se rechaza con un mensaje. (RF-011) -El mismo dispositivo sí puede votar por canciones distintas. (RF-011) -El control se mantiene al recargar la página o reabrir el navegador. (RF-011) -Con varios dispositivos votando a la vez no se pierden ni se duplican votos. (RF-011) |

| Pantalla del mockup En la cola, el botón «Votaste» indica que ese teléfono ya votó por la canción. | Diagrama de flujo |
|---|---|

### PB-16 · Registrar las acciones de uso

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-13, PB-05, PB-12 | Requisitos: RF-046, RNF-008, RNF-014 |
|---|---|---|---|

| Nombre: Como administrador, quiero que cada solicitud, voto, cambio de voto, reproducción, omisión, bloqueo y transición a la lista por defecto quede registrada con su sesión, sobre la base de eventos de PB-05, para poder analizar después qué música funciona. | Atributos o entradas al sistema: -Acción: solicitud, voto, cambio de voto, reproducción (inicio y fin), omisión, bloqueo, transición a la lista por defecto, apertura o cierre de sesión. -Sesión vigente, establecimiento, canción y dispositivo. |
|---|---|
| Caso de uso: Es un proceso del sistema, sin pantalla propia. Sobre la base de PB-05, cada acción relevante de la noche genera un evento con su sesión, establecimiento, canción y dispositivo. Si la acción falla, se guarda igual con su resultado. Estos eventos alimentan después las «Métricas de la noche» y el tablero analítico. | Criterios de aceptación: -Cada evento guarda tipo, fecha y hora, sesión, establecimiento, canción (si aplica) e identificador de dispositivo. (RF-046) -Los eventos se guardan aunque la acción del cliente falle de forma parcial, indicando el resultado. (RF-046) -El registro no retrasa la respuesta al cliente de forma perceptible. (RF-046) -Los eventos quedan disponibles para el pipeline analítico. (RF-046) |

Pantalla del mockup

Proceso interno. Las «Métricas de la noche» del panel se calculan con estos eventos.

Diagrama de flujo

### PB-17 · Ver la cola ordenada por popularidad

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-13 | Requisitos: RF-015, RF-016, RNF-005 |
|---|---|---|---|

| Nombre: Como persona en el bar, quiero ver en pantalla qué sigue y qué suena ahora, para saber cuándo llegará mi canción. | Atributos o entradas al sistema: -Canciones en espera con su contador de votos. -Canción en reproducción y su origen. |
|---|---|
| Caso de uso: La pantalla pública del bar (y la vista «La cola» del teléfono) muestra «Lo que viene» ordenado de mayor a menor número de votos; si hay empate, va primero la que se pidió antes. Cada canción tiene miniatura, título, artista y votos. A la izquierda se destaca «Suena ahora» e indica si viene de una solicitud o de la lista por defecto. No hace falta iniciar sesión para verla. | Criterios de aceptación: -La cola se ordena de mayor a menor número de votos; el empate se resuelve por antigüedad de la solicitud. (RF-015) -Cada canción muestra título, artista, miniatura y contador de votos. (RF-015) -La vista es accesible sin iniciar sesión. (RF-015) -Con la cola vacía se muestra un mensaje y se indica que suena la lista por defecto. (RF-015) -La canción en reproducción se distingue claramente del resto de la cola. (RF-016) -Se muestra título, artista y miniatura. (RF-016) -Al cambiar de canción, la indicación se actualiza sin recargar. (RF-016) -Se indica si la canción viene de una solicitud o de la lista por defecto. (RF-016) |

Pantalla del mockup

Diagrama de flujo

### PB-18 · Ver la cola actualizarse sola

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-17 | Requisitos: RF-017, RNF-009 |
|---|---|---|---|

| Nombre: Como persona en el bar, quiero que los votos y el orden cambien sin recargar, para seguir la votación en vivo. | Atributos o entradas al sistema: -Cambios en solicitudes, votos y canción actual. -Conexión de cada pantalla (WebSocket). |
|---|---|
| Caso de uso: Cuando alguien pide o vota, el cambio aparece en pocos segundos en la pantalla del bar y en los teléfonos conectados, sin recargar. La cola se reordena sola y las flechas (por ejemplo, «↑2») muestran lo que subió. Si una pantalla pierde la conexión, muestra un aviso y se resincroniza al volver. | Criterios de aceptación: -Un voto o solicitud nuevo se refleja en las demás pantallas en pocos segundos. (RF-017) -El reordenamiento por popularidad se aplica solo. (RF-017) -Si se pierde la conexión, la pantalla muestra un aviso y se resincroniza al recuperarla. (RF-017) -Funciona con varios dispositivos conectados al mismo tiempo. (RF-017) |

Pantalla del mockup

Diagrama de flujo

### PB-19 · Saber cuánto falta para mi canción

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-17 | Requisitos: RF-018 |
|---|---|---|---|

| Nombre: Como cliente, quiero ver el tiempo estimado de espera de cada canción, para decidir si voto o espero. | Atributos o entradas al sistema: -Duración restante de la canción actual. -Duración de las canciones que van antes en la cola. |
|---|---|
| Caso de uso: En la tarjeta «Tu canción va de #3», el cliente ve en minutos cuánto falta para que suene su canción («Suena en unos 13 min»), y la cola indica la duración total. El cálculo suma lo que le queda a la canción actual y la duración de las que van antes, se recalcula cuando cambian votos u orden, y se presenta siempre como estimación. | Criterios de aceptación: -Cada canción en espera muestra su tiempo estimado en minutos. (RF-018) -El cálculo suma la duración restante de la canción actual y la de las que la preceden. (RF-018) -El estimado se recalcula cuando cambian votos, orden o la canción actual. (RF-018) -Se indica que es una estimación. (RF-018) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-20 · Reproducir la cola automáticamente

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-17 | Requisitos: RF-019, RNF-011, RNF-012 |
|---|---|---|---|

| Nombre: Como administrador, quiero que la canción más votada suene sola y pase a la siguiente, para que nadie tenga que operar la música. | Atributos o entradas al sistema: -Cola ordenada por votos. -Reproductor web o SmartTV con YouTube incrustado. -Fin de la canción actual. |
|---|---|
| Caso de uso: La pantalla del bar reproduce, con el reproductor incrustado de YouTube, la canción más votada. Al terminar, pasa sola a la siguiente de mayor popularidad; la que sonó cambia a «reproducida» y sale de la cola. El contenido no se descarga ni se redistribuye, y cada reproducción registra su inicio y fin. | Criterios de aceptación: -Al terminar una canción, comienza la siguiente de mayor popularidad sin intervención. (RF-019) -Cada canción reproducida cambia a estado «reproducida» y sale de la cola. (RF-019) -Se registra un evento de reproducción con inicio y fin. (RF-019) -No se descarga ni se redistribuye el contenido; solo se reproduce mediante YouTube. (RF-019) |

Pantalla del mockup

Diagrama de flujo

### PB-21 · Rechazar videos que no se pueden reproducir

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-13 | Requisitos: RF-007, RNF-011 |
|---|---|---|---|

| Nombre: Como administrador, quiero que no se acepten videos no disponibles, para que la música no se interrumpa. | Atributos o entradas al sistema: -Identificador del video. -Estado del video en YouTube: existe, público, sin restricciones e incrustable. |
|---|---|
| Caso de uso: Al pedir una canción, el sistema verifica que el video exista, esté disponible y se pueda reproducir incrustado. Si es eliminado, privado, restringido o no incrustable, se rechaza con un mensaje al cliente. Si es válido, se acepta y se guardan título, artista, duración y miniatura. Antes de reproducirla se verifica otra vez; si ya no está disponible, se omite y sigue la siguiente. | Criterios de aceptación: -Un video eliminado, privado, restringido o no incrustable se rechaza con un mensaje al cliente. (RF-007) -Un video válido se acepta y se guardan sus metadatos (título, artista, duración, miniatura). (RF-007) -Si un video deja de estar disponible antes de reproducirse, el sistema lo omite y sigue con el siguiente. (RF-007) -Cada rechazo u omisión queda registrado como evento. (RF-007) |

| Pantalla del mockup El mockup no muestra el rechazo; el mensaje aparece al pulsar «Pedir». | Diagrama de flujo |
|---|---|

### PB-22 · Seguir el estado de mis solicitudes

| Prioridad: Should | Sprint: Sprint 2 | Depende de: PB-13, PB-20 | Requisitos: RF-013 |
|---|---|---|---|

| Nombre: Como cliente, quiero ver si mi canción está en espera, sonando, omitida o bloqueada, para no quedarme sin saber qué pasó con mi petición. | Atributos o entradas al sistema: -Identificador de dispositivo. -Solicitudes y votos del dispositivo en la sesión. |
|---|---|
| Caso de uso: En «La cola», la tarjeta destacada «Tu canción va de #3» muestra al cliente el estado de lo que pidió o votó: en espera, sonando, reproducida, omitida, bloqueada o retirada. El estado se actualiza solo, sin recargar («Subió 2 puestos»). Si una canción fue omitida o bloqueada, se muestra el motivo general, y solo aparecen las solicitudes de ese teléfono. | Criterios de aceptación: -El cliente ve una lista de sus solicitudes con su estado actual. (RF-013) -El estado se actualiza solo, sin recargar. (RF-013) -Una canción omitida o bloqueada muestra el motivo general (por ejemplo, «video no disponible»). (RF-013) -Solo se muestran las solicitudes del propio dispositivo. (RF-013) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-23 · Crear y consultar usuarios del personal

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-11 | Requisitos: RF-023, RF-024 |
|---|---|---|---|

| Nombre: Como administrador, quiero crear usuarios con su rol y ver la lista del personal, para dar acceso a mi equipo. | Atributos o entradas al sistema: -Nombre, correo y rol (Administrador u Operador). -Establecimiento. -Contraseña inicial. -Filtros de búsqueda: nombre, correo, rol y estado. |
|---|---|
| Caso de uso: El administrador abre la gestión de usuarios y ve la lista del personal de su bar, con nombre, correo, rol y estado, y puede buscar y filtrar. Para crear uno, ingresa nombre, correo y rol; el sistema valida que el correo no exista, lo asocia a su establecimiento y le asigna una contraseña inicial que deberá cambiar en el primer ingreso. Nunca se muestra la contraseña. | Criterios de aceptación: -El usuario se crea solo si todos los campos obligatorios son válidos. (RF-023) -No se permiten dos usuarios con el mismo correo. (RF-023) -El usuario nuevo queda asociado a un único establecimiento y a un rol. (RF-023) -En el primer ingreso se obliga a cambiar la contraseña inicial. (RF-023) -La creación queda registrada en la bitácora. (RF-023) -La lista muestra nombre, correo, rol y estado (activo o inactivo). (RF-024) -Se puede buscar por nombre o correo y filtrar por rol y estado. (RF-024) -El Administrador solo ve los usuarios de su propio establecimiento. (RF-024) -Nunca se muestra la contraseña. (RF-024) |

| Pantalla del mockup Aún no hay mockups. | Diagrama de flujo |
|---|---|

### PB-24 · Editar y desactivar usuarios del personal

| Prioridad: Must | Sprint: Sprint 2 | Depende de: PB-23 | Requisitos: RF-025, RF-026, RNF-014 |
|---|---|---|---|

| Nombre: Como administrador, quiero editar, restablecer la contraseña y desactivar usuarios, para mantener los accesos al día. | Atributos o entradas al sistema: -Usuario a modificar. -Nuevo nombre, correo o rol. -Acción: restablecer contraseña, desactivar, eliminar o reactivar. -Confirmación. |
|---|---|
| Caso de uso: Desde la lista del personal, el administrador edita nombre, correo o rol, restablece la contraseña (se genera una temporal) o desactiva o elimina un usuario. El sistema no deja quitar el rol al último administrador activo ni que un administrador se elimine a sí mismo. La eliminación es lógica: el usuario no puede entrar, su sesión activa se cierra y sus acciones siguen en la bitácora. | Criterios de aceptación: -Los cambios se guardan solo si los datos son válidos y el correo no está repetido. (RF-025) -Un cambio de rol se aplica en el siguiente ingreso del usuario. (RF-025) -No se puede quitar el rol de Administrador al último administrador activo. (RF-025) -Al restablecer la contraseña, se genera una temporal que debe cambiarse al ingresar. (RF-025) -Cada modificación queda registrada en la bitácora. (RF-025) -Un usuario desactivado no puede iniciar sesión, y su sesión activa se cierra. (RF-026) -Se pide confirmación antes de eliminar. (RF-026) -Un administrador no puede eliminarse a sí mismo ni al último administrador activo. (RF-026) -Se puede reactivar un usuario desactivado. (RF-026) -Las acciones históricas del usuario siguen visibles en la bitácora. (RF-026) |

| Pantalla del mockup Sin pantalla en los mockups. No hay mockup de la gestión de usuarios. | Diagrama de flujo |
|---|---|

### PB-25 · Retirar solicitudes de la cola

| Prioridad: Should | Sprint: Sprint 2 | Depende de: PB-10, PB-17 | Requisitos: RF-041 |
|---|---|---|---|

| Nombre: Como Operador, quiero retirar una solicitud inadecuada o duplicada, para mantener la cola limpia. | Atributos o entradas al sistema: -Canción de la cola. -Motivo: inapropiada, duplicada u otro. |
|---|---|
| Caso de uso: En la tabla «Cola» del panel, el operador elige una canción que no corresponde y la retira indicando el motivo. La canción desaparece de la cola pública, sus votos dejan de contar y el cliente que la pidió ve el estado «retirada». La acción queda en la bitácora. | Criterios de aceptación: -La canción retirada desaparece de la cola pública y sus votos dejan de contar. (RF-041) -Se solicita un motivo (por ejemplo, inapropiada, duplicada, otro). (RF-041) -El cliente que la pidió ve el estado «retirada» (RF-013). (RF-041) -La acción queda registrada en la bitácora. (RF-041) |

Pantalla del mockup

El mockup muestra la acción «Bloquear» por fila; «Retirar» va en el mismo lugar.

Diagrama de flujo

### PB-26 · Consultar, editar y desactivar el establecimiento

| Prioridad: Should | Sprint: Sprint 2 | Depende de: PB-01 | Requisitos: RF-034, RNF-014 |
|---|---|---|---|

| Nombre: Como administrador, quiero actualizar los datos de mi establecimiento o desactivarlo, para mantenerlos al día. | Atributos o entradas al sistema: -Datos del establecimiento: nombre, dirección y zona horaria. -Acción: editar o desactivar. |
|---|---|
| Caso de uso: El administrador consulta la información de su establecimiento y la edita cuando cambia algo, por ejemplo la dirección o la zona horaria. También puede desactivarlo: un establecimiento desactivado no acepta solicitudes, pero conserva su historial. Los datos de un bar nunca son visibles para otro y cada cambio queda en la bitácora. | Criterios de aceptación: -Se puede consultar y editar la información de un establecimiento registrado. (RF-034) -Un establecimiento desactivado no permite solicitudes y conserva sus datos históricos. (RF-034) -Los datos de un establecimiento no son visibles para otro. (RF-034) -Los cambios quedan registrados en la bitácora. (RF-034) |

Pantalla del mockup

No hay mockup de la ficha del establecimiento; su nombre aparece en el encabezado del panel.

Diagrama de flujo

### PB-27 · Pausar y reanudar el servicio

| Prioridad: Should | Sprint: Sprint 2 | Depende de: PB-10, PB-12 | Requisitos: RF-039 |
|---|---|---|---|

| Nombre: Como administrador, quiero pausar las solicitudes y reanudarlas, para atender un imprevisto sin cerrar la sesión. | Atributos o entradas al sistema: -Acción del administrador: pausar o reanudar. |
|---|---|
| Caso de uso: En la barra superior del panel, el administrador pulsa «Pausar solicitudes» para atender un imprevisto. Los clientes ven un mensaje de pausa y no pueden pedir ni votar, pero la cola, los votos y la sesión se conservan. Al reanudar, el servicio vuelve a la normalidad. Ambas acciones quedan en la bitácora. | Criterios de aceptación: -En pausa, los clientes no pueden solicitar ni votar, y ven un mensaje de pausa. (RF-039) -La cola y los votos existentes se conservan. (RF-039) -Al reanudar, el servicio vuelve a su estado normal. (RF-039) -La pausa y la reanudación quedan registradas en la bitácora. (RF-039) |

Pantalla del mockup

Diagrama de flujo

## Sprint 3

15 historias de usuario.

### PB-28 · Programar horarios y activar el QR automáticamente

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-02, PB-10 | Requisitos: RF-036 |
|---|---|---|---|

| Nombre: Como administrador, quiero definir los horarios del servicio y que el QR se active y desactive solo, para no depender de nadie al abrir y cerrar. | Atributos o entradas al sistema: -Días de la semana. -Hora de inicio y fin de cada franja. -Activación o desactivación manual. |
|---|---|
| Caso de uso: En la tarjeta «Horario del QR», el administrador elige los días (por ejemplo, jueves a sábado) y la franja (8:00 p. m. a 2:00 a. m.) y pulsa «Editar horario» para guardar. El sistema no permite franjas solapadas. A la hora de inicio el servicio y el QR se activan solos, y a la hora de fin se desactivan; fuera de horario el cliente ve «Por ahora no recibimos canciones» con el horario de pedidos. El administrador puede activar o desactivar a mano, por encima de la programación. | Criterios de aceptación: -Se pueden definir una o varias franjas por día (hora de inicio y fin). (RF-036) -Franjas que se solapan no se pueden guardar. (RF-036) -Al llegar la hora de inicio, el servicio se activa; al llegar la de fin, se desactiva, sin intervención. (RF-036) -El Administrador puede activar o desactivar el servicio manualmente, por encima de la programación. (RF-036) -Cada cambio queda registrado en la bitácora. (RF-036) |

Pantalla del mockup

Diagrama de flujo

### PB-29 · Abrir y cerrar la sesión automáticamente según el horario

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-28, PB-12 | Requisitos: RF-037 |
|---|---|---|---|

| Nombre: Como administrador, quiero que la sesión de la noche se abra y se cierre sola según el horario programado, para no depender de que alguien lo haga a mano. | Atributos o entradas al sistema: -Programación de horarios (PB-28). -Calendario de eventos del establecimiento. -Fecha y hora actuales. |
|---|---|
| Caso de uso: Cuando llega la hora de inicio programada, el sistema abre la sesión de la noche sin intervención, y la barra del panel muestra «Abierto, recibiendo canciones. Desde las 8:00 p. m. Cierra a las 2:00 a. m. según el horario del QR». A la hora de fin la cierra y guarda fecha, horas y el tipo de evento del calendario. Nunca hay dos sesiones abiertas en el mismo bar. | Criterios de aceptación: -La sesión se abre y cierra automáticamente según el horario. (RF-037) -Cada evento de uso se asocia a la sesión vigente. (RF-037) -Al cerrar, la sesión conserva su fecha, hora de inicio y fin, y el tipo de evento del calendario, si lo hay. (RF-037) -Hay una sola sesión abierta por establecimiento a la vez. (RF-037) |

Pantalla del mockup

Diagrama de flujo

### PB-30 · Ver si el servicio está disponible

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-28 | Requisitos: RF-003 |
|---|---|---|---|

| Nombre: Como cliente, quiero saber si el servicio está disponible, en pausa o en mantenimiento, para entender por qué no puedo pedir. | Atributos o entradas al sistema: -Estado del servicio: activo, fuera de horario, pausado o mantenimiento. -Horario programado. |
|---|---|
| Caso de uso: Si el cliente entra fuera de horario, ve «Por ahora no recibimos canciones», a qué hora abren los pedidos y el horario de la semana, junto con «Ver qué está sonando». Lo mismo pasa con pausa o mantenimiento, cada uno con su mensaje. En esos estados no puede pedir ni votar, pero sí ver la cola pública. El estado coincide con lo configurado por el administrador. | Criterios de aceptación: -Con el servicio activo, la aplicación permite buscar, solicitar y votar. (RF-003) -Fuera del horario programado, en pausa manual o en mantenimiento, se muestra un mensaje claro del estado. (RF-003) -En esos estados no se puede solicitar ni votar, pero sí ver la cola pública. (RF-003) -El estado mostrado coincide con la configuración del administrador (RF-036, RF-038, RF-039). (RF-003) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-31 · Limpiar las listas entre sesiones

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-29 | Requisitos: RF-038, RNF-014 |
|---|---|---|---|

| Nombre: Como administrador, quiero un modo de mantenimiento que limpie la cola sin perder el historial, para empezar cada noche desde cero. | Atributos o entradas al sistema: -Acción «Modo mantenimiento». -Confirmación. |
|---|---|
| Caso de uso: Al terminar la noche, el administrador pulsa «Modo mantenimiento» en la barra superior y confirma. El sistema reinicia la cola y los contadores de votos para dejar todo listo para la siguiente noche, pero conserva el historial de solicitudes, votos y reproducciones para la analítica. Mientras dura, los clientes ven «en mantenimiento». | Criterios de aceptación: -Se pide confirmación antes de limpiar. (RF-038) -La cola y los contadores de votos de la sesión se reinician. (RF-038) -El histórico de solicitudes, votos y reproducciones se conserva. (RF-038) -Durante el mantenimiento, los clientes ven el estado «en mantenimiento» (RF-003). (RF-038) -La acción queda registrada en la bitácora. (RF-038) |

Pantalla del mockup

Diagrama de flujo

### PB-32 · Saltar canciones desde el panel

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-20, PB-10 | Requisitos: RF-040 |
|---|---|---|---|

| Nombre: Como Operador, quiero saltar la canción que suena, para cortar una que no corresponde. | Atributos o entradas al sistema: -Canción que suena. -Acción: saltar, pausar o reanudar. -Usuario del personal. |
|---|---|
| Caso de uso: En la tarjeta «Suena ahora» del panel, el operador pulsa «Saltar». La reproducción pasa de inmediato a la siguiente canción de la cola o de la lista por defecto, y la saltada queda «omitida». También puede pausar y reanudar la reproducción sin alterar la cola. Cada acción queda en el registro con usuario, canción y hora. | Criterios de aceptación: -Saltar una canción pasa de inmediato a la siguiente de la cola o de la lista por defecto. (RF-040) -La canción saltada queda marcada como «omitida». (RF-040) -Pausar y reanudar la reproducción no altera la cola. (RF-040) -Cada acción queda registrada con usuario, canción y hora. (RF-040) |

Pantalla del mockup

Diagrama de flujo

### PB-33 · Bloquear contenido inapropiado

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-20, PB-10 | Requisitos: RF-042, RNF-006, RNF-014 |
|---|---|---|---|

| Nombre: Como administrador, quiero bloquear un video por un tiempo definido, para mantener un ambiente adecuado. | Atributos o entradas al sistema: -Video a bloquear. -Motivo. -Duración: hasta fin de sesión, número de horas o indefinida. |
|---|---|
| Caso de uso: Desde «Suena ahora» o desde una fila de la «Cola», el administrador pulsa «Bloquear», indica el motivo y la duración. El video sale de la cola si estaba ahí y ya no se puede pedir: quien lo intente recibe un mensaje de que no está disponible. El bloqueo vence solo al cumplirse su duración y queda registrado con usuario, video, motivo y hora (por ejemplo, «Bloqueaste “La Bachata”»). | Criterios de aceptación: -Un video bloqueado no puede solicitarse; si ya estaba en la cola, se retira. (RF-042) -El bloqueo pide un motivo y una duración. (RF-042) -El cliente que intenta pedirlo recibe un mensaje de que no está disponible. (RF-042) -El bloqueo vence automáticamente al cumplirse su duración. (RF-042) -La acción queda registrada con usuario, video, motivo y hora. (RF-042) |

Pantalla del mockup

Diagrama de flujo

### PB-34 · Consultar y desbloquear contenido

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-33 | Requisitos: RF-043 |
|---|---|---|---|

| Nombre: Como administrador, quiero ver los bloqueos vigentes y levantar uno antes de tiempo, para corregir bloqueos que ya no aplican. | Atributos o entradas al sistema: -Filtro: vigentes o vencidos. -Bloqueo seleccionado. -Acción: desbloquear o extender. |
|---|---|
| Caso de uso: En la tarjeta «Canciones bloqueadas», el administrador ve cada video con su motivo, quién lo bloqueó, inicio y vencimiento, y puede filtrar entre vigentes y vencidos. Con «Desbloquear» el video queda habilitado al instante para nuevas solicitudes; también puede extender un bloqueo. Cada cambio queda en la bitácora. | Criterios de aceptación: -La lista muestra video, motivo, quién lo bloqueó, inicio y vencimiento. (RF-043) -Desbloquear un video lo habilita de inmediato para solicitudes. (RF-043) -Se puede filtrar por vigentes y vencidos. (RF-043) -Cada cambio queda registrado en la bitácora. (RF-043) |

Pantalla del mockup

Diagrama de flujo

### PB-35 · Crear y consultar listas por defecto

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-10 | Requisitos: RF-027, RF-028 |
|---|---|---|---|

| Nombre: Como administrador, quiero crear y ver mis listas de reproducción por defecto, para tener música lista cuando no hay pedidos. | Atributos o entradas al sistema: -Nombre (obligatorio y único) y descripción opcional de la lista. -Texto de búsqueda de listas. |
|---|---|
| Caso de uso: El administrador crea una lista por defecto con nombre y, si quiere, una descripción; queda inactiva y vacía hasta que le agregue canciones. En la tarjeta «Lista de la casa» ve cuál está activa (por ejemplo, «Salsa y tropical de la casa, 42 canciones»). Al abrir una lista ve sus canciones en orden, con título, artista, duración y miniatura, y la duración total. | Criterios de aceptación: -La lista se crea con un nombre obligatorio y único dentro del establecimiento. (RF-027) -Una lista nueva queda inactiva y vacía hasta que se le agreguen canciones. (RF-027) -La creación queda registrada en la bitácora. (RF-027) -La lista de listas indica cuál está activa. (RF-028) -El detalle de una lista muestra sus canciones en orden con título, artista, duración y miniatura. (RF-028) -Se muestra la duración total de la lista. (RF-028) -Se puede buscar una lista por nombre. (RF-028) |

Pantalla del mockup

Diagrama de flujo

### PB-36 · Armar una lista: agregar, quitar y ordenar canciones

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-35, PB-08 | Requisitos: RF-031, RF-032 |
|---|---|---|---|

| Nombre: Como administrador, quiero buscar canciones y ordenarlas en una lista, para diseñar la ambientación del bar. | Atributos o entradas al sistema: -Lista seleccionada. -Texto de búsqueda en YouTube. -Canción a agregar, quitar o mover. -Confirmación al quitar. |
|---|---|
| Caso de uso: Dentro de una lista, el administrador busca en YouTube con la misma búsqueda que usan los clientes y agrega canciones; solo entran videos disponibles, no bloqueados y que no estén ya en la lista. También puede quitar canciones (con confirmación) y reordenarlas; el orden se respeta si la rotación es secuencial. Una lista activa nunca puede quedar vacía. | Criterios de aceptación: -La búsqueda usa la misma integración de YouTube que el cliente (RF-005 y RF-006). (RF-031) -Solo se agregan videos disponibles y reproducibles (RF-007). (RF-031) -Una canción no se repite dentro de la misma lista. (RF-031) -No se puede agregar contenido que esté bloqueado. (RF-031) -Quitar una canción pide confirmación y no afecta a las demás. (RF-032) -El nuevo orden se guarda y se respeta en la reproducción cuando el modo es secuencial. (RF-032) -Si se quita la canción que está sonando, termina normalmente y no vuelve a reproducirse en ese ciclo. (RF-032) -Una lista activa no puede quedar vacía. (RF-032) |

Pantalla del mockup

No hay mockup del editor de listas; se accede desde la tarjeta «Lista de la casa».

Diagrama de flujo

### PB-37 · Activar una lista y definir su rotación

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-35 | Requisitos: RF-033 |
|---|---|---|---|

| Nombre: Como administrador, quiero elegir qué lista suena y en qué orden, para variar la música cuando no hay pedidos. | Atributos o entradas al sistema: -Lista por defecto a activar. -Modo de rotación: secuencial o aleatorio. |
|---|---|
| Caso de uso: En la tarjeta «Lista de la casa», el administrador pulsa «Cambiar», elige la lista que debe sonar cuando no hay pedidos y su modo de rotación. Al activarla, la anterior se desactiva; el cambio se aplica desde la siguiente canción, sin cortar la actual. Cuando la lista termina, vuelve a empezar. | Criterios de aceptación: -Solo una lista está activa por establecimiento; activar otra desactiva la anterior. (RF-033) -El modo de rotación (secuencial o aleatorio) se puede cambiar. (RF-033) -Al terminar la lista, vuelve a empezar de forma cíclica. (RF-033) -El cambio de lista activa se aplica desde la siguiente canción, sin cortar la actual. (RF-033) -El cambio queda registrado en la bitácora. (RF-033) |

Pantalla del mockup

Diagrama de flujo

### PB-38 · Que la música nunca se detenga

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-20, PB-36, PB-37 | Requisitos: RF-020 |
|---|---|---|---|

| Nombre: Como administrador, quiero que suene la lista por defecto cuando no hay pedidos y vuelvan los pedidos cuando lleguen, para no tener silencios. | Atributos o entradas al sistema: -Estado de la cola (vacía o con solicitudes). -Lista por defecto activa. |
|---|---|
| Caso de uso: Cuando la cola se vacía, el reproductor pasa solo a la lista por defecto activa, sin silencios; la app lo indica («Mientras tanto suena la lista de la casa»). Si llega una solicitud mientras suena la lista, la solicitud suena al terminar la canción actual. Si no hay lista activa, el administrador recibe una alerta. Cada transición queda registrada como evento. | Criterios de aceptación: -Con la cola vacía, empieza a sonar la lista por defecto activa sin intervención. (RF-020) -Si llega una solicitud mientras suena la lista por defecto, la solicitud suena al terminar la canción actual. (RF-020) -Si no hay lista por defecto activa, se muestra una alerta al administrador. (RF-020) -Cada transición queda registrada como evento (alimenta el cumplimiento de la lista por defecto). (RF-020) |

| Pantalla del mockup | Diagrama de flujo |
|---|---|

### PB-39 · Editar y eliminar listas por defecto

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-35 | Requisitos: RF-029, RF-030, RNF-014 |
|---|---|---|---|

| Nombre: Como administrador, quiero renombrar y eliminar listas que ya no uso, para mantenerlas ordenadas. | Atributos o entradas al sistema: -Lista seleccionada. -Nuevo nombre o descripción. -Confirmación al eliminar. |
|---|---|
| Caso de uso: El administrador cambia el nombre o la descripción de una lista; el nombre debe seguir siendo único y editar la lista activa no interrumpe la canción que suena. También puede eliminar una lista que no esté activa, con confirmación; el historial de lo que esa lista reprodujo se conserva para la analítica. | Criterios de aceptación: -Los cambios se guardan solo si el nombre sigue siendo único en el establecimiento. (RF-029) -Editar una lista activa no interrumpe la canción que está sonando. (RF-029) -Cada modificación queda registrada en la bitácora. (RF-029) -Se pide confirmación antes de eliminar. (RF-030) -No se puede eliminar la lista activa; antes hay que activar otra. (RF-030) -El historial de reproducción de canciones de esa lista se conserva para la analítica. (RF-030) -La eliminación queda registrada en la bitácora. (RF-030) |

Pantalla del mockup

No hay mockup de la edición; se accede desde la tarjeta «Lista de la casa».

Diagrama de flujo

### PB-40 · Consultar la bitácora de acciones

| Prioridad: Must | Sprint: Sprint 3 | Depende de: PB-10 | Requisitos: RF-044, RNF-014 |
|---|---|---|---|

| Nombre: Como administrador, quiero ver quién hizo qué y cuándo en el panel, para tener trazabilidad. | Atributos o entradas al sistema: -Filtros: fecha, usuario y tipo de acción. |
|---|---|
| Caso de uso: En la tarjeta «Registro de acciones» el administrador ve quién hizo qué y cuándo esa noche (por ejemplo, «10:41 p. m. Bloqueaste “La Bachata”»). En la vista completa puede filtrar por fecha, usuario y tipo de acción, e incluye los errores de la API de YouTube. Los registros no se pueden editar ni borrar desde la aplicación. | Criterios de aceptación: -Cada registro incluye usuario, acción, objeto afectado, fecha y hora. (RF-044) -Se puede filtrar por fecha, usuario y tipo de acción. (RF-044) -Los registros no se pueden editar ni borrar desde la aplicación. (RF-044) -Se incluyen los errores de la API de YouTube (RF-008). (RF-044) |

Pantalla del mockup

Diagrama de flujo

### PB-41 · Registrar los eventos del bar

| Prioridad: Should | Sprint: Sprint 3 | Depende de: PB-10 | Requisitos: RF-045 |
|---|---|---|---|

| Nombre: Como administrador, quiero registrar los eventos del establecimiento con su tipo, para comparar noches normales con noches de evento. | Atributos o entradas al sistema: -Nombre del evento. -Fecha y horario. -Tipo: noche regular o evento especial. -Descripción. |
|---|---|
| Caso de uso: El administrador registra en el calendario los eventos del bar, como una noche regular o un evento especial, con nombre, fecha, horario y tipo. Puede editar o eliminar eventos futuros; de uno ya realizado solo puede cambiar la descripción. Cuando una sesión coincide con la fecha de un evento, toma su tipo, y el calendario alimenta el pipeline analítico. | Criterios de aceptación: -Se puede crear un evento con nombre, fecha, horario y tipo. (RF-045) -Se puede editar y eliminar un evento futuro; uno ya realizado solo se puede editar su descripción. (RF-045) -La sesión de operación toma el tipo de evento del calendario si coincide con su fecha. (RF-045) -Los eventos son una fuente de datos para el pipeline analítico (RF-047). (RF-045) |

| Pantalla del mockup Sin pantalla en los mockups. No hay mockup del calendario de eventos. | Diagrama de flujo |
|---|---|

### PB-42 · Tener un manual de uso del panel

| Prioridad: Should | Sprint: Sprint 3 | Depende de: PB-32, PB-33 | Requisitos: RNF-016 |
|---|---|---|---|

| Nombre: Como Operador (mesero, bartender o DJ), quiero un manual breve con pasos ilustrados, para operar el panel sin pedir ayuda técnica. | Atributos o entradas al sistema: -Clic en «Manual de uso» desde el panel. -Rol del usuario. |
|---|---|
| Caso de uso: Desde el enlace «Manual de uso» del encabezado del panel, el operador abre un manual breve con pasos ilustrados para controlar la reproducción, saltar canciones, pausar el servicio, retirar solicitudes y bloquear contenido. El administrador encuentra además las secciones de usuarios, listas por defecto, horarios, bitácora y tablero. El manual se actualiza cada vez que cambia una funcionalidad descrita. | Criterios de aceptación: -El manual es breve y usa pasos ilustrados. (RNF-016) -Para el Operador cubre controlar la reproducción, saltar canciones, pausar el servicio, retirar solicitudes y bloquear contenido. (RNF-016) -Para el Administrador cubre además usuarios, listas por defecto, horarios, bitácora y tablero analítico. (RNF-016) -Está disponible en formato digital y accesible desde el panel. (RNF-016) -Se actualiza cada vez que cambia una funcionalidad descrita. (RNF-016) |

Pantalla del mockup

Diagrama de flujo

## Sprint 4

5 historias de usuario.

### PB-43 · Reunir los datos en un solo lugar

| Prioridad: Must | Sprint: Sprint 4 | Depende de: PB-16, PB-29 | Requisitos: RF-047 |
|---|---|---|---|

| Nombre: Como analista de datos, quiero reunir en un solo lugar los eventos de la app, los metadatos de YouTube y el calendario de eventos, para analizar la operación sin juntar datos a mano. | Atributos o entradas al sistema: -Eventos de la app. -Metadatos de las canciones desde la API de YouTube. -Calendario de eventos del establecimiento. -Modo de ejecución: por sesión o por lote. |
|---|---|
| Caso de uso: Es un proceso del sistema, sin pantalla. Al cerrar una sesión, o en un lote programado, el pipeline toma los eventos de la app, los metadatos de YouTube y el calendario, y los carga en el almacenamiento analítico. Si se ejecuta dos veces, no duplica registros, y cada ejecución deja constancia de cuántos registros ingirió y cuántos rechazó. | Criterios de aceptación: -La ingesta incluye las tres fuentes: eventos, metadatos de YouTube y calendario de eventos. (RF-047) -Se puede ejecutar por sesión o por lote, de forma repetible. (RF-047) -Una ejecución repetida no duplica registros. (RF-047) -Cada ejecución deja constancia de cuántos registros ingirió y cuántos rechazó. (RF-047) |

| Pantalla del mockup Sin pantalla en los mockups. Proceso interno del sistema, sin pantalla. | Diagrama de flujo |
|---|---|

### PB-44 · Tener datos limpios y anonimizados

| Prioridad: Must | Sprint: Sprint 4 | Depende de: PB-43 | Requisitos: RF-048, RNF-007, RNF-010, RNF-015 |
|---|---|---|---|

| Nombre: Como analista de datos, quiero datos depurados, con el identificador de dispositivo anonimizado y procesados con Spark, para analizar sin exponer a los clientes. | Atributos o entradas al sistema: -Datos ingeridos (PB-43). -Reglas de limpieza: duplicados, nulos y formatos. -Clave para transformar el identificador de dispositivo. |
|---|---|
| Caso de uso: Es un proceso del sistema, sin pantalla. Con Spark, el pipeline elimina duplicados, trata nulos, corrige formatos y reporta lo que descartó. Antes del análisis transforma el identificador de dispositivo para que no se pueda ver el real, y carga todo en un modelo que se consulta por sesión, noche, día de la semana, canción y género. | Criterios de aceptación: -Los duplicados y registros inválidos se tratan y se reportan. (RF-048) -El identificador de dispositivo queda anonimizado en el modelo analítico. (RF-048) -El modelo analítico permite consultar por sesión, noche, día de la semana, canción y género. (RF-048) -El procesamiento usa un motor como Spark y queda documentado. (RF-048) |

| Pantalla del mockup Sin pantalla en los mockups. Proceso interno del sistema, sin pantalla. El requisito dice «anonimizado»; ver el hallazgo 4 del documento de analítica. | Diagrama de flujo |
|---|---|

### PB-45 · Ver el tablero de la sesión

| Prioridad: Must | Sprint: Sprint 4 | Depende de: PB-44 | Requisitos: RF-049, RF-050, RNF-015 |
|---|---|---|---|

| Nombre: Como administrador, quiero ver solicitudes, canción más popular, usuarios únicos, historial, tiempos de espera y cumplimiento de la lista por defecto, para saber cómo funcionó la noche. | Atributos o entradas al sistema: -Sesión o rango de fechas a consultar. -Modelo analítico (PB-44). |
|---|---|
| Caso de uso: El administrador abre «Tablero analítico» desde el encabezado del panel (las «Métricas de la noche» muestran un adelanto: solicitudes, teléfonos, votos y espera promedio). Elige una sesión o un rango de fechas y ve el total de solicitudes, la canción más popular, los usuarios únicos, el historial reproducido, la espera promedio y máxima, el porcentaje de tiempo de la lista por defecto frente a solicitudes y las solicitudes que nunca sonaron. | Criterios de aceptación: -Se muestran los cuatro indicadores para la sesión elegida. (RF-049) -Las cifras coinciden con una muestra auditada de los registros operativos. (RF-049) -Se puede elegir la sesión o el rango de fechas a consultar. (RF-049) -Solo el Administrador accede al tablero. (RF-049) -Se muestra el tiempo de espera promedio y máximo por sesión. (RF-050) -Se muestra el porcentaje de tiempo en que sonó la lista por defecto frente a solicitudes. (RF-050) -Se registran las solicitudes que nunca llegaron a reproducirse. (RF-050) -Los valores se calculan solo con datos de la sesión consultada. (RF-050) |

Pantalla del mockup

Diagrama de flujo

### PB-46 · Detectar problemas con la analítica diagnóstica

| Prioridad: Must | Sprint: Sprint 4 | Depende de: PB-44 | Requisitos: RF-052 |
|---|---|---|---|

| Nombre: Como administrador, quiero saber qué canciones muy pedidas no sonaron y si pocos dispositivos concentran los votos, para corregir la operación. | Atributos o entradas al sistema: -Sesiones a analizar. -Umbrales configurables: votos mínimos, concentración y participación. |
|---|---|
| Caso de uso: En el tablero, la sección de diagnóstico lista las canciones con muchos votos que no llegaron a sonar y su causa probable (omitida, bloqueada o fuera de tiempo), avisa cuando pocos teléfonos concentran gran parte de los votos y marca las sesiones con baja participación. Los umbrales están documentados y el administrador puede ajustarlos. | Criterios de aceptación: -Se listan las canciones con muchos votos que no llegaron a sonar y la causa probable (omitida, bloqueada o fuera de tiempo). (RF-052) -Se señala cuando pocos dispositivos concentran una parte alta de los votos, con un umbral definido. (RF-052) -Se marcan las sesiones por debajo de un umbral de participación. (RF-052) -Los umbrales están documentados y son configurables. (RF-052) |

| Pantalla del mockup Sin pantalla en los mockups. No hay mockup del tablero analítico; se accede desde «Tablero analítico» en el panel. | Diagrama de flujo |
|---|---|

### PB-47 · Ver géneros y franjas de mayor participación

| Prioridad: Should | Sprint: Sprint 4 | Depende de: PB-44 | Requisitos: RF-051 |
|---|---|---|---|

| Nombre: Como administrador, quiero saber qué géneros y horas tienen más participación, para programar mejor la música. | Atributos o entradas al sistema: -Filtros: sesión, día de la semana o rango de fechas. -Género de cada canción según los metadatos de YouTube. |
|---|---|
| Caso de uso: El administrador ve el ranking de géneros por número de solicitudes y votos, y las franjas horarias con más solicitudes, votos y usuarios únicos. Puede filtrar por sesión, día de la semana o rango de fechas. Las canciones sin género en los metadatos aparecen como «sin clasificar». | Criterios de aceptación: -Se muestra un ranking de géneros por número de solicitudes y votos. (RF-051) -El género se toma de los metadatos de YouTube cuando existan; si no, la canción queda como «sin clasificar». (RF-051) -Se muestran las franjas horarias con más solicitudes, votos y usuarios únicos. (RF-051) -Se puede filtrar por sesión, día de la semana o rango de fechas. (RF-051) |

| Pantalla del mockup Sin pantalla en los mockups. No hay mockup del tablero analítico. Sobre el género, ver el hallazgo 7 del documento de analítica. | Diagrama de flujo |
|---|---|

## Sprint 5

7 historias de usuario.

### PB-48 · Comparar noches, días y tipos de evento

| Prioridad: Could | Sprint: Sprint 5 | Depende de: PB-44, PB-41, PB-29 | Requisitos: RF-053 |
|---|---|---|---|

| Nombre: Como administrador, quiero comparar lo planeado con lo reproducido por noche, día y tipo de evento, para evaluar la ambientación. | Atributos o entradas al sistema: -Noches a comparar (dos o más). -Agrupación: día de la semana o tipo de evento. |
|---|---|
| Caso de uso: El administrador elige dos o más noches y compara sus indicadores, o los agrupa por día de la semana y por tipo de evento del calendario. El tablero contrasta lo planeado (la lista por defecto) con lo que sonó por solicitudes, en gráficos y tablas legibles. | Criterios de aceptación: -Se pueden comparar dos o más noches entre sí. (RF-053) -Los indicadores se agrupan por día de la semana y por tipo de evento del calendario. (RF-053) -Se contrasta lo planeado (lista por defecto) con lo reproducido (solicitudes). (RF-053) -La comparación se presenta en gráficos y tablas legibles. (RF-053) |

| Pantalla del mockup Sin pantalla en los mockups. No hay mockup del tablero analítico. | Diagrama de flujo |
|---|---|

### PB-49 · Recibir recomendaciones para la operación

| Prioridad: Should | Sprint: Sprint 5 | Depende de: PB-46, PB-48 | Requisitos: RF-054 |
|---|---|---|---|

| Nombre: Como administrador, quiero recibir sugerencias basadas en los datos para la lista por defecto, los bloqueos y los horarios, para mejorar la operación. | Atributos o entradas al sistema: -Resultados de la analítica de géneros, diagnóstico y comparativos. |
|---|---|
| Caso de uso: El tablero muestra recomendaciones concretas, como canciones o géneros sugeridos para la lista por defecto, y cada una indica el dato en que se basa. Desde la recomendación, el administrador va directo a la gestión de listas, bloqueos u horarios; nada se aplica solo. | Criterios de aceptación: -Se muestran recomendaciones concretas, por ejemplo canciones o géneros sugeridos para la lista por defecto. (RF-054) -Cada recomendación indica el dato en que se basa. (RF-054) -El Administrador puede ir directo a la gestión de listas, bloqueos u horarios desde la recomendación. (RF-054) -Las recomendaciones no se aplican solas: requieren acción del Administrador. (RF-054) |

| Pantalla del mockup Sin pantalla en los mockups. No hay mockup de las recomendaciones. | Diagrama de flujo |
|---|---|

### PB-50 · Cambiar mi voto a otra canción

| Prioridad: Could | Sprint: Sprint 5 | Depende de: PB-14, PB-04 | Requisitos: RF-014 |
|---|---|---|---|

| Nombre: Como cliente, quiero mover mi voto a otra canción, para corregir mi elección. | Atributos o entradas al sistema: -Canción de la que se retira el voto. -Canción que recibe el voto. -Identificador de dispositivo. |
|---|---|
| Caso de uso: En «La cola», el cliente que ya votó por una canción (botón «Votaste») puede retirar ese voto y darlo a otra, siempre que la primera siga en espera. Los contadores de ambas se actualizan de forma consistente y el teléfono nunca queda con más de un voto por canción. El cambio se registra como evento de cambio de voto. | Criterios de aceptación: -El cliente puede mover su voto de una canción a otra mientras la primera siga en espera. (RF-014) -Los contadores de ambas canciones se actualizan de forma consistente. (RF-014) -El dispositivo nunca queda con más de un voto por canción. (RF-014) -El cambio se registra como evento de cambio de voto. (RF-014) |

| Pantalla del mockup El mockup muestra el estado «Votaste»; la acción de mover el voto parte de ese botón. | Diagrama de flujo |
|---|---|

### PB-51 · Filtrar canciones por género, duración o popularidad

| Prioridad: Could | Sprint: Sprint 5 | Depende de: PB-08 | Requisitos: RF-055 |
|---|---|---|---|

| Nombre: Como cliente, quiero filtrar los resultados, para encontrar música más rápido. | Atributos o entradas al sistema: -Filtros: género, duración o popularidad (uno o varios). |
|---|---|
| Caso de uso: En la búsqueda o en la cola, el cliente aplica uno o varios filtros por género, duración o popularidad, y la lista se actualiza en menos de 2 segundos. Al quitar los filtros vuelve la lista completa. Las canciones sin género aparecen como «sin clasificar». | Criterios de aceptación: -Se puede aplicar uno o varios filtros a la vez. (RF-055) -Quitar los filtros restablece la lista completa. (RF-055) -Los filtros no hacen más lento el uso de la aplicación (menos de 2 segundos). (RF-055) -Las canciones sin género disponible aparecen como «sin clasificar». (RF-055) |

| Pantalla del mockup El mockup aún no muestra los filtros; irían junto a la caja de búsqueda. | Diagrama de flujo |
|---|---|

### PB-52 · Reproducir por el sistema de sonido del bar

| Prioridad: Could | Sprint: Sprint 5 | Depende de: PB-20 | Requisitos: RF-056 |
|---|---|---|---|

| Nombre: Como Operador, quiero que la música salga por los parlantes del bar, para que suene en el local real. | Atributos o entradas al sistema: -Salida de audio del reproductor. -Sistema de sonido conectado. |
|---|---|
| Caso de uso: El reproductor de la pantalla del bar envía el audio al sistema de sonido del local, para que la música pedida suene por los parlantes. El operador verifica la conexión desde el panel; si la salida falla, recibe una alerta y la reproducción sigue en el reproductor web. | Criterios de aceptación: -El audio de la canción en reproducción sale por el sistema de sonido conectado. (RF-056) -El Operador puede verificar la conexión desde el panel. (RF-056) -Si falla la salida, se muestra una alerta y la reproducción sigue en el reproductor web. (RF-056) |

Pantalla del mockup

El mockup muestra el reproductor público; la conexión al sonido se verifica desde el panel.

Diagrama de flujo

### PB-53 · Respaldar las listas más populares

| Prioridad: Could | Sprint: Sprint 5 | Depende de: PB-29 | Requisitos: RF-057 |
|---|---|---|---|

| Nombre: Como administrador, quiero que al cerrar la sesión se guarde una copia de las canciones más votadas, para analizarlas después. | Atributos o entradas al sistema: -Cierre de la sesión. -Canciones más votadas de la sesión. |
|---|---|
| Caso de uso: Es un proceso del sistema, sin pantalla. Al cerrarse la sesión, se guarda una copia con las canciones más votadas de esa noche en el almacenamiento analítico, asociada a la sesión. Si el respaldo falla, el error queda registrado y el cierre de la sesión sigue normalmente. | Criterios de aceptación: -Al cerrar la sesión se genera una copia con las canciones más votadas. (RF-057) -La copia queda en el almacenamiento analítico asociada a la sesión. (RF-057) -Si el respaldo falla, se registra el error y no afecta el cierre de la sesión. (RF-057) |

| Pantalla del mockup Sin pantalla en los mockups. Proceso interno del sistema, sin pantalla. | Diagrama de flujo |
|---|---|

### PB-54 · Seguir buscando aunque se agote la cuota de YouTube

| Prioridad: Could | Sprint: Sprint 5 | Depende de: PB-07 | Requisitos: RF-008 |
|---|---|---|---|

| Nombre: Como administrador, quiero que las búsquedas sigan funcionando aunque se agote la cuota diaria de YouTube, para no quedarme sin servicio en plena noche. | Atributos o entradas al sistema: -Texto de búsqueda. -Error de cuota agotada de la API de YouTube. -Resultados guardados en caché de búsquedas recientes. |
|---|---|
| Caso de uso: Si se agota la cuota diaria de YouTube en plena noche, la búsqueda no se cae: la app responde con resultados de búsquedas recientes guardados en caché, y el cliente ve un aviso de que pueden no estar completos. La cola y la música siguen funcionando, y el error queda registrado para el administrador. | Criterios de aceptación: -Ante un error de la API, el cliente ve un mensaje comprensible y puede reintentar. (RF-008) -La aplicación no se bloquea ni pierde la cola existente. (RF-008) -La música en curso y la lista por defecto siguen reproduciéndose aunque la API falle. (RF-008) -Cada error se registra con tipo, fecha y hora, y el administrador puede consultarlo. (RF-008) -Complementario del backlog (no está en RF-008): con la cuota agotada, la búsqueda responde con resultados en caché y avisa que pueden estar incompletos. |

| Pantalla del mockup El mockup no muestra el aviso de caché; aparece en la pantalla de búsqueda. | Diagrama de flujo |
|---|---|

## Imágenes incluidas en el original

- `word/media/image1.jpg`
- `word/media/image10.jpg`
- `word/media/image100.png`
- `word/media/image11.png`
- `word/media/image12.png`
- `word/media/image13.jpg`
- `word/media/image14.png`
- `word/media/image15.jpg`
- `word/media/image16.jpg`
- `word/media/image17.jpg`
- `word/media/image19.jpg`
- `word/media/image2.jpg`
- `word/media/image20.jpg`
- `word/media/image21.jpg`
- `word/media/image22.jpg`
- `word/media/image23.jpg`
- `word/media/image24.png`
- `word/media/image25.jpg`
- `word/media/image26.png`
- `word/media/image27.jpg`
- `word/media/image28.png`
- `word/media/image29.png`
- `word/media/image3.jpg`
- `word/media/image30.jpg`
- `word/media/image31.jpg`
- `word/media/image32.png`
- `word/media/image33.jpg`
- `word/media/image34.jpg`
- `word/media/image35.png`
- `word/media/image36.png`
- `word/media/image37.png`
- `word/media/image38.png`
- `word/media/image39.png`
- `word/media/image4.jpg`
- `word/media/image40.jpg`
- `word/media/image41.jpg`
- `word/media/image42.jpg`
- `word/media/image43.png`
- `word/media/image44.png`
- `word/media/image45.jpg`
- `word/media/image46.png`
- `word/media/image47.png`
- `word/media/image48.jpg`
- `word/media/image49.png`
- `word/media/image50.png`
- `word/media/image51.jpg`
- `word/media/image52.jpg`
- `word/media/image53.png`
- `word/media/image55.png`
- `word/media/image56.jpg`
- `word/media/image57.jpg`
- `word/media/image58.jpg`
- `word/media/image59.jpg`
- `word/media/image6.jpg`
- `word/media/image60.png`
- `word/media/image61.jpg`
- `word/media/image62.jpg`
- `word/media/image63.png`
- `word/media/image64.jpg`
- `word/media/image65.jpg`
- `word/media/image66.jpg`
- `word/media/image67.png`
- `word/media/image68.jpg`
- `word/media/image69.png`
- `word/media/image7.jpg`
- `word/media/image70.jpg`
- `word/media/image71.jpg`
- `word/media/image72.jpg`
- `word/media/image73.jpg`
- `word/media/image74.png`
- `word/media/image75.png`
- `word/media/image76.jpg`
- `word/media/image77.png`
- `word/media/image78.jpg`
- `word/media/image79.jpg`
- `word/media/image8.jpg`
- `word/media/image80.png`
- `word/media/image81.jpg`
- `word/media/image82.png`
- `word/media/image83.png`
- `word/media/image84.png`
- `word/media/image85.jpg`
- `word/media/image86.jpg`
- `word/media/image87.jpg`
- `word/media/image88.png`
- `word/media/image89.jpg`
- `word/media/image9.jpg`
- `word/media/image90.jpg`
- `word/media/image91.png`
- `word/media/image92.png`
- `word/media/image93.jpg`
- `word/media/image94.jpg`
- `word/media/image95.png`
- `word/media/image96.png`
- `word/media/image97.png`
- `word/media/image98.png`
- `word/media/image99.png`
