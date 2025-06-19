import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.maven.publish)
}

val isCiBuild = System.getenv("CI") != null

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

    // ✅ Build iOS targets only when NOT on JitPack
    if (!isCiBuild) {
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
val libraryVersion = "0.0.3"

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

mavenPublishing {
    // Define coordinates for the published artifact
    coordinates(
        groupId = groupGitHubId,
        artifactId = artifactCoreId,
        version = libraryVersion
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("NetFlow KMP")
        description.set("Network API library for Kotlin Multiplatform")
        inceptionYear.set("2024")
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
