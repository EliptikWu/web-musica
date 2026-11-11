package com.example.rokola.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/* Radios, brillo y movimiento de tokens.css. Sin píldoras. */

val RadioInsignia = RoundedCornerShape(4.dp)
val RadioTarjeta = RoundedCornerShape(6.dp)
val RadioDia = RoundedCornerShape(8.dp)
val RadioTecla = RoundedCornerShape(10.dp)
val RadioAviso = RoundedCornerShape(12.dp)
val RadioCaja = RoundedCornerShape(14.dp)
val RadioMarco = RoundedCornerShape(18.dp)
/** Miniatura y etiqueta del puesto sobre ella (componentes.css, .tarjeta__miniatura: 3px). */
val RadioFoto = RoundedCornerShape(3.dp)

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
val RadioArco = FormaArco(altoTapa = 0.34f, radioInferior = 22.dp)
/** .arco__luz: 50% 50% 18px 18px / 33% 33% 18px 18px (tubos de luz). */
val RadioArcoLuz = FormaArco(altoTapa = 0.33f, radioInferior = 18.dp)

/** :focus-visible: 3px sólido en ámbar, separado 3px. */
object Foco {
    val grosor = 3.dp
    val separacion = 3.dp
    val color = Ambar
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

/** Los componentes de Material que lean MaterialTheme.shapes reciben los radios Rockola. */
internal val FormasRockola = Shapes(
    extraSmall = RadioInsignia,
    small = RadioTarjeta,
    medium = RadioTecla,
    large = RadioCaja,
    extraLarge = RadioMarco,
)
