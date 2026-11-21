-- Modelo analítico (RF-047 a RF-054, RF-063, ADR-27). Lo llena el pipeline de Spark
-- (rokolaBackend/pipeline); el backend solo lo lee. `analitica_stg` es el área de paso: Spark
-- escribe ahí y una sola transacción pasa los datos a `analitica`.
CREATE SCHEMA IF NOT EXISTS analitica;
CREATE SCHEMA IF NOT EXISTS analitica_stg;

-- Constancia de cada ejecución (RF-047): cuántos registros leyó y cuántos rechazó, y por qué.
CREATE TABLE analitica.carga (
    id_carga    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iniciada_en TIMESTAMPTZ NOT NULL,
    terminada_en TIMESTAMPTZ NOT NULL,
    origen      VARCHAR(12) NOT NULL CHECK (origen IN ('registrado', 'ejemplo')),
    alcance     VARCHAR(40) NOT NULL,
    leidos      JSONB       NOT NULL,
    rechazados  JSONB       NOT NULL,
    auditoria   JSONB       NOT NULL
);

CREATE TABLE analitica.dim_noche (
    id_sesion          BIGINT PRIMARY KEY,
    id_establecimiento BIGINT      NOT NULL,
    inicio             TIMESTAMPTZ NOT NULL,
    fin                TIMESTAMPTZ NOT NULL,
    fecha_local        DATE        NOT NULL,
    dia_semana         SMALLINT    NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    id_evento          BIGINT,
    evento_nombre      VARCHAR(150),
    tipo_evento        VARCHAR(20) NOT NULL
);
CREATE INDEX idx_dim_noche_bar_fecha ON analitica.dim_noche (id_establecimiento, fecha_local);

CREATE TABLE analitica.dim_cancion (
    id_cancion   BIGINT PRIMARY KEY,
    video_id     VARCHAR(30)  NOT NULL,
    titulo       VARCHAR(300) NOT NULL,
    artista      VARCHAR(200) NOT NULL,
    duracion_seg INTEGER      NOT NULL,
    miniatura_url TEXT,
    genero       VARCHAR(100)  -- NULL: «sin clasificar» (RF-051)
);

CREATE TABLE analitica.hecho_solicitud (
    id_solicitud       BIGINT PRIMARY KEY,
    id_establecimiento BIGINT      NOT NULL,
    id_sesion          BIGINT      NOT NULL REFERENCES analitica.dim_noche (id_sesion),
    id_cancion         BIGINT      NOT NULL REFERENCES analitica.dim_cancion (id_cancion),
    dispositivo_seud   CHAR(32)    NOT NULL,
    fecha_solicitud    TIMESTAMPTZ NOT NULL,
    hora_local         SMALLINT    NOT NULL,
    estado             VARCHAR(25) NOT NULL,
    votos              INTEGER     NOT NULL,
    inicio_reproduccion TIMESTAMPTZ,
    espera_seg         INTEGER,
    causa_no_sono      VARCHAR(20)
);
CREATE INDEX idx_hecho_solicitud_sesion ON analitica.hecho_solicitud (id_sesion);

CREATE TABLE analitica.hecho_voto (
    id_voto            BIGINT PRIMARY KEY,
    id_establecimiento BIGINT      NOT NULL,
    id_sesion          BIGINT      NOT NULL REFERENCES analitica.dim_noche (id_sesion),
    id_solicitud       BIGINT      NOT NULL REFERENCES analitica.hecho_solicitud (id_solicitud),
    dispositivo_seud   CHAR(32)    NOT NULL,
    fecha_voto         TIMESTAMPTZ NOT NULL,
    hora_local         SMALLINT    NOT NULL
);
CREATE INDEX idx_hecho_voto_sesion ON analitica.hecho_voto (id_sesion);

CREATE TABLE analitica.hecho_reproduccion (
    id_reproduccion    BIGINT PRIMARY KEY,
    id_establecimiento BIGINT      NOT NULL,
    id_sesion          BIGINT      NOT NULL REFERENCES analitica.dim_noche (id_sesion),
    id_cancion         BIGINT      NOT NULL REFERENCES analitica.dim_cancion (id_cancion),
    id_solicitud       BIGINT,
    origen             VARCHAR(12) NOT NULL CHECK (origen IN ('solicitud', 'lista_casa')),
    inicio             TIMESTAMPTZ NOT NULL,
    segundos           INTEGER     NOT NULL,
    votos              INTEGER     NOT NULL
);
CREATE INDEX idx_hecho_reproduccion_sesion ON analitica.hecho_reproduccion (id_sesion, inicio);

-- El cubo (RF-063): cada celda guarda los KPI del Entregable 6. '*' es el miembro «Todos» de
-- cada eje. Teléfonos, esperas y canción más popular se calculan desde los hechos en cada celda:
-- no se pueden obtener sumando otras celdas.
CREATE TABLE analitica.cubo_participacion (
    id_establecimiento BIGINT       NOT NULL,
    eje                VARCHAR(6)   NOT NULL CHECK (eje IN ('dia', 'evento')),
    genero             VARCHAR(100) NOT NULL,
    franja             VARCHAR(20)  NOT NULL,
    tercero            VARCHAR(20)  NOT NULL,
    solicitudes        INTEGER      NOT NULL,
    votos              INTEGER      NOT NULL,
    telefonos          INTEGER      NOT NULL,
    no_sonaron         INTEGER      NOT NULL,
    espera_promedio_seg INTEGER,
    espera_maxima_seg  INTEGER,
    pedidos_seg        BIGINT       NOT NULL,
    lista_casa_seg     BIGINT       NOT NULL,
    id_cancion_popular BIGINT REFERENCES analitica.dim_cancion (id_cancion),
    votos_popular      INTEGER      NOT NULL,
    solicitudes_popular INTEGER     NOT NULL,
    PRIMARY KEY (id_establecimiento, eje, genero, franja, tercero)
);

-- Umbrales del diagnóstico (RF-052). No los toca el pipeline.
CREATE TABLE analitica.umbrales (
    id_establecimiento   BIGINT PRIMARY KEY REFERENCES public.establecimiento (id_establecimiento) ON DELETE CASCADE,
    votos_minimos        INTEGER NOT NULL CHECK (votos_minimos BETWEEN 1 AND 500),
    concentracion_pct    INTEGER NOT NULL CHECK (concentracion_pct BETWEEN 10 AND 100),
    participacion_minima INTEGER NOT NULL CHECK (participacion_minima BETWEEN 1 AND 1000)
);
