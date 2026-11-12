# Prototipos de Rockola

Prototipos estáticos (HTML + un CSS, sin JavaScript) de las tres direcciones visuales de
`docs/fuente/Prototipos, Arquitectura y Modelado de datos .docx`: **Rockola** (Tema #2, la
base), **Vidrio** (Tema #1) y **Afiche** (Tema #3). Son la referencia visual de la app:
`frontend/` no los importa y aquí no se programa nada nuevo.

Vidrio y Afiche se habían descartado (D2) y volvieron el 2026-11-11 (D13): el Administrador
elige el diseño de su bar (RF-059). Se copiaron sin cambios de la carpeta original del
equipo (`Documents\Rockola`); `vidrio/` y `afiche/` tienen las mismas seis pantallas que
`rockola/`, con capturas en `capturas/vidrio/` y `capturas/afiche/`. Cargan sus fuentes de
Google Fonts (Outfit; League Gothic y Libre Franklin); la app las sirve con `next/font`.

## Qué hay

| Archivo | Pantalla | Lienzo |
|---|---|---|
| `rockola/cliente-1-bienvenida.html` | Cliente: bienvenida | 390 × 844 |
| `rockola/cliente-2-buscar.html` (`#pedido`: después de pedir) | Cliente: buscar, pedir y votar | 390 × 844 |
| `rockola/cliente-3-cola.html` | Cliente: cola en tiempo real | 390 × 844 |
| `rockola/cliente-4-fuera-de-horario.html` | Cliente: fuera de horario | 390 × 844 |
| `rockola/pantalla-bar.html` | Pantalla pública del bar (TV) | 1280 × 720 |
| `rockola/panel-admin.html` (`#pausa`: en pausa) | Panel de administración | 1440 × 1000 |
| `rockola/estilos.css` | Tokens y componentes; de aquí salen `packages/ui/src/*.css` | — |
| `capturas/rockola/*.png` | Capturas a 2x de cada pantalla y estado | — |
| `decisiones-de-diseno.md` | Reglas comunes y por qué de cada una | — |
| `herramientas/notas-de-diseno.md` | Notas de trabajo: tokens, escala tipográfica, principios | — |
| `herramientas/datos-de-ejemplo.md` | Los dos momentos de la noche que usan todas las pantallas | — |
| `herramientas/capturar.py`, `revisar.py` | Captura y auditoría (contraste, objetivos de 44 px) con Playwright para Python | — |

## Arrancar en local

Abrir cualquier `.html` en el navegador. Para capturar o auditar:

```
pip install playwright pillow && python -m playwright install chromium
python prototipos/herramientas/capturar.py
python prototipos/herramientas/revisar.py
```

## Lo que conviene saber

- Los estados se cambian con el ancla de la URL (`:target`), no con JavaScript.
- Son lienzos fijos: el comportamiento responsive de la app es trabajo nuevo (doc 05).
- Los datos son de ejemplo (`herramientas/datos-de-ejemplo.md`); lo pendiente va entre
  corchetes: `[Nombre del bar]`, `[Usuario]`, `[enlace del bar]`.
- La app no copia este HTML: reimplementa los mismos tokens y piezas en `packages/ui`.
  Si un prototipo y el doc 05 chocan, gana el doc 05.
