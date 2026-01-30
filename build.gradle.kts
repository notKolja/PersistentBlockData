plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    `maven-publish`
}

group = "gg.kpjm"
version = "0.1.11"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://repo.codemc.io/repository/maven-public/") {
        name = "codemc-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("de.tr7zw:item-nbt-api:2.15.5")
}

tasks {
    runServer {
        minecraftVersion("1.21")
    }

    shadowJar {
        archiveFileName.set("PersistentBlockData.jar")

        relocate("de.tr7zw.changeme.nbtapi", "gg.kpjm.persistentBlockData.nbt.api")
        relocate("de.tr7zw.annotations", "gg.kpjm.persistentBlockData.nbt.annotations")
    }

    named<Jar>("jar") {
        enabled = false
    }

    // Disable the default jar task since we only want shadowJar
    named<Jar>("jar") {
        enabled = false
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

// JitPack Publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "gg.kpjm"
            artifactId = "persistentblockdata"
            version = project.version.toString()

            // Only publish the shadowed JAR
            artifact(tasks.shadowJar)
        }
    }
}