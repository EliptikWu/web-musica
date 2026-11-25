package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * `GET/PUT /api/panel/horario` (RF-036): el horario semanal del bar, solo el Administrador y solo
 * su bar. Guardar reemplaza todas las franjas de una vez, como la simulación de la web.
 *
 * Hecho por el equipo de frontend (2026-11-21, D14).
 */
@Service
class ContratoHorarioService(
  private val jdbc: JdbcTemplate,
  private val bitacora: ContratoBitacora,
  private val panel: CambioPanelPublisher,
  private val servicio: ContratoServicioService,
) {
  fun leer(p: ContratoPanelAcceso.Personal): Map<String, Any?> =
    mapOf("zona_horaria" to p.barZona, "franjas" to franjas(p.barId).map(ReglasHorario.Franja::aContrato))

  fun franjas(idBar: Long): List<ReglasHorario.Franja> = jdbc.query(
    """SELECT dia_semana, hora_inicio, hora_fin, activo FROM public.horario_operacion
       WHERE id_establecimiento = ? ORDER BY dia_semana, hora_inicio""",
    { rs, _ ->
      ReglasHorario.Franja(rs.getInt(1), rs.getObject(2, LocalTime::class.java), rs.getObject(3, LocalTime::class.java), rs.getBoolean(4))
    },
    idBar,
  )

  @Transactional
  fun guardar(p: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?): Map<String, Any?> {
    val franjas = ReglasHorario.leer(cuerpo ?: emptyMap())
    // Bloquea el bar: dos guardados a la vez no mezclan sus franjas.
    jdbc.query("SELECT 1 FROM public.establecimiento WHERE id_establecimiento = ? FOR UPDATE", { _, _ -> }, p.barId)
    jdbc.update("DELETE FROM public.horario_operacion WHERE id_establecimiento = ?", p.barId)
    jdbc.batchUpdate(
      "INSERT INTO public.horario_operacion (id_establecimiento, dia_semana, hora_inicio, hora_fin, activo) VALUES (?, ?, ?, ?, ?)",
      franjas.map { arrayOf<Any>(p.barId, it.dia.toShort(), it.inicio, it.fin, it.activo) },
    )
    val dias = franjas.count { it.activo }
    bitacora.anotar(p, "editar_horario", "horario", null, "Horario", "$dias franjas activas")
    // El teléfono y la TV ven el horario nuevo haya o no noche abierta: sin noche es justo cuando
    // muestran «Los pedidos abren…» (punto 7 del equipo). Como los eventos (RF-045).
    servicio.avisarHorario(p)
    panel.publicar(p.barSlug, setOf("horario", "noche", "bitacora"))
    return leer(p)
  }
}

/**
 * Las reglas del horario (RF-036, RN-09), las mismas de `frontend/web/lib/reglas/horario.ts`: una
 * franja que cruza la medianoche pertenece al día en que empieza; empezar y terminar a la misma
 * hora no es válido (400) y dos franjas activas que se pisan son un conflicto (409).
 */
object ReglasHorario {
  data class Franja(val dia: Int, val inicio: LocalTime, val fin: LocalTime, val activo: Boolean) {
    fun aContrato(): Map<String, Any> = mapOf(
      "dia_semana" to dia, "hora_inicio" to inicio.format(HHMM), "hora_fin" to fin.format(HHMM), "activo" to activo,
    )

    val duracion: Int get() = Math.floorMod(minutos(fin) - minutos(inicio), DIA)
    val inicioEnSemana: Int get() = (dia - 1) * DIA + minutos(inicio)
  }

  private const val DIA = 24 * 60
  private const val SEMANA = 7 * DIA
  /** *(Propuesto)*: más que suficiente para siete días con varias franjas. */
  const val MAXIMO_FRANJAS = 50
  private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
  private val FORMATO_HORA = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

  private fun minutos(h: LocalTime) = h.hour * 60 + h.minute

  fun leer(cuerpo: Map<String, Any?>): List<Franja> {
    ContratoCuerpo.exigirCampos(cuerpo, setOf("franjas"))
    val lista = cuerpo["franjas"] as? List<*> ?: ContratoCuerpo.invalido("Falta franjas")
    if (lista.size > MAXIMO_FRANJAS) ContratoCuerpo.invalido("Máximo $MAXIMO_FRANJAS franjas")
    val franjas = lista.map { item ->
      @Suppress("UNCHECKED_CAST")
      val f = item as? Map<String, Any?> ?: ContratoCuerpo.invalido("Cada franja debe ser un objeto")
      ContratoCuerpo.exigirCampos(f, setOf("dia_semana", "hora_inicio", "hora_fin", "activo"))
      Franja(
        dia = ContratoCuerpo.entero(f, "dia_semana", 1, 7, obligatorio = true)!!,
        inicio = hora(f, "hora_inicio"),
        fin = hora(f, "hora_fin"),
        activo = ContratoCuerpo.booleano(f, "activo"),
      )
    }
    validar(franjas)
    return franjas
  }

  fun validar(franjas: List<Franja>) {
    if (franjas.any { it.duracion == 0 }) ContratoCuerpo.invalido("Una franja empieza y termina a la misma hora")
    val activas = franjas.filter { it.activo }
    for (i in activas.indices) for (j in i + 1 until activas.size) {
      val a = activas[i]
      val b = activas[j]
      val desdeA = Math.floorMod(b.inicioEnSemana - a.inicioEnSemana, SEMANA)
      val desdeB = Math.floorMod(a.inicioEnSemana - b.inicioEnSemana, SEMANA)
      if (desdeA < a.duracion || desdeB < b.duracion) {
        throw ContratoApiException(HttpStatus.CONFLICT, "conflicto", "Hay franjas que se pisan")
      }
    }
  }

  private fun hora(f: Map<String, Any?>, campo: String): LocalTime {
    val texto = f[campo] as? String ?: ContratoCuerpo.invalido("Falta $campo")
    if (!FORMATO_HORA.matches(texto)) ContratoCuerpo.invalido("$campo debe ser HH:mm")
    return LocalTime.parse(texto, HHMM)
  }
}
