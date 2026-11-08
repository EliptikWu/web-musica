package com.proyectoN.rokolaBackend.usuario

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Rol del personal de un establecimiento (por ejemplo Administrador u Operador). Corresponde a la tabla `rol`.
 *
 * `id_rol` es `smallint` en PostgreSQL, por eso aquí es [Short] y no [Long].
 */
@Entity
@Table(name = "rol")
class Rol(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_rol")
	var id: Short? = null,

	@Column(name = "nombre", nullable = false, length = 40, unique = true)
	var nombre: String,

	@Column(name = "descripcion", length = 250)
	var descripcion: String? = null,
)
