plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    `maven-publish`
}

group = "gg.kpjm"
version = "0.1.12"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("de.tr7zw:item-nbt-api:2.15.5")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    runServer {
        minecraftVersion("1.21")
    }

    // NORMAL JAR — required for Maven
    named<Jar>("jar") {
        enabled = true
    }

    // SHADOW JAR — optional, gets classifier
    shadowJar {
        archiveClassifier.set("all")

        relocate(
            "de.tr7zw.changeme.nbtapi",
            "gg.kpjm.persistentBlockData.nbt.api"
        )
        relocate(
            "de.tr7zw.annotations",
            "gg.kpjm.persistentBlockData.nbt.annotations"
        )
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"]) // publishes NORMAL jar

            artifact(tasks.shadowJar) {
                classifier = "all"    // publishes shadow jar
            }
        }
    }
}
