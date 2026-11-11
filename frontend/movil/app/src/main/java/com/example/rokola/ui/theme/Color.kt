package com.example.rokola.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Colores de la dirección Rockola, 1:1 con C:\web-musica\packages\ui\src\tokens.css.
 * Ningún otro archivo escribe un color suelto. Los degradados viven en Degradados.kt.
 */

// Madera
val Nogal = Color(0xFF2A1711)
val NogalHondo = Color(0xFF1D0F0B)
val Madera = Color(0xFF3A2218)
val MaderaClara = Color(0xFF4A2C1F)
val Borde = Color(0xFFF4E7CB).copy(alpha = 0.14f)
/** Fondo hundido en la madera: pestañas, días, etiquetas. */
val Hueco = Color(0xFF000000).copy(alpha = 0.22f)

// Gráficos del tablero (forma «énfasis»: el ámbar es el dato que importa)
val DatoContexto = Color(0xFF85715F)
val Rejilla = Color(0xFFF4E7CB).copy(alpha = 0.10f)
val Eje = Color(0xFFF4E7CB).copy(alpha = 0.28f)

// Papel y texto
val Crema = Color(0xFFF4E7CB)
val Crema2 = Color(0xFFF4E7CB).copy(alpha = 0.72f)
val Papel = Color(0xFFF4E7CB)
val PapelSombra = Color(0xFFD9C7A3)
val PapelApagado = Color(0xFFCDBFA6)
val Tinta = Color(0xFF23140F)
val Tinta2 = Color(0xFF5B4434)
val GrisApagado = Color(0xFF6B5A50)

// Acentos
val Cereza = Color(0xFFB8282F)
val CerezaHondo = Color(0xFF7E171D)
val Ambar = Color(0xFFF5A524)
val AmbarClaro = Color(0xFFFFD27A)

/** --dato: el dato resaltado de un gráfico es la luz. */
val Dato = Ambar

// Brillos, pie, video, QR
val LuzBrillo = Color(0xFFFFE7B0)
val FichaBrillo = Color(0xFFFFF0C4)
val FichaCanto = Color(0xFFB8741A)
val PieFondo = Color(0xFF2E1A12)
val PieFilete = Color(0xFF9EA3AA)
val VideoFondo = Color(0xFF1A0E0A)
val QrFondo = Color(0xFFFFFFFF)

// Teclas
val TeclaCanto = Color(0xFF7A6450)

/** Color de --brillo-ambar (0 0 70px -6px); las medidas están en Forma.kt. */
val BrilloAmbar = Color(0xFFF5A524).copy(alpha = 0.45f)
