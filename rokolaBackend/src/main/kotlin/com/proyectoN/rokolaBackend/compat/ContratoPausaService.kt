package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * `POST /api/panel/reproduccion/{id}/pausa` y `/reanudacion` (RF-040). Operador y Administrador.
 * La pausa se guarda en `reproduccion` (migración V3): el reloj la descuenta, el estado pasa a
 * `pausada` y la TV detiene el video. Es idempotente por naturaleza: pausar lo pausado no cambia
 * nada ni deja otra línea en la bitácora.
 *
 * Hecho por el equipo de frontend (2026-11-12, D14) sobre el esquema reconstruido del código.
 */
@Service
class ContratoPausaService(
  private val jdbc: JdbcTemplate,
  private val cola: CambioColaPublisher,
  private val panel: CambioPanelPublisher,
) {
  private data class Fila(val sesionId: Long, val cancionId: Long, val pausada: Boolean)

  @Transactional
  fun pausar(p: ContratoPanelAcceso.Personal, idTexto: String) = cambiar(p, idTexto, pausar = true)

  @Transactional
  fun reanudar(p: ContratoPanelAcceso.Personal, idTexto: String) = cambiar(p, idTexto, pausar = false)

  private fun cambiar(p: ContratoPanelAcceso.Personal, idTexto: String, pausar: Boolean) {
    val id = idTexto.toLongOrNull()?.takeIf { it > 0L } ?: throw noEncontrada()
    // Solo reproducciones de su propio bar: un id de otro bar es «no encontrado», no «sin permiso».
    val sesionId = jdbc.query(
      """SELECT r.id_sesion FROM public.reproduccion r JOIN public.sesion s ON s.id_sesion = r.id_sesion
         WHERE r.id_reproduccion = ? AND s.id_establecimiento = ?""",
      { rs, _ -> rs.getLong(1) }, id, p.barId,
    ).firstOrNull() ?: throw noEncontrada()
    // Bloquear la noche y volver a leer: no se cruza con el fin que reporta la TV ni con saltar.
    jdbc.query("SELECT 1 FROM public.sesion WHERE id_sesion = ? FOR UPDATE", RowCallbackHandler { }, sesionId)
    val fila = jdbc.query(
      """SELECT r.id_sesion, r.id_cancion, r.pausada_desde IS NOT NULL
         FROM public.reproduccion r JOIN public.sesion s ON s.id_sesion = r.id_sesion
         WHERE r.id_reproduccion = ? AND r.resultado = 'iniciada' AND s.estado = 'abierta'""",
      { rs, _ -> Fila(rs.getLong(1), rs.getLong(2), rs.getBoolean(3)) }, id,
    ).firstOrNull() ?: throw noEncontrada() // Ya terminó o la noche se cerró.
    if (fila.pausada == pausar) return

    if (pausar) {
      jdbc.update("UPDATE public.reproduccion SET pausada_desde = CURRENT_TIMESTAMP WHERE id_reproduccion = ?", id)
    } else {
      jdbc.update(
        """UPDATE public.reproduccion
           SET segundos_en_pausa = segundos_en_pausa
                 + GREATEST(0, FLOOR(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - pausada_desde))))::int,
               pausada_desde = NULL
           WHERE id_reproduccion = ?""",
        id,
      )
    }
    jdbc.update(
      """INSERT INTO public.accion_admin (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
         VALUES (?, ?, ?, 'cancion', ?, NULL)""",
      p.barId, p.id, if (pausar) "pausar_reproduccion" else "reanudar_reproduccion", fila.cancionId,
    )
    cola.publicar(fila.sesionId, if (pausar) "reproduccion_pausada" else "reproduccion_reanudada") // teléfonos y TV
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  private fun noEncontrada() = ContratoApiException(HttpStatus.NOT_FOUND, "no_encontrado", "Reproducción no encontrada")
}
