plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.services)
}


android {
    namespace = "com.example.seniorcitizensupport"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.seniorcitizensupport"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "FIREBASE_API_KEY",
            "\"${project.properties["FIREBASE_API_KEY"]}\""
        )

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

    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth) // Corrected line
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
