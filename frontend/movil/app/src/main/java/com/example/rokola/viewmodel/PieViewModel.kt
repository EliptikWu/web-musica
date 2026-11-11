package com.example.rokola.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rokola.dominio.RepositorioRockola
import com.example.rokola.dominio.Reproduccion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/** Lo que muestra el pie: lo que suena y si la conexión en vivo sigue (PieCliente.tsx). */
data class Pie(val reproduccion: Reproduccion?, val servidorAhora: Instant, val enVivo: Boolean)

/** El mini reproductor del pie: escucha la foto en vivo y se queda solo con lo que suena. */
class PieViewModel(private val repositorio: RepositorioRockola) : ViewModel() {

    private val _estado = MutableStateFlow<Pie?>(null)
    val estado: StateFlow<Pie?> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            repositorio.ahoraEnVivo().collect { evento ->
                when (evento) {
                    is EventoEnVivo.Foto -> _estado.value = Pie(evento.ahora.reproduccion, evento.ahora.servidorAhora, evento.enVivo)
                    is EventoEnVivo.Fallo -> _estado.value = _estado.value?.copy(enVivo = false)
                }
            }
        }
    }

    companion object {
        val Fabrica = fabrica { PieViewModel(it) }
    }
}
