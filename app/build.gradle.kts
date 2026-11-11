import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.jaggedgem"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "2.4.0"

        val envFile = rootProject.file(".env")
        val envProps = Properties().apply {
            if (envFile.exists()) envFile.inputStream().use { load(it) }
        }
        buildConfigField("String", "MISTRAL_API_KEY", "\"${envProps.getProperty("MISTRAL_API_KEY", "")}\"")
        buildConfigField("String", "CHAT_MODEL", "\"${envProps.getProperty("CHAT_MODEL", "mistral-small-latest")}\"")
        buildConfigField("String", "TTS_VOICE", "\"${envProps.getProperty("TTS_VOICE", "gb_jane_neutral")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
