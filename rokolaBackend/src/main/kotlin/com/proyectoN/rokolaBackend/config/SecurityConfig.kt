package com.proyectoN.rokolaBackend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Permisos de las APIs usadas por el frontend y Android.
 * La autenticación contractual del panel y la pantalla se valida también en sus servicios.
 * El generador de QR legado se conserva hasta disponer de regeneración en el panel contractual.
 */
@Configuration
class SecurityConfig {
  @Bean
  fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
      .csrf {
        it.ignoringRequestMatchers(
          // Acceso QR legado: la página /q/{token} registra el teléfono con POST.
          "/api/acceso/qr/*/dispositivos",
          // Generación manual de QR; protegida por autenticación HTTP.
          "/api/establecimientos/*/codigos-qr/generar",
          // Operaciones contractuales de los clientes.
          "/api/bares/*/dispositivo",
          "/api/bares/*/dispositivo/aviso",
          "/api/bares/*/solicitudes",
          "/api/bares/*/solicitudes/*/votos",
          "/api/bares/*/solicitudes/*",
          "/api/bares/*/dispositivo/voto",
          // Vinculación y reportes: se exige cookie de pantalla en los servicios.
          "/api/bares/*/pantalla/vinculo",
          "/api/bares/*/pantalla/reproducciones/*/inicio",
          "/api/bares/*/pantalla/reproducciones/*/fin",
          "/api/bares/*/pantalla/reproducciones/*/error",
          // La sesión administrativa se valida por cookie dentro del contrato.
          "/api/panel/**",
        )
      }
      .authorizeHttpRequests {
        it.requestMatchers(HttpMethod.GET, "/q/*", "/api/acceso/qr/*").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/acceso/qr/*/dispositivos").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/qr/**", "/api/bares/**").permitAll()
        it.requestMatchers(HttpMethod.POST,
          "/api/bares/*/dispositivo", "/api/bares/*/dispositivo/aviso",
          "/api/bares/*/solicitudes", "/api/bares/*/solicitudes/*/votos",
          "/api/bares/*/pantalla/vinculo").permitAll()
        it.requestMatchers(HttpMethod.PUT, "/api/bares/*/dispositivo/voto").permitAll()
        it.requestMatchers(HttpMethod.DELETE,
          "/api/bares/*/solicitudes/*", "/api/bares/*/solicitudes/*/votos").permitAll()
        it.requestMatchers(HttpMethod.POST,
          "/api/bares/*/pantalla/reproducciones/*/inicio",
          "/api/bares/*/pantalla/reproducciones/*/fin",
          "/api/bares/*/pantalla/reproducciones/*/error").permitAll()
        it.requestMatchers("/api/panel/**").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/establecimientos/*/codigos-qr/generar").authenticated()
        it.anyRequest().authenticated()
      }
      .httpBasic(Customizer.withDefaults())
    return http.build()
  }
}
