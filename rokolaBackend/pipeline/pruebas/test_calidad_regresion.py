"""CP-024/025/032/033/047: regresiones del ETL en Spark local, sin Railway."""
import unittest
from pyspark.sql import SparkSession, functions as F
from rockola_pipeline import carga, ingesta, limpieza, modelo


class CalidadRegresion(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.spark = (SparkSession.builder.master("local[2]")
                     .appName("rokola-test-calidad")
                     .config("spark.sql.session.timeZone", "UTC")
                     .config("spark.sql.shuffle.partitions", "2").getOrCreate())

    @classmethod
    def tearDownClass(cls):
        cls.spark.stop()

    def test_fechas_lunes_y_domingo_con_formato_iso(self):
        df = self.spark.createDataFrame([("2026-11-23",), ("2026-11-29",)], ["fecha"])
        registros = df.select(modelo.dia_iso(F.to_date("fecha")).alias("dia")).collect()
        self.assertEqual([1, 7], [r.dia for r in registros])

    def test_mapeo_de_siete_dias_conserva_orden(self):
        df = self.spark.createDataFrame([(i,) for i in range(1, 8)], ["dia"])
        self.assertEqual(["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"],
                         [r.nombre for r in df.select(modelo.dia_cubo(F.col("dia")).alias("nombre")).collect()])

    def test_seudonimos_deben_tener_32_hexadecimales_y_no_ser_nulos(self):
        df = self.spark.createDataFrame([
            (1, "a" * 32), (2, "A" * 32), (3, "abc"), (4, None), (5, "f" * 32),
        ], ["id", "dispositivo_seud"])
        validos, rechazados = limpieza.filtrar(df, "id", [("sin_dispositivo", limpieza.seudonimo_valido())])
        self.assertEqual([1, 5], [r.id for r in validos.orderBy("id").collect()])
        self.assertEqual(3, rechazados["sin_dispositivo"])

    def test_reglas_en_cascada_reportan_el_motivo_sin_contar_dos_veces(self):
        df = self.spark.createDataFrame([(1, 10, "si"), (2, -1, "si"), (3, 10, "no"),
                                         (4, -5, "no")], ["id", "duracion", "estado"])
        valido, rechazados = limpieza.filtrar(df, "id", [
            ("duracion_invalida", F.col("duracion") >= 0),
            ("estado_invalido", F.col("estado") == "si"),
        ])
        self.assertEqual([1], [r.id for r in valido.collect()])
        self.assertEqual({"duracion_invalida": 2, "estado_invalido": 1}, rechazados)

    def test_sesion_excluida_nunca_se_inyecta_como_sql_de_usuario(self):
        self.assertEqual("SELECT 1 WHERE x AND s.id_sesion NOT IN (2,9)",
                         ingesta.sin_excluidas("SELECT 1 WHERE x", frozenset([9, 2])))

    def test_una_noche_incompleta_no_se_audita_como_perdida(self):
        faltantes = carga.noches_distintas("votos", {1: 3, 2: 9}, {1: 3},
                                           {"quitados": {}, "telefonos": {}, "noches": {1}})
        self.assertEqual([], faltantes)

    def test_reintentos_con_la_misma_clave_de_seudonimo_son_deterministas(self):
        df = self.spark.createDataFrame([("dispositivo-A",)], ["h"])
        pseudonimizador = ingesta.seudonimizador("clave-test-" * 4)
        uno = df.select(pseudonimizador("h").alias("hash")).first().hash
        dos = df.select(pseudonimizador("h").alias("hash")).first().hash
        self.assertEqual(uno, dos)
        self.assertNotEqual(uno, "dispositivo-A")


if __name__ == "__main__":
    unittest.main()
