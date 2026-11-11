package com.example.rokola

import android.content.Context
import android.content.res.Resources
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.rokola.datos.api.ClienteApi
import com.example.rokola.datos.api.RepositorioApi
import com.example.rokola.datos.api.nuevoHttp
import com.example.rokola.datos.local.AlmacenTelefono
import com.example.rokola.datos.simulacion.RepositorioSimulado
import com.example.rokola.dominio.RepositorioRockola
import okhttp3.OkHttpClient

/**
 * Dónde se decide qué repositorio usa la app, como `NEXT_PUBLIC_API_MODE` en la web: si se
 * compiló con `rockola.apiUrl` (BuildConfig.ROCKOLA_API_URL), la API real; si no, la simulación.
 * Los ViewModel no se enteran del cambio.
 */
object Contenedor {
    /** Lo que el teléfono recuerda; existe desde [iniciar]. No se guarda el Context. */
    lateinit var almacen: AlmacenTelefono
        private set

    /** Se llama una vez, antes de pedir nada (MainActivity.onCreate). */
    fun iniciar(context: Context) {
        if (::almacen.isInitialized) return
        almacen = AlmacenTelefono(context)
        // Las miniaturas comparten conexiones con la API, pero sí siguen redirecciones (i.ytimg.com
        // puede redirigir y a ellas no va el identificador del teléfono).
        SingletonImageLoader.setSafe { ctx ->
            val imagenes = http.newBuilder().followRedirects(true).followSslRedirects(true).build()
            ImageLoader.Builder(ctx).components { add(OkHttpNetworkFetcherFactory(callFactory = { imagenes })) }.build()
        }
    }

    val conApi: Boolean get() = BuildConfig.ROCKOLA_API_URL.isNotEmpty()

    /** Sin backend, la app abre el bar de la simulación sin pedir QR. */
    val barPorDefecto: String? get() = if (conApi) null else SLUG_SIMULADO

    private val http: OkHttpClient by lazy { nuevoHttp() }

    val repositorio: RepositorioRockola by lazy {
        if (conApi) {
            RepositorioApi(
                api = ClienteApi(BuildConfig.ROCKOLA_API_URL, http, almacen::idDispositivo),
                http = http,
                slugActual = { almacen.slug },
                tipoDispositivo = { tipoDispositivo() },
            )
        } else {
            RepositorioSimulado(slugPedido = { almacen.slug })
        }
    }

    private const val SLUG_SIMULADO = "demo"
}

/** Por el ancho de la pantalla, no por el modelo (apps/web/lib/dispositivo.ts). */
fun tipoDispositivo(anchoDp: Int = Resources.getSystem().configuration.screenWidthDp): String = when {
    anchoDp < 600 -> "movil"
    anchoDp < 1024 -> "tableta"
    else -> "otro"
}
