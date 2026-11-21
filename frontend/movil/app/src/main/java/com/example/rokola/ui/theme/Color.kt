package com.example.rokola.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/*
 * Colores de la dirección Rockola, 1:1 con C:\web-musica\packages\ui\src\tokens.css.
 * Ningún otro archivo de la interfaz escribe un color suelto. Los degradados viven en
 * Degradados.kt y los valores de Vidrio y Afiche en Diseno.kt.
 *
 * `…Base` es el valor de Rockola. El nombre sin sufijo es el token vigente: lo da el tema del bar.
 */

// Madera
val Nogal = Color(0xFF2A1711)
val NogalHondo = Color(0xFF1D0F0B)
val Madera = Color(0xFF3A2218)
val MaderaClaraBase = Color(0xFF4A2C1F)
val BordeBase = Color(0xFFF4E7CB).copy(alpha = 0.14f)
/** Fondo hundido en la madera: pestañas, días, etiquetas. */
val HuecoBase = Color(0xFF000000).copy(alpha = 0.22f)

// Gráficos del tablero (forma «énfasis»: el ámbar es el dato que importa)
val DatoContexto = Color(0xFF85715F)
val Rejilla = Color(0xFFF4E7CB).copy(alpha = 0.10f)
val Eje = Color(0xFFF4E7CB).copy(alpha = 0.28f)

// Papel y texto
val CremaBase = Color(0xFFF4E7CB)
val Crema2Base = Color(0xFFF4E7CB).copy(alpha = 0.72f)
val PapelBase = Color(0xFFF4E7CB)
val PapelSombra = Color(0xFFD9C7A3)
val PapelApagadoBase = Color(0xFFCDBFA6)
val TintaBase = Color(0xFF23140F)
val Tinta2Base = Color(0xFF5B4434)
val GrisApagadoBase = Color(0xFF6B5A50)

// Acentos
val CerezaBase = Color(0xFFB8282F)
val CerezaHondo = Color(0xFF7E171D)
val AmbarBase = Color(0xFFF5A524)
val AmbarClaro = Color(0xFFFFD27A)

/** --dato: el dato resaltado de un gráfico es la luz. */
val Dato = AmbarBase

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

// Los tokens vigentes, según el tema del bar.
val MaderaClara: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.maderaClara
val Borde: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.borde
val Hueco: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.hueco
val Crema: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.crema
val Crema2: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.crema2
val Papel: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.papel
val PapelApagado: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.papelApagado
val Tinta: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.tinta
val Tinta2: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.tinta2
val GrisApagado: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.grisApagado
val Cereza: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.cereza
val Ambar: Color @Composable @ReadOnlyComposable get() = LocalDiseno.current.paleta.ambar
