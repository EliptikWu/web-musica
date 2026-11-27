package com.proyectoN.rokolaBackend.integracion

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.sql.SQLException
import java.util.UUID
import kotlin.test.*

/** CP-035/037: índice único por establecimiento, dispositivo y UUID. */
@EnabledIfEnvironmentVariable(named = "ROKOLA_TEST_DB_URL", matches = ".+")
class IdempotenciaPostgresIntegrationTest {
    @Test fun `misma clave en mismo dispositivo no se puede persistir dos veces`() {
        PostgresLocalFixture.conDatos { d ->
            val uuid = UUID.randomUUID()
            val sql = """INSERT INTO public.api_idempotencia
              (id_establecimiento,device_hash,clave,operacion,huella_peticion,respuesta_http,respuesta_json)
              VALUES (?,?,?,'pedir',?,201,'{}')"""
            PostgresLocalFixture.ejecutar(d.conexion, sql, d.bar, "a".repeat(64), uuid, "b".repeat(64))
            assertFailsWith<SQLException> {
                PostgresLocalFixture.ejecutar(d.conexion, sql, d.bar, "a".repeat(64), uuid, "b".repeat(64))
            }
        }
    }
}
