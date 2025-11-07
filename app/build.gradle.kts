plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // "Processador de anotações" (ksp) para funcionar o Room
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.genaro_mian.mysqlmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.genaro_mian.mysqlmanager"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material3)
    implementation (libs.androidx.compose.material.icons.extended)
    implementation("androidx.navigation:navigation-compose:2.7.7") // Navegar entre telas
    implementation("mysql:mysql-connector-java:5.1.49")
    implementation(libs.androidx.compose.animation) //Driver do MySQL
    // ESTAS TRÊS LINHAS É PARA O ROOM: -->
    val room_version = "2.6.1" // Versão estável atual
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Para Coroutines
    ksp("androidx.room:room-compiler:$room_version") // O processador
    // <-- ROOM
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}