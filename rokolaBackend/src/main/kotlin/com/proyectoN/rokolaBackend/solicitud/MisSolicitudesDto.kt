package com.proyectoN.rokolaBackend.solicitud

import java.time.OffsetDateTime

data class MiSolicitudResponse(
    val solicitudId: Long,
    val cancionId: Long,
    val youtubeVideoId: String,
    val titulo: String,
    val artista: String?,
    val miniaturaUrl: String?,
    val estado: String,
    val motivoEstado: String?,
    val fechaSolicitud: OffsetDateTime,
    val votos: Long,
    val posicionEnCola: Int?,
    val esperaEstimadaSeg: Long?,
)

data class MisSolicitudesResponse(
    val sesionId: Long,
    val total: Int,
    val solicitudes: List<MiSolicitudResponse>,
)
