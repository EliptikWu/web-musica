"""Bronce (RF-047): copia las fuentes a Parquet, con el teléfono ya seudonimizado.

El `device_hash` se reemplaza dentro de la ingesta: ni el hash ni `id_dispositivo` llegan al
disco del pipeline (RF-048, RNF-007). Solo entran noches cerradas.
"""
from __future__ import annotations

import hashlib
import hmac
import os
import shutil

from pyspark.sql import DataFrame, SparkSession
from pyspark.sql import functions as F
from pyspark.sql.types import StringType

from .config import Config

CERRADAS = "s.estado = 'cerrada' AND s.fecha_cierre IS NOT NULL"

CONSULTAS = {
    "sesiones": f"""
        SELECT s.id_sesion, s.id_establecimiento, s.fecha_apertura, s.fecha_cierre, s.tipo_evento,
               s.id_evento_calendario, ev.nombre AS evento_nombre, ev.tipo_evento AS evento_tipo,
               e.zona_horaria
        FROM public.sesion s
        JOIN public.establecimiento e ON e.id_establecimiento = s.id_establecimiento
        LEFT JOIN public.evento_calendario ev ON ev.id_evento_calendario = s.id_evento_calendario
        WHERE {CERRADAS}""",
    "solicitudes": f"""
        SELECT so.id_solicitud, so.id_sesion, s.id_establecimiento, so.id_cancion, d.device_hash,
               so.fecha_solicitud, so.estado
        FROM public.solicitud so
        JOIN public.sesion s ON s.id_sesion = so.id_sesion
        LEFT JOIN public.dispositivo d ON d.id_dispositivo = so.id_dispositivo_solicitante
        WHERE {CERRADAS}""",
    "votos": f"""
        SELECT v.id_voto, so.id_sesion, s.id_establecimiento, v.id_solicitud, d.device_hash,
               v.fecha_voto
        FROM public.voto v
        JOIN public.solicitud so ON so.id_solicitud = v.id_solicitud
        JOIN public.sesion s ON s.id_sesion = so.id_sesion
        LEFT JOIN public.dispositivo d ON d.id_dispositivo = v.id_dispositivo
        WHERE v.activo AND {CERRADAS}""",
    "reproducciones": f"""
        SELECT r.id_reproduccion, r.id_sesion, s.id_establecimiento, r.id_cancion, r.id_solicitud,
               r.fecha_inicio, r.fecha_fin, r.segundos_reproducidos, r.resultado
        FROM public.reproduccion r
        JOIN public.sesion s ON s.id_sesion = r.id_sesion
        WHERE {CERRADAS}""",
    "eventos": f"""
        SELECT ea.id_evento, ea.id_sesion, ea.id_establecimiento, ea.nombre_evento, ea.device_hash,
               ea.fecha_evento
        FROM public.evento_analitico ea
        JOIN public.sesion s ON s.id_sesion = ea.id_sesion
        WHERE {CERRADAS}""",
}

# Metadatos de YouTube: se reescriben enteros en cada ejecución.
CANCIONES = """
    SELECT id_cancion, youtube_video_id, titulo, artista, canal, categoria, duracion_seg, miniatura_url
    FROM public.cancion"""


def seudonimizador(clave: str):
    """HMAC-SHA256 truncado a 128 bits. Sin la clave no se puede volver ni comparar con la base."""
    llave = clave.encode("utf-8")

    def seudonimo(valor: str | None) -> str | None:
        if valor is None:
            return None
        return hmac.new(llave, valor.encode("utf-8"), hashlib.sha256).hexdigest()[:32]

    return F.udf(seudonimo, StringType())


def _leer(spark: SparkSession, cfg: Config, consulta: str) -> DataFrame:
    return spark.read.jdbc(cfg.jdbc_url, f"({consulta}) AS fuente", properties=cfg.propiedades)


def _noches_a_ingerir(spark: SparkSession, cfg: Config, sesion: int) -> list[int]:
    """La noche pedida y las cerradas que bronce todavía no tiene: el modelo siempre va completo."""
    cerradas = {int(f.id_sesion) for f in _leer(spark, cfg, f"SELECT s.id_sesion FROM public.sesion s WHERE {CERRADAS}").collect()}
    if int(sesion) not in cerradas:
        raise SystemExit(f"La noche {int(sesion)} no existe o no está cerrada")
    carpeta = f"{cfg.datos}/bronce/sesiones"
    presentes = {int(d.split("=", 1)[1]) for d in os.listdir(carpeta) if d.startswith("id_sesion=")} if os.path.isdir(carpeta) else set()
    return sorted({int(sesion)} | (cerradas - presentes))


def ingerir(spark: SparkSession, cfg: Config, sesion: int | None = None) -> dict[str, int]:
    """Escribe bronce. Con `sesion` reemplaza esa noche y trae las cerradas que aún no estén."""
    seudonimo = seudonimizador(cfg.clave_seudonimo)
    objetivo = _noches_a_ingerir(spark, cfg, sesion) if sesion is not None else None
    modo_particion = "dynamic" if objetivo is not None else "static"
    spark.conf.set("spark.sql.sources.partitionOverwriteMode", modo_particion)
    leidos: dict[str, int] = {}

    for nombre, consulta in CONSULTAS.items():
        if objetivo is not None:
            lista = ",".join(str(n) for n in objetivo)
            consulta = f"{consulta} AND s.id_sesion IN ({lista})"
            # El modo dinámico no borra una noche que ahora viene sin filas en esta fuente.
            for noche in objetivo:
                shutil.rmtree(f"{cfg.datos}/bronce/{nombre}/id_sesion={noche}", ignore_errors=True)
        df = _leer(spark, cfg, consulta)
        if "device_hash" in df.columns:
            df = df.withColumn("dispositivo_seud", seudonimo("device_hash")).drop("device_hash")
        df = df.cache()
        leidos[nombre] = df.count()
        df.write.mode("overwrite").partitionBy("id_sesion").parquet(f"{cfg.datos}/bronce/{nombre}")
        # Una fuente sin filas no deja Parquet del que Spark pueda deducir el esquema.
        os.makedirs(f"{cfg.datos}/esquemas", exist_ok=True)
        with open(f"{cfg.datos}/esquemas/{nombre}.json", "w", encoding="utf-8") as archivo:
            archivo.write(df.schema.json())
        df.unpersist()

    canciones = _leer(spark, cfg, CANCIONES).cache()
    leidos["canciones"] = canciones.count()
    canciones.write.mode("overwrite").parquet(f"{cfg.datos}/bronce/canciones")
    canciones.unpersist()
    return leidos
