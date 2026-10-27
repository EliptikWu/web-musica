# 10 — Corrección de errores

## Qué cuenta como error

Cualquier diferencia entre lo que la interfaz hace y lo que dicen los requisitos del
doc 02 o el diseño del doc 05, y cualquier cosa que una persona no logra hacer sin ayuda.
Una petición de algo que nunca estuvo previsto no es un error: es un requisito nuevo y
entra primero al doc 02.

## De dónde llegan

| Fuente | Cómo entra |
|---|---|
| Pruebas | Una prueba que falla en `npm run check` |
| El barrido | La pasada que cierra cada iteración y cada encargo |
| Revisión de diseño o accesibilidad | Hallazgos de `design-critique` y `accessibility-review` |
| Equipo de backend | Diferencias de contrato al integrar |
| Pruebas con usuarios | Lo que alguien no logra hacer en el bar |

Lo que no se corrige en el momento se anota en el registro del doc 08, columna
«Pendiente», con su severidad.

## El barrido

Al terminar cada iteración, y cada encargo que cambie código, se hace una pasada de
búsqueda de errores sobre lo recién hecho, antes de darlo por hecho. Son cinco pasos:

1. **Releer** el cambio entero con otros ojos: casos límite, caminos de error, permisos,
   lo que queda a medias, textos que ya no son verdad.
2. **Usarlo de verdad** fuera del camino feliz: entradas malas, doble toque, botón atrás,
   sesión caducada, servicio caído, YouTube caído, sin conexión, solo con teclado, a
   360 px, con la red lenta (dos acciones seguidas antes de que llegue la primera
   respuesta) y con varias pestañas a la vez. Si hay pantallas, la auditoría de
   accesibilidad pasa por **todos** sus estados, no solo por el normal.
3. **Mirar los avisos**, no solo los fallos: compilador, consola del navegador, React
   (claves, hidratación), pruebas que pasan con advertencias.
4. **Tratar cada error con el flujo de abajo:** reproducir, prueba que falla, corregir en
   la capa que toca; la prueba se queda.
5. **Informar:** qué se encontró y se corrigió, qué se encontró y no se corrigió y por
   qué, y qué no se llegó a mirar.

Un encargo que solo toca documentos lleva un repaso de coherencia (citas, IDs, fechas,
cosas que dejaron de ser verdad), no el barrido entero.

## Severidad

| Nivel | Definición | Ejemplo | Respuesta *(propuesto)* |
|---|---|---|---|
| Crítico | El panel se opera sin sesión o fuera del rol, se expone el ID del teléfono, o se duplican votos | Un Operador cambia el horario | Se atiende de inmediato |
| Alto | Una función principal no sirve y no hay alternativa | No se puede pedir una canción | En la iteración en curso, antes de cerrarla |
| Medio | Algo falla pero se puede seguir | La espera estimada no se actualiza | En la iteración en curso o la siguiente |
| Bajo | Detalle estético o molestia menor | Un texto desalineado | Cuando haya hueco |

Un error de seguridad o privacidad es crítico por defecto hasta que se demuestre que no
lo es.

## Flujo de un error

```
Reportado → Reproducido → Diagnosticado → Corregido → Verificado → Cerrado
```

1. **Reportar:** qué se esperaba, qué pasó, pasos, navegador o dispositivo y captura.
2. **Reproducir:** si no se repite, se pide más información; no se corrige a ciegas.
   En modo simulado, el escenario (`?escenario=`) ayuda a repetirlo.
3. **Diagnosticar:** causa raíz comprobable: archivo y línea, y por qué ocurre.
4. **Escribir la prueba que falla** antes de tocar el código.
5. **Corregir a la altitud correcta.**
6. **Verificar:** la prueba nueva pasa y el resto sigue en verde.
7. **Cerrar.**

La prueba del paso 4 se queda para siempre: es lo que impide que el error vuelva.

## Regresiones

- Toda corrección lleva su prueba.
- Si un error corregido reaparece, se trata como alto como mínimo y se revisa por qué la
  prueba no lo detectó.
