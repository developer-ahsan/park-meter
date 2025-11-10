plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

repositories {
    google()
    mavenCentral()
}

android {
    val minSdkVersion: Int by project
    val latestSdkVersion: Int by project

    namespace = "com.parkmeter.og"
    compileSdk = latestSdkVersion

    defaultConfig {
        applicationId = "com.parkmeter.og"
        minSdk = minSdkVersion
        targetSdk = latestSdkVersion
        versionCode = 1
        versionName = "1.0.0"

        val backendUrl = project.property("EXAMPLE_BACKEND_URL").toString().trim('"')
        buildConfigField("String", "EXAMPLE_BACKEND_URL", "\"$backendUrl\"")
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    lint {
        enable += "Interoperability"
        disable += "MergeRootFrame"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore")
            storePassword = "park45pass"
            keyAlias = "park45-release"
            keyPassword = "park45pass"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // signingConfig = signingConfigs.getByName("release") // Temporarily disabled for build
            isDebuggable = false
            isJniDebuggable = false
            isPseudoLocalesEnabled = false
            
            // Production optimizations
            manifestPlaceholders["appName"] = "Park45 Meter"
            buildConfigField("boolean", "DEBUG", "false")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
        }
        
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appName"] = "Park45 Meter (Debug)"
            buildConfigField("boolean", "DEBUG", "true")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }
    }
}

val androidxLifecycleVersion = "2.6.2"
val retrofitVersion = "2.11.0"
val stripeTerminalVersion = "4.6.0"

dependencies {
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Annotations
    implementation("org.jetbrains:annotations:24.1.0")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-livedata:$androidxLifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel:$androidxLifecycleVersion")
    
    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // OK HTTP
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")

    // Stripe Terminal library
    implementation("com.stripe:stripeterminal-taptopay:$stripeTerminalVersion")
    implementation("com.stripe:stripeterminal-core:$stripeTerminalVersion")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Leak canary
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")

    // ZXing for QR code generation
    implementation("com.google.zxing:core:3.5.2")
}
