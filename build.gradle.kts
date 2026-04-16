plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
}

group = "com.netonstream.privchat"
version = "0.1.0"

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs += listOf(
                    "-Xjvm-default=all",
                    "-opt-in=kotlin.RequiresOptIn"
                )
            }
        }
    }

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Common compiler options
    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-opt-in=kotlin.RequiresOptIn"
                )
            }
        }
    }

    sourceSets {
        // Common dependencies
        commonMain.dependencies {
            // ============================================================================
            // PrivChat-UI 100% 依赖 GearUI-Kit，不直接使用 KuiklyUI
            // ============================================================================

            // GearUI-Kit Component Library
            api("com.gearui:gearui-kit")

            // PrivChat SDK - 直接使用 SDK 数据类型，零转换
            api("com.netonstream.privchat:sdk")
        }

        // Android-specific dependencies
        androidMain.dependencies {
            implementation("androidx.annotation:annotation:1.9.1")
        }

        // iOS-specific dependencies
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "com.netonstream.privchat.ui"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
