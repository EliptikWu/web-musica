#!/usr/bin/env bash
# Corre el pipeline en la imagen oficial de Spark (RF-047, RF-048, ADR-27).
# Requiere: ANALITICA_JDBC_URL, DB_USERNAME, DB_PASSWORD y ROCKOLA_SEUDONIMO_CLAVE.
# Opcional: ROCKOLA_DOCKER_RED (red de Docker de la base). Argumentos: [--sesion <id>]
set -euo pipefail

aqui="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -W 2>/dev/null || pwd)"
red=()
if [ -n "${ROCKOLA_DOCKER_RED:-}" ]; then red=(--network "$ROCKOLA_DOCKER_RED"); fi

# Las claves pasan por nombre: docker las toma del entorno y no quedan en la línea de comandos.
MSYS_NO_PATHCONV=1 docker run --rm --user 0 "${red[@]}" \
  -e ANALITICA_JDBC_URL -e DB_USERNAME -e DB_PASSWORD -e ROCKOLA_SEUDONIMO_CLAVE \
  -e ANALITICA_DATOS=/datos -e PYTHONPATH=/pipeline -e HOME=/tmp \
  -v "$aqui:/pipeline:ro" -v rockola-analitica:/datos \
  apache/spark:4.0.1-python3 \
  /opt/spark/bin/spark-submit --master 'local[*]' \
    --packages org.postgresql:postgresql:42.7.4 \
    --conf spark.jars.ivy=/tmp/.ivy \
    --conf spark.driver.extraJavaOptions=-Duser.timezone=UTC \
    --conf spark.executor.extraJavaOptions=-Duser.timezone=UTC \
    /pipeline/ejecutar.py "$@"
