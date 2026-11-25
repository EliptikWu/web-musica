package com.proyectoN.rokolaBackend

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.boot.test.context.SpringBootTest

/**
 * Arranca la aplicación entera: Flyway corre las migraciones contra una base de verdad. Solo con
 * `DB_URL` (y su usuario y clave) definidos; sin base se salta en vez de fallar, para que
 * `./gradlew test` sirva en cualquier máquina. Las demás pruebas no necesitan base.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+", disabledReason = "Sin DB_URL no hay base contra la que arrancar")
class RokolaBackendApplicationTests {

	@Test
	fun contextLoads() {
	}

}
