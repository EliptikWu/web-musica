package com.proyectoN.rokolaBackend.establecimiento

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * Código QR de acceso de un establecimiento. Corresponde a la tabla `codigo_qr`.
 *
 * La base solo guarda la huella ([tokenHash]) del token que va dentro del QR, nunca el token.
 * [tokenHash] no debe salir nunca en una respuesta de la API.
 */
@Entity
@Table(name = "codigo_qr")
class CodigoQr(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_qr")
	var id: Long? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_establecimiento", nullable = false)
	var establecimiento: Establecimiento,

	@Column(name = "token_hash", nullable = false, length = 255, unique = true)
	var tokenHash: String,

	@Column(name = "fecha_creacion", nullable = false)
	var fechaCreacion: OffsetDateTime,

	@Column(name = "fecha_revocacion")
	var fechaRevocacion: OffsetDateTime? = null,

	@Column(name = "activo", nullable = false)
	var activo: Boolean,
)
