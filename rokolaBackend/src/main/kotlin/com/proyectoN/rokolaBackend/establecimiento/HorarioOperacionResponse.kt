package com.proyectoN.rokolaBackend.establecimiento

import java.time.LocalTime

/** Franja horaria de funcionamiento. `diaSemana` va de 1 (lunes) a 7 (domingo). */
data class HorarioOperacionResponse(
	val id: Long,
	val diaSemana: Int,
	val horaInicio: LocalTime,
	val horaFin: LocalTime,
	val activo: Boolean,
)

fun HorarioOperacion.toResponse() = HorarioOperacionResponse(
	id = requireNotNull(id) { "Un horario leído de la base de datos siempre tiene id" },
	diaSemana = diaSemana.toInt(),
	horaInicio = horaInicio,
	horaFin = horaFin,
	activo = activo,
)
