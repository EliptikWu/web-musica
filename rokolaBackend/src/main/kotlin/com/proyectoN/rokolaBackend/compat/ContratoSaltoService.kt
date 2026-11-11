package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.reproduccion.FinalizarReproduccionRequest
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionGestionService
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

/**
 * `POST /panel/reproduccion/{id}/salto` (RF-040): el personal salta la canción que suena.
 * Si ese id ya no es el que suena, responde 204 sin hacer nada (otro la saltó o terminó).
 * Idempotente por `Idempotency-Key`, con la misma tabla que pedir y votar.
 */
@Service
class ContratoSaltoService(
  private val jdbc: JdbcTemplate,
  private val gestion: ReproduccionGestionService,
  private val panel: CambioPanelPublisher,
) {
  private data class Fila(val sesionId: Long, val cancionId: Long)

  @Transactional
  fun saltar(p: ContratoPanelAcceso.Personal, idTexto: String, claveCabecera: String?) {
    val idReproduccion = idTexto.toLongOrNull()?.takeIf { it > 0L }
      ?: throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Reproducción no encontrada")
    val clave = try { UUID.fromString(claveCabecera) } catch (_: Exception) { null }
    if (clave == null || clave.toString() != claveCabecera?.lowercase()) {
      throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "Idempotency-Key debe ser un UUID")
    }
    val quien = sha256("panel:${p.id}")
    val huella = sha256("panel_saltar|${p.barId}|$idReproduccion")
    jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", RowCallbackHandler { }, "${p.barId}|$quien|$clave")
    val previo = jdbc.query(
      "SELECT operacion, huella_peticion FROM public.api_idempotencia WHERE id_establecimiento = ? AND device_hash = ? AND clave = ?",
      { rs, _ -> rs.getString(1) to rs.getString(2) }, p.barId, quien, clave,
    ).firstOrNull()
    if (previo != null) {
      if (previo.first != "panel_saltar" || previo.second != huella) {
        throw ContratoApiException(HttpStatus.CONFLICT, "conflicto", "La clave de idempotencia ya se usó con otra petición")
      }
      return
    }

    // Solo reproducciones de su propio bar: un id de otro bar es «no encontrado», no «sin permiso».
    val fila = jdbc.query(
      """SELECT r.id_sesion, r.id_cancion
         FROM public.reproduccion r JOIN public.sesion s ON s.id_sesion = r.id_sesion
         WHERE r.id_reproduccion = ? AND s.id_establecimiento = ?""",
      { rs, _ -> Fila(rs.getLong(1), rs.getLong(2)) },
      idReproduccion, p.barId,
    ).firstOrNull() ?: throw ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Reproducción no encontrada")

    // Bloquear la noche y volver a leer: así no se cruza con el fin que reporta la TV ni con
    // la siguiente asignación, y finalizar() no falla a mitad de la transacción.
    jdbc.query("SELECT 1 FROM public.sesion WHERE id_sesion = ? FOR UPDATE", RowCallbackHandler { }, fila.sesionId)
    val sigueSonando = jdbc.queryForObject(
      """SELECT EXISTS(SELECT 1 FROM public.reproduccion r JOIN public.sesion s ON s.id_sesion = r.id_sesion
         WHERE r.id_reproduccion = ? AND r.resultado = 'iniciada' AND s.estado = 'abierta')""",
      Boolean::class.javaObjectType, idReproduccion,
    ) == true
    if (sigueSonando) {
      gestion.finalizar(fila.sesionId, idReproduccion, FinalizarReproduccionRequest("saltada"))
      jdbc.update(
        """INSERT INTO public.accion_admin (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
           VALUES (?, ?, 'saltar', 'cancion', ?, NULL)""",
        p.barId, p.id, fila.cancionId,
      )
      panel.publicar(p.barSlug, setOf("noche", "bitacora"))
    }
    guardarClave(p.barId, quien, clave, huella)
  }

  private fun guardarClave(idBar: Long, quien: String, clave: UUID, huella: String) {
    jdbc.update(
      """INSERT INTO public.api_idempotencia
         (id_establecimiento, device_hash, clave, operacion, huella_peticion, respuesta_http, respuesta_json)
         VALUES (?, ?, ?, 'panel_saltar', ?, 204, '{}')""",
      idBar, quien, clave, huella,
    )
  }

  private fun sha256(texto: String): String = HexFormat.of().formatHex(
    MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8)),
  )
}
