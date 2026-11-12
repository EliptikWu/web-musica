package com.proyectoN.rokolaBackend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/** Mantiene la seguridad anterior y añade solo las rutas públicas del contrato. */
@Configuration
class SecurityConfig {
  @Bean
  fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
      .csrf {
        it.ignoringRequestMatchers(
          "/api/acceso/qr/*/dispositivos",
          "/api/establecimientos/*/codigos-qr/generar",
          "/api/sesiones/*/solicitudes",
          "/api/solicitudes/*/votos",
          "/api/sesiones/*/reproduccion/iniciar",
          "/api/sesiones/*/reproducciones/*/finalizar",
          "/api/sesiones/*/solicitudes/*/descartar",
          // El registro/aviso con UUID del contrato no requiere sesión administrativa.
          "/api/bares/*/dispositivo",
          "/api/bares/*/dispositivo/aviso",
          "/api/bares/*/solicitudes",
          "/api/bares/*/solicitudes/*/votos",
          "/api/bares/*/solicitudes/*", // RF-061: retirar mi pedido (DELETE)
          "/api/bares/*/dispositivo/voto",
          "/api/bares/*/pantalla/reproducciones/*/inicio",
          "/api/bares/*/pantalla/reproducciones/*/fin",
          "/api/bares/*/pantalla/reproducciones/*/error",
          // Panel del contrato: cookie propia SameSite=Lax (un sitio ajeno no la manda en un
          // POST) y el navegador solo llega por el proxy del mismo origen.
          "/api/panel/**",
        )
      }
      .authorizeHttpRequests {
        it.requestMatchers(HttpMethod.GET, "/q/*", "/api/acceso/qr/*").permitAll()
        // Los GET del contrato son públicos, incluso para responder 404 con ErrorApi.
        // No se abre POST/PUT/PATCH/DELETE de forma general.
        it.requestMatchers(HttpMethod.GET, "/api/qr/**", "/api/bares/**").permitAll()
        it.requestMatchers(HttpMethod.GET,
          "/api/bares/*/dispositivo", "/api/bares/*/busqueda").permitAll()
        it.requestMatchers(HttpMethod.POST,
          "/api/bares/*/dispositivo", "/api/bares/*/dispositivo/aviso").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/bares/*/solicitudes", "/api/bares/*/solicitudes/*/votos").permitAll()
        it.requestMatchers(HttpMethod.PUT, "/api/bares/*/dispositivo/voto").permitAll()
        // RF-060 y RF-061: el teléfono quita su voto o retira su pedido; el servicio comprueba que sean suyos.
        it.requestMatchers(HttpMethod.DELETE, "/api/bares/*/solicitudes/*", "/api/bares/*/solicitudes/*/votos").permitAll()
        // El propio servicio exige la credencial de pantalla antes de cualquier escritura.
        it.requestMatchers(HttpMethod.POST,
          "/api/bares/*/pantalla/reproducciones/*/inicio",
          "/api/bares/*/pantalla/reproducciones/*/fin",
          "/api/bares/*/pantalla/reproducciones/*/error").permitAll()
        // El panel del contrato se autentica con su cookie en ContratoPanelAcceso, no con
        // Basic: así un 401 es ErrorApi y el navegador no abre su ventana de usuario y clave.
        it.requestMatchers("/api/panel/**").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/acceso/qr/*/dispositivos").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/youtube/buscar", "/api/youtube/sugerencias").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/sesiones/*/cola").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/sesiones/*/mis-solicitudes").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/sesiones/*/reproduccion/actual").permitAll()
        it.requestMatchers(HttpMethod.GET, "/ws/rokola/cola").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/sesiones/*/solicitudes").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/solicitudes/*/votos").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/establecimientos/*/codigos-qr/generar").authenticated()
        it.requestMatchers(HttpMethod.POST, "/api/sesiones/*/reproduccion/iniciar").authenticated()
        it.requestMatchers(HttpMethod.POST, "/api/sesiones/*/reproducciones/*/finalizar").authenticated()
        it.requestMatchers(HttpMethod.POST, "/api/sesiones/*/solicitudes/*/descartar").authenticated()
        it.anyRequest().authenticated()
      }
      .httpBasic(Customizer.withDefaults())
    return http.build()
  }
}
