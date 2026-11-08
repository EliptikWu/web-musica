package com.proyectoN.rokolaBackend.usuario

/** Datos de un rol que entrega la API. */
data class RolResponse(
	val id: Short,
	val nombre: String,
	val descripcion: String?,
)

fun Rol.toResponse() = RolResponse(
	id = requireNotNull(id) { "Un rol leído de la base de datos siempre tiene id" },
	nombre = nombre,
	descripcion = descripcion,
)
