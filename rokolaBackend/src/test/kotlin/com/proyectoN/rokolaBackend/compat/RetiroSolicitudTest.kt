package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-022: rechaza retiradas sin idempotencia o con motivo inválido antes de mutar SQL. */
class RetiroSolicitudTest {
    private val servicio = ContratoRetiroService(mock(JdbcTemplate::class.java), mock(ContratoBitacora::class.java),
        mock(CambioColaPublisher::class.java), mock(CambioPanelPublisher::class.java))
    private val personal = ContratoPanelAcceso.Personal(7, "Admin", "a@b.co", "administrador", true, false, 1, "bar", "Bar", "America/Bogota")
    @Test fun `rechaza identificador invalido y cuerpo sin motivo`() {
        assertFailsWith<ContratoApiException> { servicio.retirar(personal, "no-id", mapOf("motivo" to "otro"), null) }
        assertFailsWith<ContratoApiException> { servicio.retirar(personal, "1", mapOf("motivo" to "cancelada"), null) }
    }
    @Test fun `requiere llave uuid de idempotencia`() {
        for (key in listOf(null, "abc", "000")) {
            assertEquals("datos_invalidos", assertFailsWith<ContratoApiException> {
                servicio.retirar(personal, "1", mapOf("motivo" to "duplicada"), key)
            }.codigo)
        }
    }
}
