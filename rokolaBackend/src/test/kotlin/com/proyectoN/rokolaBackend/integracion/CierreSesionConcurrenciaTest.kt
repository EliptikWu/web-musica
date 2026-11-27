package com.proyectoN.rokolaBackend.integracion

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.sql.SQLException
import kotlin.test.*

/** CP-049: comprueba bloqueo de sesión que serializa cierres y solicitudes. */
@EnabledIfEnvironmentVariable(named = "ROKOLA_TEST_DB_URL", matches = ".+")
class CierreSesionConcurrenciaTest {
    @Test fun `dos operaciones no adquieren a la vez el lock de una sesion`() {
        PostgresLocalFixture.conDatos { d ->
            d.conexion.autoCommit = false
            try {
                d.conexion.prepareStatement("SELECT id_sesion FROM public.sesion WHERE id_sesion=? FOR UPDATE").use { p ->
                    p.setLong(1, d.sesion)
                    p.executeQuery().use { rs -> assertTrue(rs.next()) }
                }
                PostgresLocalFixture.conectar().use { otra ->
                    otra.createStatement().use { s -> s.execute("SET lock_timeout = '300ms'") }
                    val error = assertFailsWith<SQLException> {
                        otra.prepareStatement("SELECT id_sesion FROM public.sesion WHERE id_sesion=? FOR UPDATE").use { p ->
                            p.setLong(1, d.sesion)
                            p.executeQuery().close()
                        }
                    }
                    assertEquals("55P03", error.sqlState)
                }
            } finally {
                d.conexion.rollback()
                d.conexion.autoCommit = true
            }
        }
    }
}
