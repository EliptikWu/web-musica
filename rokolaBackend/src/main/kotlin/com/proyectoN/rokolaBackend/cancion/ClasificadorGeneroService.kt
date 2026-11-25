package com.proyectoN.rokolaBackend.cancion

import org.springframework.stereotype.Service
import java.text.Normalizer
import java.util.Locale

/**
 * Clasificación conservadora de videos musicales mediante etiquetas de YouTube.
 * - Acepta alias exactos, hashtags y expresiones dentro de etiquetas extensas.
 * - Exige límites de palabra; no busca subcadenas dentro de otras palabras.
 * - Prefiere un subgénero a su categoría paraguas cuando ambos aparecen.
 * - Ante géneros contradictorios sin un predominio claro, devuelve null.
 *
 * No utiliza nombres de artistas ni inventa un género cuando YouTube no envía tags.
 * Mantiene las firmas generos(), detectar() y canonico() usadas por el backend.
 */
@Service
class ClasificadorGeneroService {

    private val diccionario: Map<String, List<String>> = linkedMapOf(
        // Géneros originales: mantener estos nombres evita dividir las estadísticas existentes.
        "Salsa" to listOf(
            "salsa", "salsa romantica", "salsa dura", "salsa music", "salsa colombiana",
            "salsa caleña", "salsa calena", "salsa cubana", "salsa choke", "salsa brava",
            "salsa clasica", "salsa vieja", "salsa baul", "salsa baúl", "salsa tropical",
            "salsa sensual", "salsa erotica", "salsas romanticas"
        ),
        "Urbano" to listOf(
            "reggaeton", "reguetón", "regueton", "reggaetón", "reggaeton latino",
            "reggaeton colombiano", "reggaeton romantico", "reggaeton viejo",
            "reggaeton old school", "perreo", "perreo intenso", "musica urbana",
            "urbano latino", "urban latino", "latin urban", "trap latino", "latin trap",
            "trap en espanol", "trap en español", "trap music", "neoperreo",
            "reggaeton music", "trap colombiano", "reggaetones"
        ),
        "Pop" to listOf(
            "pop", "pop latino", "latin pop", "pop music", "pop en espanol",
            "pop en español", "spanish pop", "pop romantico", "pop rock", "electropop",
            "dance pop", "synthpop", "synth pop", "indie pop"
        ),
        "Tropical" to listOf(
            "musica tropical", "tropical music", "ritmos tropicales", "tropical latino"
        ),
        "Vallenato" to listOf(
            "vallenato", "vallenato romantico", "vallenato music", "vallenato colombiano",
            "vallenato clasico", "vallenato nuevo", "vallenato tradicional",
            "vallenato de la nueva ola", "paseo vallenato", "merengue vallenato",
            "vallenatos", "vallenatos romanticos", "vallenatos clasicos"
        ),
        "Bachata" to listOf(
            "bachata", "bachata romantica", "bachata music", "bachata moderna",
            "bachata dominicana", "bachata sensual", "bachata urbana", "bachata pop",
            "bachatas", "bachatas romanticas"
        ),
        "Rock" to listOf(
            "rock", "rock en espanol", "rock en español", "latin rock", "rock music",
            "rock latino", "rock clasico", "classic rock", "hard rock", "soft rock",
            "rock alternativo", "rock colombiano", "rock and roll", "rock n roll"
        ),
        "Electrónica" to listOf(
            "electronica", "electrónica", "musica electronica", "electronic music",
            "electronic dance music", "edm", "electro dance", "dance electronico",
            "electronica latina"
        ),
        "Merengue" to listOf(
            "merengue", "merengue music", "merengue dominicano", "merengue clasico",
            "merengue de calle", "merengue urbano", "merengue mambo", "merengues"
        ),
        "Cumbia" to listOf(
            "cumbia", "cumbia colombiana", "cumbia music", "cumbia argentina",
            "cumbia villera", "cumbia mexicana", "cumbia sonidera", "cumbia peruana",
            "cumbia tropical", "cumbia romantica", "cumbia rebajada", "tecnocumbia",
            "cumbias", "cumbia sabanera"
        ),

        // Más estilos latinoamericanos.
        "Champeta" to listOf("champeta", "champeta urbana", "champeta africana", "champeta cartagenera", "champeta criolla"),
        "Porro" to listOf("porro", "porro colombiano", "porro sabanero", "porro pelayero"),
        "Música Popular" to listOf(
            "musica popular colombiana", "popular colombiana", "carrilera",
            "musica de despecho", "canciones de despecho", "guascarrilera",
            "despecho", "musica de cantina", "musica guasca"
        ),
        "Música Llanera" to listOf(
            "musica llanera", "llanera", "joropo", "joropo llanero", "pasaje llanero", "folclor llanero"
        ),
        "Música Andina" to listOf(
            "musica andina", "folclor andino", "huayno", "huaino", "sanjuanito",
            "bambuco", "pasillo colombiano", "musica andina colombiana"
        ),
        "Folclor Colombiano" to listOf(
            "folclor colombiano", "folklore colombiano", "musica folclorica colombiana",
            "mapale", "mapalé", "currulao", "bullerengue", "tambora colombiana"
        ),
        "Bolero" to listOf("bolero", "boleros", "bolero romantico", "bolero cubano", "bolero clasico"),
        "Balada" to listOf(
            "balada", "balada romantica", "baladas romanticas", "balada en espanol",
            "balada pop", "balada latina", "power ballad", "baladas"
        ),
        "Tango" to listOf("tango", "tango argentino", "tango music", "tango tradicional", "tango electronico", "tangos"),
        "Samba" to listOf("samba", "samba brasileira", "samba brasilena", "samba music", "pagode brasileiro"),
        "Bossa Nova" to listOf("bossa nova", "bossanova", "bossa nova brasileira", "brazilian bossa"),
        "Forró" to listOf("forro", "forró", "forro brasileiro", "forro pe de serra", "forró pé de serra"),
        "Zouk" to listOf("zouk", "zouk music", "zouk caribeño", "brazilian zouk"),
        "Kizomba" to listOf("kizomba", "kizomba music", "kizomba angolana"),

        // Música regional mexicana.
        "Regional Mexicano" to listOf(
            "regional mexicano", "musica regional mexicana", "regional mexican music"
        ),
        "Banda" to listOf("banda sinaloense", "banda mexicana", "musica de banda", "banda sinaloense music"),
        "Norteño" to listOf("norteno", "norteño", "musica nortena", "musica norteña", "norteno banda"),
        "Corridos" to listOf(
            "corridos", "corrido mexicano", "corridos tumbados", "corridos belicos",
            "corridos bélicos", "corridos alterados", "corrido tumbado"
        ),
        "Ranchera" to listOf("ranchera", "rancheras", "musica ranchera", "mariachi", "mariachi mexicano"),
        "Grupera" to listOf("grupera", "musica grupera", "grupero", "grupera romantica"),

        // Estilos internacionales.
        "Reggae" to listOf("reggae", "reggae music", "roots reggae", "reggae latino", "reggae en espanol"),
        "Dancehall" to listOf("dancehall", "dancehall music", "dancehall jamaicano", "latin dancehall"),
        "Hip Hop" to listOf(
            "hip hop", "hiphop", "hip hop music", "rap", "rap music", "rap latino",
            "rap colombiano", "rap en espanol", "rap en español", "boom bap",
            "freestyle rap", "gangsta rap", "old school rap"
        ),
        "R&B" to listOf("r&b", "rnb", "r and b", "rhythm and blues", "contemporary rnb", "neo soul"),
        "Soul" to listOf("soul", "soul music", "classic soul", "motown soul"),
        "Funk" to listOf("funk", "funk music", "funk brasileiro", "funk carioca", "funk brasileno"),
        "Disco" to listOf("disco music", "disco funk", "disco clasico", "italo disco", "nu disco"),
        "Jazz" to listOf("jazz", "jazz music", "latin jazz", "smooth jazz", "jazz fusion", "bebop", "swing jazz"),
        "Blues" to listOf("blues", "blues music", "delta blues", "electric blues", "chicago blues"),
        "Country" to listOf("country", "country music", "country pop", "country rock", "bluegrass"),
        "Folk" to listOf("folk", "folk music", "folk latino", "acoustic folk"),
        "Afrobeats" to listOf("afrobeats", "afrobeat", "afro beats", "afro pop", "afropop", "afro fusion", "afrofusion"),
        "Amapiano" to listOf("amapiano", "amapiano music", "south african amapiano"),
        "K-Pop" to listOf("kpop", "k pop", "korean pop", "korean pop music"),
        "J-Pop" to listOf("jpop", "j pop", "japanese pop", "japanese pop music"),
        "Metal" to listOf(
            "metal", "heavy metal", "metal music", "thrash metal", "death metal",
            "black metal", "power metal", "metalcore", "nu metal"
        ),
        "Punk" to listOf("punk", "punk rock", "pop punk", "punk music", "hardcore punk", "post punk"),
        "Indie" to listOf("indie", "indie music", "indie rock", "indie alternative", "indie folk"),
        "Góspel" to listOf("gospel", "góspel", "gospel music", "gospel choir", "black gospel"),
        "Música Cristiana" to listOf(
            "musica cristiana", "christian music", "christian pop", "christian rock",
            "alabanza", "musica de adoracion", "worship music", "praise and worship"
        ),
        "Música Clásica" to listOf(
            "musica clasica", "classical music", "classical piano", "musica sinfonica",
            "orquesta sinfonica", "baroque music", "opera clasica"
        ),
        "Instrumental" to listOf(
            "musica instrumental", "instrumental music", "instrumental piano",
            "instrumental guitar", "solo instrumental"
        ),
        "Lo-Fi" to listOf("lofi", "lo fi", "lo fi hip hop", "lofi beats", "lofi chill", "chillhop"),
        "Phonk" to listOf("phonk", "drift phonk", "brazilian phonk", "phonk music"),
        "House" to listOf("house music", "deep house", "tech house", "progressive house", "afro house"),
        "Techno" to listOf("techno", "techno music", "hard techno", "melodic techno", "minimal techno"),
        "Trance" to listOf("trance music", "psytrance", "progressive trance", "uplifting trance"),
        "Dubstep" to listOf("dubstep", "dubstep music", "brostep", "melodic dubstep"),
        "Drum and Bass" to listOf("drum and bass", "drum n bass", "dnb music", "liquid dnb", "jungle dnb"),
        "Dembow" to listOf("dembow", "dembow dominicano", "dembow music", "dembow urbano"),
        "Flamenco" to listOf("flamenco", "flamenco music", "flamenco pop", "rumba flamenca"),
    )

    // Estos son paraguas musicales; si se identifica un estilo más concreto,
    // no se considera una contradicción que también aparezca el paraguas.
    private val subgenerosPorCategoria: Map<String, Set<String>> = mapOf(
        "Tropical" to setOf("Salsa", "Merengue", "Cumbia", "Vallenato", "Bachata", "Champeta", "Porro"),
        "Electrónica" to setOf("House", "Techno", "Trance", "Dubstep", "Drum and Bass", "Amapiano"),
        "Regional Mexicano" to setOf("Banda", "Norteño", "Corridos", "Ranchera", "Grupera"),
        "Pop" to setOf("K-Pop", "J-Pop"),
        "Rock" to setOf("Metal", "Punk"),
    )

    private val separacionCamel = Regex("(?<=[a-záéíóúüñ])(?=[A-ZÁÉÍÓÚÜÑ])")
    private val separacionLetrasNumeros = Regex("(?<=[a-z])(?=[0-9])|(?<=[0-9])(?=[a-z])")
    private val diacriticos = Regex("\\p{M}+")
    private val simbolos = Regex("[^a-z0-9]+")
    private val espacios = Regex("\\s+")

    private val indice: Map<String, String> = buildMap {
        for ((genero, alias) in diccionario) {
            for (valor in alias) {
                val normalizado = normalizar(valor)
                val variantes = if (' ' in normalizado) {
                    listOf(normalizado, normalizado.replace(" ", ""))
                } else {
                    listOf(normalizado)
                }
                for (variante in variantes) {
                    if (variante.isBlank()) continue
                    val anterior = putIfAbsent(variante, genero)
                    require(anterior == null || anterior == genero) {
                        "Alias duplicado '$variante' para $anterior y $genero"
                    }
                }
            }
        }
    }

    /** Catálogo permitido para los endpoints administrativos actuales. */
    fun generos(): List<String> = diccionario.keys.toList()

    /**
     * Clasifica si encuentra un género o un predominio claro de al menos dos etiquetas.
     * Por seguridad, ante señales realmente contradictorias devuelve null.
     */
    fun detectar(etiquetas: List<String>): String? {
        val puntuacion = mutableMapOf<String, Int>()
        val etiquetasPorGenero = mutableMapOf<String, Int>()

        for (etiqueta in etiquetas.map(::normalizar).filter { it.isNotBlank() }.distinct()) {
            val coincidencias = indice.mapNotNull { (alias, genero) ->
                val exacta = etiqueta == alias
                if (exacta || (" $etiqueta ").contains(" $alias ")) {
                    Triple(alias, genero, if (exacta) 5 else 2)
                } else null
            }

            // «pop punk» no debe sumar un voto para Pop solo por contener «pop»;
            // la expresión más específica del mismo fragmento tiene prioridad.
            val masEspecificas = coincidencias.filter { (alias, genero, _) ->
                coincidencias.none { (otroAlias, otroGenero, _) ->
                    otroGenero != genero && otroAlias.length > alias.length &&
                        (" $otroAlias ").contains(" $alias ")
                }
            }
            val puntajeEnEtiqueta = mutableMapOf<String, Int>()
            for ((_, genero, puntos) in masEspecificas) {
                puntajeEnEtiqueta[genero] = maxOf(puntajeEnEtiqueta[genero] ?: 0, puntos)
            }
            for ((genero, puntos) in puntajeEnEtiqueta) {
                puntuacion[genero] = (puntuacion[genero] ?: 0) + puntos
                etiquetasPorGenero[genero] = (etiquetasPorGenero[genero] ?: 0) + 1
            }
        }

        if (puntuacion.isEmpty()) return null

        val generosDetectados = puntuacion.keys
        val candidatos = puntuacion.filterKeys { genero ->
            subgenerosPorCategoria[genero]?.none { it in generosDetectados } != false
        }
        if (candidatos.size == 1) return candidatos.keys.first()

        val ordenados = candidatos.entries.sortedByDescending { it.value }
        val primero = ordenados[0]
        val segundo = ordenados[1]
        val respaldos = etiquetasPorGenero[primero.key] ?: 0

        return if (
            respaldos >= 2 &&
            primero.value >= segundo.value * 2 &&
            primero.value - segundo.value >= 4
        ) primero.key else null
    }

    /** Resultado con procedencia auditable; nunca debe sobrescribir una corrección MANUAL. */
    data class Clasificacion(val genero: String, val origen: String)

    /**
     * Fuentes por prioridad: etiquetas > título > descripción.
     * En título y descripción exigimos indicios explícitos de género: evitamos
     * interpretar cualquier palabra de una letra, letra de canción o publicidad.
     */
    fun clasificar(etiquetas: List<String>, titulo: String, descripcion: String?): Clasificacion? {
        detectar(etiquetas)?.let { return Clasificacion(it, "ETIQUETAS") }
        detectarEnTitulo(titulo)?.let { return Clasificacion(it, "TITULO") }
        detectarEnDescripcion(descripcion)?.let { return Clasificacion(it, "DESCRIPCION") }
        return null
    }

    private val etiquetaGenero = Regex(
        """(?i)\b(?:g[eé]nero(?:\s+musical)?|genre|estilo(?:\s+musical)?|ritmo(?:\s+musical)?)\s*[:=]\s*([^\n|;]{2,90})"""
    )
    private val entreCorchetes = Regex("""\(([^()]{2,65})\)|\[([^\[\]]{2,65})\]""")
    private val tituloSeparadores = Regex("""\s+[|•·–—]\s+""")
    private val hashtags = Regex("""#[\p{L}\p{N}_]+""")
    private val lineaGenero = Regex(
        """(?i)^[\p{So}\p{P}\s]{0,8}(?:g[eé]nero(?:\s+musical)?|genre|music\s+genre|estilo(?:\s+musical)?|ritmo(?:\s+musical)?)\s*[:=\-]\s*(.{2,100})$"""
    )
    private val fraseGenero = Regex(
        """(?i)\b(?:canci[oó]n|tema|sencillo|single|ritmo|m[uú]sica)\s+(?:de|del|en\s+estilo|con\s+ritmo\s+de)\s+(.{2,100})"""
    )

    private fun detectarEnTitulo(titulo: String): String? {
        val fragmentos = mutableListOf<String>()
        fragmentos += etiquetaGenero.findAll(titulo).map { it.groupValues[1] }.toList()
        fragmentos += entreCorchetes.findAll(titulo).map { it.groupValues[1].ifBlank { it.groupValues[2] } }.toList()
        fragmentos += hashtags.findAll(titulo).map { it.value }.toList()
        // Un bloque separado por «|» suele indicar estilo: «Tema | Salsa romántica».
        val segmentos = tituloSeparadores.split(titulo)
        if (segmentos.size > 1) {
            fragmentos += listOf(segmentos.first(), segmentos.last()).filter { it.length <= 70 }
        }
        // «Artista - Bachata 2026» es un rótulo reconocible; no analizar títulos libres.
        val guion = titulo.split(Regex("""\s+-\s+"""))
        if (guion.size >= 2) {
            val ultimo = guion.last().trim()
            if (ultimo.length <= 32 && ultimo.split(Regex("""\s+""")).size <= 4) {
                fragmentos += ultimo
            }
        }
        return unico(fragmentos)
    }

    private fun detectarEnDescripcion(descripcion: String?): String? {
        if (descripcion.isNullOrBlank()) return null
        val inicio = descripcion.take(1800)
        val lineas = inicio.lineSequence().take(16).toList()
        // Una declaración explícita es más fiable que hashtags de promoción.
        val declarados = lineas.mapNotNull { lineaGenero.find(it.trim())?.groupValues?.get(1) }
        unico(declarados)?.let { return it }

        // «Esta canción de salsa romántica...»; jamás escanear una descripción entera.
        val frases = lineas.take(8)
            .filter { it.length <= 220 && !it.contains("playlist", ignoreCase = true) }
            .mapNotNull { fraseGenero.find(it)?.groupValues?.get(1) }
        unico(frases)?.let { return it }

        // Hashtags solo en el inicio, fuera de enlaces de redes sociales o playlists.
        val etiquetas = lineas.take(10)
            .filterNot { linea ->
                listOf("http", "instagram", "tiktok", "facebook", "playlist", "suscrib").any {
                    linea.contains(it, ignoreCase = true)
                }
            }
            .flatMap { hashtags.findAll(it).map { match -> match.value }.toList() }
        return detectar(etiquetas)
    }

    /** Si las evidencias difieren, se deja sin clasificar. */
    private fun unico(fragmentos: List<String>): String? =
        fragmentos.mapNotNull { detectar(listOf(it)) }.distinct().singleOrNull()

    /** Valida las categorías manuales (sin permitir nombres ajenos al catálogo). */
    fun canonico(nombre: String): String? = diccionario.keys.firstOrNull {
        normalizar(it) == normalizar(nombre)
    }

    private fun normalizar(texto: String): String {
        val separado = texto.replace(separacionCamel, " ")
        val sinAcentos = Normalizer.normalize(
            separado.lowercase(Locale.ROOT), Normalizer.Form.NFD
        ).replace(diacriticos, "")
        return sinAcentos.replace(separacionLetrasNumeros, " ")
            .replace(simbolos, " ").trim().replace(espacios, " ")
    }
}
