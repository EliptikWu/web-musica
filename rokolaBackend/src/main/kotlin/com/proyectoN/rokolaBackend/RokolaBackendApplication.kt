package com.proyectoN.rokolaBackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RokolaBackendApplication

fun main(args: Array<String>) {
	runApplication<RokolaBackendApplication>(*args)
  println("bip bip")
}
