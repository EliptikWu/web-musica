# @rockola/web

La app de Rockola (Next.js 16, App Router): el cliente (`/b/[bar]`), la pantalla del bar
(`/b/[bar]/pantalla`) y el panel (`/panel`). Ver `docs/03-arquitectura.md`.

## Arrancar en local

Desde la raíz del repositorio:

```
npm install
npm run dev
```

Y abrir `http://localhost:3200` (Rockola usa el 3200 para no chocar con otros proyectos).
Sin backend, la app corre contra la **simulación** del navegador: la portada lleva a las
tres pantallas y a los escenarios. Panel: `admin@rockola.demo` u `operador@rockola.demo`,
clave `rockola-demo` (solo existen en la simulación).

Parámetros de la simulación en cualquier URL: `?escenario=<nombre>` (ver
`simulacion/datos/semilla.ts`), `&reloj=fijo|rapido` y `&reiniciar`.

## Comprobar

```
npm run lint -w @rockola/web
npm run typecheck -w @rockola/web
npm run test -w @rockola/web
npm run build -w @rockola/web
npm run e2e -w @rockola/web
```

`npm run check`, desde la raíz, lo pasa todo. Las pruebas de extremo a extremo usan el Edge
instalado y la app ya compilada.

## Lo que conviene saber

- **El navegador no llama al backend ni a YouTube.** Todo va a `/api/*` del mismo origen;
  en modo real `app/api/[...path]/route.ts` lo reenvía a `ROCKOLA_API_URL`.
- **Datos en vivo, desde el cliente.** Las páginas del servidor dan el armazón; los datos
  los piden los hooks con TanStack Query y se validan con `@rockola/contrato`.
- **Sin `style=""`.** La CSP con nonce lo bloquea; ESLint lo prohíbe. Los valores
  dinámicos (el avance de una canción) se ponen desde una ref.
- **La simulación vive en `simulacion/`** y solo se carga en modo simulado
  (`instrumentation-client.ts`). Su motor es puro y tiene pruebas (`motor.test.ts`).
