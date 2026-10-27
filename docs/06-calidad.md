# 06 — Modelo de calidad

Se usa ISO/IEC 25010 como lista de atributos, ordenados por lo que más importa en el
frontend de Rockola. Cada atributo remite a los RNF del doc 02.

## Atributos priorizados

| Prioridad | Atributo | Qué significa aquí | Se mide con |
|---|---|---|---|
| 1 | Seguridad y privacidad | Nadie opera el panel sin sesión ni hace lo que su rol no permite; el teléfono no deja datos personales | RNF-006, RNF-007; matriz de permisos (doc 09) |
| 2 | Adecuación funcional | Pedir, votar y ver la cola funcionan como dicen los criterios de aceptación | RF del doc 02 |
| 3 | Usabilidad | Se usa sin instrucciones, con una mano y con poca luz | RNF-001, RNF-005; revisión de diseño y accesibilidad |
| 4 | Fiabilidad | Recargar, perder la señal o tocar dos veces no altera la cola ni duplica votos | RNF-008, RNF-012 |
| 5 | Eficiencia de desempeño | Abre rápido desde el QR y responde al instante | RNF-002, RNF-009; doc 07 |
| 6 | Mantenibilidad | El otro equipo y quien venga después lo cambian sin miedo | RNF-013 |
| 7 | Compatibilidad | Navegadores de Android e iOS, computador y TV | RNF-004 |
| 8 | Portabilidad | Cambiar de backend es cambiar una variable | ADR-04, ADR-06 |

Cuando dos atributos chocan, gana el de mayor prioridad. Ejemplo: mostrar el código del
teléfono que pidió cada canción ayuda al personal, pero solo se muestra en el panel y
abreviado; en lo público, gana la privacidad.

## Principios de diseño

- **KISS:** la solución más directa que cumpla el requisito.
- **YAGNI:** lo que no está en el doc 02 no se hace; si hace falta, primero entra al doc 02.
- **SOLID con criterio:**
  - Responsabilidad única: las reglas (`lib/reglas`) no conocen React ni la red.
  - Abierto/cerrado: un transporte de tiempo real nuevo es otra implementación de
    `FuenteTiempoReal`, no un cambio en los componentes.
  - Inversión de dependencias: los componentes reciben datos por hooks, no hacen `fetch`.
- **Regla de altitud:** un error de una capa compartida (`packages/ui`,
  `packages/contrato`, `lib/`) se corrige en esa capa, no con un caso especial donde se
  notó.
- **No inventar:** lo que depende del backend o de YouTube se verifica o queda `TBD`.

## Reglas de código

- TypeScript `strict` con `noUncheckedIndexedAccess`; nada de `any` sin motivo escrito.
- ESLint de Next y React; el chequeo falla si no se cumple.
- La lógica que merece prueba vive en módulos puros; las páginas y los componentes se
  mantienen delgados.
- Los errores no se silencian: se muestran con un texto útil o se propagan con contexto.
  No hay bloques `catch` vacíos.
- Los mensajes al usuario nunca incluyen trazas, códigos ni detalles internos.
- Toda respuesta del backend se valida con el esquema del contrato antes de usarse.
- Comentarios solo donde el porqué no es evidente.

## Métricas que se vigilan

| Métrica | Umbral | Dónde |
|---|---|---|
| Cobertura de `lib/reglas` y del motor de simulación | 80 % o más *(propuesto)* | Vitest |
| Matriz de permisos del panel | 100 % de las acciones probadas | e2e |
| Vulnerabilidades altas en dependencias de producción | 0 | `npm audit` |
| Violaciones de axe (wcag2a/aa, 21aa) | 0 en cada estado visitado | e2e |
| JS inicial de la ruta del cliente | Ver doc 07 *(propuesto)* | `next build` |

## Definición de hecho

Un cambio está hecho cuando se cumple todo esto:

1. Cumple el requisito y su criterio de aceptación.
2. `npm run check` está en verde: lint, tipos, pruebas, compilación y e2e.
3. Tiene pruebas que fallarían sin el cambio.
4. Si toca permisos, la matriz del doc 04 y su prueba están al día.
5. Si toca la interfaz, se comprobó con captura a 360, 768 y 1440 px (TV a 1280 × 720 y
   1920 × 1080) en todos sus estados, y pasó `design-critique` y `accessibility-review`.
6. Si toca el contrato, se regeneró `packages/contrato/generado/` y se actualizó el doc 04.
7. La documentación afectada se actualizó en el mismo cambio.
8. Pasó su barrido (doc 10).
9. Funciona contra el backend real. Mientras no exista, se comprueba en modo simulado y
   queda anotado en el doc 08.

Una iteración está hecha cuando sus historias cumplen sus criterios y no quedan errores
críticos ni altos abiertos.

## Revisión

Sin revisión entre pares en cada cambio, se sustituye por:

- Revisión propia del diff completo antes del commit.
- `code-review` en cada barrido; `security-review` además en lo que toque sesión,
  permisos, CSP, proxy o datos del teléfono.
- Antes de integrar con el backend, el equipo de backend revisa el doc 04 y
  `packages/contrato/generado/`.
