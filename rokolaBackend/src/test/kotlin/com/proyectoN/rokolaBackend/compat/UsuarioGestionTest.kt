package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate
import java.security.SecureRandom
import kotlin.test.*

/** CP-017/030/041: validaciones de usuarios previas a escritura. */
class UsuarioGestionTest {
    private val servicio = ContratoUsuariosService(
        mock(JdbcTemplate::class.java), mock(ContratoPanelSesiones::class.java),
        mock(ContratoBitacora::class.java), mock(CambioPanelPublisher::class.java),
    )
    private val admin = ContratoPanelAcceso.Personal(
        1, "Admin", "admin@example.test", "administrador", true, false, 10, "bar-test", "Bar Test", "America/Bogota",
    )
    @Test fun `crear valida campos obligatorios antes de insertar`() {
        val cuerpos: List<Map<String, Any?>?> = listOf(null, emptyMap(), mapOf("nombre" to "", "correo" to "x@test.co", "rol" to "operador"),
            mapOf("nombre" to "A", "correo" to "correo-invalido", "rol" to "operador"),
            mapOf("nombre" to "A", "correo" to "a@b.co", "rol" to "superadmin"),
            mapOf("nombre" to "A", "correo" to "a@b.co", "rol" to "operador", "bar_id" to 20))
        for (cuerpo in cuerpos) {
            assertEquals("datos_invalidos", assertFailsWith<ContratoApiException> { servicio.crear(admin, cuerpo) }.codigo)
        }
    }
    @Test fun `filtros inválidos de listado se rechazan antes del SQL`() {
        assertFailsWith<ContratoApiException> { servicio.listar(admin, null, "superadmin", null) }
        assertFailsWith<ContratoApiException> { servicio.listar(admin, null, null, "desconocido") }
    }
    @Test fun `las claves temporales tienen longitud y alfabeto controlados`() {
        val claves = (1..30).map { ContratoUsuariosService.generarClaveTemporal(SecureRandom()) }
        assertEquals(30, claves.toSet().size)
        assertTrue(claves.all { Regex("^[a-z2-9]{4}-[a-z2-9]{4}-[a-z2-9]{4}$").matches(it) })
        assertTrue(claves.none { it.any { c -> c in "01ilO" } })
    }
}
