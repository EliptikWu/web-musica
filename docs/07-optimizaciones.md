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
- Diseños del bar (RF-059): `temas/afiche.css` y `temas/vidrio.css` se importan en el layout
  de `/b/[bar]` (el panel no los baja), los dos a la vez. Bajar solo el del bar haría esperar
  a `/bar` y volvería el destello de Rockola (ADR-26). Sus fuentes se declaran en el layout
  raíz sin precarga: solo se descargan si el tema las usa, pero sus variables tienen que estar
  en `<html>` para que `--fuente` las resuelva.
- Búsqueda (corrección del 2026-11-12): el backend arma cada página de resultados con cuatro
  consultas en lote (`IN (…)`) y no cuatro por resultado; solo arma la foto de la cola si la
  búsqueda coincide con algo de ella; y guarda la búsqueda de YouTube 6 h *(propuesto; `youtube.cache.horas`)* en vez de 30 min,
  porque la cuota es de 80 búsquedas diarias y la disponibilidad se valida al pedir. El
  autocompletado sigue mirando solo lo buscado en los últimos 30 min (la caché es de todos los bares).
  El teléfono sigue esperando 300 ms tras la última tecla.
- Vidrio desenfoca el fondo (`backdrop-filter`) en las piezas del prototipo, sobre un fondo
  que se desplaza con la página (sin `background-attachment: fixed`, que obliga a desenfocar
  de nuevo en cada fotograma). Su costo en teléfonos de gama baja está **sin medir**.

## Mediciones

| Fecha | Qué | Resultado |
|---|---|---|
| 2026-10-27 | JavaScript que descarga cada ruta, compilación real (`NEXT_PUBLIC_API_MODE=real`), API del navegador (`encodedBodySize`) | `/b/[bar]` 211 kB comprimidos (699 kB sin comprimir), `/b/[bar]/buscar` 213 kB, `/b/[bar]/pantalla` 213 kB, `/panel/entrar` 204 kB |
| 2026-10-27 | Qué pesa en `/b/[bar]` | React DOM 72 kB y el runtime de Next 44 kB (fijos); zod con el contrato **entero** 40 kB (incluye los esquemas del panel y de la analítica); el resto, menos de 12 kB por archivo |
| 2026-10-27 | La simulación en la compilación real | Fuera: ningún archivo lleva MSW. En modo simulado es un archivo aparte de 110 kB comprimidos que solo se descarga en la demo |
| 2026-11-12 | Búsqueda del backend (`GET /bares/{slug}/busqueda`), 10 resultados en caché y una cola de 8; Postgres 16 en Docker con `log_statement=all`, 20 peticiones | **Antes:** 55 consultas a la base por búsqueda, 35 ms de media en local. **Después:** 9 consultas al buscar algo que no está en la cola (lo habitual) y 20 cuando coincide; 12 y 20 ms en local. Con unos 25 ms por ida y vuelta a una base remota *(estimado, sin medir contra Railway)*, de ~1,4 s a ~0,2 s solo en base de datos. Mismas respuestas (bloqueada, sonó hace poco, no embebible) |
| 2026-11-25 | `e2e/rendimiento.spec.ts`, compilación simulada en esta máquina (Edge, sin limitar la red) | Tres corridas. Primera pantalla del cliente 360–700 ms. Pausar: se ve en 110–350 ms y el servidor lo confirma (204 y la noche nueva, tecla libre) en 130–400 ms. Saltar: TV en 130–280 ms y panel en 150–330 ms. JS de `/b/[bar]` 220 kB comprimidos sin los 114 kB de la simulación: React DOM 72, Next 52, zod con el contrato entero 42 y el código propio unos 54 en archivos chicos |
| 2026-11-25 | Pausar en el panel, simulación local, antes y después del cambio optimista (cuatro vueltas) | Antes: 140–500 ms hasta ver «Reanudar». Después: 80–250 ms. Contra el backend real la diferencia debería ser mayor (la ida y vuelta y la recarga de `/panel/noche` ya no se esperan), pero **no se midió** |
| 2026-11-25 | Carga con k6 (`scripts/carga/noche.js`) contra el backend en esta máquina y Postgres 16 en Docker (esquema reconstruido, 20 pedidos sembrados, sin YouTube ni SSE abiertos): teléfonos que miran la cola, votan y quitan el voto; dos paneles que recargan la noche | **Antes del arreglo**, 60 teléfonos: 2,6 % de fallas y p95 de votar y de `/panel/noche` en **30 s**. El volcado de hilos mostró los 62 hilos esperando conexión: el oyente SSE pedía una segunda (`REQUIRES_NEW`) con la de la petición aún tomada, y con 10 a la vez el pool de 10 se trababa hasta el tiempo límite de Hikari. **Después** (avisos en su propio hilo): 60 teléfonos, 0 % de fallas en 5 596 peticiones, 45 por segundo, p95 de votar 26 ms, quitar 20 ms, `ahora` 15 ms y `/panel/noche` 26 ms. 200 teléfonos: 0 % en 9 490 peticiones, 146 por segundo, p95 de votar 523 ms (bajo el segundo de RF-012), `ahora` 146 ms y `/panel/noche` 31 ms. Una primera corrida de 200 justo tras reiniciar tuvo 27 registros fallidos sin error en el log del backend; no se repitió en dos corridas más |

**Por encima del presupuesto propuesto** (180 kB para `/b/[bar]`): 211 kB. El siguiente paso
es partir el registro de operaciones por superficie (`endpoints` del cliente, de la pantalla y
del panel) para que el teléfono no descargue los esquemas del panel ni de la analítica; se
estima que baja unos 20 kB. Si no alcanza, validar con `zod/mini`. No se hizo en este cierre
porque cambia la firma de `pedir()` en todas las llamadas.

## Análisis del 2026-11-25

Pedido por el equipo («añadan un análisis para optimizar»). Lo medido está arriba; esto es qué
se hizo, qué falta y en qué orden, de más a menos efecto para el usuario.

| # | Hallazgo | Medido | Qué se hizo o qué toca |
|---|---|---|---|
| 1 | Los botones de operación esperaban la ida y vuelta y la recarga de `/panel/noche`, que el aviso SSE ya había lanzado y el panel cancelaba y repetía | Simulación: 140–500 ms → 80–250 ms | **Hecho.** Pausar y reanudar son optimistas y toda acción espera la noche nueva antes de soltar la tecla (`useAccionPanel`) |
| 2 | En el backend, los oyentes SSE armaban la foto **dentro** de la petición y pedían una segunda conexión: con 10 votos a la vez el backend se congelaba 30 s. Al saltar, la siguiente canción todavía espera a YouTube (`obtenerVideoPorId`, sin caché) | Carga con k6: de 30 s a 26 ms de p95 al votar con 60 teléfonos | **Hecho.** Los avisos salen en un hilo propio, de a uno y en orden (`ContratoSseService`, prueba `ContratoSseHiloTest`). Al saltar, un pedido validado hace menos de 30 min *(propuesto)* suena sin consultar a YouTube (doc 11); la ganancia contra el YouTube real no se midió |
| 3 | El teléfono baja el contrato entero (42 kB con zod), con los esquemas del panel y de la analítica | 220 kB frente a 180 *(propuesto)* | Tanda del backlog: partir `endpoints` por superficie (unos 20 kB, estimado) y, si no alcanza, `zod/mini`. Mientras, `rendimiento.spec.ts` no deja que crezca de 225 kB |
| 4 | Operación se vuelve a dibujar entera cada segundo (`useAhoraServidor` en el componente de la página) | Sin medir (el Profiler de React no se corrió) | Medir con el Profiler en un teléfono de gama baja; si pasa de unos ms por segundo, llevar el reloj a los componentes que muestran la hora |
| 5 | `/panel/noche` es una consulta combinada grande y se pide con cada aviso del panel | Sin medir | Medir su p95 contra el backend desplegado antes de tocarla |
| 6 | Vidrio desenfoca el fondo (`backdrop-filter`) | Sin medir | Medir en un teléfono de gama baja (fotogramas al desplazar la cola) |
| 7 | Prueba de carga del backend | Ver «Mediciones»: 60 y 200 teléfonos | **Hecho** en local con k6 (`scripts/carga/`). Falta: correrla contra el backend desplegado (red y base reales), con conexiones SSE abiertas (k6 necesita una extensión) y con pedir y buscar (necesitan YouTube) |

