"""CP-024/025/032/040: integración Spark del cubo por establecimiento (sin Railway)."""
import unittest

from pyspark.sql import SparkSession

from rockola_pipeline import modelo


class AislamientoCubo(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.spark = (SparkSession.builder.master("local[2]")
                     .appName("rokola-cubo-bar-aislado")
                     .config("spark.sql.session.timeZone", "UTC")
                     .config("spark.sql.shuffle.partitions", "2").getOrCreate())

    @classmethod
    def tearDownClass(cls):
        cls.spark.stop()

    def _datos(self):
        s = self.spark
        noches = s.createDataFrame([(101, 10, 5, "normal"), (201, 20, 5, "normal")],
                                   ["id_sesion", "id_establecimiento", "dia_semana", "tipo_evento"])
        canciones = s.createDataFrame([(1000, "Salsa")], ["id_cancion", "genero"])
        solicitudes = s.createDataFrame([
            (11, 10, 101, 1000, "a" * 32, 21, 60, None),
            (22, 20, 201, 1000, "b" * 32, 21, 120, None),
            (23, 20, 201, 1000, "b" * 32, 21, None, "omitida"),
        ], "id_solicitud long, id_establecimiento long, id_sesion long, id_cancion long, "
           "dispositivo_seud string, hora_local int, espera_seg int, causa_no_sono string")
        votos = s.createDataFrame([
            (501, 10, 101, 11, "a" * 32, 21),
            (502, 20, 201, 22, "b" * 32, 21),
        ], "id_voto long, id_establecimiento long, id_sesion long, id_solicitud long, "
           "dispositivo_seud string, hora_local int")
        reproducciones = s.createDataFrame([], "id_reproduccion long, id_establecimiento long, "
              "id_sesion long, id_cancion long, id_solicitud long, origen string, inicio timestamp, "
              "segundos int, hora_local int")
        return noches, canciones, solicitudes, votos, reproducciones

    def test_ambos_bares_tienen_subtotales_independientes(self):
        cubo = modelo.construir_cubo(self.spark, *self._datos())
        totales = cubo.filter("eje = 'dia' AND genero = '*' AND franja = '*' AND tercero = '*'")
        por_bar = {r.id_establecimiento: r for r in totales.collect()}
        self.assertEqual({10, 20}, set(por_bar))
        self.assertEqual((1, 1, 1, 0),
                         tuple(por_bar[10][k] for k in ("solicitudes", "votos", "telefonos", "no_sonaron")))
        self.assertEqual((2, 1, 1, 1),
                         tuple(por_bar[20][k] for k in ("solicitudes", "votos", "telefonos", "no_sonaron")))

    def test_agregado_no_suma_usuarios_unicos_de_solicitudes_y_votos(self):
        cubo = modelo.construir_cubo(self.spark, *self._datos())
        filas = cubo.filter("eje = 'evento' AND genero = '*' AND franja = '*' AND tercero = '*'").collect()
        self.assertTrue(filas)
        self.assertTrue(all(r.telefonos == 1 for r in filas))
        self.assertEqual({10, 20}, {r.id_establecimiento for r in filas})


if __name__ == "__main__":
    unittest.main()
