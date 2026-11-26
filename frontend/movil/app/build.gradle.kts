plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/*
 * Como NEXT_PUBLIC_API_MODE en la web: se fija al compilar. Sin `rockola.apiUrl` la app usa
 * la simulación; con ella, la API real (p. ej. `-Prockola.apiUrl=http://10.0.2.2:8080/api`
 * o en ~/.gradle/gradle.properties). Ninguna credencial va aquí.
 */
val apiUrl = (findProperty("rockola.apiUrl") as String?) ?: System.getenv("ROCKOLA_API_URL") ?: ""
/* Dominio de la web, donde vive el QR (https://<dominio>/q/<token>). TBD: el dominio real. */
val dominio = (findProperty("rockola.dominio") as String?) ?: "rockola.invalid"

/* Con la API real y sin dominio, el escáner rechazaría todo QR de la web (RF-066). */
val verificarDominio = tasks.register("verificarDominioRelease") {
    val api = apiUrl
    val dominioFijado = dominio
    doLast {
        if (api.isNotEmpty() && dominioFijado == "rockola.invalid") {
            throw GradleException("Falta -Prockola.dominio: el APK con API real necesita el dominio de la web.")
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(verificarDominio) }

/*
 * Firma del APK de release (el que se reparte). El almacén y su clave viven fuera del
 * repositorio: en ~/.gradle/gradle.properties o en variables de entorno. Sin ellos, el release
 * sale sin firmar (`-unsigned`) y Android no lo instala.
 */
fun propiedad(nombre: String, entorno: String) = (findProperty(nombre) as String?) ?: System.getenv(entorno)
val almacenFirma = propiedad("rockola.firma.almacen", "ROCKOLA_FIRMA_ALMACEN")

base { archivesName = "rockola" }

android {
    namespace = "com.example.rokola"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.rokola"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "ROCKOLA_API_URL", "\"${apiUrl.trimEnd('/')}\"")
        manifestPlaceholders["dominioRockola"] = dominio
        buildConfigField("String", "ROCKOLA_DOMINIO", "\"$dominio\"")
    }

    signingConfigs {
        if (almacenFirma != null) {
            create("release") {
                storeFile = file(almacenFirma)
                // PKCS12: la llave usa la misma clave que el almacén.
                storePassword = propiedad("rockola.firma.clave", "ROCKOLA_FIRMA_CLAVE")
                keyPassword = storePassword
                keyAlias = propiedad("rockola.firma.alias", "ROCKOLA_FIRMA_ALIAS") ?: "rockola"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        // android.util.Log en las pruebas de la JVM (ClienteApi registra respuestas fuera del contrato).
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}