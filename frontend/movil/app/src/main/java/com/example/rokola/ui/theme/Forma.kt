package com.example.rokola.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/* Radios, brillo y movimiento de tokens.css. Solo Vidrio usa píldoras (D13). */

/** Los radios de un tema. Con las medidas sueltas para lo que se dibuja a mano. */
@Immutable
class Radios(
    insigniaDp: Dp,
    tarjetaDp: Dp,
    diaDp: Dp,
    teclaDp: Dp,
    avisoDp: Dp,
    val cajaDp: Dp,
    val marcoDp: Dp,
    fotoDp: Dp,
    /** La caja de filtros: en Rockola es de tecla, y en Vidrio una píldora no le sirve. */
    filtrosDp: Dp,
    val buscadorDp: Dp,
) {
    val insignia: Shape = RoundedCornerShape(insigniaDp)
    val tarjeta: Shape = RoundedCornerShape(tarjetaDp)
    val dia: Shape = RoundedCornerShape(diaDp)
    val tecla: Shape = RoundedCornerShape(teclaDp)
    val aviso: Shape = RoundedCornerShape(avisoDp)
    val caja: Shape = RoundedCornerShape(cajaDp)
    val marco: Shape = RoundedCornerShape(marcoDp)
    val foto: Shape = RoundedCornerShape(fotoDp)
    val filtros: Shape = RoundedCornerShape(filtrosDp)
    val buscador: Shape = RoundedCornerShape(buscadorDp)
}

val RadiosRockola = Radios(4.dp, 6.dp, 8.dp, 10.dp, 12.dp, 14.dp, 18.dp, 3.dp, 10.dp, 10.dp)
val RadiosVidrio = Radios(999.dp, 20.dp, 16.dp, 999.dp, 20.dp, 20.dp, 28.dp, 11.dp, 20.dp, 26.dp)
val RadiosAfiche = Radios(3.dp, 0.dp, 2.dp, 4.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)

val RadioInsignia: Shape @Composable @ReadOnlyComposable get() = LocalDiseno.current.radios.insignia
val RadioTarjeta: Shape @Composable @ReadOnlyComposable get() = LocalDiseno.current.radios.tarjeta
val RadioDia: Shape @Composable @ReadOnlyComposable get() = LocalDiseno.current.radios.dia
val RadioTecla: Shape @Composable @ReadOnlyComposable get() = LocalDiseno.current.radios.tecla
val RadioAviso: Shape @Composable @ReadOnlyComposable get() = LocalDiseno.current.radios.aviso
val RadioCaja: Shape @Composable @ReadOnlyComposable get() = LocalDiseno.current.radios.caja
val RadioMarco: Shape @Composable @ReadOnlyComposable get() = LocalDiseno.current.radios.marco
/** Miniatura y etiqueta del puesto sobre ella (componentes.css, .tarjeta__miniatura: 3px). */
val RadioFoto: Shape @Composable @ReadOnlyComposable get() = LocalDiseno.current.radios.foto
val RadioFiltros: Shape @Composable @ReadOnlyComposable get() = LocalDiseno.current.radios.filtros
val RadioBuscador: Shape @Composable @ReadOnlyComposable get() = LocalDiseno.current.radios.buscador

/**
 * Tapa elíptica del arco (estilos.css .arco y .arco__luz):
 * `50% 50% R R / alto% alto% R R`. Las esquinas de arriba son media elipse que ocupa todo
 * el ancho y `alto` del alto; las de abajo, un radio fijo.
 */
class FormaArco(private val altoTapa: Float, private val radioInferior: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val rx = w / 2f
        val ry = h * altoTapa
        val rb = with(density) { radioInferior.toPx() }
        val tapa = Path().apply {
            moveTo(0f, ry)
            arcTo(Rect(0f, 0f, 2 * rx, 2 * ry), 180f, 90f, false)
            arcTo(Rect(w - 2 * rx, 0f, w, 2 * ry), 270f, 90f, false)
            lineTo(w, h - rb)
            arcTo(Rect(w - 2 * rb, h - 2 * rb, w, h), 0f, 90f, false)
            lineTo(rb, h)
            arcTo(Rect(0f, h - 2 * rb, 2 * rb, h), 90f, 90f, false)
            close()
        }
        return Outline.Generic(tapa)
    }
}

/** .arco: 50% 50% 22px 22px / 34% 34% 22px 22px (marco de cromo). */
val FormaArcoCromo = FormaArco(altoTapa = 0.34f, radioInferior = 22.dp)
/** .arco__luz: 50% 50% 18px 18px / 33% 33% 18px 18px (tubos de luz). */
val RadioArcoLuz = FormaArco(altoTapa = 0.33f, radioInferior = 18.dp)
/** La silueta de lo que suena: el arco en Rockola, la tarjeta o el bloque en los otros temas. */
val RadioArco: Shape
    @Composable @ReadOnlyComposable get() = LocalDiseno.current.let { if (it.esRockola) FormaArcoCromo else it.radios.marco }

/** :focus-visible: 3px sólido en ámbar, separado 3px. */
object Foco {
    val grosor = 3.dp
    val separacion = 3.dp
    val color = AmbarBase
}

/** --brillo-ambar: 0 0 70px -6px (el color está en Color.kt). */
object BrilloAmbarMedidas {
    val desenfoque = 70.dp
    val extension = (-6).dp
}

/** Movimiento: --pulsar y --reordenar, en milisegundos. */
object Movimiento {
    const val PULSAR = 80
    const val REORDENAR = 300
}

/** Los componentes de Material que lean MaterialTheme.shapes reciben los radios del tema. */
internal fun formasDe(r: Radios) = Shapes(
    extraSmall = r.insignia as RoundedCornerShape,
    small = r.tarjeta as RoundedCornerShape,
    medium = r.tecla as RoundedCornerShape,
    large = r.caja as RoundedCornerShape,
    extraLarge = r.marco as RoundedCornerShape,
)
