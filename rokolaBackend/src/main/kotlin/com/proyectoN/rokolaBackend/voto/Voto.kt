package com.proyectoN.rokolaBackend.voto

import com.proyectoN.rokolaBackend.dispositivo.Dispositivo
import com.proyectoN.rokolaBackend.solicitud.Solicitud
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

/** Un voto asociado a una solicitud y a un dispositivo específico. */
@Entity
@Table(name = "voto", schema = "public")
class Voto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_voto")
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false)
    var solicitud: Solicitud,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dispositivo", nullable = false)
    var dispositivo: Dispositivo,

    @Column(name = "fecha_voto", nullable = false)
    var fechaVoto: OffsetDateTime,

    @Column(name = "activo", nullable = false)
    var activo: Boolean,

    @Column(name = "fecha_retiro")
    var fechaRetiro: OffsetDateTime? = null,
)
