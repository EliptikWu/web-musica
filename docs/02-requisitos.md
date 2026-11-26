# 02 — Requisitos

Índice de trabajo de los requisitos del equipo. **El enunciado completo y los criterios
de aceptación de cada uno están en `fuente/extraido/requisitos.md`** (y las historias en
`fuente/extraido/historias-de-usuario-detalladas.md`); aquí no se copian para no tener
dos versiones. Este doc añade lo que el frontend necesita: qué historia lo cubre, en qué
iteración se hace y de quién es el trabajo.

Columna «Quién»: **F** = este repositorio, **B** = equipo de backend, **F+B** = los dos
(la interfaz aquí, la regla en el servidor).

## Requisitos funcionales

### Módulo 1. Acceso y estado del servicio

| ID | Requisito | Prioridad | Historia | It. | Quién |
|---|---|---|---|---|---|
| RF-001 | Acceso al establecimiento mediante código QR | Must | PB-03 | 1 | F+B |
| RF-002 | Identificación única de dispositivo | Must | PB-04 | 1 | F+B |
| RF-003 | Consulta del estado del servicio | Must | PB-30 | 1 | F+B |
| RF-004 | Aviso de tratamiento de datos | Must | PB-06 | 1 | F+B |

### Módulo 2. Búsqueda e integración con YouTube

| ID | Requisito | Prioridad | Historia | It. | Quién |
|---|---|---|---|---|---|
| RF-005 | Búsqueda de canciones con autocompletado | Must | PB-07 | 1 | F+B |
| RF-006 | Presentación de resultados de búsqueda | Must | PB-08 | 1 | F |
| RF-007 | Validación de disponibilidad del video | Must | PB-21 | 1, 2 | F+B |
| RF-008 | Manejo de errores de la API de YouTube | Must | PB-09, PB-54 | 1, 6 | F+B |

### Módulo 3. Solicitud y votación

| ID | Requisito | Prioridad | Historia | It. | Quién |
|---|---|---|---|---|---|
| RF-009 | Solicitud de canción | Must | PB-13 | 1 | F+B |
| RF-010 | Votación de canciones solicitadas | Must | PB-14 | 1 | F+B |
| RF-011 | Restricción de un voto por dispositivo | Must | PB-15 | 1 | F+B |
| RF-012 | Confirmación visual del voto | Must | PB-14 | 1 | F |
| RF-013 | Consulta del estado de mis solicitudes | Should | PB-22 | 1 | F+B |
| RF-014 | Cambio de voto | Could | PB-50 | 6 | F+B |
| RF-060 | Quitar mi voto (nuevo, 2026-11-12; criterio abajo) | Should | — | 12 | F+B |
| RF-061 | Retirar mi pedido (nuevo, 2026-11-12; criterio abajo) | Should | — | 12 | F+B |

**RF-060 y RF-061 (nuevos, correcciones del equipo del 2026-11-12, no están en las fuentes).**
- RF-060: desde «Votaste» (el diálogo de mover el voto), el teléfono quita su voto de una
  canción pendiente que pidió otro. Se puede volver a votar. El voto de tu propio pedido no
  se quita: para eso está RF-061.
- RF-061: desde «Tuya», el teléfono retira una canción que pidió y que todavía no suena,
  con confirmación. Sale de la cola, el teléfono recupera ese pedido (RN-01) y los votos que
  tenía se pierden. Una canción que ya suena no se retira (la quita el bar, RF-040, RF-041).
- Los dos solo con los pedidos abiertos, como votar; el servidor comprueba que el voto o el
  pedido sean de ese teléfono. El retiro queda con estado `retirada` y motivo «La retiraste
  tú» en «Tus pedidos» (RF-013).

### Módulo 4. Cola pública y reproducción

| ID | Requisito | Prioridad | Historia | It. | Quién |
|---|---|---|---|---|---|
| RF-015 | Cola pública ordenada por popularidad | Must | PB-17 | 1, 2 | F+B |
| RF-016 | Indicación de la canción en reproducción | Must | PB-17 | 1, 2 | F |
| RF-017 | Actualización automática de la cola | Must | PB-18 | 1, 2 | F+B |
| RF-018 | Tiempo estimado de espera (lo calcula el backend: `espera_seg`, ADR-20) | Must | PB-19 | 1, 2 | F+B |
| RF-019 | Reproducción automática de la cola | Must | PB-20 | 2 | F+B |
| RF-020 | Transición automática entre solicitudes y lista por defecto | Must | PB-38 | 2, 3 | F+B |
| RF-058 | Vinculación de la pantalla del bar (nuevo, 2026-11-10; criterio abajo) | Must | — | 7 | F+B |

**RF-058 (nuevo, no está en las fuentes del equipo).** Solo una pantalla que el
Administrador vinculó puede mover la cola; si no, cualquiera que abra
`/b/{bar}/pantalla` podría reportar «fin» y saltar canciones (RF-019). Criterios:
- Una pantalla sin vincular no reproduce ni reporta: muestra un código de 6 caracteres,
  grande para leerlo de lejos, que vence a los 10 min *(propuesto)* y se renueva solo.
- El Administrador escribe ese código en el panel («Bar y QR», «Pantallas») con un nombre
  («TV de la barra»). En 3 s o menos la TV pasa a mostrar la rockola sin que nadie toque
  la TV, salvo «Encender» (RNF-009).
- Los reportes de reproducción sin una pantalla vinculada responden `401 no_autenticado`.
- El panel lista las pantallas vinculadas y puede desvincular cualquiera; vincular y
  desvincular quedan en la bitácora (RNF-014). Una pantalla desvinculada deja de poner
  música y vuelve a mostrar un código, con un aviso, en 10 s o menos *(propuesto)*.
- La credencial de la pantalla va en una cookie httpOnly: el JavaScript nunca la ve
  (ADR-23).

### Módulo 5. Autenticación y gestión de usuarios

| ID | Requisito | Prioridad | Historia | It. | Quién |
|---|---|---|---|---|---|
| RF-021 | Autenticación del personal | Must | PB-10 | 3 | F+B |
| RF-022 | Control de acceso por rol | Must | PB-11 | 3 | F+B |
| RF-023 | Crear usuario del personal | Must | PB-23 | 4 | F+B |
| RF-024 | Consultar y listar usuarios | Must | PB-23 | 4 | F+B |
| RF-025 | Editar usuario del personal | Must | PB-24 | 4 | F+B |
| RF-026 | Desactivar o eliminar usuario del personal | Must | PB-24 | 4 | F+B |

### Módulo 6. Gestión de listas por defecto

| ID | Requisito | Prioridad | Historia | It. | Quién |
|---|---|---|---|---|---|
| RF-027 | Crear lista de reproducción por defecto | Must | PB-35 | 4 | F+B |
| RF-028 | Consultar listas de reproducción por defecto | Must | PB-35 | 4 | F+B |
| RF-029 | Editar lista de reproducción por defecto | Must | PB-39 | 4 | F+B |
| RF-030 | Eliminar lista de reproducción por defecto | Must | PB-39 | 4 | F+B |
| RF-031 | Agregar canciones a una lista por defecto | Must | PB-36 | 4 | F+B |
| RF-032 | Quitar y reordenar canciones de una lista por defecto | Must | PB-36 | 4 | F+B |
| RF-033 | Activar lista por defecto y configurar su rotación | Must | PB-37 | 4 | F+B |

### Módulo 7. Administración del servicio

| ID | Requisito | Prioridad | Historia | It. | Quién |
|---|---|---|---|---|---|
| RF-034 | Gestión de establecimientos (CRUD) | Must | PB-01, PB-26 | 4 | F+B |
| RF-035 | Generación y publicación del código QR | Must | PB-02 | 2, 4 | F+B |
| RF-036 | Programación de horarios de operación | Must | PB-28 | 4 | F+B |
| RF-037 | Gestión de sesiones de operación | Must | PB-12, PB-29 | 3 | F+B |
| RF-038 | Modo de mantenimiento | Must | PB-31 | 3 | F+B |
| RF-039 | Pausa manual del sistema | Should | PB-27 | 3 | F+B |
| RF-040 | Control de la reproducción | Must | PB-32 | 3 | F+B |
| RF-041 | Retiro de solicitudes de la cola | Should | PB-25 | 3 | F+B |
| RF-042 | Bloqueo temporal de contenido inapropiado | Must | PB-33 | 3 | F+B |
| RF-043 | Gestión de contenido bloqueado | Must | PB-34 | 3, 4 | F+B |
| RF-044 | Bitácora de acciones administrativas | Must | PB-40 | 3, 4 | F+B |
| RF-045 | Gestión del calendario de eventos del establecimiento | Should | PB-41 | 4 | F+B |
| RF-059 | Diseño de la rockola del establecimiento (nuevo, 2026-11-11; criterio abajo) | Should | — | 8–10 | F+B |
| RF-062 | Volumen de la música desde el panel (nuevo, 2026-11-12; criterio abajo) | Should | — | 13 | F+B |

**RF-059 (nuevo, no está en las fuentes del equipo).** El Administrador elige con qué
dirección visual ven los clientes la rockola de su bar, entre las tres del documento de
prototipos del equipo: **Rockola** (Tema #2, la de siempre), **Vidrio** (Tema #1) y
**Afiche** (Tema #3). Criterios:
- Se elige en el panel, en «Bar y QR», con una vista previa de cada diseño. Solo el
  Administrador la cambia (`editar_establecimiento`); el servidor lo comprueba y el cambio
  queda en la bitácora (RNF-014).
- El diseño cambia el teléfono, la pantalla del bar y, desde D19, el panel con sesión y el
  tablero. Entrar, cambiar la clave y la plataforma siguen en Rockola: no pertenecen a un bar.
- Cambiarlo no toca la cola, los votos ni las reglas: es solo la apariencia. Un teléfono o
  una TV abiertos lo toman la próxima vez que piden los datos del bar.
- Todos los diseños cumplen lo mismo: contraste AA, objetivos de 44 px, estados vacío,
  cargando y error, y «Votaste» y «Tuya» distinguibles sin color.
- Un bar nuevo empieza en Rockola. Sin dato del backend, también Rockola.

**RF-062 (nuevo, corrección del equipo del 2026-11-12).** El Operador y el Administrador
suben y bajan el volumen de la pantalla del bar desde el panel, sin tocar la TV.
- De 0 a 100 %, en pasos de 10 con las teclas o directo con la barra. 0 es silencio.
- La TV lo aplica en 3 s o menos (RNF-009) y lo recuerda: al recargar vuelve al último.
- No va a la bitácora (cambio del equipo, 2026-11-25: subir y bajar la llenaba y tapaba lo
  importante). El teléfono no lo muestra: la música la controla el bar.
- Un toque basta: el valor no vuelve atrás mientras el servidor responde.

### Módulo 8. Datos y analítica

| ID | Requisito | Prioridad | Historia | It. | Quién |
|---|---|---|---|---|---|
| RF-046 | Registro de eventos de uso | Must | PB-05, PB-16 | — | B (la simulación los imita) |
| RF-047 | Ingesta de datos para analítica | Must | PB-43 | — | B |
| RF-048 | Limpieza, transformación y modelo analítico | Must | PB-44 | — | B |
| RF-049 | Tablero descriptivo de la sesión | Must | PB-45 | 5 | F+B |
| RF-050 | Indicadores de tiempos de espera y lista por defecto | Must | PB-45 | 5 | F+B |
| RF-051 | Géneros más solicitados y franjas de participación | Should | PB-47 | 5 | F+B |
| RF-052 | Analítica diagnóstica | Must | PB-46 | 5 | F+B |
| RF-053 | Indicadores comparativos | Should | PB-48 | 5 | F+B |
| RF-054 | Retroalimentación de hallazgos a la operación | Could | PB-49 | 5 | F+B |
| RF-063 | Cubo de participación (OLAP) en el tablero | Should | PB-45, PB-47, PB-48 | 15 | F+B |
| RF-064 | Plataforma: crear bares y sus cuentas (nuevo, 2026-11-21) | Must | — | 24 | F+B |
| RF-065 | Destino del QR: web o descargar la app (nuevo, 2026-11-21) | Should | — | 26 | F+B |
| RF-066 | Escanear el QR del bar desde la app Android (nuevo, 2026-11-21) | Should | — | 29 | F |
| RF-067 | Cubo en vivo, con la noche abierta (nuevo, 2026-11-24) | Should | PB-45 | 31 | F+B |
| RF-068 | Catálogo de canciones: corregir el género a mano (nuevo, 2026-11-24) | Should | PB-47 | 31 | F+B |
| RF-069 | Cambiar de bar desde la app Android (nuevo, 2026-11-24) | Should | — | 32 | F |
| RF-070 | Aviso antes del cierre por horario (nuevo, 2026-11-25) | Should | — | 34 | F |

**RF-066 — Escanear el QR desde la app.** Añadido el 2026-11-21 (D20). La app Android abre la cámara
con «Escanear QR», desde la pantalla sin bar, los avisos de QR y la bienvenida. Desde el 2026-11-24
(D22), si se abre sin enlace y sin bar recordado (recién instalada), abre el escáner sola. Criterios:
- El permiso de cámara se pide solo al entrar al escáner: al tocar «Escanear QR» o cuando la app lo
  abre sola sin bar, una vez por apertura; si se cierra, queda la tecla. Si se niega, la app explica para qué
  sirve y cómo activarlo, y sigue funcionando sin cámara.
- La imagen se analiza en el teléfono: no se guarda, no se envía y su contenido no va a los logs.
- Solo abre enlaces `https` de la web de Rockola (`/q/<token>`, `/b/<slug>`) o un `/q/<token>`
  impreso con la dirección del backend. Cualquier otro QR se rechaza con un aviso y no se abre.
  En la simulación (D23, ADR-34) vale también el QR de la web local, de cualquier host y con
  `http`, pero solo con esas rutas; sin QR a mano, «Entrar al bar de demostración».
- Un QR válido entra por el mismo camino que un enlace, con sus avisos de QR viejo o sin bar.

**RF-069 — Cambiar de bar desde la app.** Añadido el 2026-11-24 (D22). La app recuerda el bar aunque
se cierre; quien se va a otro bar esa misma noche necesita salirse. Criterios:
- «Cambiar de bar», bajo la tecla principal de la bienvenida (abierta o cerrada), reemplaza a
  «Escanear otro QR».
- Al tocarlo, el teléfono olvida el bar y se abre el escáner. Si se cierra sin escanear, queda
  «Escanea el QR de tu mesa» y, al volver a abrir la app, no regresa al bar anterior.
- No borra nada en el servidor: solo cambia el bar que recuerda el teléfono.

**RF-070 — Aviso antes del cierre por horario.** Añadido el 2026-11-25 (punto 6 del equipo). En los
últimos 15 minutos *(propuesto)* antes de que el horario cierre las solicitudes, el teléfono muestra
arriba «Los pedidos se cierran en N min» y el panel dice a qué hora cierran y que lo que quede en la
cola se descarta. No sale con la apertura forzada. Sale de `servicio.hasta` de la foto: no necesita
nada nuevo del backend. Al cambiar el horario, el teléfono y la TV lo ven sin recargar.

**RF-065 — Destino del QR.** Añadido el 2026-11-21 (D18). El Administrador elige con un interruptor si
el QR de su bar abre la web o ofrece descargar la app. Cambia sin reimprimir: el código es el mismo.
Con «app», Android ve la descarga y la opción de seguir en la web; iPhone y computador siguen en
la web. Sin instalador publicado (`ROKOLA_APK_URL` y `ROKOLA_WEB_URL`), la opción no se puede elegir y el
QR abre la web.

**RF-064 — Plataforma (superadmin).** Añadido el 2026-11-21 (D16). Quien administra Rockola entra
en `/plataforma`, ve todos los bares con sus cuentas, crea un bar con su primer Administrador y
agrega cuentas de Operador o Administrador a un bar, y da una clave nueva a una cuenta que la
perdió. Cada cuenta nace con una clave temporal, que se
muestra una sola vez y obliga a cambiarla. No hay registro público. La plataforma no opera ningún
bar ni ve datos de sus clientes, y una sesión del panel de un bar no entra en ella. La lista muestra
cada bar plegado, con nombre, dirección web, dirección y horario; sus cuentas se despliegan. Con
varios bares, «Ir a un bar» deja solo el elegido, con sus cuentas abiertas (punto 15, 2026-11-25).

**RF-063 — Cubo de participación.** Añadido el 2026-11-20 (D15) para el Entregable 6 de la guía
del seminario. El Administrador explora en un cubo 3D **los KPI del entregable** por género,
franja horaria y día de la semana o tipo de evento: total de solicitudes, votos, canción más
popular, usuarios únicos, espera promedio y máxima, cumplimiento de la lista por defecto (parte
del tiempo que sonó la lista de la casa) y solicitudes que no sonaron. Se elige qué KPI ilumina
el cubo.

- Permite las operaciones slice, dice, roll-up, drill-down y pivot, y cada una muestra la tabla
  resultante. Todo lo que hace el cubo se puede hacer también con teclado y con la tabla.
- Los agregados los calcula Spark con `cube`. Los KPI no aditivos (usuarios únicos, espera,
  cumplimiento y canción más popular) **no se suman**: cada subtotal se calcula desde los hechos.
- Muestra el origen y la fecha de los datos (RNF-015) y solo lo ve el Administrador.
- *Nota (2026-11-24, D21):* el cubo **histórico** sigue saliendo de Spark. El cubo **en vivo**
  (RF-067) lo calcula el backend con `CUBE` en PostgreSQL al consultarlo, con las mismas reglas.

**RF-067 — Cubo en vivo.** Añadido el 2026-11-24 (D21), a pedido del equipo de backend, para que
en la presentación se vea el cubo cambiar cuando alguien pide o vota. Criterios:
- El cubo se ve **en vivo** (por defecto: todas las noches, también la abierta) o **histórico**
  (lo que cargó el pipeline). La pantalla dice cuál es: «Datos en vivo» con la luz encendida y
  la hora de la última actualización, o el origen y la fecha de la carga (RNF-015).
- En vivo se actualiza solo, sin recargar, en ≤ 3 s después de un pedido o voto (RNF-009), con
  el aviso `panel: ["analitica"]` del tiempo real. Varios avisos seguidos no repiten peticiones.
- El cubo dibuja de entrada los 5 géneros con más pedidos y votos; se pueden ver todos o elegir
  cualquiera con los filtros, y con filtro solo se dibuja lo elegido. El tamaño se ajusta solo.
- Una combinación sin datos (ninguna celda) se distingue de un 0: cubo hueco y «Sin datos».
- Suma el KPI «Tiempo de reproducción» (lo pedido más la lista de la casa).

**RF-068 — Catálogo de canciones.** Añadido el 2026-11-24 (D21). El Administrador ve las canciones
que usó su bar (pedidas, en listas, bloqueadas o que sonaron) con su género y quién se lo puso
(clasificación automática o a mano). Criterios:
- Filtro «Sin género» (por defecto) o «Todas», y búsqueda por título o artista.
- Elige un género del catálogo del backend y lo guarda; la fila cambia sin recargar y un aviso
  confirma. El backend la marca `MANUAL` y la clasificación automática ya no la cambia.
- El cubo en vivo la cuenta de inmediato; el histórico, cuando vuelva a correr el pipeline.
- Queda en la bitácora. Solo el Administrador; el Operador no la ve.
- Los géneros van en orden alfabético. «Clasificar las que faltan» vuelve a pasar por el
  clasificador las que no tienen género (de 10 en 10, hasta 100 por vez) y dice cuántas encontró
  y cuántas hay que poner a mano; nunca toca las `MANUAL` (punto 18, 2026-11-25).

### Módulo 9. Funcionalidades opcionales

| ID | Requisito | Prioridad | Historia | It. | Quién |
|---|---|---|---|---|---|
| RF-055 | Filtros por género, duración o popularidad | Could | PB-51 | 6 | F+B |
| RF-056 | Integración con el sistema de sonido del establecimiento | Could | PB-52 | — | Sin interfaz: `TBD` |
| RF-057 | Respaldo automático de las listas más populares | Could | PB-53 | — | B |

**Diferencias de prioridad entre fuentes:** el backlog pone PB-48 en Could (RF-053 dice
Should) y PB-49 en Should (RF-054 dice Could). Aquí manda el requisito.

## Requisitos no funcionales

| ID | Atributo | Lo que significa para el frontend | Dónde se comprueba |
|---|---|---|---|
| RNF-001 | Usabilidad | Pedir y votar sin instrucciones; una mano; un solo botón principal por pantalla | Revisión de diseño, pruebas con usuarios |
| RNF-002 | Rendimiento ≤ 3 s | Acciones principales con respuesta visible inmediata (voto optimista) | e2e, doc 07 |
| RNF-003 | Disponibilidad 95 % | La interfaz degrada con avisos, nunca en blanco | Pruebas de error |
| RNF-004 | Compatibilidad móvil | Navegadores modernos de Android e iOS, sin instalar | e2e en Chromium y WebKit |
| RNF-005 | Adaptabilidad | Teléfono, computador y TV (doc 05, «Responsive») | Capturas a 360, 768, 1440 y TV |
| RNF-006 | Seguridad | Panel tras sesión, permisos por rol, CSP | Matriz de permisos (doc 09) |
| RNF-007 | Privacidad | ID seudónimo; nada personal; aviso de datos | Revisión de seguridad |
| RNF-008 | Integridad | Sin duplicados por reintentos: `Idempotency-Key`, doble toque bloqueado | Pruebas del cliente API y e2e |
| RNF-009 | Tiempo real ≤ 3 s | Cola y panel se actualizan solos | e2e de sincronía |
| RNF-010 | Escalabilidad | Multi-establecimiento por `slug` en todas las rutas | — |
| RNF-011 | Interoperabilidad | Reproductor embebido oficial de YouTube; nada se descarga | Revisión |
| RNF-012 | Confiabilidad | Recargar o perder la conexión no altera la cola: resync | e2e de desconexión |
| RNF-013 | Mantenibilidad | Capas y paquetes del doc 03 | Revisión de código |
| RNF-014 | Trazabilidad | Las acciones del panel quedan en la bitácora | e2e del panel |
| RNF-015 | Calidad de datos analíticos | Mostrar origen y fecha de los datos; rotular los de ejemplo | Revisión |
| RNF-016 | Manual de uso | Manual ilustrado dentro del panel, por rol. Desde el 2026-11-25 (punto 20): empieza con «Configurar el bar paso a paso», todos los temas tienen captura con anillo y flecha donde se toca, y cada tema lleva su etiqueta de color («Para empezar», «En la noche», «Solo el Administrador») | Iteración 4 |

## Reglas de negocio

Del documento de modelo de datos (`fuente/extraido/prototipos-arquitectura-y-modelado-de-datos.md`, §2.3).
La regla vive en el servidor; el frontend la **explica** y la anticipa en la interfaz.

| ID | Regla | Dónde se ve |
|---|---|---|
| RN-01 | Máximo 3 solicitudes pendientes por teléfono y sesión (configurable). La que suena cuenta hasta que termina (2026-11-10) | Fichas «Te quedan N de 3 pedidos» |
| RN-02 | Pedir una canción ya pendiente registra un voto, no la duplica | «Ya está en la cola: vota en vez de pedirla» |
| RN-03 | Quien pide da automáticamente el primer voto | Su canción aparece como «Tuya» |
| RN-04 | Un voto activo por teléfono y solicitud | Tecla hundida «Votaste» |
| RN-05 | Orden por votos desc.; en empate, la pedida antes | Cola, TV, panel; nota de desempate |
| RN-06 | No se aceptan bloqueadas, no disponibles, no embebibles ni de más de 10 min; el cliente ve el motivo | Texto en lugar de «Pedir» |
| RN-07 | Una canción ya sonada no se repite en 60 min (configurable) | «Sonó hace poco» con la hora en que se podrá pedir |
| RN-08 | Sin pendientes suena la lista de la casa; al llegar una solicitud se termina la canción en curso. Suena con la noche abierta, también con las solicitudes en pausa; sin noche (fuera de horario) o en mantenimiento no suena nada (D24) | Etiqueta de origen en TV y cliente |
| RN-09 | El QR sigue el horario salvo pausa o activación forzada. La activación forzada termina cuando empieza el horario (2026-11-25) | Barra de estado del panel |
| RN-10 | El mantenimiento cierra la sesión y expira lo pendiente | Confirmación en el panel; «expirada» en «Tus pedidos» |
| RN-11 | Toda acción del personal queda registrada | Registro de acciones y bitácora |

## Historias por iteración

| It. | Historias |
|---|---|
| 1 Cliente | PB-03, 04, 06, 07, 08, 09, 13, 14, 15, 17, 18, 19, 21, 22, 30 (PB-05 y PB-16 los imita la simulación) |
| 2 Pantalla del bar | PB-17, 18, 19, 20, 21, 38, 02 (placa del QR) |
| 3 Panel operativo | PB-10, 11, 12, 25, 27, 29, 31, 32, 33, 34, 38, 40 |
| 4 Gestión | PB-01, 02, 23, 24, 26, 28, 34, 35, 36, 37, 39, 40, 41, 42 |
| 5 Analítica | PB-45, 46, 47, 48, 49 (PB-43 y PB-44 son de datos: solo se documentan) |
| 6 Extras | PB-50, 51, 54 (PB-52 y PB-53 no tienen interfaz) |
