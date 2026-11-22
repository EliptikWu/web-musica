package com.example.rokola.escaner

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rokola.dominio.TemaBar
import com.example.rokola.ui.pantallas.PantallaEscaner
import com.example.rokola.ui.theme.RokolaTheme

/**
 * El escáner del QR (RF-066), aparte para que la cámara viva solo mientras se ve y la app
 * conserve la pantalla donde estaba. No es exportada: solo la abre MainActivity.
 */
class EscanerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val tema = TemaBar.desde(intent.getStringExtra(EXTRA_TEMA))
        setContent {
            RokolaTheme(tema) {
                PantallaEscaner(
                    onLeido = { texto ->
                        setResult(RESULT_OK, Intent().putExtra(EXTRA_QR, texto))
                        finish()
                    },
                    onCerrar = ::finish,
                )
            }
        }
    }

    companion object {
        const val EXTRA_TEMA = "tema"
        const val EXTRA_QR = "qr"
    }
}
