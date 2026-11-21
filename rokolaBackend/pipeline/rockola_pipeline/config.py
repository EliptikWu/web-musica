"""Configuración por variables de entorno: ninguna credencial vive en el código."""
from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Config:
    jdbc_url: str
    usuario: str
    clave: str
    clave_seudonimo: str
    datos: str

    @property
    def propiedades(self) -> dict[str, str]:
        return {"user": self.usuario, "password": self.clave, "driver": "org.postgresql.Driver"}


def _exigir(nombre: str) -> str:
    valor = os.environ.get(nombre, "").strip()
    if not valor:
        raise SystemExit(f"Falta la variable de entorno {nombre}")
    return valor


def desde_entorno() -> Config:
    clave_seudonimo = _exigir("ROCKOLA_SEUDONIMO_CLAVE")
    if len(clave_seudonimo) < 32:
        raise SystemExit("ROCKOLA_SEUDONIMO_CLAVE debe tener al menos 32 caracteres")
    return Config(
        jdbc_url=_exigir("ANALITICA_JDBC_URL"),
        usuario=_exigir("DB_USERNAME"),
        clave=_exigir("DB_PASSWORD"),
        clave_seudonimo=clave_seudonimo,
        datos=os.environ.get("ANALITICA_DATOS", "/datos"),
    )
