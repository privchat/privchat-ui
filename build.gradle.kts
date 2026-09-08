plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization") version "2.1.21"
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

            // BOT_INTERACTION_SPEC §4：菜单 action 解析与 metadata 序列化用 kotlinx.serialization。
            // sdk 那边是 implementation 依赖，不会传递；此处显式声明保证 Android target 也能解析。
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        // Android-specific dependencies
        androidMain.dependencies {
            implementation("androidx.annotation:annotation:1.9.1")
            implementation("androidx.core:core-ktx:1.13.1")
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

/**
 * 硬编码中文护栏。
 *
 * 「界面文案一律走语言包」这条规矩以前只写在文档里，没有任何东西在执行它，于是
 * 整个创建群聊页在英文界面下还是全中文。这个 task 把规矩变成会失败的构建。
 *
 * 判定：commonMain 的 .kt 里出现带中文的字符串字面量即失败。豁免只有两类，
 * 都必须在同一行用行尾注释标注原因：
 *   - `// i18n-exempt: <原因>` —— 匹配底层错误文本这类**非展示**用途
 *   - i18n 目录自身（语言包就是中文的所在）
 */
val checkNoHardcodedChinese by tasks.registering {
    group = "verification"
    description = "Fails if commonMain has Chinese string literals outside the language packs"
    val sources = fileTree("src/commonMain/kotlin") { include("**/*.kt") }
    inputs.files(sources)
    // 无产物：声明一个 marker 让 Gradle 能做增量。
    val marker = layout.buildDirectory.file("i18n-guard.ok")
    outputs.file(marker)
    doLast {
        val chinese = Regex("\"[^\"]*[\\u4e00-\\u9fff][^\"]*\"")
        val comment = Regex("^\\s*(//|\\*|/\\*)")
        val logCall = Regex("\\b(println|print|Log\\.[dviwe]|logD|logI|logW|logE|logV)\\s*\\(")
        val offenders = mutableListOf<String>()
        sources.forEach { file ->
            if (file.path.contains("/i18n/")) return@forEach
            file.readLines().forEachIndexed { idx, line ->
                if (comment.containsMatchIn(line)) return@forEachIndexed
                if (line.contains("i18n-exempt:")) return@forEachIndexed
                // 日志不是界面。开发者读的诊断信息用中文写没问题，也不该逼进语言包。
                if (logCall.containsMatchIn(line)) return@forEachIndexed
                if (chinese.containsMatchIn(line)) {
                    offenders += "${file.relativeTo(projectDir)}:${idx + 1}: ${line.trim()}"
                }
            }
        }
        marker.get().asFile.also { it.parentFile.mkdirs() }.writeText("checked ${sources.files.size} files\n")
        if (offenders.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Hardcoded Chinese found in ${offenders.size} place(s).")
                    appendLine("Move the text into PrivChatDomainStrings.kt + all four packs, or, if this")
                    appendLine("string matches underlying error text rather than being shown, mark the line")
                    appendLine("with `// i18n-exempt: <why>`.")
                    offenders.forEach { appendLine("  $it") }
                }
            )
        }
    }
}

tasks.named("check") { dependsOn(checkNoHardcodedChinese) }
