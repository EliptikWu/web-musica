# 07 — Optimizaciones y presupuestos

Las metas con número de los requisitos son de la fuente; las demás son *(propuesto)*
hasta medirlas.

## Presupuestos

| Qué | Meta | Fuente | Cómo se cumple |
|---|---|---|---|
| Primera pantalla del cliente tras el QR | < 2 s con red móvil estándar | RF-001 | Armazón renderizado en el servidor, fuente propia con `next/font`, sin bibliotecas pesadas en la ruta del cliente |
| Sugerencias de búsqueda | < 2 s | RF-005 | Espera de 300 ms entre teclas, se cancela la petición anterior, se conservan los resultados previos mientras llegan los nuevos |
| Confirmación del voto | < 1 s | RF-012 | Voto optimista: la tecla se hunde al instante y se revierte si falla |
| Acciones principales | ≤ 3 s | RNF-002 | — |
| Propagación en vivo | ≤ 3 s | RNF-009 | Eventos SSE; sondeo de 3 s si no hay SSE |
| JS inicial de `/b/[bar]` | ≤ 180 kB comprimido *(propuesto)* | — | El panel, la analítica y la simulación no entran en el paquete del cliente |
| Miniaturas | Tamaño `default` de YouTube (120 × 90) en listas | — | `loading="lazy"`, dimensiones fijas para no mover la página |

## Reglas

- Una ruta no importa código de otra superficie: el cliente no carga nada del panel.
- La simulación (MSW, motor y datos) solo se descarga en modo simulado.
- Las imágenes llevan ancho y alto para no mover la maqueta al cargar.
- Las animaciones usan `transform` y `opacity`.
- Los gráficos de la analítica son SVG propios, sin biblioteca de gráficas.

## Mediciones

| Fecha | Qué | Resultado |
|---|---|---|
| — | Se registran aquí al cerrar cada iteración (tamaño de `next build` y tiempos medidos) | — |
