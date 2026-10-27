# Notas de diseño (borrador de trabajo)

## Vidrio

**Color.** Carbón `#121016` de fondo. Manchas `#F4A98E`, `#E07FAE`, `#9A86EC` con
`blur(80px)` y opacidad 0,35–0,5, en sitios distintos en cada pantalla. Vidrio blanco 8 %
(filas 7 %, tarjetas destacadas 12 %), borde blanco 13 %. Degradado de acento
`#F6B29A → #E58BB0 → #A894EE` solo en tres sitios: el botón principal de cada pantalla, la
tarjeta «Tu canción» y la franja «Abierto». Texto `#FFFFFF` y `rgba(255,255,255,.62)`;
`#2A1230` sobre el degradado.

**Tipo.** Outfit. Escala móvil 13 / 15 / 17 / 20 / 26 / 34; 400 texto, 500 metadatos,
600 títulos, 700 solo para cifras (votos, puesto). Interletra −0,02em desde 26px.

**Maqueta.** Una columna, alineada a la izquierda, margen 16px. Jerarquía de radios: 28 la
tarjeta grande de cada pantalla, 20 los grupos de filas, 14 las carátulas medianas,
píldora los botones. Las listas no son una tarjeta por fila: un solo grupo de vidrio con
separadores finos.

```
Bienvenida                       Cola
[Nombre del bar]   (o En vivo)   La cola            (o En vivo)
+-- vidrio 28 ---------------+   9 canciones, unos 38 min
| [portada 132] Suena ahora  |   +-- degradado 28 -----------+
|               Rebelión     |   | Tu canción va de #3       |
|               Joe Arroyo   |   | Monaco, subió 2 puestos   |
| ======------  2:41  5:58   |   +---------------------------+
+----------------------------+   1 [p] Cali Pachanguero 21 [+1]
Lo más votado de la noche        2 [p] TQG             18 [Votaste]
1 [p] Rebelión        24         3 [p] Monaco          15 [Tuya]
2 [p] Cali Pachanguero 21        ...
3 [p] TQG             18         Con los mismos votos, va primero...
Sin registro ni descargas...     [mini reproductor]
[====== Entrar y votar ======]   [Inicio  Buscar  Cola]
```

**Principios.** (1) Lo que suena y los votos mandan; el resto se calla. (2) El degradado
es escaso a propósito: si aparece, es lo que hay que tocar o lo que es tuyo. (3) El
cliente no controla la música: el mini reproductor no tiene play ni pausa.

**Revisión contra lo genérico (antes de construir).**
- «Kit de tarjetas SaaS» (cada cosa en su tarjeta, mismo radio): cambiado a grupos con
  separadores y radios por jerarquía.
- Etiquetas en mayúsculas espaciadas sobre cada sección: no; títulos de sección en
  oración, 17px/600.
- «A · B · C» en los metadatos: cambiado a comas («Bad Bunny, puesto 3, 13 votos»).
- Número grande con etiqueta pequeña: se queda solo en «Tu canción va de #3», porque el
  puesto es justo lo que esa persona quiere saber.
- Botón deshabilitado para el video largo: cambiado por el motivo escrito en su sitio
  («Dura más de 10 min»), que se lee y no depende de un gris.
- Reproductor con controles de música: quitado; el bar controla la música.

## Rockola

**Idea.** El teléfono es el panel de selección de una rockola (la palabra viene de la marca
Rock-Ola). Tres piezas del objeto real hacen todo el trabajo: el **arco de tubos de luz**
(ventana de «Suena ahora»), las **tarjetas de título** (tira de papel crema con franja de
color, una por canción) y las **teclas de selección** (votar = hundir la tecla; «Votaste» =
la tecla se queda abajo). Los pedidos son **fichas**, como las monedas de la rockola de
tienda: tres círculos, los libres encendidos.

**Color.** Nogal `#2A1711` de fondo; madera clara `#3A2218` en superficies; crema de
tarjeta `#F4E7CB`; cereza `#B8282F` (franja de las tarjetas y tecla principal); ámbar
`#F5A524` (luz: arco, ficha libre, franja de «Tuya»); cromo como degradado gris
`#F4F4F2 → #A9AEB6 → #E4E6EA` en marcos y teclas; tinta `#23140F` sobre crema. Texto
sobre nogal en crema y crema al 70 %.

**Tipo.** Archivo variable, una sola familia jugando con su eje de ancho: expandida 125 %
y 800 para el nombre del bar y los títulos (las placas de las rockolas), condensada 75 %
y 700 en mayúsculas para los títulos de canción en las tarjetas (así se imprimían; es la
única mayúscula, y tiene motivo), normal 400/500 para el resto.

**Maqueta.** Simétrica y centrada, como el mueble: arco arriba, tarjetas apiladas en el
medio, teclas abajo. Radios: el arco es un semicírculo; tarjetas de 6px (papel); teclas
de 12px. Nada de píldoras: aquí los botones son teclas.

```
        [NOMBRE DEL BAR]
   .-''''''''''''''''''''''-.        <- tubos ámbar/cereza
  |   .------------------.   |
  |   | Suena ahora      |   |       <- ventana crema
  |   | REBELIÓN         |   |
  |   | Joe Arroyo  24   |   |
  |   '------------------'   |
  |   2:41 ======----- 5:58  |
  '--------------------------'
  Lo más votado de la noche
  [1 | REBELIÓN / Joe Arroyo     | 24]   <- tarjetas de título
  [2 | HIPS DON'T LIE / Shakira  | 19]
  [3 | CALI PACHANGUERO / Niche  | 15]
  [====== ENTRAR Y VOTAR ======]        <- tecla cereza
```

**Principios.** (1) El arco es lo único que brilla. (2) Cada canción es una tarjeta de
título, en todas las pantallas, también en la TV y en el panel. (3) El estado del voto se
ve en la tecla, no en un color: arriba, abajo, con tu franja.

**Revisión contra lo genérico.**
- Crema + serif + terracota (el rasgo 1): el crema aquí es solo el papel de las tarjetas,
  sobre nogal oscuro; nada de serif; cereza saturada, no arcilla.
- Riesgo de disfraz retro (cromo y neón por todas partes): el cromo solo en marcos de
  teclas y del arco; sin letras de neón, sin estrellas, sin guiños de los 50.
- Monoespaciada «de máquina de escribir» en las tarjetas: descartada (rasgo genérico);
  la condensada de Archivo es más fiel a las tarjetas impresas.
- Mayúsculas forzadas en los títulos de las tarjetas: probadas y quitadas en la revisión
  de las capturas, porque cambiaban el nombre real de canciones como DtMF o NUEVAYoL. Las
  mayúsculas quedan solo en las teclas, como en las teclas del mueble.

## Afiche

**Idea.** La noche anunciada como un cartel callejero de tipos de madera (los de verbenas,
casetas y conciertos de salsa): fondo de tinta partida fluorescente, letras góticas
condensadas enormes, y la jerarquía dicha solo con el tamaño. Lo de cartel se gasta en
tres sitios (lo que suena, tu canción, la franja «Abierto»); las listas van sobrias, tinta
negra sobre papel. Los estados del voto salen de la imprenta: «+1» es una casilla
recuadrada, «Votaste» es un **sello** de tinta magenta, «Tuya» va **resaltada**, «Pedir»
es un bloque de tinta negra.

**Color.** Papel periódico `#ECE9E1` (no crema: más gris y frío), tinta `#17120F`; tinta
partida `#FFE600 → #FF7A2F → #FF2E93` (amarillo, naranja, rosa fluorescentes) para los tres
bloques de cartel; magenta de sello `#C8136B` (texto que pasa 4,5:1 sobre el papel);
amarillo de resaltador `#FFE600` para «Tuya».

**Tipo.** League Gothic (gótica condensada de tipos de madera) para titulares y cifras de
cartel; Libre Franklin (la gótica de los carteles americanos, de la misma familia
histórica) 400–800 para todo lo que se lee de cerca. Los nombres de canción en las listas
van en Libre Franklin con su grafía real.

**Maqueta.** Bloques a sangre, de borde a borde, como pliegos; reglas gruesas de 3px, no
finas; esquinas casi rectas (4px en botones, para que no corten). Titulares centrados en
los bloques de cartel; listas alineadas a la izquierda con líneas punteadas de corte.

```
######## tinta partida a sangre ########
           HOY EN
       [NOMBRE DEL BAR]
          o En vivo
         SUENA AHORA
          REBELIÓN               <- 90px, League Gothic
      Joe Arroyo, 24 votos
  ===============---------- 2:41 5:58
#########################################
LO MÁS VOTADO DE LA NOCHE
1  Rebelión ............... 24
2  Hips Don't Lie ......... 19
3  Cali Pachanguero ....... 15
[######## ENTRAR Y VOTAR ########]       <- bloque negro, letras amarillas
```

**Principios.** (1) El tamaño es la jerarquía: lo que suena es lo más grande de la
pantalla. (2) El color fluorescente es escaso y siempre lleva tinta negra encima. (3) Cada
estado tiene un gesto de imprenta (recuadro, sello, resaltador, bloque).

**Revisión contra lo genérico.**
- Rasgo 3 (periódico: reglas finas, radio cero, columnas densas): evitado con reglas
  gruesas, bloques a sangre, escala de cartel y color fluorescente; nada de columnas.
- Rasgo 1 (crema + serif + terracota): papel gris de periódico, sin serif, sin terracota.
- Mayúsculas: solo en los titulares de cartel (es la letra de madera); las etiquetas
  pequeñas van en oración.
- Puntos suspensivos de guía («Rebelión ....... 24»): se quedan porque unen nombre y cifra
  en una lista de tres, como en los carteles de programación; no se usan en las listas
  largas.
