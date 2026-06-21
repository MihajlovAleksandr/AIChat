plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.aichat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aichat"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SERVER_URL", "\"https://aichatt.xyz\"")
        buildConfigField("String", "PAYMENT_TOKEN", "\"pk_test_51TSMniF1IjO64lEWu6hmQKjvBp9bls13zJccmAcDLyVhC3wQ7nFyJsYY8zmWrVoCIcietaHoGYrklY6dzOsX6LwM005982mRfp\"")
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

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.process)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.play.services.mlkit.barcode.scanning)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")

    implementation("com.bihe0832.android:lib-sherpa-onnx:8.5.4")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:okhttp-sse:4.9.3")
    implementation("androidx.camera:camera-video:1.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")
    implementation("com.google.android.gms:play-services-ads:22.6.0")

    implementation("androidx.security:security-crypto:1.1.0-alpha03")
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    implementation("androidx.room:room-runtime:2.5.0")
    annotationProcessor("androidx.room:room-compiler:2.5.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("androidx.camera:camera-core:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-view:1.1.0")
    implementation("androidx.media:media:1.7.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    implementation("com.github.yalantis:ucrop:2.2.9")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.microsoft.signalr:signalr:7.0.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")

    implementation("com.stripe:stripe-android:21.29.0")
}