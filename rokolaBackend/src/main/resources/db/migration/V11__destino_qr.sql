-- A dónde lleva el QR del bar (RF-065, ADR-30): a la web o a descargar la app. Cambia sin
-- reimprimir: el código impreso sigue siendo el mismo.
ALTER TABLE public.establecimiento ADD COLUMN IF NOT EXISTS destino_qr VARCHAR(10) NOT NULL DEFAULT 'web';
ALTER TABLE public.establecimiento DROP CONSTRAINT IF EXISTS ck_establecimiento_destino_qr;
ALTER TABLE public.establecimiento ADD CONSTRAINT ck_establecimiento_destino_qr CHECK (destino_qr IN ('web', 'app'));
