package com.example.rokola.ui.pantallas

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.util.Size
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.rokola.Contenedor
import com.example.rokola.escaner.AnalizadorQr
import com.example.rokola.ui.componentes.Aviso
import com.example.rokola.ui.componentes.Enlace
import com.example.rokola.ui.componentes.MarcoCliente
import com.example.rokola.ui.componentes.TamanoTecla
import com.example.rokola.ui.componentes.Tecla
import com.example.rokola.ui.componentes.TipoAviso
import com.example.rokola.ui.componentes.VarianteTecla
import com.example.rokola.ui.componentes.Ventana
import com.example.rokola.ui.textos.TextosEscaner
import com.example.rokola.ui.textos.TituloMeta
import com.example.rokola.ui.theme.CremaBase
import com.example.rokola.ui.theme.Crema2
import com.example.rokola.ui.theme.Cuerpo
import com.example.rokola.ui.theme.NogalHondo
import com.example.rokola.ui.theme.RadioMarco
import com.example.rokola.ui.theme.Tinta
import com.example.rokola.ui.theme.Tinta2
import com.example.rokola.ui.theme.TituloSeccion
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

private const val AVISO_AJENO_MS = 4_000L

/** Si Android responde así de rápido, no mostró el diálogo: el permiso ya estaba bloqueado. */
private const val RESPUESTA_SIN_DIALOGO_MS = 250L

private fun tienePermiso(contexto: Context) =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/**
 * Escanear el QR del bar con la cámara (RF-066). Pide el permiso al entrar, porque quien llega
 * aquí tocó «Escanear QR»; si lo niega, dice cómo activarlo. Un QR que no es de Rockola no se
 * abre nunca: se avisa y la cámara sigue buscando.
 */
@Composable
fun PantallaEscaner(onLeido: (String) -> Unit, onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val tieneCamara = remember { contexto.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var permiso by remember { mutableStateOf(tienePermiso(contexto)) }
    var respondio by rememberSaveable { mutableStateOf(false) }
    var falloCamara by remember { mutableStateOf(false) }
    var intento by remember { mutableIntStateOf(0) }
    var ajeno by remember { mutableStateOf(false) }
    var ultimoAjeno by remember { mutableLongStateOf(0L) }

    val debeExplicar = {
        (contexto as? Activity)?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } ?: false
    }
    // Bloqueado: Android ya no muestra el diálogo y solo queda ir a Ajustes. Cerrar el diálogo sin
    // responder no cuenta: deja `debeExplicar` en falso igual que antes de preguntar.
    var bloqueada by rememberSaveable { mutableStateOf(false) }
    var explicabaAntes by remember { mutableStateOf(false) }
    var pedidoEn by remember { mutableLongStateOf(0L) }

    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        permiso = concedido
        respondio = true
        val sinDialogo = SystemClock.elapsedRealtime() - pedidoEn < RESPUESTA_SIN_DIALOGO_MS
        bloqueada = !concedido && !debeExplicar() && (explicabaAntes || sinDialogo)
    }
    val pedirPermiso = {
        explicabaAntes = debeExplicar()
        pedidoEn = SystemClock.elapsedRealtime()
        lanzador.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(Unit) {
        if (tieneCamara && !permiso && !respondio) pedirPermiso()
    }
    // Al volver de los ajustes del teléfono, el permiso pudo cambiar en cualquier sentido.
    LifecycleResumeEffect(Unit) {
        permiso = tienePermiso(contexto)
        if (permiso || debeExplicar()) bloqueada = false
        onPauseOrDispose { }
    }
    // El aviso sigue mientras el mismo QR esté a la vista: quitarlo y ponerlo interrumpiría a TalkBack.
    LaunchedEffect(ajeno) {
        while (ajeno) {
            delay(500)
            if (SystemClock.elapsedRealtime() - ultimoAjeno > AVISO_AJENO_MS) ajeno = false
        }
    }

    val vista = LocalView.current
    val alLeido by rememberUpdatedState(onLeido)
    val terminado = remember { AtomicBoolean(false) }
    val principal = remember { ContextCompat.getMainExecutor(contexto) }
    val alLeer = remember {
        { texto: String ->
            principal.execute {
                when {
                    terminado.get() -> Unit
                    Contenedor.leerQr(texto) == null -> {
                        ultimoAjeno = SystemClock.elapsedRealtime()
                        ajeno = true
                    }
                    terminado.compareAndSet(false, true) -> {
                        vista.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        alLeido(texto)
                    }
                }
            }
        }
    }

    val aviso: TituloMeta? = when {
        !tieneCamara -> TextosEscaner.sinCamara
        !permiso -> if (bloqueada) TextosEscaner.bloqueada else TextosEscaner.permiso
        falloCamara -> TextosEscaner.falloCamara
        else -> null
    }

    MarcoCliente(
        titulo = TextosEscaner.TITULO,
        abajo = {
            when {
                !tieneCamara -> Tecla(TextosEscaner.VOLVER, onCerrar, variante = VarianteTecla.CEREZA, tamano = TamanoTecla.GRANDE)
                !permiso && bloqueada -> AccionYVolver(TextosEscaner.ABRIR_AJUSTES, { abrirAjustes(contexto) }, onCerrar)
                !permiso -> AccionYVolver(TextosEscaner.PERMITIR, { pedirPermiso() }, onCerrar)
                falloCamara -> AccionYVolver(TextosEscaner.REINTENTAR, { falloCamara = false; intento++ }, onCerrar)
                else -> Tecla(TextosEscaner.CANCELAR, onCerrar, tamano = TamanoTecla.GRANDE)
            }
        },
    ) {
        TituloPlaca(TextosEscaner.TITULO)
        if (aviso != null) {
            Ventana(
                Modifier.padding(top = 20.dp),
                relleno = PaddingValues(16.dp),
                alineacion = Alignment.Start,
            ) {
                Text(aviso.titulo, style = TituloSeccion, color = Tinta)
                Text(aviso.meta, Modifier.padding(top = 6.dp), style = Cuerpo.copy(fontSize = 15.sp, lineHeight = 1.45.em), color = Tinta2)
            }
        } else {
            Text(
                TextosEscaner.INSTRUCCION,
                Modifier.padding(top = 6.dp),
                style = Cuerpo.copy(fontSize = 15.sp, lineHeight = 1.4.em),
                color = Crema2,
            )
            // Otro intento, otra cámara: la llave vuelve a montar el visor desde cero.
            androidx.compose.runtime.key(intento) {
                VisorCamara(Modifier.padding(top = 16.dp), alLeer) { falloCamara = true }
            }
            if (ajeno) {
                Aviso(
                    Modifier.padding(top = 12.dp),
                    tipo = TipoAviso.ERROR,
                    titulo = TextosEscaner.ajeno.titulo,
                    meta = TextosEscaner.ajeno.meta,
                )
            }
            Text(
                TextosEscaner.PRIVACIDAD,
                Modifier.padding(top = 12.dp),
                style = Cuerpo.copy(fontSize = 14.sp, lineHeight = 1.4.em),
                color = Crema2,
            )
        }
    }
}

@Composable
private fun ColumnScope.AccionYVolver(texto: String, alPulsar: () -> Unit, onCerrar: () -> Unit) {
    Tecla(texto, alPulsar, variante = VarianteTecla.CEREZA, tamano = TamanoTecla.GRANDE)
    Enlace(TextosEscaner.VOLVER, onCerrar, Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp))
}

private fun abrirAjustes(contexto: Context) {
    contexto.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null)),
    )
}

/** La imagen de la cámara con las cuatro esquinas de mira. Vive solo mientras está en pantalla. */
@Composable
private fun VisorCamara(modifier: Modifier, alLeer: (String) -> Unit, alFallar: () -> Unit) {
    val contexto = LocalContext.current
    val dueno = LocalLifecycleOwner.current
    val fallar by rememberUpdatedState(alFallar)
    val imagen = remember {
        PreviewView(contexto).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    DisposableEffect(dueno) {
        val hilo = Executors.newSingleThreadExecutor()
        val futuro = ProcessCameraProvider.getInstance(contexto)
        var activo = true
        var enUso: List<androidx.camera.core.UseCase> = emptyList()
        var estadoCamara: androidx.lifecycle.LiveData<CameraState>? = null
        val alCambiarEstado = androidx.lifecycle.Observer<CameraState> { if (it.error != null) fallar() }
        futuro.addListener({
            if (!activo) return@addListener
            runCatching {
                val proveedor = futuro.get()
                val previa = Preview.Builder().build().also { it.setSurfaceProvider(imagen.surfaceProvider) }
                val analisis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                            .build(),
                    )
                    .build()
                    .also { it.setAnalyzer(hilo, AnalizadorQr(alLeer)) }
                // La de atrás; si no hay, la de adelante o la que tenga (una USB en una tableta).
                val camara = when {
                    proveedor.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    proveedor.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> proveedor.availableCameraInfos.first().cameraSelector
                }
                enUso = listOf(previa, analisis)
                // Cámara ocupada o desactivada: CameraX lo avisa después de enlazar, no con una excepción.
                estadoCamara = proveedor.bindToLifecycle(dueno, camara, previa, analisis).cameraInfo.cameraState
                estadoCamara?.observe(dueno, alCambiarEstado)
            }.onFailure { fallar() }
        }, ContextCompat.getMainExecutor(contexto))
        onDispose {
            activo = false
            estadoCamara?.removeObserver(alCambiarEstado)
            if (futuro.isDone) runCatching { futuro.get().unbind(*enUso.toTypedArray()) }
            hilo.shutdown()
        }
    }
    // Crema con halo oscuro en los tres temas: se lee sobre cualquier imagen y no gasta el acento.
    val mira = CremaBase
    val fondoMira = NogalHondo
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RadioMarco)
            .semantics { contentDescription = TextosEscaner.VISOR },
    ) {
        AndroidView({ imagen }, Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize().padding(28.dp)) {
            esquinas(fondoMira, 9.dp.toPx())
            esquinas(mira, 5.dp.toPx())
        }
    }
}

private fun DrawScope.esquinas(color: Color, grosor: Float) {
    val brazo = size.minDimension * 0.18f
    val (a, b) = size.width to size.height
    val trazos = listOf(
        listOf(Offset(0f, brazo), Offset.Zero, Offset(brazo, 0f)),
        listOf(Offset(a - brazo, 0f), Offset(a, 0f), Offset(a, brazo)),
        listOf(Offset(a, b - brazo), Offset(a, b), Offset(a - brazo, b)),
        listOf(Offset(brazo, b), Offset(0f, b), Offset(0f, b - brazo)),
    )
    trazos.forEach { (inicio, codo, fin) ->
        val camino = Path().apply {
            moveTo(inicio.x, inicio.y)
            lineTo(codo.x, codo.y)
            lineTo(fin.x, fin.y)
        }
        drawPath(camino, color, style = Stroke(grosor, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
