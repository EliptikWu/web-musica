package com.proyectoN.rokolaBackend.solicitud

import com.proyectoN.rokolaBackend.cancion.Cancion
import com.proyectoN.rokolaBackend.dispositivo.Dispositivo
import com.proyectoN.rokolaBackend.sesion.Sesion
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

/** Una solicitud musical realizada en una sesión por un dispositivo. */
@Entity
@Table(name = "solicitud", schema = "public")
class Solicitud(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sesion", nullable = false)
    var sesion: Sesion,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cancion", nullable = false)
    var cancion: Cancion,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dispositivo_solicitante", nullable = false)
    var dispositivoSolicitante: Dispositivo,

    @Column(name = "fecha_solicitud", nullable = false)
    var fechaSolicitud: OffsetDateTime,

    @Column(name = "estado", nullable = false, length = 25)
    var estado: EstadoSolicitud,

    @Column(name = "motivo_estado", columnDefinition = "text")
    var motivoEstado: String? = null,
)
