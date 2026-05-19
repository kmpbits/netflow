import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.maven.publish)
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
            baseName = "NetflowPaging"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":netflow-core"))
            api(libs.androidx.paging.common)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {

        }
    }
}

val artifactId = "netflow-paging"
val groupGitHubId = "io.github.kmpbits"
val libraryVersion = "0.0.18-alpha"

group = groupGitHubId
version = libraryVersion

android {
    namespace = "com.kmpbits.netflow_paging"
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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(groupGitHubId, artifactId, libraryVersion)

    pom {
        name = "NetFlow Paging KMP"
        description = "NetFlow Paging Extension"
        inceptionYear = "2024"
        url = "https://github.com/kmpbits/netflow"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "kmpbits"
                name = "KMP Bits"
                url = "https://github.com/kmpbits/"
            }
        }
        scm {
            url = "https://github.com/kmpbits/netflow/"
            connection = "scm:git:git://github.com/kmpbits/netflow.git"
            developerConnection = "scm:git:ssh://git@github.com/kmpbits/netflow.git"
        }
    }
}
