plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.hlcyn.ui"
    compileSdk = 34

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.2"
    }

    defaultConfig {
        minSdk = 34
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
    implementation("androidx.compose.ui:ui:1.6.0-alpha02")
    implementation("androidx.compose.material3:material3:1.2.0-alpha04")
    implementation("androidx.compose.ui:ui-graphics:1.6.0-alpha02")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0-alpha02")
    implementation("androidx.compose.material:material-icons-extended:1.6.0-alpha02")
}
