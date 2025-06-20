plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.kmpbits.sample.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.kmpbits.sample.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":netflow-core"))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.json.serialization)
    implementation(libs.bundles.koin)
}