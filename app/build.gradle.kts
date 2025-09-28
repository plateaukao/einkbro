import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" // this version matches your Kotlin version
}

fun getLastCommitTimeStamp(): String {
    val epoch = System.getenv("SOURCE_DATE_EPOCH")?.toLongOrNull()
    val date = if (epoch != null) {
        Date(epoch * 1_000) // Convert seconds to milliseconds
    } else {
        Date()
    }

    val dateFormat = SimpleDateFormat("MMddHHmm")
    dateFormat.timeZone = TimeZone.getTimeZone("Asia/Taipei")
    return dateFormat.format(date)
}

fun showUpdateButton(): String {
    val value = project.findProperty("showUpdateButton")
    return value?.toString() ?: "false"
}

android {
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "info.plateaukao.einkbro"
        minSdk = 24
        targetSdk = 34
        versionCode = 15_02_00
        versionName = "15.2.0"

        buildConfigField("String", "lastCommitTime", "\"${getLastCommitTimeStamp()}\"")
        buildConfigField("boolean", "showUpdateButton", showUpdateButton())

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.txt"
            )
        }
        create("releaseDebuggable") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packagingOptions {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }

    lint {
        baseline = file("lint-baseline.xml")
        disable.add("MissingTranslation")
        checkReleaseBuilds = false
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    namespace = "info.plateaukao.einkbro"
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":ad-filter"))

    implementation(libs.material)

    // epublib
    implementation("com.positiondev.epublib:epublib-core:3.1") {
        exclude(group = "org.slf4j")
        exclude(group = "xmlpull")
    }
    implementation(libs.slf4j.api)
    implementation(libs.kxml2)

    // for epub saving: html processing
    implementation(libs.jsoup)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation(project(":ad-filter"))
    implementation(libs.timber)
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.navigation:navigation-runtime-ktx:2.7.7")
    implementation("androidx.navigation:navigation-compose:2.8.0-rc01")

    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.fragment:fragment-ktx:1.3.6")


    // for dark mode
    implementation("androidx.webkit:webkit:1.11.0")

    val koinVersion = "3.1.2"
    // Koin core features
    implementation("io.insert-koin:koin-core:$koinVersion")
    // Koin test features
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    // Android
    implementation("io.insert-koin:koin-android-compat:$koinVersion")

    // memory leak detection
    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")

    // compose
    // Compose Material Design
    implementation("androidx.compose.material:material:1.7.2")
    implementation("androidx.compose.material:material-icons-extended:1.7.2")

    // Tooling support (Previews, etc.)
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")

    // UI Tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.8")

    implementation(libs.accompanist.drawablepainter)

    // reorder lazylist
    implementation(libs.reorderable)

    // okhttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)

    // adfilter

    implementation("androidx.work:work-runtime-ktx:2.7.0")
}

