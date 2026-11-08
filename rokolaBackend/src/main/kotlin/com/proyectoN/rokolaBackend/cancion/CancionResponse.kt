package com.proyectoN.rokolaBackend.cancion

import java.time.OffsetDateTime

/** Datos de una canción que entrega la API. */
data class CancionResponse(
	val id: Long,
	val youtubeVideoId: String,
	val titulo: String,
	val artista: String?,
	val canal: String?,
	val categoria: String?,
	val duracionSeg: Int,
	val miniaturaUrl: String?,
	val categoriaYoutube: String?,
	val embebible: Boolean,
	val disponibilidad: Disponibilidad,
	val ultimaValidacion: OffsetDateTime?,
	val fechaCreacion: OffsetDateTime,
)

fun Cancion.toResponse() = CancionResponse(
	id = requireNotNull(id) { "Una canción leída de la base de datos siempre tiene id" },
	youtubeVideoId = youtubeVideoId,
	titulo = titulo,
	artista = artista,
	canal = canal,
	categoria = categoria,
	duracionSeg = duracionSeg,
	miniaturaUrl = miniaturaUrl,
	categoriaYoutube = categoriaYoutube,
	embebible = embebible,
	disponibilidad = disponibilidad,
	ultimaValidacion = ultimaValidacion,
	fechaCreacion = fechaCreacion,
)
