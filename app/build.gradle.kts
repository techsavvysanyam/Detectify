
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.gmail.techsavvysanyam.detectify"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gmail.techsavvysanyam.detectify"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    // core library camera-camera2
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    // CameraX Lifecycle library
    implementation(libs.androidx.camera.lifecycle)
    // CameraX View class
    implementation(libs.androidx.camera.view)
    implementation (libs.barcode.scanning)
    implementation (libs.face.detection)
    implementation (libs.text.recognition)

    //noinspection UseTomlInstead
    implementation ("com.google.mlkit:object-detection-custom:17.0.1")
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}