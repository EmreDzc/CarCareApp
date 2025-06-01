plugins {
    id("com.android.application") // Bu şekilde kullanmak daha yaygın
    id("com.google.gms.google-services") // Bu şekilde kullanmak daha yaygın
}

android {
    namespace = "com.example.carcare"
    compileSdk = 35 // 35 henüz beta/alpha olabilir, stabil 34'ü kullanmak daha güvenli olabilir.
// Eğer 35 kullanmakta kararlıysan ve Android Studio'n güncelse sorun yok.

    defaultConfig {
        applicationId = "com.example.carcare"
        minSdk = 24
        targetSdk = 35 // compileSdk ile aynı olması genellikle önerilir.
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Geliştirme aşamasında false kalabilir, yayınlarken true yapmayı düşün.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Android geliştirmede 1.8 hala çok yaygın ve stabil.
        // 11 kullanıyorsan ve sorun yaşamıyorsan kalabilir.
        targetCompatibility = JavaVersion.VERSION_1_8 // sourceCompatibility ile aynı olmalı.
    }
// Kotlin kullanıyorsan (ki hata mesajında kotlin-dsl geçiyor):
// kotlinOptions {
//     jvmTarget = "1.8"
// }
}

dependencies {
// AndroidX ve Material Bağımlılıkları (libs.xyz formatını koruyarak)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0") // Bu genellikle appcompat veya material ile gelir,
// ama açıkça belirtmek sorun değil.
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0") // Bu da libs.coordinatorlayout ile aynı olmalı.
// Tekrar etmemek için birini seç.


    // Google Maps bağımlılıkları
    implementation ("com.google.android.gms:play-services-maps:18.1.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
// Google Maps ve Location (Tekrar edenleri kaldırdım)
    implementation("com.google.android.gms:play-services-maps:18.2.0") // Sürümü güncelledim, kontrol et.
    implementation("com.google.android.gms:play-services-location:21.2.0") // Sürümü güncelledim, kontrol et.
    implementation(libs.places) // com.google.android.libraries.places:places:X.Y.Z
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

// Firebase Bağımlılıkları (libs.xyz formatını koruyarak)
    implementation(platform("com.google.firebase:firebase-bom:33.0.0")) // Firebase BOM ekle, sürümleri yönetir.
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)

// Firebase
    implementation ("com.google.firebase:firebase-firestore:24.9.1")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.firebase:firebase-core:21.1.1")
    implementation ("com.google.firebase:firebase-auth:22.3.0")

// Glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

// Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Bu sürüm kalabilir, ama daha yenileri olabilir.
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.volley) // com.android.volley:volley:X.Y.Z


// Arka Plan İşlemleri
    implementation(libs.work.runtime) // androidx.work:work-runtime:X.Y.Z

    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")

    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")




// Test Bağımlılıkları
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}