-- Registrar la procedencia de la detección automática sin afectar las canciones existentes.
-- V12 ya creó ck_cancion_origen_categoria; no modificar una migración aplicada.
ALTER TABLE public.cancion DROP CONSTRAINT IF EXISTS ck_cancion_origen_categoria;
ALTER TABLE public.cancion
  ADD CONSTRAINT ck_cancion_origen_categoria
  CHECK (origen_categoria IN ('ETIQUETAS', 'TITULO', 'DESCRIPCION', 'MANUAL'));
