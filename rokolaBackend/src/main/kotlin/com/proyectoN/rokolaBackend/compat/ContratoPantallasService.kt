package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioPanelPublisher
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.HexFormat

/** Vinculación RF-058 / ADR-23. Token secreto de 256 bits: solo su SHA-256 llega a PostgreSQL.
 * La tabla es persistente para que revocar siga funcionando después de reiniciar Railway.
 */
@Service
class ContratoPantallasService(
    private val jdbc: JdbcTemplate,
    private val cambioPanel: CambioPanelPublisher,
    @Value("\${ROKOLA_PANTALLA_COOKIE_SEGURA:true}") private val cookieSegura: Boolean,
) {
    data class PantallaAutorizada(val id: Long, val barId: Long)

    private val azar = SecureRandom()
    private val alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val codigoValido = Regex("^[A-HJ-NP-Z2-9]{6}$")
    private val tokenValido = Regex("^[A-Za-z0-9_-]{43}$")

    @Transactional(readOnly = true)
    fun consultar(slug: String, token: String?): Map<String, Any?> {
        val barId = idBar(slug)
        if (!validarToken(token)) return mapOf("estado" to "sin_vincular")
        val fila = jdbc.query(
            """SELECT estado, nombre, codigo, codigo_expira_en
               FROM public.pantalla_vinculo WHERE id_establecimiento = ? AND token_hash = ?""",
            { rs, _ ->
                FilaEstado(rs.getString("estado"), rs.getString("nombre"),
                    rs.getString("codigo"), rs.getObject("codigo_expira_en", OffsetDateTime::class.java))
            }, barId, huella(token!!),
        ).firstOrNull() ?: return mapOf("estado" to "sin_vincular")
        return when (fila.estado) {
            "vinculada" -> mapOf("estado" to "vinculada", "nombre" to fila.nombre)
            "esperando" -> if (fila.codigo != null && fila.expira?.isAfter(OffsetDateTime.now(ZoneOffset.UTC)) == true)
                mapOf("estado" to "esperando", "codigo" to fila.codigo, "expira_en" to fila.expira)
                else mapOf("estado" to "sin_vincular")
            else -> mapOf("estado" to "sin_vincular")
        }
    }

    private data class FilaEstado(val estado: String, val nombre: String?, val codigo: String?, val expira: OffsetDateTime?)

    /** Cada POST renueva el código en esa misma pantalla, o crea una pantalla nueva. */
    @Transactional
    fun pedirCodigo(slug: String, tokenAnterior: String?): Pair<Map<String, Any?>, ResponseCookie> {
        val barId = idBar(slug)
        val reutilizar = validarToken(tokenAnterior) && jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM public.pantalla_vinculo WHERE id_establecimiento = ? AND token_hash = ?)",
            Boolean::class.javaObjectType, barId, huella(tokenAnterior!!),
        ) == true
        val token = if (reutilizar) tokenAnterior!! else nuevoToken()
        val codigo = nuevoCodigo()
        val expira = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10)
        if (reutilizar) {
            jdbc.update(
                """UPDATE public.pantalla_vinculo SET codigo = ?, codigo_expira_en = ?,
                   estado = 'esperando', nombre = NULL, id_usuario_vinculador = NULL,
                   vinculada_en = NULL, ultimo_reporte = NULL
                   WHERE id_establecimiento = ? AND token_hash = ?""",
                codigo, expira, barId, huella(token),
            )
        } else {
            jdbc.update(
                """INSERT INTO public.pantalla_vinculo
                   (id_establecimiento, token_hash, codigo, codigo_expira_en, estado)
                   VALUES (?, ?, ?, ?, 'esperando')""",
                barId, huella(token), codigo, expira,
            )
        }
        return mapOf("estado" to "esperando", "codigo" to codigo, "expira_en" to expira) to cookie(slug, token)
    }

    // FOR UPDATE serializa los reportes de esta pantalla con su eventual revocación.
    // El método participa de la transacción externa de reportar().
    @Transactional
    fun exigirVinculada(slug: String, token: String?): PantallaAutorizada {
        // No revelar si el token pertenece a otro bar: mismo error que cookie ausente.
        if (!validarToken(token)) throw noAutenticado()
        val id = jdbc.query(
            """SELECT p.id_pantalla, p.id_establecimiento
               FROM public.pantalla_vinculo p
               JOIN public.establecimiento e ON e.id_establecimiento = p.id_establecimiento
               WHERE e.slug = ? AND e.activo = TRUE AND p.token_hash = ? AND p.estado = 'vinculada'
               FOR UPDATE OF p""",
            { rs, _ -> PantallaAutorizada(rs.getLong(1), rs.getLong(2)) }, slug, huella(token!!),
        ).firstOrNull()
        return id ?: throw noAutenticado()
    }

    fun registrarReporte(pantallaId: Long) {
        jdbc.update(
            "UPDATE public.pantalla_vinculo SET ultimo_reporte = CURRENT_TIMESTAMP WHERE id_pantalla = ? AND estado = 'vinculada'",
            pantallaId,
        )
    }

    @Transactional(readOnly = true)
    fun listar(personal: ContratoPanelAcceso.Personal): Map<String, Any> {
        val pantallas = jdbc.query(
            """SELECT p.id_pantalla, p.nombre, p.vinculada_en, p.ultimo_reporte,
                      u.id_usuario, u.nombre AS autor
               FROM public.pantalla_vinculo p
               JOIN public.usuario u ON u.id_usuario = p.id_usuario_vinculador
               WHERE p.id_establecimiento = ? AND p.estado = 'vinculada'
               ORDER BY p.vinculada_en DESC""",
            { rs, _ -> mapOf<String, Any?>(
                "id" to rs.getLong("id_pantalla").toString(),
                "nombre" to rs.getString("nombre"),
                "vinculada_en" to rs.getObject("vinculada_en", OffsetDateTime::class.java),
                "vinculada_por" to mapOf(
                    "id" to rs.getLong("id_usuario").toString(), "nombre" to rs.getString("autor"),
                ),
                "ultimo_reporte" to rs.getObject("ultimo_reporte", OffsetDateTime::class.java),
            ) }, personal.barId,
        )
        return mapOf("pantallas" to pantallas)
    }

    /** Se acepta solo un código pendiente, no vencido y del bar del administrador. */
    @Transactional
    fun vincular(personal: ContratoPanelAcceso.Personal, cuerpo: Map<String, Any?>?): Map<String, Any?> {
        val codigo = cuerpo?.get("codigo") as? String
        val nombre = (cuerpo?.get("nombre") as? String)?.trim()
        if (codigo == null || !codigoValido.matches(codigo) || nombre.isNullOrEmpty() || nombre.length > 40 ||
            (cuerpo?.keys?.any { it !in setOf("codigo", "nombre") } == true)
        ) throw fallo(HttpStatus.BAD_REQUEST, "datos_invalidos", "Se requiere un código de seis caracteres y un nombre de hasta 40 caracteres")
        // PostgreSQL bloquea la fila y solo una vinculación puede consumir el código.
        val vinculada = jdbc.query(
            """UPDATE public.pantalla_vinculo SET
                      estado = 'vinculada', codigo = NULL, codigo_expira_en = NULL,
                      nombre = ?, id_usuario_vinculador = ?, vinculada_en = CURRENT_TIMESTAMP
               WHERE id_establecimiento = ? AND codigo = ? AND estado = 'esperando'
                 AND codigo_expira_en > CURRENT_TIMESTAMP
               RETURNING id_pantalla, nombre, vinculada_en, ultimo_reporte""",
            { rs, _ -> mapOf<String, Any?>(
                "id" to rs.getLong("id_pantalla").toString(),
                "nombre" to rs.getString("nombre"),
                "vinculada_en" to rs.getObject("vinculada_en", OffsetDateTime::class.java),
                "vinculada_por" to mapOf("id" to personal.id.toString(), "nombre" to personal.nombre),
                "ultimo_reporte" to rs.getObject("ultimo_reporte", OffsetDateTime::class.java),
            ) }, nombre, personal.id, personal.barId, codigo,
        ).firstOrNull() ?: throw fallo(HttpStatus.NOT_FOUND, "no_encontrado", "El código no existe o venció")
        val id = (vinculada["id"] as String).toLong()
        registrarAccion(personal, "vincular_pantalla", id, nombre)
        cambioPanel.publicar(personal.barSlug, setOf("pantallas", "bitacora"))
        return vinculada
    }

    @Transactional
    fun desvincular(personal: ContratoPanelAcceso.Personal, idTexto: String) {
        val id = idTexto.toLongOrNull()?.takeIf { it > 0 } ?: throw fallo(HttpStatus.NOT_FOUND, "no_encontrado", "Pantalla no encontrada")
        val pantalla = jdbc.query(
            """UPDATE public.pantalla_vinculo SET estado = 'revocada',
                      codigo = NULL, codigo_expira_en = NULL
               WHERE id_pantalla = ? AND id_establecimiento = ? AND estado = 'vinculada'
               RETURNING nombre""",
            { rs, _ -> rs.getString(1) }, id, personal.barId,
        ).firstOrNull() ?: throw fallo(HttpStatus.NOT_FOUND, "no_encontrado", "Pantalla no encontrada")
        registrarAccion(personal, "desvincular_pantalla", id, pantalla)
        cambioPanel.publicar(personal.barSlug, setOf("pantallas", "bitacora"))
    }

    private fun registrarAccion(personal: ContratoPanelAcceso.Personal, tipo: String, objetivoId: Long, nombre: String) {
        jdbc.update(
            """INSERT INTO public.accion_admin
               (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
               VALUES (?, ?, ?, 'pantalla', ?, jsonb_build_object('nombre', ?))""",
            personal.barId, personal.id, tipo, objetivoId, nombre,
        )
    }

    /** Código legible solo durante 10 minutos; los códigos caducados se eliminan periódicamente. */
    @Scheduled(fixedDelay = 600_000L, initialDelay = 600_000L)
    fun limpiarCodigosVencidos() {
        jdbc.update("""UPDATE public.pantalla_vinculo
                      SET estado = 'revocada', codigo = NULL, codigo_expira_en = NULL
                      WHERE estado = 'esperando' AND codigo_expira_en <= CURRENT_TIMESTAMP""")
    }

    fun cookie(slug: String, token: String): ResponseCookie = ResponseCookie.from(COOKIE, token)
        .httpOnly(true).secure(cookieSegura).sameSite("Strict")
        .path("/api/bares/$slug/pantalla").maxAge(Duration.ofDays(365)).build()

    private fun nuevoToken(): String {
        val b = ByteArray(32).also(azar::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    }

    private fun nuevoCodigo(): String {
        repeat(20) {
            val b = ByteArray(6).also(azar::nextBytes)
            val codigo = b.map { alfabeto[(it.toInt() and 0xff) % alfabeto.length] }.joinToString("")
            if (jdbc.queryForObject(
                    "SELECT NOT EXISTS(SELECT 1 FROM public.pantalla_vinculo WHERE codigo = ?)",
                    Boolean::class.javaObjectType, codigo,
                ) == true) return codigo
        }
        throw fallo(HttpStatus.CONFLICT, "conflicto", "No se pudo reservar un código nuevo")
    }

    private fun validarToken(token: String?): Boolean = token != null && tokenValido.matches(token)
    private fun huella(token: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)),
    )
    private fun idBar(slug: String): Long = jdbc.query(
        "SELECT id_establecimiento FROM public.establecimiento WHERE slug = ? AND activo = TRUE",
        { rs, _ -> rs.getLong(1) }, slug,
    ).firstOrNull() ?: throw fallo(HttpStatus.NOT_FOUND, "no_encontrado", "Bar no encontrado")
    private fun noAutenticado() = fallo(HttpStatus.UNAUTHORIZED, "no_autenticado", "Pantalla no vinculada")
    private fun fallo(status: HttpStatus, codigo: String, mensaje: String) = ContratoApiException(status, codigo, mensaje)

    companion object { const val COOKIE = "rockola_pantalla" }
}
