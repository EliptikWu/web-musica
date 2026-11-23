-- Ejecutar en pgAdmin después de una carga de Spark.
-- Las canciones que todavía no forman parte del modelo no se tratan como discrepancias.
SELECT
    c.id_cancion,
    c.titulo,
    c.categoria AS genero_operativo,
    d.genero AS genero_analitico
FROM public.cancion c
JOIN analitica.dim_cancion d ON d.id_cancion = c.id_cancion
WHERE (CASE WHEN LOWER(BTRIM(c.categoria)) IN ('music', 'música', 'musica')
            THEN NULL ELSE NULLIF(BTRIM(c.categoria), '') END) IS DISTINCT FROM d.genero
ORDER BY c.id_cancion;

-- Si la primera consulta devuelve cero filas, las dimensiones cargadas están sincronizadas.
-- El cubo se regenera completo en la misma transacción de carga.
SELECT MAX(terminada_en) AS ultima_carga FROM analitica.carga;

-- Cobertura de géneros en el catálogo operativo.
SELECT COUNT(*) AS total,
       COUNT(*) FILTER (WHERE categoria IS NOT NULL) AS con_genero,
       COUNT(*) FILTER (WHERE origen_categoria = 'ETIQUETAS') AS automaticas,
       COUNT(*) FILTER (WHERE origen_categoria = 'MANUAL') AS manuales
FROM public.cancion;
