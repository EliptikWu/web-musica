# Rockola Android — versión B nativa (Kotlin + Jetpack Compose, MVVM)

Réplica nativa de la web (`frontend/web`), dentro del mismo repositorio: también aplica el
`CLAUDE.md` de la raíz. Las rutas de este archivo van desde la raíz del repositorio, salvo
las del código de la app (`app/…`, `ui/…`), que van desde `frontend/movil`. La identidad
visual es la misma: mandan `docs/05-ui-ux.md` y los prototipos de `prototipos/rockola/`.
Android Studio abre `frontend/movil` como proyecto; Gradle se corre desde esa carpeta.

## Diseño
- Los tokens salen 1:1 de `packages/ui/src/tokens.css` y viven SOLO en
  ui/theme/ (Color.kt, Type.kt, Forma.kt). Ningún color, radio o tamaño suelto.
- Los degradados (luz, cromo, teclas) son Brush.verticalGradient con los mismos stops.
- Una sola familia: Archivo variable (res/font). Ancho 125 % placas, 112 % secciones,
  75 % nombres de canción y teclas (FontVariation.width). Requiere minSdk 26.
- Los títulos de canción conservan su ortografía real (DtMF, NUEVAYoL); solo las teclas
  van en mayúsculas.
- Prohibido: Material por defecto a la vista (colores morados, FAB, píldoras), emojis
  como iconos. Los iconos se redibujan como ImageVector desde packages/ui/src/Icono.tsx.
- El acento ámbar es escaso: solo la acción principal, «Tu canción» y la franja «Abierto».
- Cada pantalla parte de su prototipo y se compara con prototipos/capturas a 360 dp,
  con sus estados vacío, cargando y error.

## Skills obligatorias (igual que en la web)
- Dirección visual y composición: frontend-design:frontend-design
- Tokens y componentes compartidos: design:design-system
- Textos: design:ux-copy
- Antes de dar por buena una pantalla: design:design-critique y design:accessibility-review
  (TalkBack, objetivos táctiles ≥ 48 dp, contraste)
- Si una skill no está disponible, se dice y se pregunta antes de seguir sin ella.

## Arquitectura
- Capas: ui → viewmodel → dominio → datos. La interfaz nunca toca la red.
- Dos repositorios con la misma interfaz (`dominio/RepositorioRockola.kt`), elegidos al
  compilar en `Contenedor.kt`, como `NEXT_PUBLIC_API_MODE` en la web:
  - Sin `rockola.apiUrl`: la simulación (`datos/simulacion/`), con los datos de ejemplo de la
    web (canciones reales; votos y horas de ejemplo) y el bar «demo» sin pedir QR.
  - Con `-Prockola.apiUrl=http://…/api`: la API real (`datos/api/`), con el contrato de
    `packages/contrato` campo por campo. Toda respuesta se valida: si no cumple,
    es un error, no un dato a medias.
- Tiempo real por SSE (`datos/api/TiempoReal.kt`): una sola conexión por bar compartida por
  todas las pantallas; los reintentos y el sondeo cada 3 s están en `viewmodel/EnVivo.kt`.
- Entrada por QR: `https://<rockola.dominio>/q/<token>` o `/b/<slug>` abre la app
  (`EntradaViewModel`). El bar se recuerda; un QR viejo o un bar inexistente no lo borran.
  TBD: el dominio real y el `/.well-known/assetlinks.json` de la web con la huella de la firma
  (sin él, Android pregunta si abrir con la app o el navegador).
- Identificador del teléfono (`datos/local/AlmacenTelefono.kt`): UUID guardado en el
  teléfono, fuera de las copias de seguridad, solo en la cabecera `X-Rockola-Dispositivo`.
  Nunca va a un log ni a la pantalla.
- Identificadores del dominio en español (Solicitud, ordenarCola, Tecla).
- Cero credenciales en el código. La búsqueda en YouTube pasa por el backend.

## Dónde está cada cosa
- `ui/theme/`: tokens (colores, tipografía, radios, degradados). Único lugar con valores.
- `ui/componentes/`: Tecla, TarjetaTitulo, Arco, Ventana, Marco, LuzDeEstado, Fichas,
  Insignia, DiasSemana, Avance, Aviso, Vacio, Esqueleto, Iconos, MarcoCliente y PieCliente.
  Réplica de `packages/ui/src`; un cambio de aspecto se hace aquí, no en la pantalla.
- `ui/textos/`: textos y formatos, copiados de `frontend/web/lib/textos/cliente.ts` y `lib/formato.ts`.
  Diferencia a propósito: la app no dice «sin descargas» (en la versión nativa sí se instala).
- `ui/pantallas/`: Bienvenida, Buscar, Cola, Política, Entrada (avisos del QR) y AppRockola
  (navegación por pestañas).
- `datos/api/`: cliente HTTP (OkHttp), esquemas del contrato (kotlinx.serialization) y SSE.
- `viewmodel/PantallasDelBar.kt`: los ViewModel del bar actual; al cambiar de bar se cierran.

## Trampas conocidas
- `dropShadow` con `spread` negativo (Compose 1.10) pinta un rectángulo de bordes duros. Las
  sombras CSS con extensión negativa se aproximan sin ella (ver Arco en Piezas.kt).
- Teclas y columnas de la tarjeta usan medidas mínimas, no fijas: con la letra del sistema
  al 200 % el texto debe caber. El pie limita su letra a 1,5×.
- `widthIn(max = …)` va antes de `fillMaxSize()`/`fillMaxWidth()`; al revés el tope no hace
  nada y en tableta todo se estira (pasó en MarcoCliente).
- Cada pantalla tiene su ViewModel y todos escuchan el tiempo real: sin compartir la conexión
  (TiempoRealCompartido) serían 4 conexiones SSE por teléfono.
- En SSE, el JSON de `data:` va en una sola línea.

## Verificar
- Solo en depuración, como `?escenario=` de la web:
  `adb shell am start -S -n com.example.rokola/.MainActivity --es pantalla cola --es escenario en-pausa`
  (pantallas: inicio, buscar, cola, politica; escenarios en datos/simulacion/Semilla.kt).
- Captura: `adb exec-out screencap -p > captura.png`, comparada con
  `prototipos/capturas/rockola/`. A 360 dp: `adb shell wm density 480` en un
  emulador de 1080 px (`adb shell wm density reset` para volver).
- Contra una API local: `adb reverse tcp:8085 tcp:8085` y
  `./gradlew installDebug -Prockola.apiUrl=http://localhost:8085/api` (HTTP sin cifrar solo
  se permite en depuración hacia localhost y 10.0.2.2: `src/debug/res/xml/red.xml`). Un QR:
  `adb shell am start -a android.intent.action.VIEW -d https://rockola.invalid/q/<token> com.example.rokola`.
- Tableta: `adb shell wm size 1600x2560` y `wm density 320` (horizontal: `2560x1600`); la
  columna del cliente se queda en 480 dp, centrada, como en la web.
- `./gradlew testDebugUnitTest lintDebug` en verde antes de cada commit.
- Comentarios, textos de interfaz y commits en español (Colombia).
