package com.example.rokola.ui.pantallas

import com.example.rokola.ui.theme.comoCartel
import com.example.rokola.ui.theme.estiloCartel
import com.example.rokola.ui.theme.TonosAfiche
import com.example.rokola.ui.theme.TituloCerrado
import com.example.rokola.ui.theme.LocalDiseno
import com.example.rokola.ui.componentes.fondoTuya
import com.example.rokola.ui.componentes.alineacionArco
import com.example.rokola.ui.componentes.aSangre
import com.example.rokola.ui.componentes.CajaHorario
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.rokola.dominio.Ahora
import com.example.rokola.dominio.Bar
import com.example.rokola.dominio.ItemCola
import com.example.rokola.dominio.reglas.EstadoTecla
import com.example.rokola.ui.componentes.AccionAviso
import com.example.rokola.ui.componentes.Arco
import com.example.rokola.ui.componentes.Aviso
import com.example.rokola.ui.componentes.Dia
import com.example.rokola.ui.componentes.DiasSemana
import com.example.rokola.ui.componentes.Enlace
import com.example.rokola.ui.componentes.Esqueleto
import com.example.rokola.ui.componentes.LuzDeEstado
import com.example.rokola.ui.componentes.TamanoTecla
import com.example.rokola.ui.componentes.TarjetaTitulo
import com.example.rokola.ui.componentes.Tecla
import com.example.rokola.ui.componentes.TeclaFija
import com.example.rokola.ui.componentes.TipoAviso
import com.example.rokola.ui.componentes.TituloDeSeccion
import com.example.rokola.ui.componentes.VarianteTarjeta
import com.example.rokola.ui.componentes.VarianteTecla
import com.example.rokola.ui.componentes.Ventana
import com.example.rokola.ui.textos.DIAS
import com.example.rokola.ui.textos.T
import com.example.rokola.ui.textos.TextosVoto
import com.example.rokola.ui.textos.diaDeLaSemana
import com.example.rokola.ui.textos.falloCarga
import com.example.rokola.ui.textos.resumenHorario
import com.example.rokola.ui.textos.servicioCerrado
import com.example.rokola.ui.theme.Crema
import com.example.rokola.ui.theme.Crema2
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.Placa
import com.example.rokola.ui.theme.RadioArco
import com.example.rokola.ui.theme.Tinta
import com.example.rokola.ui.theme.Tinta2

/** Título de página (.placa): 125 %, 800. */
@Composable
fun TituloPlaca(texto: String, modifier: Modifier = Modifier, tamano: Int = 24, alineacion: TextAlign = TextAlign.Start) {
    val tipos = LocalDiseno.current.tipos
    // En Vidrio y Afiche la placa tiene su propio tamaño (h1.placa en temas/*.css).
    val medida = if (tamano == 24) tipos.tamanoPlaca ?: tamano else tamano
    Text(
        texto.comoCartel(),
        modifier.semantics {
            heading()
            contentDescription = texto
        },
        style = Placa.copy(fontSize = medida.sp),
        color = Crema,
        textAlign = alineacion,
    )
}

/**
 * El nombre del bar con su luz. Rockola lo centra; Vidrio lo pone a la izquierda con la luz al
 * lado; Afiche lo imprime a sangre sobre la tinta partida, unido al arco que sigue.
 */
@Composable
fun ColumnScope.CabeceraBar(nombre: String, luz: String, encendida: Boolean, apagada: Boolean = false) {
    val d = LocalDiseno.current
    val tamano = (if (apagada) d.tipos.tamanoPlacaBarApagada else d.tipos.tamanoPlacaBar) ?: 25
    when {
        d.esRockola -> {
            TituloPlaca(nombre, Modifier.fillMaxWidth(), tamano = tamano, alineacion = TextAlign.Center)
            LuzDeEstado(luz, encendida = encendida, modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally))
        }
        // Con la letra grande el nombre necesita todo el ancho: la luz baja a su propia línea.
        (d.esVidrio || apagada) && LocalDensity.current.fontScale > 1.3f -> Column(Modifier.fillMaxWidth()) {
            TituloPlaca(nombre, Modifier.fillMaxWidth(), tamano = tamano)
            LuzDeEstado(luz, encendida = encendida, modifier = Modifier.padding(top = 8.dp))
        }
        d.esVidrio || apagada -> Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (d.esAfiche) Modifier.drawBehind {
                        val g = 3.dp.toPx()
                        drawRect(TonosAfiche.TintaAfiche, topLeft = Offset(0f, size.height - g), size = Size(size.width, g))
                    }.padding(bottom = 13.dp) else Modifier,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TituloPlaca(nombre, Modifier.weight(1f).padding(end = 12.dp), tamano = tamano)
            LuzDeEstado(luz, encendida = encendida)
        }
        else -> Column(
            Modifier
                .aSangre(arriba = 22.dp)
                .background(d.paleta.luz)
                .drawBehind {
                    val g = 3.dp.toPx()
                    drawRect(TonosAfiche.TintaAfiche, topLeft = Offset(0f, size.height - g), size = Size(size.width, g))
                }
                .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 19.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TituloPlaca(nombre, Modifier.fillMaxWidth(), tamano = tamano, alineacion = TextAlign.Center)
            LuzDeEstado(luz, encendida = encendida, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** Mientras llega la primera foto: la forma de la pantalla, sin texto inventado. */
@Composable
fun CargandoCliente(arco: Boolean = true) {
    Column(Modifier.fillMaxWidth().semantics { contentDescription = T.CARGANDO; liveRegion = LiveRegionMode.Polite }) {
        Esqueleto(Modifier.width(192.dp).height(28.dp).align(Alignment.CenterHorizontally))
        if (arco) Esqueleto(Modifier.padding(top = 24.dp).fillMaxWidth().height(292.dp), forma = RadioArco)
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { Esqueleto(Modifier.fillMaxWidth().height(56.dp)) }
        }
    }
}

/** No llegó el bar o la foto: se dice y se puede volver a intentar. */
@Composable
fun FalloCarga(onReintentar: () -> Unit) {
    Aviso(tipo = TipoAviso.ERROR, titulo = falloCarga.titulo, meta = falloCarga.meta, accion = AccionAviso(T.REINTENTAR, onReintentar))
}

/** Enlace a la política de datos, centrado. */
@Composable
fun EnlacePolitica(onPolitica: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Enlace(T.POLITICA, onPolitica) }
}

/**
 * El bar no recibe canciones (prototipo 4, PB-30): el arco apagado con el motivo, el horario
 * de la semana y la salida a «Ver qué está sonando». Sirve para fuera de horario, pausa y
 * mantenimiento, cada uno con su texto.
 */
@Composable
fun ColumnScope.EstadoCerradoArriba(bar: Bar, ahora: Ahora) {
    val textos = servicioCerrado(ahora.servicio.estado, ahora.servicio.proximaApertura, bar.zonaHoraria, ahora.servidorAhora)
    val abren = bar.horario.filter { it.activo }.map { it.diaSemana }.toSet()
    val hoy = diaDeLaSemana(ahora.servidorAhora, bar.zonaHoraria)
    val lineas = resumenHorario(bar.horario)
    val rockola = LocalDiseno.current.esRockola

    CabeceraBar(bar.nombre, textos.luz, encendida = false, apagada = true)

    Arco(
        Modifier.padding(top = 34.dp),
        apagado = true,
        altoMinimo = 322.dp,
        relleno = PaddingValues(start = 18.dp, end = 18.dp, top = 104.dp, bottom = 18.dp),
        descripcion = "Estado de la rockola",
    ) {
        Ventana(relleno = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp)) {
            Text(
                textos.titulo.comoCartel(),
                Modifier.semantics {
                    heading()
                    contentDescription = textos.titulo
                },
                style = estiloCartel(TituloCerrado, textos.titulo),
                color = Tinta,
                textAlign = alineacionArco(),
            )
            Text(
                textos.detalle,
                Modifier.padding(top = if (rockola) 10.dp else 14.dp),
                style = Cuerpo.copy(fontSize = if (rockola) 15.sp else 17.sp, lineHeight = if (rockola) 1.4.em else 1.45.em),
                color = Tinta2,
                textAlign = alineacionArco(),
            )
        }
    }

    if (lineas.isNotEmpty()) {
        CajaHorario(Modifier.padding(top = if (rockola) 0.dp else 32.dp)) {
            TituloDeSeccion(T.HORARIO_DE_PEDIDOS, Modifier.padding(top = if (rockola) 36.dp else 0.dp).fillMaxWidth(), alineacion = TextAlign.Center)
            DiasSemana(
                DIAS.mapIndexed { i, d -> Dia(d.corto, d.largo, abre = (i + 1) in abren, hoy = hoy == i + 1) },
                Modifier.padding(top = 12.dp),
            )
            lineas.forEach { l ->
                Text(
                    buildAnnotatedString {
                        append("${l.dias}, ")
                        withStyle(SpanStyle(color = Crema, fontWeight = FontWeight.W700)) { append(l.horas) }
                    },
                    Modifier.padding(top = 14.dp).fillMaxWidth(),
                    style = Cuerpo.copy(fontSize = 16.sp),
                    color = Crema2,
                    textAlign = if (rockola) TextAlign.Center else TextAlign.Start,
                )
            }
        }
    }
}

@Composable
fun ColumnScope.EstadoCerradoAbajo(onVerQueSuena: () -> Unit, onPolitica: () -> Unit) {
    Tecla(T.VER_QUE_SUENA, onVerQueSuena, variante = VarianteTecla.CEREZA, tamano = TamanoTecla.GRANDE)
    EnlacePolitica(onPolitica, Modifier.padding(top = 10.dp))
}

/**
 * Una canción de la cola con su tecla (doc 05): «+1» para votar, «Votaste» hundida, «Tuya»
 * ámbar. Los tres estados se distinguen por texto y forma, no solo por color (WCAG 1.4.1).
 */
@Composable
fun FilaCancion(
    item: ItemCola,
    tecla: EstadoTecla,
    puedeVotar: Boolean,
    ocupada: Boolean,
    onVotar: (ItemCola) -> Unit,
    artista: String = item.cancion.artista,
    onMover: ((ItemCola) -> Unit)? = null,
) {
    val titulo = item.cancion.titulo
    Row(
        Modifier.fillMaxWidth().fondoTuya(tecla == EstadoTecla.TUYA, LocalDiseno.current),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TarjetaTitulo(
            titulo = titulo,
            artista = artista,
            modifier = Modifier.weight(1f),
            variante = if (tecla == EstadoTecla.TUYA) VarianteTarjeta.TUYA else VarianteTarjeta.NORMAL,
            puesto = item.puesto,
            votos = item.votosTotal,
            miniatura = item.cancion.miniaturaUrl,
        )
        when {
            tecla == EstadoTecla.TUYA -> TeclaFija("Tuya", VarianteTecla.TUYA, etiqueta = TextosVoto.tuya(titulo))
            tecla == EstadoTecla.VOTASTE && puedeVotar && onMover != null ->
                Tecla("Votaste", { onMover(item) }, variante = VarianteTecla.HUNDIDA, etiqueta = TextosVoto.mover(titulo), ocupada = ocupada)
            tecla == EstadoTecla.VOTASTE -> TeclaFija("Votaste", VarianteTecla.HUNDIDA, etiqueta = TextosVoto.votaste(titulo))
            puedeVotar -> Tecla("+1", { onVotar(item) }, etiqueta = TextosVoto.etiqueta(titulo), ocupada = ocupada)
        }
    }
}
