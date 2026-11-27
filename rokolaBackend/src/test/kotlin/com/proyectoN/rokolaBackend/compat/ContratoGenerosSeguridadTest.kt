package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.cancion.ClasificadorGeneroService
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-025/032/040: corrección manual con catálogo válido y alcance del bar. */
class ContratoGenerosSeguridadTest {
    private val jdbc = mock(JdbcTemplate::class.java)
    private val auditoria = mock(ContratoBitacora::class.java)
    private val panel = mock(CambioPanelPublisher::class.java)
    private val servicio = ContratoGenerosService(jdbc, ClasificadorGeneroService(),
        mock(YoutubeApiCliente::class.java), auditoria, panel)
    private val admin = ContratoPanelAcceso.Personal(1, "Ana", "ana@example.test", "administrador",
        true, false, 55, "bar-55", "Bar 55", "America/Bogota")

    @Test fun `categoria desconocida no modifica BD ni notifica a nadie`() {
        for (categoria in listOf(null, "", "inexistente-deliberadamente")) {
            val e = assertFailsWith<ContratoApiException> { servicio.corregir(admin, 77L, categoria) }
            assertEquals(HttpStatus.BAD_REQUEST, e.status)
            assertEquals("datos_invalidos", e.codigo)
        }
        verifyNoInteractions(jdbc, auditoria, panel)
    }

    @Test fun `una cancion que no pertenezca al bar no se corrige ni audita`() {
        // JdbcTemplate mock devuelve lista vacía: simulamos que el UPDATE ... RETURNING no obtuvo filas.
        val e = assertFailsWith<ContratoApiException> { servicio.corregir(admin, 77L, "Salsa") }
        assertEquals(HttpStatus.NOT_FOUND, e.status)
        assertEquals("no_encontrado", e.codigo)
        verifyNoInteractions(auditoria, panel)
    }

    @Test fun `reclasificacion prohibe cursores negativos antes de contactar youtube`() {
        val e = assertFailsWith<ContratoApiException> { servicio.reclasificar(admin, 5, -1) }
        assertEquals(HttpStatus.BAD_REQUEST, e.status)
        verifyNoInteractions(jdbc, auditoria, panel)
    }
}
