plugins {
    id("java")
}

group = "io.gitlab.icestom.eschaton"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":src:kinematics"))
}