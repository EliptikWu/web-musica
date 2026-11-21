-- El calendario y el horario efectivo (RF-045, RF-036) leen estas columnas. Con IF NOT EXISTS no
-- cambia nada si la base ya las tiene; si faltaran, el estado del bar respondería 500.
ALTER TABLE public.evento_calendario ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE public.evento_calendario ADD COLUMN IF NOT EXISTS fecha DATE;
ALTER TABLE public.evento_calendario ADD COLUMN IF NOT EXISTS hora_inicio TIME;
ALTER TABLE public.evento_calendario ADD COLUMN IF NOT EXISTS hora_fin TIME;
ALTER TABLE public.evento_calendario ADD COLUMN IF NOT EXISTS tipo VARCHAR(60);
