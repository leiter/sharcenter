import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

/**
 * The desktop (JVM) application — the second target the whole KMP migration was building toward.
 *
 * It is deliberately thin: everything (UI, ViewModels, repositories, persistence) lives in
 * `:shared/commonMain`, and this module only supplies the desktop composition root (a Compose
 * `Window`), the desktop Koin bindings (JDBC driver + the `Desktop*` platform seams), and the
 * `main()` entry point. It is the desktop analogue of `:app`.
 */
kotlin {
    // Bytecode target only (no toolchain provisioning), matching :app and :shared.
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// Keep the (source-less) Java compile task on the same target as Kotlin, so JVM-target
// validation doesn't trip on the running JDK's default.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":shared"))

    // Compose desktop runtime (Skia-backed) plus the same Compose artifacts :shared uses.
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.components.resources)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Supplies Dispatchers.Main on the JVM (the Swing EDT). Without it every ViewModel that
    // touches the main dispatcher fails to construct — Android gets this from
    // kotlinx-coroutines-android, desktop needs the Swing artifact explicitly.
    implementation(libs.kotlinx.coroutines.swing)

    // Koin: core to start the graph, koin-compose for KoinContext/koinInject, and the
    // viewmodel bridge for koinViewModel() in the composition.
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    // Desktop persistence + HTTP engine actuals for the seams declared in :shared.
    implementation(libs.sqldelight.sqlite.driver)
    implementation(libs.ktor.client.okhttp)

    // Thumbnails: same Coil stack as Android, wired to a Ktor fetcher in main().
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor2)
}

compose.desktop {
    application {
        mainClass = "cut.the.crap.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ShareCenter"
            packageVersion = "1.0.0"
        }
    }
}
