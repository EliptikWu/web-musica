package com.proyectoN.rokolaBackend.acceso

import java.time.OffsetDateTime

/** Datos públicos mínimos para resolver un QR. */
data class AccesoQrResponse(
  val establecimientoId: Long,
  val nombre: String,
  val slug: String,
  val zonaHoraria: String,
  val servicioDisponible: Boolean,
  val motivoNoDisponible: String?,
  val avisoPrivacidadVersion: String?,
)

/** Si deviceToken no se envía, el backend crea uno nuevo. */
data class RegistroDispositivoRequest(
  val tipo: String,
  val deviceToken: String? = null,
  val aceptaAvisoPrivacidad: Boolean = false,
)

/** El consentimiento es NULL cuando el acceso se registra automáticamente sin aceptación. */
data class RegistroDispositivoResponse(
  val dispositivoId: Long,
  val establecimientoId: Long,
  val deviceToken: String,
  val avisoPrivacidadAceptadoEn: OffsetDateTime?,
  val avisoPrivacidadVersionActual: String?,
)
