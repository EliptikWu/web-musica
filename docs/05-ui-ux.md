# 05 — UI/UX: sistema de diseño Rockola

Dirección elegida: **Rockola** (Tema #2, D2). Referencia visual: `prototipos/rockola/` y
sus capturas. Este doc manda sobre los prototipos cuando chocan, y registra cada cambio
(«Reworks»).

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

Con `prefers-reduced-motion: reduce` todo cambia sin transición.

## Componentes (`packages/ui`)

| Componente | Variantes / estados | Accesibilidad |
|---|---|---|
| `Tecla` | `normal`, `hundida` (Votaste, no interactiva), `cereza` (principal), `tuya` (no interactiva), tamaños `grande`, `chica`; estado `ocupada` (envío en curso) | `<button>` o `<a>`; la hundida y la tuya son texto con `aria-label` claro; ≥ 44 px |
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
| `EstadoVacio`, `Esqueleto`, `Reintentar` | Vacío, cargando, error | El esqueleto no se anuncia; el error sí |
| `PlacaQr` | Placa cromada con el QR | El QR lleva texto alternativo con el enlace |
| `Icono` | Dibujados a mano, 24 × 24, trazo 1,9, puntas redondas | Decorativos con `aria-hidden`; con función, con nombre |

La galería de todos está en `/muestras` (solo en modo simulado).

## Pantallas

| Superficie | Ruta | Prototipo | Estados obligatorios |
|---|---|---|---|
| Cliente: Bienvenida | `/b/[bar]` | `cliente-1-bienvenida` | abierto, en pausa, mantenimiento, fuera de horario (`cliente-4`), cargando, bar no encontrado, primer ingreso (aviso de datos) |
| Cliente: Buscar | `/b/[bar]/buscar` | `cliente-2-buscar` (+ `#pedido`) | sin consulta, escribiendo, resultados, sin resultados, YouTube caído, desde caché, después de pedir, 0 de 3, motivos de rechazo, servicio no disponible |
| Cliente: Cola | `/b/[bar]/cola` | `cliente-3-cola` | con canción propia, sin canción propia, vacía (suena la lista de la casa), cargando, sin conexión |
| Cliente: Tus pedidos | `/b/[bar]/tus-pedidos` | — (nuevo) | sin pedidos, cada estado de solicitud |
| Cliente: Política de datos | `/b/[bar]/politica-de-datos` | — (nuevo) | texto `TBD` rotulado |
| Pantalla del bar | `/b/[bar]/pantalla` | `pantalla-bar` | encender, sonando (pedidos / lista de la casa), cerrado, en pausa, mantenimiento, cola vacía, sin conexión, video no disponible |
| Panel: Entrar | `/panel/entrar`, `/panel/cambiar-clave` | — (nuevo) | error genérico, cuenta bloqueada, sesión caducada, cambio forzado |
| Panel: Operación | `/panel` | `panel-admin` (+ `#pausa`) | abierto, en pausa, fuera de horario, mantenimiento, sin sesión, sin lista activa, vista Operador |
| Panel: Gestión | `/panel/{usuarios,establecimiento,horario,listas,bloqueos,bitacora,eventos,manual}` | — (nuevas) | vacío, cargando, error, sin permiso |
| Panel: Analítica | `/panel/analitica/*` | — (nuevas) | sin datos, datos de ejemplo rotulados |

## Responsive

| Superficie | Regla |
|---|---|
| Cliente | Se diseña a 360 px. Una columna de `100dvh`; el pie (mini reproductor + pestañas) queda fijo abajo con `safe-area-inset-bottom` y el contenido se desplaza por encima. Desde 768 px, la columna se centra (máx. 30rem) sobre la madera, como el mueble de la rockola. |
| Pantalla del bar | Lienzo 16:9 que escala completo con `--u = min(100vw/1280, 100dvh/720)`: toda medida es `calc(n * var(--u))` con los valores del prototipo. En otras proporciones queda centrado con madera alrededor. Se revisa a 1280 × 720, 1920 × 1080 y 3840 × 2160. |
| Panel | ≥ 1280 px: dos columnas como el prototipo. 768–1279: una columna. < 768: menú plegable, teclas a todo el ancho y las tablas pasan a tarjetas (`@container`). El Operador lo usa desde el teléfono. |

## Accesibilidad

- Contraste AA medido contra el fondo pintado (4,5:1; 3:1 en texto grande). Sobre
  degradados se mide el punto más desfavorable.
- Objetivos táctiles de 44 px o más; foco visible; todo se usa con teclado.
- `lang="es-CO"`; un `h1` en cada pantalla, también en las de aviso.
- Nada se comunica solo con color. Los cambios en vivo se anuncian con `aria-live`
  cortés (sin anunciar cada voto ajeno).
- Movimiento reducido respetado.

## Reglas para que no parezca una plantilla

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
- Todos los textos de la interfaz viven en `apps/web/lib/textos/` para revisarlos juntos.

## Proceso de diseño

1. Si hay prototipo, se reimplementa con `packages/ui` y se compara con su captura.
2. Si no hay, se diseña dentro del sistema con `frontend-design:frontend-design` y
   `design:ux-copy`; `playground:playground` solo si hay variantes abiertas.
3. Antes de darla por buena: `design:design-critique` y `design:accessibility-review`
   sobre capturas reales en todos sus estados.
4. Los cambios al prototipo se anotan abajo.

## Reworks respecto al prototipo

| Fecha | Pantalla | Cambio | Por qué |
|---|---|---|---|
| 2026-10-26 | Todas | Lienzos fijos → diseño adaptable (ver «Responsive») | RNF-005 |
| 2026-10-26 | Tarjeta de título | Se añade miniatura opcional del video | RF-006 la exige en los resultados |
| 2026-10-26 | Cola y TV | Se añade la espera estimada de cada canción | RF-018 la pide por canción |
| 2026-10-26 | TV | Se añade la etiqueta de origen (pedidos del público / lista de la casa) | RF-016 |
| 2026-10-26 | Panel | Se añaden Pausar/Reanudar reproducción y Retirar con motivo | RF-040 y RF-041 |
