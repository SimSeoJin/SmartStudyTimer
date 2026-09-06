import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 카카오 네이티브 앱 키: local.properties(git에 커밋되지 않음)의 KAKAO_NATIVE_APP_KEY를 읽음.
// 없으면 플레이스홀더를 쓰므로, 실제 카카오 로그인을 테스트하려면 Kakao Developers에서
// 발급받은 네이티브 앱 키를 local.properties에 추가해야 함.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val kakaoNativeAppKey: String =
    (localProperties.getProperty("KAKAO_NATIVE_APP_KEY") ?: "REPLACE_WITH_KAKAO_NATIVE_APP_KEY")

// 백엔드 서버 주소: local.properties의 API_BASE_URL을 읽음. 에뮬레이터는 10.0.2.2가 호스트 PC를
// 가리키지만, 실기기는 PC의 실제 LAN IP(같은 Wi-Fi여야 함)를 넣어야 함.
val apiBaseUrl: String =
    (localProperties.getProperty("API_BASE_URL") ?: "http://10.0.2.2:8080/")

android {
    namespace = "com.sm.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sm.myapplication"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        ndk {
            abiFilters += listOf(
                "armeabi-v7a",
                "arm64-v8a",
                "x86"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // 기본 Android / Kotlin
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking (backend 연동)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 카카오 로그인
    implementation("com.kakao.sdk:v2-user:2.25.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ML Kit - 얼굴 감지
    implementation("com.google.mlkit:face-detection:16.1.7")

    // MediaPipe - 손 제스처 인식
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // Guava
    implementation("com.google.guava:guava:33.2.1-android")
}