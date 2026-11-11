package com.example.rokola.datos.local

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/**
 * Lo que el teléfono recuerda entre aperturas (apps/web/lib/dispositivo.ts):
 *
 * - El identificador seudónimo (RF-002, RNF-007): un UUID creado aquí. El servidor guarda solo
 *   su SHA-256; la app nunca lo muestra ni lo escribe en el registro. Si se borran los datos
 *   de la app, el teléfono cuenta como nuevo (doc 01).
 * - El bar del último QR, para volver a abrirlo sin escanear otra vez.
 *
 * El archivo queda fuera de las copias de seguridad (backup_rules.xml y data_extraction_rules.xml): un identificador
 * copiado a otro teléfono haría que dos teléfonos contaran como uno.
 */
class AlmacenTelefono(context: Context) {
    private val preferencias = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    @Volatile private var enMemoria: String? = null

    @Synchronized
    fun idDispositivo(): String {
        enMemoria?.let { return it }
        val guardado = preferencias.getString(CLAVE_DISPOSITIVO, null)
        val id = guardado ?: UUID.randomUUID().toString().also { preferencias.edit { putString(CLAVE_DISPOSITIVO, it) } }
        enMemoria = id
        return id
    }

    var slug: String?
        get() = preferencias.getString(CLAVE_SLUG, null)
        set(valor) = preferencias.edit { putString(CLAVE_SLUG, valor) }

    companion object {
        /** Mismo nombre en backup_rules.xml y data_extraction_rules.xml. */
        const val ARCHIVO = "rockola-telefono"
        private const val CLAVE_DISPOSITIVO = "dispositivo"
        private const val CLAVE_SLUG = "bar"
    }
}
