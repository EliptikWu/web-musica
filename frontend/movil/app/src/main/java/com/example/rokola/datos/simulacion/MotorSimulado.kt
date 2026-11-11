package com.example.rokola.datos.simulacion

import com.example.rokola.dominio.Ahora
import com.example.rokola.dominio.Busqueda
import com.example.rokola.dominio.Cancion
import com.example.rokola.dominio.CodigoError
import com.example.rokola.dominio.ContextoDestacada
import com.example.rokola.dominio.Destacada
import com.example.rokola.dominio.ErrorRockola
import com.example.rokola.dominio.EstadoReproduccion
import com.example.rokola.dominio.EstadoServicio
import com.example.rokola.dominio.EstadoSolicitud
import com.example.rokola.dominio.ItemCola
import com.example.rokola.dominio.ListaCasaResumen
import com.example.rokola.dominio.MiSolicitud
import com.example.rokola.dominio.ModoQr
import com.example.rokola.dominio.MotivoNoPedible
import com.example.rokola.dominio.OrigenReproduccion
import com.example.rokola.dominio.Reproduccion
import com.example.rokola.dominio.RespuestaSolicitud
import com.example.rokola.dominio.RespuestaVoto
import com.example.rokola.dominio.Resultado
import com.example.rokola.dominio.ResultadoBusqueda
import com.example.rokola.dominio.Servicio
import com.example.rokola.dominio.TipoRespuestaSolicitud
import com.example.rokola.dominio.Yo
import com.example.rokola.dominio.reglas.abiertoPorHorario
import com.example.rokola.dominio.reglas.cierreDeFranja
import com.example.rokola.dominio.reglas.duracionFranja
import com.example.rokola.dominio.reglas.franjaVigente
import com.example.rokola.dominio.reglas.proximaApertura
import java.text.Normalizer
import java.time.Instant
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

/*
 * El «servidor» de la simulación: la parte del cliente de apps/web/simulacion/motor.ts,
 * portada regla por regla. Funciones puras sobre [EstadoSim]; el repositorio las llama
 * dentro de un candado. [ahora] es la hora simulada en milisegundos.
 */

private const val VIGENCIA_SUBIDA_MS = 10 * 60_000L // «Subió N puestos» se ve 10 min (propuesto)
private const val DIA_MS = 24 * 3600_000L

private fun <T> bien(valor: T): Resultado<T> = Resultado.Exito(valor)
private fun mal(codigo: CodigoError, mensaje: String, disponibleDesde: Long? = null): Resultado<Nothing> =
    Resultado.Fallo(ErrorRockola(codigo, mensaje, disponibleDesde?.let(Instant::ofEpochMilli)))

private fun instante(ms: Long): Instant = Instant.ofEpochMilli(ms)

// ── Reloj ────────────────────────────────────────────────────────────────────

internal fun ahoraSim(estado: EstadoSim, ahoraReal: Long): Long {
    val r = estado.reloj
    return floor(r.anclaSim + (ahoraReal - r.anclaReal) * r.velocidad).toLong()
}

private fun tocar(estado: EstadoSim) {
    estado.version++
}

private fun nuevoId(estado: EstadoSim, prefijo: String): String {
    estado.secuencia++
    return "$prefijo-${estado.secuencia}"
}

// ── Consultas internas ───────────────────────────────────────────────────────

private fun cancion(estado: EstadoSim, videoId: String): CancionSim =
    estado.catalogo[videoId]
        ?: CancionSim(videoId, videoId, "", 0, embebible = false, disponible = false, genero = "sin_clasificar")

private fun aDominio(c: CancionSim) = Cancion(c.videoId, c.titulo, c.artista, c.duracionSeg, miniaturaUrl = null)

private fun bloqueoVigente(estado: EstadoSim, videoId: String, ahora: Long): BloqueoSim? =
    estado.bloqueos.firstOrNull { b ->
        b.videoId == videoId && !b.levantado && b.inicio <= ahora && (b.fin == null || b.fin!! > ahora)
    }

private fun deLaSesion(estado: EstadoSim): List<SolicitudSim> {
    val id = estado.sesion?.id ?: return emptyList()
    return estado.solicitudes.filter { it.sesionId == id }
}

/** Pendientes ordenadas por RN-05. */
private fun pendientes(estado: EstadoSim): List<SolicitudSim> =
    deLaSesion(estado)
        .filter { it.estado == EstadoSolicitud.PENDIENTE }
        .sortedWith(compareByDescending<SolicitudSim> { it.votos.size }.thenBy { it.fecha })

/** Cupo usado: las pendientes y la que suena (TBD con el backend, doc 04). */
private fun cupoUsado(estado: EstadoSim, dispositivo: String): Int =
    deLaSesion(estado).count {
        it.dispositivo == dispositivo && (it.estado == EstadoSolicitud.PENDIENTE || it.estado == EstadoSolicitud.REPRODUCIENDO)
    }

private fun posicionSeg(rep: ReproduccionSim, ahora: Long): Double =
    rep.acumuladoSeg + (rep.reanudadaEn?.let { (ahora - it) / 1000.0 } ?: 0.0)

private fun restanteSeg(estado: EstadoSim, ahora: Long): Double {
    val rep = estado.reproduccion ?: return 0.0
    return maxOf(0.0, cancion(estado, rep.videoId).duracionSeg - posicionSeg(rep, ahora))
}

/** Código corto del teléfono («7F3K»). Seudónimo: no deja ver el ID. */
private fun codigoTelefono(estado: EstadoSim, dispositivo: String): String {
    estado.dispositivos[dispositivo]?.codigo?.let { return it }
    val alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    var h = 2166136261L.toInt()
    for (c in dispositivo) h = (h xor c.code) * 16777619
    return (0 until 4).map { i -> alfabeto[(h ushr (i * 5)) and 31] }.joinToString("")
}

// ── Servicio y sesión (RF-003, RF-036 a RF-039, RN-09, RN-10) ────────────────

internal fun evaluarServicio(estado: EstadoSim, ahora: Long): Servicio {
    val zona = estado.bar.zonaHoraria
    val ya = instante(ahora)
    val cierre = cierreDeFranja(estado.horario, ya, zona)
    val proxima = proximaApertura(estado.horario, ya, zona)
    fun ultima(vararg tipos: String) = estado.acciones.lastOrNull { it.tipo in tipos }?.fecha
    val tipo: EstadoServicio
    val desde: Long?
    when {
        estado.mantenimiento -> { tipo = EstadoServicio.MANTENIMIENTO; desde = ultima("mantenimiento") }
        estado.modoQr == ModoQr.PAUSADO -> { tipo = EstadoServicio.EN_PAUSA; desde = ultima("pausar_solicitudes") }
        estado.modoQr == ModoQr.FORZADO_ACTIVO || abiertoPorHorario(estado.horario, ya, zona) -> {
            tipo = EstadoServicio.ABIERTO; desde = estado.sesion?.apertura
        }
        else -> { tipo = EstadoServicio.FUERA_DE_HORARIO; desde = null }
    }
    return Servicio(
        estado = tipo,
        modoQr = estado.modoQr,
        desde = desde?.let(::instante),
        hasta = if (tipo == EstadoServicio.ABIERTO && estado.modoQr != ModoQr.FORZADO_ACTIVO) cierre else null,
        proximaApertura = if (tipo == EstadoServicio.ABIERTO) null else proxima,
    )
}

private fun abrirSesion(estado: EstadoSim, en: Long, origen: String) {
    estado.sesion = SesionSim(nuevoId(estado, "n"), en, "normal", origen)
    estado.acciones += AccionSim("abrir_sesion", en)
}

private fun cerrarSesion(estado: EstadoSim, en: Long) {
    if (estado.sesion == null) return
    for (s in deLaSesion(estado)) if (s.estado == EstadoSolicitud.PENDIENTE) s.estado = EstadoSolicitud.EXPIRADA
    for (b in estado.bloqueos) if (b.duracion == DuracionBloqueo.FIN_SESION && b.fin == null && !b.levantado) b.fin = en
    estado.acciones += AccionSim("cerrar_sesion", en)
    estado.sesion = null
}

// ── Reproducción (RF-019, RF-020, RN-08) ─────────────────────────────────────

private fun terminar(estado: EstadoSim, fin: Long) {
    val rep = estado.reproduccion ?: return
    rep.solicitudId?.let { id ->
        estado.solicitudes.firstOrNull { it.id == id }?.let { s ->
            s.estado = EstadoSolicitud.REPRODUCIDA
            s.fin = fin
        }
    }
    estado.reproduccion = null
}

/** Pseudoaleatorio reproducible a partir de la secuencia (igual que la web). */
private fun aleatorio(estado: EstadoSim): Double {
    estado.secuencia = (estado.secuencia * 1103515245 + 12345) and 0x7fffffff
    return estado.secuencia / 0x7fffffff.toDouble()
}

private fun siguienteDeLista(estado: EstadoSim, en: Long): String? {
    val lista = estado.listas.firstOrNull { it.activa } ?: return null
    if (lista.elementos.isEmpty()) return null
    repeat(lista.elementos.size) {
        val elemento = if (lista.modoRotacion == ModoRotacion.SECUENCIAL) {
            lista.elementos[lista.indice % lista.elementos.size].also {
                lista.indice = (lista.indice + 1) % lista.elementos.size
            }
        } else {
            if (lista.pendientesAleatorios.isEmpty()) lista.pendientesAleatorios = lista.elementos.map { it.elementoId }.toMutableList()
            val i = floor(aleatorio(estado) * lista.pendientesAleatorios.size).toInt()
            val id = lista.pendientesAleatorios.removeAt(i)
            lista.elementos.first { it.elementoId == id }
        }
        if (bloqueoVigente(estado, elemento.videoId, en) == null) {
            elemento.veces++
            elemento.ultima = en
            return elemento.videoId
        }
    }
    return null
}

private fun iniciarSiguiente(estado: EstadoSim, en: Long) {
    val siguiente = pendientes(estado).firstOrNull()
    if (siguiente != null) {
        siguiente.estado = EstadoSolicitud.REPRODUCIENDO
        siguiente.inicio = en
        estado.reproduccion = ReproduccionSim(nuevoId(estado, "r"), siguiente.videoId, OrigenReproduccion.SOLICITUD, siguiente.id, en, 0.0, en)
        return
    }
    estado.reproduccion = siguienteDeLista(estado, en)?.let { video ->
        ReproduccionSim(nuevoId(estado, "r"), video, OrigenReproduccion.LISTA_CASA, null, en, 0.0, en)
    }
}

private fun duracionMs(f: com.example.rokola.dominio.Franja): Long =
    (duracionFranja(f).takeIf { it != 0 } ?: 1440) * 60_000L

/**
 * Lleva el estado hasta [ahora]: abre o cierra la sesión según el horario, termina las
 * canciones que ya acabaron y empieza las siguientes sin silencios. Devuelve si cambió algo.
 */
internal fun avanzar(estado: EstadoSim, ahora: Long): Boolean {
    val antes = estado.version
    val zona = estado.bar.zonaHoraria
    val ya = instante(ahora)

    // Sesión según el horario (RN-09).
    val enHorario = abiertoPorHorario(estado.horario, ya, zona)
    val vetada = estado.sinAperturaAutomaticaHasta?.let { ahora < it } ?: false
    if (!vetada) estado.sinAperturaAutomaticaHasta = null
    if (estado.sesion == null && !estado.mantenimiento && !vetada && (enHorario || estado.modoQr == ModoQr.FORZADO_ACTIVO)) {
        val franja = franjaVigente(estado.horario, ya, zona)
        val cierre = cierreDeFranja(estado.horario, ya, zona)
        val inicio = if (franja != null && cierre != null) maxOf(ahora - DIA_MS, cierre.toEpochMilli() - duracionMs(franja)) else ahora
        abrirSesion(estado, minOf(inicio, ahora), if (enHorario) "horario" else "manual")
        tocar(estado)
    } else if (estado.sesion != null && !enHorario && estado.modoQr != ModoQr.FORZADO_ACTIVO) {
        // Fuera de horario y sin apertura forzada, la noche se cierra.
        cerrarSesion(estado, ahora)
        tocar(estado)
    }

    // Lo que ya terminó, termina; lo siguiente empieza justo a su hora.
    var vueltas = 0
    while (vueltas < 500) {
        val rep = estado.reproduccion ?: break
        val reanudada = rep.reanudadaEn ?: break
        val fin = reanudada + ((cancion(estado, rep.videoId).duracionSeg - rep.acumuladoSeg) * 1000).toLong()
        if (fin > ahora) break
        terminar(estado, fin)
        iniciarSiguiente(estado, fin)
        vueltas++
        tocar(estado)
    }
    if (vueltas >= 500) {
        // Pasaron días sin abrir la simulación: se retoma desde ahora.
        estado.reproduccion?.let { it.inicio = ahora; it.reanudadaEn = ahora; it.acumuladoSeg = 0.0 }
    }
    if (estado.reproduccion == null && (pendientes(estado).isNotEmpty() || estado.listas.any { it.activa && it.elementos.isNotEmpty() })) {
        iniciarSiguiente(estado, ahora)
        if (estado.reproduccion != null) tocar(estado)
    }
    return estado.version != antes
}

// ── Lo que ve el teléfono ────────────────────────────────────────────────────

private fun itemCola(estado: EstadoSim, s: SolicitudSim, puesto: Int, ahora: Long, espera: Double): ItemCola {
    val vigente = s.subida?.takeIf { ahora - it.en < VIGENCIA_SUBIDA_MS }
    return ItemCola(
        solicitudId = s.id,
        cancion = aDominio(cancion(estado, s.videoId)),
        votosTotal = s.votos.size,
        fechaSolicitud = instante(s.fecha),
        puesto = puesto,
        puestoAnterior = vigente?.let { puesto + it.puestos },
        esperaSeg = espera.roundToInt(),
    )
}

/** Espera de cada una (RF-018), como la calcula el backend: lo que le falta a lo que suena más lo que duran las de delante. */
private fun colaConEspera(estado: EstadoSim, ahora: Long): List<ItemCola> {
    var espera = restanteSeg(estado, ahora)
    return pendientes(estado).mapIndexed { i, s ->
        itemCola(estado, s, i + 1, ahora, espera).also { espera += cancion(estado, s.videoId).duracionSeg }
    }
}

internal fun construirAhora(estado: EstadoSim, ahora: Long): Ahora {
    val rep = estado.reproduccion
    val actual = rep?.solicitudId?.let { id -> estado.solicitudes.firstOrNull { it.id == id } }
    val lista = estado.listas.firstOrNull { it.activa }
    return Ahora(
        version = estado.version,
        servidorAhora = instante(ahora),
        servicio = evaluarServicio(estado, ahora),
        reproduccion = rep?.let {
            val c = cancion(estado, it.videoId)
            Reproduccion(
                id = it.id,
                cancion = aDominio(c),
                origen = it.origen,
                solicitudId = it.solicitudId,
                votos = actual?.votos?.size,
                inicio = instante(it.inicio),
                posicionSeg = floor(minOf(posicionSeg(it, ahora), c.duracionSeg.toDouble())).toInt(),
                estado = if (it.reanudadaEn == null) EstadoReproduccion.PAUSADA else EstadoReproduccion.SONANDO,
            )
        },
        cola = colaConEspera(estado, ahora),
        listaCasa = lista?.let { ListaCasaResumen(it.nombre, it.elementos.size) },
    )
}

internal fun construirDestacadas(estado: EstadoSim): List<Destacada> {
    val contexto = mapOf(
        EstadoSolicitud.REPRODUCIENDO to ContextoDestacada.SONANDO,
        EstadoSolicitud.REPRODUCIDA to ContextoDestacada.SONO,
        EstadoSolicitud.PENDIENTE to ContextoDestacada.EN_COLA,
    )
    return deLaSesion(estado)
        .filter { it.estado in contexto }
        .sortedWith(compareByDescending<SolicitudSim> { it.votos.size }.thenBy { it.fecha })
        .take(3)
        .map { s ->
            Destacada(
                cancion = aDominio(cancion(estado, s.videoId)),
                votos = s.votos.size,
                contexto = contexto.getValue(s.estado),
                hora = if (s.estado == EstadoSolicitud.REPRODUCIDA) s.inicio?.let(::instante) else null,
            )
        }
}

internal fun construirYo(estado: EstadoSim, dispositivo: String): Yo {
    val cola = pendientes(estado)
    val propias = deLaSesion(estado).filter { it.dispositivo == dispositivo }.sortedByDescending { it.fecha }
    return Yo(
        pedidosMaximos = estado.bar.pedidosMaximos,
        pedidosRestantes = maxOf(0, estado.bar.pedidosMaximos - cupoUsado(estado, dispositivo)),
        solicitudes = propias.map { s ->
            MiSolicitud(
                solicitudId = s.id,
                cancion = aDominio(cancion(estado, s.videoId)),
                estado = s.estado,
                fechaSolicitud = instante(s.fecha),
                votosTotal = s.votos.size,
                puesto = if (s.estado == EstadoSolicitud.PENDIENTE) cola.indexOfFirst { it.id == s.id } + 1 else null,
                motivo = s.motivo,
            )
        },
        votos = deLaSesion(estado)
            .filter { (it.estado == EstadoSolicitud.PENDIENTE || it.estado == EstadoSolicitud.REPRODUCIENDO) && dispositivo in it.votos }
            .map { it.id },
        avisoVistoEn = estado.dispositivos[dispositivo]?.aviso?.let(::instante),
    )
}

internal fun registrarDispositivo(estado: EstadoSim, dispositivo: String, tipo: String, ahora: Long): Yo {
    if (dispositivo !in estado.dispositivos) {
        estado.dispositivos[dispositivo] = DispositivoSim(codigoTelefono(estado, dispositivo), tipo, ahora, null)
        tocar(estado)
    }
    return construirYo(estado, dispositivo)
}

internal fun anotarAviso(estado: EstadoSim, dispositivo: String, ahora: Long) {
    val d = estado.dispositivos[dispositivo] ?: return
    if (d.aviso == null) {
        d.aviso = ahora
        tocar(estado)
    }
}

// ── Buscar, pedir y votar (RF-005 a RF-012, RN-01 a RN-07) ───────────────────

private val DIACRITICOS = Regex("\\p{Mn}+")
private val NO_ALFANUMERICO = Regex("[^a-z0-9ñ ]+")
private val ESPACIOS = Regex("\\s+")

internal fun normalizar(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(DIACRITICOS, "")
        .lowercase(Locale.ROOT)
        .replace(NO_ALFANUMERICO, " ")
        .replace(ESPACIOS, " ")
        .trim()

private class NoPedible(val motivo: MotivoNoPedible, val desde: Long?)

private fun motivoNoPedible(estado: EstadoSim, c: CancionSim, ahora: Long): NoPedible? {
    if (bloqueoVigente(estado, c.videoId, ahora) != null) return NoPedible(MotivoNoPedible.BLOQUEADA, null)
    if (!c.disponible) return NoPedible(MotivoNoPedible.NO_DISPONIBLE, null)
    if (!c.embebible) return NoPedible(MotivoNoPedible.NO_EMBEBIBLE, null)
    if (c.duracionSeg > estado.bar.duracionMaxSeg) return NoPedible(MotivoNoPedible.MUY_LARGA, null)
    val ventana = estado.bar.ventanaNoRepeticionMin * 60_000L
    val sono = deLaSesion(estado)
        .filter {
            it.videoId == c.videoId && it.inicio != null &&
                it.estado in setOf(EstadoSolicitud.REPRODUCIENDO, EstadoSolicitud.REPRODUCIDA, EstadoSolicitud.SALTADA)
        }
        .maxOfOrNull { it.inicio!! }
    if (sono != null && ahora - sono < ventana) return NoPedible(MotivoNoPedible.REPRODUCIDA_HACE_POCO, sono + ventana)
    return null
}

private fun MotivoNoPedible.codigo(): CodigoError = when (this) {
    MotivoNoPedible.BLOQUEADA -> CodigoError.BLOQUEADA
    MotivoNoPedible.NO_DISPONIBLE -> CodigoError.NO_DISPONIBLE
    MotivoNoPedible.NO_EMBEBIBLE -> CodigoError.NO_EMBEBIBLE
    MotivoNoPedible.MUY_LARGA -> CodigoError.MUY_LARGA
    MotivoNoPedible.REPRODUCIDA_HACE_POCO -> CodigoError.REPRODUCIDA_HACE_POCO
}

/**
 * Veces pedida en el bar en las últimas cuatro semanas. La web suma además las noches
 * cerradas del tablero analítico; aquí solo cuentan las solicitudes del estado.
 */
internal fun pedidasEnElBar(estado: EstadoSim, ahora: Long): (String) -> Int {
    val desde = ahora - 28 * DIA_MS
    val cuenta = estado.solicitudes.filter { it.fecha >= desde }.groupingBy { it.videoId }.eachCount()
    return { videoId -> cuenta[videoId] ?: 0 }
}

internal fun buscar(estado: EstadoSim, consulta: String, ahora: Long): Resultado<Busqueda> {
    if (estado.falloYoutube == FalloYoutube.CAIDO) return mal(CodigoError.YOUTUBE_NO_DISPONIBLE, "YouTube no respondió")
    val tokens = normalizar(consulta).split(" ").filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return mal(CodigoError.DATOS_INVALIDOS, "Consulta vacía")
    fun coincide(c: CancionSim): Boolean {
        val texto = normalizar("${c.titulo} ${c.artista}")
        return tokens.all { it in texto }
    }
    val popularidad = pedidasEnElBar(estado, ahora)
    val cola = construirAhora(estado, ahora).cola
    val enCola = cola.filter { coincide(cancion(estado, it.cancion.videoId)) }
    val idsEnCola = cola.map { it.cancion.videoId }.toSet()
    val primero = tokens.first()
    val resultados = estado.catalogo.values
        .filter { coincide(it) && it.videoId !in idsEnCola }
        .sortedByDescending { normalizar(it.titulo).startsWith(primero) }
        .take(8)
        .map { c ->
            val no = motivoNoPedible(estado, c, ahora)
            ResultadoBusqueda(
                cancion = aDominio(c),
                pedible = no == null,
                motivo = no?.motivo,
                disponibleDesde = no?.desde?.let(::instante),
                genero = c.genero.takeIf { it != "sin_clasificar" },
                pedidasEnElBar = popularidad(c.videoId),
            )
        }
    return bien(Busqueda(consulta, enCola, resultados, desdeCache = estado.falloYoutube == FalloYoutube.CUOTA))
}

/** Reordena la cola alrededor de un cambio de votos y anota quién subió. */
private fun conSubidas(estado: EstadoSim, ahora: Long, cambio: () -> Unit) {
    val antes = pendientes(estado).map { it.id }
    cambio()
    pendientes(estado).forEachIndexed { nuevo, s ->
        val viejo = antes.indexOf(s.id)
        if (viejo == -1 || viejo == nuevo) return@forEachIndexed
        s.subida = if (nuevo < viejo) Subida(viejo - nuevo, ahora) else null
    }
}

private fun aceptaPedidos(estado: EstadoSim, ahora: Long): Boolean =
    evaluarServicio(estado, ahora).estado == EstadoServicio.ABIERTO && estado.sesion != null

private fun esperaDe(estado: EstadoSim, solicitudId: String, ahora: Long): Int {
    var total = restanteSeg(estado, ahora)
    for (s in pendientes(estado)) {
        if (s.id == solicitudId) break
        total += cancion(estado, s.videoId).duracionSeg
    }
    return total.roundToInt()
}

internal fun pedirCancion(estado: EstadoSim, dispositivo: String, videoId: String, ahora: Long): Resultado<RespuestaSolicitud> {
    if (!aceptaPedidos(estado, ahora)) return mal(CodigoError.SERVICIO_NO_DISPONIBLE, "Ahora no se reciben canciones")
    val c = estado.catalogo[videoId] ?: return mal(CodigoError.NO_DISPONIBLE, "El video no existe o no está disponible")
    pendientes(estado).firstOrNull { it.videoId == videoId }?.let { yaEnCola ->
        // RN-02: pedir una que ya está pendiente es votar por ella.
        val voto = votar(estado, dispositivo, yaEnCola.id, ahora)
        if (voto is Resultado.Fallo) return voto
        val puesto = pendientes(estado).indexOfFirst { it.id == yaEnCola.id } + 1
        return bien(
            RespuestaSolicitud(TipoRespuestaSolicitud.VOTADA, yaEnCola.id, puesto, esperaDe(estado, yaEnCola.id, ahora), construirYo(estado, dispositivo).pedidosRestantes),
        )
    }
    motivoNoPedible(estado, c, ahora)?.let { return mal(it.motivo.codigo(), "No se puede pedir", it.desde) }
    if (cupoUsado(estado, dispositivo) >= estado.bar.pedidosMaximos) {
        return mal(CodigoError.LIMITE_PEDIDOS, "Ya tienes todos tus pedidos en la cola")
    }
    val s = SolicitudSim(nuevoId(estado, "s"), estado.sesion!!.id, videoId, dispositivo, ahora, mutableListOf(dispositivo))
    conSubidas(estado, ahora) { estado.solicitudes += s }
    tocar(estado)
    avanzar(estado, ahora) // si no sonaba nada, empieza ya
    val puesto = pendientes(estado).indexOfFirst { it.id == s.id } + 1
    return bien(
        RespuestaSolicitud(TipoRespuestaSolicitud.CREADA, s.id, maxOf(1, puesto), esperaDe(estado, s.id, ahora), construirYo(estado, dispositivo).pedidosRestantes),
    )
}

internal fun votar(estado: EstadoSim, dispositivo: String, solicitudId: String, ahora: Long): Resultado<RespuestaVoto> {
    if (!aceptaPedidos(estado, ahora)) return mal(CodigoError.SERVICIO_NO_DISPONIBLE, "Ahora no se puede votar")
    val s = deLaSesion(estado).firstOrNull { it.id == solicitudId }
    if (s == null || s.estado != EstadoSolicitud.PENDIENTE) return mal(CodigoError.NO_PENDIENTE, "Esa canción ya no está en la cola")
    if (dispositivo in s.votos) return mal(CodigoError.YA_VOTASTE, "Ya votaste por esta canción")
    conSubidas(estado, ahora) { s.votos += dispositivo }
    tocar(estado)
    return bien(RespuestaVoto(s.id, s.votos.size))
}

internal fun moverVoto(estado: EstadoSim, dispositivo: String, desde: String, hacia: String, ahora: Long): Resultado<RespuestaVoto> {
    if (!aceptaPedidos(estado, ahora)) return mal(CodigoError.SERVICIO_NO_DISPONIBLE, "Ahora no se puede votar")
    val origen = deLaSesion(estado).firstOrNull { it.id == desde }
    val destino = deLaSesion(estado).firstOrNull { it.id == hacia }
    if (origen == null || destino == null || origen.estado != EstadoSolicitud.PENDIENTE || destino.estado != EstadoSolicitud.PENDIENTE) {
        return mal(CodigoError.NO_PENDIENTE, "Esa canción ya no está en la cola")
    }
    if (dispositivo !in origen.votos) return mal(CodigoError.DATOS_INVALIDOS, "No habías votado por esa canción")
    if (origen.dispositivo == dispositivo) return mal(CodigoError.DATOS_INVALIDOS, "El voto de tu propia canción no se mueve")
    if (dispositivo in destino.votos) return mal(CodigoError.YA_VOTASTE, "Ya votaste por esa canción")
    conSubidas(estado, ahora) {
        origen.votos.remove(dispositivo)
        destino.votos += dispositivo
    }
    tocar(estado)
    return bien(RespuestaVoto(destino.id, destino.votos.size))
}
