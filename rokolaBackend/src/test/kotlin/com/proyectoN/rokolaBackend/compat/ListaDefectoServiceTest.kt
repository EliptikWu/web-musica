package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import com.proyectoN.rokolaBackend.youtube.YoutubeService
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-018/031/042: nombres y payload de listas, sin escribir datos reales. */
class ListaDefectoServiceTest {
    private val servicio = ContratoListasService(mock(JdbcTemplate::class.java), mock(ContratoCanciones::class.java),
        mock(YoutubeService::class.java), mock(ContratoBitacora::class.java),
        mock(CambioColaPublisher::class.java), mock(CambioPanelPublisher::class.java))
    private val admin = ContratoPanelAcceso.Personal(1, "A", "a@b.co", "administrador", true, false, 2, "bar", "Bar", "America/Bogota")
    @Test fun `nombre de lista rechaza valores vacios mal tipados y campos extra`() {
        assertFailsWith<ContratoApiException> { servicio.crear(admin, null) }
        assertFailsWith<ContratoApiException> { servicio.crear(admin, mapOf("nombre" to " ")) }
        assertFailsWith<ContratoApiException> { servicio.crear(admin, mapOf("nombre" to 3)) }
        assertFailsWith<ContratoApiException> { servicio.crear(admin, mapOf("nombre" to "Rock", "bar_id" to 2)) }
    }
    @Test fun `busqueda de lista no acepta consultas vacias o excesivas`() {
        for (consulta in listOf(null, "", " ", "a", "x".repeat(81))) {
            val error = assertFailsWith<ContratoApiException> { servicio.buscar(consulta) }
            assertEquals("datos_invalidos", error.codigo)
        }
    }
}
