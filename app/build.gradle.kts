import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jgit)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

val repo = jgit.repo()
val commitCount = (repo?.commitCount("refs/remotes/origin/master") ?: 1) + 4000
val latestTag = repo?.latestTag?.removePrefix("v") ?: "4.0.0-SNAPSHOT"

val verCode by extra(commitCount)
val verName by extra(latestTag)
val androidTargetSdkVersion by extra(36)
val androidMinSdkVersion by extra(28)

android {
    namespace = "website.xihan.signhelper"
    compileSdk {
        version = release(androidTargetSdkVersion)
    }

    androidResources.additionalParameters += arrayOf(
        "--allow-reserved-package-id",
        "--package-id",
        "0x64"
    )

    signingConfigs {
        create("xihantest") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    defaultConfig {
        minSdk = androidMinSdkVersion
        versionCode = verCode
        versionName = verName

        androidResources.localeFilters += listOf("zh")

        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")

        ndk {
            abiFilters += setOf("arm64-v8a", "armeabi-v7a")
        }

        signingConfig = signingConfigs.getByName("xihantest")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            isPseudoLocalesEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )

            applicationVariants.all {
                outputs.all {
                    this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                    if (buildType.name != "debug" && outputFileName.endsWith(".apk")) {
                        val apkName = "SignHook-release_${verName}_$verCode.apk"
                        outputFileName = apkName
                    }
                }
                tasks.configureEach {
                    var maybeNeedCopy = false
                    if (name.startsWith("assembleRelease")) {
                        maybeNeedCopy = true
                    }
                    if (maybeNeedCopy) {
                        doLast {
                            this@all.outputs.all {
                                this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                                if (buildType.name != "debug" && outputFileName.endsWith(".apk")) {
                                    if (outputFile != null && outputFileName.endsWith(".apk")) {
                                        val targetDir =
                                            rootProject.file("归档/v${verName}-${verCode}")
                                        val targetDir2 = rootProject.file("release")
                                        targetDir.mkdirs()
                                        targetDir2.mkdirs()
                                        println("path: ${outputFile.absolutePath}")
                                        copy {
                                            from(outputFile)
                                            into(targetDir)
                                        }
                                        copy {
                                            from(outputFile)
                                            into(targetDir2)
                                        }
                                        copy {
                                            from(rootProject.file("app/build/outputs/mapping/release/mapping.txt"))
                                            into(targetDir)
                                        }
                                    }
                                }
                            }

                        }
                    }

                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packagingOptions.apply {
        excludes += setOf("lib/**/libandroidx.graphics.path.so")
        resources.excludes += mutableSetOf(
            "META-INF/*******",
            "**/*.txt",
            "**/*.xml",
            "**/*.properties",
            "DebugProbesKt.bin",
            "java-tooling-metadata.json",
            "kotlin-tooling-metadata.json"
        )
        dex.useLegacyPackaging = true
    }

    applicationVariants.all {
        addJavaSourceFoldersToModel(file("build/generated/ksp/$name/kotlin"))
    }

    lint.abortOnError = false
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.android.material)
    implementation(libs.appcompat)
    implementation(libs.compose.activity)
    implementation(libs.compose.coil) {
        exclude(group = "io.coil-kt")
    }
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(platform(libs.compose.bom))
    implementation(libs.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose.navigation)
    implementation(libs.koin.core.coroutines)
    implementation(platform(libs.koin.bom))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kv.storage)
    implementation(libs.lifecycle.common.java8)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.multidex)
    implementation(libs.orbit.compose)
    implementation(libs.orbit.core)
    implementation(libs.orbit.viewmodel)
    implementation(libs.paging.compose)
    implementation(libs.paging.runtime)
    implementation(libs.paging.runtime.ktx)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.startup)
    compileOnly(libs.xposed.api)

}

// 定义停止 QQ 的任务
val stopQQ = tasks.register<Exec>("stopQQ") {
    commandLine("cmd", "/c", "adb", "shell", "am", "force-stop", "com.tencent.mobileqq")
}

// 定义启动 QQ 的任务
val startQQ = tasks.register<Exec>("startQQ") {
    // 注意：原命令中使用了 shell 的 $()，这需要通过 shell 解释器执行
    // 因为 Exec 任务默认不经过 shell，所以需显式调用 sh -c
    commandLine(
//        "sh", "-c",
        "cmd",
        "/c",
        "adb shell am start \"$(pm resolve-activity --components com.tencent.mobileqq)\""
    )
}

// 主任务依赖上述两个任务
val restartQQ = tasks.register("restartQQ") {
    dependsOn(stopQQ, startQQ)
}

// 停止微信
val stopWx = tasks.register<Exec>("stopWx") {
    commandLine("cmd", "/c", "adb", "shell", "am", "force-stop", "com.tencent.mm")
}

// 启动微信（注意：需通过 shell 解析 $()）
val startWx = tasks.register<Exec>("startWx") {
    commandLine(
//        "sh", "-c",
        "cmd", "/c", "adb shell am start \"$(pm resolve-activity --components com.tencent.mm)\""
    )
}

// 重启微信的主任务
val restartWx = tasks.register("restartWx") {
    dependsOn(stopWx, startWx)
}

afterEvaluate {
//    tasks.getByPath("installDebug").finalizedBy(restartQQ)
}