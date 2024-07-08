import java.io.FileInputStream
import java.util.Properties

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinKsp)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose.compiler)
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

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }

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
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(platform(libs.compose.bom))
    implementation(libs.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose.navigation)
    implementation(libs.koin.core.coroutines)
    implementation(platform(libs.koin.bom))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.landscapist.coil) {
        exclude(group = "io.coil-kt")
    }
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
    implementation(libs.yukihook.api)
    ksp(libs.yukihook.ksp)
    compileOnly(libs.xposed.api)
}


