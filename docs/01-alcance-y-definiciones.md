# 01 — Alcance y definiciones

## El problema

En los bares el personal elige toda la música y los pedidos del público llegan de palabra
al mesero o al bartender, donde se pierden o se atienden sin criterio. No hay forma de
saber qué canción quiere más gente ni en qué va cada pedido. Rockola digitaliza esa
selección: el público pide y vota desde su teléfono, la cola se ordena sola por
popularidad y el personal controla el servicio (fuente: `fuente/extraido/requisitos.md`,
«Contexto»).

## Qué es este repositorio

El **frontend completo** de Rockola: las tres superficies, su sistema de diseño, el
contrato que se propone al backend y una simulación que permite usarlo todo sin backend.

| Dentro | Fuera (lo hace el equipo de backend o no se hace) |
|---|---|
| Interfaz del cliente, pantalla del bar y panel | Base de datos, reglas del servidor, workflows |
| Reproductor de YouTube embebido en la pantalla del bar | Llamadas a la YouTube Data API y su llave |
| Contrato propuesto (`packages/contrato`) y su documentación | Implementación de los endpoints y del tiempo real |
| Simulación en el navegador con datos de ejemplo | Ingesta, limpieza y seudonimización con Spark (RF-047, RF-048) |
| Tablero analítico (pantallas y gráficas) | El cálculo real de los indicadores |
| Manual de uso del panel (RNF-016) | Salida al sistema de sonido (RF-056) y respaldo de listas (RF-057), sin interfaz |

## Superficies

| Superficie | Quién | Dispositivo | Rutas |
|---|---|---|---|
| Cliente | Cualquier persona en el bar, anónima | Teléfono, vertical | `/q/[token]` → `/b/[bar]`, `/b/[bar]/buscar`, `/b/[bar]/cola` (con «Tus pedidos»), `/b/[bar]/politica-de-datos` |
| Pantalla del bar | Público (pasiva) | TV o monitor horizontal con navegador | `/b/[bar]/pantalla` |
| Panel | Personal con cuenta | Computador; el Operador también desde el teléfono | `/panel/**` |

## Roles

| Rol | Cómo entra | Qué hace |
|---|---|---|
| Cliente | QR, sin cuenta; identificado por un ID seudónimo del teléfono (RF-002) | Buscar, pedir (máx. 3 pendientes), votar, ver la cola y sus pedidos |
| Público | Mira la pantalla del bar | Nada: solo ve |
| Operador (mesero, bartender o DJ) | Panel con correo y contraseña | Controla la reproducción y la cola (matriz en el doc 04, D9) |
| Administrador | Panel con correo y contraseña | Todo el panel: servicio, horarios, listas, usuarios, establecimiento, QR, bitácora, eventos, analítica |
| Sistema | — | Abre y cierra sesiones por horario, avanza la cola, pasa a la lista de la casa |

El modelo de datos llama a los roles `admin` y `dj`; en el contrato y la interfaz se usan
`administrador` y `operador`, como en los requisitos (doc 04).

## Iteraciones

Equivalen a las fases del proyecto. Cada una cierra como dice el doc 08.

| It. | Nombre | Qué entrega |
|---|---|---|
| 0 | Cimientos | Repositorio, documentación, sistema de diseño, contrato, simulación, herramientas de calidad |
| 1 | Cliente | Bienvenida, buscar/pedir/votar, cola, tus pedidos, estados del servicio, política de datos |
| 2 | Pantalla del bar | Reproductor de YouTube, lo que suena, lo que viene, QR, estados |
| 3 | Panel operativo | Entrar, estado del servicio, control de la reproducción, cola, bloqueos, registro, métricas |
| 4 | Gestión | Usuarios, establecimiento y QR, horario, listas de la casa, bloqueos, bitácora, eventos, manual |
| 5 | Analítica | Tablero descriptivo, diagnóstico, participación, comparativo, recomendaciones |
| 6 | Extras y cierre | Cambiar voto, filtros, aviso de caché; pase final; entrega del contrato |

## Limitaciones conocidas

- **Identidad del teléfono:** vive en el almacenamiento del navegador. En modo incógnito
  o si se borran los datos del sitio, el teléfono cuenta como nuevo (y recupera sus 3
  pedidos). Lo mismo pasaría con la cookie temporal de Bubble que proponía la fuente.
- **La simulación sincroniza pestañas, no dispositivos:** cliente, TV y panel abiertos en
  el mismo navegador se ven en vivo; un teléfono real y una TV real no, hasta que exista
  el backend.

## Glosario

| Término | Significado |
|---|---|
| Solicitud / pedido | Una canción en la cola de una sesión, pedida por un teléfono |
| Voto | Apoyo de un teléfono a una solicitud; uno por teléfono y solicitud |
| Sesión (de operación) | Una noche de servicio, desde que se abre hasta que se cierra o entra en mantenimiento |
| Lista de la casa / lista por defecto | Lista que suena cuando no hay solicitudes pendientes |
| Estado del servicio | `abierto`, `fuera_de_horario`, `en_pausa`, `mantenimiento` |
| Arco de luz | Pieza de la interfaz que enmarca lo que suena; encendida = se reciben canciones |
| Tarjeta de título | Tira de papel crema con el título y una franja cereza con el artista |
| Tecla | Botón con relieve; «Votaste» es la tecla hundida, «Tuya» la ámbar |
| Fichas | Monedas que cuentan los pedidos que le quedan al teléfono |
| Simulación | Implementación del contrato que corre en el navegador mientras no hay backend |
