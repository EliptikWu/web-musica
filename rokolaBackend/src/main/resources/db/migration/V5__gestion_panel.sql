-- Gestión del panel (frontend, 2026-11-21, D14). Revisar con el equipo de backend (doc 11).

-- RF-030 y RF-032: quitar una canción de la lista de la casa, o eliminar la lista, aunque ya
-- haya sonado. Antes la FK de reproduccion.id_elemento lo impedía (y su CHECK exigía un origen).
-- La reproducción pasada se conserva con su canción y su noche; solo pierde el vínculo al
-- elemento que ya no existe. Una reproducción nunca tiene los dos orígenes a la vez.
ALTER TABLE public.reproduccion DROP CONSTRAINT IF EXISTS ck_reproduccion_un_origen;
ALTER TABLE public.reproduccion
  ADD CONSTRAINT ck_reproduccion_un_origen CHECK (NOT (id_solicitud IS NOT NULL AND id_elemento IS NOT NULL));

ALTER TABLE public.reproduccion DROP CONSTRAINT IF EXISTS fk_reproduccion_elemento;
ALTER TABLE public.reproduccion
  ADD CONSTRAINT fk_reproduccion_elemento FOREIGN KEY (id_elemento)
  REFERENCES public.elemento_lista (id_elemento) ON DELETE SET NULL;

-- RF-042 y RF-043: la duración que eligió quien bloqueó. «Hasta el fin de la noche» se guarda
-- con su fecha_fin calculada y esta marca, para mostrarla así y no como «por N horas».
ALTER TABLE public.bloqueo_contenido ADD COLUMN IF NOT EXISTS duracion VARCHAR(12);
ALTER TABLE public.bloqueo_contenido DROP CONSTRAINT IF EXISTS ck_bloqueo_duracion;
ALTER TABLE public.bloqueo_contenido
  ADD CONSTRAINT ck_bloqueo_duracion CHECK (duracion IS NULL OR duracion IN ('fin_sesion', 'horas', 'indefinido'));
