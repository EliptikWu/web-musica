-- Semilla de la prueba de carga (doc 07, análisis del 2026-11-25, #7; doc 09, «Rendimiento»).
-- SOLO para una base local de prueba cargada con rokolaBackend/src/test/resources/esquema-reconstruido.sql
-- y con la noche ya abierta por el horario (el backend la abre solo al arrancar: el esquema trae
-- los 7 días de 00:00 a 23:59). Nunca contra una base real.
--
-- Pone 20 pedidos pendientes en la noche abierta, de un teléfono de la casa, para que los teléfonos
-- de la prueba tengan por qué votar. Las canciones y los artistas son reales; los IDs de video no
-- (`carga-NN`): la reproducción automática va apagada en la prueba y no se buscan en YouTube.

INSERT INTO public.cancion (youtube_video_id, titulo, artista, duracion_seg, embebible, disponibilidad)
VALUES
  ('carga-01', 'Rebelión', 'Joe Arroyo', 358, TRUE, 'disponible'),
  ('carga-02', 'Cali Pachanguero', 'Grupo Niche', 352, TRUE, 'disponible'),
  ('carga-03', 'DtMF', 'Bad Bunny', 237, TRUE, 'disponible'),
  ('carga-04', 'Monaco', 'Bad Bunny', 267, TRUE, 'disponible'),
  ('carga-05', 'La Bicicleta', 'Carlos Vives y Shakira', 227, TRUE, 'disponible'),
  ('carga-06', 'NUEVAYoL', 'Bad Bunny', 183, TRUE, 'disponible'),
  ('carga-07', 'TQG', 'Karol G y Shakira', 197, TRUE, 'disponible'),
  ('carga-08', 'Luna', 'Feid y ATL Jacob', 196, TRUE, 'disponible'),
  ('carga-09', 'Una aventura', 'Grupo Niche', 285, TRUE, 'disponible'),
  ('carga-10', 'Tití Me Preguntó', 'Bad Bunny', 243, TRUE, 'disponible'),
  ('carga-11', 'La gota fría', 'Carlos Vives', 260, TRUE, 'disponible'),
  ('carga-12', 'Hips Don''t Lie', 'Shakira', 218, TRUE, 'disponible'),
  ('carga-13', 'Despechá', 'Rosalía', 157, TRUE, 'disponible'),
  ('carga-14', 'La Bachata', 'Manuel Turizo', 162, TRUE, 'disponible'),
  ('carga-15', 'Gasolina', 'Daddy Yankee', 192, TRUE, 'disponible'),
  ('carga-16', 'Mi gente', 'Héctor Lavoe', 335, TRUE, 'disponible'),
  ('carga-17', 'La noche', 'Joe Arroyo', 300, TRUE, 'disponible'),
  ('carga-18', 'Micaela', 'Sonora Carruseles', 250, TRUE, 'disponible'),
  ('carga-19', 'Provenza', 'Karol G', 210, TRUE, 'disponible'),
  ('carga-20', 'Que alguien me diga', 'Gilberto Santa Rosa', 290, TRUE, 'disponible')
ON CONFLICT (youtube_video_id) DO NOTHING;

-- device_hash es un SHA-256 en hexadecimal, como el de un teléfono real (el panel saca de ahí el seudónimo).
INSERT INTO public.dispositivo (id_establecimiento, device_hash, tipo)
SELECT id_establecimiento, encode(sha256('carga-telefono-de-la-casa'::bytea), 'hex'), 'movil' FROM public.establecimiento WHERE slug = 'demo'
ON CONFLICT (id_establecimiento, device_hash) DO NOTHING;

INSERT INTO public.solicitud (id_sesion, id_cancion, id_dispositivo_solicitante, estado)
SELECT s.id_sesion, c.id_cancion, d.id_dispositivo, 'pendiente'
FROM public.sesion s
JOIN public.establecimiento e ON e.id_establecimiento = s.id_establecimiento AND e.slug = 'demo'
JOIN public.dispositivo d ON d.id_establecimiento = e.id_establecimiento AND d.device_hash = encode(sha256('carga-telefono-de-la-casa'::bytea), 'hex')
CROSS JOIN public.cancion c
WHERE s.estado = 'abierta' AND c.youtube_video_id LIKE 'carga-%';
