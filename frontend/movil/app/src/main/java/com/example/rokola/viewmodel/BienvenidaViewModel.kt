package com.example.rokola.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rokola.dominio.Ahora
import com.example.rokola.dominio.Bar
import com.example.rokola.dominio.Destacada
import com.example.rokola.dominio.RepositorioRockola
import com.example.rokola.dominio.Yo
import com.example.rokola.dominio.reglas.aceptaPedidos
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Lo que muestra la bienvenida (prototipo cliente-1-bienvenida). */
data class Bienvenida(
    val bar: Bar,
    val ahora: Ahora,
    /** «Lo más votado de la noche». */
    val destacadas: List<Destacada>,
    val yo: Yo,
    /** false = «Sin conexión»: la luz se apaga (rework del doc 05). */
    val enVivo: Boolean,
) {
    val aceptaPedidos: Boolean get() = aceptaPedidos(ahora.servicio.estado)

    /** Primer ingreso: falta mostrar el aviso de datos (RF-004). */
    val debeVerAviso: Boolean get() = yo.avisoVistoEn == null
}

/**
 * Bienvenida: el bar, su estado (abierto, en pausa, mantenimiento, fuera de horario), lo que
 * suena y lo más votado. Es la entrada desde el QR, así que registra el teléfono (RF-002).
 * Vacía cuando no suena nada y aún no hay canciones de la noche.
 */
class BienvenidaViewModel(private val repositorio: RepositorioRockola) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoPantalla<Bienvenida>>(EstadoPantalla.Cargando)
    val estado: StateFlow<EstadoPantalla<Bienvenida>> = _estado.asStateFlow()

    private var trabajo: Job? = null

    init {
        cargar()
    }

    fun reintentar() = cargar()

    /** La pantalla mostró el aviso de datos: se anota una sola vez. */
    fun avisoMostrado() {
        val actual = _estado.value.datos ?: return
        if (!actual.debeVerAviso) return
        viewModelScope.launch {
            repositorio.anotarAvisoVisto()
            repositorio.yo().valorONulo()?.let { yo -> publicar(actual.copy(yo = yo)) }
        }
    }

    private fun cargar() {
        trabajo?.cancel()
        _estado.value = EstadoPantalla.Cargando
        trabajo = viewModelScope.launch {
            val bar = async { repositorio.bar() }
            val yo = async { repositorio.registrarDispositivo() }
            val destacadas = async { repositorio.destacadas() }
            val elBar = bar.await().valorO { _estado.value = EstadoPantalla.Error(it); return@launch }
            var miYo = yo.await().valorO { _estado.value = EstadoPantalla.Error(it); return@launch }
            var lasDestacadas = destacadas.await().valorO { _estado.value = EstadoPantalla.Error(it); return@launch }

            var version: Int? = null
            repositorio.ahoraEnVivo().collect { evento ->
                when (evento) {
                    is EventoEnVivo.Foto -> {
                        if (version != null && evento.ahora.version != version) {
                            // Cambió la noche: los votos y el cupo pueden ser otros.
                            repositorio.destacadas().valorONulo()?.let { lasDestacadas = it }
                            repositorio.yo().valorONulo()?.let { miYo = it }
                        }
                        version = evento.ahora.version
                        publicar(Bienvenida(elBar, evento.ahora, lasDestacadas, miYo, evento.enVivo))
                    }
                    is EventoEnVivo.Fallo -> {
                        val actual = _estado.value.datos
                        _estado.value = if (actual == null) EstadoPantalla.Error(evento.error) else contenidoDe(actual.copy(enVivo = false))
                    }
                }
            }
        }
    }

    private fun publicar(datos: Bienvenida) {
        _estado.value = contenidoDe(datos)
    }

    private fun contenidoDe(datos: Bienvenida) =
        contenidoOVacio(datos, vacio = datos.ahora.reproduccion == null && datos.destacadas.isEmpty())

    companion object {
        val Fabrica = fabrica { BienvenidaViewModel(it) }
    }
}
