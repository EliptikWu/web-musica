package com.example.rokola.viewmodel

import com.example.rokola.dominio.ErrorRockola

/**
 * Los cuatro estados que toda pantalla dibuja (CLAUDE.md: vacío, cargando y error, además
 * del contenido). Vacío lleva datos porque una cola vacía igual muestra lo que suena.
 */
sealed interface EstadoPantalla<out T> {
    data object Cargando : EstadoPantalla<Nothing>
    data class Vacio<out T>(val datos: T) : EstadoPantalla<T>
    data class Error(val error: ErrorRockola) : EstadoPantalla<Nothing>
    data class Contenido<out T>(val datos: T) : EstadoPantalla<T>
}
