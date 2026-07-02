import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "cut.the.crap"
    compileSdk = 36

    defaultConfig {
        applicationId = "cut.the.crap.sharecare"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
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
            // Debug API URL - typically points to local development server
            // Example: "http://192.168.1.100:8080" or "http://10.0.2.2:8080" for Android emulator
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.100:8080\"")
        }
        release {

            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            // Production API URL - points to production server
            // TODO: Update this to your production server URL when deploying
            // Example: "https://api.yourapp.com"
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.100:8080\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
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
    composeOptions {
//        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // Use custom lint configuration to ignore markdown files
        lintConfig = file("lint.xml")
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    val hiltVersion = "2.56.2"
    implementation ("androidx.hilt:hilt-navigation-compose:1.3.0")
    kapt ("androidx.hilt:hilt-compiler:1.3.0")
    kapt ("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation ("com.google.dagger:hilt-android:$hiltVersion")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")

    val roomVersion = "2.8.4" // Check for the latest version
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt ("androidx.room:room-compiler:$roomVersion" )
    // Optional: Room Kotlin Extensions and Coroutines support
    implementation( "androidx.room:room-ktx:$roomVersion")

    implementation("androidx.compose.material:material-icons-extended")

    // DataStore for settings persistence
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("io.ktor:ktor-client-core:2.3.5")
    implementation("io.ktor:ktor-client-okhttp:2.3.5") // or ktor-client-cio for other engines
    implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
    implementation("io.ktor:ktor-client-logging:2.3.5")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Coil for image loading (thumbnails)
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation("junit:junit:4.13.2")
    // Coroutines testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    // MockK for Kotlin mocking
    testImplementation("io.mockk:mockk:1.13.9")
    // Turbine for Flow testing
    testImplementation("app.cash.turbine:turbine:1.0.0")
    // Architecture components testing
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    // Truth assertions for readability
    testImplementation("com.google.truth:truth:1.1.5")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
