package com.proyectoN.rokolaBackend.youtube

/** La API de YouTube no expone un campo universal de artista musical. */
data class YoutubeVideoResponse(
    val youtubeVideoId: String,
    val titulo: String,
    val artista: String?,
    val artistaEstimado: Boolean,
    val canal: String,
    val duracionSeg: Int,
    val miniaturaUrl: String?,
    val embebible: Boolean,
    val url: String,
    // Tags opcionales de videos.list (snippet.tags); no siempre existen.
    val etiquetas: List<String> = emptyList(),
    // ID de videoCategories de YouTube: 10 = Music. NO es un género musical.
    val categoriaYoutube: String? = null,
)

data class YoutubeBusquedaResponse(
    val consulta: String,
    val origen: String, // "youtube" o "cache"
    val resultados: List<YoutubeVideoResponse>,
)
