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

/** CP-028/038: validar peticiones antes de alterar votos. */
class VotoEstadoTest {
    private val servicio = ContratoParticipacionService(mock(JdbcTemplate::class.java), mock(EntityManager::class.java),
        mock(ContratoDispositivoService::class.java), mock(HorarioActivoService::class.java),
        mock(YoutubeApiCliente::class.java), mock(ColaService::class.java),
        mock(CambioColaPublisher::class.java), mock(JsonMapper::class.java), mock(ClasificadorGeneroService::class.java))
    @Test fun `video id mal formado no llega a consultar datos ni youtube`() {
        for (id in listOf("", "corto", "demasiado-largo-para-youtube", "id con espacio")) {
            assertEquals("datos_invalidos", assertFailsWith<ContratoApiException> {
                servicio.pedir("bar", null, null, id)
            }.codigo)
        }
    }
}
