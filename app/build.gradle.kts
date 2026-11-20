import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Clave de firma de release: fuera del repositorio (ver PLAN.md §0). Sin ella, la release no se firma.
val firma = Properties().apply {
    val archivo = file(System.getenv("KCALMA_FIRMA") ?: "C:/Users/Mikel/.kcalma/keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mikel.kcalma"
    compileSdk = 36

    defaultConfig {
        // No cambiar nunca: los amigos perderían sus datos al actualizar.
        applicationId = "com.mikel.kcalma"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "0.4.1"
    }

    signingConfigs {
        if (firma.getProperty("storeFile") != null) create("release") {
            storeFile = file(firma.getProperty("storeFile"))
            storePassword = firma.getProperty("storePassword")
            keyAlias = firma.getProperty("keyAlias")
            keyPassword = firma.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    // Escáner: CameraX + ML Kit incluido en la APK (funciona sin Google Play Services ni conexión)
    val camerax = "1.4.2"
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
