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
import java.time.LocalTime

/**
 * Franja horaria en la que un establecimiento funciona un día de la semana.
 * Corresponde a la tabla `horario_operacion`.
 */
@Entity
@Table(name = "horario_operacion")
class HorarioOperacion(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_horario")
	var id: Long? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_establecimiento", nullable = false)
	var establecimiento: Establecimiento,

	/** Día de la semana según ISO-8601: 1 = lunes … 7 = domingo. La columna es `smallint`. */
	@Column(name = "dia_semana", nullable = false)
	var diaSemana: Short,

	@Column(name = "hora_inicio", nullable = false)
	var horaInicio: LocalTime,

	/** Si es menor que [horaInicio], la franja termina al día siguiente (por ejemplo 18:00 a 02:00). */
	@Column(name = "hora_fin", nullable = false)
	var horaFin: LocalTime,

	@Column(name = "activo", nullable = false)
	var activo: Boolean,
)
