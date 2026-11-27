package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.HorarioEfectivoService
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionGestionService
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.mockito.Mockito.*
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-022: los bloqueos malformados se rechazan antes de guardar contenido. */
class ContratoBloqueosValidacionTest {
    private val jdbc = mock(JdbcTemplate::class.java)
    private val servicio = ContratoBloqueosService(jdbc, mock(ContratoCanciones::class.java),
        mock(HorarioEfectivoService::class.java), mock(ReproduccionGestionService::class.java),
        mock(ContratoBitacora::class.java), mock(CambioColaPublisher::class.java),
        mock(CambioPanelPublisher::class.java))
    private val admin = ContratoPanelAcceso.Personal(1, "Admin", "admin@example.test", "administrador",
        true, false, 10, "bar", "Bar", "America/Bogota")

    @Test fun `no admite duraciones o cantidades de horas invalidas`() {
        val casos: List<Map<String, Any?>> = listOf(
            mapOf("video_id" to "abc12345678", "motivo" to "spam", "duracion" to "horas"),
            mapOf("video_id" to "abc12345678", "motivo" to "spam", "duracion" to "horas", "horas" to 0),
            mapOf("video_id" to "abc12345678", "motivo" to "spam", "duracion" to "horas", "horas" to 169),
            mapOf("video_id" to "abc12345678", "motivo" to "spam", "duracion" to "desconocida"),
            mapOf("video_id" to "abc12345678", "motivo" to " ", "duracion" to "indefinido"),
            mapOf("video_id" to "abc12345678", "motivo" to "spam", "duracion" to "indefinido", "bar_id" to 2),
        )
        for (cuerpo in casos) {
            assertEquals("datos_invalidos", assertFailsWith<ContratoApiException> {
                servicio.bloquear(admin, cuerpo)
            }.codigo)
        }
        verifyNoInteractions(jdbc)
    }

    @Test fun `filtro de vigencia desconocido no hace consultas`() {
        assertFailsWith<ContratoApiException> { servicio.listar(admin, "vigente-ayer") }
        verifyNoInteractions(jdbc)
    }
}
