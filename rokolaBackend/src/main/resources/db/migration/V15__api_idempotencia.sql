-- Claves de idempotencia de pedir, votar, mover el voto y los reportes de la pantalla (doc 04,
-- «Pendiente del backend», punto 12; backlog T9, 2026-11-25). El código la usaba desde antes, pero
-- solo existía en la base de Railway, creada a mano el 2026-11-10 con esta misma forma: por eso
-- IF NOT EXISTS, que no toca la tabla si ya está. Cuánto se guardan las claves es TBD; la fecha y
-- su índice sirven para borrar las viejas.
CREATE TABLE IF NOT EXISTS public.api_idempotencia (
    id_establecimiento BIGINT       NOT NULL REFERENCES public.establecimiento (id_establecimiento) ON DELETE CASCADE,
    device_hash        VARCHAR(64)  NOT NULL,
    clave              UUID         NOT NULL,
    operacion          VARCHAR(40)  NOT NULL,
    huella_peticion    VARCHAR(64)  NOT NULL,
    respuesta_http     INTEGER      NOT NULL,
    respuesta_json     TEXT         NOT NULL,
    fecha_creacion     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_establecimiento, device_hash, clave)
);
CREATE INDEX IF NOT EXISTS idx_api_idempotencia_fecha ON public.api_idempotencia (fecha_creacion);
