"""Carga el oro a Postgres en una sola transacción y audita contra los registros operativos."""
from __future__ import annotations

import json
from datetime import datetime, timezone

from pyspark.sql import DataFrame, SparkSession

from .config import Config

# Orden de inserción: las dimensiones antes que los hechos que las referencian.
TABLAS = ["dim_noche", "dim_cancion", "hecho_solicitud", "hecho_voto", "hecho_reproduccion", "cubo_participacion"]

# Cada indicador se calcula por separado en la base operativa y en el modelo, por noche.
AUDITORIA = {
    "solicitudes": (
        """SELECT so.id_sesion, COUNT(*) FROM public.solicitud so JOIN public.sesion s ON s.id_sesion = so.id_sesion
           WHERE s.estado = 'cerrada' AND s.fecha_cierre IS NOT NULL GROUP BY 1""",
        "SELECT id_sesion, COUNT(*) FROM analitica.hecho_solicitud GROUP BY 1",
    ),
    "votos": (
        """SELECT so.id_sesion, COUNT(*) FROM public.voto v JOIN public.solicitud so ON so.id_solicitud = v.id_solicitud
           JOIN public.sesion s ON s.id_sesion = so.id_sesion
           WHERE v.activo AND s.estado = 'cerrada' AND s.fecha_cierre IS NOT NULL GROUP BY 1""",
        "SELECT id_sesion, COUNT(*) FROM analitica.hecho_voto GROUP BY 1",
    ),
    "reproducciones": (
        """SELECT r.id_sesion, COUNT(*) FROM public.reproduccion r JOIN public.sesion s ON s.id_sesion = r.id_sesion
           WHERE s.estado = 'cerrada' AND s.fecha_cierre IS NOT NULL GROUP BY 1""",
        "SELECT id_sesion, COUNT(*) FROM analitica.hecho_reproduccion GROUP BY 1",
    ),
    "telefonos": (
        """SELECT id_sesion, COUNT(DISTINCT id_dispositivo) FROM (
             SELECT so.id_sesion, so.id_dispositivo_solicitante AS id_dispositivo FROM public.solicitud so
             UNION ALL
             SELECT so.id_sesion, v.id_dispositivo FROM public.voto v JOIN public.solicitud so ON so.id_solicitud = v.id_solicitud WHERE v.activo
           ) p JOIN public.sesion s USING (id_sesion)
           WHERE s.estado = 'cerrada' AND s.fecha_cierre IS NOT NULL GROUP BY 1""",
        """SELECT id_sesion, COUNT(DISTINCT dispositivo_seud) FROM (
             SELECT id_sesion, dispositivo_seud FROM analitica.hecho_solicitud
             UNION ALL SELECT id_sesion, dispositivo_seud FROM analitica.hecho_voto
           ) p GROUP BY 1""",
    ),
}

FUGAS = """
    SELECT COUNT(*) FROM public.dispositivo d WHERE EXISTS (
      SELECT 1 FROM analitica.hecho_solicitud h WHERE h.dispositivo_seud = LEFT(d.device_hash, 32)
         OR h.dispositivo_seud = d.device_hash
    ) OR EXISTS (
      SELECT 1 FROM analitica.hecho_voto h WHERE h.dispositivo_seud = LEFT(d.device_hash, 32)
         OR h.dispositivo_seud = d.device_hash
    )"""


def _conexion(spark: SparkSession, cfg: Config):
    jvm = spark._jvm
    propiedades = jvm.java.util.Properties()
    propiedades.setProperty("user", cfg.usuario)
    propiedades.setProperty("password", cfg.clave)
    return jvm.org.postgresql.Driver().connect(cfg.jdbc_url, propiedades)


def _por_noche(conexion, consulta: str) -> dict[int, int]:
    sentencia = conexion.createStatement()
    try:
        filas = sentencia.executeQuery(consulta)
        resultado: dict[int, int] = {}
        while filas.next():
            resultado[int(filas.getLong(1))] = int(filas.getLong(2))
        return resultado
    finally:
        sentencia.close()


def _escalar(conexion, consulta: str) -> int:
    sentencia = conexion.createStatement()
    try:
        filas = sentencia.executeQuery(consulta)
        filas.next()
        return int(filas.getLong(1))
    finally:
        sentencia.close()


def noches_distintas(nombre: str, operativa: dict[int, int], modelo: dict[int, int], conciliacion: dict) -> list[int]:
    """Las noches que no cuadran. Cada noche solo tolera lo que la limpieza quitó en esa noche."""
    quitados = conciliacion["quitados"]
    noches = set(operativa) | set(modelo)
    if "noches" in conciliacion:
        noches &= conciliacion["noches"]
    if nombre != "telefonos":
        tolerado = quitados.get(nombre, {})
        return sorted(n for n in noches if operativa.get(n, 0) - modelo.get(n, 0) != tolerado.get(n, 0))
    # Con rechazos, los únicos esperados son los de los registros limpios de esa noche.
    con_rechazo = set(quitados.get("solicitudes", {})) | set(quitados.get("votos", {}))
    limpios = conciliacion["telefonos"]
    return sorted(n for n in noches if modelo.get(n, 0) != (limpios.get(n, 0) if n in con_rechazo else operativa.get(n, 0)))


def auditar(conexion, conciliacion: dict) -> dict:
    """Compara noche por noche contra la base operativa."""
    indicadores: dict[str, dict] = {}
    for nombre, (operativa, modelo) in AUDITORIA.items():
        a, b = _por_noche(conexion, operativa), _por_noche(conexion, modelo)
        a = {n: v for n, v in a.items() if n in conciliacion["noches"]}
        distintas = noches_distintas(nombre, a, b, conciliacion)
        indicadores[nombre] = {
            "operativa": sum(a.values()), "modelo": sum(b.values()), "noches_revisadas": len(set(a) | set(b)),
            "noches_distintas": distintas[:20], "coincide": not distintas,
        }
    fugas = _escalar(conexion, FUGAS)
    return {"indicadores": indicadores, "seudonimos_iguales_al_hash": fugas,
            "aprobada": fugas == 0 and all(i["coincide"] for i in indicadores.values())}


def cargar(spark: SparkSession, cfg: Config, oro: dict[str, DataFrame], leidos: dict, rechazados: dict,
           conciliacion: dict, alcance: str, iniciada: datetime) -> dict:
    for tabla in TABLAS:
        (oro[tabla].write.mode("overwrite").option("truncate", "false")
         .jdbc(cfg.jdbc_url, f"analitica_stg.{tabla}", properties=cfg.propiedades))

    es_ejemplo = oro["dim_cancion"].filter("video_id LIKE 'ejemplo-%'").limit(1).count() > 0
    origen = "ejemplo" if es_ejemplo else "registrado"

    conexion = _conexion(spark, cfg)
    try:
        conexion.setAutoCommit(False)
        sentencia = conexion.createStatement()
        sentencia.execute("SET LOCAL TIME ZONE 'UTC'")
        sentencia.execute("TRUNCATE " + ", ".join(f"analitica.{t}" for t in TABLAS))
        for tabla in TABLAS:
            columnas = ", ".join(f'"{c}"' for c in oro[tabla].columns)
            sentencia.execute(f"INSERT INTO analitica.{tabla} ({columnas}) SELECT {columnas} FROM analitica_stg.{tabla}")
        auditoria = auditar(conexion, conciliacion)
        if not auditoria["aprobada"]:
            conexion.rollback()
            raise SystemExit("La auditoría no aprobó; el modelo anterior queda intacto:\n" + json.dumps(auditoria, ensure_ascii=False, indent=2))
        registro = conexion.prepareStatement(
            """INSERT INTO analitica.carga (iniciada_en, terminada_en, origen, alcance, leidos, rechazados, auditoria)
               VALUES (?::timestamptz, now(), ?, ?, ?::jsonb, ?::jsonb, ?::jsonb)""")
        registro.setString(1, iniciada.astimezone(timezone.utc).isoformat())
        registro.setString(2, origen)
        registro.setString(3, alcance)
        registro.setString(4, json.dumps(leidos))
        registro.setString(5, json.dumps(rechazados))
        registro.setString(6, json.dumps(auditoria))
        registro.executeUpdate()
        conexion.commit()
    except Exception:
        conexion.rollback()
        raise
    finally:
        conexion.close()
    return {"origen": origen, "alcance": alcance, "leidos": leidos, "rechazados": rechazados, "auditoria": auditoria}
