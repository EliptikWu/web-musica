package com.proyectoN.rokolaBackend.usuario

import java.time.OffsetDateTime

/**
 * Datos de un usuario que entrega la API.
 *
 * A propósito NO incluye:
 * - `passwordHash`: con la huella alguien podría intentar adivinar la contraseña sin pasar por el login.
 * - `intentosFallidos`: es un dato interno del control de inicio de sesión (Etapa 7).
 */
data class UsuarioResponse(
	val id: Long,
	val idEstablecimiento: Long,
	val rol: RolResponse,
	val nombre: String,
	val correo: String,
	val activo: Boolean,
	val cambiarPassword: Boolean,
	val bloqueadoHasta: OffsetDateTime?,
	val fechaCreacion: OffsetDateTime,
)

fun Usuario.toResponse() = UsuarioResponse(
	id = requireNotNull(id) { "Un usuario leído de la base de datos siempre tiene id" },
	// Leer solo el id del establecimiento no dispara una consulta extra: Hibernate ya lo conoce.
	idEstablecimiento = requireNotNull(establecimiento.id) { "El establecimiento de un usuario siempre tiene id" },
	rol = rol.toResponse(),
	nombre = nombre,
	correo = correo,
	activo = activo,
	cambiarPassword = cambiarPassword,
	bloqueadoHasta = bloqueadoHasta,
	fechaCreacion = fechaCreacion,
)
