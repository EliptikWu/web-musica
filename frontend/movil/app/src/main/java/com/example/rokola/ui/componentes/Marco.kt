package com.example.rokola.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.rokola.dominio.reglas.posicionActual
import com.example.rokola.ui.textos.T
import com.example.rokola.ui.theme.Ambar
import com.example.rokola.ui.theme.Apagado
import com.example.rokola.ui.theme.Archivo
import com.example.rokola.ui.theme.Cereza
import com.example.rokola.ui.theme.Cifra
import com.example.rokola.ui.theme.Condensada
import com.example.rokola.ui.theme.Crema
import com.example.rokola.ui.theme.Crema2
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.FondoMadera
import com.example.rokola.ui.theme.FondoMaderaApagada
import com.example.rokola.ui.theme.Hueco
import com.example.rokola.ui.theme.Luz
import com.example.rokola.ui.theme.Pie as FondoPie
import com.example.rokola.ui.theme.PieFilete
import com.example.rokola.ui.theme.RadioTecla
import com.example.rokola.ui.theme.TeclaCanto
import com.example.rokola.ui.theme.TeclaCara
import com.example.rokola.ui.theme.Tinta
import com.example.rokola.ui.theme.vetaMadera
import com.example.rokola.viewmodel.Pie
import kotlinx.coroutines.delay
import java.time.Instant

/* El mueble del teléfono (MarcoCliente.tsx y PieCliente.tsx de la web). */

private val Negro = Color(0xFF000000)
private val Blanco = Color(0xFFFFFFFF)

/** Nogal con la luz del arco arriba, sombra abajo y la veta; apagada sin el resplandor. */
fun Modifier.madera(apagada: Boolean): Modifier = drawBehind {
    (if (apagada) FondoMaderaApagada else FondoMadera).forEach { drawRect(it) }
    vetaMadera(this).forEach { drawRect(it) }
}

/**
 * La hora del servidor que avanza cada segundo desde la última foto, para el avance de lo
 * que suena sin preguntar al servidor (apps/web/lib/reloj-servidor.ts).
 */
@Composable
fun rememberAhoraServidor(servidorAhora: Instant): Instant {
    val recibida = remember(servidorAhora) { System.currentTimeMillis() }
    var ahora by remember(servidorAhora) { mutableLongStateOf(recibida) }
    LaunchedEffect(servidorAhora) {
        while (true) {
            delay(1000)
            ahora = System.currentTimeMillis()
        }
    }
    return servidorAhora.plusMillis(ahora - recibida)
}

enum class Pestana(val texto: String, val icono: NombreIcono) {
    INICIO("Inicio", NombreIcono.INICIO),
    BUSCAR("Buscar", NombreIcono.BUSCAR),
    COLA("Cola", NombreIcono.COLA),
}

/**
 * Una pantalla del cliente: el contenido se desplaza y, si sobra alto, [abajo] queda pegado al
 * final (la tecla grande de la bienvenida). Desde 480 dp la columna se centra sobre la madera.
 */
@Composable
fun MarcoCliente(
    apagada: Boolean = false,
    sinConexion: Boolean = false,
    titulo: String,
    pie: (@Composable () -> Unit)? = null,
    abajo: (@Composable ColumnScope.() -> Unit)? = null,
    arriba: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().madera(apagada).semantics { paneTitle = titulo }, contentAlignment = Alignment.TopCenter) {
        // widthIn antes de fillMaxSize: al revés, el tope no tiene efecto y en tableta todo se estira.
        Column(Modifier.widthIn(max = 480.dp).fillMaxSize()) {
            Spacer(Modifier.statusBarsPadding())
            if (sinConexion) BannerConexion()
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val alto = maxHeight
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = alto)
                        .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 24.dp)
                        .then(if (pie == null) Modifier.navigationBarsPadding() else Modifier),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(content = arriba)
                    if (abajo != null) Column(Modifier.padding(top = 32.dp), content = abajo)
                }
            }
            pie?.invoke()
        }
    }
}

/** Sale cuando la conexión en vivo se cortó. */
@Composable
private fun BannerConexion() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Cereza)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).background(Crema.copy(alpha = 0.7f), CircleShape))
        Text(com.example.rokola.ui.textos.Conexion.DESCONECTADO, style = Cuerpo.copy(fontSize = 14.sp, fontWeight = FontWeight.W500), color = Crema)
    }
}

/**
 * Pie del teléfono: lo que suena (sin play ni pausa: la música la controla el bar) y las
 * pestañas Inicio, Buscar y Cola, como en los prototipos 2 y 3.
 */
@Composable
fun PieCliente(pie: Pie?, actual: Pestana, onIr: (Pestana) -> Unit) {
    // Con la letra del sistema al 200 % el pie fijo tapaba un cuarto de la pantalla. Su letra
    // crece hasta 1,5×; lo que suena se lee completo en Inicio y el resto de la app no tiene tope.
    val densidad = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(densidad.density, minOf(densidad.fontScale, 1.5f))) {
        ContenidoPie(pie, actual, onIr)
    }
}

@Composable
private fun ContenidoPie(pie: Pie?, actual: Pestana, onIr: (Pestana) -> Unit) {
    val rep = pie?.reproduccion
    val fraccion = if (rep != null && pie != null) {
        val ahora = rememberAhoraServidor(pie.servidorAhora)
        (posicionActual(rep, pie.servidorAhora, ahora) / maxOf(1, rep.cancion.duracionSeg)).toFloat()
    } else 0f
    Column(
        Modifier
            .fillMaxWidth()
            .dropShadow(RoundedCornerShape(0.dp), Shadow(radius = 30.dp, color = Negro.copy(alpha = 0.45f), offset = DpOffset(0.dp, (-12).dp)))
            .background(FondoPie)
            .drawBehind { drawRect(PieFilete, size = size.copy(height = 2.dp.toPx())) }
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)
            .navigationBarsPadding(),
    ) {
        // Mini reproductor
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
                .semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .width(10.dp)
                    .height(40.dp)
                    .then(
                        if (rep != null) Modifier.dropShadow(RoundedCornerShape(5.dp), Shadow(radius = 12.dp, color = Ambar.copy(alpha = 0.6f))).background(Luz, RoundedCornerShape(5.dp))
                        else Modifier.background(Apagado, RoundedCornerShape(5.dp)),
                    ),
            )
            Column(Modifier.weight(1f)) {
                Text(if (rep != null) T.SUENA_AHORA else T.NADA_SUENA, style = Cuerpo.copy(fontSize = 12.sp), color = Crema2)
                if (rep != null) {
                    Text(
                        buildAnnotatedString {
                            append(rep.cancion.titulo)
                            append(" ")
                            withStyle(SpanStyle(fontFamily = Archivo, fontWeight = FontWeight.W400, fontSize = 14.sp, color = Crema2, letterSpacing = 0.sp)) {
                                append(rep.cancion.artista)
                            }
                        },
                        style = Condensada.copy(fontSize = 19.sp, lineHeight = 1.15.em),
                        color = Crema,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (rep?.votos != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("${rep.votos}", style = Cifra.copy(fontSize = 20.sp), color = Crema)
                    Text(if (rep.votos == 1) "voto" else "votos", style = Cuerpo.copy(fontSize = 13.sp), color = Crema2)
                }
            } else if (rep != null) {
                Text(T.LISTA_DE_LA_CASA, Modifier.widthIn(max = 88.dp), style = Cuerpo.copy(fontSize = 13.sp, lineHeight = 1.2.em), color = Crema2, textAlign = TextAlign.End)
            }
        }
        MiniAvance(fraccion, Modifier.padding(start = 6.dp, end = 6.dp, bottom = 10.dp))
        // Pestañas
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pestana.entries.forEach { p -> BotonPestana(p, p == actual, Modifier.weight(1f)) { onIr(p) } }
        }
    }
}

@Composable
private fun BotonPestana(pestana: Pestana, actual: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    val densidad = LocalDensity.current
    val forma = if (actual) {
        Modifier
            .graphicsLayer { translationY = with(densidad) { (-2).dp.toPx() } }
            .dropShadow(RadioTecla, Shadow(radius = 0.dp, color = TeclaCanto, offset = DpOffset(0.dp, 3.dp)))
            .background(TeclaCara, RadioTecla)
            .innerShadow(RadioTecla, Shadow(radius = 0.dp, color = Blanco.copy(alpha = 0.8f), offset = DpOffset(0.dp, 1.dp)))
    } else {
        Modifier
            .background(Hueco, RadioTecla)
            .innerShadow(RadioTecla, Shadow(radius = 3.dp, color = Negro.copy(alpha = 0.4f), offset = DpOffset(0.dp, 1.dp)))
    }
    val color = if (actual) Tinta else Crema2
    Column(
        modifier
            .height(54.dp)
            .then(forma)
            .clearAndSetSemantics {
                contentDescription = pestana.texto + if (actual) ", pestaña actual" else ""
                this.role = Role.Tab
                this.selected = actual
                onClick { alPulsar(); true }
            }
            .clickable(onClick = alPulsar),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icono(pestana.icono, tamano = 22.dp, color = color)
        Text(pestana.texto, style = Cuerpo.copy(fontSize = 12.sp, fontWeight = FontWeight.W600, lineHeight = 1.2.em), color = color)
    }
}
