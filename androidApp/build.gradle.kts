import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// Release signing is provided out-of-band — via a git-ignored keystore.properties
// at the repo root, or env vars (CI). The keystore itself is NEVER committed. When
// nothing is configured the release build still runs R8 but stays unsigned.
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun releaseProp(name: String): String? = keystoreProperties.getProperty(name) ?: System.getenv(name)
val hasReleaseSigning = releaseProp("RELEASE_STORE_FILE") != null

// Sentry DSN for the `tm-app` project. A DSN is NOT a secret — it ships in every
// client — so it's committed as the default and applied to RELEASE builds only
// (see the release buildType). Overridable via the SENTRY_DSN env var / gradle
// property. Debug builds keep an empty DSN (defaultConfig) so dev crashes never
// reach the prod Sentry project.
val sentryDsn = releaseProp("SENTRY_DSN")
    ?: "https://fbb0ccfc2a3a80f62e86008ce7c5843d@o4511389786701824.ingest.de.sentry.io/4511656601387088"

android {
    namespace = "com.foxugly.trainingmanager_app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.foxugly.trainingmanager_app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        // Default: no DSN → Sentry auto-init treats a blank DSN as "off". This
        // covers debug builds; the release buildType overrides it with the real
        // DSN so only release crashes reach the prod Sentry project.
        manifestPlaceholders["sentryDsn"] = ""
    }
    buildFeatures {
        // BuildConfig.DEBUG is read in MainActivity to enable HTTP logging only
        // in debug builds.
        buildConfig = true
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseProp("RELEASE_STORE_FILE")!!)
                storePassword = releaseProp("RELEASE_STORE_PASSWORD")
                keyAlias = releaseProp("RELEASE_KEY_ALIAS")
                keyPassword = releaseProp("RELEASE_KEY_PASSWORD")
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            // Drop unreferenced resources alongside R8 code shrinking — minify
            // without this leaves the (often larger) resource table untrimmed.
            isShrinkResources = true
            // Activate Sentry on release builds (real DSN); debug stays disabled.
            manifestPlaceholders["sentryDsn"] = sentryDsn
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.androidx.core.ktx) // NotificationCompat
    // Firebase Cloud Messaging — the FirebaseMessagingService lives in this module
    // (it references MainActivity for the tap intent).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    // Crash + error reporting (OPERATIONS.md §3.8 parity with the backend/web).
    // Auto-initializes from the io.sentry.* manifest meta-data; disabled while
    // the DSN placeholder is empty.
    implementation(libs.sentry.android)
    debugImplementation(libs.compose.uiTooling)
}
