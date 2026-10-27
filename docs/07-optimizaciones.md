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
| 2026-10-27 | JavaScript que descarga cada ruta, compilación real (`NEXT_PUBLIC_API_MODE=real`), API del navegador (`encodedBodySize`) | `/b/[bar]` 211 kB comprimidos (699 kB sin comprimir), `/b/[bar]/buscar` 213 kB, `/b/[bar]/pantalla` 213 kB, `/panel/entrar` 204 kB |
| 2026-10-27 | Qué pesa en `/b/[bar]` | React DOM 72 kB y el runtime de Next 44 kB (fijos); zod con el contrato **entero** 40 kB (incluye los esquemas del panel y de la analítica); el resto, menos de 12 kB por archivo |
| 2026-10-27 | La simulación en la compilación real | Fuera: ningún archivo lleva MSW. En modo simulado es un archivo aparte de 110 kB comprimidos que solo se descarga en la demo |

**Por encima del presupuesto propuesto** (180 kB para `/b/[bar]`): 211 kB. El siguiente paso
es partir el registro de operaciones por superficie (`endpoints` del cliente, de la pantalla y
del panel) para que el teléfono no descargue los esquemas del panel ni de la analítica; se
estima que baja unos 20 kB. Si no alcanza, validar con `zod/mini`. No se hizo en este cierre
porque cambia la firma de `pedir()` en todas las llamadas.
