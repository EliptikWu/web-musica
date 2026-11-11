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

    buildTypes {
        release {
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