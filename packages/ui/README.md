# @rockola/ui

El sistema de diseño Rockola (doc 05): **el único lugar donde viven los tokens**
(`src/tokens.css`), el tema de Tailwind que solo conoce esos tokens (`src/theme.css`) y
las piezas de la rockola (tecla, tarjeta de título, arco, fichas, luz de estado, avance,
días, iconos). Portado de `prototipos/rockola/estilos.css`.

- Se ve entero en `/muestras` con la app en modo simulado.
- Los componentes no conocen datos ni rutas: reciben props.
- `claseTecla()` y `TeclaFija` sirven en el servidor; `Tecla` y `Avance` son de cliente.
