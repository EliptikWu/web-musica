package com.proyectoN.rokolaBackend.solicitud

/** El token proviene del registro QR; nunca enviar un idDispositivo arbitrario. */
data class CrearSolicitudRequest(
    val youtubeVideoId: String,
    val deviceToken: String,
)

data class CrearSolicitudResponse(
    val solicitudId: Long,
    val sesionId: Long,
    val cancionId: Long,
    val youtubeVideoId: String,
    val resultado: String, // solicitud_creada | voto_agregado
    val estado: String,
    val votos: Long,
)
