-- RF-062 (frontend, 2026-11-12, D14): volumen de la pantalla del bar, de 0 (silencio) a 100.
-- Lo fija el panel (PUT /api/panel/volumen) y la TV lo aplica al reproductor.
ALTER TABLE public.configuracion_servicio
  ADD COLUMN IF NOT EXISTS volumen INTEGER NOT NULL DEFAULT 80;

ALTER TABLE public.configuracion_servicio DROP CONSTRAINT IF EXISTS configuracion_servicio_volumen_check;
ALTER TABLE public.configuracion_servicio
  ADD CONSTRAINT configuracion_servicio_volumen_check CHECK (volumen BETWEEN 0 AND 100);
