package com.proyectoN.rokolaBackend.reproduccion

import java.time.OffsetDateTime

/** Instantanea de la reproduccion actual. No incluye informacion privada. */
data class ReproduccionActualResponse(
    val reproduccionId: Long,
    val sesionId: Long,
    val solicitudId: Long?,
    val elementoListaId: Long?,
    val origen: String, // solicitud | lista_defecto
    val cancionId: Long,
    val youtubeVideoId: String,
    val titulo: String,
    val artista: String?,
    val miniaturaUrl: String?,
    val duracionSeg: Int,
    val fechaInicio: OffsetDateTime?,
    val segundosTranscurridos: Long,
    val segundosRestantes: Long,
    val resultado: String, // iniciada
)

data class EstadoReproduccionResponse(
    val sesionId: Long,
    val actual: ReproduccionActualResponse?,
)

/** Solo el reproductor/operador autenticado puede informar resultados. */
data class FinalizarReproduccionRequest(
    val resultado: String, // completada | saltada | fallida
    val segundosReproducidos: Int? = null,
)

data class ReproduccionFinalizadaResponse(
    val reproduccionId: Long,
    val sesionId: Long,
    val solicitudId: Long?,
    val cancionId: Long,
    val resultado: String,
    val estadoSolicitud: String?,
    val fechaFin: OffsetDateTime,
    val segundosReproducidos: Int,
)
