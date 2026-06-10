import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
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
    // compileSdk 36 is required by current androidx artifacts; targetSdk stays 34,
    // so runtime behavior is unchanged.
    compileSdk = 36

    defaultConfig {
        applicationId = "info.plateaukao.einkbro"
        minSdk = 24
        targetSdk = 34
        // Beta sits between 15.14.0 (15_14_00) and the eventual 15.15.0 (15_15_00)
        versionCode = 15_14_90
        versionName = "15.15.0-beta1"

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
            // Must stay on `proguard-android-optimize.txt`: dropping it to the plain
            // `proguard-android.txt` saved ~35s on R8 but grew the APK by ~1MB, which is
            // not acceptable for a ~7MB release. Don't re-try this without a new plan.
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
        // kxml2's service file uses a non-standard comma-separated entry that R8 cannot
        // parse; Android registers KXmlParser/KXmlSerializer via its platform XmlPullParserFactory.
        resources.excludes.add("META-INF/services/org.xmlpull.v1.XmlPullParserFactory")
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
            val abis = (project.findProperty("buildAbis") as String?)?.split(",")
                ?: listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            include(*abis.toTypedArray())
            // Only emit the universal APK when explicitly requested (`-PuniversalApk`).
            // Skipping it saves a full packaging pass for local iteration; the release
            // skill should pass -PuniversalApk when cutting a real release.
            isUniversalApk = project.hasProperty("universalApk")
        }
    }
    namespace = "info.plateaukao.einkbro"
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":ad-filter"))

    implementation(libs.material)

    // epub4j (maintained fork of epublib). Android ships xmlpull in the platform, so
    // the transitive xmlpull jar is excluded to avoid duplicate XmlPullParser classes.
    implementation(libs.epub4j.core) {
        exclude(group = "xmlpull")
    }

    // for epub saving: html processing
    implementation(libs.jsoup)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.timber)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)

    // for dark mode
    implementation(libs.androidx.webkit)

    // Koin
    implementation(libs.koin.core)
    testImplementation(libs.koin.test)
    implementation(libs.koin.android)
    implementation(libs.koin.android.compat)

    // Unit testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    // Real org.json implementation for JVM unit tests (the android.jar on the
    // unit-test classpath only contains non-functional stubs of JSONObject/JSONArray).
    testImplementation(libs.org.json)

    // memory leak detection
    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")

    // compose
    // Compose Material Design
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.extended)

    // Tooling support (Previews, etc.)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)

    // UI Tests
    androidTestImplementation(libs.compose.ui.test.junit4)

    implementation(libs.accompanist.drawablepainter)

    // reorder lazylist
    implementation(libs.reorderable)

    // okhttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)

    // adfilter
    implementation(libs.androidx.work.runtime.ktx)

    // media session for TTS notification
    implementation(libs.androidx.media)
}
