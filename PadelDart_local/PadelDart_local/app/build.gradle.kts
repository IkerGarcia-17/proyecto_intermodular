plugins {
    alias(libs.plugins.android.application)
    // Aplica google-services al módulo :app para que lea el google-services.json automáticamente
    alias(libs.plugins.google.services)
}

android {
    namespace = "dam.iker.padeldart"
    compileSdk = 36

    defaultConfig {
        applicationId = "dam.iker.padeldart"
        minSdk = 24
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Firebase BOM: define la versión una sola vez; los módulos la heredan automáticamente
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)  // Base de datos en la nube (NoSQL en tiempo real)
    implementation(libs.firebase.auth)       // Autenticación email/contraseña gestionada por Google

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
