package com.proyectoN.rokolaBackend.acceso

import com.proyectoN.rokolaBackend.dispositivo.TipoDispositivo
import com.proyectoN.rokolaBackend.establecimiento.CodigoQr
import com.proyectoN.rokolaBackend.establecimiento.CodigoQrRepository
import com.proyectoN.rokolaBackend.establecimiento.ConfiguracionServicioRepository
import com.proyectoN.rokolaBackend.establecimiento.EstadoServicio
import com.proyectoN.rokolaBackend.sesion.EstadoSesion
import com.proyectoN.rokolaBackend.sesion.SesionRepository
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime

@Service
class AccesoService(
  private val qrRepositorio: CodigoQrRepository,
  private val configuraciones: ConfiguracionServicioRepository,
  private val sesiones: SesionRepository,
  private val horarioActivo: HorarioActivoService,
  private val tokens: TokenSeguro,
  private val jdbc: JdbcTemplate,
) {
  @Transactional(readOnly = true)
  fun resolverQr(token: String): AccesoQrResponse {
    val qr = buscarQrValido(token)
    val establecimiento = qr.establecimiento
    val idEstablecimiento = requireNotNull(establecimiento.id)
    val configuracion = configuraciones.findByEstablecimientoId(idEstablecimiento)
    val motivo = when {
      configuracion == null -> "Configuración del servicio no disponible"
      configuracion.estadoManual != EstadoServicio.DISPONIBLE -> "Servicio pausado o en mantenimiento"
      sesiones.findFirstByEstablecimientoIdAndEstadoOrderByFechaAperturaDesc(
        idEstablecimiento, EstadoSesion.ABIERTA,
      ) == null -> "No hay una sesión musical abierta"
      !horarioActivo.estaEnHorario(idEstablecimiento, establecimiento.zonaHoraria) -> "Fuera del horario de operación"
      else -> null
    }
    return AccesoQrResponse(
      establecimientoId = idEstablecimiento,
      nombre = establecimiento.nombre,
      slug = establecimiento.slug,
      zonaHoraria = establecimiento.zonaHoraria,
      servicioDisponible = motivo == null,
      motivoNoDisponible = motivo,
      avisoPrivacidadVersion = configuracion?.avisoPrivacidadVersion,
    )
  }

  @Transactional
  fun registrarDispositivo(token: String, request: RegistroDispositivoRequest): RegistroDispositivoResponse {
    val qr = buscarQrValido(token)
    val establecimientoId = requireNotNull(qr.establecimiento.id)
    val tipo = try {
      TipoDispositivo.desdeValor(request.tipo)
    } catch (_: IllegalArgumentException) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo debe ser movil, tableta u otro")
    }
    val tokenDispositivo = request.deviceToken ?: tokens.generar()
    if (!tokens.esValido(tokenDispositivo)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceToken no tiene el formato válido")
    }
    val hash = tokens.huellaDispositivo(establecimientoId, tokenDispositivo)

    // Inserta o actualiza una sola fila del dispositivo. No se finge consentimiento.
    val registro = jdbc.queryForObject(
      """
            INSERT INTO public.dispositivo
                (id_establecimiento, device_hash, tipo, primer_acceso, ultimo_acceso, aviso_privacidad_fecha)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                    CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END)
            ON CONFLICT (id_establecimiento, device_hash)
            DO UPDATE SET ultimo_acceso = CURRENT_TIMESTAMP,
                          aviso_privacidad_fecha = CASE
                              WHEN ? AND dispositivo.aviso_privacidad_fecha IS NULL
                                  THEN CURRENT_TIMESTAMP
                              ELSE dispositivo.aviso_privacidad_fecha
                          END
            RETURNING id_dispositivo, aviso_privacidad_fecha
            """.trimIndent(),
      { rs, _ ->
        RegistroGuardado(
          id = rs.getLong("id_dispositivo"),
          aceptadoEn = rs.getObject("aviso_privacidad_fecha", OffsetDateTime::class.java),
        )
      },
      establecimientoId,
      hash,
      tipo.valor,
      request.aceptaAvisoPrivacidad,
      request.aceptaAvisoPrivacidad,
    ) ?: error("INSERT/UPDATE no retornó el dispositivo")
    return RegistroDispositivoResponse(
      dispositivoId = registro.id,
      establecimientoId = establecimientoId,
      deviceToken = tokenDispositivo,
      avisoPrivacidadAceptadoEn = registro.aceptadoEn,
      avisoPrivacidadVersionActual = configuraciones.findByEstablecimientoId(establecimientoId)?.avisoPrivacidadVersion,
    )
  }

  private data class RegistroGuardado(val id: Long, val aceptadoEn: OffsetDateTime?)

  private fun buscarQrValido(token: String): CodigoQr {
    if (!tokens.esValido(token)) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "Código QR no encontrado")
    }
    val qr = qrRepositorio.findByTokenHashAndActivoTrueAndFechaRevocacionIsNull(tokens.huellaQr(token))
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Código QR no encontrado o revocado")
    if (!qr.establecimiento.activo) {
      throw ResponseStatusException(HttpStatus.GONE, "El establecimiento está inactivo")
    }
    return qr
  }
}
