pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }

    plugins {
        kotlin("multiplatform") version "2.1.21"
        kotlin("plugin.compose") version "2.1.21"
        id("com.android.library") version "8.7.2"
        id("org.jetbrains.compose") version "1.7.3"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

rootProject.name = "privchat-ui"

// GearUI-Kit
includeBuild("../gearui-kit") {
    dependencySubstitution {
        substitute(module("com.gearui:gearui-kit")).using(project(":gearui-kit"))
    }
}

// PrivChat SDK Kotlin
includeBuild("../privchat-sdk-kotlin") {
    dependencySubstitution {
        substitute(module("com.netonstream.privchat:sdk")).using(project(":sdk"))
    }
}
