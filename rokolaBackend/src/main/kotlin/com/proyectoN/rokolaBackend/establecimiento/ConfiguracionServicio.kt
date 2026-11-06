package com.proyectoN.rokolaBackend.establecimiento

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * Reglas de participación musical de un establecimiento (una sola configuración por establecimiento).
 * Corresponde a la tabla `configuracion_servicio`.
 */
@Entity
@Table(name = "configuracion_servicio")
class ConfiguracionServicio(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_configuracion")
	var id: Long? = null,

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_establecimiento", nullable = false, unique = true)
	var establecimiento: Establecimiento,

	@Column(name = "estado_manual", nullable = false, length = 20)
	var estadoManual: EstadoServicio,

	/** Cantidad máxima de solicitudes que puede hacer un mismo dispositivo. */
	@Column(name = "max_solicitudes_dispositivo", nullable = false)
	var maxSolicitudesDispositivo: Int,

	/** Duración máxima, en segundos, de una canción que se puede solicitar. */
	@Column(name = "duracion_max_seg", nullable = false)
	var duracionMaxSeg: Int,

	/** Minutos que deben pasar antes de que una canción ya reproducida pueda volver a sonar. */
	@Column(name = "minutos_bloqueo_repeticion", nullable = false)
	var minutosBloqueoRepeticion: Int,

	@Column(name = "aviso_privacidad_version", length = 40)
	var avisoPrivacidadVersion: String? = null,

	@Column(name = "fecha_modificacion", nullable = false)
	var fechaModificacion: OffsetDateTime,
)
