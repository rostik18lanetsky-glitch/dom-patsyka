plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.patsyk.widget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.patsyk.widget"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }

    // Постоянный ключ подписи — чтобы обновления ставились поверх старой версии
    signingConfigs {
        create("patsyk") {
            storeFile = file("patsyk.keystore")
            storePassword = "patsyk123"
            keyAlias = "patsyk"
            keyPassword = "patsyk123"
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("patsyk")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
