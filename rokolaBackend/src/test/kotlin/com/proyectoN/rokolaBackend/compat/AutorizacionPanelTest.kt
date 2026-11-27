package com.proyectoN.rokolaBackend.compat

import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.*

/** CP-015/016/039: sesiones inválidas y no exposición de credenciales. */
class AutorizacionPanelTest {
    private val sesiones = ContratoPanelSesiones(cookieSegura = true)
    private val acceso = ContratoPanelAcceso(mock(JdbcTemplate::class.java), sesiones)
    @Test fun `sin cookie o con cookie desconocida exige autenticacion`() {
        for (token in listOf(null, "", "token-inventado")) {
            val e = assertFailsWith<ContratoApiException> { acceso.exigir(token) }
            assertEquals(HttpStatus.UNAUTHORIZED, e.status)
            assertEquals("no_autenticado", e.codigo)
        }
    }
    @Test fun `cerrar sesion invalida acceso posterior`() {
        val token = sesiones.crear(8)
        assertNotNull(sesiones.vigente(token))
        sesiones.cerrar(token)
        assertNull(sesiones.vigente(token))
        assertFailsWith<ContratoApiException> { acceso.exigir(token) }
    }
    @Test fun `credenciales malformadas se rechazan sin consultar usuarios`() {
        for ((correo, clave) in listOf("x" to "pass", "x@y.com" to "", "x@y.com" to "a".repeat(201), null to "clave")) {
            assertEquals(HttpStatus.BAD_REQUEST, assertFailsWith<ContratoApiException> { acceso.entrar(correo, clave) }.status)
        }
    }
    @Test fun `yo no expone contrasena ni hash ni token`() {
        val personal = ContratoPanelAcceso.Personal(5, "Op", "op@example.test", "operador", true, false, 10, "test", "Test", "America/Bogota")
        val info = acceso.yo(personal).toString().lowercase()
        assertTrue("operador" in info)
        assertFalse("password_hash" in info || "token" in info || "clave_temporal" in info)
    }
}
