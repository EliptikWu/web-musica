package com.proyectoN.rokolaBackend.usuario

import org.springframework.data.jpa.repository.JpaRepository

/** Acceso a la tabla `rol`. El tipo del id es [Short] porque la columna es `smallint`. */
interface RolRepository : JpaRepository<Rol, Short>
