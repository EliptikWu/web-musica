package com.proyectoN.rokolaBackend.sesion

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
 * Sesión musical de un establecimiento: el periodo (normalmente una noche) en el que los clientes
 * pueden pedir y votar canciones. Corresponde a la tabla `sesion`.
 */
@Entity
@Table(name = "sesion")
class Sesion(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_sesion")
	var id: Long? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_establecimiento", nullable = false)
	var establecimiento: Establecimiento,

	/**
	 * Evento especial del calendario al que pertenece la sesión (opcional).
	 * Se guarda solo el id: la tabla `evento_calendario` todavía no tiene entidad propia.
	 */
	@Column(name = "id_evento_calendario")
	var idEventoCalendario: Long? = null,

	@Column(name = "fecha_apertura", nullable = false)
	var fechaApertura: OffsetDateTime,

	/** `null` mientras la sesión sigue abierta. */
	@Column(name = "fecha_cierre")
	var fechaCierre: OffsetDateTime? = null,

	@Column(name = "tipo_evento", length = 60)
	var tipoEvento: String? = null,

	@Column(name = "estado", nullable = false, length = 20)
	var estado: EstadoSesion,

	/** Posición (desde 1) por la que va la lista predeterminada cuando no hay solicitudes. */
	@Column(name = "indice_lista_defecto", nullable = false)
	var indiceListaDefecto: Int,
)
