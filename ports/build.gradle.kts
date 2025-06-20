/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

// disable default versioning
version = ""

// custom versioning flag
val VERSION = "0.0.13"

// base of output jar name
val OUTPUT_JAR_NAME = "nodes-ports"

// target will be set to minecraft version by cli input parameter
var target = ""


plugins {
    // paperweight for nms
    id("io.papermc.paperweight.userdev") version "1.3.8"
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    // maven() // no longer needed in gradle 7
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()

    // paper
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public")
    }
    maven {
        url = uri("https://ci.ender.zone/plugin/repository/everything")
    }
}


configurations {
    create("resolvableImplementation") {
        isCanBeResolved = true
        isCanBeConsumed = true
    }
}

val instrumentedClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    // Align versions of all Kotlin components
    compileOnly(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // google json
    // compileOnly("com.google.code.gson:gson:2.8.6")

    // nodes (local repo)
    implementation(project(":nodes"))

    // put spigot/paper on path otherwise kotlin vs code plugin language server gets mad
    api("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    if ( project.hasProperty("1.16") == true ) {
        target = "1.16.5"
        // java must be up to 16 for 1.16
        java.toolchain.languageVersion.set(JavaLanguageVersion.of(16))
        // spigot/paper api
        compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    } else if ( project.hasProperty("1.18") == true ) {
        target = "1.18.2"
        // java must be up to 17 for 1.18
        java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        // nms
        paperDevBundle("1.18.2-R0.1-SNAPSHOT")
        // spigot/paper api
        compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")

        tasks {
            assemble {
                // must write it like below because in 1.16 config, reobfJar does not exist
                // so the simpler definition below wont compile
                // dependsOn(reobfJar) // won't compile :^(
                dependsOn(project.tasks.first { it.name.contains("reobfJar") })
            }
        }

        tasks.named("reobfJar") {
            base.archivesBaseName = "${OUTPUT_JAR_NAME}-${target}-${VERSION}"
        }
    }
}

tasks {
    named<ShadowJar>("shadowJar") {
        // verify valid target minecraft version
        doFirst {
            val supportedMinecraftVersions = setOf("1.16.5", "1.18.2")
            if ( !supportedMinecraftVersions.contains(target) ) {
                throw Exception("Invalid Minecraft version! Supported versions are: 1.16, 1.18")
            }
        }

        classifier = ""
        configurations = mutableListOf(project.configurations.named("resolvableImplementation").get()) as List<FileCollection>
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    
    test {
        testLogging.showStandardStreams = true
    }
}

gradle.taskGraph.whenReady {
    tasks {
        named<ShadowJar>("shadowJar") {
            // baseName = "${OUTPUT_JAR_NAME}-${target}"
            baseName = "${OUTPUT_JAR_NAME}-${target}-${VERSION}"
            minimize() // FOR PRODUCTION USE MINIMIZE
        }
    }
}
