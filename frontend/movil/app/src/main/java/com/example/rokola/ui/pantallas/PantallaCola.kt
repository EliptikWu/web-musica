package com.example.rokola.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.EstadoServicio
import com.example.rokola.dominio.EstadoSolicitud
import com.example.rokola.dominio.ItemCola
import com.example.rokola.dominio.MiSolicitud
import com.example.rokola.dominio.reglas.duracionCola
import com.example.rokola.ui.componentes.AccionAviso
import com.example.rokola.ui.componentes.Aviso
import com.example.rokola.ui.componentes.Insignia
import com.example.rokola.ui.componentes.LuzDeEstado
import com.example.rokola.ui.componentes.Marco
import com.example.rokola.ui.componentes.MarcoCliente
import com.example.rokola.ui.componentes.NombreIcono
import com.example.rokola.ui.componentes.TamanoTecla
import com.example.rokola.ui.componentes.TarjetaTitulo
import com.example.rokola.ui.componentes.Tecla
import com.example.rokola.ui.componentes.TipoAviso
import com.example.rokola.ui.componentes.TituloDeSeccion
import com.example.rokola.ui.componentes.Vacio
import com.example.rokola.ui.componentes.VarianteTarjeta
import com.example.rokola.ui.componentes.VarianteTecla
import com.example.rokola.ui.componentes.Ventana
import com.example.rokola.ui.textos.ColaVacia
import com.example.rokola.ui.textos.Conexion
import com.example.rokola.ui.textos.REGLA_DESEMPATE
import com.example.rokola.ui.textos.T
import com.example.rokola.ui.textos.TextosVoto
import com.example.rokola.ui.textos.TituloMeta
import com.example.rokola.ui.textos.colaCerrada
import com.example.rokola.ui.textos.estadoPedido
import com.example.rokola.ui.textos.formatoEspera
import com.example.rokola.ui.textos.formatoEsperaCorta
import com.example.rokola.ui.textos.formatoResumenCola
import com.example.rokola.ui.textos.luzServicio
import com.example.rokola.ui.theme.ArchivoSeccion
import com.example.rokola.ui.theme.Borde
import com.example.rokola.ui.theme.Cifra
import com.example.rokola.ui.theme.Condensada
import com.example.rokola.ui.theme.Crema
import com.example.rokola.ui.theme.Crema2
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.FondoDialogo
import com.example.rokola.ui.theme.RadioMarco
import com.example.rokola.ui.theme.Tinta
import com.example.rokola.ui.theme.Tinta2
import com.example.rokola.viewmodel.AvisoCola
import com.example.rokola.viewmodel.Cola
import com.example.rokola.viewmodel.EstadoPantalla
import com.example.rokola.viewmodel.FilaCola
import kotlinx.coroutines.flow.Flow

/** Un aviso en pantalla, con su tipo. */
data class AvisoEnPantalla(val tipo: TipoAviso, val textos: TituloMeta)

/** Qué decir cuando el servidor no aceptó un voto (useVotarConAviso.ts de la web). */
fun avisoDeVoto(error: ErrorRockola, titulo: String): AvisoEnPantalla = when (error.codigo) {
    CodigoError.YA_VOTASTE -> AvisoEnPantalla(TipoAviso.INFO, TextosVoto.yaVotaste(titulo))
    CodigoError.NO_ENCONTRADO, CodigoError.NO_PENDIENTE -> AvisoEnPantalla(TipoAviso.INFO, TextosVoto.yaNoEsta(titulo))
    CodigoError.SERVICIO_NO_DISPONIBLE -> AvisoEnPantalla(TipoAviso.INFO, TextosVoto.cerrado)
    else -> AvisoEnPantalla(TipoAviso.ERROR, TextosVoto.fallo(titulo))
}

/**
 * La cola en tiempo real (prototipo 3, PB-14, PB-17 a PB-19, PB-22): «Tu canción», la cola
 * ordenada por votos con la espera de cada una, la regla de desempate con el caso real y lo
 * que pasó con los pedidos de este teléfono.
 */
@Composable
fun PantallaCola(
    estado: EstadoPantalla<Cola>,
    avisos: Flow<AvisoCola>,
    onReintentar: () -> Unit,
    onVotar: (String) -> Unit,
    onMover: (desde: String, hacia: String) -> Unit,
    destinos: (desde: String) -> List<FilaCola>,
    onPedirUna: () -> Unit,
    onPolitica: () -> Unit,
    pie: @Composable () -> Unit,
) {
    var aviso by remember { mutableStateOf<AvisoEnPantalla?>(null) }
    var ultimoVoto by remember { mutableStateOf("") }
    var moviendo by remember { mutableStateOf<ItemCola?>(null) }
    var desdeMovido by remember { mutableStateOf("") }
    val titulos by rememberUpdatedState((estado as? EstadoPantalla.Contenido)?.datos?.filas.orEmpty().associate { it.item.solicitudId to it.item.cancion.titulo })

    LaunchedEffect(avisos) {
        avisos.collect { a ->
            aviso = when (a) {
                is AvisoCola.VotoFallido -> avisoDeVoto(a.error, ultimoVoto)
                is AvisoCola.VotoMovido -> AvisoEnPantalla(TipoAviso.LUZ, TextosVoto.movido(desdeMovido, titulos[a.hacia] ?: ""))
            }
        }
    }

    val datos = when (estado) {
        is EstadoPantalla.Contenido -> estado.datos
        is EstadoPantalla.Vacio -> estado.datos
        EstadoPantalla.Cargando -> return MarcoCliente(titulo = T.LA_COLA, pie = pie) { CargandoCliente(arco = false) }
        is EstadoPantalla.Error -> return MarcoCliente(titulo = T.LA_COLA, pie = pie) { FalloCarga(onReintentar) }
    }
    val abierto = datos.puedeVotar
    val filas = datos.filas

    MarcoCliente(
        apagada = !abierto,
        sinConexion = !datos.enVivo,
        titulo = T.LA_COLA,
        pie = pie,
        abajo = { EnlacePolitica(onPolitica) },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                TituloPlaca(T.LA_COLA)
                Text(
                    formatoResumenCola(filas.size, duracionCola(filas.map { it.item }).toDouble()),
                    Modifier.padding(top = 4.dp),
                    style = Cuerpo.copy(fontSize = 14.sp),
                    color = Crema2,
                )
            }
            LuzDeEstado(
                if (!datos.enVivo) Conexion.LUZ else luzServicio(datos.servicio.estado),
                encendida = abierto && datos.enVivo,
                modifier = Modifier.padding(top = 6.dp, start = 12.dp),
            )
        }

        if (datos.servicio.estado != EstadoServicio.ABIERTO) {
            Aviso(Modifier.padding(top = 16.dp), tipo = TipoAviso.INFO, meta = colaCerrada(datos.servicio.estado))
        }

        val tuya = datos.tuCancion
        val suenaLaMia = datos.reproduccion?.solicitudId?.let { id -> datos.misSolicitudes.any { it.solicitudId == id } } == true
        if (tuya != null) {
            TuCancion(tuya, Modifier.padding(top = 16.dp))
        } else if (suenaLaMia && datos.reproduccion != null) {
            TuCancionSuena(datos.reproduccion.cancion.titulo, datos.reproduccion.cancion.artista, Modifier.padding(top = 16.dp))
        }

        aviso?.let { a ->
            Aviso(Modifier.padding(top = 16.dp), tipo = a.tipo, titulo = a.textos.titulo, meta = a.textos.meta, accion = AccionAviso(T.CERRAR) { aviso = null })
        }

        if (filas.isNotEmpty()) {
            Column(Modifier.padding(top = 14.dp).semantics { contentDescription = "Canciones en la cola" }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filas.forEach { f ->
                    FilaCancion(
                        item = f.item,
                        tecla = f.tecla,
                        artista = "${f.item.cancion.artista}, ${formatoEsperaCorta(f.esperaSeg)}",
                        puedeVotar = abierto,
                        ocupada = f.ocupada,
                        onVotar = { ultimoVoto = it.cancion.titulo; onVotar(it.solicitudId) },
                        onMover = { moviendo = it },
                    )
                }
            }
            Text(
                buildAnnotatedString {
                    datos.desempate?.let { par ->
                        append("$REGLA_DESEMPATE Por eso ")
                        withStyle(SpanStyle(color = Crema, fontWeight = FontWeight.W700)) { append(par.antes.cancion.titulo) }
                        append(" va antes que ")
                        withStyle(SpanStyle(color = Crema, fontWeight = FontWeight.W700)) { append(par.despues.cancion.titulo) }
                        append(". ")
                    }
                    append(T.ESPERAS_ESTIMADAS)
                },
                Modifier.padding(top = 12.dp),
                style = Cuerpo.copy(fontSize = 14.sp, lineHeight = 1.45.em),
                color = Crema2,
            )
        } else {
            val vacia = if (datos.listaCasa != null) ColaVacia.conLista else ColaVacia.sinLista
            Vacio(
                vacia.meta,
                Modifier.padding(top = 14.dp),
                destacado = vacia.titulo,
                abajo = if (abierto) ({ Tecla(T.PEDIR_UNA, onPedirUna, variante = VarianteTecla.CEREZA, tamano = TamanoTecla.CHICA) }) else null,
            )
        }

        TusPedidos(datos.misSolicitudes)
    }

    moviendo?.let { desde ->
        DialogoMoverVoto(
            desde = desde,
            opciones = destinos(desde.solicitudId),
            onCerrar = { moviendo = null },
            onMover = { hacia ->
                desdeMovido = desde.cancion.titulo
                ultimoVoto = desde.cancion.titulo
                onMover(desde.solicitudId, hacia.item.solicitudId)
                moviendo = null
            },
        )
    }
}

/** La tarjeta destacada (PB-19, PB-22): en qué puesto va, si subió y cuánto falta. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TuCancion(fila: FilaCola, modifier: Modifier) {
    val item = fila.item
    Marco(modifier.semantics(mergeDescendants = true) { contentDescription = "Tu canción" }) {
        Ventana(relleno = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp), alineacion = Alignment.Start) {
            // FlowRow: con la letra grande, la insignia baja de línea en vez de partirse.
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(T.TU_CANCION_VA_DE, Modifier.padding(end = 8.dp), style = Cuerpo.copy(fontSize = 14.sp, fontWeight = FontWeight.W600), color = Tinta2)
                if (fila.subio > 0) {
                    Insignia(if (fila.subio == 1) "Subió 1 puesto" else "Subió ${fila.subio} puestos", Modifier.heightIn(min = 26.dp), icono = NombreIcono.SUBIR)
                }
            }
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "#${item.puesto}",
                    Modifier.semantics { contentDescription = "Puesto ${item.puesto}" },
                    style = Cifra.copy(fontSize = 60.sp, letterSpacing = (-0.01).em),
                    color = Tinta,
                )
                Column(Modifier.weight(1f).padding(bottom = 6.dp)) {
                    Text(item.cancion.titulo, style = Condensada.copy(fontSize = 30.sp, lineHeight = 1.em), color = Tinta)
                    Text(
                        "${item.cancion.artista}, ${if (item.votosTotal == 1) "1 voto" else "${item.votosTotal} votos"}",
                        Modifier.padding(top = 3.dp),
                        style = Cuerpo.copy(fontSize = 14.sp),
                        color = Tinta2,
                    )
                }
            }
            Text("Suena ${formatoEspera(fila.esperaSeg)}", Modifier.padding(top = 6.dp), style = Cuerpo.copy(fontSize = 14.sp), color = Tinta2)
        }
    }
}

@Composable
private fun TuCancionSuena(titulo: String, artista: String, modifier: Modifier) {
    Marco(modifier.semantics(mergeDescendants = true) { contentDescription = "Tu canción" }) {
        Ventana(relleno = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp), alineacion = Alignment.Start) {
            Text(T.TU_CANCION_SUENA, style = Cuerpo.copy(fontSize = 14.sp, fontWeight = FontWeight.W600), color = Tinta2)
            Text(titulo, Modifier.padding(top = 4.dp), style = Condensada.copy(fontSize = 30.sp, lineHeight = 1.em), color = Tinta)
            Text(artista, Modifier.padding(top = 3.dp), style = Cuerpo.copy(fontSize = 14.sp), color = Tinta2)
        }
    }
}

/** RF-013: todos los pedidos de este teléfono esta noche, con su estado y el motivo si lo hay. */
@Composable
private fun TusPedidos(solicitudes: List<MiSolicitud>) {
    if (solicitudes.isEmpty()) return
    TituloDeSeccion(T.TUS_PEDIDOS, Modifier.padding(top = 28.dp, bottom = 12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        solicitudes.forEach { s ->
            val estado = estadoPedido(s.estado, s.puesto)
            TarjetaTitulo(
                titulo = s.cancion.titulo,
                artista = if (s.motivo != null) "$estado: ${s.motivo}" else estado,
                modifier = Modifier.fillMaxWidth(),
                variante = when (s.estado) {
                    EstadoSolicitud.PENDIENTE, EstadoSolicitud.REPRODUCIENDO -> VarianteTarjeta.TUYA
                    EstadoSolicitud.REPRODUCIDA -> VarianteTarjeta.NORMAL
                    else -> VarianteTarjeta.APAGADA
                },
                miniatura = s.cancion.miniaturaUrl,
            )
        }
    }
}

/**
 * Mover el voto (RF-014, PB-50): desde «Votaste», a otra canción de la cola por la que este
 * teléfono no haya votado y que no sea suya. Sube desde abajo, como el diálogo de la web en
 * pantallas angostas.
 */
@Composable
private fun DialogoMoverVoto(desde: ItemCola, opciones: List<FilaCola>, onCerrar: () -> Unit, onMover: (FilaCola) -> Unit) {
    val altoMax = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * 0.52f).toDp() }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize()) {
            // Tocar la penumbra cierra, como Escape en la web.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCerrar)
                    .clearAndSetSemantics { },
            )
            val forma = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            // En tableta, del ancho de la columna del cliente (480 dp), no de toda la pantalla.
            Column(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .background(FondoDialogo, forma)
                    .innerShadow(forma, Shadow(radius = 0.dp, color = Borde, spread = 1.dp))
                    .navigationBarsPadding()
                    .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    TextosVoto.moverTitulo(desde.cancion.titulo),
                    Modifier.semantics { heading() },
                    style = Cuerpo.copy(fontFamily = ArchivoSeccion, fontSize = 22.sp, fontWeight = FontWeight.W800, lineHeight = 1.2.em),
                    color = Crema,
                )
                if (opciones.isEmpty()) {
                    Text(TextosVoto.SIN_DESTINO, style = Cuerpo.copy(fontSize = 15.sp, lineHeight = 1.45.em), color = Crema2)
                } else {
                    Text(TextosVoto.MOVER_TEXTO, style = Cuerpo.copy(fontSize = 15.sp, lineHeight = 1.45.em), color = Crema2)
                    Column(
                        Modifier.heightIn(max = altoMax).verticalScroll(rememberScrollState()).padding(2.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        opciones.forEach { f ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TarjetaTitulo(f.item.cancion.titulo, f.item.cancion.artista, Modifier.weight(1f), puesto = f.item.puesto, votos = f.item.votosTotal, miniatura = f.item.cancion.miniaturaUrl)
                                Tecla("Aquí", { onMover(f) }, etiqueta = TextosVoto.moverA(f.item.cancion.titulo))
                            }
                        }
                    }
                }
                Tecla("Cancelar", onCerrar, Modifier.fillMaxWidth())
            }
        }
    }
}
