-- RF-059, ADR-25 (frontend, 2026-11-12): el diseño con que el teléfono y la TV ven la rockola
-- del bar. Lo cambia solo el Administrador (PATCH /api/panel/establecimiento).
-- Es la primera migración del repositorio: la base existente queda como versión 1
-- (spring.flyway.baseline-on-migrate=true, baseline-version=1).
ALTER TABLE public.establecimiento
  ADD COLUMN IF NOT EXISTS tema VARCHAR(10) NOT NULL DEFAULT 'rockola';

ALTER TABLE public.establecimiento DROP CONSTRAINT IF EXISTS establecimiento_tema_check;
ALTER TABLE public.establecimiento
  ADD CONSTRAINT establecimiento_tema_check CHECK (tema IN ('rockola', 'vidrio', 'afiche'));
