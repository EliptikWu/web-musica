package com.example.rokola.datos.api

import android.util.Log
import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.Resultado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Lectura del contrato: tolera campos de más (como zod) y exige los que faltan. */
internal val JsonContrato = Json { ignoreUnknownKeys = true }

private val TIPO_JSON = "application/json; charset=utf-8".toMediaType()

/** Tiempo máximo de una llamada (apps/web/lib/api/cliente.ts) *(propuesto)*. */
private const val TIEMPO_MAXIMO_S = 8L

/**
 * Cliente HTTP de la API. No sigue redirecciones: OkHttp copiaría `X-Rockola-Dispositivo` a
 * otro host y el identificador del teléfono saldría de la API (RNF-007). Un 3xx es un error.
 */
fun nuevoHttp(): OkHttpClient = OkHttpClient.Builder()
    .callTimeout(TIEMPO_MAXIMO_S, TimeUnit.SECONDS)
    .followRedirects(false)
    .followSslRedirects(false)
    .build()

/**
 * Habla con la API como apps/web/lib/api/cliente.ts: añade `X-Rockola-Dispositivo` en todo lo
 * del cliente e `Idempotency-Key` en lo idempotente, traduce `{ codigo, mensaje, detalle }` a
 * [ErrorRockola] y valida cada respuesta con su esquema. Nunca devuelve datos sin validar.
 *
 * [base] es `ROCKOLA_API_URL` (puede llevar ruta, p. ej. `http://10.0.2.2:8080/api`).
 */
class ClienteApi(
    base: String,
    private val http: OkHttpClient,
    private val idDispositivo: () -> String,
) {
    private val base: HttpUrl = base.toHttpUrl()

    /** La dirección de [segmentos] bajo la base, cada uno codificado (un slug o token no rompe la ruta). */
    fun url(vararg segmentos: String, consulta: Map<String, String> = emptyMap()): HttpUrl =
        base.newBuilder().apply {
            segmentos.forEach { addPathSegment(it) }
            consulta.forEach { (clave, valor) -> addQueryParameter(clave, valor) }
        }.build()

    suspend fun <J, T> obtener(url: HttpUrl, esquema: DeserializationStrategy<J>, aDominio: (J) -> T): Resultado<T> =
        llamar(Request.Builder().url(url).get(), idempotente = false) { aDominio(JsonContrato.decodeFromString(esquema, it)) }

    /** Con [cuerpo] null no se manda cuerpo ni `Content-Type`, como la web (votar). */
    suspend fun <J, T> enviar(
        metodo: String,
        url: HttpUrl,
        cuerpo: String?,
        idempotente: Boolean,
        esquema: DeserializationStrategy<J>,
        aDominio: (J) -> T,
    ): Resultado<T> = llamar(peticion(metodo, url, cuerpo), idempotente) { aDominio(JsonContrato.decodeFromString(esquema, it)) }

    /** Para las operaciones que responden 204 (el aviso visto). */
    suspend fun enviarSinRespuesta(metodo: String, url: HttpUrl, cuerpo: String? = null, idempotente: Boolean = false): Resultado<Unit> =
        llamar(peticion(metodo, url, cuerpo), idempotente) { }

    private fun peticion(metodo: String, url: HttpUrl, cuerpo: String?) =
        Request.Builder().url(url).method(metodo, cuerpo?.toRequestBody(TIPO_JSON) ?: ByteArray(0).toRequestBody(null))

    /** [leer] convierte el cuerpo de la respuesta; si no cumple el contrato, lanza. */
    private suspend fun <T> llamar(peticion: Request.Builder, idempotente: Boolean, leer: (String) -> T): Resultado<T> = withContext(Dispatchers.IO) {
        peticion.header("Accept", "application/json").header("X-Rockola-Dispositivo", idDispositivo())
        // Una clave por pulsación, como la web; si OkHttp reintenta la misma petición, va igual.
        if (idempotente) peticion.header("Idempotency-Key", UUID.randomUUID().toString())
        val request = peticion.build()
        val respuesta = try {
            http.newCall(request).esperar()
        } catch (e: IOException) {
            return@withContext Resultado.Fallo(ErrorRockola(CodigoError.SIN_CONEXION, e.message ?: "Sin respuesta del servidor"))
        }
        respuesta.use { r ->
            val texto = try {
                r.body.string()
            } catch (e: IOException) {
                return@withContext Resultado.Fallo(ErrorRockola(CodigoError.SIN_CONEXION, e.message ?: "Respuesta cortada"))
            }
            if (!r.isSuccessful) return@withContext Resultado.Fallo(leerError(r.code, texto))
            try {
                Resultado.Exito(leer(texto))
            } catch (e: IllegalArgumentException) {
                // SerializationException también es IllegalArgumentException; igual que los rangos.
                Log.e(ETIQUETA, "Respuesta fuera del contrato en ${request.method} ${request.url.encodedPath}: ${e.message}")
                Resultado.Fallo(ErrorRockola(CodigoError.ERROR_INTERNO, "Respuesta fuera del contrato"))
            } catch (e: java.time.DateTimeException) {
                Log.e(ETIQUETA, "Fecha fuera del contrato en ${request.method} ${request.url.encodedPath}: ${e.message}")
                Resultado.Fallo(ErrorRockola(CodigoError.ERROR_INTERNO, "Respuesta fuera del contrato"))
            }
        }
    }

    internal companion object {
        const val ETIQUETA = "Rockola/API"
    }
}

/** `{ codigo, mensaje, detalle? }`; si el cuerpo no lo cumple, `error_interno` (como la web). */
internal fun leerError(estado: Int, texto: String): ErrorRockola {
    val cuerpo = try {
        JsonContrato.decodeFromString(ErrorApiJson.serializer(), texto)
    } catch (_: IllegalArgumentException) {
        return ErrorRockola(CodigoError.ERROR_INTERNO, "Respuesta $estado sin cuerpo de error")
    }
    val codigo = CodigoError.entries.firstOrNull { it != CodigoError.SIN_CONEXION && it.name.equals(cuerpo.codigo, ignoreCase = true) }
        ?: return ErrorRockola(CodigoError.ERROR_INTERNO, cuerpo.mensaje)
    val disponibleDesde = try {
        cuerpo.detalle?.get("disponible_desde")?.jsonPrimitive?.content?.let { OffsetDateTime.parse(it).toInstant() }
    } catch (_: RuntimeException) {
        null
    }
    return ErrorRockola(codigo, cuerpo.mensaje, disponibleDesde)
}

/** La llamada sin bloquear un hilo; se cancela si se cancela la corrutina. */
private suspend fun Call.esperar(): Response = suspendCancellableCoroutine { continuacion ->
    continuacion.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = continuacion.resume(response) { response.close() }
        override fun onFailure(call: Call, e: IOException) = continuacion.resumeWithException(e)
    })
}
