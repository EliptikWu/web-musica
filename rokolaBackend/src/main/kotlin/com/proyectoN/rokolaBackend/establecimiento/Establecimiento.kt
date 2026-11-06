package com.proyectoN.rokolaBackend.establecimiento

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * Bar, taberna u otro local que usa Rokola. Corresponde a la tabla `establecimiento`.
 */
@Entity
@Table(name = "establecimiento")
class Establecimiento(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_establecimiento")
	var id: Long? = null,

	@Column(name = "nombre", nullable = false, length = 150)
	var nombre: String,

	/** Identificador legible y único que se usa en las URL, por ejemplo `bar-la-nota`. */
	@Column(name = "slug", nullable = false, length = 100, unique = true)
	var slug: String,

	@Column(name = "direccion", length = 250)
	var direccion: String? = null,

	/** Zona horaria IANA usada para interpretar los horarios de operación, por ejemplo `America/Bogota`. */
	@Column(name = "zona_horaria", nullable = false, length = 60)
	var zonaHoraria: String,

	@Column(name = "activo", nullable = false)
	var activo: Boolean,

	@Column(name = "fecha_creacion", nullable = false)
	var fechaCreacion: OffsetDateTime,
)
