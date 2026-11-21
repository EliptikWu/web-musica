package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.HorarioEfectivoService
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
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * La noche y el servicio del bar (RF-036 a RF-039, RF-045, RN-09): abrir y cerrar a mano, pausar
 * las solicitudes, mantenimiento y el ciclo que abre y cierra según el horario efectivo (franjas
 * más eventos). El modo vive en `configuracion_servicio.estado_manual`; `forzado_abierto` atiende
 * fuera del horario y `sin_apertura_hasta` evita reabrir una noche cerrada a mano (V8).
 *
 * Cada noche se liga al evento de hoy del calendario: el que está en curso o el próximo de hoy.
 * La noche toma su `tipo_evento`; si el evento deja de ser de hoy, vuelve a `normal`.
 *
 * Quien decide los permisos por ruta es el controlador; forzar la apertura y salir de
 * mantenimiento se deciden aquí porque dependen del cuerpo y del estado (D9).
 *
 * Hecho por el equipo de frontend (2026-11-21, D14).
 */
@Service
class ContratoServicioService(
  private val jdbc: JdbcTemplate,
  private val horarioEfectivo: HorarioEfectivoService,
  private val reproducciones: ReproduccionConsultaRepository,
  private val gestion: ReproduccionGestionService,
  private val bitacora: ContratoBitacora,
  private val cola: CambioColaPublisher,
  private val panel: CambioPanelPublisher,
  private val eventos: ApplicationEventPublisher,
  transacciones: PlatformTransactionManager,
) {
  private val log = LoggerFactory.getLogger(javaClass)
  private val porBar = TransactionTemplate(transacciones)

  private data class Config(val estado: String, val forzado: Boolean, val sinAperturaHasta: OffsetDateTime?)
  private data class BarActivo(val id: Long, val slug: String, val zona: ZoneId)

  // ── Servicio (RF-036, RF-038, RF-039) ──────────────────────────────────────

  @Transactional
  fun cambiar(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?) {
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
    bitacora.anotar(p, tipo, "servicio", null, "Solicitudes", if (antes.estado == "mantenimiento") "sale de mantenimiento" else null)
    sincronizar(BarActivo(p.barId, p.barSlug, ZoneId.of(p.barZona)))
    avisar(p.barId, p.barSlug)
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  /** RF-038: corta lo que suena, expira lo pendiente y cierra la noche. Solo el Administrador. */
  @Transactional
  fun mantenimiento(p: ContratoPanelAcceso.Personal) {
    val antes = bloquearConfig(p.barId)
    val abierta = sesionAbierta(p.barId)
    if (antes.estado == "mantenimiento" && abierta == null) return
    jdbc.update(
      """UPDATE public.configuracion_servicio SET estado_manual = 'mantenimiento', forzado_abierto = FALSE,
           sin_apertura_hasta = NULL, fecha_modificacion = CURRENT_TIMESTAMP WHERE id_establecimiento = ?""",
      p.barId,
    )
    val expirados = abierta?.let { cerrarSesion(it, "Mantenimiento") } ?: 0
    bitacora.anotar(p, "mantenimiento", "sesion", abierta, "Noche", textoExpirados(expirados))
    if (abierta != null) cola.publicar(abierta, "sesion_cerrada") else eventos.publishEvent(CambioServicioEvent(p.barSlug))
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  // ── Abrir y cerrar a mano (RF-037) ─────────────────────────────────────────

  /** Con una abierta, 409. Si el bar estaba en mantenimiento, sale de él. */
  @Transactional
  fun abrirNoche(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?) {
    val tipo = leerTipoEvento(cuerpo)
    bloquearConfig(p.barId)
    if (sesionAbierta(p.barId) != null) ContratoCuerpo.conflicto("Ya hay una noche abierta")
    val zona = ZoneId.of(p.barZona)
    val fuera = !horarioEfectivo.estaAbierto(p.barId, zona)
    jdbc.update(
      """UPDATE public.configuracion_servicio SET
           estado_manual = CASE WHEN estado_manual = 'mantenimiento' THEN 'disponible' ELSE estado_manual END,
           forzado_abierto = ?, sin_apertura_hasta = NULL, fecha_modificacion = CURRENT_TIMESTAMP
         WHERE id_establecimiento = ?""",
      fuera, p.barId,
    )
    val id = insertarNoche(p.barId, tipo, "manual")
    val evento = ligar(p.barId, zona)
    val texto = listOfNotNull(if (fuera) "manual, fuera del horario" else "manual", evento?.let { "evento ${it.tipo}" }).joinToString(", ")
    bitacora.anotar(p, "abrir_sesion", "sesion", id, "Noche", texto)
    cola.publicar(id, "sesion_abierta")
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  /** Sin una abierta, 409. Dentro del horario no se reabre sola hasta que esa ventana termine. */
  @Transactional
  fun cerrarNoche(p: ContratoPanelAcceso.Personal) {
    bloquearConfig(p.barId)
    val id = sesionAbierta(p.barId) ?: ContratoCuerpo.conflicto("No hay una noche abierta")
    val expirados = cerrarSesion(id, "Noche cerrada")
    jdbc.update(
      """UPDATE public.configuracion_servicio SET forzado_abierto = FALSE, sin_apertura_hasta = ?,
           fecha_modificacion = CURRENT_TIMESTAMP WHERE id_establecimiento = ?""",
      horarioEfectivo.cierre(p.barId, ZoneId.of(p.barZona)), p.barId,
    )
    bitacora.anotar(p, "cerrar_sesion", "sesion", id, "Noche", textoExpirados(expirados))
    cola.publicar(id, "sesion_cerrada")
    panel.publicar(p.barSlug, setOf("noche", "bitacora"))
  }

  /** Liga la noche abierta al evento de hoy, o la desliga. La llaman los cambios del calendario. */
  fun ligarNocheAbierta(p: ContratoPanelAcceso.Personal): HorarioEfectivoService.EventoDeHoy? {
    val zona = ZoneId.of(p.barZona)
    // Un evento pudo alargar la ventana: la noche cerrada a mano sigue cerrada hasta el nuevo fin.
    horarioEfectivo.cierre(p.barId, zona)?.let { fin ->
      jdbc.update(
        """UPDATE public.configuracion_servicio SET sin_apertura_hasta = GREATEST(sin_apertura_hasta, ?)
           WHERE id_establecimiento = ? AND sin_apertura_hasta > CURRENT_TIMESTAMP""",
        fin, p.barId,
      )
    }
    return ligar(p.barId, zona)
  }

  /** Un cambio del calendario llega al teléfono y a la TV, haya o no noche abierta. */
  fun avisarHorario(p: ContratoPanelAcceso.Personal) {
    val abierta = sesionAbierta(p.barId)
    if (abierta != null) cola.publicar(abierta, "horario") else eventos.publishEvent(CambioServicioEvent(p.barSlug))
  }

  // ── Según el horario (RF-037, RN-09) ───────────────────────────────────────

  @Scheduled(initialDelay = 5_000L, fixedDelay = 15_000L)
  fun sincronizarNoches() {
    val bares = try {
      jdbc.query(
        """SELECT e.id_establecimiento, e.slug, e.zona_horaria FROM public.establecimiento e
           JOIN public.configuracion_servicio c ON c.id_establecimiento = e.id_establecimiento
           WHERE e.activo = TRUE""",
        { rs, _ -> BarActivo(rs.getLong(1), rs.getString(2), ZoneId.of(rs.getString(3))) },
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
    val enHorario = horarioEfectivo.estaAbierto(bar.id, bar.zona)
    val abierta = sesionAbierta(bar.id)
    when (accionHorario(abierta != null, enHorario, c.forzado, vetada)) {
      AccionHorario.ABRIR -> {
        val id = insertarNoche(bar.id, "normal", if (enHorario) "horario" else "manual")
        ligar(bar.id, bar.zona)
        bitacora.anotar(null, "abrir_sesion", "sesion", id, "Noche", if (enHorario) "Por horario" else "Apertura forzada", bar.id)
        cola.publicar(id, "sesion_abierta")
      }
      AccionHorario.CERRAR -> {
        val expirados = cerrarSesion(abierta!!, "Noche cerrada")
        bitacora.anotar(null, "cerrar_sesion", "sesion", abierta, "Noche", "Por horario, ${textoExpirados(expirados)}", bar.id)
        cola.publicar(abierta, "sesion_cerrada")
      }
      AccionHorario.NADA -> return
    }
    panel.publicar(bar.slug, setOf("noche", "bitacora"))
  }

  // ── Comunes ────────────────────────────────────────────────────────────────

  private fun insertarNoche(idBar: Long, tipo: String, origen: String): Long = jdbc.queryForObject(
    "INSERT INTO public.sesion (id_establecimiento, tipo_evento, estado, origen) VALUES (?, ?, 'abierta', ?) RETURNING id_sesion",
    Long::class.javaObjectType, idBar, tipo, origen,
  )!!

  private fun ligar(idBar: Long, zona: ZoneId): HorarioEfectivoService.EventoDeHoy? {
    val sesion = sesionAbierta(idBar) ?: return null
    val evento = horarioEfectivo.eventoDeHoy(idBar, zona)
    val ahoraEnElBar = LocalDateTime.now(zona)
    // Un evento que ya empezó sigue siendo el de esta noche aunque haya terminado.
    val sinEventoEmpezado = """NOT EXISTS (SELECT 1 FROM public.evento_calendario e
        WHERE e.id_evento_calendario = s.id_evento_calendario AND e.fecha + e.hora_inicio <= ?)"""
    if (evento != null) {
      // Un tipo elegido a mano y distinto del evento se respeta.
      jdbc.update(
        """UPDATE public.sesion s SET id_evento_calendario = ?, tipo_evento = ?
           WHERE s.id_sesion = ? AND s.id_evento_calendario IS DISTINCT FROM ?
             AND (s.tipo_evento = 'normal' OR s.tipo_evento = ? OR s.id_evento_calendario IS NOT NULL)
             AND $sinEventoEmpezado""",
        evento.id, evento.tipo, sesion, evento.id, evento.tipo, ahoraEnElBar,
      )
    } else {
      // Solo si estaba ligada: un tipo puesto a mano sin evento se respeta.
      jdbc.update(
        """UPDATE public.sesion s SET id_evento_calendario = NULL, tipo_evento = 'normal'
           WHERE s.id_sesion = ? AND s.id_evento_calendario IS NOT NULL AND $sinEventoEmpezado""",
        sesion, ahoraEnElBar,
      )
    }
    val ligada = jdbc.queryForObject(
      "SELECT id_evento_calendario FROM public.sesion WHERE id_sesion = ?", Long::class.javaObjectType, sesion,
    )
    return evento?.takeIf { it.id == ligada }
  }

  /** Corta lo que suena con los segundos que alcanzó, expira lo pendiente y cierra. Devuelve cuántos expiraron. */
  private fun cerrarSesion(id: Long, motivo: String): Int {
    // El asignador automático toma este mismo candado: no puede empezar otra canción en medio.
    jdbc.query("SELECT 1 FROM public.sesion WHERE id_sesion = ? FOR UPDATE", { rs, _ -> rs.getInt(1) }, id)
    reproducciones.actual(id)?.let {
      val sonado = it.segundosTranscurridos.coerceIn(0, it.duracionSeg.toLong()).toInt()
      gestion.finalizar(id, it.reproduccionId, FinalizarReproduccionRequest("saltada", sonado))
    }
    val expirados = jdbc.update(
      "UPDATE public.solicitud SET estado = 'expirada', motivo_estado = ? WHERE id_sesion = ? AND estado = 'pendiente'",
      motivo, id,
    )
    jdbc.update(
      "UPDATE public.sesion SET estado = 'cerrada', fecha_cierre = GREATEST(fecha_apertura, CURRENT_TIMESTAMP) WHERE id_sesion = ?",
      id,
    )
    return expirados
  }

  private fun textoExpirados(n: Int) = if (n == 1) "1 pedido pendiente expiró" else "$n pedidos pendientes expiraron"

  /** Teléfonos y TV: por la noche abierta, o por el bar si no hay ninguna. */
  private fun avisar(idBar: Long, slug: String) {
    val abierta = sesionAbierta(idBar)
    if (abierta != null) cola.publicar(abierta, "servicio") else eventos.publishEvent(CambioServicioEvent(slug))
  }

  /** Serializa todo lo que cambia la noche de un bar: dos clics o el ciclo no abren dos noches. */
  private fun bloquearConfig(idBar: Long): Config = jdbc.query(
    """SELECT estado_manual, forzado_abierto, sin_apertura_hasta FROM public.configuracion_servicio
       WHERE id_establecimiento = ? FOR UPDATE""",
    { rs, _ -> Config(rs.getString(1), rs.getBoolean(2), rs.getObject(3, OffsetDateTime::class.java)) }, idBar,
  ).firstOrNull() ?: throw ContratoApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno", "El bar no tiene configuración del servicio")

  private fun sesionAbierta(idBar: Long): Long? = jdbc.query(
    "SELECT id_sesion FROM public.sesion WHERE id_establecimiento = ? AND estado = 'abierta' ORDER BY fecha_apertura DESC, id_sesion DESC LIMIT 1",
    { rs, _ -> rs.getLong(1) }, idBar,
  ).firstOrNull()

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
