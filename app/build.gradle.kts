import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.lsplugin.jgit)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.lsplugin.resopt)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

val repo = jgit.repo()
val commitCount = (repo?.commitCount("refs/remotes/origin/master") ?: 200) + 2025
val latestTag = repo?.latestTag?.removePrefix("v") ?: "3.x.x-SNAPSHOT"

val verCode by extra(commitCount)
val verName by extra(latestTag)
println("verCode: $verCode, verName: $verName")
val androidTargetSdkVersion by extra(36)
val androidMinSdkVersion by extra(26)

android {
    namespace = "cn.xihan.signhook"
    compileSdk = androidTargetSdkVersion

    androidResources.additionalParameters += arrayOf(
        "--allow-reserved-package-id", "--package-id", "0x23"
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

    buildFeatures {
        buildConfig = true
        compose = true
        prefab = true
    }

    defaultConfig {
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = verCode
        versionName = verName

        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")

        signingConfig = signingConfigs.getByName("xihantest")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packagingOptions.apply {
        resources.excludes += mutableSetOf(
            "META-INF/**", "**/*.properties", "schema/**", "**.bin", "kotlin-tooling-metadata.json"
        )
        dex.useLegacyPackaging = true
    }

    lint.checkReleaseBuilds = false

    dependenciesInfo.includeInApk = false
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs = listOf(
            "-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions"
        )
    }
}

dependencies {

    implementation(libs.android.material)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.core.ktx)
    implementation(libs.fast.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.core.coroutines)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktorfit.lib)
    implementation(libs.lifecycle.common.java8)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.multidex)
    implementation(libs.orbit.core)
    implementation(libs.orbit.compose)
    implementation(libs.orbit.viewmodel)
    implementation(libs.paging.compose)
    implementation(libs.paging.runtime)
    implementation(libs.paging.runtime.ktx)

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))


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
    tasks.getByPath("installDebug").finalizedBy(restartQQ)
}