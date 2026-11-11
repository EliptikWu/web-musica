package com.example.rokola.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rokola.dominio.Ahora
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.ItemCola
import com.example.rokola.dominio.ListaCasaResumen
import com.example.rokola.dominio.MiSolicitud
import com.example.rokola.dominio.RepositorioRockola
import com.example.rokola.dominio.Reproduccion
import com.example.rokola.dominio.Resultado
import com.example.rokola.dominio.Servicio
import com.example.rokola.dominio.Yo
import com.example.rokola.dominio.reglas.Desempate
import com.example.rokola.dominio.reglas.EstadoTecla
import com.example.rokola.dominio.reglas.aceptaPedidos
import com.example.rokola.dominio.reglas.ejemploDesempate
import com.example.rokola.dominio.reglas.estadoTecla
import com.example.rokola.dominio.reglas.puestosSubidos
import com.example.rokola.dominio.reglas.renumerar
import com.example.rokola.dominio.reglas.solicitudesPropiasPendientes
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant

/** Una canción de la cola con lo que el teléfono necesita para dibujarla. */
data class FilaCola(
    val item: ItemCola,
    val tecla: EstadoTecla,
    /** «Subió N puestos»; 0 si no subió. */
    val subio: Int,
    /** Espera estimada en segundos («en unos 3 min»). */
    val esperaSeg: Double,
    /** El voto de esta canción se está enviando: la tecla no se vuelve a pulsar (RNF-008). */
    val ocupada: Boolean,
)

/** Lo que muestra la cola (prototipo cliente-3-cola), con «Tus pedidos» al final. */
data class Cola(
    val servicio: Servicio,
    val reproduccion: Reproduccion?,
    val servidorAhora: Instant,
    val filas: List<FilaCola>,
    /** La primera canción propia en la cola, para «Tu canción va de #3». */
    val tuCancion: FilaCola?,
    /** «Con los mismos votos, suena primero la que se pidió antes…» */
    val desempate: Desempate?,
    /** Lo que suena con la cola vacía. */
    val listaCasa: ListaCasaResumen?,
    /** «Tus pedidos» (RF-013), de la más nueva a la más vieja. */
    val misSolicitudes: List<MiSolicitud>,
    val pedidosRestantes: Int,
    val pedidosMaximos: Int,
    /** false = «Sin conexión». */
    val enVivo: Boolean,
) {
    val puedeVotar: Boolean get() = aceptaPedidos(servicio.estado)
}

/** Avisos de una sola vez (la pantalla los muestra y los olvida). */
sealed interface AvisoCola {
    data class VotoFallido(val error: ErrorRockola) : AvisoCola

    /** El voto pasó a [hacia]: el foco va a su nueva «Votaste». */
    data class VotoMovido(val hacia: String) : AvisoCola
}

/**
 * La cola, ordenada por votos (RN-05), con el voto optimista de la web (RF-012): la tecla se
 * hunde y la cola se reordena al instante; si el servidor dice que no, se deshace.
 * Vacía cuando no hay canciones en espera (suena la lista de la casa).
 */
class ColaViewModel(private val repositorio: RepositorioRockola) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoPantalla<Cola>>(EstadoPantalla.Cargando)
    val estado: StateFlow<EstadoPantalla<Cola>> = _estado.asStateFlow()

    private val _avisos = Channel<AvisoCola>(Channel.BUFFERED)
    val avisos: Flow<AvisoCola> = _avisos.receiveAsFlow()

    private var ahora: Ahora? = null
    private var yo: Yo? = null
    private var enVivo = true
    /** Votos enviados sin respuesta: true = se puso, false = se quitó (al moverlo). */
    private val votosEnVuelo = mutableMapOf<String, Boolean>()
    private var trabajo: Job? = null

    init {
        cargar()
    }

    fun reintentar() = cargar()

    /** Canciones a las que se puede pasar el voto de [desde]: las que aún no votó. */
    fun destinosParaMoverVoto(desde: String): List<FilaCola> =
        _estado.value.datos?.filas.orEmpty().filter { it.tecla == EstadoTecla.VOTAR && it.item.solicitudId != desde }

    fun votar(solicitudId: String) {
        val antes = ahora ?: return
        val miYo = yo ?: return
        if (!aceptaPedidos(antes.servicio.estado) || solicitudId in votosEnVuelo) return
        if (estadoTecla(solicitudId, miYo) != EstadoTecla.VOTAR) return

        ahora = antes.copy(cola = renumerar(antes.cola.map { if (it.solicitudId == solicitudId) it.copy(votosTotal = it.votosTotal + 1) else it }))
        yo = miYo.copy(votos = miYo.votos + solicitudId)
        votosEnVuelo[solicitudId] = true
        publicar()

        viewModelScope.launch {
            val r = repositorio.votar(solicitudId)
            votosEnVuelo -= solicitudId
            if (r is Resultado.Fallo) {
                // Se deshace solo si no llegó nada más nuevo; si llegó, se pregunta al servidor.
                if (ahora?.version == antes.version) ahora = antes else refrescarAhora()
                yo = yo?.let { y -> y.copy(votos = y.votos - solicitudId) }
                _avisos.send(AvisoCola.VotoFallido(r.error))
            }
            refrescarYo()
        }
    }

    /** Mover el voto (RF-014): la cola se reordena al instante; si falla, se pregunta al servidor. */
    fun moverVoto(desde: String, hacia: String) {
        val antes = ahora ?: return
        val miYo = yo ?: return
        if (!aceptaPedidos(antes.servicio.estado) || desde in votosEnVuelo || hacia in votosEnVuelo) return

        ahora = antes.copy(
            cola = renumerar(
                antes.cola.map {
                    when (it.solicitudId) {
                        desde -> it.copy(votosTotal = maxOf(0, it.votosTotal - 1))
                        hacia -> it.copy(votosTotal = it.votosTotal + 1)
                        else -> it
                    }
                },
            ),
        )
        yo = miYo.copy(votos = miYo.votos - desde + hacia)
        votosEnVuelo[desde] = false
        votosEnVuelo[hacia] = true
        publicar()

        viewModelScope.launch {
            val r = repositorio.moverVoto(desde, hacia)
            votosEnVuelo -= setOf(desde, hacia)
            when (r) {
                is Resultado.Exito -> _avisos.send(AvisoCola.VotoMovido(hacia))
                is Resultado.Fallo -> {
                    refrescarAhora()
                    _avisos.send(AvisoCola.VotoFallido(r.error))
                }
            }
            refrescarYo()
        }
    }

    private fun cargar() {
        trabajo?.cancel()
        ahora = null
        yo = null
        _estado.value = EstadoPantalla.Cargando
        trabajo = viewModelScope.launch {
            yo = repositorio.yo().valorO { _estado.value = EstadoPantalla.Error(it); return@launch }
            repositorio.ahoraEnVivo().collect { evento ->
                when (evento) {
                    is EventoEnVivo.Foto -> {
                        val cambio = ahora != null && ahora?.version != evento.ahora.version
                        // Sin tiempo real llega la misma versión cada 3 s: no pisa un voto en vuelo.
                        if (cambio || votosEnVuelo.isEmpty()) ahora = evento.ahora
                        enVivo = evento.enVivo
                        if (cambio) refrescarYo() else publicar()
                    }
                    is EventoEnVivo.Fallo -> {
                        enVivo = false
                        if (ahora == null) _estado.value = EstadoPantalla.Error(evento.error) else publicar()
                    }
                }
            }
        }
    }

    private suspend fun refrescarAhora() {
        repositorio.ahora().valorONulo()?.let { ahora = it }
        publicar()
    }

    private suspend fun refrescarYo() {
        repositorio.yo().valorONulo()?.let { nuevo ->
            // Una respuesta vieja no deshace un voto que sigue en vuelo.
            val puestos = votosEnVuelo.filterValues { it }.keys
            yo = nuevo.copy(votos = (nuevo.votos.filter { votosEnVuelo[it] != false } + puestos).distinct())
        }
        publicar()
    }

    private fun publicar() {
        val a = ahora ?: return
        val y = yo ?: return
        val filas = a.cola.map { item ->
            FilaCola(
                item = item,
                tecla = estadoTecla(item.solicitudId, y),
                subio = puestosSubidos(item),
                esperaSeg = item.esperaSeg.toDouble(),
                ocupada = item.solicitudId in votosEnVuelo,
            )
        }
        val propias = solicitudesPropiasPendientes(y)
        val datos = Cola(
            servicio = a.servicio,
            reproduccion = a.reproduccion,
            servidorAhora = a.servidorAhora,
            filas = filas,
            tuCancion = filas.firstOrNull { it.item.solicitudId in propias },
            desempate = ejemploDesempate(a.cola, propias),
            listaCasa = a.listaCasa,
            misSolicitudes = y.solicitudes,
            pedidosRestantes = y.pedidosRestantes,
            pedidosMaximos = y.pedidosMaximos,
            enVivo = enVivo,
        )
        _estado.value = contenidoOVacio(datos, vacio = filas.isEmpty())
    }

    companion object {
        val Fabrica = fabrica { ColaViewModel(it) }
    }
}
