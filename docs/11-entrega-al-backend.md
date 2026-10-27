# 11 — Entrega al equipo de backend

Qué recibe el equipo de backend, cómo se conecta el frontend a su API y qué falta decidir.
El detalle del contrato está en el [doc 04](04-modelo-de-datos-y-contrato.md); este documento
es la puerta de entrada.

## Qué se entrega

| Pieza | Dónde | Para qué |
|---|---|---|
| Contrato propuesto (fuente) | `packages/contrato/src/*.ts` | Esquemas zod de cada cuerpo y respuesta, y el registro de las 67 operaciones (`endpoints.ts`) |
| OpenAPI generado | `packages/contrato/generado/openapi.json` | Lo mismo en formato estándar, para generar servidor, clientes o pruebas |
| Resumen legible | `packages/contrato/generado/endpoints.md` | Una tabla por superficie: método, ruta, rol y errores |
| Reglas y decisiones | Doc 04 (entidades, estados, permisos, eventos, errores) y doc 03 (ADR) | Por qué el contrato es así |
| Pendientes | Doc 04, «Pendiente del backend», y README, «Decisiones abiertas» | Lo que el frontend no puede decidir solo |

Todo es **propuesta**: lo que el backend cambie se corrige en `packages/contrato`, se
regenera con `npm run generar` y la compilación y las pruebas dicen qué se rompió en la
interfaz.

## Cómo habla el frontend con la API

- **Mismo origen.** El navegador solo llama a `/api/*` de la propia app. La ruta
  `apps/web/app/api/[...path]/route.ts` reenvía a `ROCKOLA_API_URL` (por ejemplo,
  `/api/bares/demo/ahora` → `${ROCKOLA_API_URL}/bares/demo/ahora`), con el método, el cuerpo,
  la consulta y las cabeceras. La CSP solo permite `connect-src 'self'`.
- **Cabeceras propias.** `X-Rockola-Dispositivo` (UUID anónimo del teléfono) en todo lo del
  cliente; `Idempotency-Key` en las operaciones marcadas `idempotente` (pedir, votar, mover el
  voto, los reportes de la pantalla y, en el panel, saltar, retirar, bloquear y crear un
  usuario): la misma clave tiene que devolver la misma respuesta.
- **Sesión del panel.** Cookie `httpOnly` que pone el backend al entrar (el frontend no la
  lee). Al ir por el mismo origen, es de primera parte. Un 401 en cualquier pantalla lleva a
  `/panel/entrar?motivo=caducada`.
- **Errores.** Siempre `{ codigo, mensaje, detalle? }` con los códigos del doc 04; la
  interfaz decide el texto por `codigo`, nunca muestra `mensaje` al público.
- **Fechas.** ISO 8601 con zona. Cada foto en vivo trae `servidor_ahora` para corregir el
  reloj del teléfono.
- **Tiempo real.** SSE en `GET /bares/{slug}/eventos`: primero una foto `ahora` completa,
  después `ahora`, `panel` (los recursos que cambiaron) y `ping` cada 15 s. Sin SSE, la
  interfaz sondea cada 3 s.
- **Validación.** El cliente valida cada respuesta con el esquema del contrato: una
  respuesta que no cumple es un error en la interfaz, no un dato a medias.

## Cómo se conecta

1. Compilar con `NEXT_PUBLIC_API_MODE=real` (se fija al compilar: la simulación queda fuera
   del paquete; medido el 2026-10-27, no queda nada de MSW en los archivos).
2. Arrancar con `ROCKOLA_API_URL` apuntando a la API. Si delante hay un proxy de confianza
   que reescribe `X-Forwarded-*`, añadir `ROCKOLA_CONFIAR_PROXY=1`; si no, esas cabeceras
   no se reenvían y el backend no debe usar la IP para limitar intentos.
3. Probar a mano los recorridos del doc 09 (pedir, votar, mover el voto, la TV, el panel).
   Las pruebas automáticas de extremo a extremo corren contra la simulación (usan sus
   escenarios por URL): sirven para la interfaz, no para la API.
4. Para comprobar la API contra el contrato, validar sus respuestas con los esquemas de
   `packages/contrato` o con el OpenAPI.

## Lo que el frontend no hace

- Llamar a YouTube: la búsqueda pasa por el backend (cuota, caché y claves, RF-008, PB-54).
- El pipeline de analítica (RF-047, RF-048) ni los agregados del tablero (ADR-18).
- Sonido del local (PB-52) y respaldo de las más votadas (PB-53): no tienen pantalla.
- Registrar los eventos de uso (RF-046), incluido el cambio de voto.

## Pendientes que necesitan respuesta

Están numerados en el doc 04, «Pendiente del backend». Los que más afectan a la interfaz:
transporte y autenticación del tiempo real, autenticación de la pantalla del bar, matriz
del Operador (D9), estado `retirada`, si la canción que suena cuenta para el cupo, origen
del género y de la popularidad, y los valores de bloqueo de cuenta.
