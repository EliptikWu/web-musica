-- La categoría musical ya existe en public.cancion como columna categoria.
-- Solo se agrega el origen para distinguir una estimación de la revisión humana.
-- No se reetiquetan canciones existentes sin evidencia.
ALTER TABLE public.cancion
  ADD COLUMN IF NOT EXISTS origen_categoria VARCHAR(20);

ALTER TABLE public.cancion DROP CONSTRAINT IF EXISTS ck_cancion_origen_categoria;
ALTER TABLE public.cancion
  ADD CONSTRAINT ck_cancion_origen_categoria
  CHECK (origen_categoria IN ('ETIQUETAS', 'MANUAL'));
