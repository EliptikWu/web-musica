package com.example.rokola.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.rokola.datos.api.BarJson
import com.example.rokola.datos.api.JsonContrato
import com.example.rokola.dominio.TemaBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** RF-059: el tema del servidor y el contraste de cada tema (WCAG 2.1 AA). */
class TemasTest {
    private fun bar(tema: String?) = JsonContrato.decodeFromString<BarJson>(
        """{"slug":"la-rockola","nombre":"La Rockola","zona_horaria":"America/Bogota","pedidos_maximos":3,
            "duracion_max_seg":600,"ventana_no_repeticion_min":60,"horario":[]${if (tema != null) ""","tema":$tema""" else ""}}""",
    ).aDominio()

    @Test
    fun `lee cada tema del contrato`() {
        assertEquals(TemaBar.ROCKOLA, bar("\"rockola\"").tema)
        assertEquals(TemaBar.VIDRIO, bar("\"vidrio\"").tema)
        assertEquals(TemaBar.AFICHE, bar("\"afiche\"").tema)
    }

    @Test
    fun `sin tema o con uno desconocido cae a Rockola y el bar se abre`() {
        assertEquals(TemaBar.ROCKOLA, bar(null).tema)
        assertEquals(TemaBar.ROCKOLA, bar("null").tema)
        assertEquals(TemaBar.ROCKOLA, bar("\"neon\"").tema)
        assertEquals(TemaBar.ROCKOLA, bar("7").tema)
    }

    private fun contraste(texto: Color, fondo: Color): Double {
        val f = fondo.compositeOver(Color.Black)
        val t = texto.compositeOver(f)
        val a = t.luminance().toDouble() + 0.05
        val b = f.luminance().toDouble() + 0.05
        return max(a, b) / min(a, b)
    }

    private fun exige(nombre: String, texto: Color, fondo: Color, minimo: Double = 4.5) {
        val r = contraste(texto, fondo)
        assertTrue("$nombre: $r < $minimo", r >= minimo)
    }

    @Test
    fun `Vidrio se lee sin desenfoque, sobre la mancha mas clara`() {
        // Sin backdrop-filter (Compose no lo tiene): carbón + la mancha durazno al máximo + vidrio.
        val mancha = Color(red = 244, green = 169, blue = 142).copy(alpha = 0.34f).compositeOver(TonosVidrio.Carbon)
        // El texto secundario va sobre el vidrio normal; sobre el alto (avisos, pestaña actual), blanco.
        exige("texto", PaletaVidrio.crema, TonosVidrio.SuperficieAlta.compositeOver(mancha))
        exige("texto secundario", PaletaVidrio.crema2, TonosVidrio.Superficie.compositeOver(mancha))
        exige("tecla", TonosVidrio.Blanco, TonosVidrio.TeclaFondo.compositeOver(mancha))
        val claroSobreVotado = TonosVidrio.Votado.compositeOver(mancha)
        exige("votaste", TonosVidrio.Blanco, claroSobreVotado)
        TonosVidrio.ParadasAcento.forEach {
            exige("tinta sobre el degradado", TonosVidrio.TintaVidrio, it)
            exige("tinta 2 sobre el degradado", TonosVidrio.Tinta2Vidrio, it)
        }
    }

    @Test
    fun `Afiche se lee sobre el papel y la tinta partida`() {
        exige("tinta", PaletaAfiche.crema, TonosAfiche.PapelAfiche)
        exige("tinta 2", PaletaAfiche.crema2, TonosAfiche.PapelAfiche)
        exige("tinta 2 en papel hondo", PaletaAfiche.tinta2, TonosAfiche.PapelHondo)
        exige("sello", TonosAfiche.Sello, TonosAfiche.PapelAfiche)
        exige("amarillo sobre tinta", TonosAfiche.Amarillo, TonosAfiche.TintaAfiche)
        exige("papel sobre tinta", TonosAfiche.PapelSobreTinta, TonosAfiche.TintaAfiche)
        exige("tinta en la fila tuya", TonosAfiche.TintaAfiche, TonosAfiche.ResaltadorFila.compositeOver(TonosAfiche.PapelAfiche))
        TonosAfiche.ParadasPartida.forEach { exige("tinta sobre la partida", TonosAfiche.TintaAfiche, it) }
    }

    @Test
    fun `Rockola conserva sus valores`() {
        assertEquals(Color(0xFFF4E7CB), PaletaRockola.crema)
        assertEquals(Color(0xFF23140F), PaletaRockola.tinta)
        assertEquals(Color(0xFFB8282F), PaletaRockola.cereza)
        assertEquals(Color(0xFFF5A524), PaletaRockola.ambar)
    }
}
