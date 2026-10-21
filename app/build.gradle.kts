import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/** Lee un secreto de las variables de entorno (CI) o de local.properties. */
fun secret(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: localProperties.getProperty(name, "")

val ciVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1

android {
    namespace = "com.rippipupil.clothify"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rippipupil.clothify"
        minSdk = 26
        targetSdk = 36
        versionCode = ciVersionCode
        versionName = "1.0.$ciVersionCode"

        buildConfigField("String", "GEMINI_API_KEY", "\"${secret("GEMINI_API_KEY")}\"")
        buildConfigField("String", "SERPAPI_KEY", "\"${secret("SERPAPI_KEY")}\"")
        buildConfigField("String", "ALIEXPRESS_APP_KEY", "\"${secret("ALIEXPRESS_APP_KEY")}\"")
        buildConfigField("String", "ALIEXPRESS_APP_SECRET", "\"${secret("ALIEXPRESS_APP_SECRET")}\"")
        buildConfigField("String", "ALIEXPRESS_TRACKING_ID", "\"${secret("ALIEXPRESS_TRACKING_ID")}\"")
        buildConfigField("String", "GEMINI_MODEL", "\"gemini-flash-latest\"")
    }

    signingConfigs {
        val keystoreFile = secret("KEYSTORE_FILE")
        if (keystoreFile.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = secret("KEYSTORE_PASSWORD")
                keyAlias = secret("KEY_ALIAS")
                keyPassword = secret("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        // Esquemas exportados de Room para probar las migraciones (Robolectric lee los assets de debug;
        // no llegan al APK release).
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }

    androidResources {
        noCompress += "onnx"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.maxHeapSize = "2g" }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime)
    implementation(libs.onnxruntime.android)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.room.testing)
    testImplementation(libs.work.testing)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}
