pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Eschaton"

include(":src:core")
include(":src:kinematics")
include(":src:server")
include(":src:paper")