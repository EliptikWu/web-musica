# Pipeline analítico de Rockola

Convierte los registros de la aplicación en el modelo que lee el tablero (RF-047, RF-048,
RF-063, ADR-27). Corre con PySpark en la imagen oficial `apache/spark:4.0.1-python3`.

| Capa | Dónde | Qué tiene |
|---|---|---|
| Bronce | volumen `rockola-analitica`, `/datos/bronce` | Copia de solicitudes, votos, reproducciones, sesiones, eventos y canciones de las noches cerradas. El teléfono ya va seudonimizado |
| Plata | `/datos/plata` | Sin duplicados ni registros inválidos. Cada rechazo se cuenta con su motivo |
| Oro | esquema `analitica` de Postgres | Modelo en estrella (`dim_noche`, `dim_cancion`, `hecho_*`) y `cubo_participacion` con los KPI |

## Ejecutar

Necesita Docker, la migración `V6` aplicada y estas variables de entorno:

| Variable | Para qué |
|---|---|
| `ANALITICA_JDBC_URL` | Base de datos, vista desde el contenedor |
| `DB_USERNAME`, `DB_PASSWORD` | Credenciales de esa base |
| `ROCKOLA_SEUDONIMO_CLAVE` | Clave HMAC de al menos 32 caracteres. Guárdala fuera de la base: si cambia, los teléfonos dejan de coincidir entre cargas |
| `ROCKOLA_DOCKER_RED` | Opcional: red de Docker de la base |

```bash
bash rokolaBackend/pipeline/ejecutar.sh                # todas las noches cerradas
bash rokolaBackend/pipeline/ejecutar.sh --sesion 123   # vuelve a ingerir una noche
```

Repetir una ejecución no duplica nada: el modelo se reemplaza en una sola transacción. Si la
auditoría no aprueba, la transacción se revierte y el modelo anterior queda intacto.

## Evidencia para el Entregable 6

Cada ejecución deja una fila en `analitica.carga` y un JSON en `/datos/reportes` con lo leído,
lo rechazado y la auditoría:

- **Cifras:** solicitudes, votos, reproducciones y teléfonos únicos, comparados noche por noche
  contra las tablas operativas.
- **Anonimización:** ni `device_hash` ni `id_dispositivo` salen de la ingesta, y la auditoría
  comprueba que ningún seudónimo coincida con un hash de la base.

## Datos de ejemplo

`sembrar_ejemplo.py` genera 20 noches para validar el pipeline en una base de prueba. Canciones
y artistas son reales; votos, horas y teléfonos son inventados. Sus videos llevan el prefijo
`ejemplo-`, y por eso la carga y el tablero se rotulan como datos de ejemplo. No lo uses en la
base del piloto: sus cifras se mezclarían con las reales.

## Pruebas

```bash
MSYS_NO_PATHCONV=1 docker run --rm --user 0 -e PYTHONPATH=/pipeline -e HOME=/tmp \
  -v "$(pwd -W 2>/dev/null || pwd)/rokolaBackend/pipeline:/pipeline:ro" apache/spark:4.0.1-python3 \
  /opt/spark/bin/spark-submit --master 'local[2]' /pipeline/pruebas/test_pipeline.py
```
