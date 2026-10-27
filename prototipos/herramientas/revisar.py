"""Revisa las pantallas de los prototipos: cortes, superposiciones, objetivos táctiles y
contraste del texto contra el fondo que de verdad se pinta (vidrio, manchas, degradados).

Uso:
    python revisar.py                 # la dirección rockola
    python revisar.py vidrio          # una dirección
    python revisar.py vidrio cola     # solo las pantallas cuyo PNG contenga «cola»

Contraste: se captura la pantalla con todo el texto transparente y, para cada línea de
texto, se mide su color (con su transparencia y la de sus padres) contra cada píxel del
fondo que tiene debajo. Se informa el percentil 5 (el fondo más desfavorable, sin contar
píxeles sueltos de un borde). Umbral WCAG AA: 4,5:1, o 3:1 en texto grande (24px, o
18,66px en negrita).
"""
import io
import sys
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

from capturar import DIRECCIONES, PANTALLAS, RAIZ, esperar_fuentes

TEXTOS_JS = r"""
() => {
  const vw = innerWidth, vh = innerHeight;
  const opacidad = el => { let o = 1; for (let e = el; e; e = e.parentElement) o *= +getComputedStyle(e).opacity; return o; };
  const recortadores = el => { const r = []; for (let e = el.parentElement; e; e = e.parentElement) { const s = getComputedStyle(e); if (s.overflow !== 'visible' || s.overflowX !== 'visible' || s.overflowY !== 'visible') r.push(e); } return r; };
  const nombre = el => (el.className && typeof el.className === 'string' ? el.tagName.toLowerCase() + '.' + el.className.split(' ')[0] : el.tagName.toLowerCase());
  const out = [];
  const w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  let n, id = 0;
  while ((n = w.nextNode())) {
    if (!n.textContent.trim()) continue;
    const el = n.parentElement, s = getComputedStyle(el);
    if (s.visibility === 'hidden' || s.display === 'none') continue;
    if (el.closest('[data-revisar="no"]')) continue;
    const range = document.createRange(); range.selectNodeContents(n);
    const rects = [...range.getClientRects()].filter(r => r.width > 1 && r.height > 1);
    if (!rects.length) continue;
    const clip = s.backgroundClip === 'text' || s.webkitBackgroundClip === 'text';
    id++;
    for (const r of rects) {
      const cortes = [];
      if (r.left < -0.5 || r.top < -0.5 || r.right > vw + 0.5 || r.bottom > vh + 0.5) cortes.push('fuera de la pantalla');
      for (const c of recortadores(el)) { const b = c.getBoundingClientRect(); if (r.left < b.left - 0.5 || r.right > b.right + 0.5 || r.top < b.top - 0.5 || r.bottom > b.bottom + 0.5) { cortes.push('cortado por ' + nombre(c)); break; } }
      out.push({ id, texto: n.textContent.trim().slice(0, 48), el: nombre(el), x: r.left, y: r.top, w: r.width, h: r.height,
                 color: s.color, size: parseFloat(s.fontSize), peso: +s.fontWeight, opacidad: opacidad(el), clip, cortes,
                 fijo: !!el.closest('[data-capa]'), solapeOk: !!el.closest('[data-solape="ok"]') });
    }
  }
  const desbordes = [...document.querySelectorAll('body *')].filter(e => !e.closest('[data-revisar="no"]')).filter(e => { const s = getComputedStyle(e); return (s.overflow === 'hidden' || s.textOverflow === 'ellipsis') && (e.scrollWidth > e.clientWidth + 1) && e.textContent.trim(); }).map(e => nombre(e) + ': ' + e.textContent.trim().slice(0, 40));
  const tactiles = [...document.querySelectorAll('a[href], button, input, select, [role=button], [tabindex]')].map(e => { const r = e.getBoundingClientRect(); const enLinea = getComputedStyle(e).display === 'inline'; return { t: (e.getAttribute('aria-label') || e.textContent.trim() || e.tagName).slice(0, 36), w: r.width, h: r.height, enLinea }; }).filter(t => (t.w < 44 || t.h < 44) && t.w > 0);
  const sinNombre = [...document.querySelectorAll('button, a[href], [role=button]')].filter(e => !(e.getAttribute('aria-label') || e.textContent.trim())).length;
  // Capas: elementos posicionados (pie, botón anclado) que tapan contenido del flujo.
  const posicionado = e => { for (let x = e; x && x !== document.body; x = x.parentElement) { const p = getComputedStyle(x).position; if (p === 'absolute' || p === 'fixed') return x; } return null; };
  const visible = e => { const s = getComputedStyle(e); return s.display !== 'none' && s.visibility !== 'hidden'; };
  const capas = [...document.body.children].filter(e => visible(e) && ['absolute', 'fixed'].includes(getComputedStyle(e).position) && !e.classList.contains('fondo') && e.textContent.trim());
  const pintado = e => { const s = getComputedStyle(e); return parseFloat(s.borderTopWidth) > 0 || parseFloat(s.borderBottomWidth) > 0 || (s.backgroundColor !== 'rgba(0, 0, 0, 0)' && s.backgroundColor !== 'transparent') || s.backgroundImage !== 'none' || [...e.childNodes].some(n => n.nodeType === 3 && n.textContent.trim()); };
  const tapados = [];
  for (const capa of capas) {
    const c = capa.getBoundingClientRect();
    for (const e of document.querySelectorAll('body *')) {
      if (posicionado(e) || !visible(e) || !pintado(e) || e.closest('[data-revisar="no"]')) continue;
      const r = e.getBoundingClientRect();
      if (r.width < 1 || r.height < 1) continue;
      const ancho = Math.min(r.right, c.right) - Math.max(r.left, c.left), alto = Math.min(r.bottom, c.bottom) - Math.max(r.top, c.top);
      if (ancho > 1 && alto > 1 && !tapados.some(t => t.el.contains(e))) tapados.push({ el: e, t: nombre(e) + ' bajo ' + nombre(capa) + ` (${Math.round(alto)}px)` });
    }
  }
  return { textos: out, desbordes, tactiles, sinNombre, tapados: tapados.map(t => t.t), alto: document.documentElement.scrollHeight, ancho: document.documentElement.scrollWidth };
}
"""

SIN_TEXTO = """
*, *::before, *::after { color: transparent !important; -webkit-text-fill-color: transparent !important;
  text-shadow: none !important; caret-color: transparent !important; text-decoration-color: transparent !important; }
::placeholder { color: transparent !important; }
"""


def rgba(css):
    v = css[css.index("(") + 1 : css.index(")")].replace("/", ",").split(",")
    v = [x.strip() for x in v if x.strip()]
    r, g, b = (float(x) for x in v[:3])
    a = float(v[3]) if len(v) > 3 else 1.0
    return r, g, b, a


def lum(c):
    def canal(x):
        x /= 255
        return x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4
    r, g, b = c
    return 0.2126 * canal(r) + 0.7152 * canal(g) + 0.0722 * canal(b)


def contraste(a, b):
    la, lb = lum(a), lum(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)


def peor_contraste(img, t):
    r, g, b, a = rgba(t["color"])
    a *= t["opacidad"]
    x0, y0 = max(0, int(t["x"])), max(0, int(t["y"]))
    x1, y1 = min(img.width, int(t["x"] + t["w"]) + 1), min(img.height, int(t["y"] + t["h"]) + 1)
    if x1 <= x0 or y1 <= y0:
        return None
    zona = img.crop((x0, y0, x1, y1)).convert("RGB")
    paso = max(1, (zona.width * zona.height) // 900)
    valores = []
    pixeles = zona.get_flattened_data() if hasattr(zona, "get_flattened_data") else zona.getdata()
    for i, (br, bg, bb) in enumerate(pixeles):
        if i % paso:
            continue
        fg = (a * r + (1 - a) * br, a * g + (1 - a) * bg, a * b + (1 - a) * bb)
        valores.append(contraste(fg, (br, bg, bb)))
    valores.sort()
    return valores[int(len(valores) * 0.05)]


def solapes(textos):
    hallados = []
    for i, a in enumerate(textos):
        for b in textos[i + 1 :]:
            # data-solape="ok": titulares de cartel con interlínea menor que 1; sus cajas
            # se tocan aunque la tinta no (comprobado a ojo en la captura).
            if a["id"] == b["id"] or a["solapeOk"] or b["solapeOk"]:
                continue
            ancho = min(a["x"] + a["w"], b["x"] + b["w"]) - max(a["x"], b["x"])
            alto = min(a["y"] + a["h"], b["y"] + b["h"]) - max(a["y"], b["y"])
            if ancho > 1.5 and alto > 1.5:
                hallados.append(f"«{a['texto']}» pisa «{b['texto']}» ({ancho:.0f}×{alto:.0f}px)")
    return hallados


def revisar(direccion, filtro, browser):
    carpeta = RAIZ / direccion
    total = 0
    for archivo, estado, ancho, alto, png in PANTALLAS:
        if filtro and filtro not in png:
            continue
        ruta = carpeta / archivo
        if not ruta.exists():
            continue
        ctx = browser.new_context(viewport={"width": ancho, "height": alto}, device_scale_factor=1)
        page = ctx.new_page()
        page.goto(ruta.as_uri() + estado)
        fallan, _ = esperar_fuentes(page)
        page.wait_for_timeout(200)
        datos = page.evaluate(TEXTOS_JS)
        page.add_style_tag(content=SIN_TEXTO)
        page.wait_for_timeout(100)
        fondo = Image.open(io.BytesIO(page.screenshot()))
        ctx.close()

        problemas = []
        if fallan:
            problemas.append(f"fuentes sin cargar: {fallan}")
        if datos["alto"] > alto + 1 or datos["ancho"] > ancho + 1:
            problemas.append(f"la página mide {datos['ancho']}×{datos['alto']} y la pantalla {ancho}×{alto}")
        for t in datos["textos"]:
            for c in t["cortes"]:
                problemas.append(f"«{t['texto']}»: {c}")
        problemas += [f"desborda: {d}" for d in datos["desbordes"]]
        problemas += [f"tapado: {t}" for t in datos["tapados"]]
        problemas += [f"se superpone: {s}" for s in solapes(datos["textos"])]
        for t in datos["tactiles"]:
            nota = " (enlace en línea, exento)" if t["enLinea"] else ""
            if not t["enLinea"]:
                problemas.append(f"objetivo táctil {t['w']:.0f}×{t['h']:.0f}px < 44: «{t['t']}»{nota}")
        if datos["sinNombre"]:
            problemas.append(f"{datos['sinNombre']} botones o enlaces sin nombre accesible")
        vistos = set()
        for t in datos["textos"]:
            if t["clip"]:
                problemas.append(f"«{t['texto']}» usa texto con degradado: revisar a ojo")
                continue
            c = peor_contraste(fondo, t)
            if c is None:
                continue
            grande = t["size"] >= 24 or (t["size"] >= 18.66 and t["peso"] >= 700)
            minimo = 3.0 if grande else 4.5
            if c < minimo and t["id"] not in vistos:
                vistos.add(t["id"])
                problemas.append(f"contraste {c:.2f}:1 < {minimo}: «{t['texto']}» ({t['size']:.0f}px {t['peso']})")
        total += len(problemas)
        print(f"\n{direccion}/{png}: {'bien' if not problemas else str(len(problemas)) + ' avisos'}")
        for p in problemas:
            print("   - " + p)
    return total


def main():
    direcciones = [a for a in sys.argv[1:2] if a in DIRECCIONES] or DIRECCIONES
    filtro = sys.argv[2] if len(sys.argv) > 2 else ""
    with sync_playwright() as p:
        browser = p.chromium.launch()
        total = sum(revisar(d, filtro, browser) for d in direcciones)
        browser.close()
    print(f"\nTotal de avisos: {total}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
