package com.example.rokola.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rokola.Contenedor
import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.FORMATO_SLUG
import com.example.rokola.dominio.RepositorioRockola
import com.example.rokola.dominio.Resultado
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Un enlace que abre la app: el del QR (`/q/{token}`) o el de un bar (`/b/{slug}/…`), como en la web. */
sealed interface Enlace {
    data class Qr(val token: String) : Enlace
    data class Bar(val slug: String) : Enlace
}

/** Un token de QR: letras, números, guion y guion bajo (nada de `.` ni `..` que muevan la ruta). */
private val FORMATO_TOKEN = Regex("^[A-Za-z0-9_-]{1,128}$")

/**
 * Lee la ruta de `https://<dominio>/q/<token>` o `/b/<slug>`; cualquier otra, o con un token o
 * un slug fuera de forma, no es un enlace de Rockola. El esquema y el dominio los revisa quien
 * llama (MainActivity): esto solo ve la ruta ya decodificada.
 */
fun leerEnlace(segmentos: List<String>): Enlace? = when {
    segmentos.size == 2 && segmentos[0] == "q" && FORMATO_TOKEN.matches(segmentos[1]) -> Enlace.Qr(segmentos[1])
    segmentos.size >= 2 && segmentos[0] == "b" && FORMATO_SLUG.matches(segmentos[1]) -> Enlace.Bar(segmentos[1])
    else -> null
}

/** Antes de las pantallas del bar: a cuál se entra, o por qué no se puede. */
sealed interface Entrada {
    data object Abriendo : Entrada
    data class Lista(val slug: String) : Entrada
    /** Se abrió la app sin QR y no recuerda ningún bar. */
    data object SinBar : Entrada
    /** [puedeVolver]: hay un bar recordado al que regresar (un QR viejo no deja a nadie atascado). */
    data class QrReemplazado(val puedeVolver: Boolean) : Entrada
    data class BarNoEncontrado(val puedeVolver: Boolean) : Entrada
    /** No hubo respuesta; [enlace] se vuelve a intentar con «Reintentar». */
    data class Fallo(val enlace: Enlace) : Entrada
}

/**
 * Entrada por QR (RF-001, RF-035; apps/web/components/cliente/ResolverQr.tsx). El QR lleva un
 * token, no el bar, para poder invalidarlo al regenerarlo: se resuelve y se recuerda el bar
 * para la próxima vez. Un bar que no existe no reemplaza al que se recordaba.
 *
 * [barPorDefecto] es el de la simulación («demo»): sin backend no hace falta un QR para probar.
 */
class EntradaViewModel(
    private val repositorio: RepositorioRockola,
    private val leerSlug: () -> String?,
    private val guardarSlug: (String?) -> Unit,
    private val barPorDefecto: String?,
) : ViewModel() {

    private val _estado = MutableStateFlow<Entrada>(Entrada.Abriendo)
    val estado: StateFlow<Entrada> = _estado.asStateFlow()

    private var trabajo: Job? = null

    private var iniciado = false

    /**
     * Al crear la actividad. Si se recrea (girar el teléfono) el ViewModel sigue y no se
     * resuelve el QR dos veces; si el sistema cerró la app, se vuelve a leer el enlace.
     */
    fun iniciar(enlace: Enlace?) {
        if (iniciado) return
        iniciado = true
        abrir(enlace)
    }

    /** Con el enlace con que se abrió la app (o null si se abrió desde el ícono). */
    fun abrir(enlace: Enlace?) {
        trabajo?.cancel()
        when (enlace) {
            null -> {
                // Ya dentro de un bar (p. ej. al girar el teléfono), se queda en él.
                if (_estado.value is Entrada.Lista) return
                val slug = leerSlug() ?: barPorDefecto
                _estado.value = if (slug == null) Entrada.SinBar else Entrada.Lista(slug)
            }
            is Enlace.Bar -> trabajo = viewModelScope.launch { entrar(enlace.slug, enlace) }
            is Enlace.Qr -> trabajo = viewModelScope.launch {
                // Dentro de un bar, sus pantallas siguen a la vista mientras se resuelve: si el QR
                // es del mismo bar (volver a escanearlo), no se pierde dónde estaba.
                if (_estado.value !is Entrada.Lista) _estado.value = Entrada.Abriendo
                when (val r = repositorio.resolverQr(enlace.token)) {
                    is Resultado.Exito ->
                        if (FORMATO_SLUG.matches(r.valor)) entrar(r.valor, enlace) else _estado.value = Entrada.BarNoEncontrado(hayBar())
                    is Resultado.Fallo -> _estado.value = when (r.error.codigo) {
                        CodigoError.QR_REEMPLAZADO -> Entrada.QrReemplazado(hayBar())
                        CodigoError.NO_ENCONTRADO -> Entrada.BarNoEncontrado(hayBar())
                        else -> Entrada.Fallo(enlace)
                    }
                }
            }
        }
    }

    fun reintentar() {
        (_estado.value as? Entrada.Fallo)?.let { abrir(it.enlace) }
    }

    /** Desde un aviso de QR: de vuelta al bar que se recordaba. */
    fun volver() {
        trabajo?.cancel()
        val slug = leerSlug() ?: barPorDefecto
        _estado.value = if (slug == null) Entrada.SinBar else Entrada.Lista(slug)
    }

    private fun hayBar(): Boolean = (leerSlug() ?: barPorDefecto) != null

    /**
     * Comprueba que el bar existe y solo entonces lo deja como el bar del teléfono. Nada cambia
     * antes: si se cancela a medias (llega otro QR), el bar recordado sigue siendo el de antes.
     */
    private suspend fun entrar(slug: String, enlace: Enlace) {
        val actual = _estado.value
        if (actual is Entrada.Lista && actual.slug == slug) return
        _estado.value = Entrada.Abriendo
        val r = repositorio.bar(slug)
        _estado.value = when {
            r is Resultado.Exito && r.valor.slug == slug -> {
                guardarSlug(slug)
                Entrada.Lista(slug)
            }
            r is Resultado.Fallo && r.error.codigo != CodigoError.NO_ENCONTRADO -> Entrada.Fallo(enlace)
            else -> Entrada.BarNoEncontrado(hayBar())
        }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer {
                EntradaViewModel(
                    repositorio = Contenedor.repositorio,
                    leerSlug = { Contenedor.almacen.slug },
                    guardarSlug = { Contenedor.almacen.slug = it },
                    barPorDefecto = Contenedor.barPorDefecto,
                )
            }
        }
    }
}
