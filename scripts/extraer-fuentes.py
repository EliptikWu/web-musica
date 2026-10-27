"""Extrae el texto de los documentos fuente (.docx y .xlsx) a Markdown legible.

Uso: python -I scripts/extraer-fuentes.py
Lee docs/fuente/*.docx y *.xlsx y escribe docs/fuente/extraido/<nombre>.md.
Se corre una vez por cada versión nueva de los documentos; los originales no se tocan.
"""

from __future__ import annotations

import re
import sys
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

RAIZ = Path(__file__).resolve().parent.parent
FUENTE = RAIZ / "docs" / "fuente"
SALIDA = FUENTE / "extraido"

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
S = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"


def texto_de(elemento: ET.Element) -> str:
    partes: list[str] = []
    for nodo in elemento.iter():
        if nodo.tag == f"{W}t" and nodo.text:
            partes.append(nodo.text)
        elif nodo.tag == f"{W}tab":
            partes.append(" ")
        elif nodo.tag in (f"{W}br", f"{W}cr"):
            partes.append(" ")
    return re.sub(r"\s+", " ", "".join(partes)).strip()


def nivel_titulo(parrafo: ET.Element) -> int | None:
    estilo = parrafo.find(f"{W}pPr/{W}pStyle")
    if estilo is None:
        return None
    valor = (estilo.get(f"{W}val") or "").lower()
    coincidencia = re.match(r"(?:heading|ttulo|titulo|title)(\d?)", valor)
    if not coincidencia:
        return None
    return int(coincidencia.group(1) or 1)


def es_lista(parrafo: ET.Element) -> bool:
    return parrafo.find(f"{W}pPr/{W}numPr") is not None


def celda(texto: str) -> str:
    return texto.replace("|", "\\|")


def tabla_md(tabla: ET.Element) -> list[str]:
    filas: list[list[str]] = []
    for fila in tabla.findall(f"{W}tr"):
        celdas = []
        for c in fila.findall(f"{W}tc"):
            parrafos = [texto_de(p) for p in c.findall(f".//{W}p")]
            celdas.append(celda(" ".join(p for p in parrafos if p)))
        filas.append(celdas)
    if not filas:
        return []
    ancho = max(len(f) for f in filas)
    filas = [f + [""] * (ancho - len(f)) for f in filas]
    lineas = ["| " + " | ".join(filas[0]) + " |", "|" + "---|" * ancho]
    lineas += ["| " + " | ".join(f) + " |" for f in filas[1:]]
    return lineas


def docx_a_md(ruta: Path) -> str:
    with zipfile.ZipFile(ruta) as z:
        raiz = ET.fromstring(z.read("word/document.xml"))
        imagenes = sorted(n for n in z.namelist() if n.startswith("word/media/"))
    cuerpo = raiz.find(f"{W}body")
    lineas = [f"# {ruta.stem.strip()}", "", f"> Texto extraído de `{ruta.name}` con `scripts/extraer-fuentes.py`. "
              "El original manda: las imágenes (maquetas y diagramas) solo están en el .docx.", ""]
    if cuerpo is None:
        return "\n".join(lineas)
    for bloque in cuerpo:
        if bloque.tag == f"{W}p":
            texto = texto_de(bloque)
            if not texto:
                continue
            nivel = nivel_titulo(bloque)
            if nivel:
                lineas += ["", "#" * min(nivel + 1, 6) + " " + texto, ""]
            elif es_lista(bloque):
                lineas.append("- " + texto)
            else:
                lineas += [texto, ""]
        elif bloque.tag == f"{W}tbl":
            lineas += [""] + tabla_md(bloque) + [""]
    if imagenes:
        lineas += ["", "## Imágenes incluidas en el original", ""]
        lineas += [f"- `{n}`" for n in imagenes]
    return re.sub(r"\n{3,}", "\n\n", "\n".join(lineas)).strip() + "\n"


def xlsx_a_md(ruta: Path) -> str:
    with zipfile.ZipFile(ruta) as z:
        compartidas: list[str] = []
        if "xl/sharedStrings.xml" in z.namelist():
            for si in ET.fromstring(z.read("xl/sharedStrings.xml")).findall(f"{S}si"):
                compartidas.append("".join(t.text or "" for t in si.iter(f"{S}t")))
        hojas = sorted(n for n in z.namelist() if re.match(r"xl/worksheets/sheet\d+\.xml$", n))
        lineas = [f"# {ruta.stem.strip()}", "", f"> Texto extraído de `{ruta.name}` con `scripts/extraer-fuentes.py`.", ""]
        for hoja in hojas:
            filas: list[list[str]] = []
            for fila in ET.fromstring(z.read(hoja)).iter(f"{S}row"):
                valores: dict[int, str] = {}
                for c in fila.findall(f"{S}c"):
                    ref = c.get("r") or ""
                    letras = re.match(r"[A-Z]+", ref)
                    col = 0
                    for letra in letras.group(0) if letras else "A":
                        col = col * 26 + (ord(letra) - 64)
                    v = c.find(f"{S}v")
                    if c.get("t") == "s" and v is not None and v.text:
                        valor = compartidas[int(v.text)]
                    elif c.get("t") == "inlineStr":
                        valor = "".join(t.text or "" for t in c.iter(f"{S}t"))
                    else:
                        valor = v.text if v is not None and v.text else ""
                    valores[col] = celda(re.sub(r"\s+", " ", valor).strip())
                if any(valores.values()):
                    ancho = max(valores)
                    filas.append([valores.get(i, "") for i in range(1, ancho + 1)])
            if not filas:
                continue
            ancho = max(len(f) for f in filas)
            filas = [f + [""] * (ancho - len(f)) for f in filas]
            lineas += [f"## {Path(hoja).stem}", "", "| " + " | ".join(filas[0]) + " |", "|" + "---|" * ancho]
            lineas += ["| " + " | ".join(f) + " |" for f in filas[1:]]
            lineas.append("")
    return "\n".join(lineas).strip() + "\n"


def main() -> int:
    SALIDA.mkdir(parents=True, exist_ok=True)
    for ruta in sorted(FUENTE.iterdir()):
        if ruta.suffix == ".docx":
            md = docx_a_md(ruta)
        elif ruta.suffix == ".xlsx":
            md = xlsx_a_md(ruta)
        else:
            continue
        nombre = re.sub(r"[^a-z0-9]+", "-", ruta.stem.strip().lower()
                        .translate(str.maketrans("áéíóúñ", "aeioun"))).strip("-")
        (SALIDA / f"{nombre}.md").write_text(md, encoding="utf-8", newline="\n")
        print(f"{ruta.name} -> extraido/{nombre}.md ({len(md.splitlines())} líneas)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
