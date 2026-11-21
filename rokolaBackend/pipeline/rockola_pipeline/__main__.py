"""Ejecuta el pipeline: ingesta → limpieza → modelo → carga y auditoría."""
from __future__ import annotations

import argparse
import json
import os
from datetime import datetime, timezone

from pyspark.sql import SparkSession

from . import carga, config, ingesta, limpieza, modelo


def main() -> None:
    parser = argparse.ArgumentParser(description="Pipeline analítico de Rockola")
    parser.add_argument("--sesion", type=int, help="Reingesta solo esta noche; sin él, todas las cerradas")
    parser.add_argument("--excluir", type=int, action="append", default=[], metavar="ID",
                        help="Noche que no entra al análisis (repetible); no cambia la base operativa")
    args = parser.parse_args()
    excluir = frozenset(args.excluir)
    if args.sesion in excluir:
        parser.error("--sesion y --excluir no pueden ser la misma noche")

    cfg = config.desde_entorno()
    iniciada = datetime.now(timezone.utc)
    spark = (SparkSession.builder.appName("rockola-analitica")
             .config("spark.sql.session.timeZone", "UTC")
             .config("spark.sql.shuffle.partitions", "8")
             .getOrCreate())
    spark.sparkContext.setLogLevel("WARN")
    try:
        leidos = ingesta.ingerir(spark, cfg, args.sesion, excluir)
        plata, rechazados = limpieza.limpiar(spark, cfg.datos)
        oro = {n: df.cache() for n, df in modelo.construir(spark, plata).items()}
        alcance = f"sesion:{args.sesion}" if args.sesion is not None else "todas"
        if excluir:
            alcance = f"{alcance} sin {','.join(str(n) for n in sorted(excluir))}"[:40]
        conciliacion = limpieza.conciliar(spark, cfg.datos, plata)
        resultado = carga.cargar(spark, cfg, oro, leidos, rechazados, conciliacion, alcance, iniciada)
        os.makedirs(f"{cfg.datos}/reportes", exist_ok=True)
        nombre = iniciada.strftime("%Y%m%dT%H%M%SZ")
        with open(f"{cfg.datos}/reportes/carga-{nombre}.json", "w", encoding="utf-8") as archivo:
            json.dump(resultado, archivo, ensure_ascii=False, indent=2)
        print(json.dumps(resultado, ensure_ascii=False, indent=2))
    finally:
        spark.stop()


if __name__ == "__main__":
    main()
