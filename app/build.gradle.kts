plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 21
        targetSdk = 34
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
    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.android.v10151)

    // Mapbox Turf for geometry calculations
    implementation(libs.mapbox.sdk.turf.v600)

    // GeoJSON Support
    implementation(libs.mapbox.sdk.geojson.v600)

    // If you need legacy FillLayer, GeoJsonSource, etc., add this:
    implementation(libs.mapbox.android.sdk)
        implementation(libs.navigationcore.android)  // Adds core Navigation SDK functionality
    implementation(libs.appcompat.v161)
    implementation(libs.kotlin.stdlib)
}
dependencies {
    implementation (libs.cardview)
    implementation(libs.maps.compose)
    implementation(libs.android)
    implementation(libs.mapbox.sdk.geojson)
    implementation(libs.mapbox.sdk.services)
    implementation(libs.mapbox.sdk.turf)
    implementation(libs.mapbox.sdk.core)
    implementation(libs.annotation)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation (libs.play.services.location)

}