package com.proyectoN.rokolaBackend.acceso

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/** Página mínima generada desde el BACKEND. Al abrir el QR hace POST automáticamente. */
@RestController
class QrRedirectController(private val acceso: AccesoService) {
  @GetMapping("/q/{token}", produces = [MediaType.TEXT_HTML_VALUE])
  fun abrir(@PathVariable token: String): ResponseEntity<String> {
    val establecimiento = acceso.resolverQr(token) // Valida QR activo; GET no inserta datos.
    val nombreSeguro = escaparHtml(establecimiento.nombre)
    val html = """
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <meta name="referrer" content="no-referrer">
              <title>Acceso Rokola</title>
              <style>
                body{font-family:system-ui,Arial,sans-serif;max-width:560px;margin:48px auto;padding:0 20px;line-height:1.5}
                .muted{color:#555}.ok{color:#126b36}.error{color:#a11}
              </style>
            </head>
            <body>
              <h1>Rokola</h1>
              <h2>$nombreSeguro</h2>
              <p id="estado" role="status">Registrando tu dispositivo…</p>
              <p class="muted">Para reconocerte en próximas visitas guardamos un identificador aleatorio en este navegador y una huella seudónima en el servidor. No solicitamos nombre ni correo.</p>
              <p class="muted">Este acceso no registra aceptación del aviso de privacidad. La aceptación se solicitará antes de usar funciones que la requieran.</p>
              <script>
                (async function () {
                  const token = "__TOKEN__";
                  const establecimientoId = __ESTABLECIMIENTO_ID__;
                  const llaveLocal = "rokola.device." + establecimientoId;
                  const estado = document.getElementById("estado");
                  let guardado = null;
                  try { guardado = localStorage.getItem(llaveLocal); } catch (_) {}
                  async function enviar(deviceToken) {
                    const datos = { tipo: /iPad|Tablet/i.test(navigator.userAgent) ? "tableta" : "movil", aceptaAvisoPrivacidad: false };
                    if (deviceToken) datos.deviceToken = deviceToken;
                    return fetch("/api/acceso/qr/" + token + "/dispositivos", {
                      method: "POST",
                      headers: { "Content-Type": "application/json" },
                      body: JSON.stringify(datos),
                      cache: "no-store",
                      credentials: "same-origin"
                    });
                  }
                  try {
                    let respuesta = await enviar(guardado);
                    // Si un identificador local viejo ya no sirve, reintenta como nuevo.
                    if (respuesta.status === 400 && guardado) {
                      try { localStorage.removeItem(llaveLocal); } catch (_) {}
                      respuesta = await enviar(null);
                    }
                    if (!respuesta.ok) throw new Error("Error HTTP " + respuesta.status);
                    const registro = await respuesta.json();
                    try { localStorage.setItem(llaveLocal, registro.deviceToken); } catch (_) {}
                    estado.className = "ok";
                    estado.textContent = "Dispositivo registrado correctamente. ID: " + registro.dispositivoId;
                  } catch (e) {
                    estado.className = "error";
                    estado.textContent = "No se pudo registrar el dispositivo: " + e.message;
                  }
                })();
              </script>
            </body>
            </html>
        """.trimIndent()
      .replace("__TOKEN__", token)
      .replace("__ESTABLECIMIENTO_ID__", establecimiento.establecimientoId.toString())
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .header("Referrer-Policy", "no-referrer")
      .body(html)
  }

  private fun escaparHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")
}
