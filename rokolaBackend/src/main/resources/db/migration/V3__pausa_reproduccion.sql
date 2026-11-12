-- RF-040 (frontend, 2026-11-12, D14): pausar y reanudar lo que suena desde el panel. Antes el
-- backend no guardaba la pausa y siempre informaba «sonando»: la barra de avance del teléfono y
-- de la TV seguía corriendo con la música detenida.
--   pausada_desde      cuándo empezó la pausa en curso (NULL = sonando)
--   segundos_en_pausa  pausas ya terminadas de esta reproducción; se restan del reloj
ALTER TABLE public.reproduccion
  ADD COLUMN IF NOT EXISTS pausada_desde TIMESTAMPTZ NULL,
  ADD COLUMN IF NOT EXISTS segundos_en_pausa INTEGER NOT NULL DEFAULT 0;
