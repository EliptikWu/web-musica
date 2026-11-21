package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.reproduccion.FinalizarReproduccionRequest
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionConsultaRepository
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionGestionService
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioServicioEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * La noche y el servicio del bar (RF-036 a RF-039, RN-09): abrir y cerrar a mano, pausar las
 * solicitudes, mantenimiento y el ciclo que abre y cierra según el horario. Quien decide los
 * permisos por ruta es el controlador; forzar la apertura y salir de mantenimiento se deciden
 * aquí porque dependen del cuerpo y del estado (D9).
 *
 * Hecho por el equipo de frontend (2026-11-21, D14) sobre el esquema reconstruido.
 */
@Service
class ContratoSesionesService(
  private val jdbc: JdbcTemplate,
  private val horarios: HorarioActivoService,
  private val franjas: ContratoHorarioService,
  private val reproducciones: ReproduccionConsultaRepository,
  private val gestion: ReproduccionGestionService,
  private val cola: CambioColaPublisher,
  private val panel: CambioPanelPublisher,
  private val eventos: ApplicationEventPublisher,
  transacciones: PlatformTransactionManager,
) {
  private val log = LoggerFactory.getLogger(javaClass)
  private val porBar = TransactionTemplate(transacciones)
  /** Fuera de la transacción: si esta consulta falla, no debe impedir abrir la noche. */
  private val aparte = TransactionTemplate(transacciones).apply { propagationBehavior = TransactionDefinition.PROPAGATION_NOT_SUPPORTED }

  private data class Config(val estado: String, val forzado: Boolean, val sinAperturaHasta: OffsetDateTime?)
  private data class BarActivo(val id: Long, val slug: String, val zona: String)

  // ── Abrir y cerrar a mano (RF-037) ─────────────────────────────────────────

  @Transactional
  fun abrir(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?) {
    val tipo = leerTipoEvento(cuerpo)
    bloquearConfig(p.barId)
    if (sesionAbierta(p.barId) != null) throw conflicto("Ya hay una noche abierta")
    val fuera = !horarios.estaEnHorario(p.barId, p.barZona)
    jdbc.update(
      """UPDATE public.configuracion_servicio SET
           estado_manual = CASE WHEN estado_manual = 'mantenimiento' THEN 'disponible' ELSE estado_manual END,
           forzado_abierto = ?, sin_apertura_hasta = NULL, fecha_modificacion = CURRENT_TIMESTAMP
         WHERE id_establecimiento = ?""",
      fuera, p.barId,
    )
    abrirNoche(p.barId, tipo, p.id, if (fuera) "Abierta a mano, fuera del horario" else "Abierta a mano", "manual")
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  @Transactional
  fun cerrar(p: ContratoPanelAcceso.Personal) {
    bloquearConfig(p.barId)
    val id = sesionAbierta(p.barId) ?: throw conflicto("No hay una noche abierta")
    cerrarNoche(p.barId, id, p.id, "cerrar_sesion", "Cerrada a mano")
    // Cerrada dentro de una franja: no se reabre sola hasta que esa franja termine.
    jdbc.update(
      """UPDATE public.configuracion_servicio SET forzado_abierto = FALSE, sin_apertura_hasta = ?,
           fecha_modificacion = CURRENT_TIMESTAMP WHERE id_establecimiento = ?""",
      ReglasHorario.cierreContiguo(franjas.franjas(p.barId), OffsetDateTime.now(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of(p.barZona)))?.toOffsetDateTime(),
      p.barId,
    )
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  // ── Servicio (RF-036, RF-038, RF-039) ──────────────────────────────────────

  @Transactional
  fun cambiarServicio(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?) {
    val modo = leerModo(cuerpo)
    val antes = bloquearConfig(p.barId)
    // Quitar lo forzado cierra la noche fuera de horario, y cerrarla es del Administrador.
    val quitaForzado = modo == "automatico" && antes.forzado && antes.estado != "pausado"
    if (p.rol != "administrador" && (modo == "forzado_activo" || antes.estado == "mantenimiento" || quitaForzado)) {
      throw ContratoApiException(HttpStatus.FORBIDDEN, "sin_permiso", "Tu rol no permite esta acción")
    }
    val modoAntes = if (antes.estado == "pausado") "pausado" else if (antes.forzado) "forzado_activo" else "automatico"
    if (modo == modoAntes && antes.estado != "mantenimiento") return
    // Pausar y reanudar una noche forzada no la cierra: sigue forzada hasta que la cierren.
    val forzado = modo == "forzado_activo" || (antes.forzado && (modo == "pausado" || antes.estado == "pausado"))
    jdbc.update(
      """UPDATE public.configuracion_servicio SET estado_manual = ?, forzado_abierto = ?,
           sin_apertura_hasta = CASE WHEN ? THEN NULL ELSE sin_apertura_hasta END,
           fecha_modificacion = CURRENT_TIMESTAMP WHERE id_establecimiento = ?""",
      if (modo == "pausado") "pausado" else "disponible", forzado, modo == "forzado_activo", p.barId,
    )
    val tipo = when (modo) {
      "pausado" -> "pausar_solicitudes"
      "forzado_activo" -> "forzar_apertura"
      else -> "reanudar_solicitudes"
    }
    anotar(p.barId, p.id, tipo, "servicio", null, null)
    sincronizar(BarActivo(p.barId, p.barSlug, p.barZona))
    avisar(p.barId, p.barSlug)
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  /** RF-038: cierra la noche y lo pendiente expira. Solo el Administrador (controlador). */
  @Transactional
  fun mantenimiento(p: ContratoPanelAcceso.Personal) {
    val antes = bloquearConfig(p.barId)
    if (antes.estado == "mantenimiento") return
    jdbc.update(
      """UPDATE public.configuracion_servicio SET estado_manual = 'mantenimiento', forzado_abierto = FALSE,
           sin_apertura_hasta = NULL, fecha_modificacion = CURRENT_TIMESTAMP WHERE id_establecimiento = ?""",
      p.barId,
    )
    val abierta = sesionAbierta(p.barId)
    if (abierta != null) cerrarNoche(p.barId, abierta, p.id, "mantenimiento", "Modo mantenimiento")
    else {
      anotar(p.barId, p.id, "mantenimiento", "servicio", null, "Modo mantenimiento")
      eventos.publishEvent(CambioServicioEvent(p.barSlug))
    }
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  // ── Según el horario (RF-037, RN-09) ───────────────────────────────────────

  @Scheduled(initialDelay = 5_000L, fixedDelay = 15_000L)
  fun sincronizarNoches() {
    val bares = try {
      jdbc.query(
        """SELECT e.id_establecimiento, e.slug, e.zona_horaria FROM public.establecimiento e
           JOIN public.configuracion_servicio c ON c.id_establecimiento = e.id_establecimiento
           WHERE e.activo = TRUE""",
        { rs, _ -> BarActivo(rs.getLong(1), rs.getString(2), rs.getString(3)) },
      )
    } catch (e: Exception) {
      log.warn("No se pudo revisar el horario de los bares: {}", e.javaClass.simpleName)
      return
    }
    // Cada bar en su transacción: el error de uno no detiene a los demás.
    bares.forEach { bar ->
      try {
        porBar.executeWithoutResult { sincronizar(bar) }
      } catch (e: Exception) {
        log.warn("No se pudo sincronizar la noche del bar {}: {}", bar.id, e.javaClass.simpleName)
      }
    }
  }

  private fun sincronizar(bar: BarActivo) {
    val c = bloquearConfig(bar.id)
    if (c.estado == "mantenimiento") return
    val ahora = OffsetDateTime.now(ZoneOffset.UTC)
    val vetada = c.sinAperturaHasta?.isAfter(ahora) == true
    if (c.sinAperturaHasta != null && !vetada) {
      jdbc.update("UPDATE public.configuracion_servicio SET sin_apertura_hasta = NULL WHERE id_establecimiento = ?", bar.id)
    }
    val enHorario = horarios.estaEnHorario(bar.id, bar.zona)
    val abierta = sesionAbierta(bar.id)
    when (accionHorario(abierta != null, enHorario, c.forzado, vetada)) {
      AccionHorario.ABRIR -> abrirNoche(bar.id, "normal", null, if (enHorario) "Por horario" else "Apertura forzada", if (enHorario) "horario" else "manual")
      AccionHorario.CERRAR -> cerrarNoche(bar.id, abierta!!, null, "cerrar_sesion", "Por horario")
      AccionHorario.NADA -> return
    }
    panel.publicar(bar.slug, setOf("noche", "bitacora"))
  }

  // ── Comunes ────────────────────────────────────────────────────────────────

  private fun eventoVigente(barId: Long): Pair<Long, String?>? = try {
    aparte.execute {
      jdbc.query(
        """SELECT id_evento_calendario, tipo_evento FROM public.evento_calendario
           WHERE id_establecimiento = ? AND fecha_inicio <= CURRENT_TIMESTAMP AND fecha_fin > CURRENT_TIMESTAMP
           ORDER BY fecha_inicio DESC LIMIT 1""",
        { rs, _ -> rs.getLong(1) to rs.getString(2) }, barId,
      ).firstOrNull()
    }
  } catch (e: Exception) {
    log.warn("No se pudo consultar el calendario del bar {}: {}", barId, e.javaClass.simpleName)
    null
  }

  private fun abrirNoche(barId: Long, tipoPedido: String, usuario: Long?, texto: String, origen: String) {
    // Si el calendario tiene un evento ahora, la noche queda asociada a él (RF-045, RF-053),
    // salvo que el Administrador haya elegido a propósito otro tipo de noche.
    val evento = eventoVigente(barId)?.takeIf { tipoPedido == "normal" || it.second == tipoPedido }
    val id = jdbc.queryForObject(
      """INSERT INTO public.sesion (id_establecimiento, id_evento_calendario, tipo_evento, estado, origen)
         VALUES (?, ?, ?, 'abierta', ?) RETURNING id_sesion""",
      Long::class.java, barId, evento?.first, evento?.second ?: tipoPedido, origen,
    )!!
    anotar(barId, usuario, "abrir_sesion", "sesion", id, texto)
    cola.publicar(id, "sesion_abierta")
  }

  private fun cerrarNoche(barId: Long, id: Long, usuario: Long?, tipoAccion: String, texto: String) {
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
    jdbc.update("UPDATE public.sesion SET estado = 'cerrada', fecha_cierre = CURRENT_TIMESTAMP WHERE id_sesion = ?", id)
    anotar(barId, usuario, tipoAccion, "sesion", id, texto)
    cola.publicar(id, "sesion_cerrada")
  }

  /** Teléfonos y TV: por la noche abierta, o por el bar si no hay ninguna. */
  private fun avisar(barId: Long, slug: String) {
    val abierta = sesionAbierta(barId)
    if (abierta != null) cola.publicar(abierta, "servicio") else eventos.publishEvent(CambioServicioEvent(slug))
  }

  /** Serializa todo lo que cambia la noche de un bar: dos clics o el ciclo no abren dos noches. */
  private fun bloquearConfig(barId: Long): Config = jdbc.query(
    """SELECT estado_manual, forzado_abierto, sin_apertura_hasta FROM public.configuracion_servicio
       WHERE id_establecimiento = ? FOR UPDATE""",
    { rs, _ -> Config(rs.getString(1), rs.getBoolean(2), rs.getObject(3, OffsetDateTime::class.java)) }, barId,
  ).firstOrNull() ?: throw ContratoApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio")

  private fun sesionAbierta(barId: Long): Long? = jdbc.query(
    """SELECT id_sesion FROM public.sesion WHERE id_establecimiento = ? AND estado = 'abierta'
       ORDER BY fecha_apertura DESC, id_sesion DESC LIMIT 1""",
    { rs, _ -> rs.getLong(1) }, barId,
  ).firstOrNull()

  private fun anotar(barId: Long, usuario: Long?, tipo: String, objetivoTipo: String, objetivoId: Long?, texto: String?) {
    jdbc.update(
      """INSERT INTO public.accion_admin (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
         VALUES (?, ?, ?, ?, ?, jsonb_strip_nulls(jsonb_build_object('texto', ?::text)))""",
      barId, usuario, tipo, objetivoTipo, objetivoId, texto,
    )
  }

  private fun conflicto(mensaje: String) = ContratoApiException(HttpStatus.CONFLICT, "conflicto", mensaje)

  internal enum class AccionHorario { ABRIR, CERRAR, NADA }

  companion object {
    private val MODOS = setOf("automatico", "forzado_activo", "pausado")

    /** RN-09: abre en horario o forzada, salvo veto; cierra fuera de horario si no está forzada. */
    internal fun accionHorario(hayNoche: Boolean, enHorario: Boolean, forzado: Boolean, vetada: Boolean): AccionHorario = when {
      !hayNoche && !vetada && (enHorario || forzado) -> AccionHorario.ABRIR
      hayNoche && !enHorario && !forzado -> AccionHorario.CERRAR
      else -> AccionHorario.NADA
    }

    internal fun leerModo(cuerpo: Map<String, Any?>?): String =
      (cuerpo?.get("modo_qr") as? String)?.takeIf { it in MODOS }
        ?: throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "modo_qr debe ser automatico, forzado_activo o pausado")

    internal fun leerTipoEvento(cuerpo: Map<String, Any?>?): String {
      val tipo = (cuerpo?.get("tipo_evento") as? String)?.trim()
      if (tipo.isNullOrEmpty() || tipo.length > 60) {
        throw ContratoApiException(HttpStatus.BAD_REQUEST, "datos_invalidos", "tipo_evento debe tener de 1 a 60 caracteres")
      }
      return tipo
    }
  }
}
