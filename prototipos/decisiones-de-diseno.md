# Decisiones de diseño: prototipos de Rockola

Tres interfaces (cliente 390 × 844, pantalla del bar 1280 × 720, panel 1440 × 1000) en tres
direcciones visuales con el mismo contenido: **Vidrio** (la del brief), **Rockola** (el
panel de selección de una rockola) y **Afiche** (cartel callejero de tipos de madera).
Canciones, artistas y títulos son reales; duraciones, votos, horas y métricas son datos de
ejemplo. Lo pendiente va entre corchetes: `[Nombre del bar]`, `[Usuario]`, `[enlace del bar]`.

## Decisiones comunes

| Tema | Qué se decidió | Por qué |
|---|---|---|
| Jerarquía | Lo que suena es lo más grande de cada pantalla; después, la posición de la canción propia («Tu canción va de #3»). | Es lo que la persona viene a mirar en la mesa: qué suena y cuándo suena lo suyo. |
| Acento | El color fuerte (degradado, luz o tinta fluorescente) aparece solo en tres sitios: el botón principal, «Tu canción» y la franja «Abierto». | Si el acento está en todas partes deja de señalar; así marca lo que se toca o lo que es tuyo. |
| Estados del voto | «+1», «Votaste» y «Tuya» se distinguen por texto y por forma, no solo por color. | Funciona con daltonismo y en pantallas con brillo bajo (WCAG 1.4.1). |
| Reglas visibles | El contador dice «Te quedan 2 de 3 pedidos»; el video largo dice «Dura más de 10 min» en lugar de un botón gris; la nota de desempate usa el caso real («Monaco va antes que La Bicicleta»). | «2 de 3» solo es ambiguo (¿usados o libres?); un botón deshabilitado no explica nada. |
| Sin controles de música | El mini reproductor del cliente no tiene play ni pausa. | El cliente no controla la música; el bar sí (Saltar y Bloquear en el panel). |
| Datos coherentes | La cola, la TV y el panel muestran el mismo momento de la noche; el empate de 15 votos se resuelve igual en las tres. | Un evaluador nota si una pantalla contradice a otra. |
| Accesibilidad | Contraste medido contra el fondo pintado (mínimo 4,5:1; 3:1 en texto grande), objetivos de 44 px o más, foco visible con teclado, `lang="es"` y nombres accesibles en botones de ícono. | El vidrio y los degradados cambian el fondo punto a punto: se midió el píxel más desfavorable, no un color promedio. |
| Íconos | Solo los que tienen función: pestañas Inicio, Buscar y Cola, lupa, borrar, flecha de subida. Sin emojis. | Pedido del brief; cada ícono responde a una acción o a un dato. |

## Por dirección

| Dirección | Color | Tipografía | Lo que la hace propia |
|---|---|---|---|
| Vidrio | Carbón `#121016`, manchas durazno/rosa/lavanda difuminadas, vidrio blanco 7–12 % con borde 13 %, degradado `#F6B29A → #E58BB0 → #A894EE` con texto `#2A1230`. | Outfit 400/500/600/700; cifras en 700. | Radios por jerarquía (28 tarjeta principal, 20 grupos, píldora en botones) y listas en un solo grupo con separadores, no una tarjeta por fila. Las manchas se colocaron detrás de carátulas y huecos, no detrás del texto secundario. |
| Rockola | Nogal `#2A1711`, papel `#F4E7CB`, cereza `#B8282F`, ámbar `#F5A524`, cromo en marcos. | Archivo con su eje de ancho: expandida en títulos, condensada en los nombres de canción. | Arco de luz (lo que suena), tarjetas de título (cada canción), teclas («Votaste» es la tecla hundida) y fichas (los pedidos). La luz es el estado: encendida se reciben canciones, apagada no. |
| Afiche | Papel `#ECE9E1`, tinta `#17120F`, tinta partida amarillo → naranja → rosa fluorescente, magenta de sello `#B0105D`. | League Gothic para titulares y cifras de cartel; Libre Franklin para lo que se lee de cerca. | El tamaño es la jerarquía. Gestos de imprenta para los estados: casilla («+1»), sello («Votaste»), resaltador («Tuya»), bloque negro («Pedir»). Cerrado o en pausa, el cartel pierde la tinta fluorescente. |

## Cómo se traduce a Bubble

| Pieza del prototipo | En Bubble | Verificado en |
|---|---|---|
| Colores y fuentes | Variables de color y de fuente, usadas por los estilos. | Manual, *Styles* |
| Degradados (acento, tinta partida, arco) | Fondo de tipo *Gradient*, lineal o radial, con color inicial, intermedio y final. Afiche usa 4 paradas en el prototipo: en Bubble se queda en 3 (amarillo, naranja, rosa). | Manual, *Styling properties* |
| Vidrio esmerilado (`backdrop-filter: blur`) | No es nativo. Opciones: un plugin de desenfoque de fondo, o CSS en un elemento HTML apuntando a IDs de elemento. Sin ellas, el respaldo es un fondo translúcido sólido. | Manual, *Styles* (no lo menciona); foro y plugins de Bubble. **TBD**: elegir y probar el plugin. |
| Cola ordenada por votos con desempate | *Repeating group* con «Do a search for» Pedido: orden por votos descendente y, como regla adicional, por fecha de creación ascendente. | Manual, *Search* («Adding additional sorting rules… applied in the order listed») |
| Teléfono anónimo, sin registro | Usuario temporal de Bubble: se crea con una cookie al primer acceso y dura 72 horas en el mismo dispositivo. Los 3 pedidos y el voto por canción se guardan en él. | Manual, *User accounts* |
| Mini reproductor y barra de pestañas | Elementos reutilizables dentro de un *Floating group* anclado abajo. | Manual, *Containers* |
| Estados «después de pedir» y «En pausa» | *Custom states* para lo que es de una pantalla; el estado del bar (abierto, en pausa, mantenimiento, fuera de horario) en un *Option set*. | Manual, *States* y *Option sets* |
| Video en la TV | Elemento *Video* con el ID de YouTube y reproducción automática. La barra de progreso se calcula con la hora de inicio y la duración guardadas, porque el elemento no ofrece el avance del video. | Manual, *Video element* (ID, autoplay, bucle; no menciona avance). **TBD**: confirmar en una prueba que no se puede leer el tiempo del video. |
| Límite de 10 minutos | La duración viene de la API de YouTube al buscar; el botón «Pedir» se sustituye por el motivo cuando pasa de 600 s. | **TBD**: verificar el campo de duración en la API de YouTube Data al conectarla. |

## Fuentes de lo verificado en Bubble

- [Search](https://manual.bubble.io/core-resources/data/search) (orden con reglas adicionales)
- [User accounts](https://manual.bubble.io/help-guides/data/user-accounts) (usuario temporal, cookie, 72 horas)
- [Containers](https://manual.bubble.io/core-resources/elements/containers) (floating group, repeating group)
- [Styles](https://manual.bubble.io/help-guides/design/variables-and-styles/styles) y [Styling properties](https://manual.bubble.io/core-resources/elements/styling-properties) (variables, degradados)
- [Video element](https://manual.bubble.io/core-resources/bubble-elements/element-properties/web-element-properties/visual-element-properties/video-element)
- [States](https://manual.bubble.io/core-resources/elements/states) y [Option sets](https://manual.bubble.io/help-guides/data/static-data/option-sets)
- Desenfoque de fondo: no hay opción nativa documentada; existen plugins como [Blur Effect](https://bubble.io/plugin/blur-effect-1750174975753x253003163959296000) y la vía de CSS descrita en el [foro de Bubble](https://forum.bubble.io/t/translucent-background-for-elements/121251).
