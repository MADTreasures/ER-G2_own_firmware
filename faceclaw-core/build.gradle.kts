import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

// Faceclaw's shared Kotlin core (GPL-3.0), copied unchanged from
// https://github.com/jimrandomh/faceclaw native/kotlin/shared/src. Provenance and the update
// procedure are in UPSTREAM.md. Only the Android target is built; the iOS sources are omitted.
kotlin {
    compilerOptions {
        // The upstream core uses expect/actual classes; silence the Beta notice rather than edit it.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.faceclaw.shared"
        compileSdk = 37
        minSdk = 30
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        withHostTestBuilder {}.configure {}
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
