package com.proyectoN.rokolaBackend.cola

import java.time.OffsetDateTime

/** Respuesta pública: no contiene IDs de dispositivos ni hashes. */
data class CancionEnColaResponse(
    val posicion: Int,
    val solicitudId: Long,
    val cancionId: Long,
    val youtubeVideoId: String,
    val titulo: String,
    val artista: String?,
    val duracionSeg: Int,
    val miniaturaUrl: String?,
    val votos: Long,
    val fechaSolicitud: OffsetDateTime,
)

data class ColaResponse(
    val sesionId: Long,
    val totalSolicitudes: Int,
    val canciones: List<CancionEnColaResponse>,
)
