package com.proyectoN.rokolaBackend.usuario

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
 * Persona del personal de un establecimiento que entra al panel administrativo. Corresponde a la tabla `usuario`.
 *
 * Cada usuario pertenece a un solo establecimiento y tiene un solo rol.
 * [passwordHash] es la huella de la contraseña: no debe salir nunca en una respuesta de la API.
 */
@Entity
@Table(name = "usuario")
class Usuario(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	var id: Long? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_establecimiento", nullable = false)
	var establecimiento: Establecimiento,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_rol", nullable = false)
	var rol: Rol,

	@Column(name = "nombre", nullable = false, length = 120)
	var nombre: String,

	/** Único dentro de cada establecimiento (restricción `uq_usuario_correo_establecimiento`). */
	@Column(name = "correo", nullable = false, length = 254)
	var correo: String,

	@Column(name = "password_hash", nullable = false, columnDefinition = "text")
	var passwordHash: String,

	@Column(name = "activo", nullable = false)
	var activo: Boolean,

	/** Si es `true`, el usuario debe cambiar su contraseña al iniciar sesión. */
	@Column(name = "cambiar_password", nullable = false)
	var cambiarPassword: Boolean,

	/** Intentos de inicio de sesión fallidos seguidos. Se usará en la Etapa 7 (seguridad). */
	@Column(name = "intentos_fallidos", nullable = false)
	var intentosFallidos: Short,

	/** Si tiene una fecha futura, el usuario no puede iniciar sesión hasta entonces. */
	@Column(name = "bloqueado_hasta")
	var bloqueadoHasta: OffsetDateTime? = null,

	@Column(name = "fecha_creacion", nullable = false)
	var fechaCreacion: OffsetDateTime,
)
