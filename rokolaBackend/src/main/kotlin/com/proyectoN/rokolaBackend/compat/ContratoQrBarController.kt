package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.TokenSeguro
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Adaptador de lectura al contrato del frontend para QR y bar público.
 * No reemplaza AccesoController, QrRedirectController ni EstablecimientoController.
 * Se usan mapas con llaves literales para no cambiar el JSON de las APIs anteriores.
 */
@RestController
@RequestMapping("/api")
class ContratoQrBarController(
  private val jdbc: JdbcTemplate,
  private val tokens: TokenSeguro,
  private val dispositivos: ContratoDispositivoService,
) {
  private data class QrRow(val slug: String, val barActivo: Boolean, val vigente: Boolean)

  @GetMapping("/qr/{token}")
  fun resolverQr(
    @PathVariable token: String,
    @RequestHeader(value = "X-Rockola-Dispositivo", required = false) uuid: String?,
  ): Map<String, String> {
    // OpenAPI exige UUID para todas las rutas del cliente, incluso las lecturas.
    // Se valida su sintaxis sin crear ni devolver un registro de dispositivo.
    dispositivos.huella(uuid)
    if (!tokens.esValido(token)) {
      throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Código QR no encontrado")
    }
    val qr = jdbc.query(
      """
            SELECT e.slug, e.activo AS bar_activo,
                   (q.activo AND q.fecha_revocacion IS NULL) AS vigente
            FROM public.codigo_qr q
            JOIN public.establecimiento e ON e.id_establecimiento = q.id_establecimiento
            WHERE q.token_hash = ?
            """.trimIndent(),
      { rs, _ -> QrRow(rs.getString("slug"), rs.getBoolean("bar_activo"), rs.getBoolean("vigente")) },
      tokens.huellaQr(token),
    ).firstOrNull() ?: throw ContratoApiException(
      HttpStatus.NOT_FOUND, "no_encontrado", "Código QR no encontrado",
    )
    if (!qr.barActivo) {
      throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Bar no disponible")
    }
    if (!qr.vigente) {
      throw ContratoApiException(HttpStatus.GONE, "qr_reemplazado", "Este QR fue reemplazado")
    }
    return mapOf("slug" to qr.slug)
  }

  private data class BarRow(
    val id: Long,
    val slug: String,
    val nombre: String,
    val zonaHoraria: String,
    val activo: Boolean,
    val pedidosMaximos: Int?,
    val duracionMaxSeg: Int?,
    val ventanaNoRepeticion: Int?,
    val tema: String,
  )

  @GetMapping("/bares/{slug}")
  fun bar(
    @PathVariable slug: String,
    @RequestHeader(value = "X-Rockola-Dispositivo", required = false) uuid: String?,
  ): Map<String, Any> {
    dispositivos.huella(uuid)
    val bar = jdbc.query(
      """
            SELECT e.id_establecimiento, e.slug, e.nombre, e.zona_horaria, e.activo, e.tema,
                   c.max_solicitudes_dispositivo,
                   c.duracion_max_seg,
                   c.minutos_bloqueo_repeticion
            FROM public.establecimiento e
            LEFT JOIN public.configuracion_servicio c
              ON c.id_establecimiento = e.id_establecimiento
            WHERE e.slug = ?
            """.trimIndent(),
      { rs, _ ->
        BarRow(
          id = rs.getLong("id_establecimiento"),
          slug = rs.getString("slug"),
          nombre = rs.getString("nombre"),
          zonaHoraria = rs.getString("zona_horaria"),
          activo = rs.getBoolean("activo"),
          pedidosMaximos = rs.getObject("max_solicitudes_dispositivo")?.let { (it as Number).toInt() },
          duracionMaxSeg = rs.getObject("duracion_max_seg")?.let { (it as Number).toInt() },
          ventanaNoRepeticion = rs.getObject("minutos_bloqueo_repeticion")?.let { (it as Number).toInt() },
          tema = rs.getString("tema"), // RF-059 (migración V2)
        )
      },
      slug,
    ).firstOrNull() ?: throw ContratoApiException(
      HttpStatus.NOT_FOUND, "no_encontrado", "Bar no encontrado",
    )
    if (!bar.activo) {
      throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Bar no disponible")
    }
    // No inventar límites: el establecimiento debe tener configuración real en PostgreSQL.
    val pedidosMaximos = bar.pedidosMaximos ?: throw ContratoApiException(
      HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio",
    )
    val duracionMaxSeg = bar.duracionMaxSeg ?: throw ContratoApiException(
      HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio",
    )
    val ventana = bar.ventanaNoRepeticion ?: throw ContratoApiException(
      HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio",
    )
    val formato = DateTimeFormatter.ofPattern("HH:mm")
    val horario: List<Map<String, Any>> = jdbc.query(
      """
            SELECT dia_semana, hora_inicio, hora_fin, activo
            FROM public.horario_operacion
            WHERE id_establecimiento = ?
            ORDER BY dia_semana, hora_inicio
            """.trimIndent(),
      { rs, _ ->
        mapOf(
          "dia_semana" to rs.getInt("dia_semana"),
          "hora_inicio" to rs.getObject("hora_inicio", LocalTime::class.java).format(formato),
          "hora_fin" to rs.getObject("hora_fin", LocalTime::class.java).format(formato),
          "activo" to rs.getBoolean("activo"),
        )
      },
      bar.id,
    )
    return mapOf(
      "slug" to bar.slug,
      "nombre" to bar.nombre,
      "zona_horaria" to bar.zonaHoraria,
      "pedidos_maximos" to pedidosMaximos,
      "duracion_max_seg" to duracionMaxSeg,
      "ventana_no_repeticion_min" to ventana,
      "tema" to bar.tema,
      "horario" to horario,
    )
  }
}
