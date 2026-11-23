package com.proyectoN.rokolaBackend.cancion

import org.springframework.stereotype.Service
import java.text.Normalizer
import java.util.Locale

/**
 * PMV: catálogo fijo y coincidencia EXACTA de etiquetas normalizadas.
 * No deduce géneros a partir de artistas ni etiquetas generales como "latin music".
 * Cuando hay señales contradictorias devuelve null, no un género aleatorio.
 */
@Service
class ClasificadorGeneroService {
    private val diccionario = linkedMapOf(
        "Salsa" to listOf("salsa", "salsa romantica", "salsa dura", "salsa music"),
        "Urbano" to listOf("reggaeton", "regueton", "reggaeton latino", "latin trap", "trap latino", "musica urbana"),
        "Pop" to listOf("pop", "pop latino", "latin pop", "pop music"),
        "Tropical" to listOf("musica tropical", "tropical music"),
        "Vallenato" to listOf("vallenato", "vallenato romantico", "vallenato music"),
        "Bachata" to listOf("bachata", "bachata romantica", "bachata music"),
        "Rock" to listOf("rock", "rock en espanol", "latin rock", "rock music"),
        "Electrónica" to listOf("electronica", "electronic dance music", "edm"),
        "Merengue" to listOf("merengue", "merengue music"),
        "Cumbia" to listOf("cumbia", "cumbia colombiana", "cumbia music"),
    )

    private val indice: Map<String, String> = diccionario.flatMap { (genero, alias) ->
        alias.map { normalizar(it) to genero }
    }.toMap()

    fun generos(): List<String> = diccionario.keys.toList()

    /** Devuelve null cuando no hay coincidencias o aparecen géneros diferentes. */
    fun detectar(etiquetas: List<String>): String? {
        val candidatos = etiquetas.mapNotNull { indice[normalizar(it)] }.distinct()
        return candidatos.singleOrNull()
    }

    /** Para validar el JSON del panel, sin aceptar categorías inventadas. */
    fun canonico(nombre: String): String? = diccionario.keys.firstOrNull {
        normalizar(it) == normalizar(nombre)
    }

    private fun normalizar(texto: String): String = Normalizer.normalize(
        texto.lowercase(Locale.ROOT), Normalizer.Form.NFD
    ).replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim().replace(Regex("\\s+"), " ")
}
