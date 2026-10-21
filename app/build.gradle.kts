plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// GitHub Actions supplies these values; local builds stay explicitly marked as local.
val tuddySourceSha = System.getenv("GITHUB_SHA") ?: "local"
val tuddyBuildRunId = System.getenv("GITHUB_RUN_ID") ?: "local"

android {
    namespace = "com.livelingo.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.livelingo.android"
        minSdk = 31
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"
        buildConfigField("String", "TUDDY_SOURCE_SHA", "\"$tuddySourceSha\"")
        buildConfigField("String", "TUDDY_BUILD_RUN_ID", "\"$tuddyBuildRunId\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(bom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:language-id:17.0.6")
    testImplementation("junit:junit:4.13.2")
}
