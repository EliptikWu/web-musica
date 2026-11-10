
package com.proyectoN.rokolaBackend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

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
        )
      }
      .authorizeHttpRequests {
        it.requestMatchers(HttpMethod.GET, "/q/*", "/api/acceso/qr/*").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/acceso/qr/*/dispositivos").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/youtube/buscar", "/api/youtube/sugerencias").permitAll()
        it.requestMatchers(HttpMethod.GET, "/api/sesiones/*/cola").permitAll()
        it.requestMatchers(
          HttpMethod.POST,
          "/api/sesiones/*/solicitudes"
        ).permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/solicitudes/*/votos").permitAll()
        it.requestMatchers(HttpMethod.POST, "/api/establecimientos/*/codigos-qr/generar")
          .authenticated()
        it.anyRequest().authenticated()
      }
      .httpBasic(Customizer.withDefaults())

    return http.build()
  }
}
