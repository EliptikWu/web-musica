package com.example.rokola.ui.componentes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.rokola.ui.theme.Ambar
import com.example.rokola.ui.theme.Apagado
import com.example.rokola.ui.theme.BrilloAmbar
import com.example.rokola.ui.theme.BrilloAmbarMedidas
import com.example.rokola.ui.theme.Cereza
import com.example.rokola.ui.theme.Cifra
import com.example.rokola.ui.theme.Condensada
import com.example.rokola.ui.theme.Crema
import com.example.rokola.ui.theme.Crema2
import com.example.rokola.ui.theme.Cromo
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.FichaLibre
import com.example.rokola.ui.theme.GrisApagado
import com.example.rokola.ui.theme.Hueco
import com.example.rokola.ui.theme.Luz
import com.example.rokola.ui.theme.LuzEstado
import com.example.rokola.ui.theme.Papel
import com.example.rokola.ui.theme.PapelApagado
import com.example.rokola.ui.theme.RadioArco
import com.example.rokola.ui.theme.RadioArcoLuz
import com.example.rokola.ui.theme.RadioCaja
import com.example.rokola.ui.theme.RadioDia
import com.example.rokola.ui.theme.RadioFoto
import com.example.rokola.ui.theme.FiltroFoto
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.example.rokola.ui.theme.RadioInsignia
import com.example.rokola.ui.theme.RadioMarco
import com.example.rokola.ui.theme.RadioTarjeta
import com.example.rokola.ui.theme.RadioTecla
import com.example.rokola.ui.theme.TeclaCanto
import com.example.rokola.ui.theme.TeclaCara
import com.example.rokola.ui.theme.Tinta
import com.example.rokola.ui.theme.Tinta2
import com.example.rokola.ui.theme.TituloSeccion
import com.example.rokola.ui.theme.tubos
import com.example.rokola.ui.theme.tubosApagados

/* Piezas de la rockola (packages/ui/src/Piezas.tsx, TarjetaTitulo.tsx, Avance.tsx). */

private val Negro = Color(0xFF000000)
private val Blanco = Color(0xFFFFFFFF)
/** rgba(35, 20, 15, .3): el corte punteado entre columnas de la tarjeta. */
private val Corte = Tinta.copy(alpha = 0.3f)

/** Raya punteada vertical (border-left/right: 1px dashed). */
private fun Modifier.corte(izquierda: Boolean): Modifier = drawBehind {
    val x = if (izquierda) 0.5f else size.width - 0.5f
    drawLine(
        color = Corte,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
    )
}

/** Título de sección (.titulo-seccion): 112 %, 600, 16. */
@Composable
fun TituloDeSeccion(texto: String, modifier: Modifier = Modifier, color: Color = Crema, alineacion: TextAlign = TextAlign.Start) {
    Text(texto, modifier.semantics { heading() }, style = TituloSeccion, color = color, textAlign = alineacion)
}

// ── Tarjeta de título ─────────────────────────────────────────────────────────

enum class VarianteTarjeta { NORMAL, TUYA, APAGADA }

/**
 * Tarjeta de título: la tira de papel que identificaba cada disco (doc 05). Papel crema,
 * título condensado con su grafía real y franja cereza con el artista; ámbar si es tuya,
 * apagada si no se puede pedir.
 *
 * Con [miniatura] (la de YouTube), la primera columna es una foto pegada en la tira y el
 * puesto va encima como etiqueta (rework del doc 05). Es decorativa: el título y el artista
 * ya dicen qué canción es, así que TalkBack no la lee.
 */
@Composable
fun TarjetaTitulo(
    titulo: String,
    artista: String,
    modifier: Modifier = Modifier,
    variante: VarianteTarjeta = VarianteTarjeta.NORMAL,
    puesto: Int? = null,
    votos: Int? = null,
    duracion: String? = null,
    miniatura: String? = null,
) {
    val fondo = if (variante == VarianteTarjeta.APAGADA) PapelApagado else Papel
    val franja = when (variante) {
        VarianteTarjeta.NORMAL -> Cereza
        VarianteTarjeta.TUYA -> Ambar
        VarianteTarjeta.APAGADA -> GrisApagado
    }
    val textoFranja = if (variante == VarianteTarjeta.TUYA) Tinta else Crema
    val leido = buildList {
        puesto?.let { add("Puesto $it") }
        add(titulo)
        add(artista)
        votos?.let { add(if (it == 1) "1 voto" else "$it votos") }
        duracion?.let { add("Duración $it") }
    }.joinToString(", ")
    Row(
        modifier
            .dropShadow(RadioTarjeta, Shadow(radius = 0.dp, color = Negro.copy(alpha = 0.3f), offset = DpOffset(0.dp, 2.dp)))
            .background(fondo, RadioTarjeta)
            .innerShadow(RadioTarjeta, Shadow(radius = 0.dp, color = Negro.copy(alpha = 0.14f), spread = 1.dp))
            .heightIn(min = 56.dp)
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .clearAndSetSemantics { contentDescription = leido },
    ) {
        if (miniatura != null) {
            Miniatura(miniatura, puesto)
        } else if (puesto != null) {
            Box(Modifier.widthIn(min = 34.dp).fillMaxHeight().corte(izquierda = false), contentAlignment = Alignment.Center) {
                Text("$puesto", style = Cifra.copy(fontSize = 18.sp), color = Tinta2)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 6.dp), verticalArrangement = Arrangement.Center) {
            Text(
                titulo,
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
                style = Condensada.copy(fontSize = 19.sp, lineHeight = 1.12.em),
                color = Tinta,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                artista,
                Modifier.fillMaxWidth().background(franja).padding(start = 6.dp, end = 6.dp, top = 3.dp, bottom = 4.dp),
                style = Cuerpo.copy(fontSize = 13.sp, fontWeight = FontWeight.W500, lineHeight = 1.25.em),
                color = textoFranja,
                textAlign = TextAlign.Center,
            )
        }
        if (votos != null) {
            Column(
                Modifier.widthIn(min = 46.dp).padding(horizontal = 2.dp).fillMaxHeight().corte(izquierda = true),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("$votos", style = Cifra.copy(fontSize = 20.sp), color = Tinta)
                Text(
                    if (votos == 1) "voto" else "votos",
                    Modifier.padding(top = 2.dp),
                    style = Cuerpo.copy(fontSize = 11.sp, fontWeight = FontWeight.W500, lineHeight = 1.2.em),
                    color = Tinta2,
                )
            }
        }
        if (duracion != null) {
            Box(Modifier.widthIn(min = 62.dp).padding(horizontal = 4.dp).fillMaxHeight().corte(izquierda = true), contentAlignment = Alignment.Center) {
                Text(duracion, style = Cuerpo.copy(fontSize = 14.sp, fontWeight = FontWeight.W600, fontFeatureSettings = "tnum"), color = Tinta)
            }
        }
    }
}

/** .tarjeta__miniatura: 54 dp, la foto a 4 dp del borde y el puesto en la esquina de abajo. */
@Composable
private fun Miniatura(url: String, puesto: Int?) {
    Box(Modifier.width(54.dp).fillMaxHeight().corte(izquierda = false)) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .padding(4.dp)
                .clip(RadioFoto)
                .border(1.dp, Negro.copy(alpha = 0.2f), RadioFoto),
            contentScale = ContentScale.Crop,
            colorFilter = FiltroFoto,
        )
        if (puesto != null) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 2.dp, bottom = 2.dp)
                    .dropShadow(RadioFoto, Shadow(radius = 0.dp, color = Negro.copy(alpha = 0.35f), offset = DpOffset(0.dp, 1.dp)))
                    .background(Papel, RadioFoto)
                    .widthIn(min = 22.dp)
                    .heightIn(min = 22.dp)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("$puesto", style = Cifra.copy(fontSize = 16.sp), color = Tinta)
            }
        }
    }
}

// ── Arco, ventana y marco ─────────────────────────────────────────────────────

/** La silueta del arco para sus sombras: tapa redonda arriba, 22 dp abajo. */
private val SombraArco = RoundedCornerShape(
    topStart = androidx.compose.foundation.shape.CornerSize(50),
    topEnd = androidx.compose.foundation.shape.CornerSize(50),
    bottomEnd = androidx.compose.foundation.shape.CornerSize(22.dp),
    bottomStart = androidx.compose.foundation.shape.CornerSize(22.dp),
)

/**
 * Arco de luz: marco de cromo y tubos; encendido se reciben canciones, apagado no.
 * [altoMinimo] y [relleno] vienen del prototipo (292 px con 96 arriba en la bienvenida).
 */
@Composable
fun Arco(
    modifier: Modifier = Modifier,
    apagado: Boolean = false,
    altoMinimo: Dp = 292.dp,
    relleno: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 96.dp, bottom = 20.dp),
    descripcion: String? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val densidad = LocalDensity.current
    var marco = modifier
    // En Compose 1.10, dropShadow con spread negativo pinta un rectángulo de bordes duros:
    // las sombras de CSS con extensión negativa (0 20px 40px -20px) se aproximan sin ella.
    // La silueta redondeada evita además el contorno libre de la tapa elíptica.
    if (!apagado) {
        marco = marco.dropShadow(SombraArco, Shadow(radius = BrilloAmbarMedidas.desenfoque + BrilloAmbarMedidas.extension, color = BrilloAmbar))
    }
    marco = marco.dropShadow(SombraArco, Shadow(radius = 16.dp, color = Negro.copy(alpha = 0.55f), offset = DpOffset(0.dp, 14.dp)))
    Box(
        marco
            .background(Cromo, RadioArco)
            .padding(4.dp)
            .then(if (descripcion != null) Modifier.semantics { contentDescription = descripcion } else Modifier),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = altoMinimo)
                .background(if (apagado) Apagado else Luz, RadioArcoLuz)
                .background(if (apagado) tubosApagados(densidad) else tubos(densidad), RadioArcoLuz)
                .innerShadow(RadioArcoLuz, Shadow(radius = 26.dp, color = Color(red = 80, green = 10, blue = 0).copy(alpha = 0.35f)))
                .padding(relleno),
            verticalArrangement = Arrangement.Bottom,
            content = contenido,
        )
    }
}

/** Ventana de papel dentro del arco o de un marco. */
@Composable
fun Ventana(
    modifier: Modifier = Modifier,
    relleno: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(16.dp),
    alineacion: Alignment.Horizontal = Alignment.CenterHorizontally,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .dropShadow(RadioTecla, Shadow(radius = 0.dp, color = Negro.copy(alpha = 0.18f), offset = DpOffset(0.dp, 2.dp)))
            .background(Papel, RadioTecla)
            .innerShadow(RadioTecla, Shadow(radius = 0.dp, color = Negro.copy(alpha = 0.12f), spread = 1.dp))
            .padding(relleno),
        horizontalAlignment = alineacion,
        content = contenido,
    )
}

/** Marco pequeño con luz (la tarjeta «Tu canción»). */
@Composable
fun Marco(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val densidad = LocalDensity.current
    Box(
        modifier
            .dropShadow(RadioMarco, Shadow(radius = 40.dp, color = Ambar.copy(alpha = 0.45f)))
            .background(Cromo, RadioMarco)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Luz, RadioCaja)
                .background(tubos(densidad), RadioCaja)
                .padding(9.dp),
        ) { contenido() }
    }
}

// ── Luz, fichas, insignia, días ───────────────────────────────────────────────

/** Luz de estado: punto ámbar encendido, gris apagado. El texto dice el estado. */
@Composable
fun LuzDeEstado(texto: String, encendida: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val punto = if (encendida) {
            Modifier.dropShadow(CircleShape, Shadow(radius = 10.dp, color = Ambar.copy(alpha = 0.7f), spread = 2.dp)).background(LuzEstado, CircleShape)
        } else {
            Modifier.background(GrisApagado, CircleShape)
        }
        Box(Modifier.size(10.dp).then(punto))
        Text(texto, style = Cuerpo.copy(fontSize = 14.sp, fontWeight = FontWeight.W500), color = Crema)
    }
}

/**
 * Fichas: los pedidos que le quedan al teléfono. Son decorativas: siempre van con el texto
 * «Te quedan N de M pedidos» al lado (doc 05).
 */
@Composable
fun Fichas(libres: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val ficha = if (i < libres) {
                Modifier
                    .dropShadow(CircleShape, Shadow(radius = 8.dp, color = Ambar.copy(alpha = 0.5f)))
                    .background(FichaLibre, CircleShape)
                    .innerShadow(CircleShape, Shadow(radius = 0.dp, color = Color(red = 120, green = 70, blue = 10).copy(alpha = 0.35f), spread = 2.dp))
            } else {
                Modifier.drawBehind {
                    val grosor = 1.5.dp.toPx()
                    drawCircle(
                        color = Crema.copy(alpha = 0.45f),
                        radius = size.minDimension / 2 - grosor / 2,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = grosor,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx())),
                        ),
                    )
                }
            }
            Box(Modifier.size(18.dp).then(ficha))
        }
    }
}

/** Insignia cereza: «24 votos», «Subió 2 puestos». */
@Composable
fun Insignia(texto: String, modifier: Modifier = Modifier, icono: NombreIcono? = null) {
    Row(
        modifier
            .background(Cereza, RadioInsignia)
            .padding(start = if (icono != null) 7.dp else 10.dp, end = 10.dp, top = 2.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icono != null) Icono(icono, tamano = 15.dp, color = Crema, grosor = 2.4f)
        Text(texto, style = Cuerpo.copy(fontSize = 13.sp, fontWeight = FontWeight.W600, lineHeight = 1.35.em), color = Crema)
    }
}

data class Dia(val corto: String, val largo: String, val abre: Boolean, val hoy: Boolean)

/** Los siete días del horario: los que abren son teclas; «Hoy» marca el día actual. */
@Composable
fun DiasSemana(dias: List<Dia>, modifier: Modifier = Modifier) {
    val densidad = LocalDensity.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        dias.forEach { dia ->
            val forma = if (dia.abre) {
                Modifier
                    .graphicsLayer { translationY = with(densidad) { (-2).dp.toPx() } }
                    .dropShadow(RadioDia, Shadow(radius = 0.dp, color = TeclaCanto, offset = DpOffset(0.dp, 3.dp)))
                    .background(TeclaCara, RadioDia)
                    .innerShadow(RadioDia, Shadow(radius = 0.dp, color = Blanco.copy(alpha = 0.8f), offset = DpOffset(0.dp, 1.dp)))
            } else {
                Modifier
                    .background(Hueco, RadioDia)
                    .innerShadow(RadioDia, Shadow(radius = 3.dp, color = Negro.copy(alpha = 0.4f), offset = DpOffset(0.dp, 1.dp)))
            }
            Column(
                Modifier
                    .weight(1f)
                    .height(50.dp)
                    .then(forma)
                    .clearAndSetSemantics { contentDescription = "${dia.largo}: ${if (dia.abre) "abre" else "cerrado"}${if (dia.hoy) ", hoy" else ""}" },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(dia.corto, style = Cuerpo.copy(fontSize = 14.sp, fontWeight = FontWeight.W600, lineHeight = 1.2.em), color = if (dia.abre) Tinta else Crema2)
                if (dia.hoy) {
                    Text("Hoy", style = Cuerpo.copy(fontSize = 11.sp, fontWeight = FontWeight.W600, lineHeight = 1.2.em), color = if (dia.abre) Cereza else Ambar)
                }
            }
        }
    }
}

// ── Avance ────────────────────────────────────────────────────────────────────

/** Barra de avance de una canción, con el tiempo a cada lado. */
@Composable
fun Avance(
    fraccion: Float,
    textoInicio: String,
    textoFin: String,
    modifier: Modifier = Modifier,
    colorTexto: Color = Tinta2,
    colorSurco: Color = Tinta,
) {
    val f = fraccion.coerceIn(0f, 1f)
    val estiloTiempo = Cuerpo.copy(fontSize = 13.sp, fontFeatureSettings = "tnum")
    Row(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "Avance de la canción"
                progressBarRangeInfo = ProgressBarRangeInfo(f, 0f..1f)
                stateDescription = "$textoInicio de $textoFin"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(textoInicio, style = estiloTiempo, color = colorTexto)
        Box(Modifier.weight(1f).height(4.dp).background(Tinta.copy(alpha = 0.25f), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(f).height(4.dp).background(colorSurco, RoundedCornerShape(2.dp)))
        }
        Text(textoFin, style = estiloTiempo, color = colorTexto)
    }
}

/** La raya ámbar bajo el mini reproductor: decorativa, el avance ya se lee arriba. */
@Composable
fun MiniAvance(fraccion: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(3.dp).background(Crema.copy(alpha = 0.16f), RoundedCornerShape(2.dp))) {
        Box(Modifier.fillMaxWidth(fraccion.coerceIn(0f, 1f)).height(3.dp).background(Ambar, RoundedCornerShape(2.dp)))
    }
}

// ── Esperar y estar vacío ─────────────────────────────────────────────────────

/** Mientras llega algo: la forma de lo que vendrá, sin texto inventado. */
@Composable
fun Esqueleto(modifier: Modifier = Modifier, forma: androidx.compose.ui.graphics.Shape = RadioTarjeta) {
    val t = rememberInfiniteTransition(label = "esqueleto")
    val x = t.animateFloat(1f, -1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "x")
    Box(
        modifier
            .clearAndSetSemantics { }
            .clip(forma)
            .drawBehind {
                val inicio = x.value * size.width
                drawRect(
                    Brush.horizontalGradient(
                        0f to Crema.copy(alpha = 0.06f),
                        0.5f to Crema.copy(alpha = 0.12f),
                        1f to Crema.copy(alpha = 0.06f),
                        startX = inicio,
                        endX = inicio + size.width * 2,
                        tileMode = androidx.compose.ui.graphics.TileMode.Mirror,
                    ),
                )
            },
    )
}
