package com.example.rokola.ui.textos

import com.example.rokola.dominio.EstadoServicio
import com.example.rokola.dominio.EstadoSolicitud
import com.example.rokola.dominio.MotivoNoPedible
import java.time.Instant
import java.time.ZoneId

/*
 * Textos del cliente, iguales a C:\web-musica\apps\web\lib\textos\cliente.ts (doc 05,
 * «Textos»). Vocabulario fijo: «pedido» y «pedir» (no «solicitud»), «voto», «cola», «lista de
 * la casa». Los errores dicen qué pasó y qué hacer; no se disculpan ni muestran códigos.
 */

object T {
    // La web dice «Sin registro ni descargas»; en la app nativa sí hubo descarga (versión B).
    const val PRIVACIDAD_BREVE = "Sin registro ni cuenta: tu teléfono queda como un código anónimo."
    const val POLITICA = "Política de datos"
    const val ENTRAR = "Entrar y votar"
    const val EN_VIVO = "En vivo"
    const val LO_MAS_VOTADO = "Lo más votado de la noche"
    const val SIN_VOTOS_AUN = "Nadie ha votado todavía esta noche."
    const val SUENA_AHORA = "Suena ahora"
    const val NADA_SUENA = "No suena nada ahora"
    const val LISTA_DE_LA_CASA = "Lista de la casa"
    const val PIDE_UNA_CANCION = "Pide una canción"
    const val LA_COLA = "La cola"
    const val CARGANDO = "Cargando la rockola…"
    const val BUSCAR_ETIQUETA = "Buscar en YouTube"
    const val BUSCAR_MARCADOR = "Canción o artista"
    const val BORRAR_BUSQUEDA = "Borrar búsqueda"
    const val BUSCAR_VACIO = "Escribe el nombre de una canción o de un artista."
    const val YA_EN_LA_COLA = "Ya está en la cola"
    const val VOTA_EN_VEZ = "Vota en vez de pedirla"
    const val RESULTADOS_YOUTUBE = "Resultados en YouTube"
    const val VER_COLA = "Ver cola"
    const val REINTENTAR = "Reintentar"
    const val CERRAR = "Cerrar"
    const val VER_QUE_SUENA = "Ver qué está sonando"
    const val TU_CANCION_VA_DE = "Tu canción va de"
    const val TU_CANCION_SUENA = "Tu canción está sonando"
    const val TUS_PEDIDOS = "Tus pedidos de esta noche"
    const val PEDIR_UNA = "Pedir una canción"
    const val NADA_SUENA_META = "Pide una canción y será la primera en sonar."
    const val HORARIO_DE_PEDIDOS = "Horario de pedidos"
    const val ESPERAS_ESTIMADAS = "Las esperas son aproximadas: cambian con los votos."
}

data class TituloMeta(val titulo: String, val meta: String)

fun cupo(restantes: Int, maximo: Int): String = when {
    restantes <= 0 -> "Usaste tus $maximo pedidos"
    restantes == 1 -> "Te queda 1 de $maximo pedidos"
    else -> "Te quedan $restantes de $maximo pedidos"
}

fun sinCupo(maximo: Int) = TituloMeta(
    "Ya tienes $maximo canciones en la cola",
    "Cuando suene una de las tuyas podrás pedir otra. Mientras tanto, vota.",
)

/** Motivo corto, en la columna de la tecla (76 dp). */
fun motivoCorto(motivo: MotivoNoPedible): String = when (motivo) {
    MotivoNoPedible.BLOQUEADA -> "El bar la bloqueó"
    MotivoNoPedible.NO_DISPONIBLE -> "No está disponible"
    MotivoNoPedible.NO_EMBEBIBLE -> "No se puede poner aquí"
    MotivoNoPedible.MUY_LARGA -> "Dura más de 10 min"
    MotivoNoPedible.REPRODUCIDA_HACE_POCO -> "Sonó hace poco"
}

const val MOTIVO_SIN_CUPO = "Sin pedidos libres"

/** Motivo largo, cuando el rechazo llega al pulsar «Pedir». */
fun motivoLargo(motivo: MotivoNoPedible, disponibleDesde: Instant?, zona: ZoneId): String = when (motivo) {
    MotivoNoPedible.BLOQUEADA -> "el bar la bloqueó."
    MotivoNoPedible.NO_DISPONIBLE -> "el video ya no está disponible en YouTube."
    MotivoNoPedible.NO_EMBEBIBLE -> "YouTube no deja reproducirla fuera de su página."
    MotivoNoPedible.MUY_LARGA -> "dura más de 10 minutos."
    MotivoNoPedible.REPRODUCIDA_HACE_POCO ->
        if (disponibleDesde != null) "sonó hace poco; podrás pedirla desde las ${formatoHora(disponibleDesde, zona)}." else "sonó hace poco."
}

object TextosBusqueda {
    fun sinResultados(q: String) = TituloMeta("No encontramos «$q»", "Revisa cómo se escribe o busca por el nombre del artista.")
    val youtubeCaido = TituloMeta("YouTube no respondió", "La búsqueda falló, pero la música sigue sonando.")
    val sinRed = TituloMeta("No hay conexión", "Revisa tu señal o el wifi del bar e inténtalo de nuevo.")
    const val DESDE_CACHE = "YouTube no responde ahora: estos resultados son de búsquedas anteriores y pueden estar incompletos."
}

/** Filtros de la búsqueda (RF-055, PB-51). */
object TextosFiltros {
    fun boton(n: Int) = if (n > 0) "Filtrar ($n)" else "Filtrar"
    const val GENERO = "Género"
    const val TODOS = "Todos"
    const val DURACION = "Duración"
    const val CUALQUIERA = "Cualquiera"
    const val CORTA = "Hasta 4 min"
    const val MEDIA = "De 4 a 6 min"
    const val LARGA = "Más de 6 min"
    const val POPULARIDAD = "Popularidad"
    const val TODAS = "Todas"
    const val PEDIDAS = "Ya pedidas en este bar"
    const val MUY_PEDIDAS = "Muy pedidas en este bar"
    const val QUITAR = "Quitar filtros"
    const val NINGUNO = "Ningún resultado con estos filtros."
}

private val GENEROS = mapOf("salsa" to "Salsa", "urbano" to "Urbano", "pop" to "Pop", "tropical" to "Tropical", "vallenato" to "Vallenato")

/** `salsa` → «Salsa»; null → «Sin clasificar». */
fun nombreGenero(g: String?): String = if (g == null) "Sin clasificar" else GENEROS[g] ?: capitalizar(g)

object TextosPedido {
    fun entro(titulo: String, puesto: Int, esperaSeg: Int) = TituloMeta("$titulo entró a la cola", "Puesto #$puesto, suena ${formatoEspera(esperaSeg.toDouble())}")
    fun eraVoto(titulo: String, puesto: Int) = TituloMeta("$titulo ya estaba en la cola", "Le sumaste tu voto: va de #$puesto.")
    fun rechazado(titulo: String, motivo: String) = TituloMeta("No se puede pedir $titulo", capitalizar(motivo))
    val cerrado = TituloMeta("Los pedidos se acaban de cerrar", "Puedes seguir viendo la cola.")
    fun fallo(titulo: String) = TituloMeta("No pudimos pedir $titulo", "Revisa tu conexión e inténtalo de nuevo.")
}

object TextosVoto {
    fun fallo(titulo: String) = TituloMeta("No se registró tu voto por $titulo", "Revisa tu conexión e inténtalo de nuevo.")
    fun yaVotaste(titulo: String) = TituloMeta("Ya habías votado por $titulo", "Cada teléfono vota una vez por canción.")
    fun yaNoEsta(titulo: String) = TituloMeta("$titulo ya no está en la cola", "Acaba de sonar o el bar la retiró.")
    val cerrado = TituloMeta("Ahora no se puede votar", "Los pedidos y los votos están cerrados.")
    fun etiqueta(titulo: String) = "Votar por $titulo"
    fun votaste(titulo: String) = "Ya votaste por $titulo"
    fun mover(titulo: String) = "Votaste por $titulo. Mover tu voto"
    fun moverTitulo(titulo: String) = "Mover tu voto de $titulo"
    const val MOVER_TEXTO = "Elige a qué canción de la cola pasa. La cola se reordena al instante."
    fun moverA(titulo: String) = "Mover tu voto a $titulo"
    const val SIN_DESTINO = "No hay otra canción en la cola por la que no hayas votado."
    fun movido(desde: String, hacia: String) = TituloMeta("Tu voto pasó a $hacia", "$desde tiene un voto menos.")
    fun noMovido(titulo: String) = TituloMeta("No se movió tu voto de $titulo", "Revisa tu conexión e inténtalo de nuevo.")
    fun tuya(titulo: String) = "$titulo la pediste tú"
}

fun estadoPedido(estado: EstadoSolicitud, puesto: Int?): String = when (estado) {
    EstadoSolicitud.PENDIENTE -> if (puesto != null) "En espera, puesto #$puesto" else "En espera"
    EstadoSolicitud.REPRODUCIENDO -> "Sonando ahora"
    EstadoSolicitud.REPRODUCIDA -> "Ya sonó"
    EstadoSolicitud.SALTADA -> "Omitida por el bar"
    EstadoSolicitud.NO_DISPONIBLE -> "No se pudo reproducir"
    EstadoSolicitud.BLOQUEADA -> "Bloqueada por el bar"
    EstadoSolicitud.RETIRADA -> "Retirada por el bar"
    EstadoSolicitud.EXPIRADA -> "La noche cerró antes de que sonara"
}

object ColaVacia {
    val conLista = TituloMeta("La cola está vacía.", "Mientras tanto suena la lista de la casa. Pide una canción y será la siguiente.")
    val sinLista = TituloMeta("La cola está vacía.", "Pide una canción y será la primera en sonar.")
}

const val REGLA_DESEMPATE = "Con los mismos votos, suena primero la que se pidió antes."

/** El texto de la luz según el estado del servicio. */
fun luzServicio(estado: EstadoServicio): String = when (estado) {
    EstadoServicio.ABIERTO -> "En vivo"
    EstadoServicio.FUERA_DE_HORARIO -> "Pedidos cerrados"
    EstadoServicio.EN_PAUSA -> "En pausa"
    EstadoServicio.MANTENIMIENTO -> "En mantenimiento"
}

/** Aviso corto encima de la cola cuando no se puede votar. */
fun colaCerrada(estado: EstadoServicio): String = when (estado) {
    EstadoServicio.EN_PAUSA -> "Los pedidos están en pausa: la cola sigue sonando, pero por ahora no se puede votar."
    EstadoServicio.MANTENIMIENTO -> "La rockola está en mantenimiento: por ahora no se puede pedir ni votar."
    else -> "Los pedidos están cerrados: puedes ver lo que suena, pero no votar."
}

val falloCarga = TituloMeta("La rockola no cargó", "Revisa tu conexión e inténtalo de nuevo.")

data class TextosCerrado(val luz: String, val titulo: String, val detalle: String)

/** Lo que ve el cliente cuando no se reciben canciones (RF-003, PB-30). */
fun servicioCerrado(estado: EstadoServicio, proximaApertura: Instant?, zona: ZoneId, ahora: Instant): TextosCerrado = when (estado) {
    EstadoServicio.EN_PAUSA -> TextosCerrado(luzServicio(estado), "Los pedidos están en pausa", "El bar los pausó por un momento. La cola sigue sonando y puedes verla.")
    EstadoServicio.MANTENIMIENTO -> TextosCerrado(luzServicio(estado), "La rockola está en mantenimiento", "El bar está preparando la próxima noche. Vuelve a escanear el QR cuando abran.")
    else -> {
        val cuando = proximaApertura?.let { "${diaRelativo(it, ahora, zona)} a las ${formatoHora(it, zona)}" }
        TextosCerrado(
            luzServicio(estado),
            "Por ahora no recibimos canciones",
            if (cuando != null) "${conPunto("Los pedidos abren $cuando")} Mientras tanto suena la lista de la casa." else "Mientras tanto suena la lista de la casa.",
        )
    }
}

object Conexion {
    const val LUZ = "Sin conexión"
    const val DESCONECTADO = "Sin conexión en vivo: lo que ves puede estar desactualizado."
}

/**
 * Entrada por QR (apps/web/lib/textos/cliente.ts, `qr`, y components/cliente/Estados.tsx).
 * Diferencias a propósito: la app se instala (no dice «sin descargas»), habla de «enlace» en
 * vez de «dirección» y, como tiene tecla para reintentar, el fallo de red no pide escanear.
 */
object TextosEntrada {
    const val ABRIENDO = "Abriendo la rockola del bar…"
    val sinBar = TituloMeta("Escanea el QR de tu mesa", "La rockola de cada bar se abre desde su código QR. Escanéalo con la cámara del teléfono.")
    val reemplazado = TituloMeta("Este QR ya no está en uso", "El bar cambió su código. Escanea el QR que está ahora en tu mesa.")
    val noEncontrado = TituloMeta("Aquí no hay ninguna rockola", "El enlace no lleva a ningún bar o el QR ya no está en uso. Escanea el QR que está en tu mesa.")
    val fallo = TituloMeta("No se pudo abrir la rockola", "Revisa tu conexión e inténtalo de nuevo.")
    const val VOLVER = "Volver a la rockola"
}
