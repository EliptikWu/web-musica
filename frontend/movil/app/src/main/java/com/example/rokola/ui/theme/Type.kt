package com.example.rokola.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.rokola.R

/*
 * Una sola familia: Archivo variable (res/font/archivo.ttf, ejes wght y wdth).
 * Compose elige la fuente por peso, no por ancho, así que cada ancho es una FontFamily
 * con su propio FontVariation.width. Los valores salen de packages/ui/src/base.css y
 * componentes.css (.placa, .titulo-seccion, .condensada, .cifra, .tecla).
 */

private val PESOS = listOf(400, 500, 600, 700, 800)

@OptIn(ExperimentalTextApi::class)
private fun archivo(ancho: Float) = FontFamily(
    PESOS.map { peso ->
        Font(
            resId = R.font.archivo,
            weight = FontWeight(peso),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(peso),
                FontVariation.width(ancho),
            ),
        )
    }
)

/** Ancho 100 %: cuerpo. */
val Archivo = archivo(100f)
/** Ancho 125 %: placas (nombre del bar, títulos de página, TV). */
val ArchivoPlaca = archivo(125f)
/** Ancho 112 %: títulos de sección. */
val ArchivoSeccion = archivo(112f)
/** Ancho 75 %: nombres de canción, cifras y teclas. */
val ArchivoCondensada = archivo(75f)

private const val CIFRAS_TABULARES = "tnum"

/** Cuerpo: 400, 15/1,4 (body en base.css). */
val Cuerpo = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight.W400,
    fontSize = 15.sp,
    lineHeight = 1.4.em,
)

/** .placa: 125 % / 800, −0,01em, interlínea 1,15. El tamaño lo pone cada pieza. */
val Placa = TextStyle(
    fontFamily = ArchivoPlaca,
    fontWeight = FontWeight.W800,
    letterSpacing = (-0.01).em,
    lineHeight = 1.15.em,
)

/** .titulo-seccion: 112 % / 600, 16. */
val TituloSeccion = TextStyle(
    fontFamily = ArchivoSeccion,
    fontWeight = FontWeight.W600,
    fontSize = 16.sp,
)

/** .condensada: título de canción, 75 % / 700, +0,005em. Conserva la ortografía real. */
val Condensada = TextStyle(
    fontFamily = ArchivoCondensada,
    fontWeight = FontWeight.W700,
    letterSpacing = 0.005.em,
)

/** .cifra: votos y puestos, 75 % / 800, cifras tabulares, interlínea 1. */
val Cifra = TextStyle(
    fontFamily = ArchivoCondensada,
    fontWeight = FontWeight.W800,
    fontFeatureSettings = CIFRAS_TABULARES,
    lineHeight = 1.em,
)

/** .tecla: 75 % / 800, 18, +0,02em. El texto se pasa a mayúsculas en la tecla. */
val Tecla = TextStyle(
    fontFamily = ArchivoCondensada,
    fontWeight = FontWeight.W800,
    fontSize = 18.sp,
    letterSpacing = 0.02.em,
)

/** .tecla--grande: 22, +0,04em. */
val TeclaGrande = Tecla.copy(fontSize = 22.sp, letterSpacing = 0.04.em)

/** .tecla--hundida y .tecla--tuya: 16 («Votaste», «Tuya»). */
val TeclaHundida = Tecla.copy(fontSize = 16.sp)

/** .tecla--chica: 15. */
val TeclaChica = Tecla.copy(fontSize = 15.sp)

/**
 * Los componentes de Material que lean MaterialTheme.typography reciben Archivo, nunca
 * los tamaños por defecto de Material.
 */
internal val TipografiaRockola = Typography(
    displayLarge = Placa,
    displayMedium = Placa,
    displaySmall = Placa,
    headlineLarge = Placa,
    headlineMedium = Placa,
    headlineSmall = Placa,
    titleLarge = TituloSeccion,
    titleMedium = TituloSeccion,
    titleSmall = TituloSeccion,
    bodyLarge = Cuerpo,
    bodyMedium = Cuerpo,
    bodySmall = Cuerpo,
    labelLarge = Tecla,
    labelMedium = Tecla,
    labelSmall = TeclaChica,
)
