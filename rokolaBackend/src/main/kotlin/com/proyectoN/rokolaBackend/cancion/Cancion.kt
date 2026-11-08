package com.proyectoN.rokolaBackend.cancion

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * Canción (video musical de YouTube) conocida por Rokola. Corresponde a la tabla `cancion`.
 *
 * El catálogo es común a todos los establecimientos: la tabla no tiene `id_establecimiento`.
 * Cada video aparece una sola vez (restricción única sobre `youtube_video_id`).
 */
@Entity
@Table(name = "cancion")
class Cancion(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_cancion")
	var id: Long? = null,

	/** Id del video en YouTube, por ejemplo `dQw4w9WgXcQ` (lo que va después de `watch?v=`). */
	@Column(name = "youtube_video_id", nullable = false, length = 30, unique = true)
	var youtubeVideoId: String,

	@Column(name = "titulo", nullable = false, length = 300)
	var titulo: String,

	@Column(name = "artista", length = 200)
	var artista: String? = null,

	/** Canal de YouTube que publicó el video. */
	@Column(name = "canal", length = 200)
	var canal: String? = null,

	/** Género musical asignado en Rokola, por ejemplo `Rock` o `Latino`. */
	@Column(name = "categoria", length = 100)
	var categoria: String? = null,

	@Column(name = "duracion_seg", nullable = false)
	var duracionSeg: Int,

	@Column(name = "miniatura_url", columnDefinition = "text")
	var miniaturaUrl: String? = null,

	/** Categoría que informa YouTube, por ejemplo `Music`. */
	@Column(name = "categoria_youtube", length = 100)
	var categoriaYoutube: String? = null,

	/** Si YouTube permite reproducir el video dentro de otra página (necesario para la pantalla del bar). */
	@Column(name = "embebible", nullable = false)
	var embebible: Boolean,

	@Column(name = "disponibilidad", nullable = false, length = 30)
	var disponibilidad: Disponibilidad,

	/** Cuándo se comprobó por última vez la disponibilidad en YouTube; `null` si nunca. */
	@Column(name = "ultima_validacion")
	var ultimaValidacion: OffsetDateTime? = null,

	@Column(name = "fecha_creacion", nullable = false)
	var fechaCreacion: OffsetDateTime,
)
