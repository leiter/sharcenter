import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}


android {
    namespace = "cut.the.crap"
    compileSdk = 37

    defaultConfig {
        applicationId = "cut.the.crap.sharecare"
        minSdk = 26
        targetSdk = 37
        versionCode = 7
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val signingPropsFile = File("/home/mandroid/Videos/AA_FILES/sharecare_signature_prop")
    val signingProps = Properties()
    if (signingPropsFile.exists()) {
        signingProps.load(FileInputStream(signingPropsFile))
    }

    signingConfigs {
        create("release") {
            storeFile = signingProps["storeFile"]?.let { signingPropsFile.parentFile.resolve(it as String) }
            storePassword = signingProps["storePassword"] as String?
            keyAlias = signingProps["keyAlias"] as String?
            keyPassword = signingProps["keyPassword"] as String?
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Debug API URL - typically points to local development server
            // Example: "http://192.168.1.100:8080" or "http://10.0.2.2:8080" for Android emulator
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.100:8080\"")
            // Action campaign site (a different host from the job-queue backend above).
            // Point this at the local Flask server while working on the campaign.
            buildConfigField("String", "CAMPAIGN_BASE_URL", "\"https://cutthecrap.link\"")
        }
        release {

            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            // Production API URL - points to production server
            // TODO: Update this to your production server URL when deploying
            // Example: "https://api.yourapp.com"
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.100:8080\"")
            buildConfigField("String", "CAMPAIGN_BASE_URL", "\"https://cutthecrap.link\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Non-minified variant used only to run instrumented (androidTest) tests.
        // The `debug` type deliberately minifies/obfuscates, which strips Kotlin
        // stdlib the AndroidX test runner needs; testing against this variant keeps
        // that production-like obfuscation intact while letting tests actually run.
        create("instrumentation") {
            initWith(getByName("debug"))
            isMinifyEnabled = false
            isShrinkResources = false
            // Dependencies (e.g. SQLDelight's AAR) only publish debug/release variants,
            // so resolve this custom build type against their debug variant.
            matchingFallbacks += "debug"
        }
    }
    // Run instrumented tests against the non-minified `instrumentation` build type.
    testBuildType = "instrumentation"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Bouncy Castle (the identity layer's Ed25519) and jspecify both ship an OSGi
            // manifest under the same multi-release path, which the merger will not resolve on
            // its own. None of it is used at runtime on Android.
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    lint {
        // Use custom lint configuration to ignore markdown files
        lintConfig = file("lint.xml")
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // Compose Multiplatform (replaces the AndroidX compose-bom). On Android these
    // resolve to the same androidx.compose artifacts, so behaviour is unchanged.
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.uiTooling)
    // String/drawable catalogue; the `Res` class itself is generated in :shared.
    implementation(compose.components.resources)
    // Compose Multiplatform navigation — same androidx.navigation package names,
    // so the call sites are unchanged.
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // Multiplatform core: SQLDelight persistence + domain models/repositories
    implementation(project(":shared"))

    implementation(libs.compose.icons.extended)

    // DataStore for settings persistence
    implementation(libs.datastore.preferences)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp) // or ktor-client-cio for other engines
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coil 3 for image loading (thumbnails). Coil 3 ships no network fetcher, so the
    // Ktor 2 one is added explicitly — it reuses the HTTP stack the app already has.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor2)

    testImplementation(libs.junit)
    // Coroutines testing
    testImplementation(libs.kotlinx.coroutines.test)
    // MockK for Kotlin mocking
    testImplementation(libs.mockk)
    // Turbine for Flow testing
    testImplementation(libs.turbine)
    // Architecture components testing
    testImplementation(libs.androidx.arch.core.testing)
    // Truth assertions for readability
    testImplementation(libs.truth)
    // Koin dependency-graph verification
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
