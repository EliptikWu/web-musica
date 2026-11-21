package com.example.rokola.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.rokola.dominio.TemaBar
import com.example.rokola.ui.theme.Cifra
import com.example.rokola.ui.theme.Condensada
import com.example.rokola.ui.theme.Crema
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.Diseno
import com.example.rokola.ui.theme.LocalDiseno
import com.example.rokola.ui.theme.RadioDia
import com.example.rokola.ui.theme.RadioFoto
import com.example.rokola.ui.theme.RadioInsignia
import com.example.rokola.ui.theme.RadioMarco
import com.example.rokola.ui.theme.RadioTarjeta
import com.example.rokola.ui.theme.Tinta
import com.example.rokola.ui.theme.Tinta2
import com.example.rokola.ui.theme.TonosAfiche
import com.example.rokola.ui.theme.TonosVidrio

/*
 * Las formas propias de Vidrio y Afiche (packages/ui/src/temas/). El marcado de cada pantalla es
 * el mismo de Rockola: aquí cada pieza se vuelve su equivalente, como hacen los CSS de la web.
 */

/** Dentro del arco: la insignia va sin caja. */
internal val LocalEnArco = compositionLocalOf { false }
/** Dentro del arco o de «Tu canción»: la ventana ya no es una caja propia. */
internal val LocalVentanaPlana = compositionLocalOf { false }
private val LocalAlineacionArco = compositionLocalOf { TextAlign.Center }

/** Cómo se alinea el texto dentro del arco: centrado en Rockola, a la izquierda en Vidrio. */
@Composable
@ReadOnlyComposable
fun alineacionArco(): TextAlign = LocalAlineacionArco.current

/** A sangre: la pieza se sale del margen de la columna hasta el borde de la pantalla. */
fun Modifier.aSangre(lados: Dp = 16.dp, arriba: Dp = 0.dp): Modifier = layout { medible, c ->
    val l = lados.roundToPx()
    val t = arriba.roundToPx()
    val ancho = c.maxWidth + 2 * l
    val p = medible.measure(c.copy(minWidth = ancho, maxWidth = ancho))
    layout(c.maxWidth, (p.height - t).coerceAtLeast(0)) { p.place(-l, -t) }
}

/** La fila de «lo tuyo»: un tono en Vidrio, pasada por el resaltador en Afiche. */
fun Modifier.fondoTuya(tuya: Boolean, d: Diseno): Modifier = when {
    // En Vidrio el tono llega al borde del grupo y la tecla queda a 8 dp de él.
    d.esVidrio -> (if (tuya) background(d.paleta.hueco) else this).padding(end = 8.dp)
    tuya && d.esAfiche -> background(TonosAfiche.ResaltadorFila)
    else -> this
}

private fun Modifier.rodillo(): Modifier = drawBehind {
    val blanco = Color(0xFFFFFFFF)
    drawRect(
        Brush.verticalGradient(
            0f to blanco.copy(alpha = 0.07f),
            (2f / 6f) to blanco.copy(alpha = 0.07f),
            (2f / 6f) to blanco.copy(alpha = 0f),
            1f to blanco.copy(alpha = 0f),
            startY = 0f,
            endY = 6.dp.toPx(),
            tileMode = TileMode.Repeated,
        ),
    )
}

/** La línea de corte de Afiche entre renglones. */
private fun Modifier.lineaDeCorte(): Modifier = drawBehind {
    drawLine(
        color = TonosAfiche.Punteado,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
    )
}

/** Una lista de canciones: tarjetas sueltas, un grupo de vidrio o renglones de cartel. */
@Composable
fun <T> Filas(elementos: List<T>, modifier: Modifier = Modifier, fila: @Composable (T) -> Unit) {
    val d = LocalDiseno.current
    when (d.tema) {
        TemaBar.ROCKOLA -> Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            elementos.forEach { fila(it) }
        }
        TemaBar.VIDRIO -> {
            val forma = RadioTarjeta
            Column(modifier.fillMaxWidth().clip(forma).background(TonosVidrio.Superficie).border(1.dp, TonosVidrio.BordeVidrio, forma)) {
                elementos.forEachIndexed { i, e ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(TonosVidrio.Separador))
                    fila(e)
                }
            }
        }
        TemaBar.AFICHE -> Column(
            modifier.fillMaxWidth().drawBehind {
                drawRect(TonosAfiche.TintaAfiche, size = size.copy(height = 3.dp.toPx()))
            }.padding(top = 3.dp),
        ) {
            elementos.forEachIndexed { i, e ->
                Box(if (i > 0) Modifier.lineaDeCorte() else Modifier) { fila(e) }
            }
        }
    }
}

/** La tarjeta de título sin su papel: fila de vidrio o renglón de cartel. */
@Composable
internal fun RenglonTitulo(
    titulo: String,
    artista: String,
    modifier: Modifier,
    variante: VarianteTarjeta,
    puesto: Int?,
    votos: Int?,
    duracion: String?,
    miniatura: String?,
    leido: String,
) {
    val d = LocalDiseno.current
    val afiche = d.esAfiche
    val colorTitulo = if (variante == VarianteTarjeta.APAGADA) Tinta2 else Tinta
    val colorArtista = if (afiche && variante == VarianteTarjeta.TUYA) Tinta else Tinta2
    val sinInicio = miniatura == null && puesto == null
    Row(
        modifier
            .heightIn(min = if (afiche) 60.dp else 64.dp)
            .height(IntrinsicSize.Min)
            .clearAndSetSemantics { contentDescription = leido },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (miniatura != null) {
            MiniaturaRenglon(miniatura, puesto, afiche)
        } else if (puesto != null) {
            Box(Modifier.width(if (afiche) 30.dp else 40.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text("$puesto", style = Cifra.copy(fontSize = if (afiche) 32.sp else 15.sp), color = if (afiche) Tinta else Tinta2)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = if (sinInicio && !afiche) 14.dp else if (afiche && !sinInicio) 6.dp else 0.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Text(
                titulo,
                style = Condensada.copy(fontSize = 16.sp, lineHeight = 1.25.em, letterSpacing = 0.em),
                color = colorTitulo,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(artista, style = Cuerpo.copy(fontSize = 13.sp, lineHeight = 1.35.em), color = colorArtista)
        }
        if (votos != null) {
            Column(Modifier.widthIn(min = if (afiche) 44.dp else 52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$votos", style = Cifra.copy(fontSize = if (afiche) 30.sp else 17.sp), color = colorTitulo)
                Text(
                    if (votos == 1) "voto" else "votos",
                    style = Cuerpo.copy(fontSize = if (afiche) 11.sp else 13.sp, fontWeight = if (afiche) FontWeight.W600 else FontWeight.W400, lineHeight = 1.2.em),
                    color = Tinta2,
                )
            }
        }
        if (duracion != null) {
            Text(
                duracion,
                Modifier.padding(horizontal = 4.dp),
                style = Cuerpo.copy(fontSize = 14.sp, fontWeight = if (afiche) FontWeight.W600 else FontWeight.W400, fontFeatureSettings = "tnum"),
                color = if (afiche) Tinta else Tinta2,
            )
        }
    }
}

@Composable
private fun MiniaturaRenglon(url: String, puesto: Int?, afiche: Boolean) {
    val forma = RadioFoto
    Box(Modifier.width(if (afiche) 54.dp else 62.dp).fillMaxHeight()) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .padding(start = if (afiche) 0.dp else 14.dp, end = if (afiche) 6.dp else 8.dp, top = if (afiche) 6.dp else 10.dp, bottom = if (afiche) 6.dp else 10.dp)
                .clip(forma),
            contentScale = ContentScale.Crop,
        )
        if (puesto != null) {
            val fondo = if (afiche) TonosAfiche.TintaAfiche else TonosVidrio.Velo
            val texto = if (afiche) TonosAfiche.PapelAfiche else TonosVidrio.Blanco
            val etiqueta = if (afiche) RoundedCornerShape(0.dp) else CircleShape
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = if (afiche) 0.dp else 10.dp, bottom = 4.dp)
                    .background(fondo, etiqueta)
                    .widthIn(min = 22.dp)
                    .heightIn(min = 22.dp)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("$puesto", style = Cifra.copy(fontSize = if (afiche) 18.sp else 13.sp), color = texto)
            }
        }
    }
}

/** Lo que suena: tarjeta grande de vidrio, o bloque de tinta partida a sangre. */
@Composable
internal fun ArcoTema(modifier: Modifier, apagado: Boolean, descripcion: String?, contenido: @Composable ColumnScope.() -> Unit) {
    val d = LocalDiseno.current
    val leer = if (descripcion != null) Modifier.semantics { contentDescription = descripcion } else Modifier
    val centrado = d.esAfiche && !apagado
    CompositionLocalProvider(
        LocalDiseno provides if (centrado) d.sobreLuz() else d,
        LocalEnArco provides true,
        LocalVentanaPlana provides true,
        LocalAlineacionArco provides if (centrado) TextAlign.Center else TextAlign.Start,
    ) {
        val forma = RadioMarco
        val caja = when {
            d.esVidrio && !apagado -> modifier
                .fillMaxWidth()
                .then(leer)
                .background(TonosVidrio.Superficie, forma)
                .border(1.dp, TonosVidrio.BordeVidrio, forma)
                .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp)
            d.esVidrio -> modifier.fillMaxWidth().then(leer).padding(top = 30.dp)
            !apagado -> modifier
                .aSangre()
                .then(leer)
                .background(d.paleta.luz)
                .rodillo()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp)
            else -> modifier.fillMaxWidth().then(leer)
        }
        Column(caja, horizontalAlignment = if (centrado) Alignment.CenterHorizontally else Alignment.Start, content = contenido)
    }
}

/** «Tu canción»: la tarjeta con el degradado, o el bloque de tinta a sangre. */
@Composable
internal fun MarcoTema(modifier: Modifier, contenido: @Composable () -> Unit) {
    val d = LocalDiseno.current
    val forma = RadioMarco
    val caja = if (d.esVidrio) {
        modifier
            .fillMaxWidth()
            .dropShadow(forma, Shadow(radius = 24.dp, color = TonosVidrio.SombraAcento.copy(alpha = 0.5f), offset = DpOffset(0.dp, 12.dp)))
            .background(d.paleta.luz, forma)
    } else {
        modifier.aSangre().background(d.paleta.luz).rodillo().padding(horizontal = 2.dp)
    }
    CompositionLocalProvider(
        LocalDiseno provides d.sobreLuz(),
        LocalVentanaPlana provides true,
        LocalAlineacionArco provides TextAlign.Start,
    ) {
        Box(caja) { contenido() }
    }
}

/** La caja del horario cuando el bar está cerrado. */
@Composable
fun CajaHorario(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    val d = LocalDiseno.current
    val forma = RadioMarco
    val caja = when (d.tema) {
        TemaBar.ROCKOLA -> modifier
        TemaBar.VIDRIO -> modifier
            .fillMaxWidth()
            .background(TonosVidrio.Superficie, forma)
            .border(1.dp, TonosVidrio.BordeVidrio, forma)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 20.dp)
        TemaBar.AFICHE -> modifier
            .fillMaxWidth()
            .border(3.dp, TonosAfiche.TintaAfiche)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
    }
    Column(caja, content = contenido)
}

/** «En vivo»: píldora de vidrio con el punto durazno, o punto de tinta. */
@Composable
internal fun LuzTema(texto: String, encendida: Boolean, modifier: Modifier) {
    val d = LocalDiseno.current
    if (d.esVidrio) {
        val forma = RoundedCornerShape(999.dp)
        Row(
            modifier
                .heightIn(min = 32.dp)
                .background(TonosVidrio.Superficie, forma)
                .border(1.dp, TonosVidrio.BordeVidrio, forma)
                .padding(start = 11.dp, end = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .drawBehind { if (encendida) drawCircle(TonosVidrio.Halo, radius = size.minDimension / 2 + 4.dp.toPx()) }
                    .background(if (encendida) TonosVidrio.Durazno else TonosVidrio.Apagado, CircleShape),
            )
            Text(texto, style = Cuerpo.copy(fontSize = 13.sp, fontWeight = FontWeight.W500), color = TonosVidrio.Blanco, maxLines = 1)
        }
    } else {
        val tinta = TonosAfiche.TintaAfiche
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (encendida) {
                Box(
                    Modifier
                        .size(10.dp)
                        .drawBehind { drawCircle(tinta.copy(alpha = 0.18f), radius = size.minDimension / 2 + 3.dp.toPx()) }
                        .background(tinta, CircleShape),
                )
            } else {
                Box(Modifier.size(10.dp).border(2.dp, tinta, CircleShape))
            }
            Text(texto, style = Cuerpo.copy(fontSize = 14.sp, fontWeight = FontWeight.W700), color = tinta)
        }
    }
}

/** Los pedidos que quedan: rayitas en Vidrio, boletas en Afiche. Decorativas, como las fichas. */
@Composable
internal fun FichasTema(libres: Int, total: Int, modifier: Modifier) {
    val d = LocalDiseno.current
    Row(modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val libre = i < libres
            if (d.esVidrio) {
                val forma = RoundedCornerShape(3.dp)
                Box(
                    Modifier
                        .width(18.dp)
                        .height(6.dp)
                        .then(if (libre) Modifier.background(TonosVidrio.Acento, forma) else Modifier.background(TonosVidrio.Pista, forma)),
                )
            } else {
                val forma = RoundedCornerShape(2.dp)
                val tinta = TonosAfiche.TintaAfiche
                Box(
                    Modifier
                        .width(22.dp)
                        .height(13.dp)
                        .then(if (libre) Modifier.background(tinta, forma) else Modifier)
                        .border(2.dp, tinta, forma),
                )
            }
        }
    }
}

/** «24 votos» o «Subió 2 puestos». Dentro del arco, sin caja. */
@Composable
internal fun InsigniaTema(texto: String, modifier: Modifier, icono: NombreIcono?) {
    val d = LocalDiseno.current
    val enArco = LocalEnArco.current
    val forma = RadioInsignia
    val (fondo, color) = when {
        enArco -> Color.Transparent to Crema
        d.esVidrio -> TonosVidrio.Subio to TonosVidrio.TintaVidrio
        else -> TonosAfiche.TintaAfiche to TonosAfiche.Amarillo
    }
    Row(
        modifier
            .background(fondo, forma)
            .padding(start = if (enArco) 0.dp else if (icono != null) 7.dp else 10.dp, end = if (enArco) 0.dp else 10.dp, top = 2.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icono != null) Icono(icono, tamano = 15.dp, color = color, grosor = 2.4f)
        Text(
            texto,
            style = Cuerpo.copy(
                fontSize = if (enArco) 17.sp else 13.sp,
                fontWeight = if (enArco || d.esVidrio) FontWeight.W700 else FontWeight.W800,
                lineHeight = 1.35.em,
            ),
            color = color,
        )
    }
}

/** Los días del horario: los que abren resaltan y «Hoy» marca el actual. */
@Composable
internal fun DiasTema(dias: List<Dia>, modifier: Modifier) {
    // Siete casillas en 360 dp: su letra crece hasta 1,25× para no cortarse. El horario completo
    // va en texto debajo, sin tope, y TalkBack lee cada día entero.
    val densidad = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(densidad.density, minOf(densidad.fontScale, 1.25f))) {
        CasillasDias(dias, modifier)
    }
}

@Composable
private fun CasillasDias(dias: List<Dia>, modifier: Modifier) {
    val d = LocalDiseno.current
    val forma = RadioDia
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (d.esVidrio) 4.dp else 5.dp)) {
        dias.forEach { dia ->
            val (caja, texto, hoy) = if (d.esVidrio) {
                val borde = when {
                    dia.hoy -> TonosVidrio.Hoy
                    dia.abre -> TonosVidrio.TeclaBorde
                    else -> Color.Transparent
                }
                val fondo = if (dia.abre) TonosVidrio.SuperficieAlta else Color.Transparent
                val color = if (dia.abre) TonosVidrio.Blanco else TonosVidrio.Texto2
                Triple(Modifier.background(fondo, forma).border(1.dp, borde, forma), color, color)
            } else {
                val tinta = TonosAfiche.TintaAfiche
                if (dia.abre) {
                    Triple(Modifier.background(tinta, forma), TonosAfiche.PapelAfiche, TonosAfiche.Amarillo)
                } else {
                    Triple(Modifier.border(2.dp, tinta, forma), tinta, TonosAfiche.Sello)
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = if (d.esVidrio) 58.dp else 50.dp)
                    .then(caja)
                    .padding(vertical = 3.dp)
                    .clearAndSetSemantics { contentDescription = "${dia.largo}: ${if (dia.abre) "abre" else "cerrado"}${if (dia.hoy) ", hoy" else ""}" },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(dia.corto, style = Cuerpo.copy(fontSize = 13.sp, fontWeight = FontWeight.W600, lineHeight = 1.2.em), color = texto, maxLines = 1, softWrap = false)
                if (dia.hoy) Text("Hoy", style = Cuerpo.copy(fontSize = 11.sp, fontWeight = FontWeight.W600, lineHeight = 1.2.em), color = hoy, maxLines = 1, softWrap = false)
            }
        }
    }
}
