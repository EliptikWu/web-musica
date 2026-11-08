package com.proyectoN.rokolaBackend.dispositivo

import com.proyectoN.rokolaBackend.establecimiento.Establecimiento
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
 * Teléfono o tableta de un cliente que entró a Rokola escaneando el QR. Corresponde a la tabla `dispositivo`.
 *
 * No guarda datos personales (nombre, correo, teléfono): solo una huella del dispositivo ([deviceHash]),
 * única dentro de cada establecimiento (restricción `uq_dispositivo_est_hash`).
 * [deviceHash] no debe salir nunca en una respuesta de la API: con ella alguien podría hacerse pasar por otro cliente.
 */
@Entity
@Table(name = "dispositivo")
class Dispositivo(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_dispositivo")
	var id: Long? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_establecimiento", nullable = false)
	var establecimiento: Establecimiento,

	@Column(name = "device_hash", nullable = false, length = 255)
	var deviceHash: String,

	@Column(name = "tipo", nullable = false, length = 30)
	var tipo: TipoDispositivo,

	@Column(name = "primer_acceso", nullable = false)
	var primerAcceso: OffsetDateTime,

	@Column(name = "ultimo_acceso", nullable = false)
	var ultimoAcceso: OffsetDateTime,

	/** Cuándo aceptó el cliente el aviso de privacidad; `null` si todavía no lo ha aceptado. */
	@Column(name = "aviso_privacidad_fecha")
	var avisoPrivacidadFecha: OffsetDateTime? = null,
)
