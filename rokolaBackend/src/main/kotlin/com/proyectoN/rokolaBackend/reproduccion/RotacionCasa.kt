package com.proyectoN.rokolaBackend.reproduccion

/** Regla pura: el índice de sesión comienza en 1 y avanza circularmente. */
object RotacionCasa {
    fun posicion(indice: Int, total: Int): Int {
        require(total > 0) { "La lista debe contener canciones" }
        return Math.floorMod(indice.toLong() - 1L, total.toLong()).toInt()
    }
    fun siguiente(indice: Int, total: Int): Int = ((posicion(indice, total) + 1) % total) + 1
}
