# Requisitos

> Texto extraído de `Requisitos .docx` con `scripts/extraer-fuentes.py`. El original manda: las imágenes (maquetas y diagramas) solo están en el .docx.

Descripción de Requisitos

SAMUEL ALZATE BERRIO

DERLY ELENA QUEJADA PEREA

JUAN DIEGO TAMARA

LI KUN WU LI

Corporación Universitaria Empresarial Alexander von Humboldt

Facultad de Ingenierías y Ciencias Básicas

Ingeniería De Software

Armenia, Quindío

2026

- CONTEXTO
Las tabernas, bares y establecimientos de esparcimiento nocturno enfrentan el desafío permanente de sostener una ambientación video musical que responda a los gustos diversos de su clientela. El método convencional, en el que el establecimiento o un encargado del sonido selecciona la totalidad del repertorio, produce desconexión entre la música programada y las preferencias del público presente, convierte al visitante en espectador pasivo de la experiencia, tiende a repetir un repertorio limitado y traslada las peticiones a un canal informal —la solicitud verbal al mesero o al bartender— que se pierde, se malinterpreta o se atiende sin ningún criterio de prioridad. Cuando varios clientes coinciden en una misma canción no existe mecanismo alguno que refleje esa popularidad, y la ausencia de transparencia sobre el estado de cada solicitud genera frustración en el cliente e interrumpe el flujo de trabajo del personal.

En este contexto surge la necesidad de implementar un sistema de información que digitalice y democratice la selección musical del establecimiento, abarcando desde el acceso del cliente sin barreras de descarga o registro, hasta la búsqueda de contenido, la solicitud, la votación colectiva, la conformación automática de una cola de reproducción ordenada por popularidad y la administración del servicio por parte del personal. Un sistema de esta naturaleza permitiría convertir a los asistentes en curadores colaborativos de la ambientación, reducir la carga operativa del personal y dejar evidencia trazable del comportamiento musical de cada sesión.

El eje problémico del presente proyecto nuclear radica en la necesidad de articular un

sistema de información construido para teléfonos móviles que integre la operación en

tiempo real del servicio (solicitud, votación, priorización, administración y la

reproducción que será web), con una capa analítica que transforme los datos generados en conocimiento accionable sobre las preferencias del público, cerrando así el ciclo entre la experiencia vivida cada noche y la mejora continua de la propuesta musical del establecimiento. El reto exige, además, valorar críticamente la pertinencia del enfoque móvil-agil como tendencia tecnológica capaz de digitalizar procesos tradicionales con tiempos de desarrollo reducidos.

## REQUISITOS FUNCIONALES

### Módulo 1. Acceso y estado del servicio

#### RF-001 — Acceso al establecimiento mediante código QR

| ID | RF-001 |
|---|---|
| Nombre | Acceso al establecimiento mediante código QR |
| Módulo | Acceso y estado del servicio |
| Actor principal | Cliente |
| Prioridad | Must |
| Caso de uso asociado | CU-01 — Acceder al establecimiento mediante QR |

Descripción

El sistema debe permitir que el cliente abra la aplicación del establecimiento registrado escaneando un código QR con la cámara de su teléfono, sin descargar ni instalar nada y sin crear una cuenta.

Criterios de aceptación

- Al escanear el QR vigente, la aplicación del establecimiento correcto abre en el navegador del teléfono.
- No se solicita descarga, instalación, registro ni inicio de sesión.
- La pantalla inicial carga en menos de 2 segundos con conexión móvil estándar.
- Un QR de un establecimiento distinto no muestra datos de otro establecimiento.

#### RF-002 — Identificación única de dispositivo

| ID | RF-002 |
|---|---|
| Nombre | Identificación única de dispositivo |
| Módulo | Acceso y estado del servicio |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-02 — Identificar dispositivo |

Descripción

El sistema debe asignar un identificador único a cada dispositivo en su primer acceso y reconocerlo en visitas posteriores, para limitar las votaciones, sin pedir datos personales al cliente.

Criterios de aceptación

- Al primer ingreso se genera un identificador y se conserva en el dispositivo.
- El mismo dispositivo conserva su identificador entre sesiones de la misma noche.
- No se solicita nombre, correo, teléfono ni otro dato personal.
- El identificador queda disponible para los eventos de uso (RF-046) y para el control de votos (RF-011).

#### RF-003 — Consulta del estado del servicio

| ID | RF-003 |
|---|---|
| Nombre | Consulta del estado del servicio |
| Módulo | Acceso y estado del servicio |
| Actor principal | Cliente |
| Prioridad | Must |
| Caso de uso asociado | CU-03 — Consultar estado del servicio |

Descripción

El sistema debe informar al cliente si el servicio de solicitudes está disponible, fuera de horario, pausado o en mantenimiento, y bloquear las solicitudes y votos mientras no esté disponible.

Criterios de aceptación

- Con el servicio activo, la aplicación permite buscar, solicitar y votar.
- Fuera del horario programado, en pausa manual o en mantenimiento, se muestra un mensaje claro del estado.
- En esos estados no se puede solicitar ni votar, pero sí ver la cola pública.
- El estado mostrado coincide con la configuración del administrador (RF-036, RF-038, RF-039).

#### RF-004 — Aviso de tratamiento de datos

| ID | RF-004 |
|---|---|
| Nombre | Aviso de tratamiento de datos |
| Módulo | Acceso y estado del servicio |
| Actor principal | Cliente |
| Prioridad | Must |
| Caso de uso asociado | CU-04 — Consultar aviso de tratamiento de datos |

Descripción

El sistema debe mostrar al cliente, en su primer ingreso y de forma consultable después, un aviso breve que explique qué datos se registran (identificador de dispositivo, solicitudes y votos), con qué fin y bajo qué política, conforme a la Ley 1581 de 2012.

Criterios de aceptación

- El aviso aparece en el primer ingreso sin impedir el uso inmediato de la aplicación.
- El texto indica qué se registra, para qué se usa y que no se piden datos de identificación como nombre, correo o teléfono.
- El cliente puede volver a consultarlo desde cualquier pantalla.
- La visualización del aviso queda registrada como evento.

### Módulo 2. Búsqueda e integración con YouTube

#### RF-005 — Búsqueda de canciones con autocompletado

| ID | RF-005 |
|---|---|
| Nombre | Búsqueda de canciones con autocompletado |
| Módulo | Búsqueda e integración con YouTube |
| Actor principal | Cliente |
| Prioridad | Must |
| Caso de uso asociado | CU-05 — Buscar canción |

Descripción

El sistema debe ofrecer una caja de búsqueda que sugiera canciones o artistas mientras el cliente escribe, consultando el contenido de YouTube mediante su API.

Criterios de aceptación

- Al escribir, aparecen sugerencias sin que el cliente tenga que enviar la búsqueda.
- La respuesta de la búsqueda tarda menos de 2 segundos en condiciones normales.
- Una búsqueda vacía o sin resultados muestra un mensaje claro y no genera error.
- Cada búsqueda queda registrada como evento de uso.

#### RF-006 — Presentación de resultados de búsqueda

| ID | RF-006 |
|---|---|
| Nombre | Presentación de resultados de búsqueda |
| Módulo | Búsqueda e integración con YouTube |
| Actor principal | Cliente |
| Prioridad | Must |
| Caso de uso asociado | CU-05 — Buscar canción |

Descripción

El sistema debe mostrar cada resultado con título, artista, duración y miniatura del video, obtenidos de la API de YouTube, para que el cliente identifique la canción correcta antes de solicitarla.

Criterios de aceptación

- Cada resultado muestra los cuatro datos: título, artista, duración y miniatura.
- Los datos provienen de la API de YouTube, no de una lista fija.
- La lista es legible y operable con una sola mano en pantalla de teléfono.
- Desde un resultado se puede iniciar la solicitud (RF-009).

#### RF-007 — Validación de disponibilidad del video

| ID | RF-007 |
|---|---|
| Nombre | Validación de disponibilidad del video |
| Módulo | Búsqueda e integración con YouTube |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-06 — Validar disponibilidad de video |

Descripción

El sistema debe verificar, antes de aceptar una solicitud, que el video exista, esté disponible y pueda reproducirse de forma incrustada, y volver a verificarlo antes de reproducirlo.

Criterios de aceptación

- Un video eliminado, privado, restringido o no incrustable se rechaza con un mensaje al cliente.
- Un video válido se acepta y se guardan sus metadatos (título, artista, duración, miniatura).
- Si un video deja de estar disponible antes de reproducirse, el sistema lo omite y sigue con el siguiente.
- Cada rechazo u omisión queda registrado como evento.

#### RF-008 — Manejo de errores de la API de YouTube

| ID | RF-008 |
|---|---|
| Nombre | Manejo de errores de la API de YouTube |
| Módulo | Búsqueda e integración con YouTube |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-07 — Gestionar errores de la integración |

Descripción

El sistema debe manejar de forma explícita los fallos de la API de YouTube (cuota agotada, servicio no disponible, tiempo de espera o respuesta inválida), de modo que el cliente y el administrador reciban un mensaje claro y la operación continúe.

Criterios de aceptación

- Ante un error de la API, el cliente ve un mensaje comprensible y puede reintentar.
- La aplicación no se bloquea ni pierde la cola existente.
- La música en curso y la lista por defecto siguen reproduciéndose aunque la API falle.
- Cada error se registra con tipo, fecha y hora, y el administrador puede consultarlo.

### Módulo 3. Solicitud y votación

#### RF-009 — Solicitud de canción

| ID | RF-009 |
|---|---|
| Nombre | Solicitud de canción |
| Módulo | Solicitud y votación |
| Actor principal | Cliente |
| Prioridad | Must |
| Caso de uso asociado | CU-08 — Solicitar canción |

Descripción

El sistema debe permitir que el cliente solicite una canción desde los resultados de búsqueda con un botón o gesto, agregándola a la lista de espera. Si la canción ya está en la cola, la solicitud se convierte en un voto.

Criterios de aceptación

- Al solicitar, la canción aparece en la cola pública con estado «en espera» y un voto inicial.
- Si la canción ya estaba en la cola, no se duplica: se suma un voto (sujeto a RF-011).
- No se puede solicitar contenido bloqueado (RF-042) ni con el servicio no disponible (RF-003).
- Cada solicitud genera un evento con canción, dispositivo (seudonimizado), sesión y hora.

#### RF-010 — Votación de canciones solicitadas

| ID | RF-010 |
|---|---|
| Nombre | Votación de canciones solicitadas |
| Módulo | Solicitud y votación |
| Actor principal | Cliente |
| Prioridad | Must |
| Caso de uso asociado | CU-09 — Votar canción |

Descripción

El sistema debe permitir que el cliente vote por canciones que ya están en la cola, para aumentar su popularidad y adelantar su turno de reproducción.

Criterios de aceptación

- Desde la cola, el cliente puede votar por cualquier canción en espera.
- Cada voto válido suma uno al contador de la canción.
- La posición de la canción en la cola se recalcula tras cada voto.
- Cada voto genera un evento de uso.

#### RF-011 — Restricción de un voto por dispositivo

| ID | RF-011 |
|---|---|
| Nombre | Restricción de un voto por dispositivo |
| Módulo | Solicitud y votación |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-09 — Votar canción |

Descripción

El sistema debe permitir un único voto por dispositivo sobre cada canción, usando el identificador de dispositivo, sin exigir registro del cliente.

Criterios de aceptación

- Un segundo voto del mismo dispositivo por la misma canción se rechaza con un mensaje.
- El mismo dispositivo sí puede votar por canciones distintas.
- El control se mantiene al recargar la página o reabrir el navegador.
- Con varios dispositivos votando a la vez no se pierden ni se duplican votos.

#### RF-012 — Confirmación visual del voto

| ID | RF-012 |
|---|---|
| Nombre | Confirmación visual del voto |
| Módulo | Solicitud y votación |
| Actor principal | Cliente |
| Prioridad | Must |
| Caso de uso asociado | CU-09 — Votar canción |

Descripción

El sistema debe mostrar al cliente, de inmediato, una confirmación visual de que su voto o solicitud fue registrado, y marcar las canciones por las que ya votó.

Criterios de aceptación

- Tras votar, el botón cambia de estado y aparece una confirmación en menos de 1 segundo.
- Las canciones votadas por el dispositivo se distinguen de las demás.
- Si el voto falla, se muestra un mensaje de error y el contador no cambia.
- La confirmación no depende solo del color (accesibilidad).

#### RF-013 — Consulta del estado de mis solicitudes

| ID | RF-013 |
|---|---|
| Nombre | Consulta del estado de mis solicitudes |
| Módulo | Solicitud y votación |
| Actor principal | Cliente |
| Prioridad | Should |
| Caso de uso asociado | CU-10 — Consultar estado de mis solicitudes |

Descripción

El sistema debe permitir que el cliente vea el estado de las canciones que solicitó o votó desde su dispositivo: en espera, sonando, reproducida, omitida o bloqueada.

Criterios de aceptación

- El cliente ve una lista de sus solicitudes con su estado actual.
- El estado se actualiza solo, sin recargar.
- Una canción omitida o bloqueada muestra el motivo general (por ejemplo, «video no disponible»).
- Solo se muestran las solicitudes del propio dispositivo.

#### RF-014 — Cambio de voto

| ID | RF-014 |
|---|---|
| Nombre | Cambio de voto |
| Módulo | Solicitud y votación |
| Actor principal | Cliente |
| Prioridad | Could |
| Caso de uso asociado | CU-11 — Cambiar voto |

Descripción

El sistema podrá permitir que el cliente retire su voto de una canción y lo emita por otra (funcionalidad opcional de bonificación).

Criterios de aceptación

- El cliente puede mover su voto de una canción a otra mientras la primera siga en espera.
- Los contadores de ambas canciones se actualizan de forma consistente.
- El dispositivo nunca queda con más de un voto por canción.
- El cambio se registra como evento de cambio de voto.

### Módulo 4. Cola pública y reproducción

#### RF-015 — Cola pública ordenada por popularidad

| ID | RF-015 |
|---|---|
| Nombre | Cola pública ordenada por popularidad |
| Módulo | Cola pública y reproducción |
| Actor principal | Público |
| Prioridad | Must |
| Caso de uso asociado | CU-12 — Visualizar cola pública |

Descripción

El sistema debe ofrecer una vista web pública, apta para pantalla del establecimiento o teléfono, que muestre las canciones en espera ordenadas por número de votos, con el contador de votos de cada una.

Criterios de aceptación

- La cola se ordena de mayor a menor número de votos; el empate se resuelve por antigüedad de la solicitud.
- Cada canción muestra título, artista, miniatura y contador de votos.
- La vista es accesible sin iniciar sesión.
- Con la cola vacía se muestra un mensaje y se indica que suena la lista por defecto.

#### RF-016 — Indicación de la canción en reproducción

| ID | RF-016 |
|---|---|
| Nombre | Indicación de la canción en reproducción |
| Módulo | Cola pública y reproducción |
| Actor principal | Público |
| Prioridad | Must |
| Caso de uso asociado | CU-12 — Visualizar cola pública |

Descripción

El sistema debe destacar en la vista pública cuál canción suena en ese momento y de dónde proviene (solicitud de clientes o lista por defecto).

Criterios de aceptación

- La canción en reproducción se distingue claramente del resto de la cola.
- Se muestra título, artista y miniatura.
- Al cambiar de canción, la indicación se actualiza sin recargar.
- Se indica si la canción viene de una solicitud o de la lista por defecto.

#### RF-017 — Actualización automática de la cola

| ID | RF-017 |
|---|---|
| Nombre | Actualización automática de la cola |
| Módulo | Cola pública y reproducción |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-12 — Visualizar cola pública |

Descripción

El sistema debe actualizar la cola, los contadores de votos y la canción en reproducción en todas las pantallas conectadas, sin que nadie recargue la página.

Criterios de aceptación

- Un voto o solicitud nuevo se refleja en las demás pantallas en pocos segundos.
- El reordenamiento por popularidad se aplica solo.
- Si se pierde la conexión, la pantalla muestra un aviso y se resincroniza al recuperarla.
- Funciona con varios dispositivos conectados al mismo tiempo.

#### RF-018 — Tiempo estimado de espera

| ID | RF-018 |
|---|---|
| Nombre | Tiempo estimado de espera |
| Módulo | Cola pública y reproducción |
| Actor principal | Público |
| Prioridad | Must |
| Caso de uso asociado | CU-12 — Visualizar cola pública |

Descripción

El sistema debe calcular y mostrar el tiempo estimado que falta para que suene cada canción de la cola, a partir de la duración de las canciones que van antes.

Criterios de aceptación

- Cada canción en espera muestra su tiempo estimado en minutos.
- El cálculo suma la duración restante de la canción actual y la de las que la preceden.
- El estimado se recalcula cuando cambian votos, orden o la canción actual.
- Se indica que es una estimación.

#### RF-019 — Reproducción automática de la cola

| ID | RF-019 |
|---|---|
| Nombre | Reproducción automática de la cola |
| Módulo | Cola pública y reproducción |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-13 — Reproducir cola |

Descripción

El sistema debe reproducir en un reproductor web (o SmartTV) la canción con más votos de la cola y pasar automáticamente a la siguiente al terminar, usando la reproducción incrustada de YouTube.

Criterios de aceptación

- Al terminar una canción, comienza la siguiente de mayor popularidad sin intervención.
- Cada canción reproducida cambia a estado «reproducida» y sale de la cola.
- Se registra un evento de reproducción con inicio y fin.
- No se descarga ni se redistribuye el contenido; solo se reproduce mediante YouTube.

#### RF-020 — Transición automática entre solicitudes y lista por defecto

| ID | RF-020 |
|---|---|
| Nombre | Transición automática entre solicitudes y lista por defecto |
| Módulo | Cola pública y reproducción |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-14 — Gestionar continuidad musical |

Descripción

El sistema debe pasar automáticamente a la lista de reproducción por defecto activa cuando no haya solicitudes, y volver a las solicitudes de los clientes apenas reaparezcan, sin silencios.

Criterios de aceptación

- Con la cola vacía, empieza a sonar la lista por defecto activa sin intervención.
- Si llega una solicitud mientras suena la lista por defecto, la solicitud suena al terminar la canción actual.
- Si no hay lista por defecto activa, se muestra una alerta al administrador.
- Cada transición queda registrada como evento (alimenta el cumplimiento de la lista por defecto).

### Módulo 5. Autenticación y gestión de usuarios del personal

Estos usuarios son el personal del establecimiento (administradores y operadores). Los clientes no tienen usuario: se identifican solo por dispositivo (RF-002).

#### RF-021 — Autenticación del personal

| ID | RF-021 |
|---|---|
| Nombre | Autenticación del personal |
| Módulo | Autenticación y gestión de usuarios |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-15 — Iniciar sesión en el panel |

Descripción

El sistema debe exigir inicio de sesión con credenciales para entrar al panel de administración y permitir cerrar la sesión de forma explícita.

Criterios de aceptación

- Sin sesión iniciada no se puede acceder a ninguna pantalla del panel.
- Credenciales incorrectas muestran un mensaje genérico, sin indicar cuál dato falló.
- Tras varios intentos fallidos consecutivos la cuenta se bloquea temporalmente.
- Al cerrar sesión o por inactividad prolongada, la sesión termina y se pide autenticarse de nuevo.
- Las contraseñas se almacenan cifradas, nunca en texto plano.

#### RF-022 — Control de acceso por rol

| ID | RF-022 |
|---|---|
| Nombre | Control de acceso por rol |
| Módulo | Autenticación y gestión de usuarios |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-15 — Iniciar sesión en el panel |

Descripción

El sistema debe limitar las funciones del panel según el rol del usuario: el Administrador accede a toda la gestión y la analítica; el Operador (mesero, bartender o DJ) solo controla la reproducción y la cola.

Criterios de aceptación

- Un Operador no puede gestionar usuarios, listas por defecto, horarios ni establecimientos.
- Un Operador sí puede saltar canciones, pausar y retirar solicitudes inapropiadas.
- Un intento de acceso a una función no permitida se rechaza y se registra en la bitácora.
- Los permisos de cada rol son visibles para el Administrador al crear o editar un usuario.

#### RF-023 — Crear usuario del personal

| ID | RF-023 |
|---|---|
| Nombre | Crear usuario del personal |
| Módulo | Autenticación y gestión de usuarios |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-16 — Gestionar usuarios del personal |

Descripción

El sistema debe permitir que el Administrador cree un usuario del personal, indicando nombre, correo, rol (Administrador u Operador) y el establecimiento al que pertenece, con una contraseña inicial que deba cambiarse en el primer ingreso.

Criterios de aceptación

- El usuario se crea solo si todos los campos obligatorios son válidos.
- No se permiten dos usuarios con el mismo correo.
- El usuario nuevo queda asociado a un único establecimiento y a un rol.
- En el primer ingreso se obliga a cambiar la contraseña inicial.
- La creación queda registrada en la bitácora.

#### RF-024 — Consultar y listar usuarios

| ID | RF-024 |
|---|---|
| Nombre | Consultar y listar usuarios |
| Módulo | Autenticación y gestión de usuarios |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-16 — Gestionar usuarios del personal |

Descripción

El sistema debe mostrar al Administrador la lista de usuarios de su establecimiento, con búsqueda y filtros por rol y estado, y el detalle de cada usuario.

Criterios de aceptación

- La lista muestra nombre, correo, rol y estado (activo o inactivo).
- Se puede buscar por nombre o correo y filtrar por rol y estado.
- El Administrador solo ve los usuarios de su propio establecimiento.
- Nunca se muestra la contraseña.

#### RF-025 — Editar usuario del personal

| ID | RF-025 |
|---|---|
| Nombre | Editar usuario del personal |
| Módulo | Autenticación y gestión de usuarios |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-16 — Gestionar usuarios del personal |

Descripción

El sistema debe permitir que el Administrador modifique el nombre, el correo y el rol de un usuario, y restablezca su contraseña.

Criterios de aceptación

- Los cambios se guardan solo si los datos son válidos y el correo no está repetido.
- Un cambio de rol se aplica en el siguiente ingreso del usuario.
- No se puede quitar el rol de Administrador al último administrador activo.
- Al restablecer la contraseña, se genera una temporal que debe cambiarse al ingresar.
- Cada modificación queda registrada en la bitácora.

#### RF-026 — Desactivar o eliminar usuario del personal

| ID | RF-026 |
|---|---|
| Nombre | Desactivar o eliminar usuario del personal |
| Módulo | Autenticación y gestión de usuarios |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-16 — Gestionar usuarios del personal |

Descripción

El sistema debe permitir que el Administrador desactive o elimine un usuario del personal. La eliminación es lógica: el usuario deja de poder ingresar, pero sus acciones pasadas se conservan en la bitácora.

Criterios de aceptación

- Un usuario desactivado no puede iniciar sesión, y su sesión activa se cierra.
- Se pide confirmación antes de eliminar.
- Un administrador no puede eliminarse a sí mismo ni al último administrador activo.
- Se puede reactivar un usuario desactivado.
- Las acciones históricas del usuario siguen visibles en la bitácora.

### Módulo 6. Gestión de listas de reproducción por defecto

Una lista por defecto es el conjunto de canciones que suena cuando no hay solicitudes activas. El establecimiento puede tener varias listas, pero solo una activa a la vez.

#### RF-027 — Crear lista de reproducción por defecto

| ID | RF-027 |
|---|---|
| Nombre | Crear lista de reproducción por defecto |
| Módulo | Gestión de listas por defecto |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-17 — Gestionar listas por defecto |

Descripción

El sistema debe permitir que el Administrador cree una lista por defecto con nombre y descripción opcional, asociada a su establecimiento.

Criterios de aceptación

- La lista se crea con un nombre obligatorio y único dentro del establecimiento.
- Una lista nueva queda inactiva y vacía hasta que se le agreguen canciones.
- La creación queda registrada en la bitácora.

#### RF-028 — Consultar listas de reproducción por defecto

| ID | RF-028 |
|---|---|
| Nombre | Consultar listas de reproducción por defecto |
| Módulo | Gestión de listas por defecto |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-17 — Gestionar listas por defecto |

Descripción

El sistema debe mostrar al Administrador las listas por defecto del establecimiento y el detalle de cada una: canciones, duración total, estado (activa o inactiva) y fecha de última modificación.

Criterios de aceptación

- La lista de listas indica cuál está activa.
- El detalle de una lista muestra sus canciones en orden con título, artista, duración y miniatura.
- Se muestra la duración total de la lista.
- Se puede buscar una lista por nombre.

#### RF-029 — Editar lista de reproducción por defecto

| ID | RF-029 |
|---|---|
| Nombre | Editar lista de reproducción por defecto |
| Módulo | Gestión de listas por defecto |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-17 — Gestionar listas por defecto |

Descripción

El sistema debe permitir que el Administrador cambie el nombre y la descripción de una lista por defecto.

Criterios de aceptación

- Los cambios se guardan solo si el nombre sigue siendo único en el establecimiento.
- Editar una lista activa no interrumpe la canción que está sonando.
- Cada modificación queda registrada en la bitácora.

#### RF-030 — Eliminar lista de reproducción por defecto

| ID | RF-030 |
|---|---|
| Nombre | Eliminar lista de reproducción por defecto |
| Módulo | Gestión de listas por defecto |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-17 — Gestionar listas por defecto |

Descripción

El sistema debe permitir que el Administrador elimine una lista por defecto que no esté activa, conservando el historial de reproducciones que ya hizo.

Criterios de aceptación

- Se pide confirmación antes de eliminar.
- No se puede eliminar la lista activa; antes hay que activar otra.
- El historial de reproducción de canciones de esa lista se conserva para la analítica.
- La eliminación queda registrada en la bitácora.

#### RF-031 — Agregar canciones a una lista por defecto

| ID | RF-031 |
|---|---|
| Nombre | Agregar canciones a una lista por defecto |
| Módulo | Gestión de listas por defecto |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-17 — Gestionar listas por defecto |

Descripción

El sistema debe permitir que el Administrador busque canciones en YouTube y las agregue a una lista por defecto, validando su disponibilidad.

Criterios de aceptación

- La búsqueda usa la misma integración de YouTube que el cliente (RF-005 y RF-006).
- Solo se agregan videos disponibles y reproducibles (RF-007).
- Una canción no se repite dentro de la misma lista.
- No se puede agregar contenido que esté bloqueado.

#### RF-032 — Quitar y reordenar canciones de una lista por defecto

| ID | RF-032 |
|---|---|
| Nombre | Quitar y reordenar canciones de una lista por defecto |
| Módulo | Gestión de listas por defecto |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-17 — Gestionar listas por defecto |

Descripción

El sistema debe permitir que el Administrador quite canciones de una lista por defecto y cambie su orden.

Criterios de aceptación

- Quitar una canción pide confirmación y no afecta a las demás.
- El nuevo orden se guarda y se respeta en la reproducción cuando el modo es secuencial.
- Si se quita la canción que está sonando, termina normalmente y no vuelve a reproducirse en ese ciclo.
- Una lista activa no puede quedar vacía.

#### RF-033 — Activar lista por defecto y configurar su rotación

| ID | RF-033 |
|---|---|
| Nombre | Activar lista por defecto y configurar su rotación |
| Módulo | Gestión de listas por defecto |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-18 — Configurar lista activa y rotación |

Descripción

El sistema debe permitir que el Administrador elija cuál lista por defecto está activa y cómo rota: en orden secuencial o aleatorio, de forma cíclica.

Criterios de aceptación

- Solo una lista está activa por establecimiento; activar otra desactiva la anterior.
- El modo de rotación (secuencial o aleatorio) se puede cambiar.
- Al terminar la lista, vuelve a empezar de forma cíclica.
- El cambio de lista activa se aplica desde la siguiente canción, sin cortar la actual.
- El cambio queda registrado en la bitácora.

### Módulo 7. Administración del servicio

#### RF-034 — Gestión de establecimientos (CRUD)

| ID | RF-034 |
|---|---|
| Nombre | Gestión de establecimientos (CRUD) |
| Módulo | Administración del servicio |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-19 — Gestionar establecimientos |

Descripción

El sistema debe permitir registrar, consultar, editar y desactivar los establecimientos que usan el servicio. Todo el contenido (colas, listas, usuarios, sesiones y datos analíticos) pertenece a un establecimiento.

Criterios de aceptación

- Se puede registrar un establecimiento con nombre, dirección y zona horaria.
- Se puede consultar y editar la información de un establecimiento registrado.
- Un establecimiento desactivado no permite solicitudes y conserva sus datos históricos.
- Los datos de un establecimiento no son visibles para otro.
- Los cambios quedan registrados en la bitácora.

#### RF-035 — Generación y publicación del código QR

| ID | RF-035 |
|---|---|
| Nombre | Generación y publicación del código QR |
| Módulo | Administración del servicio |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-20 — Generar código QR |

Descripción

El sistema debe generar un código QR único por establecimiento, que dirija a su aplicación, y permitir al Administrador descargarlo para imprimirlo o mostrarlo en pantalla, y regenerarlo si es necesario.

Criterios de aceptación

- El QR es único por establecimiento y abre su aplicación.
- Se puede descargar en un formato imprimible.
- Al regenerar el QR, el anterior deja de funcionar y se pide confirmación.
- El QR permanece operativo mientras el establecimiento esté activo y en horario.

#### RF-036 — Programación de horarios de operación

| ID | RF-036 |
|---|---|
| Nombre | Programación de horarios de operación |
| Módulo | Administración del servicio |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-21 — Programar operación |

Descripción

El sistema debe permitir que el Administrador cree, consulte, edite y elimine franjas de operación por día de la semana, y que el servicio (y el QR) se active y desactive solo según esa programación.

Criterios de aceptación

- Se pueden definir una o varias franjas por día (hora de inicio y fin).
- Franjas que se solapan no se pueden guardar.
- Al llegar la hora de inicio, el servicio se activa; al llegar la de fin, se desactiva, sin intervención.
- El Administrador puede activar o desactivar el servicio manualmente, por encima de la programación.
- Cada cambio queda registrado en la bitácora.

#### RF-037 — Gestión de sesiones de operación

| ID | RF-037 |
|---|---|
| Nombre | Gestión de sesiones de operación |
| Módulo | Administración del servicio |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-22 — Gestionar sesión de operación |

Descripción

El sistema debe abrir una sesión de operación cuando el servicio se activa y cerrarla cuando se desactiva. Todas las solicitudes, votos y reproducciones se asocian a la sesión, que es la unidad de análisis (por noche).

Criterios de aceptación

- La sesión se abre y cierra automáticamente según el horario, o manualmente por el Administrador.
- Cada evento de uso se asocia a la sesión vigente.
- Al cerrar, la sesión conserva su fecha, hora de inicio y fin, y el tipo de evento del calendario, si lo hay.
- Hay una sola sesión abierta por establecimiento a la vez.

#### RF-038 — Modo de mantenimiento

| ID | RF-038 |
|---|---|
| Nombre | Modo de mantenimiento |
| Módulo | Administración del servicio |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-23 — Activar modo de mantenimiento |

Descripción

El sistema debe ofrecer un modo de mantenimiento que limpie la cola y los votos entre sesiones, dejando el sistema listo para la siguiente noche sin perder el historial para la analítica.

Criterios de aceptación

- Se pide confirmación antes de limpiar.
- La cola y los contadores de votos de la sesión se reinician.
- El histórico de solicitudes, votos y reproducciones se conserva.
- Durante el mantenimiento, los clientes ven el estado «en mantenimiento» (RF-003).
- La acción queda registrada en la bitácora.

#### RF-039 — Pausa manual del sistema

| ID | RF-039 |
|---|---|
| Nombre | Pausa manual del sistema |
| Módulo | Administración del servicio |
| Actor principal | Administrador |
| Prioridad | Should |
| Caso de uso asociado | CU-24 — Pausar y reanudar el sistema |

Descripción

El sistema debe permitir que el Administrador pause manualmente la recepción de solicitudes y votos, y la reanude después, sin cerrar la sesión.

Criterios de aceptación

- En pausa, los clientes no pueden solicitar ni votar, y ven un mensaje de pausa.
- La cola y los votos existentes se conservan.
- Al reanudar, el servicio vuelve a su estado normal.
- La pausa y la reanudación quedan registradas en la bitácora.

#### RF-040 — Control de la reproducción

| ID | RF-040 |
|---|---|
| Nombre | Control de la reproducción |
| Módulo | Administración del servicio |
| Actor principal | Operador |
| Prioridad | Must |
| Caso de uso asociado | CU-25 — Controlar reproducción |

Descripción

El sistema debe ofrecer al personal (Administrador u Operador) una interfaz para controlar la reproducción: pausar, reanudar y saltar la canción actual.

Criterios de aceptación

- Saltar una canción pasa de inmediato a la siguiente de la cola o de la lista por defecto.
- La canción saltada queda marcada como «omitida».
- Pausar y reanudar la reproducción no altera la cola.
- Cada acción queda registrada con usuario, canción y hora.

#### RF-041 — Retiro de solicitudes de la cola

| ID | RF-041 |
|---|---|
| Nombre | Retiro de solicitudes de la cola |
| Módulo | Administración del servicio |
| Actor principal | Operador |
| Prioridad | Should |
| Caso de uso asociado | CU-26 — Moderar cola |

Descripción

El sistema debe permitir que el personal retire una canción de la cola de espera cuando no corresponda reproducirla (error, duplicado o contenido inadecuado).

Criterios de aceptación

- La canción retirada desaparece de la cola pública y sus votos dejan de contar.
- Se solicita un motivo (por ejemplo, inapropiada, duplicada, otro).
- El cliente que la pidió ve el estado «retirada» (RF-013).
- La acción queda registrada en la bitácora.

#### RF-042 — Bloqueo temporal de contenido inapropiado

| ID | RF-042 |
|---|---|
| Nombre | Bloqueo temporal de contenido inapropiado |
| Módulo | Administración del servicio |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-27 — Bloquear contenido |

Descripción

El sistema debe permitir que el personal bloquee un video por un tiempo definido (hasta el fin de la sesión, por un número de horas o hasta que se desbloquee), para que no pueda solicitarse ni reproducirse.

Criterios de aceptación

- Un video bloqueado no puede solicitarse; si ya estaba en la cola, se retira.
- El bloqueo pide un motivo y una duración.
- El cliente que intenta pedirlo recibe un mensaje de que no está disponible.
- El bloqueo vence automáticamente al cumplirse su duración.
- La acción queda registrada con usuario, video, motivo y hora.

#### RF-043 — Gestión de contenido bloqueado

| ID | RF-043 |
|---|---|
| Nombre | Gestión de contenido bloqueado |
| Módulo | Administración del servicio |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-27 — Bloquear contenido |

Descripción

El sistema debe permitir que el Administrador consulte los bloqueos vigentes y vencidos, desbloquee un video antes de tiempo y extienda un bloqueo.

Criterios de aceptación

- La lista muestra video, motivo, quién lo bloqueó, inicio y vencimiento.
- Desbloquear un video lo habilita de inmediato para solicitudes.
- Se puede filtrar por vigentes y vencidos.
- Cada cambio queda registrado en la bitácora.

#### RF-044 — Bitácora de acciones administrativas

| ID | RF-044 |
|---|---|
| Nombre | Bitácora de acciones administrativas |
| Módulo | Administración del servicio |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-28 — Consultar bitácora |

Descripción

El sistema debe registrar las acciones del personal (usuarios, listas, horarios, bloqueos, saltos, pausas, mantenimiento) y los errores de la integración con YouTube, y permitir al Administrador consultarlos.

Criterios de aceptación

- Cada registro incluye usuario, acción, objeto afectado, fecha y hora.
- Se puede filtrar por fecha, usuario y tipo de acción.
- Los registros no se pueden editar ni borrar desde la aplicación.
- Se incluyen los errores de la API de YouTube (RF-008).

#### RF-045 — Gestión del calendario de eventos del establecimiento

| ID | RF-045 |
|---|---|
| Nombre | Gestión del calendario de eventos del establecimiento |
| Módulo | Administración del servicio |
| Actor principal | Administrador |
| Prioridad | Should |
| Caso de uso asociado | CU-29 — Gestionar calendario de eventos |

Descripción

El sistema debe permitir que el Administrador cree, consulte, edite y elimine eventos del establecimiento (por ejemplo, noche regular o evento especial), con fecha, horario y tipo, para contrastar lo planeado con lo reproducido en la analítica.

Criterios de aceptación

- Se puede crear un evento con nombre, fecha, horario y tipo.
- Se puede editar y eliminar un evento futuro; uno ya realizado solo se puede editar su descripción.
- La sesión de operación toma el tipo de evento del calendario si coincide con su fecha.
- Los eventos son una fuente de datos para el pipeline analítico (RF-047).

### Módulo 8. Datos y analítica

#### RF-046 — Registro de eventos de uso

| ID | RF-046 |
|---|---|
| Nombre | Registro de eventos de uso |
| Módulo | Datos y analítica |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-30 — Registrar eventos de uso |

Descripción

El sistema debe registrar un evento por cada acción relevante de la aplicación: solicitud, voto, cambio de voto, reproducción (inicio y fin), omisión, bloqueo, transición a lista por defecto y apertura o cierre de sesión.

Criterios de aceptación

- Cada evento guarda tipo, fecha y hora, sesión, establecimiento, canción (si aplica) e identificador de dispositivo.
- Los eventos se guardan aunque la acción del cliente falle de forma parcial, indicando el resultado.
- El registro no retrasa la respuesta al cliente de forma perceptible.
- Los eventos quedan disponibles para el pipeline analítico.

#### RF-047 — Ingesta de datos para analítica

| ID | RF-047 |
|---|---|
| Nombre | Ingesta de datos para analítica |
| Módulo | Datos y analítica |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-31 — Ejecutar pipeline de datos |

Descripción

El sistema debe ingerir en el almacenamiento analítico los eventos de la aplicación, los metadatos de las canciones obtenidos de la API de YouTube y el calendario de eventos del establecimiento.

Criterios de aceptación

- La ingesta incluye las tres fuentes: eventos, metadatos de YouTube y calendario de eventos.
- Se puede ejecutar por sesión o por lote, de forma repetible.
- Una ejecución repetida no duplica registros.
- Cada ejecución deja constancia de cuántos registros ingirió y cuántos rechazó.

#### RF-048 — Limpieza, transformación y modelo analítico

| ID | RF-048 |
|---|---|
| Nombre | Limpieza, transformación y modelo analítico |
| Módulo | Datos y analítica |
| Actor principal | Sistema |
| Prioridad | Must |
| Caso de uso asociado | CU-31 — Ejecutar pipeline de datos |

Descripción

El sistema debe limpiar y transformar los datos ingeridos (duplicados, nulos, formatos) y cargarlos en un modelo analítico, procesado con un motor como Spark, seudonimizado el identificador de dispositivo antes del análisis.

Criterios de aceptación

- Los duplicados y registros inválidos se tratan y se reportan.
- El identificador de dispositivo queda anonimizado en el modelo analítico.
- El modelo analítico permite consultar por sesión, noche, día de la semana, canción y género.
- El procesamiento usa un motor como Spark y queda documentado.

#### RF-049 — Tablero descriptivo de la sesión

| ID | RF-049 |
|---|---|
| Nombre | Tablero descriptivo de la sesión |
| Módulo | Datos y analítica |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-32 — Consultar tablero analítico |

Descripción

El sistema debe mostrar al Administrador un tablero con el total de solicitudes, la canción más popular de la sesión, los usuarios únicos participantes y el historial de canciones reproducidas.

Criterios de aceptación

- Se muestran los cuatro indicadores para la sesión elegida.
- Las cifras coinciden con una muestra auditada de los registros operativos.
- Se puede elegir la sesión o el rango de fechas a consultar.
- Solo el Administrador accede al tablero.

#### RF-050 — Indicadores de tiempos de espera y lista por defecto

| ID | RF-050 |
|---|---|
| Nombre | Indicadores de tiempos de espera y cumplimiento de la lista por defecto |
| Módulo | Datos y analítica |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-32 — Consultar tablero analítico |

Descripción

El sistema debe mostrar el tiempo de espera de las solicitudes (de solicitud a reproducción) y el cumplimiento de la lista por defecto, es decir, cuánto sonó lo planeado frente a lo reproducido.

Criterios de aceptación

- Se muestra el tiempo de espera promedio y máximo por sesión.
- Se muestra el porcentaje de tiempo en que sonó la lista por defecto frente a solicitudes.
- Se registran las solicitudes que nunca llegaron a reproducirse.
- Los valores se calculan solo con datos de la sesión consultada.

#### RF-051 — Géneros más solicitados y franjas de participación

| ID | RF-051 |
|---|---|
| Nombre | Géneros más solicitados y franjas de participación |
| Módulo | Datos y analítica |
| Actor principal | Administrador |
| Prioridad | Should |
| Caso de uso asociado | CU-32 — Consultar tablero analítico |

Descripción

El sistema debe mostrar los géneros más solicitados y las franjas horarias de mayor participación, para conocer los gustos del público en cada momento de la noche.

Criterios de aceptación

- Se muestra un ranking de géneros por número de solicitudes y votos.
- El género se toma de los metadatos de YouTube cuando existan; si no, la canción queda como «sin clasificar».
- Se muestran las franjas horarias con más solicitudes, votos y usuarios únicos.
- Se puede filtrar por sesión, día de la semana o rango de fechas.

#### RF-052 — Analítica diagnóstica

| ID | RF-052 |
|---|---|
| Nombre | Analítica diagnóstica |
| Módulo | Datos y analítica |
| Actor principal | Administrador |
| Prioridad | Must |
| Caso de uso asociado | CU-33 — Consultar analítica diagnóstica |

Descripción

El sistema debe identificar canciones muy solicitadas que no alcanzan a reproducirse, concentración excesiva de votos y sesiones con baja participación.

Criterios de aceptación

- Se listan las canciones con muchos votos que no llegaron a sonar y la causa probable (omitida, bloqueada o fuera de tiempo).
- Se marcan las sesiones por debajo de un umbral de participación.
- Los umbrales están documentados y son configurables.

#### RF-053 — Indicadores comparativos

| ID | RF-053 |
|---|---|
| Nombre | Indicadores comparativos por noche, día y tipo de evento |
| Módulo | Datos y analítica |
| Actor principal | Administrador |
| Prioridad | Should |
| Caso de uso asociado | CU-34 — Consultar indicadores comparativos |

Descripción

El sistema debe comparar los indicadores por noche, por día de la semana y por tipo de evento, para contrastar lo planeado con lo efectivamente reproducido.

Criterios de aceptación

- Se pueden comparar dos o más noches entre sí.
- Los indicadores se agrupan por día de la semana y por tipo de evento del calendario.
- Se contrasta lo planeado (lista por defecto) con lo reproducido (solicitudes).
- La comparación se presenta en gráficos y tablas legibles.

#### RF-054 — Retroalimentación de hallazgos a la operación

| ID | RF-054 |
|---|---|
| Nombre | Retroalimentación de hallazgos a la operación |
| Módulo | Datos y analítica |
| Actor principal | Administrador |
| Prioridad | Could |
| Caso de uso asociado | CU-35 — Aplicar hallazgos a la operación |

Descripción

El sistema debe presentar recomendaciones derivadas de los datos para ajustar la lista por defecto, los bloqueos y los horarios de activación, cerrando el ciclo entre la analítica y la operación.

Criterios de aceptación

- Se muestran recomendaciones concretas, por ejemplo canciones o géneros sugeridos para la lista por defecto.
- Cada recomendación indica el dato en que se basa.
- El Administrador puede ir directo a la gestión de listas, bloqueos u horarios desde la recomendación.
- Las recomendaciones no se aplican solas: requieren acción del Administrador.

### Módulo 9. Funcionalidades opcionales de bonificación

La guía otorga bonificación a los equipos que implementen estas funcionalidades; no son necesarias para cumplir el PMV.

#### RF-055 — Filtros por género, duración o popularidad

| ID | RF-055 |
|---|---|
| Nombre | Filtros por género, duración o popularidad |
| Módulo | Funcionalidades opcionales |
| Actor principal | Cliente |
| Prioridad | Could |
| Caso de uso asociado | CU-36 — Filtrar canciones |

Descripción

El sistema podrá permitir que el cliente filtre los resultados de búsqueda y la cola por género, duración o popularidad.

Criterios de aceptación

- Se puede aplicar uno o varios filtros a la vez.
- Quitar los filtros restablece la lista completa.
- Los filtros no hacen más lento el uso de la aplicación (menos de 2 segundos).
- Las canciones sin género disponible aparecen como «sin clasificar».

#### RF-056 — Integración con el sistema de sonido del establecimiento

| ID | RF-056 |
|---|---|
| Nombre | Integración con el sistema de sonido del establecimiento |
| Módulo | Funcionalidades opcionales |
| Actor principal | Operador |
| Prioridad | Could |
| Caso de uso asociado | CU-37 — Reproducir en el sistema de sonido |

Descripción

El sistema podrá enviar el audio del reproductor al sistema de sonido real del establecimiento, de modo que la música solicitada suene por sus parlantes.

Criterios de aceptación

- El audio de la canción en reproducción sale por el sistema de sonido conectado.
- El Operador puede verificar la conexión desde el panel.
- Si falla la salida, se muestra una alerta y la reproducción sigue en el reproductor web.

#### RF-057 — Respaldo automático de las listas más populares

| ID | RF-057 |
|---|---|
| Nombre | Respaldo automático de las listas más populares |
| Módulo | Funcionalidades opcionales |
| Actor principal | Sistema |
| Prioridad | Could |
| Caso de uso asociado | CU-38 — Respaldar listas populares |

Descripción

El sistema podrá guardar automáticamente, al cerrar cada sesión, una copia de la lista de canciones más populares para analizarla después.

Criterios de aceptación

- Al cerrar la sesión se genera una copia con las canciones más votadas.
- La copia queda en el almacenamiento analítico asociada a la sesión.
- Si el respaldo falla, se registra el error y no afecta el cierre de la sesión.

## REQUISITOS NO FUNCIONALES

| Código | Requisito no funcional | Descripción |
|---|---|---|
| RNF-001 | Usabilidad | El sistema deberá contar con una interfaz intuitiva y sencilla que permita al cliente buscar una canción, solicitarla y votar por ella sin necesidad de capacitación previa. |
| RNF-002 | Rendimiento | El sistema deberá responder a las acciones principales del usuario, como consultar la cola, registrar una solicitud o votar, en un tiempo máximo de 3 segundos en condiciones normales de operación, excluyendo los tiempos de respuesta de servicios externos. |
| RNF-003 | Disponibilidad | El sistema deberá mantener una disponibilidad mínima del 95 % durante las sesiones de prueba, siempre que exista conexión a internet y los servicios externos necesarios estén operativos. |
| RNF-004 | Compatibilidad | La interfaz del cliente deberá funcionar correctamente en dispositivos móviles Android e iOS mediante navegadores web modernos, sin exigir la instalación de una aplicación. |
| RNF-005 | Adaptabilidad | Las interfaces deberán ajustarse a diferentes tamaños y resoluciones de pantalla, garantizando la visualización correcta en teléfonos móviles, computadores y pantallas de televisión compatibles con navegadores web. |
| RNF-006 | Seguridad | El sistema deberá proteger las funciones administrativas mediante autenticación y control de permisos, evitando que usuarios no autorizados modifiquen la cola de reproducción o la configuración del establecimiento. |
| RNF-007 | Privacidad | El sistema deberá evitar la recopilación de datos personales innecesarios y utilizar identificadores seudónimos para contabilizar usuarios y controlar las votaciones por dispositivo. |
| RNF-008 | Integridad de los datos | El sistema deberá garantizar que las solicitudes y los votos se registren de forma consistente, evitando duplicaciones causadas por reintentos o errores de comunicación. |
| RNF-009 | Actualización en tiempo real | La cola pública y el panel administrativo deberán reflejar los cambios en solicitudes, votos y estado de reproducción en un tiempo máximo de 3 segundos en condiciones normales de conectividad. |
| RNF-010 | Escalabilidad | La arquitectura deberá permitir aumentar progresivamente la cantidad de usuarios concurrentes y establecimientos registrados sin requerir una reconstrucción completa del sistema. |
| RNF-011 | Interoperabilidad | El sistema deberá integrarse con la API de YouTube para consultar metadatos de videos musicales y utilizar mecanismos compatibles con sus condiciones de uso para la reproducción del contenido. |
| RNF-012 | Confiabilidad | El sistema deberá conservar la consistencia de la cola de reproducción y de los registros de solicitudes y votos ante errores temporales de conexión o recargas de la interfaz. |
| RNF-013 | Mantenibilidad | La solución deberá organizar sus componentes, reglas de negocio y estructuras de datos de manera que facilite la corrección de errores, la actualización de funcionalidades y el mantenimiento posterior. |
| RNF-014 | Trazabilidad y auditoría | El sistema deberá registrar los eventos relevantes de cada sesión, incluyendo solicitudes, votos, cambios de estado y reproducciones, con fecha y hora para facilitar su seguimiento y análisis posterior. |
| RNF-015 | Calidad y disponibilidad de los datos analíticos | El sistema deberá generar indicadores de canciones más solicitadas, participación por franja horaria, tiempos de espera y cumplimiento de la lista por defecto a partir de registros completos y consistentes. Los indicadores deberán actualizarse. |
| RNF-016 | Documentación de uso | El sistema deberá contar con un manual de uso del panel administrativo, breve y con pasos ilustrados, que permita al Administrador y al Operador realizar sus tareas principales sin asistencia técnica. Para el Operador, el manual cubrirá controlar la reproducción, saltar canciones, pausar el servicio, retirar solicitudes y bloquear contenido; para el Administrador, cubrirá además usuarios, listas por defecto, horarios, bitácora y tablero analítico. Deberá estar disponible en formato digital accesible desde el panel y actualizarse cada vez que cambie una funcionalidad descrita. |
