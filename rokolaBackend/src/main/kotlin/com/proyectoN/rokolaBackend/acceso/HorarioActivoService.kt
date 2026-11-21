package com.proyectoN.rokolaBackend.acceso

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
) {
    fun estaEnHorario(idEstablecimiento: Long, zonaHoraria: String, clock: Clock = Clock.systemUTC()): Boolean =
        efectivo.estaAbierto(idEstablecimiento, ZoneId.of(zonaHoraria), clock)
}
