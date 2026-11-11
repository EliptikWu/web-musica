package com.example.rokola.dominio.reglas

import com.example.rokola.dominio.ItemCola

/*
 * Reglas de la cola (apps/web/lib/reglas/cola.ts). El orden lo decide el servidor, pero el
 * voto optimista reordena antes de que llegue la respuesta (RF-012) y tiene que dar el mismo
 * orden (RN-05).
 */

/** RN-05: más votos primero; en empate, la que se pidió antes. */
fun ordenarCola(cola: List<ItemCola>): List<ItemCola> =
    cola.sortedWith(compareByDescending<ItemCola> { it.votosTotal }.thenBy { it.fechaSolicitud })

/** Reasigna el puesto según el orden actual y guarda el anterior para «Subió N puestos». */
fun renumerar(cola: List<ItemCola>): List<ItemCola> =
    ordenarCola(cola).mapIndexed { i, item ->
        val puesto = i + 1
        item.copy(
            puestoAnterior = if (item.puesto != puesto) item.puesto else item.puestoAnterior,
            puesto = puesto,
        )
    }

/** Puestos que subió en el último reordenamiento; 0 si no subió. */
fun puestosSubidos(item: ItemCola): Int {
    val anterior = item.puestoAnterior ?: return 0
    return if (anterior > item.puesto) anterior - item.puesto else 0
}

/** Lo que falta por sonar en la cola, sin la canción actual. */
fun duracionCola(cola: List<ItemCola>): Int = cola.sumOf { it.cancion.duracionSeg }

data class Desempate(val antes: ItemCola, val despues: ItemCola)

/**
 * El caso de desempate que se explica debajo de la cola («Por eso Monaco va antes que La
 * Bicicleta»). Prefiere un empate con una canción propia; si no, el primero de la cola.
 */
fun ejemploDesempate(cola: List<ItemCola>, propias: Set<String> = emptySet()): Desempate? {
    val pares = cola.zipWithNext()
        .filter { (a, b) -> a.votosTotal == b.votosTotal }
        .map { (a, b) -> Desempate(a, b) }
    return pares.firstOrNull { it.antes.solicitudId in propias || it.despues.solicitudId in propias }
        ?: pares.firstOrNull()
}
