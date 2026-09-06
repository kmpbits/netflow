import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    alias(libs.plugins.maven.publish)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(projects.netflowAnnotations)
}

val artifactId = "netflow-ksp"
val groupGitHubId = "io.github.kmpbits"
val neflowLibrary: String by project

group = groupGitHubId
version = neflowLibrary

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(groupGitHubId, artifactId, neflowLibrary)

    pom {
        name = "NetFlow KMP KSP Processor"
        description = "KSP processor that generates NetFlow API implementations from annotated interfaces"
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
