pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Eschaton"

listOf("core", "kinematics", "server", "paper").forEach {
    include(":$it")
    project(":$it").projectDir = file("src/$it")
}