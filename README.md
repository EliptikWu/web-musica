# Rockola — frontend

Rockola digital colaborativa para bares: el público escanea un QR, pide canciones de
YouTube y vota; la cola se ordena por votos, la pantalla del bar las reproduce y el
personal administra desde un panel.

Este repositorio es **solo el frontend** (el backend lo hace otro equipo). Mientras no
haya API, la app corre contra una simulación en el navegador.

| Carpeta | Qué es |
|---|---|
| `apps/web` | La app (Next.js): cliente, pantalla del bar y panel |
| `packages/ui` | Sistema de diseño Rockola: tokens y componentes |
| `packages/contrato` | Contrato propuesto con el backend (esquemas zod → OpenAPI) |
| `docs/` | Documentación (empieza por `docs/README.md`) |
| `prototipos/` | Prototipos y capturas de la dirección Rockola |

Para trabajar en el proyecto, lee primero `CLAUDE.md` y `docs/README.md`.
