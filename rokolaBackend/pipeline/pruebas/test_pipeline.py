"""Pruebas del pipeline. Corren en la imagen de Spark (ver pipeline/README.md)."""
import unittest
from datetime import datetime

from pyspark.sql import SparkSession
from pyspark.sql import functions as F

from rockola_pipeline import carga, ingesta, limpieza, modelo


class Pipeline(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.spark = (SparkSession.builder.master("local[2]").config("spark.sql.session.timeZone", "UTC")
                     .config("spark.sql.shuffle.partitions", "2").getOrCreate())

    @classmethod
    def tearDownClass(cls):
        cls.spark.stop()

    def test_seudonimo_estable_con_clave_y_distinto_sin_ella(self):
        df = self.spark.createDataFrame([("abc",), (None,)], ["h"])
        a = df.select(ingesta.seudonimizador("k" * 32)("h").alias("s")).collect()
        b = df.select(ingesta.seudonimizador("otra" * 8)("h").alias("s")).collect()
        self.assertRegex(a[0].s, "^[0-9a-f]{32}$")
        self.assertNotEqual(a[0].s, b[0].s)
        self.assertNotIn("abc", a[0].s)
        self.assertIsNone(a[1].s)

    def test_excluir_noches_agrega_el_filtro_solo_si_hay(self):
        self.assertEqual(ingesta.sin_excluidas("SELECT 1 WHERE x", frozenset()), "SELECT 1 WHERE x")
        self.assertEqual(ingesta.sin_excluidas("SELECT 1 WHERE x", frozenset({7, 2})),
                         "SELECT 1 WHERE x AND s.id_sesion NOT IN (2,7)")

    def test_filtrar_quita_duplicados_y_cuenta_cada_motivo(self):
        df = self.spark.createDataFrame([(1, 5), (1, 5), (2, -1), (3, None)], ["id", "x"])
        limpio, rechazos = limpieza.filtrar(df, "id", [("negativo", F.col("x") >= 0)])
        self.assertEqual([r.id for r in limpio.collect()], [1])
        self.assertEqual(rechazos, {"duplicado": 1, "negativo": 2})

    def test_auditoria_tolera_solo_lo_quitado_en_cada_noche(self):
        conciliacion = {"quitados": {"votos": {1: 1}}, "telefonos": {1: 4, 2: 9}}
        self.assertEqual(carga.noches_distintas("votos", {1: 5, 2: 7}, {1: 4, 2: 7}, conciliacion), [])
        # La diferencia cae en otra noche: la suma cuadra, pero la auditoría no aprueba.
        self.assertEqual(carga.noches_distintas("votos", {1: 5, 2: 7}, {1: 5, 2: 6}, conciliacion), [1, 2])
        # Un rechazo en la noche 1 no disculpa teléfonos perdidos en la noche 2.
        self.assertEqual(carga.noches_distintas("telefonos", {1: 5, 2: 9}, {1: 4, 2: 8}, conciliacion), [2])
        # Una noche que cerró después de la ingesta no cuenta como diferencia.
        self.assertEqual(carga.noches_distintas("votos", {1: 4, 3: 9}, {1: 4}, {"quitados": {}, "telefonos": {}, "noches": {1}}), [])

    def test_franjas_de_la_noche(self):
        df = self.spark.createDataFrame([(20,), (21,), (22,), (23,), (0,), (1,), (5,), (12,)], ["h"])
        franjas = [r.f for r in df.select(modelo.franja(F.col("h")).alias("f")).collect()]
        self.assertEqual(franjas, ["Antes de las 9", "9 a 11 p. m.", "9 a 11 p. m.", "11 p. m. a 1 a. m.", "11 p. m. a 1 a. m.",
                                   "Después de la 1", "Después de la 1", "Antes de las 9"])

    def test_cubo_conserva_cuatro_generos_y_dias_reales(self):
        """Ningún género cae en «Otros» por estar fuera del top 3."""
        s = self.spark
        noches = s.createDataFrame([(1, 10, 1, "normal"), (2, 10, 7, "normal")],
                                  ["id_sesion", "id_establecimiento", "dia_semana", "tipo_evento"])
        canciones = s.createDataFrame([(100, "Salsa"), (101, "Rock"),
                                       (102, "Pop"), (103, "Bachata")], ["id_cancion", "genero"])
        solicitudes = s.createDataFrame([
            (1, 10, 1, 100, "a" * 32, 21, 15, None),
            (2, 10, 1, 101, "b" * 32, 21, 15, None),
            (3, 10, 2, 102, "c" * 32, 21, 15, None),
            (4, 10, 2, 103, "d" * 32, 21, 15, None),
        ], "id_solicitud long, id_establecimiento long, id_sesion long, id_cancion long, "
           "dispositivo_seud string, hora_local int, espera_seg int, causa_no_sono string")
        votos = s.createDataFrame([], "id_voto long, id_establecimiento long, id_sesion long, "
                                    "id_solicitud long, dispositivo_seud string, hora_local int")
        reproducciones = s.createDataFrame([], "id_reproduccion long, id_establecimiento long, "
                    "id_sesion long, id_cancion long, id_solicitud long, origen string, inicio timestamp, "
                    "segundos int, hora_local int")
        cubo = modelo.construir_cubo(s, noches, canciones, solicitudes, votos, reproducciones)
        completas = cubo.filter("eje = 'dia' AND franja = '*' AND tercero = '*'")
        miembros = {fila.genero for fila in completas.collect() if fila.genero != '*'}
        self.assertEqual(miembros, {"Salsa", "Rock", "Pop", "Bachata"})
        dias = {fila.tercero for fila in cubo.filter("eje = 'dia' AND genero = '*' AND franja = '*'").collect()}
        self.assertTrue({"Lunes", "Domingo"}.issubset(dias))
        self.assertNotIn("Otros días", dias)

    def test_corregir_genero_se_refleja_en_el_cubo_al_reprocesar(self):
        """Una nueva carga recompone las agrupaciones, no conserva el género anterior."""
        s = self.spark
        noches = s.createDataFrame([(1, 10, 5, "normal")], ["id_sesion", "id_establecimiento", "dia_semana", "tipo_evento"])
        solicitudes = s.createDataFrame(
            [(1, 10, 1, 100, "a" * 32, 21, 15, None)],
            "id_solicitud long, id_establecimiento long, id_sesion long, id_cancion long, dispositivo_seud string, hora_local int, espera_seg int, causa_no_sono string")
        votos = s.createDataFrame([], "id_voto long, id_establecimiento long, id_sesion long, id_solicitud long, dispositivo_seud string, hora_local int")
        reproducciones = s.createDataFrame([], "id_reproduccion long, id_establecimiento long, id_sesion long, id_cancion long, id_solicitud long, origen string, inicio timestamp, segundos int, hora_local int")

        def cubo(genero):
            canciones = s.createDataFrame([(100, genero)], ["id_cancion", "genero"])
            return modelo.construir_cubo(s, noches, canciones, solicitudes, votos, reproducciones)

        anterior = cubo("Pop").filter("eje = 'dia' AND genero = 'Pop' AND franja = '*' AND tercero = '*'").first()
        actualizado = cubo("Salsa").filter("eje = 'dia' AND genero = 'Salsa' AND franja = '*' AND tercero = '*'").first()
        desaparecio = cubo("Salsa").filter("eje = 'dia' AND genero = 'Pop' AND franja = '*' AND tercero = '*'").count()
        self.assertEqual(anterior.solicitudes, 1)
        self.assertEqual(actualizado.solicitudes, 1)
        self.assertEqual(desaparecio, 0)

    def test_cubo_no_suma_usuarios_unicos_y_espera_es_de_la_celda(self):
        s = self.spark
        inicio = datetime(2026, 10, 2, 2, 0)  # jueves 1 de octubre, 9 p. m. en Bogotá
        noches = s.createDataFrame([(1, 10, 4, "normal")], ["id_sesion", "id_establecimiento", "dia_semana", "tipo_evento"])
        canciones = s.createDataFrame([(100, "Salsa"), (200, "Pop")], ["id_cancion", "genero"])
        solicitudes = s.createDataFrame(
            [(1, 10, 1, 100, "a" * 32, 21, 60, None), (2, 10, 1, 200, "a" * 32, 21, 180, None), (3, 10, 1, 200, "b" * 32, 21, None, "omitida")],
            "id_solicitud long, id_establecimiento long, id_sesion long, id_cancion long, dispositivo_seud string, hora_local int, espera_seg int, causa_no_sono string")
        votos = s.createDataFrame([(1, 10, 1, 1, "b" * 32, 21)],
                                  "id_voto long, id_establecimiento long, id_sesion long, id_solicitud long, dispositivo_seud string, hora_local int")
        reproducciones = s.createDataFrame(
            [(1, 10, 1, 100, 1, "solicitud", inicio, 75, 21), (2, 10, 1, 100, None, "lista_casa", inicio, 25, 21)],
            "id_reproduccion long, id_establecimiento long, id_sesion long, id_cancion long, id_solicitud long, origen string, inicio timestamp, segundos int, hora_local int")

        cubo = modelo.construir_cubo(s, noches, canciones, solicitudes, votos, reproducciones).filter("eje = 'dia'")
        total = cubo.filter("genero = '*' AND franja = '*' AND tercero = '*'").first()
        salsa = cubo.filter("genero = 'Salsa' AND franja = '*' AND tercero = '*'").first()
        pop = cubo.filter("genero = 'Pop' AND franja = '*' AND tercero = '*'").first()

        self.assertEqual((salsa.telefonos, pop.telefonos, total.telefonos), (2, 2, 2))  # no 4
        self.assertEqual((total.solicitudes, total.votos, total.no_sonaron), (3, 1, 1))
        self.assertEqual((total.espera_promedio_seg, total.espera_maxima_seg), (120, 180))
        self.assertEqual((total.pedidos_seg, total.lista_casa_seg), (75, 25))
        self.assertEqual(total.id_cancion_popular, 100)  # la del voto


if __name__ == "__main__":
    unittest.main()
