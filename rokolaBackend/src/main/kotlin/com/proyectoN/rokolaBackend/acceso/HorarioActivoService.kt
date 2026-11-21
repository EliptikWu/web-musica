package com.proyectoN.rokolaBackend.acceso

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.ZoneId

/**
 * ¿Está el bar en horario? Franjas normales, franjas que cruzan medianoche y zona horaria IANA,
 * y desde el 2026-11-21 los eventos del calendario, que cambian el horario de su día
 * (`HorarioEfectivoService`). Lo usan pedir, la reproducción automática y el acceso por QR.
 */
@Service
class HorarioActivoService(
    private val efectivo: HorarioEfectivoService,
    private val jdbc: JdbcTemplate,
) {
    /** Si el bar atiende ahora: por su horario, o porque lo abrieron a mano fuera de él (RF-037). */
    fun atiende(idEstablecimiento: Long, zonaHoraria: String): Boolean =
        estaEnHorario(idEstablecimiento, zonaHoraria) || jdbc.queryForObject(
            "SELECT forzado_abierto FROM public.configuracion_servicio WHERE id_establecimiento = ?",
            Boolean::class.javaObjectType, idEstablecimiento,
        ) == true

    fun estaEnHorario(idEstablecimiento: Long, zonaHoraria: String, clock: Clock = Clock.systemUTC()): Boolean =
        efectivo.estaAbierto(idEstablecimiento, ZoneId.of(zonaHoraria), clock)
}
