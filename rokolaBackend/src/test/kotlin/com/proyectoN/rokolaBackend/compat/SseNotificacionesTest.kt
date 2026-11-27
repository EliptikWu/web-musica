package com.proyectoN.rokolaBackend.compat

import com.proyectoN.rokolaBackend.tiemporeal.CambioColaEvent
import com.proyectoN.rokolaBackend.tiemporeal.CambioColaPublisher
import org.mockito.Mockito.*
import org.springframework.context.ApplicationEventPublisher
import kotlin.test.*

/** CP-012/029 y RNF-009: publicación del evento que origina la actualización SSE. */
class SseNotificacionesTest {
    @Test fun `publicar envia sesion y motivo de cambio correctos`() {
        val bus = mock(ApplicationEventPublisher::class.java)
        CambioColaPublisher(bus).publicar(123, "voto_agregado")
        val captor = org.mockito.ArgumentCaptor.forClass(CambioColaEvent::class.java)
        verify(bus).publishEvent(captor.capture())
        assertEquals(123L, captor.value.sesionId)
        assertEquals("voto_agregado", captor.value.tipo)
    }
    @Test fun `cada cambio publico genera un evento independiente`() {
        val bus = mock(ApplicationEventPublisher::class.java)
        val publicador = CambioColaPublisher(bus)
        publicador.publicar(2, "solicitud_creada")
        publicador.publicar(2, "retiro_panel")
        val segundoCaptor = org.mockito.ArgumentCaptor.forClass(CambioColaEvent::class.java)
        verify(bus, times(2)).publishEvent(segundoCaptor.capture())
        assertEquals(listOf("solicitud_creada", "retiro_panel"), segundoCaptor.allValues.map { it.tipo })
    }
}
