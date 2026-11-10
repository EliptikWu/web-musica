package com.proyectoN.rokolaBackend.cola

import java.time.OffsetDateTime

/** La cola publica no expone identificadores ni tokens de dispositivos. */
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
  /** Segundos estimados hasta que COMIENCE esta cancion. */
  val esperaEstimadaSeg: Long,
  val inicioEstimado: OffsetDateTime,
)

data class ColaResponse(
  val sesionId: Long,
  val calculadaEn: OffsetDateTime,
  val totalSolicitudes: Int,
  /** Null si no hay reproduccion en curso. */
  val segundosRestantesActual: Long?,
  val canciones: List<CancionEnColaResponse>,
)
