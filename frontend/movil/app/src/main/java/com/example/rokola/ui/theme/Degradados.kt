package com.example.rokola.ui.theme

import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/*
 * Degradados de tokens.css con los mismos stops. En CSS, 180deg es de arriba abajo
 * (verticalGradient) y 90deg de izquierda a derecha (horizontalGradient).
 * Un color sin posición se reparte como en CSS: el primero en 0 y el último en 1.
 */

// Teclas
val TeclaCara = Brush.verticalGradient(
    0f to Color(0xFFFFF8E6),
    1f to Color(0xFFE8D8B6),
)
val TeclaCaraHundida = Brush.verticalGradient(
    0f to Color(0xFFD8C7A4),
    1f to Color(0xFFCDBB96),
)
val TeclaCereza = Brush.verticalGradient(
    0f to Color(0xFFC2323A),
    0.5f to Color(0xFFB8282F),
    1f to Color(0xFF9A1F25),
)

// Piezas de la rockola
val Cromo = Brush.verticalGradient(
    0f to Color(0xFFFAFAF8),
    0.42f to Color(0xFFC9CDD3),
    0.54f to Color(0xFF8F959D),
    1f to Color(0xFFE7E9EC),
)
val Luz = Brush.verticalGradient(
    0f to Color(0xFFFFD98A),
    0.38f to Color(0xFFF5A524),
    0.72f to Color(0xFFE2672B),
    1f to Color(0xFFB8282F),
)
val LuzHorizontal = Brush.horizontalGradient(
    0f to Color(0xFFFFD98A),
    0.45f to Color(0xFFF5A524),
    0.80f to Color(0xFFE2672B),
    1f to Color(0xFFB8282F),
)
val Apagado = Brush.verticalGradient(
    0f to Color(0xFF5A4A42),
    0.60f to Color(0xFF44352E),
    1f to Color(0xFF3A2C26),
)
val ApagadoHorizontal = Brush.horizontalGradient(
    0f to Color(0xFF5A4A42),
    0.60f to Color(0xFF44352E),
    1f to Color(0xFF3A2C26),
)

/** .pie: madera clara → madera 40 % → #2E1A12. */
val Pie = Brush.verticalGradient(
    0f to MaderaClara,
    0.40f to Madera,
    1f to PieFondo,
)

/*
 * Degradados radiales de estilos.css. Compose solo dibuja círculos, así que la elipse de CSS
 * se logra escalando el círculo en vertical. Centro y radios van en fracción del tamaño;
 * sin radios es `circle` hasta la esquina más lejana (el valor por defecto de CSS).
 * Lo que en CSS se desvanece a «transparent» se desvanece aquí al mismo color con alfa 0,
 * como hace el navegador, para que no aparezca un borde oscuro.
 */
private class Radial(
    private val centroX: Float,
    private val centroY: Float,
    private val radioX: Float? = null,
    private val radioY: Float? = null,
    private val paradas: List<Pair<Float, Color>>,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val cx = size.width * centroX
        val cy = size.height * centroY
        val rx: Float
        val ry: Float
        if (radioX == null || radioY == null) {
            val esquinas = listOf(0f to 0f, size.width to 0f, 0f to size.height, size.width to size.height)
            val r = esquinas.maxOf { (x, y) -> hypot(x - cx, y - cy) }
            rx = r; ry = r
        } else {
            rx = size.width * radioX
            ry = size.height * radioY
        }
        val radio = rx.coerceAtLeast(1f)
        return RadialGradientShader(
            center = Offset(cx, cy),
            radius = radio,
            colors = paradas.map { it.second },
            colorStops = paradas.map { it.first },
        ).apply {
            setLocalMatrix(Matrix().apply { setScale(1f, ry.coerceAtLeast(1f) / radio, cx, cy) })
        }
    }
}

/** .tecla--tuya: radial-gradient(120% 140% at 50% 0%, ámbar claro, ámbar 70 %). */
val TeclaTuya: Brush = Radial(0.5f, 0f, 1.2f, 1.4f, listOf(0f to AmbarClaro, 0.7f to Ambar))

/** .ficha--libre: radial-gradient(circle at 35% 30%, #FFF0C4, ámbar 55 %, #B8741A). */
val FichaLibre: Brush = Radial(0.35f, 0.30f, paradas = listOf(0f to FichaBrillo, 0.55f to Ambar, 1f to FichaCanto))

/** .aviso: radial-gradient(120% 160% at 30% 0%, ámbar claro, ámbar 70 %). */
val AvisoLuz: Brush = Radial(0.30f, 0f, 1.2f, 1.6f, listOf(0f to AmbarClaro, 0.7f to Ambar))

/** .dialogo: madera clara → madera. */
val FondoDialogo = Brush.verticalGradient(0f to MaderaClara, 1f to Madera)

/** .madera--apagada: sin el resplandor del arco (cerrado o en pausa). */
val FondoMaderaApagada: List<Brush> get() = FondoMadera.take(2)

/** .luz-estado encendida: radial-gradient(circle at 35% 35%, #FFE7B0, ámbar 60 %). */
val LuzEstado: Brush = Radial(0.35f, 0.35f, paradas = listOf(0f to LuzBrillo, 0.6f to Ambar))

/*
 * Fondo de madera (body en estilos.css), de arriba hacia abajo en orden de pintura:
 * el resplandor ámbar arriba, la sombra abajo y el nogal. La veta va encima (vetaMadera).
 */
val FondoMadera: List<Brush> = listOf(
    SolidColor(Nogal),
    Radial(0.5f, 1.10f, 0.90f, 0.60f, listOf(0f to Color(0xFF000000).copy(alpha = 0.45f), 0.70f to Color(0xFF000000).copy(alpha = 0f))),
    Radial(0.5f, -0.05f, 1.30f, 0.55f, listOf(0f to Ambar.copy(alpha = 0.16f), 0.60f to Ambar.copy(alpha = 0f))),
)

/** body::before: tres vetas finas de anchos irregulares para que no parezca rayado. */
fun vetaMadera(densidad: Density): List<Brush> = listOf(
    franja(densidad, Color(0xFF000000).copy(alpha = 0.05f), grueso = 1f, periodo = 7f),
    franja(densidad, Color(red = 255, green = 220, blue = 170).copy(alpha = 0.025f), grueso = 2f, periodo = 13f),
    franja(densidad, Color(0xFF000000).copy(alpha = 0.04f), grueso = 3f, periodo = 29f),
)

private fun franja(densidad: Density, color: Color, grueso: Float, periodo: Float): Brush {
    val ancho = with(densidad) { periodo.dp.toPx() }
    val sinColor = color.copy(alpha = 0f)
    return Brush.horizontalGradient(
        0f to color,
        grueso / periodo to color,
        grueso / periodo to sinColor,
        1f to sinColor,
        startX = 0f,
        endX = ancho,
        tileMode = TileMode.Repeated,
    )
}

/*
 * --tubos y --tubos-apagados: un patrón de 21px que se repite de izquierda a derecha y se
 * pinta encima de Luz o Apagado. Lleva cortes secos, así que cada cambio repite la posición.
 * Necesita la densidad para pasar los 21dp a píxeles.
 */
private const val PATRON = 21f
private val Transparente = Color(0xFFFFFFFF).copy(alpha = 0f)

private fun patronTubos(densidad: Density, brillo: Color, sombra: Color): Brush {
    val ancho = with(densidad) { PATRON.dp.toPx() }
    return Brush.horizontalGradient(
        0f to brillo,
        5f / PATRON to brillo,
        5f / PATRON to Transparente,
        9f / PATRON to Transparente,
        9f / PATRON to sombra,
        10f / PATRON to sombra,
        10f / PATRON to Transparente,
        1f to Transparente,
        startX = 0f,
        endX = ancho,
        tileMode = TileMode.Repeated,
    )
}

fun tubos(densidad: Density): Brush = patronTubos(
    densidad,
    brillo = Color(0xFFFFFFFF).copy(alpha = 0.22f),
    sombra = Color(red = 120, green = 20, blue = 10).copy(alpha = 0.16f),
)

fun tubosApagados(densidad: Density): Brush = patronTubos(
    densidad,
    brillo = Color(0xFFFFFFFF).copy(alpha = 0.06f),
    sombra = Color(0xFF000000).copy(alpha = 0.14f),
)

/**
 * `filter: sepia(.12) saturate(.92)` de la miniatura (componentes.css): la foto se entona con
 * el papel de la tarjeta. Las mismas matrices que la especificación de filtros de CSS, en el
 * mismo orden (primero sepia, después saturación).
 */
val FiltroFoto: androidx.compose.ui.graphics.ColorFilter = run {
    val a = 1f - 0.12f // sepia
    val sepia = floatArrayOf(
        0.393f + 0.607f * a, 0.769f - 0.769f * a, 0.189f - 0.189f * a,
        0.349f - 0.349f * a, 0.686f + 0.314f * a, 0.168f - 0.168f * a,
        0.272f - 0.272f * a, 0.534f - 0.534f * a, 0.131f + 0.869f * a,
    )
    val s = 0.92f // saturate
    val saturacion = floatArrayOf(
        0.213f + 0.787f * s, 0.715f - 0.715f * s, 0.072f - 0.072f * s,
        0.213f - 0.213f * s, 0.715f + 0.285f * s, 0.072f - 0.072f * s,
        0.213f - 0.213f * s, 0.715f - 0.715f * s, 0.072f + 0.928f * s,
    )
    val m = FloatArray(9) { i -> (0 until 3).sumOf { k -> (saturacion[i / 3 * 3 + k] * sepia[k * 3 + i % 3]).toDouble() }.toFloat() }
    androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix(
            floatArrayOf(
                m[0], m[1], m[2], 0f, 0f,
                m[3], m[4], m[5], 0f, 0f,
                m[6], m[7], m[8], 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}
