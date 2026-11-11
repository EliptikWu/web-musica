package com.example.rokola.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.MotivoNoPedible
import com.example.rokola.dominio.ResultadoBusqueda
import com.example.rokola.dominio.TipoRespuestaSolicitud
import com.example.rokola.dominio.reglas.FiltroDuracion
import com.example.rokola.dominio.reglas.FiltroGenero
import com.example.rokola.dominio.reglas.FiltroPopularidad
import com.example.rokola.dominio.reglas.FiltrosBusqueda
import com.example.rokola.dominio.reglas.SIN_FILTROS
import com.example.rokola.ui.componentes.AccionAviso
import com.example.rokola.ui.componentes.Aviso
import com.example.rokola.ui.componentes.Esqueleto
import com.example.rokola.ui.componentes.Fichas
import com.example.rokola.ui.componentes.Icono
import com.example.rokola.ui.componentes.MarcoCliente
import com.example.rokola.ui.componentes.NombreIcono
import com.example.rokola.ui.componentes.TamanoTecla
import com.example.rokola.ui.componentes.TarjetaTitulo
import com.example.rokola.ui.componentes.Tecla
import com.example.rokola.ui.componentes.TeclaFija
import com.example.rokola.ui.componentes.TipoAviso
import com.example.rokola.ui.componentes.TituloDeSeccion
import com.example.rokola.ui.componentes.Vacio
import com.example.rokola.ui.componentes.VarianteTarjeta
import com.example.rokola.ui.componentes.VarianteTecla
import com.example.rokola.ui.textos.MOTIVO_SIN_CUPO
import com.example.rokola.ui.textos.T
import com.example.rokola.ui.textos.TextosBusqueda
import com.example.rokola.ui.textos.TextosFiltros
import com.example.rokola.ui.textos.TextosPedido
import com.example.rokola.ui.textos.cupo
import com.example.rokola.ui.textos.formatoDuracion
import com.example.rokola.ui.textos.formatoHora
import com.example.rokola.ui.textos.motivoCorto
import com.example.rokola.ui.textos.motivoLargo
import com.example.rokola.ui.textos.nombreGenero
import com.example.rokola.ui.textos.sinCupo
import com.example.rokola.ui.theme.Ambar
import com.example.rokola.ui.theme.Crema
import com.example.rokola.ui.theme.Crema2
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.Hueco
import com.example.rokola.ui.theme.Papel
import com.example.rokola.ui.theme.RadioTecla
import com.example.rokola.ui.theme.Tinta
import com.example.rokola.ui.theme.Tinta2
import com.example.rokola.viewmodel.AvisoBuscar
import com.example.rokola.viewmodel.Bienvenida
import com.example.rokola.viewmodel.Buscar
import com.example.rokola.viewmodel.EstadoPantalla
import com.example.rokola.viewmodel.ResultadosBusqueda
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId

private val Negro = Color(0xFF000000)

/** Un aviso del pedido; con [verCola] lleva el enlace «Ver cola». */
private data class AvisoPedido(val aviso: AvisoEnPantalla, val verCola: Boolean = false)

private val MOTIVOS_DE_ERROR = mapOf(
    CodigoError.BLOQUEADA to MotivoNoPedible.BLOQUEADA,
    CodigoError.NO_DISPONIBLE to MotivoNoPedible.NO_DISPONIBLE,
    CodigoError.NO_EMBEBIBLE to MotivoNoPedible.NO_EMBEBIBLE,
    CodigoError.MUY_LARGA to MotivoNoPedible.MUY_LARGA,
    CodigoError.REPRODUCIDA_HACE_POCO to MotivoNoPedible.REPRODUCIDA_HACE_POCO,
)

private fun avisoDePedidoFallido(titulo: String, error: ErrorRockola, maximo: Int, zona: ZoneId): AvisoEnPantalla {
    val motivo = MOTIVOS_DE_ERROR[error.codigo]
    return when {
        error.codigo == CodigoError.LIMITE_PEDIDOS -> AvisoEnPantalla(TipoAviso.INFO, sinCupo(maximo))
        error.codigo == CodigoError.SERVICIO_NO_DISPONIBLE -> AvisoEnPantalla(TipoAviso.INFO, TextosPedido.cerrado)
        motivo != null -> AvisoEnPantalla(TipoAviso.INFO, TextosPedido.rechazado(titulo, motivoLargo(motivo, error.disponibleDesde, zona)))
        else -> AvisoEnPantalla(TipoAviso.ERROR, TextosPedido.fallo(titulo))
    }
}

/**
 * Buscar, pedir y votar (prototipo 2, PB-07 a PB-09, PB-13 a PB-15, PB-21). Busca mientras se
 * escribe; lo que ya está en la cola se vota en vez de pedirse (RN-02); lo que no se puede
 * pedir dice por qué en lugar de un botón gris (RN-06, RN-07).
 */
@Composable
fun PantallaBuscar(
    estado: Buscar,
    bienvenida: EstadoPantalla<Bienvenida>,
    avisos: Flow<AvisoBuscar>,
    onTexto: (String) -> Unit,
    onReintentar: () -> Unit,
    onFiltros: (FiltrosBusqueda) -> Unit,
    onPedir: (ResultadoBusqueda) -> Unit,
    onVotar: (String) -> Unit,
    onVerCola: () -> Unit,
    onPolitica: () -> Unit,
    pie: @Composable () -> Unit,
) {
    val base = (bienvenida as? EstadoPantalla.Contenido)?.datos ?: (bienvenida as? EstadoPantalla.Vacio)?.datos
    val zona = base?.bar?.zonaHoraria ?: ZoneId.of("America/Bogota")
    val maximo = estado.pedidosMaximos ?: base?.bar?.pedidosMaximos ?: 3
    val restantes = estado.pedidosRestantes ?: maximo

    var aviso by remember { mutableStateOf<AvisoPedido?>(null) }
    // Lo que este teléfono acaba de pedir: se ve «Tuya» con su puesto mientras dure la búsqueda.
    val pedidas = remember { mutableStateMapOf<String, Int>() }
    var ultimoVoto by remember { mutableStateOf("") }
    var verFiltros by remember { mutableStateOf(false) }
    var pidiendoId by remember { mutableStateOf<String?>(null) }
    val maximoActual by rememberUpdatedState(maximo)
    val zonaActual by rememberUpdatedState(zona)

    LaunchedEffect(avisos) {
        avisos.collect { a ->
            aviso = when (a) {
                is AvisoBuscar.Pedida -> {
                    val r = a.respuesta
                    pidiendoId?.let { pedidas[it] = r.puesto }
                    val textos = if (r.resultado == TipoRespuestaSolicitud.CREADA) TextosPedido.entro(a.titulo, r.puesto, r.esperaSeg) else TextosPedido.eraVoto(a.titulo, r.puesto)
                    AvisoPedido(AvisoEnPantalla(TipoAviso.LUZ, textos), verCola = true)
                }
                is AvisoBuscar.PedidoFallido -> AvisoPedido(avisoDePedidoFallido(a.titulo, a.error, maximoActual, zonaActual))
                is AvisoBuscar.VotoFallido -> AvisoPedido(avisoDeVoto(a.error, ultimoVoto))
            }
        }
    }

    // Con los pedidos cerrados, la pantalla es la del prototipo 4 (como en la web).
    if (estado.servicio != null && !estado.aceptaPedidos && base != null) {
        MarcoCliente(
            apagada = true,
            sinConexion = !base.enVivo,
            titulo = base.bar.nombre,
            pie = pie,
            abajo = { EstadoCerradoAbajo(onVerCola, onPolitica) },
        ) { EstadoCerradoArriba(base.bar, base.ahora) }
        return
    }

    MarcoCliente(titulo = T.PIDE_UNA_CANCION, sinConexion = base?.enVivo == false, pie = pie, abajo = { EnlacePolitica(onPolitica) }) {
        TituloPlaca(T.PIDE_UNA_CANCION)
        Row(
            Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Fichas(libres = restantes, total = maximo)
            Text(cupo(restantes, maximo), style = Cuerpo.copy(fontSize = 14.sp), color = Crema2)
        }

        Buscador(estado.texto, onTexto, Modifier.padding(top = 16.dp))

        aviso?.let { a ->
            Aviso(
                Modifier.padding(top = 14.dp),
                tipo = a.aviso.tipo,
                titulo = a.aviso.textos.titulo,
                meta = a.aviso.textos.meta,
                accion = if (a.verCola) AccionAviso(T.VER_COLA, onVerCola) else AccionAviso(T.CERRAR) { aviso = null },
            )
        }
        if (restantes <= 0 && aviso == null) {
            val t = sinCupo(maximo)
            Aviso(Modifier.padding(top = 14.dp), tipo = TipoAviso.INFO, titulo = t.titulo, meta = t.meta)
        }

        val resultados = estado.resultados
        when {
            resultados == null -> Text(
                T.BUSCAR_VACIO,
                Modifier.padding(top = 20.dp).fillMaxWidth(),
                style = Cuerpo.copy(fontSize = 15.sp),
                color = Crema2,
                textAlign = TextAlign.Center,
            )
            resultados is EstadoPantalla.Error -> {
                val t = if (resultados.error.codigo == CodigoError.SIN_CONEXION) TextosBusqueda.sinRed else TextosBusqueda.youtubeCaido
                Aviso(Modifier.padding(top = 20.dp), tipo = TipoAviso.ERROR, titulo = t.titulo, meta = t.meta, accion = AccionAviso(T.REINTENTAR, onReintentar))
            }
            resultados is EstadoPantalla.Cargando -> Column(
                Modifier.padding(top = 20.dp).semantics { contentDescription = "Buscando…"; liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { repeat(3) { Esqueleto(Modifier.fillMaxWidth().height(56.dp)) } }
            resultados is EstadoPantalla.Vacio -> {
                val t = TextosBusqueda.sinResultados(resultados.datos.consulta)
                Vacio(t.meta, Modifier.padding(top = 20.dp), destacado = "${t.titulo}.")
            }
            resultados is EstadoPantalla.Contenido -> Resultados(
                datos = resultados.datos,
                estado = estado,
                pedidas = pedidas,
                sinCupo = restantes <= 0,
                zona = zona,
                verFiltros = verFiltros,
                onVerFiltros = { verFiltros = !verFiltros },
                onFiltros = onFiltros,
                onPedir = { aviso = null; pidiendoId = it.cancion.videoId; onPedir(it) },
                onVotar = { titulo, id -> ultimoVoto = titulo; onVotar(id) },
            )
        }
    }
}

/** El buscador: papel hundido en la madera, con su lupa y su «borrar». */
@Composable
private fun Buscador(texto: String, onTexto: (String) -> Unit, modifier: Modifier) {
    var conFoco by remember { mutableStateOf(false) }
    val foco = LocalFocusManager.current
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .drawBehind {
                // :focus-within → 3 dp ámbar separado 3 dp.
                if (conFoco) {
                    val g = 3.dp.toPx()
                    val s = 3.dp.toPx() + g / 2
                    drawRoundRect(
                        Ambar,
                        topLeft = Offset(-s, -s),
                        size = Size(size.width + 2 * s, size.height + 2 * s),
                        cornerRadius = CornerRadius(10.dp.toPx() + s),
                        style = Stroke(g),
                    )
                }
            }
            .dropShadow(RadioTecla, Shadow(radius = 0.dp, color = Negro.copy(alpha = 0.25f), spread = 3.dp))
            .background(Papel, RadioTecla)
            .innerShadow(RadioTecla, Shadow(radius = 5.dp, color = Negro.copy(alpha = 0.28f), offset = androidx.compose.ui.unit.DpOffset(0.dp, 2.dp)))
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icono(NombreIcono.BUSCAR, tamano = 20.dp, color = Tinta2)
        val estilo = Cuerpo.copy(fontSize = 17.sp, fontWeight = FontWeight.W600, color = Tinta)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (texto.isEmpty()) Text(T.BUSCAR_MARCADOR, style = estilo.copy(fontWeight = FontWeight.W500), color = Tinta2)
            BasicTextField(
                value = texto,
                onValueChange = { if (it.length <= 80) onTexto(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { conFoco = it.isFocused }
                    .semantics { contentDescription = T.BUSCAR_ETIQUETA },
                textStyle = estilo,
                singleLine = true,
                cursorBrush = SolidColor(Tinta),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { foco.clearFocus() }),
            )
        }
        if (texto.isNotEmpty()) {
            Box(
                Modifier
                    .size(44.dp)
                    .clickable(role = Role.Button, onClickLabel = T.BORRAR_BUSQUEDA) { onTexto("") }
                    .semantics { contentDescription = T.BORRAR_BUSQUEDA },
                contentAlignment = Alignment.Center,
            ) { Icono(NombreIcono.BORRAR, tamano = 18.dp, color = Tinta2) }
        }
    }
}

@Composable
private fun Resultados(
    datos: ResultadosBusqueda,
    estado: Buscar,
    pedidas: Map<String, Int>,
    sinCupo: Boolean,
    zona: ZoneId,
    verFiltros: Boolean,
    onVerFiltros: () -> Unit,
    onFiltros: (FiltrosBusqueda) -> Unit,
    onPedir: (ResultadoBusqueda) -> Unit,
    onVotar: (titulo: String, id: String) -> Unit,
) {
    if (datos.desdeCache) Aviso(Modifier.padding(top = 14.dp), tipo = TipoAviso.INFO, meta = TextosBusqueda.DESDE_CACHE)

    if (datos.enCola.isNotEmpty()) {
        Row(
            Modifier.padding(top = 18.dp, bottom = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            TituloDeSeccion(T.YA_EN_LA_COLA)
            Text(T.VOTA_EN_VEZ, style = Cuerpo.copy(fontSize = 13.sp), color = Crema2)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            datos.enCola.forEach { f ->
                FilaCancion(
                    item = f.item,
                    tecla = f.tecla,
                    puedeVotar = estado.aceptaPedidos,
                    ocupada = f.ocupada,
                    onVotar = { onVotar(it.cancion.titulo, it.solicitudId) },
                )
            }
        }
    }

    if (datos.totalSinFiltrar > 0) {
        Row(
            Modifier.padding(top = 18.dp, bottom = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TituloDeSeccion(T.RESULTADOS_YOUTUBE)
            // Abierta, la tecla queda hundida: se ve qué abre y cómo cerrarlo.
            Tecla(
                TextosFiltros.boton(estado.filtrosActivos),
                onVerFiltros,
                variante = if (verFiltros) VarianteTecla.HUNDIDA else VarianteTecla.NORMAL,
                tamano = TamanoTecla.CHICA,
                expandida = verFiltros,
            )
        }
        if (verFiltros) PanelFiltros(estado.filtros, datos.generos, onFiltros)
        if (datos.resultados.isEmpty()) {
            Vacio(TextosFiltros.NINGUNO, abajo = { Tecla(TextosFiltros.QUITAR, { onFiltros(SIN_FILTROS) }, tamano = TamanoTecla.CHICA) })
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            datos.resultados.forEach { r ->
                FilaResultado(r, pedidas[r.cancion.videoId], sinCupo, estado.pidiendo == r.cancion.videoId, zona, onPedir)
            }
        }
    }
}

@Composable
private fun FilaResultado(
    resultado: ResultadoBusqueda,
    puestoPedida: Int?,
    sinCupo: Boolean,
    ocupada: Boolean,
    zona: ZoneId,
    onPedir: (ResultadoBusqueda) -> Unit,
) {
    val c = resultado.cancion
    val duracion = formatoDuracion(c.duracionSeg)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            puestoPedida != null -> {
                TarjetaTitulo(c.titulo, "${c.artista}, puesto $puestoPedida", Modifier.weight(1f), variante = VarianteTarjeta.TUYA, duracion = duracion, miniatura = c.miniaturaUrl)
                TeclaFija("Tuya", VarianteTecla.TUYA)
            }
            !resultado.pedible && resultado.motivo != null -> {
                val artista = if (resultado.motivo == MotivoNoPedible.REPRODUCIDA_HACE_POCO && resultado.disponibleDesde != null) {
                    "${c.artista}, se puede pedir desde las ${formatoHora(resultado.disponibleDesde, zona)}"
                } else c.artista
                TarjetaTitulo(c.titulo, artista, Modifier.weight(1f), variante = VarianteTarjeta.APAGADA, duracion = duracion, miniatura = c.miniaturaUrl)
                Motivo(motivoCorto(resultado.motivo))
            }
            else -> {
                TarjetaTitulo(c.titulo, c.artista, Modifier.weight(1f), duracion = duracion, miniatura = c.miniaturaUrl)
                if (sinCupo) Motivo(MOTIVO_SIN_CUPO)
                else Tecla("Pedir", { onPedir(resultado) }, variante = VarianteTecla.CEREZA, etiqueta = "Pedir ${c.titulo}", ocupada = ocupada)
            }
        }
    }
}

/** Por qué no se puede pedir, en la columna de la tecla (76 dp). */
@Composable
private fun Motivo(texto: String) {
    Text(texto, Modifier.width(76.dp), style = Cuerpo.copy(fontSize = 13.sp, lineHeight = 1.3.em), color = Crema2, textAlign = TextAlign.Center)
}

/** Los filtros, en una caja hundida bajo el título (RF-055). Cada opción es una tecla. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanelFiltros(filtros: FiltrosBusqueda, generos: List<String?>, onCambio: (FiltrosBusqueda) -> Unit) {
    val opcionesGenero = buildList {
        add(FiltroGenero.Todos to TextosFiltros.TODOS)
        val elegido = filtros.genero
        if (elegido is FiltroGenero.Uno && elegido.genero !in generos) add(elegido to nombreGenero(elegido.genero))
        generos.forEach { g -> add((if (g == null) FiltroGenero.SinClasificar else FiltroGenero.Uno(g)) to nombreGenero(g)) }
        if (elegido == FiltroGenero.SinClasificar && null !in generos) add(FiltroGenero.SinClasificar to nombreGenero(null))
    }
    Column(
        Modifier
            .padding(bottom = 14.dp)
            .fillMaxWidth()
            .background(Hueco, RadioTecla)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GrupoFiltro(TextosFiltros.GENERO, opcionesGenero, filtros.genero) { onCambio(filtros.copy(genero = it)) }
        GrupoFiltro(
            TextosFiltros.DURACION,
            listOf(
                FiltroDuracion.TODAS to TextosFiltros.CUALQUIERA,
                FiltroDuracion.CORTA to TextosFiltros.CORTA,
                FiltroDuracion.MEDIA to TextosFiltros.MEDIA,
                FiltroDuracion.LARGA to TextosFiltros.LARGA,
            ),
            filtros.duracion,
        ) { onCambio(filtros.copy(duracion = it)) }
        GrupoFiltro(
            TextosFiltros.POPULARIDAD,
            listOf(
                FiltroPopularidad.TODAS to TextosFiltros.TODAS,
                FiltroPopularidad.PEDIDAS to TextosFiltros.PEDIDAS,
                FiltroPopularidad.MUY_PEDIDAS to TextosFiltros.MUY_PEDIDAS,
            ),
            filtros.popularidad,
        ) { onCambio(filtros.copy(popularidad = it)) }
        if (filtros != SIN_FILTROS) Tecla(TextosFiltros.QUITAR, { onCambio(SIN_FILTROS) }, tamano = TamanoTecla.CHICA)
    }
}

/** Opciones como teclas: la elegida queda hundida (.grupo-teclas del panel web). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> GrupoFiltro(etiqueta: String, opciones: List<Pair<T, String>>, elegido: T, onElegir: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(etiqueta, style = Cuerpo.copy(fontSize = 14.sp, fontWeight = FontWeight.W600), color = Crema)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            opciones.forEach { (valor, texto) ->
                val activa = valor == elegido
                Tecla(
                    texto,
                    { onElegir(valor) },
                    variante = if (activa) VarianteTecla.HUNDIDA else VarianteTecla.NORMAL,
                    tamano = TamanoTecla.CHICA,
                    etiqueta = if (activa) "$etiqueta: $texto, elegido" else "$etiqueta: $texto",
                )
            }
        }
    }
}
