plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.smartgaugecontrol"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartgaugecontrol"
        minSdk = 28
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    packaging {
        resources {
            val exclusions = mutableListOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
            excludes.addAll(exclusions)
        }
    }

}

dependencies {
    implementation ("com.github.Gruzer:simple-gauge-android:0.3.1")
    implementation ("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("com.hivemq:hivemq-mqtt-client:1.2.1")


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
