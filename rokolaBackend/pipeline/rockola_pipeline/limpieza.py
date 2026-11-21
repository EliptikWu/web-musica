"""Plata (RF-048): quita duplicados y separa lo inválido, contando cada motivo."""
from __future__ import annotations

import json
import os
from collections.abc import Sequence

from pyspark.sql import Column, DataFrame, SparkSession
from pyspark.sql import functions as F
from pyspark.sql.types import StructType

ESTADOS_SOLICITUD = ["pendiente", "reproduciendo", "reproducida", "saltada", "no_disponible", "bloqueada", "expirada", "retirada"]
RESULTADOS = ["iniciada", "completada", "saltada", "fallida"]


def seudonimo_valido() -> Column:
    """Una función y no una constante: una columna solo se puede crear con Spark ya iniciado."""
    return F.col("dispositivo_seud").rlike("^[0-9a-f]{32}$")


def filtrar(df: DataFrame, clave: str, reglas: Sequence[tuple[str, Column]]) -> tuple[DataFrame, dict[str, int]]:
    """Aplica las reglas en orden. Una condición nula cuenta como inválida."""
    total = df.count()
    limpio = df.dropDuplicates([clave])
    rechazos = {"duplicado": total - limpio.count()}
    for motivo, valida in reglas:
        condicion = F.coalesce(valida, F.lit(False))
        rechazos[motivo] = limpio.filter(~condicion).count()
        limpio = limpio.filter(condicion)
    return limpio, {k: v for k, v in rechazos.items() if v > 0}


def _existe(df: DataFrame, otra: DataFrame, columna: str, columna_otra: str | None = None) -> DataFrame:
    destino = columna_otra or columna
    return df.join(otra.select(F.col(destino).alias(columna)).distinct(), columna, "left_semi")


def _huerfanos(df: DataFrame, otra: DataFrame, columna: str, motivo: str, rechazos: dict[str, int]) -> DataFrame:
    antes = df.count()
    filtrado = _existe(df, otra, columna)
    faltan = antes - filtrado.count()
    if faltan:
        rechazos[motivo] = rechazos.get(motivo, 0) + faltan
    return filtrado


def leer_bronce(spark: SparkSession, datos: str, nombre: str) -> DataFrame:
    esquema = f"{datos}/esquemas/{nombre}.json"
    if not os.path.exists(esquema):  # `canciones` no va por noches: siempre trae su esquema
        return spark.read.parquet(f"{datos}/bronce/{nombre}")
    with open(esquema, encoding="utf-8") as archivo:
        return spark.read.schema(StructType.fromJson(json.load(archivo))).parquet(f"{datos}/bronce/{nombre}")


def conciliar(spark: SparkSession, datos: str, plata: dict[str, DataFrame]) -> dict:
    """Lo que la limpieza quitó, noche por noche, y los teléfonos que deben llegar al modelo."""
    por_noche = lambda df: {int(f.id_sesion): int(f["count"]) for f in df.groupBy("id_sesion").count().collect()}
    quitados: dict[str, dict[int, int]] = {}
    for nombre in ("solicitudes", "votos", "reproducciones"):
        antes, despues = por_noche(leer_bronce(spark, datos, nombre)), por_noche(plata[nombre])
        quitados[nombre] = {n: antes[n] - despues.get(n, 0) for n in antes if antes[n] != despues.get(n, 0)}
    participantes = plata["solicitudes"].select("id_sesion", "dispositivo_seud").unionByName(plata["votos"].select("id_sesion", "dispositivo_seud"))
    telefonos = {int(f.id_sesion): int(f.n) for f in participantes.groupBy("id_sesion").agg(F.countDistinct("dispositivo_seud").alias("n")).collect()}
    return {"quitados": quitados, "telefonos": telefonos}


def limpiar(spark: SparkSession, datos: str) -> tuple[dict[str, DataFrame], dict[str, dict[str, int]]]:
    leer = lambda nombre: leer_bronce(spark, datos, nombre)
    rechazos: dict[str, dict[str, int]] = {}

    sesiones, rechazos["sesiones"] = filtrar(leer("sesiones"), "id_sesion", [
        ("fechas_invertidas", F.col("fecha_cierre") >= F.col("fecha_apertura")),
        ("sin_zona_horaria", F.length(F.trim("zona_horaria")) > 0),
    ])
    canciones, rechazos["canciones"] = filtrar(leer("canciones"), "id_cancion", [
        ("sin_video", F.length(F.trim("youtube_video_id")) > 0),
        ("sin_titulo", F.length(F.trim("titulo")) > 0),
        ("duracion_invalida", F.col("duracion_seg") >= 0),
    ])

    solicitudes, rechazos["solicitudes"] = filtrar(leer("solicitudes"), "id_solicitud", [
        ("sin_dispositivo", seudonimo_valido()),
        ("sin_fecha", F.col("fecha_solicitud").isNotNull()),
        ("estado_desconocido", F.col("estado").isin(ESTADOS_SOLICITUD)),
    ])
    solicitudes = _huerfanos(solicitudes, sesiones, "id_sesion", "sesion_invalida", rechazos["solicitudes"])
    solicitudes = _huerfanos(solicitudes, canciones, "id_cancion", "cancion_invalida", rechazos["solicitudes"])

    votos, rechazos["votos"] = filtrar(leer("votos"), "id_voto", [
        ("sin_dispositivo", seudonimo_valido()),
        ("sin_fecha", F.col("fecha_voto").isNotNull()),
    ])
    votos = _huerfanos(votos, solicitudes, "id_solicitud", "solicitud_invalida", rechazos["votos"])

    reproducciones, rechazos["reproducciones"] = filtrar(leer("reproducciones"), "id_reproduccion", [
        ("sin_inicio", F.col("fecha_inicio").isNotNull()),
        ("resultado_desconocido", F.col("resultado").isin(RESULTADOS)),
        ("segundos_negativos", F.col("segundos_reproducidos") >= 0),
    ])
    reproducciones = _huerfanos(reproducciones, sesiones, "id_sesion", "sesion_invalida", rechazos["reproducciones"])
    reproducciones = _huerfanos(reproducciones, canciones, "id_cancion", "cancion_invalida", rechazos["reproducciones"])

    eventos, rechazos["eventos"] = filtrar(leer("eventos"), "id_evento", [
        ("sin_nombre", F.length(F.trim("nombre_evento")) > 0),
        ("sin_fecha", F.col("fecha_evento").isNotNull()),
    ])

    limpias = {"sesiones": sesiones, "canciones": canciones, "solicitudes": solicitudes,
               "votos": votos, "reproducciones": reproducciones, "eventos": eventos}
    for nombre, df in limpias.items():
        df.write.mode("overwrite").parquet(f"{datos}/plata/{nombre}")
    return ({n: spark.read.parquet(f"{datos}/plata/{n}") for n in limpias},
            {n: r for n, r in rechazos.items() if r})
