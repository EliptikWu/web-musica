package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.HorarioActivoService
import com.proyectoN.rokolaBackend.cancion.ClasificadorGeneroService
import com.proyectoN.rokolaBackend.cola.ColaService
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import jakarta.persistence.EntityManager
import org.mockito.Mockito.*
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.json.JsonMapper
import kotlin.test.*

/** CP-035/037: validación de llave y entradas, sin consumir una cuota real de YouTube. */
class IdempotenciaSolicitudTest {
    private val servicio = ContratoParticipacionService(mock(JdbcTemplate::class.java), mock(EntityManager::class.java),
        mock(ContratoDispositivoService::class.java), mock(HorarioActivoService::class.java),
        mock(YoutubeApiCliente::class.java), mock(ColaService::class.java),
        mock(CambioColaPublisher::class.java), mock(JsonMapper::class.java), mock(ClasificadorGeneroService::class.java))
    @Test fun `rechaza llave ausente tras video id valido`() {
        // Se rechaza antes del acceso al dispositivo o a la base, evitando operaciones parciales.
        val e = assertFailsWith<ContratoApiException> { servicio.pedir("bar", "d", null, "dQw4w9WgXcQ") }
        assertEquals("datos_invalidos", e.codigo)
    }
    @Test fun `rechaza llave no uuid y evita alterar el estado`() {
        val e = assertFailsWith<ContratoApiException> { servicio.pedir("bar", "d", "llave-duplicada", "dQw4w9WgXcQ") }
        assertEquals("datos_invalidos", e.codigo)
    }
}
