package com.proyectoN.rokolaBackend.voto

/** Se identifica al cliente con el token del dispositivo registrado por QR. */
data class RegistrarVotoRequest(
    val deviceToken: String,
)

data class RegistrarVotoResponse(
    val votoId: Long,
    val solicitudId: Long,
    val sesionId: Long,
    val cancionId: Long,
    val resultado: String, // voto_registrado
    val estado: String,    // pendiente
    val votos: Long,
)
