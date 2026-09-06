import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
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

    jvm()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "NetflowAnnotations"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {}
    }
}

val artifactId = "netflow-annotations"
val groupGitHubId = "io.github.kmpbits"
val neflowLibrary: String by project

group = groupGitHubId
version = neflowLibrary

android {
    namespace = "com.kmpbits.netflow_annotations"
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
    coordinates(groupGitHubId, artifactId, neflowLibrary)

    pom {
        name = "NetFlow KMP Annotations"
        description = "Annotation set for NetFlow's KSP-generated API interfaces"
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
