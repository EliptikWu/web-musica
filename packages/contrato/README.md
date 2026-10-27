# @rockola/contrato

El contrato que el frontend propone al equipo de backend (doc 04): esquemas zod de cada
respuesta y cuerpo, y la lista de endpoints (`src/endpoints.ts`), que es la única fuente
para el cliente de la app, la simulación y la documentación.

```
npm run generar -w @rockola/contrato   # generado/openapi.json y generado/endpoints.md
npm run test -w @rockola/contrato
```

`generado/` no se edita a mano; el chequeo falla si no está al día.
