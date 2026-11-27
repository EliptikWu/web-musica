package com.proyectoN.rokolaBackend.integracion

import com.proyectoN.rokolaBackend.cancion.ClasificadorGeneroService
import com.proyectoN.rokolaBackend.compat.*
import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import com.proyectoN.rokolaBackend.youtube.YoutubeApiCliente
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import kotlin.test.*

/** CP-040/032: ninguna cuenta de otro establecimiento puede reclasificar un video ajeno. */
@EnabledIfEnvironmentVariable(named = "ROKOLA_TEST_DB_URL", matches = ".+")
class GenerosAislamientoPostgresIntegrationTest {
    private fun admin(idBar: Long) = ContratoPanelAcceso.Personal(7, "Admin QA", "qa@example.test", "administrador",
        true, false, idBar, "bar-$idBar", "Bar QA", "America/Bogota")

    @Test fun `un administrador ajeno no cambia categoria global ni emite eventos`() {
        PostgresLocalFixture.conDatos { d ->
            val bitacora = mock(ContratoBitacora::class.java)
            val panel = mock(CambioPanelPublisher::class.java)
            val jdbc = JdbcTemplate(SingleConnectionDataSource(d.conexion, true))
            val servicio = ContratoGenerosService(jdbc, ClasificadorGeneroService(),
                mock(YoutubeApiCliente::class.java), bitacora, panel)
            // No existe una solicitud/lista/reproducción de esta canción en otro bar.
            val e = assertFailsWith<ContratoApiException> {
                servicio.corregir(admin(d.bar + 1_000_000), d.cancion, "Salsa")
            }
            assertEquals(HttpStatus.NOT_FOUND, e.status)
            d.conexion.prepareStatement("SELECT categoria FROM public.cancion WHERE id_cancion=?").use { p ->
                p.setLong(1, d.cancion)
                p.executeQuery().use { rs -> assertTrue(rs.next()); assertNull(rs.getString(1)) }
            }
            verifyNoInteractions(bitacora, panel)
        }
    }
}
