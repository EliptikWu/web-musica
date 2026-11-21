-- La noche y el servicio (RF-036 a RF-039, RN-09). Doc 04, pendiente 20.

-- `forzado_abierto`: el bar atiende aunque esté fuera del horario, hasta que lo cierren.
-- `sin_apertura_hasta`: la noche se cerró a mano dentro de una franja y no se reabre sola hasta
-- que esa franja (y las que sigan pegadas a ella) termine.
ALTER TABLE public.configuracion_servicio ADD COLUMN IF NOT EXISTS forzado_abierto BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE public.configuracion_servicio ADD COLUMN IF NOT EXISTS sin_apertura_hasta TIMESTAMPTZ;

-- Cómo se abrió la noche. Lo anterior a esta migración solo se pudo abrir a mano.
ALTER TABLE public.sesion ADD COLUMN IF NOT EXISTS origen VARCHAR(10) NOT NULL DEFAULT 'manual';
ALTER TABLE public.sesion DROP CONSTRAINT IF EXISTS ck_sesion_origen;
ALTER TABLE public.sesion ADD CONSTRAINT ck_sesion_origen CHECK (origen IN ('horario', 'manual'));

-- `fuera_de_horario` (V7) pasa a ser del bar: una noche abierta así sigue forzada.
UPDATE public.configuracion_servicio c SET forzado_abierto = TRUE
WHERE EXISTS (
  SELECT 1 FROM public.sesion s
  WHERE s.id_establecimiento = c.id_establecimiento AND s.estado = 'abierta' AND s.fuera_de_horario
);
ALTER TABLE public.sesion DROP COLUMN IF EXISTS fuera_de_horario;
