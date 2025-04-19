plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.imapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.imapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        // 用与你的 BOM 匹配的 compiler 版本
        kotlinCompilerExtensionVersion = "1.5.5"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // --- 使用单一 BOM，确保所有 Compose 库版本一致 ---
    implementation(platform(libs.androidx.compose.bom))

    // AndroidX 基础库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose UI
    implementation(libs.androidx.ui)                 // ui
    implementation(libs.androidx.ui.graphics)        // ui-graphics
    implementation(libs.androidx.ui.tooling.preview) // ui-tooling-preview

    // Foundation（含 LazyColumn + reorderable API）
    implementation(libs.androidx.foundation)

    // Material 3

    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Media3 ExoPlayer（播放相关）
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // 其他第三方
    implementation(libs.okhttp.v4110)
    implementation(libs.gson)

    implementation(libs.androidx.material3)
    implementation(libs.ui)
    implementation(libs.androidx.foundation.v154)



    // --- 测试相关 ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
