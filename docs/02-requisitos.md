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

### Módulo 4. Cola pública y reproducción

| ID | Requisito | Prioridad | Historia | It. | Quién |
|---|---|---|---|---|---|
| RF-015 | Cola pública ordenada por popularidad | Must | PB-17 | 1, 2 | F+B |
| RF-016 | Indicación de la canción en reproducción | Must | PB-17 | 1, 2 | F |
| RF-017 | Actualización automática de la cola | Must | PB-18 | 1, 2 | F+B |
| RF-018 | Tiempo estimado de espera | Must | PB-19 | 1, 2 | F |
| RF-019 | Reproducción automática de la cola | Must | PB-20 | 2 | F+B |
| RF-020 | Transición automática entre solicitudes y lista por defecto | Must | PB-38 | 2, 3 | F+B |

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
| RNF-016 | Manual de uso | Manual ilustrado dentro del panel, por rol | Iteración 4 |

## Reglas de negocio

Del documento de modelo de datos (`fuente/extraido/prototipos-arquitectura-y-modelado-de-datos.md`, §2.3).
La regla vive en el servidor; el frontend la **explica** y la anticipa en la interfaz.

| ID | Regla | Dónde se ve |
|---|---|---|
| RN-01 | Máximo 3 solicitudes pendientes por teléfono y sesión (configurable) | Fichas «Te quedan N de 3 pedidos» |
| RN-02 | Pedir una canción ya pendiente registra un voto, no la duplica | «Ya está en la cola: vota en vez de pedirla» |
| RN-03 | Quien pide da automáticamente el primer voto | Su canción aparece como «Tuya» |
| RN-04 | Un voto activo por teléfono y solicitud | Tecla hundida «Votaste» |
| RN-05 | Orden por votos desc.; en empate, la pedida antes | Cola, TV, panel; nota de desempate |
| RN-06 | No se aceptan bloqueadas, no disponibles, no embebibles ni de más de 10 min; el cliente ve el motivo | Texto en lugar de «Pedir» |
| RN-07 | Una canción ya sonada no se repite en 60 min (configurable) | «Sonó hace poco» con la hora en que se podrá pedir |
| RN-08 | Sin pendientes suena la lista de la casa; al llegar una solicitud se termina la canción en curso | Etiqueta de origen en TV y cliente |
| RN-09 | El QR sigue el horario salvo pausa o activación forzada | Barra de estado del panel |
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
