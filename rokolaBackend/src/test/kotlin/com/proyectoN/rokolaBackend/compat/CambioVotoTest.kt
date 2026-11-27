package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.cancion.ClasificadorGeneroService
import com.proyectoN.rokolaBackend.cola.ColaService
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import jakarta.persistence.EntityManager
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.json.JsonMapper
import kotlin.test.*

/** CP-028/038: entradas inválidas de mover voto sin alterar la BD. */
class CambioVotoTest {
    private val service = ContratoParticipacionService(mock(JdbcTemplate::class.java), mock(EntityManager::class.java),
        mock(ContratoDispositivoService::class.java), mock(HorarioActivoService::class.java),
        mock(YoutubeApiCliente::class.java), mock(ColaService::class.java),
        mock(CambioColaPublisher::class.java), mock(JsonMapper::class.java), mock(ClasificadorGeneroService::class.java))
    @Test fun `no permite mover voto hacia la misma solicitud`() {
        val e = assertFailsWith<ContratoApiException> { service.cambiarVoto("bar", null, null, "2", "2") }
        assertEquals("datos_invalidos", e.codigo)
    }
    @Test fun `no permite ids inexistentes o negativos`() {
        for (id in listOf("abc", "0", "-2")) {
            assertFailsWith<ContratoApiException> { service.cambiarVoto("bar", null, null, id, "3") }
        }
    }
}
