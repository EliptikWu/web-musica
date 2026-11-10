package com.proyectoN.rokolaBackend.voto

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.acceso.TokenSeguro
import com.proyectoN.rokolaBackend.dispositivo.Dispositivo
import com.proyectoN.rokolaBackend.establecimiento.ConfiguracionServicioRepository
import com.proyectoN.rokolaBackend.establecimiento.EstadoServicio
import com.proyectoN.rokolaBackend.sesion.EstadoSesion
import com.proyectoN.rokolaBackend.sesion.Sesion
import com.proyectoN.rokolaBackend.solicitud.EstadoSolicitud
import com.proyectoN.rokolaBackend.solicitud.Solicitud
import com.proyectoN.rokolaBackend.cancion.Disponibilidad
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Paso 10: registrar un voto independiente en una canción que ya está en la cola.
 *
 * Se bloquea la misma fila de `sesion` que bloquea SolicitudRegistroService
 * (Paso 9). Esto serializa ambas rutas y evita la carrera comprobar/insertar.
 * El índice parcial único de `voto` sigue siendo la última defensa en la BD.
 */
@Service
class VotoService(
    private val tokens: TokenSeguro,
    private val entityManager: EntityManager,
    private val jdbc: JdbcTemplate,
    private val configuraciones: ConfiguracionServicioRepository,
    private val horario: HorarioActivoService,
    private val votos: VotoRepository,
) {
    @Transactional
    fun registrar(idSolicitud: Long, request: RegistrarVotoRequest): RegistrarVotoResponse {
        if (idSolicitud <= 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "idSolicitud debe ser mayor que cero")
        }
        if (!tokens.esValido(request.deviceToken)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identificador de dispositivo no válido")
        }

        // Leemos únicamente el id de sesión, sin bloquear todavía ninguna fila.
        // No exponemos el identificador del dispositivo al cliente.
        val idSesion = try {
            jdbc.queryForObject(
                "SELECT id_sesion FROM public.solicitud WHERE id_solicitud = ?",
                Long::class.javaObjectType,
                idSolicitud,
            ) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada")
        } catch (_: EmptyResultDataAccessException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada")
        }

        // MISMO BLOQUEO Y ORDEN que el registro de solicitudes del Paso 9.
        val sesion = entityManager.find(Sesion::class.java, idSesion, LockModeType.PESSIMISTIC_WRITE)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "La sesión de la solicitud no existe")
        val solicitud = entityManager.find(Solicitud::class.java, idSolicitud)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada")

        if (solicitud.estado != EstadoSolicitud.PENDIENTE) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede votar por solicitudes pendientes")
        }

        val establecimiento = sesion.establecimiento
        val idEstablecimiento = requireNotNull(establecimiento.id)
        if (!establecimiento.activo || sesion.estado != EstadoSesion.ABIERTA) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "El establecimiento o la sesión no están activos")
        }
        val configuracion = configuraciones.findByEstablecimientoId(idEstablecimiento)
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "El establecimiento no tiene configuración de servicio")
        if (configuracion.estadoManual != EstadoServicio.DISPONIBLE) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "El servicio está pausado o en mantenimiento")
        }
        if (!horario.estaEnHorario(idEstablecimiento, establecimiento.zonaHoraria)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "El establecimiento está fuera del horario de operación")
        }

        // El dispositivo debe estar registrado en el MISMO establecimiento de la sesión.
        val hash = tokens.huellaDispositivo(idEstablecimiento, request.deviceToken)
        val dispositivo = entityManager.createQuery(
            "SELECT d FROM Dispositivo d WHERE d.establecimiento.id = :establecimiento AND d.deviceHash = :hash",
            Dispositivo::class.java,
        )
            .setParameter("establecimiento", idEstablecimiento)
            .setParameter("hash", hash)
            .setMaxResults(1)
            .resultList.firstOrNull()
            ?: throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Dispositivo no registrado en este establecimiento",
            )

        if (dispositivo.avisoPrivacidadFecha == null) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Debes aceptar el aviso de privacidad antes de votar",
            )
        }

        val idCancion = requireNotNull(solicitud.cancion.id)
      when (solicitud.cancion.disponibilidad) {

        Disponibilidad.DISPONIBLE -> Unit

        Disponibilidad.NO_DISPONIBLE -> {
          throw ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "La canción no está disponible para recibir votos"
          )
        }

        Disponibilidad.RESTRINGIDA -> {
          throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "La canción está restringida"
          )
        }

        Disponibilidad.DESCONOCIDA -> {
          throw ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "La disponibilidad de la canción no ha sido verificada"
          )
        }
      }

      if (!solicitud.cancion.embebible) {
        throw ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "La canción no permite reproducción incrustada"
        )
      }
        if (estaBloqueada(idEstablecimiento, idCancion)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Esta canción está bloqueada en el establecimiento")
        }

        val idDispositivo = requireNotNull(dispositivo.id)
        if (votos.existsBySolicitudIdAndDispositivoIdAndActivoTrue(idSolicitud, idDispositivo)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Este dispositivo ya votó por la canción")
        }

        val voto = votos.saveAndFlush(
            Voto(
                solicitud = solicitud,
                dispositivo = dispositivo,
                fechaVoto = OffsetDateTime.now(ZoneOffset.UTC),
                activo = true,
            ),
        )

        return RegistrarVotoResponse(
            votoId = requireNotNull(voto.id),
            solicitudId = idSolicitud,
            sesionId = idSesion,
            cancionId = idCancion,
            resultado = "voto_registrado",
            estado = solicitud.estado.valor,
            votos = votos.countBySolicitudIdAndActivoTrue(idSolicitud),
        )
    }

    private fun estaBloqueada(idEstablecimiento: Long, idCancion: Long): Boolean =
        jdbc.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1 FROM public.bloqueo_contenido
                WHERE id_establecimiento = ? AND id_cancion = ?
                  AND activo = TRUE
                  AND fecha_inicio <= CURRENT_TIMESTAMP
                  AND (fecha_fin IS NULL OR fecha_fin > CURRENT_TIMESTAMP)
            )
            """.trimIndent(),
            Boolean::class.javaObjectType,
            idEstablecimiento,
            idCancion,
        ) ?: false
}
