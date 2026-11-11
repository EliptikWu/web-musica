package com.example.rokola.datos.api

import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.EstadoServicio
import com.example.rokola.dominio.FalloRockola
import com.example.rokola.dominio.Resultado
import com.example.rokola.dominio.TipoRespuestaSolicitud
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * El cliente HTTP contra un servidor de prueba que responde lo que dice el contrato
 * (packages/contrato). Comprueba rutas, cabeceras, la lectura de cada cuerpo y los errores.
 */
class RepositorioApiTest {
    private val servidor = MockWebServer()
    private var slug: String? = "la-rockola"
    private lateinit var repo: RepositorioApi

    @Before
    fun antes() {
        servidor.start()
        // Con ruta en la base, como ROCKOLA_API_URL=http://host:8080/api.
        val api = ClienteApi(servidor.url("/api").toString(), nuevoHttp()) { "uuid-de-prueba" }
        repo = RepositorioApi(api, nuevoHttp(), slugActual = { slug }, tipoDispositivo = { "movil" })
    }

    @After
    fun despues() = servidor.close()

    private fun responder(codigo: Int, cuerpo: String = "", tipo: String = "application/json") =
        servidor.enqueue(MockResponse.Builder().code(codigo).addHeader("Content-Type", tipo).body(cuerpo).build())

    @Test
    fun `lee el bar y lo pide con la ruta y el teléfono del contrato`() = runBlocking {
        responder(200, BAR)
        val r = repo.bar()
        val bar = (r as Resultado.Exito).valor
        assertEquals("La Rockola", bar.nombre)
        assertEquals(3, bar.pedidosMaximos)
        assertEquals(7, bar.horario.single().diaSemana)
        val pedido = servidor.takeRequest()
        assertEquals("GET", pedido.method)
        assertEquals("/api/bares/la-rockola", pedido.url.encodedPath)
        assertEquals("uuid-de-prueba", pedido.headers["X-Rockola-Dispositivo"])
        assertNull("Un GET no lleva clave de idempotencia", pedido.headers["Idempotency-Key"])
    }

    @Test
    fun `lee la foto de la noche con sus fechas y la cola`() = runBlocking {
        responder(200, AHORA)
        val ahora = (repo.ahora() as Resultado.Exito).valor
        assertEquals(EstadoServicio.ABIERTO, ahora.servicio.estado)
        assertEquals(Instant.parse("2026-10-23T03:45:00Z"), ahora.servidorAhora)
        assertEquals("DtMF", ahora.cola.single().cancion.titulo)
        assertEquals("https://i.ytimg.com/vi/abc/mqdefault.jpg", ahora.cola.single().cancion.miniaturaUrl)
    }

    @Test
    fun `pedir manda el video, una clave de idempotencia y lee la respuesta`() = runBlocking {
        responder(201, """{"resultado":"creada","solicitud_id":"s9","puesto":4,"espera_seg":720,"pedidos_restantes":1}""")
        val r = (repo.pedir("abc") as Resultado.Exito).valor
        assertEquals(TipoRespuestaSolicitud.CREADA, r.resultado)
        assertEquals(4, r.puesto)
        val pedido = servidor.takeRequest()
        assertEquals("POST", pedido.method)
        assertEquals("/api/bares/la-rockola/solicitudes", pedido.url.encodedPath)
        assertEquals("""{"video_id":"abc"}""", pedido.body?.utf8())
        assertNotNull(pedido.headers["Idempotency-Key"])
    }

    @Test
    fun `cada pedido lleva su propia clave de idempotencia`() = runBlocking {
        repeat(2) { responder(200, """{"solicitud_id":"s1","votos_total":2}""") }
        repo.votar("s1")
        repo.votar("s1")
        val claves = List(2) { servidor.takeRequest().headers["Idempotency-Key"] }
        assertTrue(claves[0] != claves[1])
    }

    @Test
    fun `votar no manda cuerpo ni Content-Type, como la web`() = runBlocking {
        responder(200, """{"solicitud_id":"s1","votos_total":2}""")
        repo.votar("s1")
        val pedido = servidor.takeRequest()
        assertEquals(0L, pedido.bodySize)
        assertNull(pedido.headers["Content-Type"])
    }

    @Test
    fun `no sigue redirecciones y el identificador no sale hacia otro host`() = runBlocking {
        val otro = MockWebServer().apply { start() }
        try {
            servidor.enqueue(MockResponse.Builder().code(302).addHeader("Location", otro.url("/robo").toString()).build())
            val r = repo.ahora() as Resultado.Fallo
            assertEquals(CodigoError.ERROR_INTERNO, r.error.codigo)
            assertEquals(0, otro.requestCount)
        } finally {
            otro.close()
        }
    }

    @Test
    fun `bar con slug consulta ese bar sin cambiar el actual`() = runBlocking {
        responder(200, BAR.replace("la-rockola", "otro-bar"))
        repo.bar("otro-bar")
        assertEquals("/api/bares/otro-bar", servidor.takeRequest().url.encodedPath)
    }

    @Test
    fun `mover el voto usa PUT con desde y hacia`() = runBlocking {
        responder(200, """{"solicitud_id":"s2","votos_total":5}""")
        repo.moverVoto("s1", "s2")
        val pedido = servidor.takeRequest()
        assertEquals("PUT", pedido.method)
        assertEquals("/api/bares/la-rockola/dispositivo/voto", pedido.url.encodedPath)
        assertEquals("""{"desde":"s1","hacia":"s2"}""", pedido.body?.utf8())
    }

    @Test
    fun `registrar el teléfono manda su tipo`() = runBlocking {
        responder(200, YO)
        val yo = (repo.registrarDispositivo() as Resultado.Exito).valor
        assertEquals(2, yo.pedidosRestantes)
        assertEquals("""{"tipo":"movil"}""", servidor.takeRequest().body?.utf8())
    }

    @Test
    fun `el aviso visto acepta 204 sin cuerpo`() = runBlocking {
        servidor.enqueue(MockResponse.Builder().code(204).build())
        assertTrue(repo.anotarAvisoVisto() is Resultado.Exito)
        assertEquals("/api/bares/la-rockola/dispositivo/aviso", servidor.takeRequest().url.encodedPath)
    }

    @Test
    fun `la consulta y el token van codificados`() = runBlocking {
        responder(200, """{"consulta":"bad bunny","en_cola":[],"resultados":[],"desde_cache":false}""")
        repo.buscar("bad bunny & co")
        assertEquals("bad bunny & co", servidor.takeRequest().url.queryParameter("q"))
        responder(200, """{"slug":"la-rockola"}""")
        assertEquals("la-rockola", (repo.resolverQr("a/b") as Resultado.Exito).valor)
        assertEquals("/api/qr/a%2Fb", servidor.takeRequest().url.encodedPath)
    }

    @Test
    fun `traduce el error del contrato a su código`() = runBlocking {
        responder(409, """{"codigo":"limite_pedidos","mensaje":"Ya tienes 3"}""")
        val r = repo.pedir("abc") as Resultado.Fallo
        assertEquals(CodigoError.LIMITE_PEDIDOS, r.error.codigo)
    }

    @Test
    fun `lee disponible_desde del detalle de reproducida_hace_poco`() = runBlocking {
        responder(422, """{"codigo":"reproducida_hace_poco","mensaje":"x","detalle":{"disponible_desde":"2026-10-22T23:30:00-05:00"}}""")
        val r = repo.pedir("abc") as Resultado.Fallo
        assertEquals(CodigoError.REPRODUCIDA_HACE_POCO, r.error.codigo)
        assertEquals(Instant.parse("2026-10-23T04:30:00Z"), r.error.disponibleDesde)
    }

    @Test
    fun `un error sin el cuerpo del contrato es error_interno`() = runBlocking {
        // Lo que hoy devuelve Spring con ProblemDetail.
        responder(404, """{"type":"about:blank","title":"Not Found","status":404}""", "application/problem+json")
        assertEquals(CodigoError.ERROR_INTERNO, (repo.bar() as Resultado.Fallo).error.codigo)
    }

    @Test
    fun `una respuesta fuera del contrato es un error, no un dato a medias`() = runBlocking {
        // camelCase en vez de snake_case: falta pedidos_maximos.
        responder(200, """{"slug":"la-rockola","nombre":"x","zonaHoraria":"America/Bogota","pedidosMaximos":3}""")
        assertEquals(CodigoError.ERROR_INTERNO, (repo.bar() as Resultado.Fallo).error.codigo)
        // Fecha sin zona.
        responder(200, AHORA.replace("2026-10-22T22:45:00-05:00", "2026-10-22T22:45:00"))
        assertEquals(CodigoError.ERROR_INTERNO, (repo.ahora() as Resultado.Fallo).error.codigo)
        // Un número fuera de rango.
        responder(200, BAR.replace("\"dia_semana\":7", "\"dia_semana\":8"))
        assertEquals(CodigoError.ERROR_INTERNO, (repo.bar() as Resultado.Fallo).error.codigo)
    }

    @Test
    fun `sin servidor es SIN_CONEXION`() = runBlocking {
        servidor.close()
        assertEquals(CodigoError.SIN_CONEXION, (repo.ahora() as Resultado.Fallo).error.codigo)
    }

    @Test
    fun `sin bar elegido no llama a nadie`() = runBlocking {
        slug = null
        assertEquals(CodigoError.NO_ENCONTRADO, (repo.bar() as Resultado.Fallo).error.codigo)
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun `el tiempo real emite cada foto y termina con SIN_CONEXION al cortarse`() = runBlocking {
        // Una línea por evento: en SSE un salto de línea dentro de `data:` corta el JSON.
        val evento = """{"tipo":"ahora","ahora":${AHORA.lines().joinToString("") { it.trim() }}}"""
        responder(
            200,
            "event: ping\ndata: {\"tipo\":\"ping\",\"servidor_ahora\":\"2026-10-22T22:45:00-05:00\"}\n\n" +
                "event: ahora\ndata: $evento\n\n" +
                "event: panel\ndata: {\"tipo\":\"panel\",\"recursos\":[\"noche\"]}\n\n" +
                "event: ahora\ndata: {\"tipo\":\"ahora\",\"ahora\":{\"roto\":true}}\n\n",
            "text/event-stream",
        )
        var fallo: Throwable? = null
        val fotos = repo.observarAhora().catch { fallo = it }.toList()
        assertEquals("Solo la foto válida; el evento roto se ignora", 1, fotos.size)
        assertEquals(CodigoError.SIN_CONEXION, (fallo as FalloRockola).error.codigo)
        val pedido = servidor.takeRequest()
        assertEquals("/api/bares/la-rockola/eventos", pedido.url.encodedPath)
        assertEquals("text/event-stream", pedido.headers["Accept"])
    }

    private companion object {
        val BAR = """{"slug":"la-rockola","nombre":"La Rockola","zona_horaria":"America/Bogota","pedidos_maximos":3,
            "duracion_max_seg":600,"ventana_no_repeticion_min":60,
            "horario":[{"dia_semana":7,"hora_inicio":"20:00","hora_fin":"02:00","activo":true}],"campo_nuevo":1}"""

        val AHORA = """{"version":12,"servidor_ahora":"2026-10-22T22:45:00-05:00",
            "servicio":{"estado":"abierto","modo_qr":"automatico","desde":null,"hasta":"2026-10-23T02:00:00-05:00","proxima_apertura":null},
            "reproduccion":null,
            "cola":[{"solicitud_id":"s1","cancion":{"video_id":"abc","titulo":"DtMF","artista":"Bad Bunny","duracion_seg":237,
              "miniatura_url":"https://i.ytimg.com/vi/abc/mqdefault.jpg"},"votos_total":5,"fecha_solicitud":"2026-10-22T22:30:00-05:00",
              "puesto":1,"puesto_anterior":null,"espera_seg":120}],
            "lista_casa":null}"""

        val YO = """{"pedidos_maximos":3,"pedidos_restantes":2,"solicitudes":[],"votos":["s1"],"aviso_visto_en":null}"""
    }
}
