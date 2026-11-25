-- Último ingreso al panel de cada cuenta (RF-046, punto 16 del equipo, 2026-11-25).
-- Sin esta columna, «Personal» decía «Todavía no ha entrado» de todas las cuentas.
-- Las cuentas que ya existen quedan en NULL hasta su próximo ingreso.
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS ultimo_acceso TIMESTAMPTZ;
