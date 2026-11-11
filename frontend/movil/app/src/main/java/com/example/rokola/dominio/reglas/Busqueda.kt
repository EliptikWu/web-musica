package com.example.rokola.dominio.reglas

import com.example.rokola.dominio.ResultadoBusqueda

/*
 * Filtros de la búsqueda (apps/web/lib/reglas/busqueda.ts; RF-055, PB-51): género, duración
 * y popularidad. Filtran lo que ya llegó, en el teléfono: el cambio es inmediato y no gasta
 * cuota de YouTube.
 */

sealed interface FiltroGenero {
    data object Todos : FiltroGenero
    data object SinClasificar : FiltroGenero
    data class Uno(val genero: String) : FiltroGenero
}

/** Con el límite de arriba incluido: hasta 4:00, de 4:01 a 6:00, más de 6:00. */
enum class FiltroDuracion(val desdeSeg: Int, val hastaSeg: Int) {
    TODAS(0, Int.MAX_VALUE),
    CORTA(0, 240),
    MEDIA(241, 360),
    LARGA(361, Int.MAX_VALUE),
}

enum class FiltroPopularidad { TODAS, PEDIDAS, MUY_PEDIDAS }

data class FiltrosBusqueda(
    val genero: FiltroGenero = FiltroGenero.Todos,
    val duracion: FiltroDuracion = FiltroDuracion.TODAS,
    val popularidad: FiltroPopularidad = FiltroPopularidad.TODAS,
)

val SIN_FILTROS = FiltrosBusqueda()

/** Desde cuántos pedidos en el bar una canción cuenta como «muy pedida» (propuesto en la web). */
const val MUY_PEDIDA = 5

fun cuantosFiltros(f: FiltrosBusqueda): Int = listOf(
    f.genero != FiltroGenero.Todos,
    f.duracion != FiltroDuracion.TODAS,
    f.popularidad != FiltroPopularidad.TODAS,
).count { it }

fun filtrarResultados(resultados: List<ResultadoBusqueda>, f: FiltrosBusqueda): List<ResultadoBusqueda> {
    val salida = resultados.filter { r ->
        val generoOk = when (val g = f.genero) {
            FiltroGenero.Todos -> true
            FiltroGenero.SinClasificar -> r.genero == null
            is FiltroGenero.Uno -> r.genero == g.genero
        }
        val duracionOk = r.cancion.duracionSeg in f.duracion.desdeSeg..f.duracion.hastaSeg
        val popularidadOk = when (f.popularidad) {
            FiltroPopularidad.TODAS -> true
            FiltroPopularidad.PEDIDAS -> r.pedidasEnElBar >= 1
            FiltroPopularidad.MUY_PEDIDAS -> r.pedidasEnElBar >= MUY_PEDIDA
        }
        generoOk && duracionOk && popularidadOk
    }
    // Con la popularidad elegida, las más pedidas primero (orden estable).
    return if (f.popularidad != FiltroPopularidad.TODAS) salida.sortedByDescending { it.pedidasEnElBar } else salida
}

/** Los géneros presentes, por nombre; «sin clasificar» (null) al final. */
fun generosDe(resultados: List<ResultadoBusqueda>): List<String?> {
    val colador = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("es"))
    val conGenero = resultados.mapNotNull { it.genero }.distinct().sortedWith(colador)
    return if (resultados.any { it.genero == null }) conGenero + null else conGenero
}
