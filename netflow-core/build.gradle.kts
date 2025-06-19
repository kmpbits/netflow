import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
    id("maven-publish")
}

kotlin {
    jvmToolchain(17)

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
            api(libs.kotlinx.coroutines)
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
val libraryVersion = "0.0.8"

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
                name.set("NetFlow KMP")
                description.set("Network API library for Kotlin Multiplatform")
                inceptionYear.set("2025")
                url.set("https://github.com/kmpbits/netflow")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                // Specify developers information
                developers {
                    developer {
                        id.set("kmpbits")
                        name.set("KMP Bits")
                        email.set("kmpbits@gmail.com")
                    }
                }

                scm {
                    url.set("https://github.com/kmpbits/netflow")
                }
            }
        }
    }
}
