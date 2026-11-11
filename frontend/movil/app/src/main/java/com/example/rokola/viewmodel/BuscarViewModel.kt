package com.example.rokola.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rokola.dominio.Busqueda
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.ItemCola
import com.example.rokola.dominio.RepositorioRockola
import com.example.rokola.dominio.RespuestaSolicitud
import com.example.rokola.dominio.Resultado
import com.example.rokola.dominio.ResultadoBusqueda
import com.example.rokola.dominio.Servicio
import com.example.rokola.dominio.Yo
import com.example.rokola.dominio.reglas.EstadoTecla
import com.example.rokola.dominio.reglas.FiltrosBusqueda
import com.example.rokola.dominio.reglas.SIN_FILTROS
import com.example.rokola.dominio.reglas.aceptaPedidos
import com.example.rokola.dominio.reglas.cuantosFiltros
import com.example.rokola.dominio.reglas.estadoTecla
import com.example.rokola.dominio.reglas.filtrarResultados
import com.example.rokola.dominio.reglas.generosDe
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Una coincidencia que ya está en la cola: se vota en lugar de pedirse. */
data class FilaEnCola(val item: ItemCola, val tecla: EstadoTecla, val ocupada: Boolean)

/** Lo que llegó de la búsqueda, ya filtrado en el teléfono (RF-055). */
data class ResultadosBusqueda(
    val consulta: String,
    val enCola: List<FilaEnCola>,
    /** Los resultados con los filtros aplicados. */
    val resultados: List<ResultadoBusqueda>,
    /** Cuántos llegaron antes de filtrar: con 0 visibles y N > 0, «Quitar filtros». */
    val totalSinFiltrar: Int,
    val generos: List<String?>,
    /** Vienen de la caché del servidor porque se agotó la cuota de YouTube. */
    val desdeCache: Boolean,
)

/** La pantalla Buscar (prototipo cliente-2-buscar). */
data class Buscar(
    val texto: String,
    /** null = sin consulta (menos de 2 letras). */
    val resultados: EstadoPantalla<ResultadosBusqueda>?,
    /** Hay una búsqueda en curso; los resultados anteriores siguen a la vista. */
    val buscando: Boolean,
    val filtros: FiltrosBusqueda,
    val filtrosActivos: Int,
    /** Se está pidiendo esta canción: la tecla no se vuelve a pulsar. */
    val pidiendo: String?,
    val servicio: Servicio?,
    val pedidosRestantes: Int?,
    val pedidosMaximos: Int?,
) {
    val aceptaPedidos: Boolean get() = servicio?.let { aceptaPedidos(it.estado) } ?: false
}

sealed interface AvisoBuscar {
    /** «Pediste…» o, si ya estaba, «Votaste por…» (RN-02). */
    data class Pedida(val titulo: String, val respuesta: RespuestaSolicitud) : AvisoBuscar
    data class PedidoFallido(val titulo: String, val error: ErrorRockola) : AvisoBuscar
    data class VotoFallido(val error: ErrorRockola) : AvisoBuscar
}

/**
 * Buscar mientras se escribe (RF-005): desde 2 letras y 300 ms después de la última tecla,
 * como la web. Estados: sin consulta, buscando, resultados, sin resultados (vacío), error
 * (YouTube caído), desde caché, después de pedir y 0 de 3.
 */
class BuscarViewModel(private val repositorio: RepositorioRockola) : ViewModel() {

    private val _estado = MutableStateFlow(
        Buscar(
            texto = "",
            resultados = null,
            buscando = false,
            filtros = SIN_FILTROS,
            filtrosActivos = 0,
            pidiendo = null,
            servicio = null,
            pedidosRestantes = null,
            pedidosMaximos = null,
        ),
    )
    val estado: StateFlow<Buscar> = _estado.asStateFlow()

    private val _avisos = Channel<AvisoBuscar>(Channel.BUFFERED)
    val avisos: Flow<AvisoBuscar> = _avisos.receiveAsFlow()

    private var busqueda: Busqueda? = null
    private var yo: Yo? = null
    private val votosEnVuelo = mutableSetOf<String>()
    private var espera: Job? = null

    init {
        viewModelScope.launch { refrescarYo() }
        viewModelScope.launch {
            repositorio.ahoraEnVivo().collect { evento ->
                if (evento is EventoEnVivo.Foto) _estado.update { it.copy(servicio = evento.ahora.servicio) }
            }
        }
    }

    fun cambiarTexto(texto: String) {
        _estado.update { it.copy(texto = texto) }
        espera?.cancel()
        val consulta = texto.trim()
        if (consulta.length < MIN_LETRAS) {
            busqueda = null
            _estado.update { it.copy(resultados = null, buscando = false) }
            return
        }
        espera = viewModelScope.launch {
            delay(ESPERA_TECLAS_MS)
            buscarAhora(consulta)
        }
    }

    fun reintentar() {
        val consulta = _estado.value.texto.trim()
        if (consulta.length >= MIN_LETRAS) {
            espera?.cancel()
            espera = viewModelScope.launch { buscarAhora(consulta) }
        }
    }

    fun cambiarFiltros(filtros: FiltrosBusqueda) {
        _estado.update { it.copy(filtros = filtros, filtrosActivos = cuantosFiltros(filtros)) }
        publicarResultados()
    }

    fun quitarFiltros() = cambiarFiltros(SIN_FILTROS)

    fun pedir(resultado: ResultadoBusqueda) {
        if (_estado.value.pidiendo != null || !resultado.pedible) return
        _estado.update { it.copy(pidiendo = resultado.cancion.videoId) }
        viewModelScope.launch {
            when (val r = repositorio.pedir(resultado.cancion.videoId)) {
                is Resultado.Exito -> _avisos.send(AvisoBuscar.Pedida(resultado.cancion.titulo, r.valor))
                is Resultado.Fallo -> _avisos.send(AvisoBuscar.PedidoFallido(resultado.cancion.titulo, r.error))
            }
            _estado.update { it.copy(pidiendo = null) }
            refrescarYo()
            // Lo pedido pasa a «ya en la cola»: se vuelve a buscar lo mismo.
            busqueda?.let { buscarAhora(it.consulta) }
        }
    }

    /** Votar una que ya está en la cola, desde los resultados. */
    fun votar(solicitudId: String) {
        val miYo = yo ?: return
        if (!_estado.value.aceptaPedidos || solicitudId in votosEnVuelo) return
        if (estadoTecla(solicitudId, miYo) != EstadoTecla.VOTAR) return
        votosEnVuelo += solicitudId
        yo = miYo.copy(votos = miYo.votos + solicitudId)
        busqueda = busqueda?.let { b ->
            b.copy(enCola = b.enCola.map { if (it.solicitudId == solicitudId) it.copy(votosTotal = it.votosTotal + 1) else it })
        }
        publicarResultados()
        viewModelScope.launch {
            val r = repositorio.votar(solicitudId)
            votosEnVuelo -= solicitudId
            if (r is Resultado.Fallo) _avisos.send(AvisoBuscar.VotoFallido(r.error))
            refrescarYo()
            busqueda?.let { buscarAhora(it.consulta) }
        }
    }

    private suspend fun buscarAhora(consulta: String) {
        _estado.update { actual ->
            // Mientras llega la respuesta, los resultados anteriores siguen a la vista.
            actual.copy(buscando = true, resultados = actual.resultados?.takeIf { it !is EstadoPantalla.Error } ?: EstadoPantalla.Cargando)
        }
        when (val r = repositorio.buscar(consulta)) {
            is Resultado.Exito -> {
                busqueda = r.valor
                publicarResultados()
            }
            is Resultado.Fallo -> {
                busqueda = null
                _estado.update { it.copy(resultados = EstadoPantalla.Error(r.error)) }
            }
        }
        _estado.update { it.copy(buscando = false) }
    }

    private suspend fun refrescarYo() {
        repositorio.yo().valorONulo()?.let { nuevo ->
            yo = nuevo.copy(votos = (nuevo.votos + votosEnVuelo).distinct())
            _estado.update { it.copy(pedidosRestantes = nuevo.pedidosRestantes, pedidosMaximos = nuevo.pedidosMaximos) }
        }
        publicarResultados()
    }

    private fun publicarResultados() {
        val b = busqueda ?: return
        val filtros = _estado.value.filtros
        val datos = ResultadosBusqueda(
            consulta = b.consulta,
            enCola = b.enCola.map { FilaEnCola(it, estadoTecla(it.solicitudId, yo), it.solicitudId in votosEnVuelo) },
            resultados = filtrarResultados(b.resultados, filtros),
            totalSinFiltrar = b.resultados.size,
            generos = generosDe(b.resultados),
            desdeCache = b.desdeCache,
        )
        // Vacío solo si no llegó nada; si los filtros lo esconden todo, es contenido con «Quitar filtros».
        _estado.update { it.copy(resultados = contenidoOVacio(datos, vacio = b.enCola.isEmpty() && b.resultados.isEmpty())) }
    }

    companion object {
        const val MIN_LETRAS = 2
        const val ESPERA_TECLAS_MS = 300L
        val Fabrica = fabrica { BuscarViewModel(it) }
    }
}
