package com.example.rokola.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Guarda los ViewModel de las pantallas del bar actual. Sobreviven a girar el teléfono, pero
 * al entrar a otro bar por QR se cierran (y con ellos su tiempo real): las pantallas del bar
 * anterior no siguen escuchando por detrás.
 */
class PantallasDelBar : ViewModel() {
    private var slug: String? = null
    private var almacen = ViewModelStore()

    fun de(bar: String): ViewModelStoreOwner {
        if (bar != slug) {
            almacen.clear()
            almacen = ViewModelStore()
            slug = bar
        }
        val actual = almacen
        return object : ViewModelStoreOwner {
            override val viewModelStore = actual
        }
    }

    /** Al salir del bar (un aviso de QR, «Abriendo…»): sus pantallas dejan de escuchar. */
    fun cerrar() {
        almacen.clear()
        almacen = ViewModelStore()
        slug = null
    }

    override fun onCleared() = almacen.clear()
}
