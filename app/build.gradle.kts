import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.play.publisher)
}

// A ValueSource so the timestamp is (re)computed on every build, even when the
// configuration cache is reused — a plain config-time value gets frozen into the
// cache entry and goes stale. SOURCE_DATE_EPOCH still wins so reproducible
// (F-Droid) builds stay deterministic.
abstract class BuildTimeValueSource :
    ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
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
}

// Play upload-key signing, same pattern as calliplus_android, but the secrets
// live outside the repo in ~/.secrets/einkbro-keystore.properties (keys:
// storeFile [absolute path], storePassword, keyAlias, keyPassword). Override the
// location with -Peinkbro.keystoreProperties=<path>.
val keystorePropsFile = File(
    project.findProperty("einkbro.keystoreProperties")?.toString()
        ?: (System.getProperty("user.home") + "/.secrets/einkbro-keystore.properties")
)
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}
val hasUploadKeystore = keystoreProps.containsKey("storeFile")

android {
    compileSdk = 36

    signingConfigs {
        if (hasUploadKeystore) {
            create("play") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "info.plateaukao.einkbro"
        minSdk = 24
        // targetSdk 36 defaults predictive back to on, which would stop KEYCODE_BACK
        // reaching BrowserActivity.onKeyDown -> KeyHandler; the manifest opts out
        // via enableOnBackInvokedCallback=false.
        targetSdk = 36
        versionCode = 16_01_01
        versionName = "16.1.1"

        // Google Drive backup sync: an "installed app" OAuth client (not a secret;
        // PKCE, no client secret) with the reversed-client-id custom-scheme redirect.
        // Override locally with -Peinkbro.driveOAuthClientId=<id>.apps.googleusercontent.com
        val driveOAuthClientId = project.findProperty("einkbro.driveOAuthClientId")?.toString()
            ?: "540228800389-7e35n5f8vvcf9bfq6lb3ra8ud2vclffk.apps.googleusercontent.com"
        val driveOAuthScheme =
            "com.googleusercontent.apps." + driveOAuthClientId.removeSuffix(".apps.googleusercontent.com")
        buildConfigField("String", "DRIVE_OAUTH_CLIENT_ID", "\"$driveOAuthClientId\"")
        buildConfigField("String", "DRIVE_OAUTH_REDIRECT", "\"$driveOAuthScheme:/oauth2redirect\"")

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
        // Same as `release` (minify, shrink, proguard, signing) but with a `.a`
        // applicationId suffix so it installs side-by-side with the real app as
        // `info.plateaukao.einkbro.a`. `initWith` copies all of release's config;
        // matchingFallbacks routes the :ad-filter dependency to its `release` variant.
        create("releaseAlt") {
            initWith(getByName("release"))
            applicationIdSuffix = ".a"
            versionNameSuffix = "-a"
            matchingFallbacks += "release"
        }
        // Google Play Store build, installed as `info.plateaukao.einkbro.g`.
        create("playRelease") {
            initWith(getByName("release"))
            applicationIdSuffix = ".g"
            matchingFallbacks += "release"
            if (hasUploadKeystore) {
                signingConfig = signingConfigs.getByName("play")
            }
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

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        // kxml2's service file uses a non-standard comma-separated entry that R8 cannot
        // parse; Android registers KXmlParser/KXmlSerializer via its platform XmlPullParserFactory.
        resources.excludes.add("META-INF/services/org.xmlpull.v1.XmlPullParserFactory")
    }

    androidResources {
        // Default ignore chain + tom_roush: drops pdfbox-android's 4.6MB of bundled
        // font/CMap/AFM assets, which are only needed for text extraction/rendering,
        // not for the COS-level operations (outline entries, page merging) we use.
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:<dir>_*:!CVS:!thumbs.db:!picasa.ini:!*~:!tom_roush"
    }

    lint {
        baseline = file("lint-baseline.xml")
        disable.add("MissingTranslation")
        checkReleaseBuilds = false
    }

    splits {
        abi {
            // ABI splits break `bundle*` (AAB) builds — the bundle task refuses
            // multi-APK shrunk resources — so disable them when building a bundle;
            // bundletool does its own per-ABI splitting on the Play side.
            val isBuildingBundle =
                gradle.startParameter.taskNames.any { it.lowercase().contains("bundle") }
            isEnable = !isBuildingBundle
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

    // Only the playRelease variant has a Play listing; disabling the rest keeps
    // aggregate GPP tasks (publishBundle, publishApps, ...) from trying to
    // publish variants whose applicationIds don't exist on Play.
    playConfigs {
        register("release") { enabled.set(false) }
        register("releaseAlt") { enabled.set(false) }
        register("releaseDebuggable") { enabled.set(false) }
    }
}

// Gradle Play Publisher (same plugin calliplus uses). Publishes the playRelease
// variant (`info.plateaukao.einkbro.g`) via `./gradlew publishPlayReleaseBundle`.
// Credentials path comes from the `playCredentials` key in the same ~/.secrets
// properties file as the upload keystore.
play {
    serviceAccountCredentials.set(
        file(
            keystoreProps.getProperty(
                "playCredentials",
                System.getProperty("user.home") + "/.secrets/einkbro-play-publisher.json"
            )
        )
    )
    defaultToAppBundles.set(true)
    // Safe default; production releases pass --track production explicitly.
    track.set("internal")
}

androidComponents {
    onVariants { variant ->
        // Wired as a task input (not read at configuration time), so each build
        // re-evaluates the ValueSource and regenerates BuildConfig without
        // invalidating the configuration cache.
        variant.buildConfigFields?.put(
            "lastCommitTime",
            providers.of(BuildTimeValueSource::class.java) {}
                .map { BuildConfigField("String", "\"$it\"", "build timestamp") }
        )
    }
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

    // PDF post-processing (TOC/outline entries, page merging). COS-level use only, so
    // BouncyCastle (encrypted-PDF support, ~4MB of unstrippable crypto resources) is
    // excluded — operating on password-protected PDFs will fail gracefully instead.
    implementation(libs.pdfbox.android) {
        exclude(group = "org.bouncycastle")
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
