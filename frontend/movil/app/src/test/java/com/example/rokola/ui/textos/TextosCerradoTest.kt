package com.example.rokola.ui.textos

import com.example.rokola.dominio.EstadoServicio
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** D24: fuera de horario no suena nada, ni la lista de la casa; el teléfono no lo promete. */
class TextosCerradoTest {
    private val zona = ZoneId.of("America/Bogota")
    private val ahora = Instant.parse("2026-11-26T23:30:00Z") // jueves 6:30 p. m. en Bogotá

    @Test
    fun `fuera de horario dice cuando abren y no promete musica`() {
        val t = servicioCerrado(EstadoServicio.FUERA_DE_HORARIO, Instant.parse("2026-11-27T01:00:00Z"), zona, ahora)
        assertFalse(t.detalle.contains("lista de la casa"))
        assertFalse(servicioCerrado(EstadoServicio.FUERA_DE_HORARIO, null, zona, ahora).detalle.contains("lista de la casa"))
        assertEquals("Los pedidos se abren en el horario del bar.", servicioCerrado(EstadoServicio.FUERA_DE_HORARIO, null, zona, ahora).detalle)
    }
}
