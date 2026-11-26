# 05 — UI/UX: sistema de diseño Rockola

Dirección base: **Rockola** (Tema #2). Referencia visual: `prototipos/rockola/` y sus
capturas. Este doc manda sobre los prototipos cuando chocan, y registra cada cambio
(«Reworks»).

**Tres diseños (D13, RF-059, ADR-25).** El Administrador elige con qué diseño ven los
clientes su bar: Rockola, **Vidrio** (Tema #1, `prototipos/vidrio/`) o **Afiche** (Tema #3,
`prototipos/afiche/`). El tema cambia el teléfono, la TV y el panel con sesión (D19, ADR-31);
entrar, cambiar la clave y la plataforma siguen en Rockola porque no pertenecen a un bar. Cada
tema son valores en `tokens.css` bajo `[data-tema=…]` y reglas de forma en
`packages/ui/src/temas/`: el marcado y los textos son los mismos. Lo que este doc dice de
Rockola (identidad, tokens, «Reglas para que no parezca una plantilla») vale para Rockola;
Vidrio y Afiche siguen su prototipo y su fila de `prototipos/decisiones-de-diseno.md`,
fieles a él (decisión del usuario), con las mismas reglas de accesibilidad y de textos.

## Identidad

> El teléfono funciona como el panel de una rockola. La canción que suena aparece dentro
> de un arco de luz; cada canción es una tarjeta de título como las tiras de papel que
> identificaban los discos. Votar es presionar una tecla, que queda hundida cuando ya se
> votó. Los pedidos son fichas, como las monedas de las rockolas de tienda de barrio. La
> luz comunica el estado: encendida, el bar recibe canciones; apagada, está cerrado o en
> pausa. — `fuente/…Prototipos…docx`, Tema #2

| Pieza | Qué representa | Componente |
|---|---|---|
| Arco de luz (marco de cromo, tubos, ventana de papel) | Lo que suena; encendido o apagado | `Arco`, `Ventana` |
| Tarjeta de título (papel crema, franja cereza) | Una canción | `TarjetaTitulo` |
| Tecla (relieve; hundida; ámbar) | Votar, pedir, acciones | `Tecla` |
| Fichas (monedas) | Pedidos que le quedan al teléfono | `Fichas` |
| Luz de estado | En vivo / pedidos cerrados | `LuzEstado` |

### Principios (de `prototipos/decisiones-de-diseno.md`)

1. **Jerarquía:** lo que suena es lo más grande de cada pantalla; después, la posición
   de la canción propia («Tu canción va de #3»).
2. **Acento escaso:** la luz ámbar solo en la acción principal, «Tu canción» y la franja
   «Abierto».
3. **Estados por texto y forma, no solo color:** «+1», «Votaste» y «Tuya» (WCAG 1.4.1).
4. **Reglas visibles:** «Te quedan 2 de 3 pedidos»; «Dura más de 10 min» en lugar de un
   botón gris; la nota de desempate con el caso real.
5. **Sin controles de música para el cliente:** solo el bar salta o bloquea.
6. **Datos coherentes:** cliente, TV y panel muestran el mismo momento de la noche.

## Tokens

Viven solo en `packages/ui/src/tokens.css`. `theme.css` los expone a Tailwind y borra sus
paletas por defecto: una clase como `bg-blue-500` no existe.

### Color

| Token | Valor | Uso |
|---|---|---|
| `--nogal` | `#2A1711` | Fondo de la página |
| `--nogal-hondo` | `#1D0F0B` | Madera más profunda (fondos de video, sombras) |
| `--madera` | `#3A2218` | Superficies, pie |
| `--madera-clara` | `#4A2C1F` | Parte alta del pie, cajas del panel |
| `--borde` | `rgba(244,231,203,.14)` | Filetes sobre madera |
| `--hueco` | `rgba(0,0,0,.22)` | Fondo hundido en la madera: pestañas, días del horario, etiquetas |
| `--dato` / `--dato-contexto` | ámbar / `#85715F` | Gráficos: el dato que importa y el resto (forma «énfasis», abajo) |
| `--rejilla` / `--eje` | crema al 10 % / 28 % | Rejilla de un pelo y línea base de los gráficos |
| `--crema` (= `--papel`) | `#F4E7CB` | Texto sobre madera; papel de tarjetas y ventanas |
| `--crema-2` | `rgba(244,231,203,.72)` | Texto secundario sobre madera |
| `--papel-apagado` | `#CDBFA6` | Tarjeta apagada (no pedible, bloqueada) |
| `--cereza` | `#B8282F` | Franja del artista, tecla principal, insignias |
| `--cereza-hondo` | `#7E171D` | Borde inferior de la tecla cereza |
| `--ambar` | `#F5A524` | Luz: arco, ficha libre, «Tuya», foco |
| `--ambar-claro` | `#FFD27A` | Brillo de «Tuya», cifras de métricas |
| `--tinta` | `#23140F` | Texto sobre papel |
| `--tinta-2` | `#5B4434` | Texto secundario sobre papel |
| `--gris-apagado` | `#6B5A50` | Luz apagada, franja de tarjeta apagada |
| `--tecla-cara` / `--tecla-canto` | `#FFF8E6→#E8D8B6` / `#7A6450` | Cara y canto de la tecla |

Degradados (no son colores, son piezas): `--cromo` (marcos), `--luz` (tubos encendidos),
`--apagado` (tubos apagados). Los valores exactos están en `tokens.css`.

### Tipografía

Una sola familia, **Archivo** variable, con su eje de ancho (`wdth` 62–125).

| Rol | Clase | Ancho / peso | Tamaños |
|---|---|---|---|
| Placa (nombre del bar, títulos de página, TV) | `.placa` | 125 % / 800, −0,01em | 22–30 |
| Título de sección | `.titulo-seccion` | 112 % / 600 | 16 (cajas del panel 17/700) |
| Título de canción | `.condensada` | 75 % / 700 | 17–46 según la pieza |
| Cifras (votos, puestos) | — | 75 % / 800, cifras tabulares | 18–60 |
| Tecla | `.tecla` | 75 % / 800, MAYÚSCULAS, +0,02em | 16–22 |
| Cuerpo | — | 100 % / 400, 15/1,4 | 11–17 |

Los títulos de canción **no** se pasan a mayúsculas: DtMF y NUEVAYoL se escriben así.

### Forma

| Pieza | Radio |
|---|---|
| Tarjeta de título | 6 |
| Día del horario, navegación del panel | 8 |
| Ventana, tecla, buscador, pestaña | 10 |
| Caja del panel | 14 |
| Marco de estado y de «Tu canción» | 16–18 |
| Insignia | 3–4 |
| Arco | Elíptico: `50% 50% 22px 22px / 34% 34% 22px 22px` |

Sin píldoras. Sombras: la tecla tiene canto (`0 4px 0`) y sombra proyectada; la hundida,
sombra interior; la tarjeta, una línea de 2px. Foco: `3px solid --ambar`, separado 3px.

### Movimiento

Los prototipos no tienen movimiento. Se añade lo mínimo, con intención:

| Qué | Cómo | Por qué |
|---|---|---|
| Pulsar una tecla | 80 ms: baja de −2px a +1px | Es una tecla física |
| Votar | La tecla queda hundida y cambia el texto a «Votaste» | Confirma en < 1 s (RF-012) |
| Subir de puesto | La tarjeta se desplaza a su nuevo sitio en 300 ms | Que se vea por qué cambió el orden |
| Avance de la canción | Continuo, sin animación de entrada | — |
| Aviso que se va solo (errores de YouTube en el panel) | Se desvanece en 600 ms (`--desvanecer`) cuando el servidor deja de mandarlo, a los 5 min del último error | Que no desaparezca de golpe ni se quede la noche entera (lista del equipo, 2026-11-25) |

Con `prefers-reduced-motion: reduce` todo cambia sin transición.

## Componentes (`packages/ui`)

| Componente | Variantes / estados | Accesibilidad |
|---|---|---|
| `Tecla` | `normal`, `hundida` (Votaste; se pulsa para mover el voto, y «Filtrar» abierto), `cereza` (principal), `tuya` (no interactiva), tamaños `grande`, `chica`; estados `ocupada` (envío en curso) y desactivada (hundida y sin brillo, p. ej. «Subir» en la primera canción) | `<button>` o `<a>`; la hundida y la tuya son texto con `aria-label` claro; ≥ 44 px |
| `TarjetaTitulo` | `normal`, `tuya`, `apagada`, `resultado` (con duración, sin puesto); partes: puesto, miniatura, título, artista, votos o duración, subida (↑N) | El título y el artista se leen en orden; la subida dice «subió 2 puestos» |
| `Arco` + `Ventana` | encendido, apagado; forma `movil` y `tv` | `<section aria-label="Suena ahora">` |
| `Fichas` | N libres de M | Siempre acompañadas del texto «Te quedan N de M pedidos» |
| `LuzEstado` | encendida, apagada | El texto dice el estado |
| `Avance` | Barra de progreso con tiempos | `role="progressbar"` con valores en segundos |
| `MiniReproductor` | Con o sin canción | — |
| `Pestanas` | Inicio, Buscar, Cola (+ Tus pedidos con aviso) | `aria-current="page"` en la activa |
| `DiasSemana` | Día abierto (tecla), cerrado (hundido), «Hoy» | Lista con el estado en texto |
| `Caja` | Caja del panel con título y nota | `<section>` con encabezado |
| `Metrica` | Cifra grande con etiqueta | — |
| `Aviso` | Ámbar (confirmación), cereza (error), madera (información) | `role="status"` o `alert` según urgencia |
| `Dialogo` | Confirmación, formulario | `<dialog>` nativo, foco atrapado, Escape cierra |
| `Campo`, `Selector`, `GrupoTeclas` | Texto, selección, opciones como teclas | Etiqueta visible, error con `aria-describedby` |
| `CampoClave` | Clave oculta o visible con «Mostrar» / «Ocultar» dentro de la entrada (2026-11-25) | El nombre del botón dice lo que hará («Mostrar clave»), sin `aria-pressed`; 44 px. En todos los campos de clave: entrar al panel, plataforma y cambiar la clave |
| `EstadoVacio`, `Esqueleto`, `Reintentar` | Vacío, cargando, error | El esqueleto no se anuncia; el error sí |
| `PlacaQr` | Placa cromada con el QR | El QR lleva texto alternativo con el enlace |
| `Icono` | Dibujados a mano, 24 × 24, trazo 1,9, puntas redondas | Decorativos con `aria-hidden`; con función, con nombre |
| Pestañas del panel (`.panel-subnav`) | Una por sección que el rol puede abrir; la actual en relieve, como la tecla de papel | `<nav aria-label>` con `aria-current="page"`; en el teléfono se desplazan de lado y la actual queda a la vista |
| `Selector` | Lista desplegable con etiqueta visible unida por `id` | Su nombre accesible es solo la etiqueta (envuelta en el `<label>`, algunos lectores le sumaban las opciones) |
| `Etiqueta` (`.etiqueta`) | Rótulo pequeño sobre madera («Administrador», «Karaoke», «Clave temporal») | Texto, nunca solo color |

La galería de todos está en `/muestras` (solo en modo simulado).

## Pantallas

| Superficie | Ruta | Prototipo | Estados obligatorios |
|---|---|---|---|
| Cliente: Bienvenida | `/b/[bar]` | `cliente-1-bienvenida` | abierto, en pausa, mantenimiento, fuera de horario (`cliente-4`), cargando, bar no encontrado, primer ingreso (aviso de datos) |
| Cliente: Buscar | `/b/[bar]/buscar` | `cliente-2-buscar` (+ `#pedido`) | sin consulta, escribiendo, resultados, sin resultados, YouTube caído, desde caché, después de pedir, 0 de 3, motivos de rechazo, servicio no disponible |
| Cliente: Cola | `/b/[bar]/cola` | `cliente-3-cola` | con canción propia, sin canción propia, vacía (suena la lista de la casa), cargando, sin conexión |
| Cliente: Tus pedidos | Sección al final de `/b/[bar]/cola` | — (nuevo) | sin pedidos, cada estado de solicitud |
| Cliente: Política de datos | `/b/[bar]/politica-de-datos` | — (nuevo) | texto `TBD` rotulado |
| Pantalla del bar | `/b/[bar]/pantalla` | `pantalla-bar` | sin vincular (código), pidiendo el código, sin red al pedirlo, desvinculada (aviso), encender, sonando (pedidos / lista de la casa), cerrado, en pausa, mantenimiento, cola vacía, sin conexión, video no disponible |
| Panel: Entrar | `/panel/entrar`, `/panel/cambiar-clave` | — (nuevo) | error genérico, cuenta bloqueada, sesión caducada, cambio forzado |
| Panel: Operación | `/panel` | `panel-admin` (+ `#pausa`) | abierto, en pausa, fuera de horario, mantenimiento, sin sesión, sin lista activa, vista Operador |
| Panel: Gestión | `/panel/{usuarios,establecimiento,horario,listas,canciones,bloqueos,bitacora,eventos,manual}` | — (nuevas) | vacío, cargando, error, sin permiso. En «Bar y QR», «Pantallas del bar»: sin pantallas, con pantallas, código que no existe o venció, desvincular (RF-058) |
| Panel: Analítica | `/panel/analitica/*` (con `/cubo`, RF-063) | — (nuevas) | sin datos, sin carga del pipeline, datos de ejemplo rotulados; en el cubo, KPI que no aplican a un dice, en vivo y reconectando (RF-067), celdas sin datos |
| Panel: Canciones | `/panel/canciones` (RF-068) | — (nueva) | sin género, todas, búsqueda sin resultados, bar sin canciones, guardado, error al guardar, más de 100 (pide buscar), vista Operador (no la ve) |

## Gráficos (tablero analítico)

Método de la skill `dataviz`, con los tokens Rockola:

- **Forma:** cifras sueltas como las «Métricas de la noche» (ámbar sobre madera); rankings en
  barras horizontales; horas y noches en columnas; partes de un todo (lo pedido frente a la
  lista de la casa) en una barra 100 %; un porcentaje contra su umbral en un medidor. Nunca
  dos ejes: solicitudes, votos y teléfonos se ven de a uno (teclas para elegir) o en
  gráficos separados.
- **Color:** forma «énfasis». El ámbar es la luz: el dato que importa (la hora pico, la
  noche floja, lo pedido); el resto va en `--dato-contexto`. Una serie sola va toda en
  ámbar. Validado con `validate_palette.js` sobre `#3A2218`: ΔE con daltonismo 22,1, ΔE normal
  25,1, contraste ≥ 3:1. La banda de luminosidad y el croma mínimo «fallan» a propósito: son
  pruebas de paletas categóricas, y aquí el gris de contexto tiene que leerse gris.
- **Marcas:** barras de 18 a 24 px con la punta redondeada (4 px) y la base recta; 2 px de
  madera entre partes; rejilla de un pelo; el texto siempre en crema, nunca del color del
  dato; rótulos solo en lo resaltado y en las puntas de las barras.
- **Interacción y accesibilidad:** cada columna se enfoca con el teclado y muestra su valor,
  como al pasar el puntero; cada gráfico tiene su «Ver como tabla»; las tablas anchas pasan
  las columnas secundarias a una segunda línea cuando la caja es angosta.
- **Datos de ejemplo:** la simulación lo dice en cada parte del tablero, junto al rango y la
  última carga (RNF-015).

## Responsive

| Superficie | Regla |
|---|---|
| Cliente | Se diseña a 360 px. Una columna de `100dvh`; el pie (mini reproductor + pestañas) queda fijo abajo con `safe-area-inset-bottom` y el contenido se desplaza por encima. Desde 768 px, la columna se centra (máx. 30rem) sobre la madera, como el mueble de la rockola. |
| Pantalla del bar | Lienzo 16:9 que escala completo con `--u = min(100vw/1280, 100dvh/720)`: toda medida es `calc(n * var(--u))` con los valores del prototipo. En otras proporciones queda centrado con madera alrededor. Se revisa a 1280 × 720, 1920 × 1080 y 3840 × 2160. |
| Panel | ≥ 1280 px: dos columnas como el prototipo. 768–1279: una columna. < 768: las pestañas de secciones se desplazan de lado, teclas a todo el ancho y las tablas pasan a tarjetas (`@container`). En el editor de listas, por debajo de 520 px las flechas bajan a su propia línea. El Operador lo usa desde el teléfono. |

## Accesibilidad

- Contraste AA medido contra el fondo pintado (4,5:1; 3:1 en texto grande). Sobre
  degradados se mide el punto más desfavorable.
- Objetivos táctiles de 44 px o más; foco visible; todo se usa con teclado.
- `lang="es-CO"`; un `h1` en cada pantalla, también en las de aviso.
- Nada se comunica solo con color. Los cambios en vivo se anuncian con `aria-live`
  cortés (sin anunciar cada voto ajeno).
- Movimiento reducido respetado.

## Reglas para que no parezca una plantilla

Valen para Rockola. Vidrio usa vidrio esmerilado, un degradado pastel y
píldoras por diseño (D13): ahí estas reglas 1 y 2 las reemplaza su prototipo.

1. Solo tokens Rockola.
2. Nada de titular centrado con tres tarjetas, degradados morados, vidrio esmerilado,
   píldoras ni emojis.
3. Un solo recurso de énfasis por vista: la luz.
4. Contenido real desde el primer momento (canciones reales; datos de ejemplo rotulados
   donde puedan confundirse).
5. Un solo juego de iconos (los de `packages/ui`), solo con función.
6. El panel es una herramienta: denso, alineado y rápido de leer.

## Textos

- Español de Colombia, de tú, frases cortas. La regla se dice con el caso real.
- Horas «8:00 p. m.» (12 h, como en Colombia); duraciones `m:ss` o `h:mm:ss`; esperas
  «unos 13 min» (redondeo hacia abajo), «en menos de 1 min».
- Comillas latinas «» para títulos de canción en frases.
- Los mensajes de error dicen qué pasó y qué hacer, sin códigos ni trazas.
- Todos los textos de la interfaz viven en `frontend/web/lib/textos/` para revisarlos juntos.

## Proceso de diseño

1. Si hay prototipo, se reimplementa con `packages/ui` y se compara con su captura.
2. Si no hay, se diseña dentro del sistema con `frontend-design:frontend-design` y
   `design:ux-copy`; `playground:playground` solo si hay variantes abiertas.
3. Antes de darla por buena: `design:design-critique` y `design:accessibility-review`
   sobre capturas reales en todos sus estados.
4. Los cambios al prototipo se anotan abajo.

## Reworks respecto al prototipo

_Los cambios hasta el 2026-11-12 están en [historial/05-reworks-anteriores.md](historial/05-reworks-anteriores.md)._

| Fecha | Pantalla | Cambio | Por qué |
|---|---|---|---|
| 2026-11-20 | Tablero, «Cubo» | Cubo 3D de cubitos de madera que se encienden en ámbar según el KPI; los totales van en papel. Los KPI del Entregable 6 van arriba y son las teclas que eligen qué ilumina el cubo; debajo, recorte por ejes, operaciones OLAP y tabla de cruce. La escala de ámbar salta del 41 % al 63 % | RF-063. La crítica movió los KPI sobre el cubo porque son lo que se viene a leer. En ese tramo de ámbar ni la crema ni la tinta llegan a 4,5:1 (medido). El cubo se usa con arrastre o flechas, y la tabla da los mismos datos a quien no lo ve |
| 2026-11-21 | TV | Aviso en la franja superior cuando el navegador solo deja sonar en silencio: «El video va en silencio. Toca la pantalla o presiona una tecla para activar el sonido.» Desaparece al primer toque | ADR-28. Usa la franja que ya avisa de la conexión; dice qué pasa y cómo arreglarlo, también para un control remoto |
| 2026-11-21 | Panel, Operación | «Abrir la noche» aparece siempre que no haya una abierta, y «Reanudar solicitudes» solo si hay algo que reanudar | RF-037. Sin noche, «Reanudar» no resolvía nada y dejaba al personal sin salida |
| 2026-11-21 | Plataforma | Pantalla nueva en el sistema Rockola con el armazón de gestión: entrar, cambiar la clave inicial, bares con sus cuentas, crear bar y clave temporal en un diálogo que se ve una vez | RF-064 |
| 2026-11-21 | App Android | Vidrio y Afiche en el teléfono, con los tokens de `tokens.css` y las formas de `temas/`. Diferencias con la web: Vidrio sin desenfoque de fondo (superficie translúcida, contraste medido sin él), Afiche sin grano de papel y sin color bajo la barra de estado. Con letra grande, los títulos de cartel se reducen para no partir palabras y los días del horario crecen hasta 1,25× | RF-059, ADR-25 |
| 2026-11-21 | Bar y QR | Interruptor del destino del QR con el grupo de teclas. Auditoría de posiciones: entradas de una fila a la misma altura, acciones del QR parejas, teclas de Personal y Listas en columnas, secciones del panel sin recorte desde 768 px, días del horario de igual alto y cubo que cabe en su escena | RF-035, RF-065 |
| 2026-11-21 | Panel y tablero | Vidrio y Afiche también en el panel, según `panel-admin.html` de cada prototipo (D19, ADR-31). Gráficas: Vidrio `#D96F62` y `#7C74C8`, Afiche `#B0105D` y `#8C7B28`, validadas con la skill dataviz. Cubo: durazno en Vidrio y resaltador amarillo bajo tinta en Afiche, porque el amarillo no llega a 3:1 como marca sobre papel | El usuario pidió coherencia entre vistas: un bar en Afiche no debe saltar a madera al entrar al panel |
| 2026-11-21 | App Android, escáner | Pantalla nueva con `MarcoCliente` y el tema del bar: visor cuadrado con esquinas de mira en crema con halo oscuro (no gastan el acento), aviso de QR ajeno y estados de permiso, sin cámara y fallo. En la bienvenida, «Escanear otro QR» va como enlace para no competir con la acción principal | RF-066 |
| 2026-11-24 | App Android, bienvenida | «Escanear otro QR» pasa a «Cambiar de bar»: olvida el bar recordado y abre la cámara. Sigue como enlace bajo la tecla principal. Sin bar, la app abre el escáner sola al arrancar | RF-066, RF-069, D22. La app recuerda el bar aunque se cierre y no había forma de salirse; «Cambiar de bar» dice el resultado, «Salir» se leía como cerrar sesión |
| 2026-11-25 | Horario y cierre | Los horarios van centrados: en la caja «Horario» del panel, en el editor, en el aviso de la TV y en el teléfono con Vidrio y Afiche (sus prototipos los alineaban a la izquierda). Franja «Los pedidos se cierran en N min» arriba del teléfono en los últimos 15 min, en crema con tinta (Afiche: papel con borde de tinta; Vidrio: velo), distinta de la de conexión caída (cereza) y sin gastar el ámbar; en el panel, aviso con el ícono de alerta | Puntos 6 y 13 del equipo, RF-070 |
| 2026-11-25 | Panel, navegación | Las 11 secciones van en grupos: Noche (Operación, Horario, Eventos, Bloqueos), Bar (Bar y QR, Personal, Bitácora), Música (Listas de la casa, Canciones), Análisis y Manual. Una fila de teclas con los grupos y, debajo, una fila más sobria (contorno, sin relieve) con las secciones del grupo abierto. Todo son enlaces, sin menús que abrir. Un grupo con una sola sección enlaza directo y lleva su nombre | Puntos 3 y 14 del equipo. Música va junto a Análisis porque «Canciones» corrige el género que lee el tablero |
| 2026-11-25 | Bar y QR, Canciones, Personal | Bar y QR: cada caja con su alto (sin hueco al estirarse), «Guardar datos», «Guardar diseño» y «Generar QR» a la derecha, y el QR centrado con su dirección debajo. Canciones: géneros en orden alfabético, «Clasificar las que faltan» con su explicación, y «Automático, de antes» para el género viejo sin origen. Personal: «Sin ingresos registrados todavía» | Puntos 14, 16, 17 y 18 del equipo |
| 2026-11-25 | Tablero, Diagnóstico y Plataforma | Dos gráficas más en Diagnóstico con datos que ya llegaban: «Por qué no sonaron» (barras por causa, bajo la tabla de las muy votadas) y «Solicitudes por noche» (columnas, aparte de «Teléfonos por noche»: una medida por gráfica, sin doble eje). Usan `Barras` y `Columnas` con sus colores ya validados en los tres temas (skill dataviz). Plataforma: selector «Ir a un bar» sobre la lista | Puntos 15 y 19 del equipo, RF-052, RF-064 |
| 2026-11-25 | Manual | Guía «Configurar el bar paso a paso» al principio, para el Administrador. Temas nuevos o con captura nueva: Canciones, Saltar o pausar la música, Cerrar la noche o mantenimiento y los grupos del menú. Cada captura marca dónde se toca con un anillo ámbar claro y una flecha, con halo de nogal para leerse sobre cualquier fondo; las pone el script de capturas sobre la pantalla de verdad, así siguen al botón si la interfaz cambia. Etiquetas: «Para empezar» (ámbar), «En la noche» (crema) y «Solo el Administrador» (cereza), en el índice y en cada tema | Punto 20 del equipo, RNF-016. El ámbar del manual no compite con la interfaz: vive en las capturas y en la etiqueta del primer tema |
| 2026-11-21 | App Android, logo | Ícono adaptativo nuevo en lugar del robot de Android: el arco de luz con marco de cromo, sus tubos y el disco que suena, sobre nogal, con los valores de `tokens.css`. Versión de una tinta para los íconos temáticos. Se descartaron la tarjeta de título dentro del arco (se lee como radio) y la ficha con arco (se lee como moneda con reja). Contraste mínimo de la silueta 3,46:1 y todo dentro de la zona segura | El usuario pidió el logo de la app; es la marca de Rockola, por eso no cambia con el tema del bar |
| 2026-11-21 | Plataforma | Cada bar llega plegado con nombre, dirección web, dirección y horario. La tecla «Ver n cuentas» queda hundida al abrir; «Agregar cuenta» pasa adentro. Un bar recién creado queda abierto | El usuario pidió ver primero los bares y desplegar las cuentas |
| 2026-11-21 | Manual | Un tema por página, con índice lateral desde 1024 px y lista desplegable en el teléfono, teclas Anterior y Siguiente y la posición. El tema vive en `?tema=` de la URL | El usuario pidió paginarlo; 16 temas seguidos obligaban a un desplazamiento muy largo |
| 2026-11-21 | Formularios | Dos fechas con hora van una debajo de la otra si no caben a lo ancho | El control nativo no se encoge y «Termina» se montaba sobre «Empieza» en el diálogo de eventos |
| 2026-11-24 | Tablero, «Cubo» | Teclas «En vivo» (por defecto) e «Histórico». En vivo, la luz de estado encendida dice «Datos en vivo» con la hora y se apaga con «En pausa: reconectando». El cubo dibuja los 5 géneros más activos (casilla para verlos todos); con un filtro solo se dibuja lo elegido, sin su «Todos», y el lado del cubito se achica con las celdas por arista para caber en la escena. Una combinación sin datos es un cubito hueco de líneas punteadas y «Sin datos» en la tabla; un 0 sigue siendo madera. «Limpiar» pasa a «Restablecer filtros»; se quita «Separar la capa elegida»; se suma el KPI «Tiempo de reproducción» | RF-067, D21. Con 15 géneros el cubo se salía de la escena. Ocultar lo no elegido hace sobrar «Separar». La diferencia entre vacío y cero va con forma y texto, no solo con color (skill dataviz) |
| 2026-11-24 | Canciones | Pantalla nueva con el armazón de gestión: filtro «Sin género / Todas», búsqueda, y por fila la tarjeta de título, el género con quién lo puso y un selector «Cambiar a» con «Guardar género» en cereza. El aviso ámbar confirma y explica cuándo lo ve el cubo | RF-068. Lista y no tabla, como Bloqueos: a 360 px el selector baja debajo de la canción |
| 2026-11-25 | Bar y QR | Dos columnas que apilan cada una sus cajas: a la izquierda «Datos del bar» y «Diseño de la rockola», a la derecha lo que va en el local (QR de las mesas, pantallas y vincular). Las vistas previas de diseño llegan a 900 px | La lista del equipo seguía viendo espacios muertos a 1440 px: con filas compartidas la caja más corta dejaba un hueco y el selector de diseño, limitado a 540 px, dejaba unos 800 px vacíos |
| 2026-11-25 | Tablero, Participación y Diagnóstico | Cuatro gráficas con `Barras` y `Columnas`: «Lo más pedido» (canciones o artistas, una a la vez), «Espera por hora» (horas seguidas con 10 pedidos o más, la más lenta resaltada), «Noche a noche» de la concentración con su umbral y «Pedidos que no sonaron, por noche». `Barras` corta con «…» la etiqueta que no cabe | La lista del equipo pidió más gráficas sin graficar de más; cada una responde una pregunta (doc 02). Las horas sueltas de la tarde partían el eje del tiempo; una etiqueta larga se montaba sobre la barra (skill dataviz) |
