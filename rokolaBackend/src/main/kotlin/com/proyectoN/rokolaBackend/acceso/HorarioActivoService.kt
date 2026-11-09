package com.proyectoN.rokolaBackend.acceso

import com.proyectoN.rokolaBackend.establecimiento.HorarioOperacion
import com.proyectoN.rokolaBackend.establecimiento.HorarioOperacionRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId

/** Comprueba franjas normales, franjas que cruzan medianoche y zona horaria IANA. */
@Service
class HorarioActivoService(
    private val horarios: HorarioOperacionRepository,
) {
    fun estaEnHorario(idEstablecimiento: Long, zonaHoraria: String, clock: Clock = Clock.systemUTC()): Boolean {
        val ahora = clock.instant().atZone(ZoneId.of(zonaHoraria))
        val diaActual = ahora.dayOfWeek.value
        val diaAnterior = if (diaActual == 1) 7 else diaActual - 1
        val hora = ahora.toLocalTime()

        return horarios.findByEstablecimientoIdOrderByDiaSemanaAscHoraInicioAsc(idEstablecimiento)
            .asSequence()
            .filter(HorarioOperacion::activo)
            .any { horario ->
                val dia = horario.diaSemana.toInt()
                val inicio = horario.horaInicio
                val fin = horario.horaFin
                when {
                    inicio == fin -> false // No representar horarios de 24h como horas iguales
                    fin.isAfter(inicio) -> dia == diaActual && entre(hora, inicio, fin)
                    else -> (dia == diaActual && !hora.isBefore(inicio)) ||
                        (dia == diaAnterior && hora.isBefore(fin))
                }
            }
    }

    private fun entre(valor: LocalTime, inicio: LocalTime, fin: LocalTime) =
        !valor.isBefore(inicio) && valor.isBefore(fin)
}
