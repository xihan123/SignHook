buildscript {
    val appVersionName by extra("1.0.2")
    val appVersionCode by extra(102)
}
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinKsp) apply false
    alias(libs.plugins.kotlinParcelize) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinKapt) apply false
    alias(libs.plugins.hiltAndroid) apply false
}
true // Needed to make the Suppress annotation work for the plugins block