package com.example.rokola.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.rokola.R
import java.util.Locale

/*
 * Rockola: una sola familia, Archivo variable (res/font/archivo.ttf, ejes wght y wdth). Compose
 * elige la fuente por peso, no por ancho, así que cada ancho es una FontFamily con su propio
 * FontVariation.width. Los valores salen de packages/ui/src/base.css y componentes.css.
 * Vidrio usa Outfit; Afiche, League Gothic para el cartel y Libre Franklin para leer de cerca
 * (temas/vidrio.css y temas/afiche.css). Licencias en assets/licencias/.
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

@OptIn(ExperimentalTextApi::class)
private fun variable(recurso: Int) = FontFamily(
    PESOS.map { peso ->
        Font(resId = recurso, weight = FontWeight(peso), variationSettings = FontVariation.Settings(FontVariation.weight(peso)))
    }
)

private val ArchivoNormal = archivo(100f)
/** Ancho 125 %: placas (nombre del bar, títulos de página, TV). */
val ArchivoPlaca = archivo(125f)
private val ArchivoSeccionBase = archivo(112f)
/** Ancho 75 %: nombres de canción, cifras y teclas. */
val ArchivoCondensada = archivo(75f)
private val Outfit = variable(R.font.outfit)
private val LibreFranklin = variable(R.font.libre_franklin)
private val LeagueGothic = FontFamily(Font(R.font.league_gothic, FontWeight.W400))

private const val CIFRAS_TABULARES = "tnum"

/** Los estilos de un tema. Los tamaños que cambian de verdad entre temas están aquí. */
@Immutable
data class Tipos(
    val cuerpo: TextStyle,
    val placa: TextStyle,
    val seccion: TextStyle,
    val condensada: TextStyle,
    val cifra: TextStyle,
    val tecla: TextStyle,
    val teclaGrande: TextStyle,
    val teclaHundida: TextStyle,
    val teclaChica: TextStyle,
    /** El título de lo que suena, dentro del arco. */
    val tituloSonando: TextStyle,
    /** «Por ahora no recibimos canciones». */
    val tituloCerrado: TextStyle,
    /** El puesto grande de «Tu canción». */
    val cifraTuya: TextStyle,
    val tituloTuyo: TextStyle,
    /** «Suena ahora», «Tu canción va de». Null: cada pantalla usa su texto de cuerpo. */
    val etiquetaArco: TextStyle?,
    val tamanoPlaca: Int?,
    val tamanoPlacaBar: Int?,
    val tamanoPlacaBarApagada: Int?,
)

private val CuerpoBase = TextStyle(fontFamily = ArchivoNormal, fontWeight = FontWeight.W400, fontSize = 15.sp, lineHeight = 1.4.em)
private val PlacaBase = TextStyle(fontFamily = ArchivoPlaca, fontWeight = FontWeight.W800, letterSpacing = (-0.01).em, lineHeight = 1.15.em)
private val SeccionBase = TextStyle(fontFamily = ArchivoSeccionBase, fontWeight = FontWeight.W600, fontSize = 16.sp)
private val CondensadaBase = TextStyle(fontFamily = ArchivoCondensada, fontWeight = FontWeight.W700, letterSpacing = 0.005.em)
private val CifraBase = TextStyle(fontFamily = ArchivoCondensada, fontWeight = FontWeight.W800, fontFeatureSettings = CIFRAS_TABULARES, lineHeight = 1.em)
private val TeclaBase = TextStyle(fontFamily = ArchivoCondensada, fontWeight = FontWeight.W800, fontSize = 18.sp, letterSpacing = 0.02.em)

val TiposRockola = Tipos(
    cuerpo = CuerpoBase,
    placa = PlacaBase,
    seccion = SeccionBase,
    condensada = CondensadaBase,
    cifra = CifraBase,
    tecla = TeclaBase,
    teclaGrande = TeclaBase.copy(fontSize = 22.sp, letterSpacing = 0.04.em),
    teclaHundida = TeclaBase.copy(fontSize = 16.sp),
    teclaChica = TeclaBase.copy(fontSize = 15.sp),
    tituloSonando = CondensadaBase.copy(fontSize = 40.sp, lineHeight = 1.em),
    tituloCerrado = CondensadaBase.copy(fontSize = 34.sp, lineHeight = 0.98.em),
    cifraTuya = CifraBase.copy(fontSize = 60.sp, letterSpacing = (-0.01).em),
    tituloTuyo = CondensadaBase.copy(fontSize = 30.sp, lineHeight = 1.em),
    etiquetaArco = null,
    tamanoPlaca = null,
    tamanoPlacaBar = null,
    tamanoPlacaBarApagada = null,
)

private val CondensadaVidrio = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.W600, letterSpacing = (-0.02).em)
private val TeclaVidrio = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.W600, fontSize = 15.sp)

val TiposVidrio = Tipos(
    cuerpo = CuerpoBase.copy(fontFamily = Outfit),
    placa = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.W600, letterSpacing = (-0.02).em, lineHeight = 1.1.em),
    seccion = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.W600, fontSize = 17.sp, letterSpacing = (-0.005).em),
    condensada = CondensadaVidrio,
    cifra = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.W700, fontFeatureSettings = CIFRAS_TABULARES, lineHeight = 1.em),
    tecla = TeclaVidrio,
    teclaGrande = TeclaVidrio.copy(fontSize = 17.sp),
    teclaHundida = TeclaVidrio.copy(fontWeight = FontWeight.W500),
    teclaChica = TeclaVidrio,
    tituloSonando = CondensadaVidrio.copy(fontSize = 34.sp, lineHeight = 1.1.em),
    tituloCerrado = CondensadaVidrio.copy(fontSize = 36.sp, lineHeight = 1.08.em, letterSpacing = (-0.025).em),
    cifraTuya = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.W700, fontSize = 52.sp, letterSpacing = (-0.03).em, lineHeight = 1.em, fontFeatureSettings = CIFRAS_TABULARES),
    tituloTuyo = CondensadaVidrio.copy(fontSize = 19.sp, lineHeight = 1.15.em, letterSpacing = 0.em),
    etiquetaArco = null,
    tamanoPlaca = 28,
    tamanoPlacaBar = 30,
    tamanoPlacaBarApagada = 20,
)

private val CartelAfiche = TextStyle(fontFamily = LeagueGothic, fontWeight = FontWeight.W400, letterSpacing = 0.005.em, lineHeight = 0.96.em)
/** --afiche-sombra-rosa: la tinta rosa corrida, sin desenfoque. */
private val SombraImprenta = Shadow(color = TonosAfiche.SombraRosa, offset = Offset(7f, 5f), blurRadius = 0f)
private val TeclaAfiche = CartelAfiche.copy(fontSize = 26.sp, letterSpacing = 0.02.em, lineHeight = 1.em)

val TiposAfiche = Tipos(
    cuerpo = CuerpoBase.copy(fontFamily = LibreFranklin),
    placa = CartelAfiche,
    seccion = CartelAfiche.copy(fontSize = 26.sp, lineHeight = 1.em, letterSpacing = 0.01.em),
    condensada = TextStyle(fontFamily = LibreFranklin, fontWeight = FontWeight.W700),
    cifra = CartelAfiche.copy(lineHeight = 1.em, fontFeatureSettings = CIFRAS_TABULARES),
    tecla = TeclaAfiche,
    teclaGrande = TeclaAfiche.copy(fontSize = 32.sp),
    teclaHundida = TeclaAfiche.copy(fontSize = 21.sp, letterSpacing = 0.04.em),
    teclaChica = TeclaAfiche.copy(fontSize = 20.sp),
    tituloSonando = CartelAfiche.copy(fontSize = 104.sp, lineHeight = 0.94.em, shadow = SombraImprenta),
    tituloCerrado = CartelAfiche.copy(fontSize = 68.sp, lineHeight = 0.96.em),
    cifraTuya = CartelAfiche.copy(fontSize = 104.sp, lineHeight = 0.9.em, letterSpacing = 0.em, shadow = SombraImprenta),
    tituloTuyo = CartelAfiche.copy(fontSize = 50.sp, lineHeight = 0.96.em),
    etiquetaArco = CartelAfiche.copy(fontSize = 27.sp, lineHeight = 1.em),
    tamanoPlaca = 52,
    tamanoPlacaBar = 54,
    tamanoPlacaBarApagada = 30,
)

val Archivo: FontFamily @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.cuerpo.fontFamily!!
val ArchivoSeccion: FontFamily @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.seccion.fontFamily!!

/** Cuerpo: 400, 15/1,4 (body en base.css). */
val Cuerpo: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.cuerpo
/** .placa: 125 % / 800, −0,01em, interlínea 1,15. El tamaño lo pone cada pieza. */
val Placa: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.placa
/** .titulo-seccion: 112 % / 600, 16. */
val TituloSeccion: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.seccion
/** .condensada: título de canción, 75 % / 700, +0,005em. Conserva la ortografía real. */
val Condensada: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.condensada
/** .cifra: votos y puestos, 75 % / 800, cifras tabulares, interlínea 1. */
val Cifra: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.cifra
/** .tecla: 75 % / 800, 18, +0,02em. */
val Tecla: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.tecla
/** .tecla--grande: 22, +0,04em. */
val TeclaGrande: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.teclaGrande
/** .tecla--hundida y .tecla--tuya: 16 («Votaste», «Tuya»). */
val TeclaHundida: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.teclaHundida
/** .tecla--chica: 15. */
val TeclaChica: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.teclaChica
val TituloSonando: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.tituloSonando
val TituloCerrado: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.tituloCerrado
val CifraTuya: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.cifraTuya
val TituloTuyo: TextStyle @Composable @ReadOnlyComposable get() = LocalDiseno.current.tipos.tituloTuyo

/** La etiqueta dentro del arco: en Afiche es de cartel; en los demás, el estilo que traiga. */
@Composable
@ReadOnlyComposable
fun estiloEtiqueta(base: TextStyle): TextStyle = LocalDiseno.current.tipos.etiquetaArco ?: base

/**
 * El cartel de Afiche es enorme: la palabra más larga tiene que caber sin partirse, también con
 * la letra del sistema grande. [anchoDp]: lo que tiene disponible. En los otros temas, igual.
 */
@Composable
@ReadOnlyComposable
fun estiloCartel(base: TextStyle, texto: String, anchoDp: Float = 328f): TextStyle {
    if (!LocalDiseno.current.esAfiche) return base
    val letras = texto.split(' ').maxOfOrNull { it.length }?.coerceAtLeast(1) ?: 1
    val cabe = anchoDp / (letras * ANCHO_LETRA_CARTEL) / LocalDensity.current.fontScale
    return base.copy(fontSize = minOf(base.fontSize.value, cabe).sp)
}

/** Ancho medio de una mayúscula de League Gothic, en em (medido con holgura). */
private const val ANCHO_LETRA_CARTEL = 0.44f

private val ESPANOL = Locale.forLanguageTag("es-CO")

/** En Afiche los títulos grandes van en mayúsculas de cartel; en los demás temas, tal cual. */
@Composable
@ReadOnlyComposable
fun String.comoCartel(): String = if (LocalDiseno.current.esAfiche) uppercase(ESPANOL) else this

/** Los componentes de Material que lean MaterialTheme.typography reciben la letra del tema. */
internal fun tipografiaDe(t: Tipos) = Typography(
    displayLarge = t.placa,
    displayMedium = t.placa,
    displaySmall = t.placa,
    headlineLarge = t.placa,
    headlineMedium = t.placa,
    headlineSmall = t.placa,
    titleLarge = t.seccion,
    titleMedium = t.seccion,
    titleSmall = t.seccion,
    bodyLarge = t.cuerpo,
    bodyMedium = t.cuerpo,
    bodySmall = t.cuerpo,
    labelLarge = t.tecla,
    labelMedium = t.tecla,
    labelSmall = t.teclaChica,
)
