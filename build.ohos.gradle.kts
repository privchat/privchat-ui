// privchat-ui for HarmonyOS.
//
// Same sources as the normal build, compiled by privchat-app's parallel ohos build
// (Kotlin 2.0.21-KBA; see privchat-app/settings.ohos.gradle.kts). Every expect needs
// an ohosArm64 actual under src/ohosArm64Main, or only this build breaks.
plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
}

group = "com.netonstream.privchat"

kotlin {
    ohosArm64()

    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    // Same flag gearui-kit passes: the Compose compiler does not know
                    // the 2.0.21-KBA version string.
                    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true",
                )
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":gearui-kit"))
            api(project(":sdk"))
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1-KBA-003")
        }
    }
}
