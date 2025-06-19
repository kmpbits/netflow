import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
    id("maven-publish")
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "netflow-core"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(libs.kotlinx.coroutines)
            implementation(libs.json.serialization)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.android.okhttp)
        }
    }
}

val artifactCoreId = "neflow-core"
val groupGitHubId = "com.github.kmpbits.libraries"
val libraryVersion = "0.0.1"

group = groupGitHubId
version = libraryVersion

android {
    namespace = "com.kmpbits.netflow_core"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            groupId = groupGitHubId
            artifactId = artifactCoreId
            version = libraryVersion

            pom {
                name.set("KMP Bits NetFlow")
                description.set("KMP Library published via JitPack")
            }
        }
    }
}
