package com.example.rokola.ui.componentes

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Iconos dibujados a mano de C:\web-musica\packages\ui\src\Icono.tsx: rejilla de 24, trazo
 * 1,9, puntas redondas. Solo los que el cliente usa. Los círculos del SVG van como dos arcos.
 */

private fun circulo(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

enum class NombreIcono(val trazo: String) {
    INICIO("M4.5 10.5 12 4.5l7.5 6V19a1 1 0 0 1-1 1H15v-5.5H9V20H5.5a1 1 0 0 1-1-1z"),
    BUSCAR(circulo(10.5f, 10.5f, 6.5f) + "M15.5 15.5l4.5 4.5"),
    COLA("M4.5 7h15M4.5 12h15M4.5 17h9"),
    BORRAR("M7 7l10 10M17 7 7 17"),
    SUBIR("M12 19V6M6.5 11.5 12 6l5.5 5.5"),
    SIN_CONEXION("M4 9.5a12 12 0 0 1 4-2.3M11 6.6c3.3-.3 6.6.8 9 3M7 13a7 7 0 0 1 3-1.6M14.5 11.8a7 7 0 0 1 2.5 1.4M10 16.5a3 3 0 0 1 4 0M4 4l16 16"),
}

private val cache = mutableMapOf<Pair<NombreIcono, Float>, ImageVector>()

private fun vector(nombre: NombreIcono, grosor: Float): ImageVector = cache.getOrPut(nombre to grosor) {
    ImageVector.Builder(name = nombre.name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = addPathNodes(nombre.trazo),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = grosor,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}

/**
 * Icono decorativo por defecto (el texto de al lado ya dice qué es). Con [descripcion], lo
 * anuncia TalkBack.
 */
@Composable
fun Icono(
    nombre: NombreIcono,
    modifier: Modifier = Modifier,
    tamano: Dp = 24.dp,
    color: Color = Color.Unspecified,
    grosor: Float = 1.9f,
    descripcion: String? = null,
) {
    Icon(
        imageVector = vector(nombre, grosor),
        contentDescription = descripcion,
        modifier = modifier.size(tamano),
        tint = color.takeOrElse(),
    )
}

@Composable
private fun Color.takeOrElse(): Color =
    if (this == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else this
