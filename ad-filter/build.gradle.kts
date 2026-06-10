plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.kapt")
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("releaseDebuggable") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packagingOptions {
        resources.pickFirsts.add("META-INF/AL2.0")
        resources.pickFirsts.add("META-INF/LGPL2.1")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    namespace = "io.github.edsuns.adfilter"
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("com.github.Edsuns:SmoothProgressAnimator:1.0")

    implementation(project(":adblock-client"))

    implementation(libs.timber)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation(libs.kotlinx.serialization.json)

    implementation("androidx.work:work-runtime-ktx:2.10.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    implementation("com.github.Edsuns:HttpRequest:0.2")

    val mezzanineVersion = "1.2.1"
    implementation("com.github.Edsuns.Mezzanine:mezzanine:$mezzanineVersion")
    // Mezzanine ships no KSP processor, so this module stays on kapt until
    // upstream adds one or the embedded-JS approach is replaced.
    kapt("com.github.Edsuns.Mezzanine:mezzanine-compiler:$mezzanineVersion")
}

kapt {
    arguments {
        arg("mezzanine.projectPath", projectDir)
    }
}
