package com.proyectoN.rokolaBackend.integracion

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.sql.SQLException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** CP-010/037: usa índices REALES de PostgreSQL, no Mockito; requiere BD LOCAL _test. */
@EnabledIfEnvironmentVariable(named = "ROKOLA_TEST_DB_URL", matches = ".+")
class VotoConcurrenciaIntegrationTest {
    @Test fun `dos votos concurrentes del mismo dispositivo no crean duplicados`() {
        PostgresLocalFixture.conDatos { datos ->
            val pool = Executors.newFixedThreadPool(2)
            val inicio = CountDownLatch(1)
            try {
                val resultados = (1..2).map {
                    pool.submit<Boolean> {
                        PostgresLocalFixture.conectar().use { con ->
                            inicio.await(5, TimeUnit.SECONDS)
                            try {
                                PostgresLocalFixture.ejecutar(con,
                                    "INSERT INTO public.voto(id_solicitud,id_dispositivo,activo) VALUES (?,?,TRUE)",
                                    datos.solicitud, datos.dispositivo1)
                                true
                            } catch (_: SQLException) { false }
                        }
                    }
                }
                inicio.countDown()
                assertEquals(1, resultados.count { it.get(15, TimeUnit.SECONDS) })
                datos.conexion.prepareStatement("SELECT COUNT(*) FROM public.voto WHERE id_solicitud=? AND activo=TRUE").use { p ->
                    p.setLong(1, datos.solicitud)
                    p.executeQuery().use { rs -> rs.next(); assertEquals(1, rs.getInt(1)) }
                }
            } finally { pool.shutdownNow() }
        }
    }
    @Test fun `dos dispositivos diferentes pueden votar una misma cancion`() {
        PostgresLocalFixture.conDatos { datos ->
            for (d in listOf(datos.dispositivo1, datos.dispositivo2)) {
                PostgresLocalFixture.ejecutar(datos.conexion,
                    "INSERT INTO public.voto(id_solicitud,id_dispositivo,activo) VALUES (?,?,TRUE)", datos.solicitud, d)
            }
            datos.conexion.prepareStatement("SELECT COUNT(*) FROM public.voto WHERE id_solicitud=? AND activo=TRUE").use { p ->
                p.setLong(1, datos.solicitud)
                p.executeQuery().use { rs -> rs.next(); assertEquals(2, rs.getInt(1)) }
            }
        }
    }
}
