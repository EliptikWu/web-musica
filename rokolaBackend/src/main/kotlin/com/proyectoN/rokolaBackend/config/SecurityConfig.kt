
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
          "/api/acceso/qr/*/dispositivos"
        )
      }
      .authorizeHttpRequests {
        it.requestMatchers(
          HttpMethod.GET,
          "/api/acceso/qr/*",
          "/q/*"
        ).permitAll()

        it.requestMatchers(
          HttpMethod.POST,
          "/api/acceso/qr/*/dispositivos"
        ).permitAll()

        it.anyRequest().authenticated()
      }
      .httpBasic(Customizer.withDefaults())

    return http.build()
  }
}
