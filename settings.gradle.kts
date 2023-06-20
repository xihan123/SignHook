pluginManagement {
    repositories {
//        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
//        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://api.xposed.info/")
        maven("https://s01.oss.sonatype.org/content/repositories/releases")
        maven("https://androidx.dev/storage/compose-compiler/repository/")
    }
}

rootProject.name = "签名助手"
include(":app")
