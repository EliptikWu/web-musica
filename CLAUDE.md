# Rockola — instrucciones para Claude

Rockola es una **rockola digital colaborativa para bares**. El cliente escanea un QR sin
instalar ni registrarse, busca en YouTube, pide canciones y vota. La cola pública se
ordena por votos, la pantalla del bar las reproduce y el personal administra el servicio
desde un panel. Es el proyecto nuclear de Ingeniería de Software (Universidad Alexander
von Humboldt, 2026).

**Este repositorio es solo el frontend.** El backend (base de datos, reglas del servidor,
YouTube Data API, tiempo real, analítica con Spark) lo hace otro equipo. Aquí no se
construye backend: mientras no exista, la app corre contra una **simulación en el
navegador** que cumple el contrato propuesto (doc 04). Integrar es apuntar a la API real.

## Estado actual

- Iteración en curso y registro de cada una: `docs/08-metodologia.md`, «Registro de
  iteraciones».
- Las iteraciones van seguidas (decisión del usuario, 2026-10-26). Cada una cierra con
  capturas, revisión de diseño y accesibilidad, `npm run check` en verde, barrido y un
  **commit local**. No se sube nada a ningún remoto sin pedirlo.
- Rockola corre en el puerto **3200** (el 3000 lo usan otros proyectos del equipo).

Comandos (desde la raíz):

| Para | Comando |
|---|---|
| Arrancar en local (modo simulado) | `npm run dev` → `http://localhost:3200` |
| Todo lo que correría el CI | `npm run check` (o `npm run check -- --sin-e2e`) |
| Una parte | `npm run lint\|typecheck\|test\|build\|e2e -w @rockola/web` |
| Regenerar el contrato para el backend | `npm run generar` (el chequeo falla si `packages/contrato/generado/` no está al día) |
| Imágenes del manual de uso | `npm run capturas -w @rockola/web` → `apps/web/public/manual/` (rehacerlas si cambia una pantalla que el manual muestra) |
| Texto de los documentos fuente | `python -I scripts/extraer-fuentes.py` |

- Las pruebas de extremo a extremo usan el Edge instalado y la app ya compilada
  (`npm run build -w @rockola/web`).
- En la simulación: `?escenario=<nombre>&reloj=fijo|rapido&reiniciar` en cualquier URL;
  panel con `admin@rockola.demo` u `operador@rockola.demo` y clave `rockola-demo`.
- Los tokens de diseño viven solo en `packages/ui/src/tokens.css`.
- `prototipos/` es referencia: nada de `apps/` lo importa.

## La documentación manda

`docs/` es la fuente de verdad. Léela antes de proponer o cambiar algo; el índice, las
decisiones tomadas y las abiertas están en `docs/README.md`. Los documentos originales
del equipo están en `docs/fuente/` (con su texto en `docs/fuente/extraido/`): si un doc
numerado y la fuente chocan, se corrige el doc numerado y se anota por qué.

| Si vas a tocar… | Lee primero |
|---|---|
| Alcance, roles, iteraciones, glosario | `docs/01-alcance-y-definiciones.md` |
| Cualquier funcionalidad | `docs/02-requisitos.md` (y el criterio completo en `docs/fuente/extraido/`) |
| Stack, carpetas, simulación, tiempo real, seguridad, decisiones | `docs/03-arquitectura.md` |
| Datos, estados, endpoints o eventos | `docs/04-modelo-de-datos-y-contrato.md` y `packages/contrato` |
| Cualquier pantalla, componente, token o texto | `docs/05-ui-ux.md` y `prototipos/` |
| Qué significa "hecho" | `docs/06-calidad.md` |
| Rendimiento | `docs/07-optimizaciones.md` |
| Iteraciones, commits, entregas | `docs/08-metodologia.md` |
| Integración con la API del backend | `docs/11-entrega-al-backend.md` |
| Pruebas | `docs/09-pruebas-qa.md` |
| Un error | `docs/10-correccion-de-errores.md` |

Reglas:

- Un requisito nuevo recibe su ID en el doc 02 **antes** de implementarse.
- Una decisión técnica es una fila nueva en el registro del doc 03, con motivo y
  alternativa descartada. Una decisión aceptada no se edita: se reemplaza.
- La documentación afectada se actualiza en el mismo cambio que altera el
  comportamiento.
- Un cambio del contrato (`packages/contrato`) se refleja en el doc 04 y se regenera
  `packages/contrato/generado/`; es lo que el equipo de backend lee.

## Prohibido inventar

- Lo que depende del backend o de YouTube (campos, límites, códigos de error) se
  verifica en la fuente antes de darlo por cierto. Lo no verificado se marca `TBD` y
  entra en «Pendiente del backend» del doc 04.
- Las metas con número sin respaldo de una medición se marcan *(propuesto)*.
- Canciones y artistas de ejemplo son reales; votos, horas, métricas e historial de la
  simulación son **datos de ejemplo** y la interfaz lo dice donde puedan confundirse
  con datos reales (analítica). Nunca se presentan como medidos.
- Lo que falta del equipo va entre corchetes o como `TBD`: nombre real del bar, texto
  legal de la política de datos, IDs de video verificados.

## Seguridad — no negociable

- Cero credenciales en código, scripts, pruebas o documentación: todo por variables de
  entorno (`.env.example` lista las que existen, sin valores reales).
- La llave de YouTube **nunca** llega al navegador: las búsquedas pasan por la API.
- El panel solo confía en la sesión que da el backend (cookie httpOnly). La interfaz
  oculta lo que el rol no permite, pero **quien decide es el servidor**; la matriz
  rol × acción está en `apps/web/lib/permisos.ts` y en el doc 04.
- La CSP con nonce de `apps/web/proxy.ts` no se relaja para "que funcione": nada de
  `style=""` en el HTML del servidor ni scripts en línea sin nonce.
- El identificador del teléfono es seudónimo (Ley 1581 de 2012, RF-002, RNF-007): no se
  registra ni se muestra completo.
- Un cambio que toque sesión, permisos, la CSP, el proxy hacia la API o datos del
  teléfono se revisa dos veces (`security-review` y `code-review`).

## Diseño — que no parezca una plantilla

La identidad y las reglas completas están en `docs/05-ui-ux.md`. Lo esencial:

- **Dirección Rockola:** el teléfono es el panel de selección de una rockola. Arco de luz
  (lo que suena), tarjetas de título (cada canción), teclas («Votaste» es la tecla
  hundida) y fichas (los pedidos). La luz es el estado: encendida se reciben canciones;
  apagada, no.
- **Solo tokens Rockola**, definidos en un único lugar: `packages/ui/src/tokens.css`
  (nogal, madera, crema, cereza, ámbar, tinta, cromo). Ningún color, tipografía o radio
  suelto. Tailwind solo conoce esos tokens.
- **Una sola familia:** Archivo con su eje de ancho (125 % placas, 112 % secciones, 75 %
  nombres de canción y teclas). Los títulos de canción conservan su ortografía real
  (DtMF, NUEVAYoL); solo las teclas van en mayúsculas.
- **Sin recursos de plantilla:** nada de titular centrado con tres tarjetas, degradados
  morados, vidrio esmerilado, píldoras ni emojis como iconos. Iconos dibujados a mano
  en `packages/ui`, solo los que tienen función.
- **El acento es escaso:** la luz ámbar solo en la acción principal, «Tu canción» y la
  franja «Abierto».
- **Toda pantalla** parte de un prototipo de `prototipos/` o se diseña en el sistema
  Rockola antes de programarla, y se comprueba en el navegador a 360, 768 y 1440 px (la
  TV a 1280 × 720 y 1920 × 1080), con sus estados vacío, cargando y error.

### Las skills de diseño son obligatorias

**Importante: ningún trabajo de diseño se hace de memoria.** Antes de diseñar, maquetar o
retocar una pantalla, un componente, un token o un texto de interfaz, se cargan con la
herramienta Skill las que correspondan al paso. Vale igual para un cambio pequeño que
para una pantalla nueva.

| Cuándo | Skill |
|---|---|
| Al decidir dirección visual, composición o tipografía | `frontend-design:frontend-design` |
| Al tocar tokens o componentes compartidos (`packages/ui`) | `design:design-system` |
| Al comparar variantes abiertas | `playground:playground` |
| Al escribir textos de interfaz, vacíos y errores | `design:ux-copy` |
| Antes de dar por buena una pantalla | `design:design-critique` y `design:accessibility-review` |
| Al documentar una pantalla para el equipo | `design:design-handoff` |
| Al hacer una gráfica (analítica) | `dataviz` |
| Al preparar o resumir pruebas con usuarios | `design:user-research` y `design:research-synthesis` |

- Las skills aportan método y criterio; la identidad la fijan los prototipos y el doc 05.
  Si chocan, gana el doc 05.
- Si una skill no está disponible en la sesión, se dice y se pregunta antes de seguir
  sin ella. `modern-web-guidance` (que usa lk-crm) no estaba disponible el 2026-10-26.
- Se permiten reworks del prototipo cuando la crítica lo justifique: se anota en el
  doc 05 qué cambió y por qué.
- Al informar se nombra qué skills se usaron y qué pantallas se revisaron con captura.

## Skills: no solo las de diseño

Una skill es método ya escrito para un tipo de trabajo. **Antes de empezar uno, se mira
si hay una que aplique y se usa.** Si surge algo para lo que no hay ninguna, se busca,
se comprueba quién la revisó y qué añade (ganchos, servidores, credenciales) y se
pregunta antes de instalar lo que no sean solo skills.

| Cuándo | Skill |
|---|---|
| Lo que «se revisa dos veces» (sesión, permisos, CSP, proxy) | `security-review` y `code-review` |
| Al barrer lo recién hecho (doc 10) | `code-review`; `simplify` para lo que sobre |
| Al comprobar la app en marcha | `run` |
| Al leer o regenerar los documentos fuente | `anthropic-skills:docx`, `anthropic-skills:xlsx` |
| Al investigar algo externo con varias fuentes (YouTube, Ley 1581) | `anthropic-skills:deep-research` |

La lista de una sesión puede cambiar: manda la que la sesión enseña, no esta tabla.

## Procedimiento de ingeniería

1. **Explorar:** leer el documento que aplica y el código existente antes de proponer, y
   mirar si hay una skill para lo que se va a hacer.
2. **Diagnosticar:** causa raíz comprobable (archivo y línea, y por qué), no síntomas.
3. **Diseñar a la altitud correcta:** un problema de una capa compartida (`packages/ui`,
   `packages/contrato`, `lib/`) se corrige en esa capa, no con un caso especial donde se
   notó. Reutilizar antes de crear.
4. **Implementar** siguiendo el estilo del código que lo rodea. KISS y YAGNI: lo que no
   está en el doc 02 no se construye.
5. **Verificar** antes de dar algo por hecho: pruebas, tipos y comprobación en el
   navegador. Ver la definición de hecho en el doc 06.
6. **Barrer** lo recién hecho antes de darlo por terminado: releerlo entero con otros
   ojos, usarlo fuera del camino feliz, mirar los avisos además de los fallos y corregir
   lo que salga con su prueba. Los cinco pasos están en el doc 10, «El barrido».
7. **Informar con exactitud:** qué se hizo, qué se verificó, qué salió en el barrido y
   qué quedó sin verificar.

## Convenciones

- Idioma: documentación, comentarios, textos de interfaz y mensajes de commit en
  español (Colombia). **Identificadores del dominio también en español** (`solicitud`,
  `ordenarCola`, `Tecla`), igual que el diccionario de datos y los prototipos; en inglés
  solo lo que impone el framework (`page.tsx`, `useQuery`). Ver ADR-05 del doc 03.
- Campos del contrato en `snake_case` español (`votos_total`, `fecha_solicitud`).
- Componentes en `PascalCase.tsx`; el resto de archivos en `kebab-case.ts`; pruebas
  junto a su fuente (`*.test.ts`).
- Commits con prefijo: `feat:`, `fix(área):`, `docs:`, `test:`, `chore:`, explicando el
  porqué y citando los ID de historia y requisito (PB-xx, RF-xxx).
- Un cambio que solo mueve archivos va en su propio commit.
