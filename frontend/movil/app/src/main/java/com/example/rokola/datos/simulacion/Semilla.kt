package com.example.rokola.datos.simulacion

import com.example.rokola.dominio.EstadoSolicitud
import com.example.rokola.dominio.Franja
import com.example.rokola.dominio.ModoQr
import com.example.rokola.dominio.OrigenReproduccion
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/*
 * Las noches de ejemplo (apps/web/simulacion/datos/semilla.ts). Votos, horas y métricas son
 * datos de ejemplo. El teléfono que abre la simulación es «el de la persona» de los
 * prototipos: pidió Rebelión (la que suena) y Monaco, y votó por DtMF. Le quedan 2 de 3 antes
 * de pedir Monaco: la simulación cuenta en el cupo las pendientes y la que suena (TBD, doc 04).
 */

/** Los escenarios de la simulación (apps/web/simulacion/escenarios.ts). */
enum class Escenario(val id: String, val nombre: String) {
    MOMENTO_B("momento-b", "Noche en curso (10:45 p. m.)"),
    MOMENTO_A("momento-a", "Antes de pedir Monaco (10:24 p. m.)"),
    FUERA_DE_HORARIO("fuera-de-horario", "Fuera de horario"),
    EN_PAUSA("en-pausa", "Solicitudes en pausa"),
    MANTENIMIENTO("mantenimiento", "Mantenimiento"),
    COLA_VACIA("cola-vacia", "Cola vacía"),
    SIN_PEDIDOS("sin-pedidos", "Sin pedidos libres"),
    SIN_LISTA_ACTIVA("sin-lista-activa", "Sin lista de la casa"),
    YOUTUBE_CAIDO("youtube-caido", "YouTube no responde"),
    CUOTA_AGOTADA("cuota-agotada", "Cuota de YouTube agotada"),
    DESCONEXION("desconexion", "Sin conexión en vivo"),
}

/** Hora de Bogotá (UTC−5, sin horario de verano) a milisegundos. */
private fun bogota(fecha: String, hora: String): Long =
    OffsetDateTime.parse("${fecha}T$hora-05:00").toInstant().toEpochMilli()

private const val JUEVES = "2026-10-22"
private const val MIN = 60_000L
private const val SESION = "n-2026-10-22"

internal const val CODIGO_PERSONA = "7F3K"

private val HORARIO = listOf(4, 5, 6).map { dia ->
    Franja(diaSemana = dia, horaInicio = LocalTime.of(20, 0), horaFin = LocalTime.of(2, 0), activo = true)
}

/** Votantes de relleno: teléfonos de otras mesas. */
private fun otros(n: Int, desde: Int = 0): List<String> = (1..n).map { String.format(java.util.Locale.ROOT, "tel-%03d", desde + it) }

private fun solicitud(
    id: String,
    video: String,
    dispositivo: String,
    fecha: Long,
    votos: List<String>,
    estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE,
    inicio: Long? = null,
    fin: Long? = null,
    subida: Subida? = null,
) = SolicitudSim(id, SESION, "sim-$video", dispositivo, fecha, votos.toMutableList(), estado, inicio, fin, null, subida)

private fun lista(id: String, nombre: String, videos: List<String>, activa: Boolean) = ListaSim(
    id = id,
    nombre = nombre,
    modoRotacion = ModoRotacion.SECUENCIAL,
    activa = activa,
    elementos = videos.mapIndexed { i, v -> ElementoLista("$id-e${i + 1}", v) },
)

/** Momento B: jueves 10:45 p. m. (cola, TV y panel de los prototipos). */
private fun momentoB(p: String): EstadoSim {
    val ahora = bogota(JUEVES, "22:45:00")
    fun hace(min: Int) = ahora - min * MIN
    val inicioRebelion = ahora - 161_000

    val solicitudes = mutableListOf(
        // Sonó a las 9:40 p. m. (para «Lo más votado de la noche»).
        solicitud("s-hips", "hips-dont-lie", "tel-090", bogota(JUEVES, "21:05:00"), listOf("tel-090") + otros(18, 60),
            EstadoSolicitud.REPRODUCIDA, bogota(JUEVES, "21:40:00"), bogota(JUEVES, "21:43:38")),
        // Saltada y bloqueada a las 10:32 p. m.
        solicitud("s-gasolina", "gasolina", "tel-091", bogota(JUEVES, "22:05:00"), otros(6, 70),
            EstadoSolicitud.SALTADA, bogota(JUEVES, "22:31:10"), bogota(JUEVES, "22:32:00")),
        // La que suena: la pidió la persona hace 31 min.
        solicitud("s-rebelion", "rebelion", p, hace(31), listOf(p) + otros(23), EstadoSolicitud.REPRODUCIENDO, inicioRebelion),
        solicitud("s-cali", "cali-pachanguero", "tel-030", hace(26), listOf("tel-030") + otros(20, 30)),
        solicitud("s-dtmf", "dtmf", "tel-041", hace(31), listOf("tel-041", p) + otros(16, 41), subida = Subida(1, hace(1))),
        solicitud("s-monaco", "monaco", p, hace(14), listOf(p) + otros(14, 2), subida = Subida(2, hace(1))),
        solicitud("s-bici", "la-bicicleta", "tel-050", hace(9), listOf("tel-050") + otros(14, 50)),
        solicitud("s-nuevayol", "nuevayol", "tel-020", hace(22), listOf("tel-020") + otros(13, 20)),
        solicitud("s-tqg", "tqg", "tel-060", hace(7), listOf("tel-060") + otros(8, 60)),
        solicitud("s-luna", "luna", "tel-070", hace(5), listOf("tel-070") + otros(6, 70)),
        solicitud("s-aventura", "una-aventura", "tel-080", hace(4), listOf("tel-080") + otros(3, 80)),
        solicitud("s-titi", "titi-me-pregunto", "tel-085", hace(2), listOf("tel-085", "tel-086")),
    )

    val admin = "Admin de prueba"
    val bloqueos = mutableListOf(
        BloqueoSim("b-bachata", "sim-la-bachata", "No va con la noche de salsa", DuracionBloqueo.FIN_SESION, bogota(JUEVES, "22:41:00"), null, admin),
        BloqueoSim("b-gasolina", "sim-gasolina", "Pedida tres veces seguidas", DuracionBloqueo.FIN_SESION, bogota(JUEVES, "22:32:05"), null, admin),
        BloqueoSim("b-despecha", "sim-despecha", "Fuera del estilo del bar", DuracionBloqueo.INDEFINIDO, bogota("2026-10-16", "23:10:00"), null, admin),
    )

    return EstadoSim(
        escenario = Escenario.MOMENTO_B,
        version = 1,
        reloj = RelojSim(anclaSim = ahora, anclaReal = 0, velocidad = 1.0),
        bar = BarSim(
            id = "e-demo",
            slug = "demo",
            nombre = "Bar de demostración",
            zonaHoraria = ZoneId.of("America/Bogota"),
            pedidosMaximos = 3,
            duracionMaxSeg = 600,
            ventanaNoRepeticionMin = 60,
        ),
        modoQr = ModoQr.AUTOMATICO,
        mantenimiento = false,
        horario = HORARIO,
        sesion = SesionSim(SESION, bogota(JUEVES, "20:00:00"), "normal", "horario"),
        sinAperturaAutomaticaHasta = null,
        catalogo = crearCatalogo(),
        solicitudes = solicitudes,
        reproduccion = ReproduccionSim("r-rebelion", "sim-rebelion", OrigenReproduccion.SOLICITUD, "s-rebelion", inicioRebelion, 0.0, inicioRebelion),
        listas = listOf(
            lista("l-salsa", "Salsa y tropical de la casa", LISTA_SALSA_Y_TROPICAL, activa = true),
            lista("l-crossover", "Viernes crossover", LISTA_VIERNES_CROSSOVER, activa = false),
        ),
        bloqueos = bloqueos,
        acciones = mutableListOf(
            AccionSim("abrir_sesion", bogota(JUEVES, "20:00:00")),
            AccionSim("saltar", bogota(JUEVES, "22:32:00")),
            AccionSim("bloquear", bogota(JUEVES, "22:32:05")),
            AccionSim("bloquear", bogota(JUEVES, "22:41:00")),
        ),
        dispositivos = mutableMapOf(p to DispositivoSim(CODIGO_PERSONA, "movil", hace(40), hace(40))),
        secuencia = 100,
    )
}

/** Momento A: jueves 10:24 p. m. (buscar de los prototipos; también la bienvenida). */
private fun momentoA(p: String): EstadoSim {
    val estado = momentoB(p)
    val ahora = bogota(JUEVES, "22:24:00")
    fun hace(min: Int) = ahora - min * MIN
    val inicioRebelion = ahora - (358 - 98) * 1000L // le quedan 1:38
    val hips = estado.solicitudes.first { it.id == "s-hips" }
    estado.solicitudes.clear()
    estado.solicitudes += listOf(
        hips,
        solicitud("s-rebelion", "rebelion", p, hace(10), listOf(p) + otros(23), EstadoSolicitud.REPRODUCIENDO, inicioRebelion),
        solicitud("s-cali", "cali-pachanguero", "tel-030", hace(5), listOf("tel-030") + otros(14, 30)),
        solicitud("s-nuevayol", "nuevayol", "tel-020", hace(1), listOf("tel-020") + otros(13, 20)),
        solicitud("s-dtmf", "dtmf", "tel-041", hace(10), listOf("tel-041", p) + otros(11, 41)),
    )
    estado.reproduccion = ReproduccionSim("r-rebelion", "sim-rebelion", OrigenReproduccion.SOLICITUD, "s-rebelion", inicioRebelion, 0.0, inicioRebelion)
    estado.bloqueos.removeAll { it.id != "b-despecha" }
    estado.acciones.removeAll { it.tipo != "abrir_sesion" }
    estado.reloj = estado.reloj.copy(anclaSim = ahora)
    return estado
}

/** Crea el estado de un escenario. [persona] es el ID del teléfono que abre la simulación. */
internal fun crearEstado(escenario: Escenario, persona: String, ahoraReal: Long, velocidad: Double = 1.0): EstadoSim {
    val estado = if (escenario == Escenario.MOMENTO_A) momentoA(persona) else momentoB(persona)
    estado.escenario = escenario
    estado.reloj = RelojSim(estado.reloj.anclaSim, ahoraReal, velocidad)

    fun pendientesA(nuevo: EstadoSolicitud) {
        for (s in estado.solicitudes) if (s.estado == EstadoSolicitud.PENDIENTE) s.estado = nuevo
    }

    when (escenario) {
        Escenario.FUERA_DE_HORARIO -> {
            // Jueves 6:30 p. m.: aún no abre. Suena la lista de la casa.
            val ahora = bogota(JUEVES, "18:30:00")
            estado.reloj = estado.reloj.copy(anclaSim = ahora)
            estado.sesion = null
            estado.solicitudes.clear()
            estado.acciones.clear()
            estado.bloqueos.removeAll { it.id != "b-despecha" }
            estado.reproduccion = ReproduccionSim("r-casa-1", "sim-gotas-de-lluvia", OrigenReproduccion.LISTA_CASA, null, ahora - 70_000, 0.0, ahora - 70_000)
            estado.listas[0].indice = 4
        }
        Escenario.EN_PAUSA -> {
            estado.modoQr = ModoQr.PAUSADO
            estado.acciones += AccionSim("pausar_solicitudes", estado.reloj.anclaSim - MIN)
        }
        Escenario.MANTENIMIENTO -> {
            estado.mantenimiento = true
            estado.sesion = null
            pendientesA(EstadoSolicitud.EXPIRADA)
        }
        Escenario.COLA_VACIA -> pendientesA(EstadoSolicitud.REPRODUCIDA)
        Escenario.SIN_PEDIDOS -> {
            // La persona ya tiene la que suena y dos más en la cola: 0 de 3.
            for (id in listOf("s-tqg", "s-luna")) {
                val s = estado.solicitudes.first { it.id == id }
                s.dispositivo = persona
                s.votos[0] = persona
            }
        }
        Escenario.SIN_LISTA_ACTIVA -> {
            for (l in estado.listas) l.activa = false
            pendientesA(EstadoSolicitud.REPRODUCIDA)
        }
        Escenario.YOUTUBE_CAIDO -> estado.falloYoutube = FalloYoutube.CAIDO
        Escenario.CUOTA_AGOTADA -> estado.falloYoutube = FalloYoutube.CUOTA
        Escenario.DESCONEXION -> estado.falloConexion = FalloConexion.CORTADA
        Escenario.MOMENTO_B, Escenario.MOMENTO_A -> Unit
    }
    return estado
}
