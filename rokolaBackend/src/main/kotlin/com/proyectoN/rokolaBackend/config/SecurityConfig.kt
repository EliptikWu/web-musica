package com.proyectoN.rokolaBackend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Lecturas publicas de estado y cola. Las escrituras de reproduccion requieren
 * la autenticacion HTTP Basic administrativa ya configurada en el proyecto.
 */
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
        )
      }
      .authorizeHttpRequests {
        it.requestMatchers(HttpMethod.GET, "/q/*", "/api/acceso/qr/*").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/acceso/qr/*/dispositivos").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/youtube/buscar", "/api/youtube/sugerencias").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/sesiones/*/cola").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/sesiones/*/mis-solicitudes").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/sesiones/*/reproduccion/actual").permitAll()
        it.requestMatchers(HttpMethod.GET, "/ws/rokola/cola").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/sesiones/*/solicitudes").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/solicitudes/*/votos").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/establecimientos/*/codigos-qr/generar").authenticated()
        // Estos endpoints NO son accesibles por los clientes sin login administrativo.
        it.requestMatchers(HttpMethod.POST, "/api/sesiones/*/reproduccion/iniciar").authenticated()
        it.requestMatchers(HttpMethod.POST, "/api/sesiones/*/reproducciones/*/finalizar").authenticated()
        it.requestMatchers(HttpMethod.POST, "/api/sesiones/*/solicitudes/*/descartar").authenticated()
        it.anyRequest().authenticated()
      }
      .httpBasic(Customizer.withDefaults())
    return http.build()
  }
}
