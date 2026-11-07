package com.proyectoN.rokolaBackend.establecimiento

import org.springframework.data.jpa.repository.JpaRepository

/** Acceso a la tabla `establecimiento`. Spring Data genera la implementación. */
interface EstablecimientoRepository : JpaRepository<Establecimiento, Long>
