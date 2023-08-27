import java.io.FileInputStream
import java.util.Properties

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinKsp)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinKapt)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.jgit)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

val repo = jgit.repo()
val commitCount = (repo?.commitCount("refs/remotes/origin/master") ?: 1) + 23
val latestTag = repo?.latestTag?.removePrefix("v") ?: "2.2.0-SNAPSHOT"

val verCode by extra(commitCount)
val verName by extra(latestTag)
val androidTargetSdkVersion by extra(34)
val androidMinSdkVersion by extra(26)

android {
    namespace = "cn.xihan.sign"
    compileSdk = androidTargetSdkVersion

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
        applicationId = "cn.xihan.sign"
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = verCode
        versionName = verName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations.addAll(listOf("zh"))

        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")

        signingConfig = signingConfigs.getByName("xihantest")

        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions.jvmTarget = "17"

    buildFeatures{
        compose = true
        buildConfig = true
    }

    composeOptions.kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()

    packagingOptions.apply {
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

    lint.abortOnError = false
}

dependencies {
    implementation(libs.multidex)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.activity.ktx)
    implementation(libs.kotlin.json)
    implementation(libs.landscapist.coil) {
        exclude(group = "io.coil-kt")
    }
    implementation(libs.xxpermissions) {
        exclude(group = "com.android.support")
    }
    implementation(libs.io.coil.compose)

    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.common.java8)
    implementation(libs.work.runtime)
    implementation(libs.work.runtime.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.room.paging)
    implementation(libs.paging.runtime)
    implementation(libs.paging.runtime.ktx)
    implementation(libs.paging.compose)
    implementation(libs.startup)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.orbit.core)
    implementation(libs.orbit.compose)
    implementation(libs.orbit.viewmodel)

    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)
    kapt(libs.hiltx.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling)
    implementation(libs.foundation)
    implementation(libs.runtime)
    implementation(libs.animation)
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.navigation.compose)
    implementation(libs.activity.compose)
    implementation(libs.com.google.android.material)

    implementation(libs.yukihook.api)
    ksp(libs.yukihook.ksp)

    compileOnly(libs.xposed.api)
}

val service = project.extensions.getByType<JavaToolchainService>()
val customLauncher = service.launcherFor {
    languageVersion.set(JavaLanguageVersion.of("17"))
}
project.tasks.withType<org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain>().configureEach {
    kotlinJavaToolchain.toolchain.use(customLauncher)
}

kotlin {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
    }
//    sourceSets.all {
//        languageSettings.apply {
//            languageVersion = "2.0"
//        }
//    }
}
