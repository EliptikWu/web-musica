package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.reproduccion.FinalizarReproduccionRequest
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionConsultaRepository
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionGestionService
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Abrir y cerrar la noche a mano (RF-037). Solo el Administrador: lo exige el controlador.
 * Abierta fuera del horario, atiende hasta que se cierre (`sesion.fuera_de_horario`, V6).
 *
 * Hecho por el equipo de frontend (2026-11-21, D14) sobre el esquema reconstruido.
 */
@Service
class ContratoSesionesService(
  private val jdbc: JdbcTemplate,
  private val horarios: HorarioActivoService,
  private val reproducciones: ReproduccionConsultaRepository,
  private val gestion: ReproduccionGestionService,
  private val cola: CambioColaPublisher,
  private val panel: CambioPanelPublisher,
) {
  @Transactional
  fun abrir(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?) {
    val tipo = leerTipoEvento(cuerpo)
    bloquearBar(p.barId)
    if (sesionAbierta(p.barId) != null) throw conflicto("Ya hay una noche abierta")
    jdbc.update(
      """UPDATE public.configuracion_servicio SET estado_manual = 'disponible', fecha_modificacion = CURRENT_TIMESTAMP
         WHERE id_establecimiento = ? AND estado_manual = 'mantenimiento'""",
      p.barId,
    )
    val fuera = !horarios.estaEnHorario(p.barId, p.barZona)
    val id = jdbc.queryForObject(
      """INSERT INTO public.sesion (id_establecimiento, tipo_evento, estado, fuera_de_horario)
         VALUES (?, ?, 'abierta', ?) RETURNING id_sesion""",
      Long::class.java, p.barId, tipo, fuera,
    )!!
    anotar(p, "abrir_sesion", id, if (fuera) "Abierta a mano, fuera del horario" else "Abierta a mano")
    cola.publicar(id, "sesion_abierta")
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  @Transactional
  fun cerrar(p: ContratoPanelAcceso.Personal) {
    bloquearBar(p.barId)
    val id = sesionAbierta(p.barId) ?: throw conflicto("No hay una noche abierta")
    // El asignador automático toma este mismo candado: no puede empezar otra canción en medio.
    jdbc.query("SELECT 1 FROM public.sesion WHERE id_sesion = ? FOR UPDATE", { rs, _ -> rs.getInt(1) }, id)
    // Lo que suena queda cortado: saltada, con los segundos que alcanzó.
    reproducciones.actual(id)?.let {
      val sonado = it.segundosTranscurridos.coerceIn(0, it.duracionSeg.toLong()).toInt()
      gestion.finalizar(id, it.reproduccionId, FinalizarReproduccionRequest("saltada", sonado))
    }
    jdbc.update(
      """UPDATE public.solicitud SET estado = 'expirada', motivo_estado = 'La noche cerró antes de que sonara'
         WHERE id_sesion = ? AND estado = 'pendiente'""",
      id,
    )
    jdbc.update(
      "UPDATE public.sesion SET estado = 'cerrada', fecha_cierre = CURRENT_TIMESTAMP WHERE id_sesion = ?",
      id,
    )
    anotar(p, "cerrar_sesion", id, "Cerrada a mano")
    cola.publicar(id, "sesion_cerrada")
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  /** Serializa abrir y cerrar del mismo bar: dos clics no abren dos noches. */
  private fun bloquearBar(barId: Long) {
    jdbc.query(
      "SELECT 1 FROM public.configuracion_servicio WHERE id_establecimiento = ? FOR UPDATE",
      { rs, _ -> rs.getInt(1) }, barId,
    ).firstOrNull() ?: throw ContratoApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio")
  }

  private fun sesionAbierta(barId: Long): Long? = jdbc.query(
    """SELECT id_sesion FROM public.sesion WHERE id_establecimiento = ? AND estado = 'abierta'
       ORDER BY fecha_apertura DESC, id_sesion DESC LIMIT 1""",
    { rs, _ -> rs.getLong(1) }, barId,
  ).firstOrNull()

  private fun anotar(p: ContratoPanelAcceso.Personal, tipo: String, sesion: Long, texto: String) {
    jdbc.update(
      """INSERT INTO public.accion_admin (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
         VALUES (?, ?, ?, 'sesion', ?, jsonb_build_object('texto', ?::text))""",
      p.barId, p.id, tipo, sesion, texto,
    )
  }

  private fun conflicto(mensaje: String) = ContratoApiException(HttpStatus.CONFLICT, "conflicto", mensaje)

  companion object {
    internal fun leerTipoEvento(cuerpo: Map<String, Any?>?): String {
      val tipo = (cuerpo?.get("tipo_evento") as? String)?.trim()
      if (tipo.isNullOrEmpty() || tipo.length > 60) {
        throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "tipo_evento debe tener de 1 a 60 caracteres")
      }
      return tipo
    }
  }
}
