package com.example.rokola

import com.example.rokola.dominio.TemaBar
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rokola.datos.simulacion.Escenario
import com.example.rokola.datos.simulacion.RepositorioSimulado
import com.example.rokola.escaner.EscanerActivity
import com.example.rokola.ui.pantallas.AppRockola
import com.example.rokola.ui.pantallas.Pantalla
import com.example.rokola.ui.pantallas.PantallaEntrada
import com.example.rokola.ui.theme.RokolaTheme
import com.example.rokola.viewmodel.Entrada
import com.example.rokola.viewmodel.Enlace
import com.example.rokola.viewmodel.EntradaViewModel
import com.example.rokola.viewmodel.PantallasDelBar
import com.example.rokola.viewmodel.leerEnlace
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val entrada: EntradaViewModel by viewModels { EntradaViewModel.Fabrica }
    private val pantallasDelBar: PantallasDelBar by viewModels()

    /** Un doble toque no abre dos escáneres. */
    private var escanerAbierto = false

    /** Lo que trae el escáner vuelve a validarse: aquí se decide a qué bar se entra (RF-066). */
    private val escaner = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        escanerAbierto = false
        if (r.resultCode != RESULT_OK) return@registerForActivityResult
        r.data?.getStringExtra(EscanerActivity.EXTRA_QR)
            ?.let(Contenedor::leerQr)
            ?.let(entrada::abrir)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Contenedor.iniciar(this)
        super.onCreate(savedInstanceState)
        // Solo oscuro: iconos claros sobre la madera, en las dos barras.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val depuracion = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (depuracion && savedInstanceState == null) elegirEscenarioDePrueba()
        val inicial = if (depuracion) pantallaDePrueba() else Pantalla.INICIO
        // Al volver tras cerrarse la app, o desde Recientes, el enlace ya se usó: se abre el bar
        // recordado en vez de resolver otra vez un QR que pudo cambiar.
        val desdeRecientes = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        val enlace = if (savedInstanceState == null && !desdeRecientes) enlaceDe(intent) ?: demoDePrueba(depuracion) else null
        entrada.iniciar(enlace)

        setContent {
            RokolaTheme {
                val estado by entrada.estado.collectAsStateWithLifecycle()
                when (val e = estado) {
                    // Otro bar, otras pantallas: los ViewModel del bar anterior se cierran.
                    is Entrada.Lista -> key(e.slug) {
                        CompositionLocalProvider(LocalViewModelStoreOwner provides pantallasDelBar.de(e.slug)) { AppRockola(inicial, entrada::cambiarDeBar) }
                    }
                    else -> {
                        LaunchedEffect(Unit) { pantallasDelBar.cerrar() }
                        // Recién instalada y sin bar: directo a la cámara (RF-066, D22). Sin cámara
                        // no se abre un escáner que solo diría que no la hay.
                        LaunchedEffect(e) {
                            val hayCamara = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                            if (hayCamara && entrada.tomarEscanerInicial()) abrirEscaner()
                        }
                        PantallaEntrada(
                            e,
                            onReintentar = entrada::reintentar,
                            onVolver = entrada::volver,
                            onEscanear = ::abrirEscaner,
                            onDemostracion = entrada.barDeDemostracion?.let { entrada::entrarALaDemostracion },
                        )
                    }
                }
            }
        }
    }

    private fun abrirEscaner() {
        if (escanerAbierto) return
        escanerAbierto = true
        // El bar abierto, aunque no sea el recordado (el de la simulación no se guarda).
        val slug = (entrada.estado.value as? Entrada.Lista)?.slug ?: Contenedor.almacen.slug
        val tema = slug?.let(Contenedor.almacen::temaDe)
        escaner.launch(Intent(this, EscanerActivity::class.java).putExtra(EscanerActivity.EXTRA_TEMA, tema))
    }

    /** Un QR abierto con la app ya abierta llega aquí, aunque el escáner esté encima (singleTask). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        enlaceDe(intent)?.let(entrada::abrir)
    }

    /**
     * Solo `https://<rockola.dominio>/…`: la actividad es pública y otra app podría mandarle un
     * enlace a mano (sin pasar por el filtro del manifiesto) para cambiar el bar del teléfono.
     */
    private fun enlaceDe(intent: Intent?): Enlace? {
        val uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return null
        if (uri.scheme != "https" || uri.host != BuildConfig.ROCKOLA_DOMINIO) return null
        return leerEnlace(uri.pathSegments)
    }

    /**
     * Como `?escenario=` en la web, solo en la compilación de depuración y con la simulación:
     * `adb shell am start -n com.example.rokola/.MainActivity --es escenario fuera-de-horario`
     */
    private fun elegirEscenarioDePrueba() {
        val repo = Contenedor.repositorio as? RepositorioSimulado ?: return
        // `--es tema vidrio|afiche`: el diseño del bar simulado.
        intent.getStringExtra("tema")?.let { repo.temaDePrueba = TemaBar.desde(it) }
        val id = intent.getStringExtra("escenario") ?: return
        val escenario = Escenario.entries.firstOrNull { it.id == id } ?: return
        runBlocking { repo.cambiarEscenario(escenario) }
    }

    /**
     * Con `--es pantalla|escenario|tema` en depuración y la simulación, directo al bar «demo», sin
     * pasar por el escáner (capturas y comprobaciones de frontend/movil/CLAUDE.md, D23).
     */
    private fun demoDePrueba(depuracion: Boolean): Enlace? {
        if (!depuracion || listOf("pantalla", "escenario", "tema").none(intent::hasExtra)) return null
        return entrada.barDeDemostracion?.let(Enlace::Bar)
    }

    /** `--es pantalla buscar|cola|politica`: abre directo en esa pantalla (capturas). */
    private fun pantallaDePrueba(): Pantalla =
        Pantalla.entries.firstOrNull { it.name.equals(intent.getStringExtra("pantalla"), ignoreCase = true) } ?: Pantalla.INICIO
}
