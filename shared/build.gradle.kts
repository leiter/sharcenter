plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * The multiplatform core. Currently holds the persistence layer (SQLDelight schema,
 * DAO contracts + implementations) and the domain models/repositories — all in
 * `commonMain`, i.e. free of Android APIs.
 *
 * The `desktop` (JVM) target is not shipped yet; it exists so `commonMain` is proven to
 * compile and run off Android — the SQLDelight migration tests execute against the JDBC
 * driver there, which is the same driver a desktop/macOS app will use.
 *
 * UI, networking and settings still live in `:app` and move here as their Android
 * couplings (R.string, Toast/Intent, java.time/io) get replaced.
 */
kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "11" }
        }
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform. On Android these artifacts delegate to the same
            // androidx.compose libraries, so :app renders identically.
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            // The string/drawable catalogue. `api`, because :app resolves `Res.*` against it.
            api(compose.components.resources)
            // JetBrains stopped publishing the icons artifact after 1.7.3; it resolves
            // cleanly against CMP 1.8.2 (verified). The app uses 87 distinct icons.
            api(libs.compose.icons.extended)
            // BackHandler. CMP ships it as its own artifact for every target, so it replaces
            // androidx.activity.compose.BackHandler outright — no seam needed. `api`, because
            // :app's screens use it directly until WP7 moves them here.
            api(libs.compose.ui.backhandler)
            // CMP navigation. `api` so :app still resolves NavHost/composable against it
            // (NavigationGraph stays in :app until WP7); commonMain needs NavHostController for the
            // app-root handleAction moved here in WP-iOS-1.
            api(libs.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            // WP5 spike: the multiplatform DataStore core. Same Preferences API as the Android
            // artifact, minus the Context-bound delegate — the file path is supplied by the caller.
            api(libs.datastore.preferences.core)
            api(libs.okio)
            api(libs.kotlinx.datetime)
            // WP5 spike: does Ktor expose what UrlResolver needs from OkHttp?
            api(libs.ktor.client.core)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.serialization.kotlinx.json)
            api(libs.kotlinx.serialization.json)
            // `api` so :app can still reference SqlDriver / the generated database types.
            api(libs.sqldelight.runtime)
            api(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.primitive.adapters)
            // WP6. `api` throughout: :app's screens still resolve the ViewModels, the Koin
            // module and koinViewModel() against these until WP7 moves the UI here too.
            api(libs.lifecycle.viewmodel)
            api(libs.koin.core)
            api(libs.koin.core.viewmodel)
            api(libs.koin.compose.viewmodel)
        }
        androidMain.dependencies {
            api(libs.sqldelight.android.driver)
            // Actual HTTP engine for the httpClientEngine() seam.
            implementation(libs.ktor.client.okhttp)
            // The FilePicker actual registers an activity-result contract; that lives here,
            // not in Compose itself.
            implementation(libs.androidx.activity.compose)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
                // Same engine as Android, so redirect semantics UrlResolver relies on match.
                implementation(libs.ktor.client.okhttp)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test"))
                // WP6: the ViewModel and share-handler tests came over from :app with these.
                implementation(libs.truth)
                implementation(libs.turbine)
                implementation(libs.mockk)
            }
        }
    }
}

// The generated `Res` class must be public (it defaults to internal) because :app consumes
// the catalogue across the module boundary.
compose.resources {
    publicResClass = true
    packageOfResClass = "cut.the.crap.shared.resources"
    generateResClass = always
}

android {
    namespace = "cut.the.crap.shared"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("ShareDatabase") {
            packageName.set("cut.the.crap.data.db.sql")
        }
    }
}
