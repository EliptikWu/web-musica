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
| Pantalla del bar | `/b/[bar]/pantalla` | `pantalla-bar` | encender, sonando (pedidos / lista de la casa), cerrado, en pausa, mantenimiento, cola vacía, sin conexión, video no disponible |
| Panel: Entrar | `/panel/entrar`, `/panel/cambiar-clave` | — (nuevo) | error genérico, cuenta bloqueada, sesión caducada, cambio forzado |
| Panel: Operación | `/panel` | `panel-admin` (+ `#pausa`) | abierto, en pausa, fuera de horario, mantenimiento, sin sesión, sin lista activa, vista Operador |
| Panel: Gestión | `/panel/{usuarios,establecimiento,horario,listas,bloqueos,bitacora,eventos,manual}` | — (nuevas) | vacío, cargando, error, sin permiso |
| Panel: Analítica | `/panel/analitica/*` | — (nuevas) | sin datos, datos de ejemplo rotulados |

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
| 2026-10-26 | Tarjeta de título | La miniatura es una foto pegada a la izquierda de la tira (54 px); el puesto va encima como etiqueta de papel. Sin miniatura, la tarjeta es la del prototipo | RF-006 y RF-015 piden miniatura también en la cola; a 360 px no cabe una columna más sin partir los títulos |
| 2026-10-26 | Cola | La espera de cada canción va en la franja, en forma corta («Grupo Niche, en 3 min»); que es estimada se dice una vez debajo de la cola y en «Tu canción» («unos 13 min») | RF-018; con «suena en unos» casi todas las franjas se partían en dos líneas |
| 2026-10-26 | Cola | «Tus pedidos» es una sección al final de la cola, no una pantalla aparte | PB-22 la sitúa en «La cola»; una cuarta pestaña rompería el pie del prototipo |
| 2026-10-26 | Bienvenida, cola | Sin conexión en vivo, la luz se apaga y dice «Sin conexión» | La crítica de diseño vio que «En vivo» contradecía el banner |
| 2026-10-26 | Teclas «Votaste» y «Tuya» | Sin `aria-label`; reciben el foco al reemplazar a «+1» | Revisión de accesibilidad: ARIA prohíbe la etiqueta en un `span` y el foco del teclado se perdía |
| 2026-10-26 | TV | «Suena ahora, pedida por el público» o «de la lista de la casa»; con la lista de la casa, la columna de votos dice «Lista de la casa» | RF-016: origen de lo que suena |
| 2026-10-26 | TV | Espera de cada canción en la franja, como en el teléfono | RF-018 |
| 2026-10-26 | TV | Estados nuevos: fuera de horario y mantenimiento (cartel con el horario y la hora de apertura), cola vacía, pausa (luz apagada y nota), sin conexión (franja cereza arriba) | El prototipo solo tenía la noche abierta |
| 2026-10-26 | TV | «Encender la pantalla»: un toque antes de sonar | Los navegadores no dejan reproducir con audio sin un gesto |
| 2026-10-26 | TV | En la simulación, el video es un disco que gira con la marca «Video simulado» | No hay IDs de YouTube verificados; no se finge un video real |
| 2026-10-26 | Panel | Pantallas nuevas en el sistema Rockola: entrar, cambiar la clave, sesión caducada; diálogos de Bloquear (motivo y duración), Retirar (motivo) y confirmación de mantenimiento y de cerrar la noche | RF-021, RF-023, RF-038, RF-041, RF-042 |
| 2026-10-26 | Panel | Junto a Saltar y Bloquear, Pausar/Reanudar música; cada fila de la cola lleva Retirar además de Bloquear; «Cerrar la noche» va en la franja de estado | RF-040, RF-041, RF-037 |
| 2026-10-26 | Panel | El Operador no ve Modo mantenimiento, Desbloquear ni la duración del bloqueo (D9) | RF-022 |
| 2026-10-26 | Panel | Por debajo de 1280 px una columna; en caja angosta la cola pasa a tarjetas y las teclas se reparten el ancho | El Operador lo usa desde el teléfono |
| 2026-10-26 | Panel | La bitácora habla en segunda persona de lo propio, en tercera de lo ajeno y en impersonal de lo del sistema | Que nadie lea como suya una acción que no hizo |
| 2026-10-26 | Gestión | Pantallas nuevas en el sistema Rockola (horario, listas y su editor, bloqueos, bitácora, personal, bar y QR, eventos, manual) con un mismo armazón: título en placa, una frase de qué es, la acción principal en cereza y cajas de madera | RF-023 a RF-036, RF-043 a RF-045 |
| 2026-10-26 | Panel | Las secciones son pestañas como las del teléfono, solo las que el rol puede abrir; quien entra por URL a una ajena ve «Esta sección es del Administrador» y la pantalla no pide nada | D9; el barrido encontró un bucle de 403 |
| 2026-10-26 | Editor de listas | Reordenar con flechas (Subir/Bajar), no arrastrando; el foco sigue a la canción y un anuncio dice su puesto nuevo | Teclado y lector de pantalla (WCAG 2.1.1, 4.1.3); arrastrar no es accesible sin alternativa |
| 2026-10-26 | Horario | Un tramo por día con interruptor; los que se pisan se marcan en la fila; debajo, el resumen en palabras («Jueves a sábado, 8:00 p. m. a 2:00 a. m.») | RF-036; el tramo que cruza medianoche es del día en que empieza |
| 2026-10-26 | Bar y QR | Las ayudas en palabras del bar (sin códigos de regla) y las zonas horarias por país | La crítica de diseño: «RN-01» no le dice nada al personal |
| 2026-10-26 | Personal | La clave temporal se muestra una sola vez, en un diálogo propio, con la advertencia | RF-023: nunca se ven claves después |
| 2026-10-26 | Manual | Ilustrado con capturas reales del panel, generadas por script (`npm run capturas -w @rockola/web`) | RNF-016: el manual no se desactualiza a mano |
| 2026-10-27 | Tablero analítico | Pantallas nuevas en el sistema Rockola: Resumen, Participación, Diagnóstico, Comparar y Recomendaciones, con una fila de filtros arriba (atajos primero) y el origen de los datos siempre a la vista | RF-049 a RF-054, RNF-015 |
| 2026-10-27 | Operación | «Métricas de la noche» lleva al tablero (solo el Administrador) | PB-45: las métricas son un adelanto del tablero |
| 2026-10-27 | Formularios | Las listas desplegables se nombran solo con su etiqueta (`Selector`) | Revisión de accesibilidad: el nombre incluía las opciones |
| 2026-10-27 | Cola | «Votaste» sigue hundida pero se pulsa: abre «Mover tu voto» con las canciones a las que puede pasar; el foco queda en la nueva «Votaste» | RF-014, PB-50: el mockup dice que mover el voto parte de ese botón |
| 2026-10-27 | Buscar | «Filtrar» junto al título de los resultados: género, duración y popularidad en una caja hundida; la tecla se hunde mientras está abierta | RF-055, PB-51: el mockup los ubicaba junto a la caja de búsqueda |
