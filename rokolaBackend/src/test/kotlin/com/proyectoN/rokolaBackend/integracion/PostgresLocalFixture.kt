package com.proyectoN.rokolaBackend.integracion

import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

/**
 * Acceso EXCLUSIVAMENTE a una BD local de pruebas. No lee DB_URL de Railway.
 * Exige ROKOLA_TEST_DB_URL jdbc:postgresql://localhost/rokola_test (o 127.0.0.1).
 * La base debe haber recibido el esquema y las migraciones Flyway reales.
 */
internal object PostgresLocalFixture {
    private val url: String get() = System.getenv("ROKOLA_TEST_DB_URL") ?: error("Falta ROKOLA_TEST_DB_URL")
    fun conectar(): Connection {
        val url = url
        require(Regex("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/[^?]+(_test)(\\?.*)?$", RegexOption.IGNORE_CASE).matches(url)) {
            "Por seguridad solo se admite PostgreSQL local y un nombre de base terminado en _test"
        }
        return DriverManager.getConnection(url, System.getenv("ROKOLA_TEST_DB_USER") ?: "postgres",
            System.getenv("ROKOLA_TEST_DB_PASSWORD") ?: "")
    }

    fun insertar(con: Connection, sql: String, vararg params: Any?): Long =
        con.prepareStatement(sql).use { p ->
            params.forEachIndexed { i, valor -> p.setObject(i + 1, valor) }
            p.executeQuery().use { rs -> check(rs.next()); rs.getLong(1) }
        }

    fun ejecutar(con: Connection, sql: String, vararg params: Any?): Int =
        con.prepareStatement(sql).use { p ->
            params.forEachIndexed { i, valor -> p.setObject(i + 1, valor) }
            p.executeUpdate()
        }

    /** Crea datos ficticios en la BD exclusiva de pruebas y los elimina explícitamente. */
    fun <T> conDatos(accion: (Datos) -> T): T {
        conectar().use { con ->
            val semilla = UUID.randomUUID().toString().replace("-", "").take(16)
            val bar = insertar(con,
                "INSERT INTO public.establecimiento(nombre,slug,zona_horaria,activo) VALUES ('Bar Test',?,'America/Bogota',TRUE) RETURNING id_establecimiento",
                "qa-$semilla")
            var sesion: Long? = null
            var d1: Long? = null
            var d2: Long? = null
            var cancion: Long? = null
            var solicitud: Long? = null
            try {
                sesion = insertar(con,
                    "INSERT INTO public.sesion(id_establecimiento,estado,indice_lista_defecto) VALUES (?,'abierta',1) RETURNING id_sesion", bar)
                d1 = insertar(con,
                    "INSERT INTO public.dispositivo(id_establecimiento,device_hash,tipo) VALUES (?,?,'movil') RETURNING id_dispositivo",
                    bar, "a$semilla")
                d2 = insertar(con,
                    "INSERT INTO public.dispositivo(id_establecimiento,device_hash,tipo) VALUES (?,?,'movil') RETURNING id_dispositivo",
                    bar, "b$semilla")
                cancion = insertar(con,
                    "INSERT INTO public.cancion(youtube_video_id,titulo,duracion_seg,embebible,disponibilidad) VALUES (?,'Cancion QA',180,TRUE,'disponible') RETURNING id_cancion",
                    ("QA" + semilla).take(11))
                solicitud = insertar(con,
                    "INSERT INTO public.solicitud(id_sesion,id_cancion,id_dispositivo_solicitante,estado) VALUES (?,?,?,'pendiente') RETURNING id_solicitud",
                    sesion, cancion, d1)
                return accion(Datos(con, bar, requireNotNull(sesion), requireNotNull(d1),
                    requireNotNull(d2), requireNotNull(cancion), requireNotNull(solicitud)))
            } finally {
                solicitud?.let { ejecutar(con, "DELETE FROM public.voto WHERE id_solicitud=?", it) }
                solicitud?.let { ejecutar(con, "DELETE FROM public.solicitud WHERE id_solicitud=?", it) }
                ejecutar(con, "DELETE FROM public.api_idempotencia WHERE id_establecimiento=?", bar)
                sesion?.let { ejecutar(con, "DELETE FROM public.sesion WHERE id_sesion=?", it) }
                d1?.let { ejecutar(con, "DELETE FROM public.dispositivo WHERE id_dispositivo=?", it) }
                d2?.let { ejecutar(con, "DELETE FROM public.dispositivo WHERE id_dispositivo=?", it) }
                cancion?.let { ejecutar(con, "DELETE FROM public.cancion WHERE id_cancion=?", it) }
                ejecutar(con, "DELETE FROM public.establecimiento WHERE id_establecimiento=?", bar)
            }
        }
    }

    data class Datos(
        val conexion: Connection, val bar: Long, val sesion: Long,
        val dispositivo1: Long, val dispositivo2: Long, val cancion: Long, val solicitud: Long,
    )
}
