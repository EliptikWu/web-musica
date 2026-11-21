package com.proyectoN.rokolaBackend.compat

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * Una línea de la bitácora (`accion_admin`, RF-044, RN-11). `detalle` es JSONB: `texto` es lo que
 * lee la bitácora del panel y `objetivo_nombre` el nombre de lo que no es una canción (un
 * usuario, una lista), que no se puede sacar de otra tabla si después se borra o se renombra.
 */
@Component
class ContratoBitacora(private val jdbc: JdbcTemplate) {
  fun anotar(
    p: ContratoPanelAcceso.Personal,
    tipo: String,
    objetivoTipo: String? = null,
    objetivoId: Long? = null,
    objetivoNombre: String? = null,
    texto: String? = null,
  ) {
    jdbc.update(
      """INSERT INTO public.accion_admin (id_establecimiento, id_usuario, tipo, objetivo_tipo, objetivo_id, detalle)
         VALUES (?, ?, ?, ?, ?, jsonb_strip_nulls(jsonb_build_object('texto', ?::text, 'objetivo_nombre', ?::text)))""",
      p.barId, p.id, tipo, objetivoTipo, objetivoId, texto, objetivoNombre,
    )
  }
}
