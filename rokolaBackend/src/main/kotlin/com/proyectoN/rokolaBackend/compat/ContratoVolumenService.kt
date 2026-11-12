package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * `PUT /api/panel/volumen` (RF-062): el volumen de la pantalla del bar, de 0 a 100, desde el
 * panel. Operador y Administrador. Se guarda en `configuracion_servicio.volumen` (migración V4),
 * sale en `ahora` y llega a la TV por el canal en vivo de la noche. El mismo valor no deja otra
 * línea en la bitácora.
 *
 * Hecho por el equipo de frontend (2026-11-12, D14) sobre el esquema reconstruido del código.
 */
@Service
class ContratoVolumenService(
  private val jdbc: JdbcTemplate,
  private val cola: CambioColaPublisher,
  private val panel: CambioPanelPublisher,
) {
  @Transactional
  fun cambiar(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?) {
    val volumen = leerVolumen(cuerpo)
    val anterior = jdbc.query(
      "SELECT volumen FROM public.configuracion_servicio WHERE id_establecimiento = ? FOR UPDATE",
      { rs, _ -> rs.getInt(1) }, p.barId,
    ).firstOrNull() ?: throw ContratoApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio")
    if (anterior == volumen) return
    jdbc.update(
      "UPDATE public.configuracion_servicio SET volumen = ?, fecha_modificacion = CURRENT_TIMESTAMP WHERE id_establecimiento = ?",
      volumen, p.barId,
    )
    jdbc.update(
      """INSERT INTO public.accion_admin (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
         VALUES (?, ?, 'cambiar_volumen', NULL, NULL, jsonb_build_object('texto', ?::text))""",
      p.barId, p.id, "$volumen %",
    )
    // Sin noche abierta no hay TV sonando: el valor queda guardado y sale en la próxima foto.
    jdbc.query(
      "SELECT id_sesion FROM public.sesion WHERE id_establecimiento = ? AND estado = 'abierta'",
      { rs, _ -> rs.getLong(1) }, p.barId,
    ).forEach { cola.publicar(it, "volumen") }
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  companion object {
    /** Entero de 0 a 100 (el contrato: `CuerpoVolumen`). Separado para probarlo sin base. */
    fun leerVolumen(cuerpo: Map<String, Any?>?): Int {
      val n = cuerpo?.get("volumen") as? Number
        ?: throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "Falta el volumen (0 a 100)")
      val d = n.toDouble()
      if (d != Math.floor(d) || d < 0 || d > 100) throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "El volumen va de 0 a 100")
      return d.toInt()
    }
  }
}
