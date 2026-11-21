"""Genera SQL con noches de EJEMPLO para validar el pipeline (guía, semana 8).

Las canciones y artistas son reales; votos, horas y teléfonos son inventados. Los videos llevan
el prefijo `ejemplo-` (no son IDs de YouTube) y por eso el pipeline rotula la carga como
`ejemplo` (RNF-015). Determinista: la misma semilla da las mismas filas.

Uso: python -I sembrar_ejemplo.py demo | psql ...
"""
from __future__ import annotations

import hashlib
import random
import sys
from datetime import datetime, timedelta, timezone

BASE = 900_000  # Los ID de ejemplo viven aparte para poder borrarlos sin tocar nada más.
ZONA = timezone(timedelta(hours=-5))
CANCIONES = [
    ("DtMF", "Bad Bunny", "Reguetón", 237), ("NUEVAYoL", "Bad Bunny", "Reguetón", 183),
    ("Provenza", "KAROL G", "Reguetón", 210), ("TQG", "KAROL G", "Reguetón", 199),
    ("La Bicicleta", "Carlos Vives", "Vallenato", 226), ("Fruta Fresca", "Carlos Vives", "Vallenato", 262),
    ("La Tierra del Olvido", "Carlos Vives", "Vallenato", 268), ("Oye Cómo Va", "Santana", "Rock", 257),
    ("De Música Ligera", "Soda Stereo", "Rock", 211), ("Persiana Americana", "Soda Stereo", "Rock", 288),
    ("Rebelión", "Joe Arroyo", "Salsa", 300), ("La Noche", "Joe Arroyo", "Salsa", 274),
    ("Pedro Navaja", "Rubén Blades", "Salsa", 442), ("Llorarás", "Oscar D'León", "Salsa", 298),
    ("Hips Don't Lie", "Shakira", "Pop", 218), ("Antología", "Shakira", "Pop", 255),
    ("Blinding Lights", "The Weeknd", "Pop", 200), ("Tusa", "KAROL G", None, 201),
]
TIPOS = ["normal", "normal", "normal", "karaoke", "partido", "tematica"]


def q(valor) -> str:
    if valor is None:
        return "NULL"
    if isinstance(valor, bool):
        return "TRUE" if valor else "FALSE"
    if isinstance(valor, int):
        return str(valor)
    if isinstance(valor, datetime):
        return f"'{valor.isoformat()}'"
    return "'" + str(valor).replace("'", "''") + "'"


def fila(tabla: str, columnas: str, valores) -> str:
    return f"INSERT INTO public.{tabla} ({columnas}) VALUES ({', '.join(q(v) for v in valores)});"


def generar(slug: str, semilla: int = 2026) -> list[str]:
    r = random.Random(semilla)
    bar = f"(SELECT id_establecimiento FROM public.establecimiento WHERE slug = {q(slug)})"
    sql = ["BEGIN;"]
    for tabla, clave in [("voto", "id_voto"), ("reproduccion", "id_reproduccion"), ("solicitud", "id_solicitud"),
                         ("sesion", "id_sesion"), ("dispositivo", "id_dispositivo"), ("cancion", "id_cancion")]:
        sql.append(f"DELETE FROM public.{tabla} WHERE {clave} >= {BASE};")

    for i, (titulo, artista, genero, duracion) in enumerate(CANCIONES):
        sql.append(fila("cancion", "id_cancion, youtube_video_id, titulo, artista, canal, categoria, duracion_seg, categoria_youtube, embebible, disponibilidad",
                        [BASE + i, f"ejemplo-{i:04d}", titulo, artista, artista, genero, duracion, "Music", True, "disponible"]))
    for i in range(140):
        huella = hashlib.sha256(f"ejemplo-{i}".encode()).hexdigest()
        sql.append(f"INSERT INTO public.dispositivo (id_dispositivo, id_establecimiento, device_hash, tipo) VALUES ({BASE + i}, {bar}, {q(huella)}, 'movil');")

    id_sol = id_voto = id_rep = BASE
    dia = datetime(2026, 10, 1, 20, 0, tzinfo=ZONA)
    noches = 0
    while noches < 20:
        if dia.isoweekday() not in (4, 5, 6):
            dia += timedelta(days=1)
            continue
        id_sesion = BASE + noches
        apertura, cierre = dia, dia + timedelta(hours=6)
        tipo = r.choice(TIPOS)
        sql.append(f"INSERT INTO public.sesion (id_sesion, id_establecimiento, fecha_apertura, fecha_cierre, tipo_evento, estado) VALUES ({id_sesion}, {bar}, {q(apertura)}, {q(cierre)}, {q(tipo)}, 'cerrada');")
        publico = r.sample(range(140), r.randint(12 if noches % 7 == 3 else 24, 70))
        reloj = apertura + timedelta(minutes=r.randint(2, 15))
        pedidas = []
        for _ in range(r.randint(25, 80)):
            minuto = min(355, max(1, int(r.gauss(190, 75))))
            fecha = apertura + timedelta(minutes=minuto)
            cancion = BASE + min(len(CANCIONES) - 1, int(r.expovariate(0.16)))
            estado = r.choices(["reproducida", "saltada", "expirada", "bloqueada", "retirada", "no_disponible"], [74, 7, 11, 3, 3, 2])[0]
            sql.append(fila("solicitud", "id_solicitud, id_sesion, id_cancion, id_dispositivo_solicitante, fecha_solicitud, estado",
                            [id_sol, id_sesion, cancion, BASE + r.choice(publico), fecha, estado]))
            pedidas.append((id_sol, cancion, fecha, estado))
            votantes = r.sample(publico, min(len(publico), int(r.expovariate(0.22))))
            for votante in votantes:
                sql.append(fila("voto", "id_voto, id_solicitud, id_dispositivo, fecha_voto, activo",
                                [id_voto, id_sol, BASE + votante, fecha + timedelta(minutes=r.randint(1, 25)), True]))
                id_voto += 1
            id_sol += 1
        pedidas.sort(key=lambda p: p[2])
        for sol, cancion, fecha, estado in pedidas:
            if estado not in ("reproducida", "saltada") or reloj >= cierre:
                continue
            reloj = max(reloj, fecha + timedelta(minutes=r.randint(2, 18)))
            if reloj >= cierre:
                break
            duracion = CANCIONES[cancion - BASE][3]
            segundos = duracion if estado == "reproducida" else r.randint(15, duracion // 2)
            resultado = "completada" if estado == "reproducida" else "saltada"
            sql.append(fila("reproduccion", "id_reproduccion, id_sesion, id_cancion, id_solicitud, fecha_inicio, fecha_fin, segundos_reproducidos, resultado",
                            [id_rep, id_sesion, cancion, sol, reloj, reloj + timedelta(seconds=segundos), segundos, resultado]))
            id_rep += 1
            reloj += timedelta(seconds=segundos)
            if r.random() < .3:
                casa = BASE + r.randrange(len(CANCIONES))
                segundos = CANCIONES[casa - BASE][3]
                sql.append(fila("reproduccion", "id_reproduccion, id_sesion, id_cancion, fecha_inicio, fecha_fin, segundos_reproducidos, resultado",
                                [id_rep, id_sesion, casa, reloj, reloj + timedelta(seconds=segundos), segundos, "completada"]))
                id_rep += 1
                reloj += timedelta(seconds=segundos)
        noches += 1
        dia += timedelta(days=1)
    sql.append("COMMIT;")
    return sql


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    print("\n".join(generar(sys.argv[1] if len(sys.argv) > 1 else "demo")))
