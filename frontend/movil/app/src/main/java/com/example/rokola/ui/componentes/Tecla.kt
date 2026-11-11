package com.example.rokola.ui.componentes

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.rokola.ui.theme.Ambar
import com.example.rokola.ui.theme.Crema
import com.example.rokola.ui.theme.CerezaHondo
import com.example.rokola.ui.theme.Movimiento
import com.example.rokola.ui.theme.RadioTecla
import com.example.rokola.ui.theme.Tecla as EstiloTecla
import com.example.rokola.ui.theme.TeclaCanto
import com.example.rokola.ui.theme.TeclaCara
import com.example.rokola.ui.theme.TeclaCaraHundida
import com.example.rokola.ui.theme.TeclaCereza
import com.example.rokola.ui.theme.TeclaChica
import com.example.rokola.ui.theme.TeclaGrande
import com.example.rokola.ui.theme.TeclaHundida
import com.example.rokola.ui.theme.TeclaTuya
import com.example.rokola.ui.theme.Tinta

/* Teclas de packages/ui/src/componentes.css (.tecla y sus variantes), con las mismas sombras. */

enum class VarianteTecla { NORMAL, CEREZA, HUNDIDA, TUYA }

enum class TamanoTecla { NORMAL, GRANDE, CHICA }

private val Negro = Color(0xFF000000)
private val Blanco = Color(0xFFFFFFFF)

private class Aspecto(
    val cara: Brush,
    val texto: Color,
    /** Canto sólido bajo la tecla (0 4px 0); al pulsar baja a 1px. */
    val canto: Color?,
    val cantoAlto: Dp,
    /** Sombra difusa bajo la tecla. */
    val sombra: Shadow?,
    /** Brillo de arriba hacia adentro. */
    val brilloInterior: Shadow,
    val borde: Color,
    /** translateY en reposo. */
    val alzada: Dp,
)

private fun aspecto(variante: VarianteTecla): Aspecto = when (variante) {
    VarianteTecla.NORMAL -> Aspecto(
        cara = TeclaCara, texto = Tinta, canto = TeclaCanto, cantoAlto = 4.dp,
        sombra = Shadow(radius = 10.dp, color = Negro.copy(alpha = 0.45f), offset = DpOffset(0.dp, 7.dp)),
        brilloInterior = Shadow(radius = 0.dp, color = Blanco.copy(alpha = 0.8f), offset = DpOffset(0.dp, 1.dp)),
        borde = Negro.copy(alpha = 0.35f), alzada = (-2).dp,
    )
    VarianteTecla.CEREZA -> Aspecto(
        cara = TeclaCereza, texto = Crema, canto = CerezaHondo, cantoAlto = 4.dp,
        sombra = Shadow(radius = 12.dp, color = Negro.copy(alpha = 0.5f), offset = DpOffset(0.dp, 7.dp)),
        brilloInterior = Shadow(radius = 0.dp, color = Blanco.copy(alpha = 0.3f), offset = DpOffset(0.dp, 1.dp)),
        borde = Negro.copy(alpha = 0.45f), alzada = (-2).dp,
    )
    VarianteTecla.HUNDIDA -> Aspecto(
        cara = TeclaCaraHundida, texto = Tinta, canto = TeclaCanto, cantoAlto = 1.dp,
        sombra = null,
        brilloInterior = Shadow(radius = 6.dp, color = Negro.copy(alpha = 0.3f), offset = DpOffset(0.dp, 3.dp)),
        borde = Negro.copy(alpha = 0.35f), alzada = 1.dp,
    )
    VarianteTecla.TUYA -> Aspecto(
        cara = TeclaTuya, texto = Tinta, canto = null, cantoAlto = 0.dp,
        sombra = Shadow(radius = 16.dp, color = Ambar.copy(alpha = 0.55f)),
        brilloInterior = Shadow(radius = 0.dp, color = Blanco.copy(alpha = 0.6f), offset = DpOffset(0.dp, 1.dp)),
        borde = Negro.copy(alpha = 0.35f), alzada = 0.dp,
    )
}

private fun estilo(variante: VarianteTecla, tamano: TamanoTecla): TextStyle = when {
    tamano == TamanoTecla.GRANDE -> TeclaGrande
    tamano == TamanoTecla.CHICA -> TeclaChica
    variante == VarianteTecla.HUNDIDA || variante == VarianteTecla.TUYA -> TeclaHundida
    else -> EstiloTecla
}

private fun Modifier.medidas(tamano: TamanoTecla): Modifier = when (tamano) {
    TamanoTecla.NORMAL -> widthIn(min = 76.dp).heightIn(min = 48.dp)
    TamanoTecla.GRANDE -> fillMaxWidth().heightIn(min = 58.dp)
    TamanoTecla.CHICA -> heightIn(min = 44.dp)
}

@Composable
private fun CuerpoTecla(
    texto: String,
    variante: VarianteTecla,
    tamano: TamanoTecla,
    pulsada: Boolean,
    modifier: Modifier,
    icono: NombreIcono? = null,
) {
    val a = aspecto(variante)
    val sePulsa = pulsada && (variante == VarianteTecla.NORMAL || variante == VarianteTecla.CEREZA)
    val alzada by animateDpAsState(if (sePulsa) 1.dp else a.alzada, tween(Movimiento.PULSAR), label = "alzada")
    val canto by animateDpAsState(if (sePulsa) 1.dp else a.cantoAlto, tween(Movimiento.PULSAR), label = "canto")
    val densidad = LocalDensity.current
    var m = modifier.graphicsLayer { translationY = with(densidad) { alzada.toPx() } }
    if (a.sombra != null) {
        val s = if (sePulsa) Shadow(radius = 4.dp, color = Negro.copy(alpha = 0.4f), offset = DpOffset(0.dp, 2.dp)) else a.sombra
        m = m.dropShadow(RadioTecla, s)
    }
    if (a.canto != null) m = m.dropShadow(RadioTecla, Shadow(radius = 0.dp, color = a.canto, offset = DpOffset(0.dp, canto)))
    m = m.medidas(tamano)
        .background(a.cara, RadioTecla)
        .innerShadow(RadioTecla, a.brilloInterior)
        .border(1.dp, a.borde, RadioTecla)
        .padding(horizontal = if (tamano == TamanoTecla.CHICA) 14.dp else 4.dp, vertical = 4.dp)
    Row(m, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        if (icono != null) Icono(icono, tamano = 20.dp, color = a.texto)
        Text(texto.uppercase(), style = estilo(variante, tamano), color = a.texto, textAlign = TextAlign.Center)
    }
}

/**
 * Tecla que se pulsa. [etiqueta] es lo que dice TalkBack («Votar por DtMF») cuando el texto
 * visible solo no basta. Mientras está [ocupada] ignora los toques (RNF-008).
 */
@Composable
fun Tecla(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variante: VarianteTecla = VarianteTecla.NORMAL,
    tamano: TamanoTecla = TamanoTecla.NORMAL,
    etiqueta: String? = null,
    ocupada: Boolean = false,
    expandida: Boolean? = null,
    icono: NombreIcono? = null,
) {
    val interaccion = remember { MutableInteractionSource() }
    val pulsada by interaccion.collectIsPressedAsState()
    CuerpoTecla(
        texto = texto,
        variante = variante,
        tamano = tamano,
        pulsada = pulsada && !ocupada,
        icono = icono,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = etiqueta ?: texto
                if (ocupada) stateDescription = "Enviando"
                if (expandida != null) stateDescription = if (expandida) "Abierto" else "Cerrado"
                onClick { if (!ocupada) onClick(); true }
            }
            .clickable(interactionSource = interaccion, indication = null) { if (!ocupada) onClick() },
    )
}

/**
 * Tecla que no se pulsa: «Votaste» (hundida) y «Tuya» (ámbar). Es texto, no un botón
 * deshabilitado: dice el estado en vez de esconder la acción (doc 05).
 */
@Composable
fun TeclaFija(texto: String, variante: VarianteTecla, modifier: Modifier = Modifier, etiqueta: String? = null) {
    CuerpoTecla(
        texto = texto,
        variante = variante,
        tamano = TamanoTecla.NORMAL,
        pulsada = false,
        modifier = modifier.clearAndSetSemantics { contentDescription = etiqueta ?: texto },
    )
}
