package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-048: fechas inválidas y filtros analíticos incorrectos. */
class AnaliticaFiltrosTest {
    private val servicio = ContratoAnaliticaService(mock(JdbcTemplate::class.java), mock(CambioPanelPublisher::class.java))
    @Test fun `cubo en vivo rechaza eje desconocido sin consulta`() {
        val vivo = ContratoCuboVivoService(mock(JdbcTemplate::class.java))
        assertEquals("datos_invalidos", assertFailsWith<ContratoApiException> { vivo.cubo(3, "desconocido") }.codigo)
    }
    @Test fun `participacion diez por ciento calcula concentracion sin divisiones por cero`() {
        assertEquals(0, ContratoAnaliticaService.pctDelDiez(emptyList()))
        assertEquals(0, ContratoAnaliticaService.pctDelDiez(listOf(0, 0)))
        assertEquals(80, ContratoAnaliticaService.pctDelDiez(listOf(8, 1, 1)))
        assertEquals(100, ContratoAnaliticaService.pctDelDiez(listOf(5)))
    }
}
