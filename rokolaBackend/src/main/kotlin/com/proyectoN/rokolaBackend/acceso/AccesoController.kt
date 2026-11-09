package com.proyectoN.rokolaBackend.acceso

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/acceso/qr")
class AccesoController(private val acceso: AccesoService) {

    @GetMapping("/{token}")
    fun resolver(@PathVariable token: String): AccesoQrResponse = acceso.resolverQr(token)

    @PostMapping("/{token}/dispositivos")
    @ResponseStatus(HttpStatus.OK)
    fun registrar(
        @PathVariable token: String,
        @RequestBody request: RegistroDispositivoRequest,
    ): RegistroDispositivoResponse = acceso.registrarDispositivo(token, request)
}
