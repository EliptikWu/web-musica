# Rockola

Rockola digital colaborativa para bares: el público escanea un QR, pide canciones de
YouTube y vota; la cola se ordena por votos, la pantalla del bar las reproduce y el
personal administra desde un panel.

El equipo de frontend hace la web y la app Android; el backend lo hace otro equipo en
`rokolaBackend/`. Mientras no haya API, la web y la app corren contra una simulación.

| Carpeta | Qué es |
|---|---|
| `frontend/web` | La app web (Next.js): cliente, pantalla del bar y panel |
| `frontend/movil` | La app Android del cliente (Kotlin + Jetpack Compose); se abre con Android Studio |
| `rokolaBackend/` | El backend (Spring Boot + Kotlin), del equipo de backend |
| `packages/ui` | Sistema de diseño Rockola: tokens y componentes |
| `packages/contrato` | Contrato propuesto con el backend (esquemas zod → OpenAPI) |
| `docs/` | Documentación (empieza por `docs/README.md`) |
| `prototipos/` | Prototipos y capturas de la dirección Rockola |

Para trabajar en el proyecto, lee primero `CLAUDE.md` y `docs/README.md`.
