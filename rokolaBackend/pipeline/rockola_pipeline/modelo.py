"""Oro (RF-048, RF-063): modelo en estrella y cubo de participación."""
from __future__ import annotations

from pyspark.sql import Column, DataFrame, SparkSession, Window
from pyspark.sql import functions as F

TODOS = "*"
SIN_CLASIFICAR = "Sin clasificar"
OTROS = "Otros"
TIPOS_EVENTO = ["normal", "karaoke", "partido", "tematica", "otro"]
ETIQUETA_EVENTO = {"normal": "Normal", "karaoke": "Karaoke", "partido": "Partido", "tematica": "Temática", "otro": "Otro"}
# La categoría de YouTube casi siempre es «Música»: eso no es un género (doc 04, pendiente 3).
NO_ES_GENERO = ["music", "música", "musica"]


def franja(hora: Column) -> Column:
    """Cuatro tramos de la noche, en la hora local del bar."""
    return (F.when(hora.isin(21, 22), "9 a 11 p. m.")
            .when(hora.isin(23, 0), "11 p. m. a 1 a. m.")
            .when(hora.between(1, 5), "Después de la 1")
            .otherwise("Antes de las 9"))


def dia_iso(fecha: Column) -> Column:
    """1 = lunes … 7 = domingo (Spark usa 1 = domingo)."""
    return ((F.dayofweek(fecha) + 5) % 7 + 1).cast("short")


def dia_cubo(dia: Column) -> Column:
    return (F.when(dia == 4, "Jueves").when(dia == 5, "Viernes").when(dia == 6, "Sábado").otherwise("Otros días"))


def construir(spark: SparkSession, plata: dict[str, DataFrame]) -> dict[str, DataFrame]:
    sesiones, canciones = plata["sesiones"], plata["canciones"]
    zonas = sesiones.select("id_sesion", "zona_horaria")

    tipo = F.lower(F.trim(F.coalesce("evento_tipo", "tipo_evento", F.lit("normal"))))
    dim_noche = sesiones.select(
        "id_sesion", "id_establecimiento",
        F.col("fecha_apertura").alias("inicio"), F.col("fecha_cierre").alias("fin"),
        F.to_date(F.from_utc_timestamp("fecha_apertura", F.col("zona_horaria"))).alias("fecha_local"),
        dia_iso(F.from_utc_timestamp("fecha_apertura", F.col("zona_horaria"))).alias("dia_semana"),
        F.col("id_evento_calendario").alias("id_evento"), "evento_nombre",
        F.when(tipo.isin(TIPOS_EVENTO), tipo).otherwise("otro").alias("tipo_evento"),
    )

    categoria = F.trim("categoria")
    dim_cancion = canciones.select(
        "id_cancion", F.col("youtube_video_id").alias("video_id"), "titulo",
        F.coalesce(F.nullif(F.trim("artista"), F.lit("")), F.nullif(F.trim("canal"), F.lit("")), F.lit("Sin artista")).alias("artista"),
        "duracion_seg", F.when(F.col("miniatura_url").startswith("https://"), F.col("miniatura_url")).alias("miniatura_url"),
        F.when((F.length(categoria) > 0) & ~F.lower(categoria).isin(NO_ES_GENERO), categoria).alias("genero"),
    )

    votos_por_solicitud = plata["votos"].groupBy("id_solicitud").agg(F.count("*").cast("int").alias("votos"))
    # Un video que falló no empezó a sonar para el público: no cuenta para la espera.
    primera = (plata["reproducciones"].filter(F.col("id_solicitud").isNotNull() & (F.col("resultado") != "fallida"))
               .groupBy("id_solicitud").agg(F.min("fecha_inicio").alias("inicio_reproduccion")))
    # La causa sale del estado final: una canción saltada a mitad también «no sonó» (doc 04).
    causa = (F.when(F.col("estado").isin("reproducida", "reproduciendo"), F.lit(None))
             .when(F.col("estado") == "saltada", "omitida")
             .when(F.col("estado") == "bloqueada", "bloqueada")
             .when(F.col("estado") == "retirada", "retirada")
             .when(F.col("estado") == "no_disponible", "no_disponible")
             .otherwise("fuera_de_tiempo"))
    hecho_solicitud = (plata["solicitudes"].join(zonas, "id_sesion")
        .join(votos_por_solicitud, "id_solicitud", "left").join(primera, "id_solicitud", "left")
        .select(
            "id_solicitud", "id_establecimiento", "id_sesion", "id_cancion", "dispositivo_seud", "fecha_solicitud",
            F.hour(F.from_utc_timestamp("fecha_solicitud", F.col("zona_horaria"))).cast("short").alias("hora_local"),
            "estado", F.coalesce("votos", F.lit(0)).alias("votos"), "inicio_reproduccion",
            # `greatest` ignora nulos: sin este `when`, lo que nunca sonó quedaría con espera 0.
            F.when(F.col("inicio_reproduccion").isNotNull(), F.greatest(F.lit(0), F.col("inicio_reproduccion").cast("long") - F.col("fecha_solicitud").cast("long"))).cast("int").alias("espera_seg"),
            causa.alias("causa_no_sono"),
        ))

    hecho_voto = plata["votos"].join(zonas, "id_sesion").select(
        "id_voto", "id_establecimiento", "id_sesion", "id_solicitud", "dispositivo_seud", "fecha_voto",
        F.hour(F.from_utc_timestamp("fecha_voto", F.col("zona_horaria"))).cast("short").alias("hora_local"),
    )

    hecho_reproduccion = (plata["reproducciones"].join(votos_por_solicitud, "id_solicitud", "left").select(
        "id_reproduccion", "id_establecimiento", "id_sesion", "id_cancion", "id_solicitud",
        F.when(F.col("id_solicitud").isNotNull(), "solicitud").otherwise("lista_casa").alias("origen"),
        F.col("fecha_inicio").alias("inicio"), F.col("segundos_reproducidos").alias("segundos"),
        F.coalesce("votos", F.lit(0)).alias("votos"),
    ))

    reproducidas = hecho_reproduccion.join(zonas, "id_sesion").withColumn(
        "hora_local", F.hour(F.from_utc_timestamp("inicio", F.col("zona_horaria"))))
    cubo = construir_cubo(spark, dim_noche, dim_cancion, hecho_solicitud, hecho_voto, reproducidas)
    return {"dim_noche": dim_noche, "dim_cancion": dim_cancion, "hecho_solicitud": hecho_solicitud,
            "hecho_voto": hecho_voto, "hecho_reproduccion": hecho_reproduccion, "cubo_participacion": cubo}


def _generos_del_cubo(solicitudes: DataFrame, canciones: DataFrame) -> DataFrame:
    """Los tres géneros más pedidos de cada bar; el resto va a «Otros» para que el cubo se pueda leer."""
    base = solicitudes.join(canciones.select("id_cancion", "genero"), "id_cancion").withColumn(
        "genero_real", F.coalesce("genero", F.lit(SIN_CLASIFICAR)))
    conteo = base.groupBy("id_establecimiento", "genero_real").count()
    orden = Window.partitionBy("id_establecimiento").orderBy(F.desc("count"), "genero_real")
    return conteo.withColumn("puesto", F.row_number().over(orden)).select(
        "id_establecimiento", "genero_real",
        F.when(F.col("puesto") <= 3, F.col("genero_real")).otherwise(OTROS).alias("genero_cubo"))


def _eventos_del_cubo(noches: DataFrame) -> DataFrame:
    """Los tres tipos de evento con más noches de cada bar; el resto va a «Otros»."""
    conteo = noches.groupBy("id_establecimiento", "tipo_evento").count()
    orden = Window.partitionBy("id_establecimiento").orderBy(F.desc("count"), "tipo_evento")
    etiqueta = F.create_map(*[F.lit(x) for par in ETIQUETA_EVENTO.items() for x in par])
    return conteo.withColumn("puesto", F.row_number().over(orden)).select(
        "id_establecimiento", "tipo_evento",
        F.when(F.col("puesto") <= 3, etiqueta[F.col("tipo_evento")]).otherwise(OTROS).alias("evento_cubo"))


def construir_cubo(spark: SparkSession, noches: DataFrame, canciones: DataFrame, solicitudes: DataFrame,
                   votos: DataFrame, reproducciones: DataFrame) -> DataFrame:
    """Una fila por hecho (pedido, voto o reproducción) y un CUBE con los KPI del Entregable 6."""
    generos = _generos_del_cubo(solicitudes, canciones)
    eventos = _eventos_del_cubo(noches)
    # Una canción de la lista de la casa puede tener un género que nadie pidió: va a «Otros».
    genero_de_cancion = (canciones.select("id_cancion", "genero")
        .withColumn("genero_real", F.coalesce("genero", F.lit(SIN_CLASIFICAR)))
        .crossJoin(noches.select("id_establecimiento").distinct())
        .join(generos, ["id_establecimiento", "genero_real"], "left")
        .select("id_establecimiento", "id_cancion", F.coalesce("genero_cubo", F.lit(OTROS)).alias("genero_cubo")))

    vacio = {"es_solicitud": 0, "es_voto": 0, "no_sono": 0, "pedidos_seg": 0, "lista_casa_seg": 0}
    def hecho(df: DataFrame, **valores) -> DataFrame:
        columnas = {**vacio, **valores}
        return df.select(
            "id_establecimiento", "id_sesion", "id_cancion", "hora_local", "dispositivo_seud", "espera_seg",
            *[(v if isinstance(v, Column) else F.lit(v)).cast("long").alias(k) for k, v in columnas.items()])

    pedidos = hecho(solicitudes, es_solicitud=1, no_sono=F.col("causa_no_sono").isNotNull().cast("int"))
    votados = hecho(
        votos.join(solicitudes.select("id_solicitud", "id_cancion"), "id_solicitud")
        .withColumn("espera_seg", F.lit(None).cast("int")), es_voto=1)
    sonadas = hecho(
        reproducciones.withColumn("dispositivo_seud", F.lit(None).cast("string"))
        .withColumn("espera_seg", F.lit(None).cast("int")),
        pedidos_seg=F.when(F.col("origen") == "solicitud", F.col("segundos")).otherwise(0),
        lista_casa_seg=F.when(F.col("origen") == "lista_casa", F.col("segundos")).otherwise(0))

    participacion = (pedidos.unionByName(votados).unionByName(sonadas)
        .join(genero_de_cancion, ["id_establecimiento", "id_cancion"])
        .join(noches.select("id_sesion", "dia_semana", "tipo_evento"), "id_sesion")
        .join(eventos, ["id_establecimiento", "tipo_evento"])
        .withColumn("franja", franja(F.col("hora_local")))
        .withColumn("dia_cubo", dia_cubo(F.col("dia_semana"))))
    participacion.createOrReplaceTempView("participacion")

    def cubo_de(eje: str, columna: str) -> DataFrame:
        celda = ["id_establecimiento", "genero", "franja", "tercero"]
        ejes = f"""COALESCE(genero_cubo, '{TODOS}') AS genero, COALESCE(franja, '{TODOS}') AS franja,
                   COALESCE({columna}, '{TODOS}') AS tercero"""
        # CUBE calcula cada subtotal desde los hechos: conteo distinto y promedios quedan exactos.
        kpis = spark.sql(f"""
            SELECT id_establecimiento, '{eje}' AS eje, {ejes},
                   CAST(SUM(es_solicitud) AS INT) AS solicitudes,
                   CAST(SUM(es_voto) AS INT) AS votos,
                   CAST(COUNT(DISTINCT dispositivo_seud) AS INT) AS telefonos,
                   CAST(SUM(no_sono) AS INT) AS no_sonaron,
                   CAST(ROUND(AVG(espera_seg)) AS INT) AS espera_promedio_seg,
                   MAX(espera_seg) AS espera_maxima_seg,
                   SUM(pedidos_seg) AS pedidos_seg,
                   SUM(lista_casa_seg) AS lista_casa_seg
            FROM participacion
            GROUP BY id_establecimiento, CUBE(genero_cubo, franja, {columna})""")
        # La más popular de cada celda: más votos; si empatan, más pedidos; si no, el id menor.
        por_cancion = spark.sql(f"""
            SELECT id_establecimiento, id_cancion, {ejes},
                   CAST(SUM(es_voto) AS INT) AS votos_popular,
                   CAST(SUM(es_solicitud) AS INT) AS solicitudes_popular
            FROM participacion WHERE es_voto = 1 OR es_solicitud = 1
            GROUP BY id_establecimiento, id_cancion, CUBE(genero_cubo, franja, {columna})""")
        orden = Window.partitionBy(*celda).orderBy(F.desc("votos_popular"), F.desc("solicitudes_popular"), "id_cancion")
        popular = (por_cancion.withColumn("puesto", F.row_number().over(orden)).filter("puesto = 1")
                   .select(*celda, F.col("id_cancion").alias("id_cancion_popular"), "votos_popular", "solicitudes_popular"))
        return (kpis.join(popular, celda, "left")
                .withColumn("votos_popular", F.coalesce("votos_popular", F.lit(0)))
                .withColumn("solicitudes_popular", F.coalesce("solicitudes_popular", F.lit(0))))

    return cubo_de("dia", "dia_cubo").unionByName(cubo_de("evento", "evento_cubo"))
