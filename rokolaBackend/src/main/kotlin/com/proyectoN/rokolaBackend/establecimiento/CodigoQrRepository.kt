package com.proyectoN.rokolaBackend.establecimiento

import org.springframework.data.jpa.repository.JpaRepository

/** Acceso a la tabla `codigo_qr`. */
interface CodigoQrRepository : JpaRepository<CodigoQr, Long> {
	/** Del más reciente al más antiguo. */
	fun findByEstablecimientoIdOrderByFechaCreacionDesc(idEstablecimiento: Long): List<CodigoQr>
  /** Solo se reconocen QR activos que aún no han sido revocados. */
  fun findByTokenHashAndActivoTrueAndFechaRevocacionIsNull(tokenHash: String): CodigoQr?
}
