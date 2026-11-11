package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.acceso.TokenSeguro
import com.proyectoN.rokolaBackend.reproduccion.FinalizarReproduccionRequest
import com.proyectoN.rokolaBackend.reproduccion.ReproduccionGestionService
import com.proyectoN.rokolaBackend.sesion.Sesion
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

/**
 * Reportes contractuales de pantalla; conserva las operaciones y tablas legacy.
 * Los reportes idempotentes utilizan api_idempotencia y el mismo lock de sesión.
 * RF-058: exige cookie de una pantalla vinculada; no acepta clave global ni acceso público.
 */
@Service
class ContratoPantallaService(
    private val jdbc: JdbcTemplate,
    private val em: EntityManager,
    private val gestion: ReproduccionGestionService,
    private val cambios: CambioColaPublisher,
    private val tokens: TokenSeguro,
    private val env: Environment,
    private val pantallas: ContratoPantallasService,
) {
    private data class ReproduccionFila(
        val sesionId: Long,
        val establecimientoId: Long,
        val resultado: String,
        val duracionSeg: Int,
    )

    @Transactional(readOnly = true)
    fun qrPublico(slug: String): Map<String, String> {
        val bar = idBar(slug)
        // La BD almacena solamente SHA-256 del token del QR, no el URL original.
        // Sin URL conocida no se construye una URL falsa con un token inventado.
        val claveSlug = "ROKOLA_QR_PUBLIC_URL_" + slug.uppercase().replace(Regex("[^A-Z0-9]"), "_")
        val url = (env.getProperty(claveSlug) ?: env.getProperty("ROKOLA_QR_PUBLIC_URL") ?: "").trim()
        val uri = try { URI(url) } catch (_: Exception) { null }
        val token = uri?.path?.substringAfterLast("/q/", "")?.takeIf { it.isNotEmpty() }
        if (uri?.scheme !in listOf("http", "https") || uri?.host.isNullOrBlank()
            || uri?.rawFragment != null || uri?.rawQuery != null
            || token == null || !tokens.esValido(token)
        ) {
            throw fallo(HttpStatus.NOT_FOUND, "no_encontrado",
                "La URL original del QR no está configurada; recupera la URL de generación del QR vigente")
        }
        val valido = jdbc.queryForObject(
            """SELECT EXISTS (
                 SELECT 1 FROM public.codigo_qr q
                 WHERE q.id_establecimiento = ? AND q.activo = TRUE AND q.fecha_revocacion IS NULL
                   AND q.token_hash = ?
               )""".trimIndent(),
            Boolean::class.javaObjectType, bar, tokens.huellaQr(token),
        ) ?: false
        if (!valido) throw fallo(HttpStatus.NOT_FOUND, "no_encontrado",
            "La URL configurada no corresponde al QR vigente de este establecimiento")
        return mapOf("url" to url)
    }

    /** Misma clave + mismos datos -> 204 sin efectos nuevos; errores NO se almacenan. */
    @Transactional
    fun reportar(
        slug: String, idTexto: String, accion: String,
        claveCabecera: String?, token: String?, cuerpo: Map<String, Any?>,
    ) {
        // RF-058: SIEMPRE autorizar antes de leer la caché de Idempotency-Key.
        val pantalla = pantallas.exigirVinculada(slug, token)
        val idReproduccion = idTexto.toLongOrNull()?.takeIf { it > 0L }
            ?: throw fallo(HttpStatus.BAD_REQUEST, "datos_invalidos", "id debe ser un entero positivo")
        val clave = try { UUID.fromString(claveCabecera) } catch (_: Exception) { null }
        if (clave == null || clave.toString() != claveCabecera?.lowercase()) {
            throw fallo(HttpStatus.BAD_REQUEST, "datos_invalidos", "Idempotency-Key debe ser un UUID")
        }
        val posicion = numeroEntero(cuerpo["posicion_seg"])
            ?: throw fallo(HttpStatus.BAD_REQUEST, "datos_invalidos", "posicion_seg debe ser un entero no negativo")
        if (!cuerpo.containsKey("codigo_youtube")) {
            throw fallo(HttpStatus.BAD_REQUEST, "datos_invalidos", "Se requiere codigo_youtube (puede ser null)")
        }
        val codigo = cuerpo["codigo_youtube"]?.let {
            numeroEntero(it) ?: throw fallo(HttpStatus.BAD_REQUEST, "datos_invalidos", "codigo_youtube debe ser un entero o null")
        }
        if (accion !in setOf("inicio", "fin", "error")) {
            throw fallo(HttpStatus.BAD_REQUEST, "datos_invalidos", "Reporte de pantalla no válido")
        }
        val idEstablecimiento = idBar(slug)
        val dispositivoPantalla = sha256("pantalla:$idEstablecimiento:${pantalla.id}:$accion")
        val operacion = "pantalla_$accion"
        val huella = sha256("$operacion|$slug|$idReproduccion|$posicion|${codigo ?: "null"}")
        bloquearClave(idEstablecimiento, dispositivoPantalla, clave)
        val repetido = jdbc.query(
            """SELECT operacion, huella_peticion FROM public.api_idempotencia
               WHERE id_establecimiento = ? AND device_hash = ? AND clave = ?""",
            { rs, _ -> rs.getString("operacion") to rs.getString("huella_peticion").trim() },
            idEstablecimiento, dispositivoPantalla, clave,
        ).firstOrNull()
        if (repetido != null) {
            if (repetido.first != operacion || repetido.second != huella)
                throw fallo(HttpStatus.CONFLICT, "conflicto", "La clave de idempotencia ya se usó con otra petición")
            return // Respuesta HTTP 204, sin repetir cambio alguno.
        }

        val fila = jdbc.query(
            """SELECT r.id_sesion, s.id_establecimiento, r.resultado, c.duracion_seg
               FROM public.reproduccion r
               JOIN public.sesion s ON s.id_sesion = r.id_sesion
               JOIN public.cancion c ON c.id_cancion = r.id_cancion
               WHERE r.id_reproduccion = ? AND s.id_establecimiento = ?""",
            { rs, _ -> ReproduccionFila(
                rs.getLong("id_sesion"), rs.getLong("id_establecimiento"),
                rs.getString("resultado"), rs.getInt("duracion_seg"),
            ) }, idReproduccion, idEstablecimiento,
        ).firstOrNull() ?: throw fallo(HttpStatus.NOT_FOUND, "no_encontrado", "Reproducción no encontrada en este bar")
        val sesion = em.find(Sesion::class.java, fila.sesionId, LockModeType.PESSIMISTIC_WRITE)
            ?: throw fallo(HttpStatus.NOT_FOUND, "no_encontrado", "Sesión no encontrada")
        // Si el ID ya no es el vigente, devolver 204 sin cambios (ADR del frontend).
        // El bloqueo de la sesión serializa esta comprobación con el fin y la siguiente asignación.
        val vigente = jdbc.query(
            """SELECT id_reproduccion FROM public.reproduccion
               WHERE id_sesion = ? AND resultado = 'iniciada'""".trimIndent(),
            { rs, _ -> rs.getLong(1) }, fila.sesionId,
        ).firstOrNull()
        if (sesion.estado.name.lowercase() != "abierta" || vigente != idReproduccion) {
            guardarClave(idEstablecimiento, dispositivoPantalla, clave, operacion, huella)
            return
        }

        when (accion) {
            "inicio" -> {
                // Alinea el reloj de inicio con el avance informado por el player.
                // Una nueva clave con otra posicion es un reporte nuevo, no un replay.
                jdbc.update(
                    """UPDATE public.reproduccion
                       SET fecha_inicio = CURRENT_TIMESTAMP - (? * INTERVAL '1 second'),
                           segundos_reproducidos = GREATEST(segundos_reproducidos, ?)
                       WHERE id_reproduccion = ? AND id_sesion = ? AND resultado = 'iniciada'""",
                    posicion.coerceAtMost(fila.duracionSeg), posicion.coerceAtMost(fila.duracionSeg),
                    idReproduccion, fila.sesionId,
                ).takeIf { it == 1 } ?: throw fallo(HttpStatus.CONFLICT, "conflicto", "No se pudo informar el inicio")
                cambios.publicar(fila.sesionId, "pantalla_inicio")
            }
            "fin" -> gestion.finalizar(fila.sesionId, idReproduccion,
                FinalizarReproduccionRequest("completada", posicion))
            "error" -> {
                gestion.finalizar(fila.sesionId, idReproduccion,
                    FinalizarReproduccionRequest("fallida", posicion))
                jdbc.update(
                    """INSERT INTO public.error_integracion
                       (id_establecimiento, id_sesion, proveedor, tipo_error, mensaje, operacion)
                       VALUES (?, ?, 'YouTube', 'reproductor', ?, 'reproduccion')""",
                    idEstablecimiento, fila.sesionId,
                    if (codigo == null) "Fallo reportado por el reproductor sin código" else "Error de reproductor YouTube ($codigo)",
                )
            }
        }
        pantallas.registrarReporte(pantalla.id)
        guardarClave(idEstablecimiento, dispositivoPantalla, clave, operacion, huella)
    }

    private fun guardarClave(
        idBar: Long, dispositivoPantalla: String, clave: UUID, operacion: String, huella: String,
    ) {
        jdbc.update(
            """INSERT INTO public.api_idempotencia
               (id_establecimiento, device_hash, clave, operacion, huella_peticion,
                respuesta_http, respuesta_json)
               VALUES (?, ?, ?, ?, ?, 204, '{}')""",
            idBar, dispositivoPantalla, clave, operacion, huella,
        )
    }

    private fun idBar(slug: String): Long = jdbc.query(
        """SELECT id_establecimiento FROM public.establecimiento
           WHERE slug = ? AND activo = TRUE""",
        { rs, _ -> rs.getLong(1) }, slug,
    ).firstOrNull() ?: throw fallo(HttpStatus.NOT_FOUND, "no_encontrado", "Bar no encontrado")

    private fun bloquearClave(idBar: Long, hash: String, clave: UUID) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
            RowCallbackHandler { }, "$idBar|$hash|$clave")
    }

    private fun numeroEntero(valor: Any?): Int? = when (valor) {
        is Int -> valor.takeIf { it >= 0 }
        is Long -> valor.takeIf { it in 0..Int.MAX_VALUE.toLong() }?.toInt()
        is Number -> valor.toDouble().let { if (it >= 0 && it <= Int.MAX_VALUE && it % 1.0 == 0.0) it.toInt() else null }
        else -> null
    }

    private fun sha256(texto: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8)),
    )

    private fun fallo(status: HttpStatus, codigo: String, mensaje: String): ContratoApiException =
        ContratoApiException(status, codigo, mensaje)
}
