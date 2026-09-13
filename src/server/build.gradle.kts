plugins {
    id("java")
    id("com.gradleup.shadow") version("9.4.3")
}

group = "io.gitlab.icestom.eschaton"
version = "0.0.1"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("net.minestom:minestom:2026.01.08-1.21.11")
    implementation("ch.qos.logback:logback-classic:1.5.32")

    implementation("io.github.openboatutils:Protocol:0.0.7")

    implementation(project(":core"))
    implementation(project(":kinematics"))
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "io.gitlab.icestom.eschaton.server.EschatonServer"
            attributes["Enable-Native-Access"] = "ALL-UNNAMED"
        }
        archiveClassifier.set("plain")
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        mergeServiceFiles()
        archiveClassifier.set("")
    }
}
