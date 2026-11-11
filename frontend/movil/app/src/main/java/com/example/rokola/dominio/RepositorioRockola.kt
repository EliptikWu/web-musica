package com.example.rokola.dominio

import kotlinx.coroutines.flow.Flow

/**
 * Lo que el teléfono le pide al backend (doc 04, «Cliente»), siempre sobre el bar actual (el
 * del último QR). Lo implementan la simulación y el cliente HTTP (datos/api); la interfaz nunca
 * toca la red: habla con esto.
 */
interface RepositorioRockola {
    /**
     * GET /qr/{token}: el bar al que lleva un QR (RF-001, RF-035). No depende del bar actual.
     * Errores: QR_REEMPLAZADO si el bar lo regeneró, NO_ENCONTRADO si no existe.
     */
    suspend fun resolverQr(token: String): Resultado<String>

    /**
     * GET /bares/{slug}: el bar actual o, con [slug], ese otro (para comprobar que existe
     * antes de entrar a él, sin cambiar el bar actual).
     */
    suspend fun bar(slug: String? = null): Resultado<Bar>

    /** GET /bares/{slug}/ahora */
    suspend fun ahora(): Resultado<Ahora>

    /**
     * GET /bares/{slug}/eventos: primero una foto y después una por cada cambio. Si el
     * tiempo real se corta, el Flow termina con [FalloRockola] (SIN_CONEXION).
     */
    fun observarAhora(): Flow<Ahora>

    /** GET /bares/{slug}/destacadas */
    suspend fun destacadas(): Resultado<List<Destacada>>

    /** POST /bares/{slug}/dispositivo: registra el teléfono y devuelve [Yo]. */
    suspend fun registrarDispositivo(): Resultado<Yo>

    /** GET /bares/{slug}/dispositivo */
    suspend fun yo(): Resultado<Yo>

    /** POST /bares/{slug}/dispositivo/aviso */
    suspend fun anotarAvisoVisto(): Resultado<Unit>

    /** GET /bares/{slug}/busqueda?q= */
    suspend fun buscar(consulta: String): Resultado<Busqueda>

    /** POST /bares/{slug}/solicitudes */
    suspend fun pedir(videoId: String): Resultado<RespuestaSolicitud>

    /** POST /bares/{slug}/solicitudes/{id}/votos */
    suspend fun votar(solicitudId: String): Resultado<RespuestaVoto>

    /** PUT /bares/{slug}/dispositivo/voto */
    suspend fun moverVoto(desde: String, hacia: String): Resultado<RespuestaVoto>
}
