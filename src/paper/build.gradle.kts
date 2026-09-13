plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.2.0"
}

group = "io.gitlab.icestom.eschaton"
version = "0.0.1"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("com.github.retrooper:packetevents-spigot:2.11.2")

    implementation(project(":src:core"))
    implementation(project(":src:kinematics"))
    implementation("io.github.openboatutils:Protocol:0.0.7")

    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

tasks {
    runServer {
        minecraftVersion("1.21.11")

        downloadPlugins {
            modrinth("packetevents", "2.13.0+spigot")
        }
    }

    processResources {
        val props = mapOf("version" to version)

        inputs.properties(props)
        filteringCharset = "UTF-8"

        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveVersion.set("")
        archiveClassifier.set("")
    }

    shadowJar {
        archiveVersion.set("")
        archiveClassifier.set("")

        dependencies {
            exclude(dependency("io.papermc.paper:paper-api"))
        }
    }

    build {
        dependsOn(shadowJar)
    }

    reobfJar {
        dependsOn(shadowJar)
        inputJar.set(shadowJar.flatMap { it.archiveFile })
    }
}