package com.proyectoN.rokolaBackend.acceso

import java.time.OffsetDateTime

/** Únicamente los datos públicos mínimos para resolver el QR. */
data class AccesoQrResponse(
    val establecimientoId: Long,
    val nombre: String,
    val slug: String,
    val zonaHoraria: String,
    val servicioDisponible: Boolean,
    val motivoNoDisponible: String?,
    val avisoPrivacidadVersion: String?,
)

/** Si deviceToken es null, se emite una credencial nueva para este cliente. */
data class RegistroDispositivoRequest(
    val tipo: String,
    val deviceToken: String? = null,
    val aceptaAvisoPrivacidad: Boolean = false,
)

/** Guardar deviceToken como secreto local. Nunca enviarlo en una URL. */
data class RegistroDispositivoResponse(
    val dispositivoId: Long,
    val establecimientoId: Long,
    val deviceToken: String,
    val avisoPrivacidadAceptadoEn: OffsetDateTime,
    val avisoPrivacidadVersionActual: String?,
)
