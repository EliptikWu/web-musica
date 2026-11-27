package com.proyectoN.rokolaBackend.integracion

import com.proyectoN.rokolaBackend.compat.ContratoCuboVivoService
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import kotlin.test.*

/** CP-025/032/033, RF-049 a RF-053: cálculo OLAP real sobre un bar ficticio.
 * La suite se habilita únicamente con ROKOLA_TEST_DB_URL y la protección PostgresLocalFixture.
 */
@EnabledIfEnvironmentVariable(named = "ROKOLA_TEST_DB_URL", matches = ".+")
class CuboVivoPostgresIntegrationTest {
    private fun celdas(respuesta: Map<String, Any?>): List<Map<String, Any?>> {
        @Suppress("UNCHECKED_CAST")
        return respuesta["celdas"] as List<Map<String, Any?>>
    }

    @Test fun `una solicitud y dos votos producen subtotales correctos y telefonos unicos`() {
        PostgresLocalFixture.conDatos { d ->
            // Dos votos activos de teléfonos diferentes, nunca se cuentan como tres teléfonos
            // por incluir la solicitud inicial en el cubo.
            PostgresLocalFixture.ejecutar(d.conexion,
                "INSERT INTO public.voto(id_solicitud,id_dispositivo,activo) VALUES (?,?,TRUE)",
                d.solicitud, d.dispositivo1)
            PostgresLocalFixture.ejecutar(d.conexion,
                "INSERT INTO public.voto(id_solicitud,id_dispositivo,activo) VALUES (?,?,TRUE)",
                d.solicitud, d.dispositivo2)
            val jdbc = JdbcTemplate(SingleConnectionDataSource(d.conexion, true))
            val resultado = ContratoCuboVivoService(jdbc).cubo(d.bar, "dia")
            val total = celdas(resultado).single { it["genero"] == "*" && it["franja"] == "*" && it["tercero"] == "*" }
            assertEquals(1, total["solicitudes"])
            assertEquals(2, total["votos"])
            assertEquals(2, total["telefonos"])
            assertEquals(0, total["no_sonaron"])
            assertEquals(1, (resultado["datos"] as Map<*, *>)["sesiones_abiertas"])
            assertTrue((resultado["miembros"] as Map<*, *>)["genero"].toString().contains("Sin clasificar"))
        }
    }

    @Test fun `cambio de genero aparece al instante y no conserva el anterior`() {
        PostgresLocalFixture.conDatos { d ->
            val jdbc = JdbcTemplate(SingleConnectionDataSource(d.conexion, true))
            val servicio = ContratoCuboVivoService(jdbc)
            val antes = celdas(servicio.cubo(d.bar, "dia"))
            assertTrue(antes.any { it["genero"] == "Sin clasificar" })
            PostgresLocalFixture.ejecutar(d.conexion,
                "UPDATE public.cancion SET categoria = 'Salsa', origen_categoria = 'MANUAL' WHERE id_cancion = ?", d.cancion)
            val despues = celdas(servicio.cubo(d.bar, "dia"))
            assertTrue(despues.any { it["genero"] == "Salsa" && it["solicitudes"] == 1 })
            assertFalse(despues.any { it["genero"] == "Sin clasificar" })
        }
    }

    @Test fun `el eje evento se agrega sin duplicar las solicitudes`() {
        PostgresLocalFixture.conDatos { d ->
            val servicio = ContratoCuboVivoService(JdbcTemplate(SingleConnectionDataSource(d.conexion, true)))
            val r = servicio.cubo(d.bar, "evento")
            assertEquals("evento", r["eje"])
            val total = celdas(r).single { it["genero"] == "*" && it["franja"] == "*" && it["tercero"] == "*" }
            assertEquals(1, total["solicitudes"])
            assertTrue(celdas(r).any { it["tercero"] == "Normal" })
        }
    }
}
