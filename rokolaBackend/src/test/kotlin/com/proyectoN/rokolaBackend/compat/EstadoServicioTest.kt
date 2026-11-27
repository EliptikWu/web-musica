package com.proyectoN.rokolaBackend.compat

import java.time.LocalTime
import kotlin.test.*

/** CP-004/020/021/043: reglas de horarios que cruzan medianoche y domingo-lunes. */
class EstadoServicioTest {
    private fun f(dia: Int, inicio: String, fin: String, activo: Boolean = true) =
        ReglasHorario.Franja(dia, LocalTime.parse(inicio), LocalTime.parse(fin), activo)
    @Test fun `un horario nocturno cubre el siguiente dia`() {
        val horario = f(5, "20:00", "03:00")
        assertEquals(7 * 60, horario.duracion)
        ReglasHorario.validar(listOf(horario))
        assertFailsWith<ContratoApiException> {
            ReglasHorario.validar(listOf(horario, f(6, "02:00", "04:00")))
        }
    }
    @Test fun `solapamiento entre domingo y lunes es conflicto`() {
        assertFailsWith<ContratoApiException> {
            ReglasHorario.validar(listOf(f(7, "22:00", "03:00"), f(1, "02:00", "06:00")))
        }
    }
    @Test fun `franjas inactivas no generan conflictos con activas`() {
        ReglasHorario.validar(listOf(f(7, "22:00", "03:00"), f(1, "02:00", "06:00", activo = false)))
    }
}
