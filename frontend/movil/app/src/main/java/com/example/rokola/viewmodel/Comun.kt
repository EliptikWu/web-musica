package com.example.rokola.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rokola.Contenedor
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.RepositorioRockola
import com.example.rokola.dominio.Resultado

/** El valor, o el error a [enFallo] (que no vuelve: lanza o sale de la corrutina). */
internal inline fun <T> Resultado<T>.valorO(enFallo: (ErrorRockola) -> Nothing): T = when (this) {
    is Resultado.Exito -> valor
    is Resultado.Fallo -> enFallo(error)
}

internal fun <T> Resultado<T>.valorONulo(): T? = (this as? Resultado.Exito)?.valor

/** Contenido o vacío según [vacio]. */
internal fun <T> contenidoOVacio(datos: T, vacio: Boolean): EstadoPantalla<T> =
    if (vacio) EstadoPantalla.Vacio(datos) else EstadoPantalla.Contenido(datos)

/** Los datos que la pantalla ya muestra, haya contenido o esté vacía. */
internal val <T> EstadoPantalla<T>.datos: T?
    get() = when (this) {
        is EstadoPantalla.Contenido -> datos
        is EstadoPantalla.Vacio -> datos
        else -> null
    }

/** Crea un ViewModel con el repositorio del [Contenedor]. */
internal inline fun <reified VM : ViewModel> fabrica(crossinline crear: (RepositorioRockola) -> VM): ViewModelProvider.Factory =
    viewModelFactory { initializer { crear(Contenedor.repositorio) } }
