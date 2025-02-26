@file:Suppress("SpellCheckingInspection")

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "2.1.0"

//    id("ir.mahozad.compose-exe-manifest")
}


group = "com.alutrippers"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {


    implementation("io.github.g0dkar:qrcode-kotlin:4.1.1")

    val ktorVersion = "3.1.0"

    //Client
    dependencies {
        implementation("io.ktor:ktor-client-core:$ktorVersion")
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
        implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    }

    //Server
    dependencies {
        implementation("io.ktor:ktor-server-core:$ktorVersion")
        implementation("io.ktor:ktor-server-netty:$ktorVersion")
        implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
        implementation("io.ktor:ktor-server-websockets:$ktorVersion")
        implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    }
    //Serialization
    dependencies{
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
        implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    }

    implementation("ch.qos.logback:logback-classic:1.5.16")


    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "lightShare"
            packageVersion = "1.0.0"
        }
    }
}
