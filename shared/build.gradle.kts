plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
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
            implementation(libs.kotlinx.coroutines.core)
            // `api` so :app can still reference SqlDriver / the generated database types.
            api(libs.sqldelight.runtime)
            api(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.primitive.adapters)
        }
        androidMain.dependencies {
            api(libs.sqldelight.android.driver)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
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
