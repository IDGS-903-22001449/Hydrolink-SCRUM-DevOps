plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize") // Added for Parcelable support
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" // Para kotlinx.serialization
}

android {
    namespace = "com.plantmonitor.app"
    compileSdk = 35 // Kept your current compileSdk

    defaultConfig {
        applicationId = "com.plantmonitor.app"
        minSdk = 24
        targetSdk = 35 // Kept your current targetSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true // Added for vector drawable support
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // Kept your current Java version
        targetCompatibility = JavaVersion.VERSION_11 // Kept your Java version
    }
    kotlinOptions {
        jvmTarget = "11" // Kept your current JVM target
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // Keeping this commented out as you are using BOM for compose,
        // but if you encounter issues with compose compiler,
        // you might need to specify the version here
        // kotlinCompilerExtensionVersion = "1.5.4"
    }
    packagingOptions {
        resources {
            // Se añadió 'META-INF/INDEX.LIST' y 'META-INF/io.netty.versions.properties'
            // para resolver el error de archivos duplicados.
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/io.netty.versions.properties", // Nueva exclusión
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // MQTT Client (si lo necesitas para otras pantallas)
    implementation("com.hivemq:hivemq-mqtt-client:1.3.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.6.7")

    // **NUEVAS DEPENDENCIAS NECESARIAS PARA TU SEARCHSCREEN:**

    // Ktor Client para hacer llamadas HTTP
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7") // OkHttp engine
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7") // ContentNegotiation
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7") // JSON serialization

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Coil para cargar imágenes (AsyncImage)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}