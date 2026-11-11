package com.example.rokola.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.rokola.ui.theme.Ambar
import com.example.rokola.ui.theme.AvisoLuz
import com.example.rokola.ui.theme.Borde
import com.example.rokola.ui.theme.Cereza
import com.example.rokola.ui.theme.Crema
import com.example.rokola.ui.theme.Crema2
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.MaderaClara
import com.example.rokola.ui.theme.RadioAviso
import com.example.rokola.ui.theme.Tinta

/* Avisos y vacíos (packages/ui/src/componentes.css: .aviso, .vacio). */

private val Negro = Color(0xFF000000)

enum class TipoAviso { LUZ, ERROR, INFO }

/** Un texto con su acción: «Ver cola», «Reintentar», «Cerrar». */
data class AccionAviso(val texto: String, val alPulsar: () -> Unit)

/**
 * Aviso dentro de una pantalla (doc 05): ámbar cuando algo salió bien y es tuyo, cereza cuando
 * falló, madera para información. TalkBack lo anuncia al aparecer.
 */
@Composable
fun Aviso(
    modifier: Modifier = Modifier,
    tipo: TipoAviso = TipoAviso.LUZ,
    titulo: String? = null,
    meta: String? = null,
    accion: AccionAviso? = null,
) {
    val texto = if (tipo == TipoAviso.LUZ) Tinta else Crema
    val forma = when (tipo) {
        TipoAviso.LUZ -> Modifier
            .dropShadow(RadioAviso, Shadow(radius = 26.dp, color = Ambar.copy(alpha = 0.4f)))
            .background(AvisoLuz, RadioAviso)
        TipoAviso.ERROR -> Modifier
            .dropShadow(RadioAviso, Shadow(radius = 10.dp, color = Negro.copy(alpha = 0.45f), offset = DpOffset(0.dp, 6.dp)))
            .background(Cereza, RadioAviso)
        TipoAviso.INFO -> Modifier
            .background(MaderaClara, RadioAviso)
            .innerShadow(RadioAviso, Shadow(radius = 0.dp, color = Borde, spread = 1.dp))
    }
    Row(
        modifier
            .fillMaxWidth()
            .then(forma)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
            .semantics { liveRegion = if (tipo == TipoAviso.ERROR) LiveRegionMode.Assertive else LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            if (titulo != null) Text(titulo, style = Cuerpo.copy(fontSize = 16.sp, fontWeight = FontWeight.W700), color = texto)
            if (meta != null) Text(meta, style = Cuerpo.copy(fontSize = 14.sp), color = texto)
        }
        if (accion != null) Enlace(accion.texto, accion.alPulsar, color = texto)
    }
}

/** Enlace subrayado con 48 dp de toque («Ver cola», «Política de datos»). */
@Composable
fun Enlace(texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier, color: Color = Crema) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = alPulsar)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            style = Cuerpo.copy(fontSize = 14.sp, fontWeight = FontWeight.W700, textDecoration = TextDecoration.Underline),
            color = color,
        )
    }
}

/** Estado vacío: caja punteada con el texto; [destacado] va en crema y negrita. */
@Composable
fun Vacio(texto: String, modifier: Modifier = Modifier, destacado: String? = null, abajo: (@Composable () -> Unit)? = null) {
    val contenido: AnnotatedString = buildAnnotatedString {
        if (destacado != null) {
            withStyle(SpanStyle(color = Crema, fontWeight = FontWeight.W700)) { append(destacado) }
            append(" ")
        }
        append(texto)
    }
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val grosor = 1.5.dp.toPx()
                drawRoundRect(
                    color = Crema.copy(alpha = 0.25f),
                    topLeft = androidx.compose.ui.geometry.Offset(grosor / 2, grosor / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - grosor, size.height - grosor),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = grosor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(contenido, style = Cuerpo.copy(fontSize = 14.sp, lineHeight = 1.45.em), color = Crema2, textAlign = TextAlign.Center)
        if (abajo != null) Box(Modifier.padding(top = 12.dp)) { abajo() }
    }
}
