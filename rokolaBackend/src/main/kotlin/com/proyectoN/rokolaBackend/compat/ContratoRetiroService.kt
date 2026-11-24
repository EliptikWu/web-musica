package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

/** RF-041: retiro de una solicitud por el personal del establecimiento. */
@Service
class ContratoRetiroService(
  private val jdbc: JdbcTemplate,
  private val bitacora: ContratoBitacora,
  private val cola: CambioColaPublisher,
  private val panel: CambioPanelPublisher,
) {
  private data class SolicitudParaRetiro(val cancionId: Long, val estado: String, val estadoSesion: String)

  /**
   * 204 al retirar. El mismo Idempotency-Key y el mismo contenido repiten 204 sin alterar nada.
   * La sesión se bloquea primero, igual que al pedir, votar o iniciar una reproducción.
   */
  @Transactional
  fun retirar(
    personal: ContratoPanelAcceso.Personal,
    idTexto: String,
    cuerpo: Map<String, Any?>?,
    claveCabecera: String?,
  ) {
    val idSolicitud = ContratoCuerpo.id(idTexto, "Solicitud no encontrada")
    val datos = ContratoDatosRetiro.validar(cuerpo)
    val clave = claveCabecera?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (clave == null || clave.toString() != claveCabecera?.lowercase()) {
      ContratoCuerpo.invalido("Idempotency-Key debe ser un UUID")
    }

    val quien = sha256("panel:${personal.id}")
    val huella = sha256("panel_retirar|${personal.barId}|$idSolicitud|${datos.motivo}|${datos.detalle?.length ?: 0}:${datos.detalle.orEmpty()}")
    jdbc.query(
      "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
      RowCallbackHandler { },
      "${personal.barId}|$quien|$clave",
    )
    val previo = jdbc.query(
      """SELECT operacion, huella_peticion FROM public.api_idempotencia
         WHERE id_establecimiento = ? AND device_hash = ? AND clave = ?""",
      { rs, _ -> rs.getString("operacion") to rs.getString("huella_peticion") },
      personal.barId, quien, clave,
    ).firstOrNull()
    if (previo != null) {
      if (previo.first != "panel_retirar" || previo.second != huella) {
        ContratoCuerpo.conflicto("La clave de idempotencia ya se usó con otra petición")
      }
      return
    }

    // Primero localizar la noche dentro del bar autenticado; jamás operar en otro bar.
    val idSesion = jdbc.query(
      """SELECT so.id_sesion FROM public.solicitud so
         JOIN public.sesion s ON s.id_sesion = so.id_sesion
         WHERE so.id_solicitud = ? AND s.id_establecimiento = ?""",
      { rs, _ -> rs.getLong(1) }, idSolicitud, personal.barId,
    ).firstOrNull() ?: throw ContratoApiException(
      HttpStatus.GONE, "no_pendiente", "Esa canción ya no está en la cola",
    )

    jdbc.query("SELECT 1 FROM public.sesion WHERE id_sesion = ? FOR UPDATE", RowCallbackHandler { }, idSesion)
    val solicitud = jdbc.query(
      """SELECT so.id_cancion, so.estado, s.estado AS estado_sesion
         FROM public.solicitud so
         JOIN public.sesion s ON s.id_sesion = so.id_sesion
         WHERE so.id_solicitud = ? AND so.id_sesion = ? AND s.id_establecimiento = ?
         FOR UPDATE OF so""",
      { rs, _ -> SolicitudParaRetiro(rs.getLong("id_cancion"), rs.getString("estado"), rs.getString("estado_sesion")) },
      idSolicitud, idSesion, personal.barId,
    ).firstOrNull() ?: throw ContratoApiException(
      HttpStatus.GONE, "no_pendiente", "Esa canción ya no está en la cola",
    )
    if (solicitud.estadoSesion != "abierta" || solicitud.estado != "pendiente") {
      throw ContratoApiException(HttpStatus.GONE, "no_pendiente", "Esa canción ya no está en la cola")
    }

    val cambiadas = jdbc.update(
      """UPDATE public.solicitud SET estado = 'retirada', motivo_estado = ?
         WHERE id_solicitud = ? AND id_sesion = ? AND estado = 'pendiente'""",
      datos.texto, idSolicitud, idSesion,
    )
    if (cambiadas != 1) {
      ContratoCuerpo.conflicto("La solicitud cambió mientras se retiraba")
    }
    jdbc.update(
      """UPDATE public.voto SET activo = FALSE, fecha_retiro = CURRENT_TIMESTAMP
         WHERE id_solicitud = ? AND activo = TRUE""", idSolicitud,
    )
    bitacora.anotar(personal, "retirar", "cancion", solicitud.cancionId, texto = datos.texto)
    jdbc.update(
      """INSERT INTO public.api_idempotencia
         (id_establecimiento, device_hash, clave, operacion, huella_peticion, respuesta_http, respuesta_json)
         VALUES (?, ?, ?, 'panel_retirar', ?, 204, '{}')""",
      personal.barId, quien, clave, huella,
    )
    // Se entregan sólo después de confirmar la transacción.
    cola.publicar(idSesion, "retiro_panel")
    panel.publicar(personal.barSlug, setOf("noche", "bitacora"))
  }

  private fun sha256(valor: String): String = HexFormat.of().formatHex(
    MessageDigest.getInstance("SHA-256").digest(valor.toByteArray(Charsets.UTF_8)),
  )
}

/** Validación del CuerpoRetiro del contrato del frontend. */
internal data class ContratoDatosRetiro(val motivo: String, val detalle: String?) {
  val texto: String get() {
    val nombre = when (motivo) {
      "inapropiada" -> "Inapropiada"
      "duplicada" -> "Duplicada"
      else -> "Otro"
    }
    return if (detalle == null) nombre else "$nombre: $detalle"
  }

  companion object {
    fun validar(cuerpo: Map<String, Any?>?): ContratoDatosRetiro {
      val datos = cuerpo ?: ContratoCuerpo.invalido("Falta el cuerpo del retiro")
      ContratoCuerpo.exigirCampos(datos, setOf("motivo", "detalle"))
      val motivo = ContratoCuerpo.opcion(
        ContratoCuerpo.texto(datos, "motivo", max = 20, obligatorio = true),
        "motivo", setOf("inapropiada", "duplicada", "otro"),
      ) { it }
      val detalle = ContratoCuerpo.texto(datos, "detalle", max = 200, vacioPermitido = true)
        ?.takeIf(String::isNotBlank)
      return ContratoDatosRetiro(motivo, detalle)
    }
  }
}
