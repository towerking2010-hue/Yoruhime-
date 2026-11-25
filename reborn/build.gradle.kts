plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.yoruhime.reborn"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.yoruhime.reborn"
        minSdk = 28 // MediaPipe Tasks Audio requires Android 9+; target Galaxy A31 is Android 12.
        targetSdk = 35
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        versionCode = 1
        versionName = "1.0.0-reborn"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.filament:filament-android:1.75.0")
    implementation("com.google.android.filament:gltfio-android:1.75.0")
    implementation("com.google.android.filament:filament-utils-android:1.75.0")
    // Optional Kokoro-82M on-device neural TTS; model is downloaded only on user request.
    implementation("com.google.mediapipe:tasks-audio:1.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
