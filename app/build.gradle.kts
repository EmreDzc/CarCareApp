plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)

}

android {
    namespace = "com.example.carcare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.carcare"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
    // AndroidX ve Material bağımlılıkları
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation ("com.google.android.gms:play-services-maps:18.1.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation ("com.google.guava:guava:30.1.1-android")

    // Google Maps bağımlılıkları
    implementation ("com.google.android.gms:play-services-maps:18.1.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")

    // Firebase bağımlılıkları
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.coordinatorlayout)  // Örneğin: 'com.google.firebase:firebase-firestore:24.4.0'
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.work.runtime)
    implementation(libs.firebase.messaging)
    implementation(libs.volley)
    implementation(libs.places)
    implementation(libs.firebase.storage)

    implementation ("com.google.firebase:firebase-firestore:24.9.1")
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.google.android.material:material:1.10.0")

    implementation ("com.google.firebase:firebase-auth:22.3.0")
    implementation ("com.google.firebase:firebase-firestore:24.10.0")

    // Firebase Storage ekleyin
    implementation ("com.google.firebase:firebase-storage:20.3.0")

    // Image loading için Glide (muhtemelen zaten var)
    implementation ("com.github.bumptech.glide:glide:4.16.0")


    // Test bağımlılıkları
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}
