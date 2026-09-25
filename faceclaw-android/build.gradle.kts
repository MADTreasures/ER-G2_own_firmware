import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

// Faceclaw's Android GATT adapters (GPL-3.0), copied unchanged from
// App_Resources/Android/src/main/java/com/faceclaw/app/. See UPSTREAM.md.
android {
    namespace = "com.faceclaw.android"
    compileSdk = 37

    defaultConfig {
        // FaceclawBleManager uses the API 33 writeCharacteristic overload (Wear OS 4 and newer).
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":faceclaw-core"))
}
