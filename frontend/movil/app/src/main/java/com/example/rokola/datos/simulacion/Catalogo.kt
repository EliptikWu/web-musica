package com.example.rokola.datos.simulacion

/*
 * Catálogo de la simulación (apps/web/simulacion/datos/catalogo.ts): lo que «YouTube»
 * devuelve al buscar. Canciones y artistas reales; duraciones aproximadas. Los videoId
 * empiezan por «sim-»: no son IDs de YouTube.
 * Generado desde la web: si cambia allá, se regenera aquí.
 */

private class Fila(val id: String, val titulo: String, val artista: String, val duracion: String, val genero: String)

/** "m:ss" o "h:mm:ss" a segundos. */
internal fun segundos(texto: String): Int = texto.split(":").fold(0) { total, parte -> total * 60 + parte.toInt() }

private val FILAS = listOf(
    Fila("rebelion", "Rebelión", "Joe Arroyo", "5:58", "salsa"),
    Fila("hips-dont-lie", "Hips Don't Lie", "Shakira", "3:38", "pop"),
    Fila("cali-pachanguero", "Cali Pachanguero", "Grupo Niche", "5:52", "salsa"),
    Fila("dtmf", "DtMF", "Bad Bunny", "3:57", "urbano"),
    Fila("monaco", "Monaco", "Bad Bunny", "4:27", "urbano"),
    Fila("la-bicicleta", "La Bicicleta", "Carlos Vives y Shakira", "3:47", "tropical"),
    Fila("nuevayol", "NUEVAYoL", "Bad Bunny", "3:03", "urbano"),
    Fila("tqg", "TQG", "Karol G y Shakira", "3:17", "urbano"),
    Fila("luna", "Luna", "Feid y ATL Jacob", "3:16", "urbano"),
    Fila("una-aventura", "Una aventura", "Grupo Niche", "4:45", "salsa"),
    Fila("titi-me-pregunto", "Tití Me Preguntó", "Bad Bunny", "4:03", "urbano"),
    Fila("gasolina", "Gasolina", "Daddy Yankee", "3:12", "urbano"),
    Fila("la-bachata", "La Bachata", "Manuel Turizo", "3:02", "urbano"),
    Fila("despecha", "Despechá", "Rosalía", "2:37", "pop"),
    Fila("baile-inolvidable", "BAILE INoLVIDABLE", "Bad Bunny", "6:07", "sin_clasificar"),
    Fila("me-porto-bonito", "Me Porto Bonito", "Bad Bunny y Chencho Corleone", "2:58", "urbano"),
    Fila("ojitos-lindos", "Ojitos Lindos", "Bad Bunny y Bomba Estéreo", "4:18", "urbano"),
    Fila("provenza", "PROVENZA", "Karol G", "3:30", "urbano"),
    Fila("classy-101", "CLASSY 101", "Feid y Young Miko", "3:15", "sin_clasificar"),
    Fila("en-barranquilla-me-quedo", "En Barranquilla me quedo", "Joe Arroyo", "4:55", "salsa"),
    Fila("la-noche", "La noche", "Joe Arroyo", "4:28", "salsa"),
    Fila("gotas-de-lluvia", "Gotas de lluvia", "Grupo Niche", "5:12", "salsa"),
    Fila("sin-sentimiento", "Sin sentimiento", "Grupo Niche", "5:20", "salsa"),
    Fila("buenaventura-y-caney", "Buenaventura y Caney", "Grupo Niche", "5:05", "salsa"),
    Fila("cali-aji", "Cali ají", "Grupo Niche", "4:30", "salsa"),
    Fila("hagamos-lo-que-diga", "Hagamos lo que diga el corazón", "Grupo Niche", "5:00", "salsa"),
    Fila("oiga-mire-vea", "Oiga, mire, vea", "Guayacán Orquesta", "4:40", "salsa"),
    Fila("vas-a-llorar-por-mi", "Vas a llorar por mí", "Guayacán Orquesta", "4:50", "salsa"),
    Fila("el-preso", "El preso", "Fruko y sus Tesos", "4:24", "salsa"),
    Fila("los-charcos", "Los charcos", "Fruko y sus Tesos", "4:30", "salsa"),
    Fila("periodico-de-ayer", "Periódico de ayer", "Héctor Lavoe", "6:20", "salsa"),
    Fila("mi-gente", "Mi gente", "Héctor Lavoe", "5:55", "salsa"),
    Fila("juanito-alimana", "Juanito Alimaña", "Héctor Lavoe", "5:45", "salsa"),
    Fila("aguanile", "Aguanile", "Héctor Lavoe y Willie Colón", "7:30", "salsa"),
    Fila("idilio", "Idilio", "Willie Colón", "5:35", "salsa"),
    Fila("pedro-navaja", "Pedro Navaja", "Rubén Blades", "7:20", "salsa"),
    Fila("plastico", "Plástico", "Rubén Blades y Willie Colón", "6:50", "salsa"),
    Fila("decisiones", "Decisiones", "Rubén Blades", "4:25", "salsa"),
    Fila("la-vida-es-un-carnaval", "La vida es un carnaval", "Celia Cruz", "4:35", "salsa"),
    Fila("quimbara", "Quimbara", "Celia Cruz", "4:10", "salsa"),
    Fila("usted-abuso", "Usted abusó", "Celia Cruz", "4:00", "salsa"),
    Fila("lloraras", "Llorarás", "Oscar D'León", "5:00", "salsa"),
    Fila("conciencia", "Conciencia", "Gilberto Santa Rosa", "5:16", "salsa"),
    Fila("que-alguien-me-diga", "Que alguien me diga", "Gilberto Santa Rosa", "4:50", "salsa"),
    Fila("vivir-mi-vida", "Vivir mi vida", "Marc Anthony", "4:12", "salsa"),
    Fila("valio-la-pena", "Valió la pena", "Marc Anthony", "4:50", "salsa"),
    Fila("tu-amor-me-hace-bien", "Tu amor me hace bien", "Marc Anthony", "4:40", "salsa"),
    Fila("desnudate-mujer", "Desnúdate mujer", "Frankie Ruiz", "4:40", "salsa"),
    Fila("la-cura", "La cura", "Frankie Ruiz", "4:30", "salsa"),
    Fila("tu-me-quemas", "Tú me quemas", "Eddie Santiago", "4:35", "salsa"),
    Fila("amores-como-el-nuestro", "Amores como el nuestro", "Jerry Rivera", "4:20", "salsa"),
    Fila("un-verano-en-nueva-york", "Un verano en Nueva York", "El Gran Combo de Puerto Rico", "5:05", "salsa"),
    Fila("brujeria", "Brujería", "El Gran Combo de Puerto Rico", "4:50", "salsa"),
    Fila("fuego-en-el-23", "Fuego en el 23", "Sonora Ponceña", "4:45", "salsa"),
    Fila("micaela", "Micaela", "Sonora Carruseles", "4:30", "salsa"),
    Fila("la-pollera-colora", "La pollera colorá", "Pedro Salcedo y su orquesta", "3:00", "tropical"),
    Fila("colombia-tierra-querida", "Colombia tierra querida", "Lucho Bermúdez", "3:00", "tropical"),
    Fila("la-gota-fria", "La gota fría", "Carlos Vives", "4:45", "vallenato"),
)

/** Por videoId, en el orden de la web (la búsqueda depende de él). */
internal fun crearCatalogo(): LinkedHashMap<String, CancionSim> {
    val catalogo = LinkedHashMap<String, CancionSim>()
    for (f in FILAS) {
        val id = "sim-${f.id}"
        catalogo[id] = CancionSim(id, f.titulo, f.artista, segundos(f.duracion), embebible = true, disponible = true, genero = f.genero)
    }
    // Un mix largo: no se puede pedir porque dura más de 10 min (RN-06).
    catalogo["sim-mix-bad-bunny-2025"] = CancionSim(
        "sim-mix-bad-bunny-2025", "Mix Bad Bunny 2025", "Grandes éxitos", segundos("1:12:40"),
        embebible = true, disponible = true, genero = "urbano",
    )
    return catalogo
}

/** Las 42 de «Salsa y tropical de la casa», en orden. */
internal val LISTA_SALSA_Y_TROPICAL = listOf(
    "rebelion", "en-barranquilla-me-quedo", "la-noche", "gotas-de-lluvia", "sin-sentimiento",
    "buenaventura-y-caney", "cali-pachanguero", "una-aventura", "oiga-mire-vea",
    "vas-a-llorar-por-mi", "el-preso", "los-charcos", "periodico-de-ayer", "mi-gente", "idilio",
    "pedro-navaja", "plastico", "decisiones", "la-vida-es-un-carnaval", "quimbara", "usted-abuso",
    "lloraras", "conciencia", "que-alguien-me-diga", "vivir-mi-vida", "valio-la-pena",
    "desnudate-mujer", "la-cura", "tu-me-quemas", "amores-como-el-nuestro",
    "un-verano-en-nueva-york", "brujeria", "fuego-en-el-23", "la-pollera-colora",
    "colombia-tierra-querida", "la-gota-fria", "micaela", "juanito-alimana", "aguanile",
    "cali-aji", "hagamos-lo-que-diga", "tu-amor-me-hace-bien",
).map { "sim-$it" }

/** Una segunda lista, inactiva. */
internal val LISTA_VIERNES_CROSSOVER = listOf(
    "hips-dont-lie", "la-bicicleta", "tqg", "provenza", "me-porto-bonito", "ojitos-lindos",
    "la-gota-fria", "vivir-mi-vida",
).map { "sim-$it" }
