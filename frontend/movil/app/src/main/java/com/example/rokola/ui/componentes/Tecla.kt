package com.example.rokola.ui.componentes

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rokola.ui.theme.AmbarBase
import com.example.rokola.ui.theme.CerezaHondo
import com.example.rokola.ui.theme.CremaBase
import com.example.rokola.ui.theme.LocalDiseno
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
import com.example.rokola.ui.theme.TintaBase
import com.example.rokola.ui.theme.TonosAfiche
import com.example.rokola.ui.theme.TonosVidrio

/*
 * Teclas de packages/ui/src/componentes.css (.tecla y sus variantes), con las mismas sombras.
 * En Vidrio son píldoras y en Afiche gestos de imprenta (temas/vidrio.css y temas/afiche.css).
 */

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
        cara = TeclaCara, texto = TintaBase, canto = TeclaCanto, cantoAlto = 4.dp,
        sombra = Shadow(radius = 10.dp, color = Negro.copy(alpha = 0.45f), offset = DpOffset(0.dp, 7.dp)),
        brilloInterior = Shadow(radius = 0.dp, color = Blanco.copy(alpha = 0.8f), offset = DpOffset(0.dp, 1.dp)),
        borde = Negro.copy(alpha = 0.35f), alzada = (-2).dp,
    )
    VarianteTecla.CEREZA -> Aspecto(
        cara = TeclaCereza, texto = CremaBase, canto = CerezaHondo, cantoAlto = 4.dp,
        sombra = Shadow(radius = 12.dp, color = Negro.copy(alpha = 0.5f), offset = DpOffset(0.dp, 7.dp)),
        brilloInterior = Shadow(radius = 0.dp, color = Blanco.copy(alpha = 0.3f), offset = DpOffset(0.dp, 1.dp)),
        borde = Negro.copy(alpha = 0.45f), alzada = (-2).dp,
    )
    VarianteTecla.HUNDIDA -> Aspecto(
        cara = TeclaCaraHundida, texto = TintaBase, canto = TeclaCanto, cantoAlto = 1.dp,
        sombra = null,
        brilloInterior = Shadow(radius = 6.dp, color = Negro.copy(alpha = 0.3f), offset = DpOffset(0.dp, 3.dp)),
        borde = Negro.copy(alpha = 0.35f), alzada = 1.dp,
    )
    VarianteTecla.TUYA -> Aspecto(
        cara = TeclaTuya, texto = TintaBase, canto = null, cantoAlto = 0.dp,
        sombra = Shadow(radius = 16.dp, color = AmbarBase.copy(alpha = 0.55f)),
        brilloInterior = Shadow(radius = 0.dp, color = Blanco.copy(alpha = 0.6f), offset = DpOffset(0.dp, 1.dp)),
        borde = Negro.copy(alpha = 0.35f), alzada = 0.dp,
    )
}

@Composable
private fun estilo(variante: VarianteTecla, tamano: TamanoTecla): TextStyle = when {
    tamano == TamanoTecla.GRANDE -> TeclaGrande
    tamano == TamanoTecla.CHICA -> TeclaChica
    variante == VarianteTecla.HUNDIDA || variante == VarianteTecla.TUYA -> TeclaHundida
    else -> EstiloTecla
}

private fun Modifier.medidas(tamano: TamanoTecla, anchoNormal: Dp = 76.dp): Modifier = when (tamano) {
    TamanoTecla.NORMAL -> widthIn(min = anchoNormal).heightIn(min = 48.dp)
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
    val d = LocalDiseno.current
    if (d.esVidrio) return TeclaVidrio(texto, variante, tamano, pulsada, modifier, icono)
    if (d.esAfiche) return TeclaAfiche(texto, variante, tamano, pulsada, modifier, icono)
    val a = aspecto(variante)
    val sePulsa = pulsada && (variante == VarianteTecla.NORMAL || variante == VarianteTecla.CEREZA)
    val alzada by animateDpAsState(if (sePulsa) 1.dp else a.alzada, tween(Movimiento.PULSAR), label = "alzada")
    val canto by animateDpAsState(if (sePulsa) 1.dp else a.cantoAlto, tween(Movimiento.PULSAR), label = "canto")
    val densidad = LocalDensity.current
    val radio = RadioTecla
    var m = modifier.graphicsLayer { translationY = with(densidad) { alzada.toPx() } }
    if (a.sombra != null) {
        val s = if (sePulsa) Shadow(radius = 4.dp, color = Negro.copy(alpha = 0.4f), offset = DpOffset(0.dp, 2.dp)) else a.sombra
        m = m.dropShadow(radio, s)
    }
    if (a.canto != null) m = m.dropShadow(radio, Shadow(radius = 0.dp, color = a.canto, offset = DpOffset(0.dp, canto)))
    m = m.medidas(tamano)
        .background(a.cara, radio)
        .innerShadow(radio, a.brilloInterior)
        .border(1.dp, a.borde, radio)
        .padding(horizontal = if (tamano == TamanoTecla.CHICA) 14.dp else 4.dp, vertical = 4.dp)
    Row(m, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        if (icono != null) Icono(icono, tamano = 20.dp, color = a.texto)
        Text(texto.uppercase(), style = estilo(variante, tamano), color = a.texto, textAlign = TextAlign.Center)
    }
}

/** Borde punteado: «Tuya» en Vidrio. */
private fun Modifier.bordePunteado(color: Color): Modifier = drawBehind {
    val g = 1.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(g / 2, g / 2),
        size = Size(size.width - g, size.height - g),
        cornerRadius = CornerRadius(size.height / 2),
        style = Stroke(width = g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
    )
}

/** Píldoras: «+1» de vidrio, «Votaste» lavanda, «Tuya» punteada y la acción con el degradado. */
@Composable
private fun TeclaVidrio(texto: String, variante: VarianteTecla, tamano: TamanoTecla, pulsada: Boolean, modifier: Modifier, icono: NombreIcono?) {
    val escala by animateFloatAsState(if (pulsada) 0.97f else 1f, tween(Movimiento.PULSAR), label = "escala")
    val radio = RadioTecla
    val colorTexto = if (variante == VarianteTecla.CEREZA) TonosVidrio.TintaVidrio else TonosVidrio.Blanco
    val forma = when (variante) {
        VarianteTecla.NORMAL -> Modifier.background(TonosVidrio.TeclaFondo, radio).border(1.dp, TonosVidrio.TeclaBorde, radio)
        VarianteTecla.HUNDIDA -> Modifier.background(TonosVidrio.Votado, radio).border(1.dp, TonosVidrio.VotadoBorde, radio)
        VarianteTecla.TUYA -> Modifier.bordePunteado(TonosVidrio.TuyaBorde)
        VarianteTecla.CEREZA -> Modifier
            .dropShadow(radio, Shadow(radius = 18.dp, color = TonosVidrio.SombraAcento, offset = DpOffset(0.dp, 8.dp)))
            .background(TonosVidrio.Acento, radio)
    }
    Row(
        modifier
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .medidas(tamano, anchoNormal = 84.dp)
            .then(forma)
            .padding(horizontal = if (tamano == TamanoTecla.CHICA) 16.dp else 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icono != null) Icono(icono, tamano = 20.dp, color = colorTexto, grosor = 1.8f)
        Text(texto, style = estilo(variante, tamano), color = colorTexto, textAlign = TextAlign.Center)
    }
}

/** Imprenta: casilla («+1»), sello torcido («Votaste»), resaltador («Tuya»), bloque de tinta. */
@Composable
private fun TeclaAfiche(texto: String, variante: VarianteTecla, tamano: TamanoTecla, pulsada: Boolean, modifier: Modifier, icono: NombreIcono?) {
    val radio = RadioTecla
    val tinta = TonosAfiche.TintaAfiche
    val colorTexto = when (variante) {
        VarianteTecla.CEREZA -> TonosAfiche.Amarillo
        VarianteTecla.HUNDIDA -> TonosAfiche.Sello
        else -> tinta
    }
    val forma = when (variante) {
        VarianteTecla.NORMAL -> Modifier
            .background(if (pulsada) SolidColor(tinta.copy(alpha = 0.07f)) else SolidColor(Color.Transparent), radio)
            .border(2.5.dp, tinta, radio)
        VarianteTecla.HUNDIDA -> Modifier
            .graphicsLayer { rotationZ = -4f; translationY = if (pulsada) 1.dp.toPx() else 0f }
            .border(2.dp, TonosAfiche.Sello, radio)
            .padding(4.dp)
            .border(1.dp, TonosAfiche.Sello, radio)
        VarianteTecla.TUYA -> Modifier.background(TonosAfiche.Amarillo, radio)
        VarianteTecla.CEREZA -> Modifier.background(if (pulsada) tinta.copy(alpha = 0.9f) else tinta, radio)
    }
    val base = estilo(variante, tamano)
    val letra = if (tamano == TamanoTecla.NORMAL && (variante == VarianteTecla.CEREZA || variante == VarianteTecla.TUYA)) base.copy(fontSize = 24.sp) else base
    Row(
        modifier
            .medidas(tamano, anchoNormal = 78.dp)
            .then(forma)
            .padding(horizontal = if (tamano == TamanoTecla.CHICA) 14.dp else 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icono != null) Icono(icono, tamano = 20.dp, color = colorTexto, grosor = 2.4f)
        Text(texto.uppercase(), style = letra, color = colorTexto, textAlign = TextAlign.Center)
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
