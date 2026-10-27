"""Captura las pantallas de los prototipos a 2x con Playwright (Chromium).

Uso:
    python capturar.py                 # la dirección rockola
    python capturar.py vidrio          # una dirección
    python capturar.py vidrio admin    # solo las pantallas cuyo nombre contenga «admin»

Deja los PNG en capturas/<dirección>/ y, por dirección, cliente-lado-a-lado.png con las
cuatro pantallas del cliente.
"""
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

RAIZ = Path(__file__).resolve().parent.parent
DIRECCIONES = ["rockola"]  # en este repositorio solo vive la dirección elegida

# (archivo, estado, ancho, alto, nombre del PNG)
PANTALLAS = [
    ("cliente-1-bienvenida.html", "", 390, 844, "cliente-1-bienvenida.png"),
    ("cliente-2-buscar.html", "", 390, 844, "cliente-2-buscar.png"),
    ("cliente-2-buscar.html", "#pedido", 390, 844, "cliente-2b-despues-de-pedir.png"),
    ("cliente-3-cola.html", "", 390, 844, "cliente-3-cola.png"),
    ("cliente-4-fuera-de-horario.html", "", 390, 844, "cliente-4-fuera-de-horario.png"),
    ("pantalla-bar.html", "", 1280, 720, "pantalla-bar.png"),
    ("panel-admin.html", "", 1440, 1000, "panel-admin.png"),
    ("panel-admin.html", "#pausa", 1440, 1000, "panel-admin-en-pausa.png"),
]

LADO_A_LADO = [
    ("cliente-1-bienvenida.png", "1. Bienvenida"),
    ("cliente-2-buscar.png", "2. Buscar, pedir y votar"),
    ("cliente-3-cola.png", "3. Cola en tiempo real"),
    ("cliente-4-fuera-de-horario.png", "4. Fuera de horario"),
]

# Fondo y texto de la lámina lado a lado, por dirección.
LAMINA = {
    "vidrio": ("#121016", "rgba(255,255,255,.72)", "Outfit"),
    "rockola": ("#24150F", "#F3E6C8", "Archivo"),
    "afiche": ("#EDEAE2", "#17120F", "Archivo"),
}


def esperar_fuentes(page):
    page.wait_for_load_state("networkidle")
    page.evaluate("document.fonts.ready.then(() => true)")
    # Google Fonts declara un subconjunto por alfabeto y solo baja los que se usan: falla
    # una familia si no cargó ninguna de sus caras.
    fallan = page.evaluate(
        """() => { const f = [...document.fonts]; const fam = [...new Set(f.map(x => x.family))];
                   return fam.filter(n => !f.some(x => x.family === n && x.status === 'loaded')); }"""
    )
    usadas = page.evaluate(
        """() => [...document.fonts].filter(f => f.status === 'loaded').map(f => f.family + ' ' + f.weight)"""
    )
    return fallan, usadas


def capturar(direccion, filtro, browser):
    carpeta = RAIZ / direccion
    salida = RAIZ / "capturas" / direccion
    salida.mkdir(parents=True, exist_ok=True)
    for archivo, estado, ancho, alto, png in PANTALLAS:
        if filtro and filtro not in png:
            continue
        ruta = carpeta / archivo
        if not ruta.exists():
            print(f"  falta {direccion}/{archivo}")
            continue
        ctx = browser.new_context(
            viewport={"width": ancho, "height": alto}, device_scale_factor=2
        )
        page = ctx.new_page()
        page.goto(ruta.as_uri() + estado)
        fallan, _ = esperar_fuentes(page)
        page.wait_for_timeout(250)
        page.screenshot(path=str(salida / png))
        aviso = f"  ¡fuentes sin cargar: {fallan}!" if fallan else ""
        print(f"  {direccion}/{png}{aviso}")
        ctx.close()


def lado_a_lado(direccion, browser):
    salida = RAIZ / "capturas" / direccion
    if not all((salida / png).exists() for png, _ in LADO_A_LADO):
        return
    fondo, texto, fuente = LAMINA[direccion]
    figuras = "".join(
        f'<figure><img src="{(salida / png).as_uri()}" alt=""><figcaption>{titulo}</figcaption></figure>'
        for png, titulo in LADO_A_LADO
    )
    html = f"""<!doctype html><html lang="es"><head><meta charset="utf-8">
<link href="https://fonts.googleapis.com/css2?family=Outfit:wght@500&family=Archivo:wdth,wght@100,500&display=swap" rel="stylesheet">
<style>
  html,body{{margin:0;background:{fondo}}}
  main{{display:flex;gap:40px;padding:48px 56px 40px;width:max-content}}
  figure{{margin:0}}
  img{{display:block;width:390px;height:844px;border-radius:34px;
       box-shadow:0 0 0 1px rgba(127,127,127,.25)}}
  figcaption{{font:500 17px/1.3 '{fuente}',sans-serif;color:{texto};margin-top:18px;padding-left:4px}}
</style></head><body><main>{figuras}</main></body></html>"""
    tmp = salida / "_lamina.html"
    tmp.write_text(html, encoding="utf-8")
    ctx = browser.new_context(viewport={"width": 1800, "height": 1000}, device_scale_factor=2)
    page = ctx.new_page()
    page.goto(tmp.as_uri())
    esperar_fuentes(page)
    page.locator("main").screenshot(path=str(salida / "cliente-lado-a-lado.png"))
    ctx.close()
    tmp.unlink()
    print(f"  {direccion}/cliente-lado-a-lado.png")


def main():
    direcciones = [a for a in sys.argv[1:2] if a in DIRECCIONES] or DIRECCIONES
    filtro = sys.argv[2] if len(sys.argv) > 2 else ""
    with sync_playwright() as p:
        browser = p.chromium.launch()
        for d in direcciones:
            capturar(d, filtro, browser)
            if not filtro:
                lado_a_lado(d, browser)
        browser.close()


if __name__ == "__main__":
    main()
