version = "0.0.14"

val outputJarName = "nodes"

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.0-RC"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/nexus/repository/public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
    compileOnly("me.clip:placeholderapi:2.11.7")
    compileOnly("com.github.NEZNAMY","TAB-API","5.4.0")
    compileOnly("com.google.code.gson:gson:2.13.2")

    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    named("reobfJar") {
        dependsOn(jar)
    }

    jar {
        archiveBaseName.set(outputJarName)
    }
}
