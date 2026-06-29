# TrainingManager Athlete App — S1a Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL — `superpowers:test-driven-development`
> (strict red → green → commit on every TDD task) and `superpowers:verification-before-completion`
> (run the verify command and read its output **before** every commit; never claim green without evidence).
> For config/scaffold tasks that can't be unit-tested, the rhythm is **write file → run the exact
> `.\gradlew.bat` task → confirm the expected output → commit**.

## Goal

Stand up the **unblocked backbone** of the `trainingmanager_app` KMP project: a buildable two-module
Compose Multiplatform scaffold, the Ktor networking + auth stack (bearer attach, single-flight 401
refresh + replay, `Accept-Language`), persistent encrypted token storage with a fakeable seam, Koin
DI, a Material 3 emerald/dark theme, a minimal `App()` auth-check stub wired to both platform entry
points, and a host/JVM `commonTest` suite proving the interceptor contract. After S1a the app builds
(`:androidApp:assembleDebug`), the iOS host code exists (compiled later on macOS), and the auth/network
layer is unit-tested on Windows.

This plan **mirrors `PushIT_app` patterns, re-namespaced**. It is the first of several S1 plans; the
items in **Out of Scope** below are deferred to their own plans.

## Architecture

Two-module split, identical in shape to PushIT:

- **`:composeApp`** — KMP **library** (`com.android.kotlin.multiplatform.library`) holding all shared
  code + Compose UI. Source sets: `commonMain`, `androidMain`, `iosMain`, `commonTest`.
- **`:androidApp`** — runnable Android application (`MainActivity`) that builds the Koin graph and
  injects the prod base URL + `BuildConfig.DEBUG` logging.
- **`iosApp/`** — SwiftUI host embedding the Compose view via `UIViewControllerRepresentable`
  (built later on macOS; written now).

Dependency flow: platform `TokenStorage` (expect/actual) → `TokenStorageStore` → `TokenStore`
(interface seam) → `TrainingManagerApi` (Ktor + `AuthInterceptor` + `LanguageInterceptor`) →
`AuthRepository`. Wired by a Koin `appModule`. `App()` resolves `AuthRepository` from Koin and runs a
bootstrap auth-check stub.

## Tech Stack

Kotlin Multiplatform, Compose Multiplatform, Ktor client, kotlinx.serialization, kotlinx.coroutines,
multiplatform-settings, Koin (DI), Navigation Compose Multiplatform (catalog entry only — deferred),
Material 3, JUnit/kotlin-test + Ktor `MockEngine` (tests).

## Global Constraints

Copy these **verbatim**; do not paraphrase or "improve" versions.

| Thing | Value (from PushIT's `gradle/libs.versions.toml`) |
|---|---|
| Kotlin | `2.3.21` |
| Compose Multiplatform | `1.10.3` |
| AGP | `9.1.1` |
| Ktor | `3.1.3` |
| kotlinx-serialization | `1.8.1` |
| kotlinx-coroutines | `1.10.2` |
| kotlinx-datetime | `0.8.0` |
| multiplatform-settings | `1.3.0` |
| material3 | `1.10.0-alpha05` |
| composeMaterialIcons | `1.7.3` |
| androidx-security-crypto | `1.0.0` |
| android compileSdk / targetSdk / minSdk | `36` / `36` / `24` |
| Gradle wrapper | `gradle-9.3.1-bin` |
| **Koin (NEW)** | `4.1.0` (`io.insert-koin:koin-core`, `io.insert-koin:koin-compose`) |
| **Navigation Compose MP (NEW, deferred)** | `2.9.0-alpha13` (`org.jetbrains.androidx.navigation:navigation-compose`) |

- **Package / namespace:** `com.foxugly.trainingmanager_app` (androidApp namespace + applicationId);
  `com.foxugly.trainingmanager_app.shared` for the `:composeApp` android library namespace.
- **rootProject name:** `trainingmanager_app`.
- **Prod base URL (default, prod-only):** `https://tm-api.foxugly.com/api/v1/` — injected at the
  platform entry point; `enableHttpLogging = BuildConfig.DEBUG`.
- **Test task (Windows):** **`.\gradlew.bat :composeApp:testAndroidHostTest`** runs all `commonTest`
  on the JVM. **Never** run `:composeApp:test`, `:composeApp:allTests`, or any `ios*Test` task — the
  iOS Kotlin/Native targets only link on macOS.
- **Isolation rule (hard):** mirror PushIT patterns but **NEVER** add a dependency on PushIT, import
  any `com.foxugly.pushit_app.*` symbol, or modify the PushIT repo. Everything is re-namespaced to
  `com.foxugly.trainingmanager_app`.
- **iOS reality:** write `iosMain` + `iosApp/` code, but verify only via the Android host test task
  above. Do not attempt to build/run iOS targets on this machine.

## Out of Scope (deferred to later S1 plans — do NOT implement here)

- OpenAPI → Kotlin client codegen (`apiGen` Gradle task) + the generated services/models.
- Navigation Compose route graph, typed destinations, deep links, the bootstrap/startup screen flow.
  *(The catalog entry is added now; no nav code is written.)*
- Auth/onboarding screens: login / register / email-confirm / resend / forgot / reset / magic-link /
  invitation.
- Profile + change-password screens.
- i18n 5-locale string catalogs + `LanguageService` persistence (S1a ships only the `Accept-Language`
  interceptor + a trivial `LanguageProvider` holding the active tag).
- FCM / Firebase, push, notifications inbox (S4). No `google-services` plugin, no `firebase-bom` in
  S1a — they would force a `google-services.json` that does not exist yet.

Two **open decisions** that gate the deferred work (flag, do not resolve here): exact prod API host
confirmation, and the Turnstile-on-mobile strategy.

---

## Task 1 — Gradle wrapper, project settings, and version catalog

Scaffold/config task (no unit test). Establishes a Gradle project that resolves the catalog and plugins.

**Files**
- create `gradlew`, `gradlew.bat` (copied verbatim from PushIT)
- create `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` (copied)
- create `settings.gradle.kts`
- create `gradle.properties`
- create `build.gradle.kts` (root)
- create `gradle/libs.versions.toml`
- create `.gitignore`

**Interfaces**
- Produces: a Gradle build that declares `:composeApp` + `:androidApp` and a version catalog `libs`.
- Consumes: nothing.

**Steps**

1. Copy the wrapper (binary jar + scripts) verbatim from PushIT — do NOT hand-write the jar:
   ```bash
   cp "D:/Projects/IdeaProjects/PushIT_app/gradlew" "D:/Projects/IdeaProjects/trainingmanager_app/gradlew"
   cp "D:/Projects/IdeaProjects/PushIT_app/gradlew.bat" "D:/Projects/IdeaProjects/trainingmanager_app/gradlew.bat"
   mkdir -p "D:/Projects/IdeaProjects/trainingmanager_app/gradle/wrapper"
   cp "D:/Projects/IdeaProjects/PushIT_app/gradle/wrapper/gradle-wrapper.jar" "D:/Projects/IdeaProjects/trainingmanager_app/gradle/wrapper/gradle-wrapper.jar"
   cp "D:/Projects/IdeaProjects/PushIT_app/gradle/wrapper/gradle-wrapper.properties" "D:/Projects/IdeaProjects/trainingmanager_app/gradle/wrapper/gradle-wrapper.properties"
   ```
   `gradle-wrapper.properties` must read (verify after copy):
   ```properties
   distributionBase=GRADLE_USER_HOME
   distributionPath=wrapper/dists
   distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
   networkTimeout=10000
   validateDistributionUrl=true
   zipStoreBase=GRADLE_USER_HOME
   zipStorePath=wrapper/dists
   ```

2. Write `settings.gradle.kts`:
   ```kotlin
   rootProject.name = "trainingmanager_app"
   enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

   pluginManagement {
       repositories {
           google {
               mavenContent {
                   includeGroupAndSubgroups("androidx")
                   includeGroupAndSubgroups("com.android")
                   includeGroupAndSubgroups("com.google")
               }
           }
           mavenCentral()
           gradlePluginPortal()
       }
   }
   plugins {
       id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
   }

   dependencyResolutionManagement {
       repositories {
           google {
               mavenContent {
                   includeGroupAndSubgroups("androidx")
                   includeGroupAndSubgroups("com.android")
                   includeGroupAndSubgroups("com.google")
               }
           }
           mavenCentral()
       }
   }

   include(":composeApp")
   include(":androidApp")
   ```

3. Write `gradle.properties`:
   ```properties
   #Kotlin
   kotlin.code.style=official
   kotlin.daemon.jvmargs=-Xmx3072M
   #Gradle
   org.gradle.jvmargs=-Xmx4096M -Dfile.encoding=UTF-8
   org.gradle.configuration-cache=true
   org.gradle.caching=true
   org.gradle.parallel=true
   #Android
   android.nonTransitiveRClass=true
   android.useAndroidX=true
   ```

4. Write the root `build.gradle.kts`:
   ```kotlin
   plugins {
       alias(libs.plugins.androidApplication) apply false
       alias(libs.plugins.androidLibrary) apply false
       alias(libs.plugins.androidMultiplatformLibrary) apply false
       alias(libs.plugins.composeMultiplatform) apply false
       alias(libs.plugins.composeCompiler) apply false
       alias(libs.plugins.kotlinMultiplatform) apply false
       alias(libs.plugins.kotlinSerialization) apply false
   }
   ```

5. Write `gradle/libs.versions.toml` (PushIT baseline minus FCM/camera/mlkit, plus Koin + Navigation):
   ```toml
   [versions]
   agp = "9.1.1"
   android-compileSdk = "36"
   android-minSdk = "24"
   android-targetSdk = "36"
   androidx-activity = "1.13.0"
   androidx-appcompat = "1.7.1"
   androidx-core = "1.18.0"
   androidx-espresso = "3.7.0"
   androidx-lifecycle = "2.10.0"
   androidx-testExt = "1.3.0"
   composeMultiplatform = "1.10.3"
   # material-icons-core is frozen at 1.7.3 (JetBrains stopped publishing it past
   # that), so it has its own version pin — NOT composeMultiplatform.
   composeMaterialIcons = "1.7.3"
   junit = "4.13.2"
   kotlin = "2.3.21"
   material3 = "1.10.0-alpha05"
   ktor = "3.1.3"
   kotlinxSerialization = "1.8.1"
   kotlinxDatetime = "0.8.0"
   multiplatformSettings = "1.3.0"
   androidxSecurityCrypto = "1.0.0"
   kotlinxCoroutines = "1.10.2"
   # NEW vs PushIT
   koin = "4.1.0"
   # Deferred to the Navigation S1 plan; catalog entry only (not referenced by any
   # module in S1a, so it is never resolved/downloaded until used). Confirm exact
   # version against Compose Multiplatform 1.10.3 when the nav graph is built.
   navigationCompose = "2.9.0-alpha13"

   [libraries]
   kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
   kotlin-testJunit = { module = "org.jetbrains.kotlin:kotlin-test-junit", version.ref = "kotlin" }
   junit = { module = "junit:junit", version.ref = "junit" }
   androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "androidx-core" }
   androidx-testExt-junit = { module = "androidx.test.ext:junit", version.ref = "androidx-testExt" }
   androidx-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "androidx-espresso" }
   androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "androidx-appcompat" }
   androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity" }
   compose-uiTooling = { module = "org.jetbrains.compose.ui:ui-tooling", version.ref = "composeMultiplatform" }
   androidx-lifecycle-viewmodelCompose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }
   androidx-lifecycle-runtimeCompose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }
   compose-runtime = { module = "org.jetbrains.compose.runtime:runtime", version.ref = "composeMultiplatform" }
   compose-foundation = { module = "org.jetbrains.compose.foundation:foundation", version.ref = "composeMultiplatform" }
   compose-material3 = { module = "org.jetbrains.compose.material3:material3", version.ref = "material3" }
   compose-ui = { module = "org.jetbrains.compose.ui:ui", version.ref = "composeMultiplatform" }
   compose-components-resources = { module = "org.jetbrains.compose.components:components-resources", version.ref = "composeMultiplatform" }
   compose-uiToolingPreview = { module = "org.jetbrains.compose.ui:ui-tooling-preview", version.ref = "composeMultiplatform" }
   compose-material-icons-core = { module = "org.jetbrains.compose.material:material-icons-core", version.ref = "composeMaterialIcons" }
   ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
   ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
   ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
   ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
   ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
   ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
   ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
   kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
   kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
   kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
   kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
   multiplatform-settings = { module = "com.russhwolf:multiplatform-settings", version.ref = "multiplatformSettings" }
   multiplatform-settings-no-arg = { module = "com.russhwolf:multiplatform-settings-no-arg", version.ref = "multiplatformSettings" }
   androidx-security-crypto = { module = "androidx.security:security-crypto", version.ref = "androidxSecurityCrypto" }
   koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
   koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
   navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigationCompose" }

   [plugins]
   androidApplication = { id = "com.android.application", version.ref = "agp" }
   androidLibrary = { id = "com.android.library", version.ref = "agp" }
   androidMultiplatformLibrary = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
   composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
   composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
   kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
   kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
   ```

6. Write `.gitignore`:
   ```gitignore
   .gradle/
   build/
   /local.properties
   /keystore.properties
   *.keystore
   .idea/
   .kotlin/
   .DS_Store
   captures/
   xcuserdata/
   *.xcworkspace/xcuserdata/
   ```

7. Verify the wrapper resolves and the catalog parses:
   ```
   .\gradlew.bat --version
   ```
   Expected: Gradle `9.3.1` banner, exit 0. *(Full project tasks come online in Task 2; `--version`
   only needs the wrapper.)*

8. Commit:
   ```
   git commit -m "S1a: Gradle wrapper, settings, and version catalog (Koin + Navigation added)"
   ```

---

## Task 2 — Buildable two-module scaffold (modules, hosts, AppLogger, plain theme, placeholder App)

Scaffold/config task. Ends with `:androidApp:assembleDebug` green and the host test task runnable
(zero tests yet). The theme is intentionally a plain `MaterialTheme {}` here; Task 8 replaces it with
the emerald/dark palette.

**Files**
- create `composeApp/build.gradle.kts`
- create `androidApp/build.gradle.kts`
- create `androidApp/src/main/AndroidManifest.xml`
- create `androidApp/src/main/res/values/strings.xml`
- create `androidApp/src/main/res/xml/network_security_config.xml`
- create `androidApp/proguard-rules.pro`
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/diagnostics/AppLogger.kt`
- create `composeApp/src/androidMain/kotlin/com/foxugly/trainingmanager_app/diagnostics/AppLogger.android.kt`
- create `composeApp/src/iosMain/kotlin/com/foxugly/trainingmanager_app/diagnostics/AppLogger.ios.kt`
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/theme/Theme.kt`
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/App.kt`
- create `composeApp/src/androidMain/kotlin/com/foxugly/trainingmanager_app/MainActivityScaffold.kt` *(temporary host; replaced in Task 10 — see note)*
- create `composeApp/src/iosMain/kotlin/com/foxugly/trainingmanager_app/MainViewController.kt`
- create `androidApp/src/main/kotlin/com/foxugly/trainingmanager_app/MainActivity.kt`
- create `iosApp/iosApp/ContentView.swift`
- create `iosApp/iosApp/iOSApp.swift`
- create `iosApp/iosApp/Info.plist`

> Note: the real Koin-wired `MainActivity`/`App()` land in Tasks 9–10. Here `MainActivity` and `App()`
> are deliberately minimal so the project compiles and runs before DI exists. Do NOT create
> `MainActivityScaffold.kt` if you prefer — it is listed only to flag that the androidMain host is
> finalized later; the canonical Android entry point is `androidApp/.../MainActivity.kt`.

**Interfaces**
- Produces: `App(): Unit` (composable, no args yet); `MainViewController(): UIViewController`;
  `expect object AppLogger`.
- Consumes: the version catalog from Task 1.

**Steps**

1. Write `composeApp/build.gradle.kts`:
   ```kotlin
   import org.jetbrains.kotlin.gradle.dsl.JvmTarget

   plugins {
       alias(libs.plugins.kotlinMultiplatform)
       alias(libs.plugins.androidMultiplatformLibrary)
       alias(libs.plugins.composeMultiplatform)
       alias(libs.plugins.composeCompiler)
       alias(libs.plugins.kotlinSerialization)
   }

   kotlin {
       android {
           namespace = "com.foxugly.trainingmanager_app.shared"
           compileSdk = libs.versions.android.compileSdk.get().toInt()
           minSdk = libs.versions.android.minSdk.get().toInt()
           compilerOptions {
               jvmTarget.set(JvmTarget.JVM_11)
               freeCompilerArgs.add("-Xexpect-actual-classes")
           }
           androidResources {
               enable = true
           }
           // Enable JVM-host unit tests so commonTest runs on the desktop (incl.
           // Windows, no Mac/Xcode needed) via :composeApp:testAndroidHostTest.
           // isReturnDefaultValues: android.util.Log (used by AppLogger) is not on
           // the JVM test classpath — return defaults instead of throwing "not mocked".
           withHostTestBuilder {}.configure {
               isReturnDefaultValues = true
           }
       }

       listOf(
           iosArm64(),
           iosSimulatorArm64()
       ).forEach { iosTarget ->
           iosTarget.binaries.framework {
               baseName = "ComposeApp"
               isStatic = true
           }
       }

       sourceSets {
           commonMain.dependencies {
               implementation(libs.compose.runtime)
               implementation(libs.compose.foundation)
               implementation(libs.compose.material3)
               implementation(libs.compose.material.icons.core)
               implementation(libs.compose.ui)
               implementation(libs.compose.components.resources)
               implementation(libs.compose.uiToolingPreview)
               implementation(libs.androidx.lifecycle.viewmodelCompose)
               implementation(libs.androidx.lifecycle.runtimeCompose)
               implementation(libs.ktor.client.core)
               implementation(libs.ktor.client.content.negotiation)
               implementation(libs.ktor.serialization.kotlinx.json)
               implementation(libs.ktor.client.logging)
               implementation(libs.kotlinx.serialization.json)
               implementation(libs.kotlinx.datetime)
               implementation(libs.kotlinx.coroutines.core)
               implementation(libs.multiplatform.settings)
               implementation(libs.multiplatform.settings.no.arg)
               implementation(libs.koin.core)
               implementation(libs.koin.compose)
           }
           commonTest.dependencies {
               implementation(libs.kotlin.test)
               implementation(libs.kotlinx.coroutines.test)
               implementation(libs.ktor.client.mock)
           }
           androidMain.dependencies {
               implementation(libs.androidx.activity.compose)
               implementation(libs.ktor.client.okhttp)
               implementation(libs.androidx.security.crypto)
           }
           iosMain.dependencies {
               implementation(libs.ktor.client.darwin)
           }
       }
   }

   dependencies {
       add("androidRuntimeClasspath", libs.compose.uiTooling)
   }
   ```

2. Write `androidApp/build.gradle.kts` (no `googleServices`, no `firebase-bom`):
   ```kotlin
   import java.util.Properties
   import org.jetbrains.kotlin.gradle.dsl.JvmTarget

   plugins {
       alias(libs.plugins.androidApplication)
       alias(libs.plugins.composeMultiplatform)
       alias(libs.plugins.composeCompiler)
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

   android {
       namespace = "com.foxugly.trainingmanager_app"
       compileSdk = libs.versions.android.compileSdk.get().toInt()

       defaultConfig {
           applicationId = "com.foxugly.trainingmanager_app"
           minSdk = libs.versions.android.minSdk.get().toInt()
           targetSdk = libs.versions.android.targetSdk.get().toInt()
           versionCode = 1
           versionName = "1.0.0"
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
       debugImplementation(libs.compose.uiTooling)
   }
   ```

3. Write `androidApp/proguard-rules.pro`:
   ```pro
   # kotlinx.serialization — keep @Serializable DTOs + generated serializers.
   -keepclassmembers,allowobfuscation class * {
       @kotlinx.serialization.SerialName <fields>;
   }
   -keepclasseswithmembers,allowshrinking class * {
       @kotlinx.serialization.Serializable <methods>;
   }
   -if @kotlinx.serialization.Serializable class **
   -keepclassmembers class <1> {
       static <1>$Companion Companion;
       *** serializer(...);
   }
   # Ktor + coroutines internals referenced reflectively.
   -keep class io.ktor.** { *; }
   -dontwarn io.ktor.**
   -dontwarn kotlinx.coroutines.**
   ```

4. Write `androidApp/src/main/AndroidManifest.xml`:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <manifest xmlns:android="http://schemas.android.com/apk/res/android">

       <uses-permission android:name="android.permission.INTERNET" />

       <application
               android:allowBackup="false"
               android:label="@string/app_name"
               android:networkSecurityConfig="@xml/network_security_config"
               android:supportsRtl="true"
               android:theme="@android:style/Theme.Material.Light.NoActionBar">
           <activity
                   android:exported="true"
                   android:launchMode="singleTop"
                   android:name=".MainActivity">
               <intent-filter>
                   <action android:name="android.intent.action.MAIN"/>
                   <category android:name="android.intent.category.LAUNCHER"/>
               </intent-filter>
           </activity>
       </application>

   </manifest>
   ```

5. Write `androidApp/src/main/res/values/strings.xml`:
   ```xml
   <resources>
       <string name="app_name">TrainingManager</string>
   </resources>
   ```

6. Write `androidApp/src/main/res/xml/network_security_config.xml`:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <network-security-config>
       <!-- Prod-only: the app talks exclusively to https://tm-api.foxugly.com.
            No cleartext traffic is permitted (there is no local/dev backend). -->
       <base-config cleartextTrafficPermitted="false" />
   </network-security-config>
   ```

7. Write `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/diagnostics/AppLogger.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.diagnostics

   expect object AppLogger {
       fun debug(tag: String, message: String)
       fun info(tag: String, message: String)
       fun warn(tag: String, message: String, throwable: Throwable? = null)
       fun error(tag: String, message: String, throwable: Throwable? = null)
   }
   ```

8. Write `composeApp/src/androidMain/kotlin/com/foxugly/trainingmanager_app/diagnostics/AppLogger.android.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.diagnostics

   import android.util.Log

   actual object AppLogger {
       actual fun debug(tag: String, message: String) {
           Log.d(tag, message)
       }

       actual fun info(tag: String, message: String) {
           Log.i(tag, message)
       }

       actual fun warn(tag: String, message: String, throwable: Throwable?) {
           Log.w(tag, message, throwable)
       }

       actual fun error(tag: String, message: String, throwable: Throwable?) {
           Log.e(tag, message, throwable)
       }
   }
   ```

9. Write `composeApp/src/iosMain/kotlin/com/foxugly/trainingmanager_app/diagnostics/AppLogger.ios.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.diagnostics

   actual object AppLogger {
       actual fun debug(tag: String, message: String) {
           println("DEBUG/$tag: $message")
       }

       actual fun info(tag: String, message: String) {
           println("INFO/$tag: $message")
       }

       actual fun warn(tag: String, message: String, throwable: Throwable?) {
           println("WARN/$tag: $message ${throwable?.message.orEmpty()}")
       }

       actual fun error(tag: String, message: String, throwable: Throwable?) {
           println("ERROR/$tag: $message ${throwable?.message.orEmpty()}")
       }
   }
   ```

10. Write a temporary plain theme `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/theme/Theme.kt`:
    ```kotlin
    package com.foxugly.trainingmanager_app.ui.theme

    import androidx.compose.material3.MaterialTheme
    import androidx.compose.runtime.Composable

    /** Placeholder theme — replaced by the emerald/dark palette in Task 8. */
    @Composable
    fun TrainingManagerTheme(content: @Composable () -> Unit) {
        MaterialTheme(content = content)
    }
    ```

11. Write the placeholder `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/App.kt`:
    ```kotlin
    package com.foxugly.trainingmanager_app

    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import com.foxugly.trainingmanager_app.ui.theme.TrainingManagerTheme

    /** Placeholder root — replaced by the auth-check stub in Task 10. */
    @Composable
    fun App() {
        TrainingManagerTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("TrainingManager")
            }
        }
    }
    ```

12. Write `composeApp/src/iosMain/kotlin/com/foxugly/trainingmanager_app/MainViewController.kt`:
    ```kotlin
    package com.foxugly.trainingmanager_app

    import androidx.compose.ui.window.ComposeUIViewController

    fun MainViewController() = ComposeUIViewController {
        App()
    }
    ```

13. Write `androidApp/src/main/kotlin/com/foxugly/trainingmanager_app/MainActivity.kt` (minimal host):
    ```kotlin
    package com.foxugly.trainingmanager_app

    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.enableEdgeToEdge

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContent {
                App()
            }
        }
    }
    ```

14. Write `iosApp/iosApp/ContentView.swift`:
    ```swift
    import UIKit
    import SwiftUI
    import ComposeApp

    struct ComposeView: UIViewControllerRepresentable {
        func makeUIViewController(context: Context) -> UIViewController {
            MainViewControllerKt.MainViewController()
        }

        func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
    }

    struct ContentView: View {
        var body: some View {
            ComposeView()
                .ignoresSafeArea()
        }
    }
    ```

15. Write `iosApp/iosApp/iOSApp.swift`:
    ```swift
    import SwiftUI

    @main
    struct iOSApp: App {
        var body: some Scene {
            WindowGroup {
                ContentView()
            }
        }
    }
    ```

16. Write `iosApp/iosApp/Info.plist`:
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
        <key>CFBundleDevelopmentRegion</key>
        <string>$(DEVELOPMENT_LANGUAGE)</string>
        <key>CFBundleExecutable</key>
        <string>$(EXECUTABLE_NAME)</string>
        <key>CFBundleIdentifier</key>
        <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
        <key>CFBundleInfoDictionaryVersion</key>
        <string>6.0</string>
        <key>CFBundleName</key>
        <string>$(PRODUCT_NAME)</string>
        <key>CFBundlePackageType</key>
        <string>$(PRODUCT_BUNDLE_PACKAGE_TYPE)</string>
        <key>CFBundleShortVersionString</key>
        <string>1.0.0</string>
        <key>CFBundleVersion</key>
        <string>1</string>
        <key>LSRequiresIPhoneOS</key>
        <true/>
        <key>UILaunchScreen</key>
        <dict/>
        <key>UISupportedInterfaceOrientations</key>
        <array>
            <string>UIInterfaceOrientationPortrait</string>
        </array>
    </dict>
    </plist>
    ```
    > The Xcode project (`iosApp.xcodeproj`) is generated on macOS when iOS work begins; it is not
    > created on Windows. These Swift/plist files are the host source it will reference.

17. Verify the Android app assembles and the host-test task is wired (no tests yet):
    ```
    .\gradlew.bat :androidApp:assembleDebug
    .\gradlew.bat :composeApp:testAndroidHostTest
    ```
    Expected: both `BUILD SUCCESSFUL`. The test task reports `NO-SOURCE` / no tests executed.

18. Commit:
    ```
    git commit -m "S1a: buildable two-module scaffold (hosts, AppLogger, placeholder App + theme)"
    ```

---

## Task 3 — Token storage seam (`TokenStore`) + `TokenStorage` expect/actual + test fake

TDD. The expect/actual `TokenStorage` cannot be instantiated in `commonTest`, so the testable unit is
the `TokenStore` interface contract exercised through the in-memory `FakeTokenStore`. This fake is the
foundation every later networking test reuses.

**Files**
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/storage/TokenStore.kt`
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/storage/TokenStorage.kt`
- create `composeApp/src/androidMain/kotlin/com/foxugly/trainingmanager_app/data/storage/TokenStorage.android.kt`
- create `composeApp/src/iosMain/kotlin/com/foxugly/trainingmanager_app/data/storage/TokenStorage.ios.kt`
- create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/TestFakes.kt`
- create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/data/storage/FakeTokenStoreTest.kt`

**Interfaces**
- Produces:
  - `interface TokenStore { getAccessToken(): String?; setAccessToken(token: String?); getRefreshToken(): String?; setRefreshToken(token: String?); getRemember(): Boolean; setRemember(value: Boolean); clearAuthTokens() }`
  - `class TokenStorageStore(storage: TokenStorage) : TokenStore`
  - `expect class TokenStorage` with the same accessor surface + `getLanguage()/setLanguage()`
  - `internal class FakeTokenStore(...) : TokenStore` (with a `cleared: Boolean` flag)
- Consumes: nothing (pure data layer).

**Steps**

1. Write the failing test `FakeTokenStoreTest.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.storage

   import com.foxugly.trainingmanager_app.FakeTokenStore
   import kotlin.test.Test
   import kotlin.test.assertEquals
   import kotlin.test.assertFalse
   import kotlin.test.assertNull
   import kotlin.test.assertTrue

   class FakeTokenStoreTest {

       @Test
       fun storesAndReadsBackTokensAndRemember() {
           val store = FakeTokenStore()
           assertNull(store.getAccessToken())
           assertFalse(store.getRemember())

           store.setAccessToken("a")
           store.setRefreshToken("r")
           store.setRemember(true)

           assertEquals("a", store.getAccessToken())
           assertEquals("r", store.getRefreshToken())
           assertTrue(store.getRemember())
       }

       @Test
       fun clearAuthTokensWipesAccessAndRefreshAndFlagsCleared() {
           val store = FakeTokenStore(access = "a", refresh = "r")
           store.clearAuthTokens()
           assertNull(store.getAccessToken())
           assertNull(store.getRefreshToken())
           assertTrue(store.cleared)
       }
   }
   ```

2. Run it — expected **fail to compile** (`TokenStore`, `FakeTokenStore` do not exist):
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

3. Implement `TokenStore.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.storage

   /**
    * Token persistence seam used by the data layer (TrainingManagerApi /
    * AuthInterceptor / repositories). [TokenStorage] is an `expect class` and so
    * can't be faked in commonTest; depending on this interface — with
    * [TokenStorageStore] adapting the real platform storage — lets tests inject a
    * fake without touching the expect/actual declarations.
    */
   interface TokenStore {
       fun getAccessToken(): String?
       fun setAccessToken(token: String?)
       fun getRefreshToken(): String?
       fun setRefreshToken(token: String?)
       // "Stay logged in" preference. Sent as the login `remember` field (backend
       // chooses a 7d vs 30d refresh TTL); persisted so a relaunch keeps the choice.
       fun getRemember(): Boolean
       fun setRemember(value: Boolean)
       fun clearAuthTokens()
   }

   /** Adapts the platform [TokenStorage] to [TokenStore] (pure commonMain — no
    * expect/actual change, so no iOS-side risk). */
   class TokenStorageStore(private val storage: TokenStorage) : TokenStore {
       override fun getAccessToken(): String? = storage.getAccessToken()
       override fun setAccessToken(token: String?) = storage.setAccessToken(token)
       override fun getRefreshToken(): String? = storage.getRefreshToken()
       override fun setRefreshToken(token: String?) = storage.setRefreshToken(token)
       override fun getRemember(): Boolean = storage.getRemember()
       override fun setRemember(value: Boolean) = storage.setRemember(value)
       override fun clearAuthTokens() = storage.clearAuthTokens()
   }
   ```

4. Implement the expect `TokenStorage.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.storage

   expect class TokenStorage {
       fun getAccessToken(): String?
       fun setAccessToken(token: String?)
       fun getRefreshToken(): String?
       fun setRefreshToken(token: String?)
       fun getRemember(): Boolean
       fun setRemember(value: Boolean)
       fun clearAuthTokens()
       // UI language preference (lowercase ISO code, e.g. "fr"). Used by the
       // Accept-Language interceptor; full LanguageService persistence is deferred.
       fun getLanguage(): String?
       fun setLanguage(code: String?)
   }
   ```

5. Implement `TokenStorage.android.kt` (EncryptedSharedPreferences):
   ```kotlin
   package com.foxugly.trainingmanager_app.data.storage

   import android.content.Context
   import android.content.SharedPreferences
   import androidx.security.crypto.EncryptedSharedPreferences
   import androidx.security.crypto.MasterKeys
   import com.foxugly.trainingmanager_app.diagnostics.AppLogger

   actual class TokenStorage(context: Context) {

       private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
           "trainingmanager_secure_prefs",
           MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
           context,
           EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
           EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
       )

       actual fun getAccessToken(): String? = readString(KEY_ACCESS)
       actual fun setAccessToken(token: String?) = writeString(KEY_ACCESS, token)

       actual fun getRefreshToken(): String? = readString(KEY_REFRESH)
       actual fun setRefreshToken(token: String?) = writeString(KEY_REFRESH, token)

       actual fun getRemember(): Boolean =
           runCatching { prefs.getBoolean(KEY_REMEMBER, false) }
               .onFailure { AppLogger.error(TAG, "Failed to read $KEY_REMEMBER", it) }
               .getOrDefault(false)

       actual fun setRemember(value: Boolean) {
           val committed = runCatching { prefs.edit().putBoolean(KEY_REMEMBER, value).commit() }
               .onFailure { AppLogger.error(TAG, "Failed to write $KEY_REMEMBER", it) }
               .getOrDefault(false)
           if (!committed) AppLogger.error(TAG, "Write of $KEY_REMEMBER was not committed")
       }

       actual fun getLanguage(): String? = readString(KEY_LANGUAGE)
       actual fun setLanguage(code: String?) = writeString(KEY_LANGUAGE, code)

       actual fun clearAuthTokens() {
           val committed = runCatching {
               prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).commit()
           }.onFailure { AppLogger.error(TAG, "Failed to clear auth tokens", it) }
               .getOrDefault(false)
           if (committed) AppLogger.info(TAG, "Auth tokens cleared")
           else AppLogger.error(TAG, "Auth token clear was not committed")
       }

       private fun readString(key: String): String? =
           runCatching { prefs.getString(key, null) }
               .onFailure { AppLogger.error(TAG, "Failed to read $key", it) }
               .getOrNull()

       private fun writeString(key: String, value: String?) {
           // commit() (synchronous): an EncryptedSharedPreferences write failure must
           // not be swallowed — a token that never landed reads back null next launch
           // and surfaces as an unexpected logout.
           val committed = runCatching { prefs.edit().putString(key, value).commit() }
               .onFailure { AppLogger.error(TAG, "Failed to write $key", it) }
               .getOrDefault(false)
           if (!committed) AppLogger.error(TAG, "Write of $key was not committed")
       }

       companion object {
           private const val TAG = "TM/TokenStorage"
           private const val KEY_ACCESS = "access_token"
           private const val KEY_REFRESH = "refresh_token"
           private const val KEY_REMEMBER = "remember"
           private const val KEY_LANGUAGE = "ui_language"
       }
   }
   ```

6. Implement `TokenStorage.ios.kt` (Keychain for secrets, NSUserDefaults for prefs):
   ```kotlin
   package com.foxugly.trainingmanager_app.data.storage

   import kotlinx.cinterop.ExperimentalForeignApi
   import kotlinx.cinterop.alloc
   import kotlinx.cinterop.memScoped
   import kotlinx.cinterop.ptr
   import kotlinx.cinterop.value
   import platform.CoreFoundation.CFDictionaryRef
   import platform.CoreFoundation.CFTypeRefVar
   import platform.CoreFoundation.kCFBooleanTrue
   import platform.Foundation.CFBridgingRelease
   import platform.Foundation.CFBridgingRetain
   import platform.Foundation.NSData
   import platform.Foundation.NSString
   import platform.Foundation.NSUTF8StringEncoding
   import platform.Foundation.NSUserDefaults
   import platform.Foundation.create
   import platform.Foundation.dataUsingEncoding
   import platform.Security.SecItemAdd
   import platform.Security.SecItemCopyMatching
   import platform.Security.SecItemDelete
   import platform.Security.errSecItemNotFound
   import platform.Security.errSecSuccess
   import platform.Security.kSecAttrAccessible
   import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
   import platform.Security.kSecAttrAccount
   import platform.Security.kSecAttrService
   import platform.Security.kSecClass
   import platform.Security.kSecClassGenericPassword
   import platform.Security.kSecMatchLimit
   import platform.Security.kSecMatchLimitOne
   import platform.Security.kSecReturnData
   import platform.Security.kSecValueData

   /**
    * iOS token storage. The two JWT secrets live in the Keychain
    * (kSecClassGenericPassword, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly —
    * encrypted at rest, not synced, not in backups). Non-secret prefs (remember
    * flag, UI language) stay in NSUserDefaults.
    *
    * iosMain compiles only on macOS; verified by review, not the Windows host test.
    */
   @OptIn(ExperimentalForeignApi::class)
   actual class TokenStorage {

       private val defaults = NSUserDefaults.standardUserDefaults

       actual fun getAccessToken(): String? = keychainGet(KEY_ACCESS)
       actual fun setAccessToken(token: String?) = keychainSet(KEY_ACCESS, token)

       actual fun getRefreshToken(): String? = keychainGet(KEY_REFRESH)
       actual fun setRefreshToken(token: String?) = keychainSet(KEY_REFRESH, token)

       actual fun getRemember(): Boolean = defaults.boolForKey(KEY_REMEMBER)
       actual fun setRemember(value: Boolean) = defaults.setBool(value, forKey = KEY_REMEMBER)

       actual fun getLanguage(): String? = defaults.stringForKey(KEY_LANGUAGE)
       actual fun setLanguage(code: String?) {
           if (code != null) defaults.setObject(code, forKey = KEY_LANGUAGE)
           else defaults.removeObjectForKey(KEY_LANGUAGE)
       }

       actual fun clearAuthTokens() {
           keychainSet(KEY_ACCESS, null)
           keychainSet(KEY_REFRESH, null)
       }

       private fun keychainBaseQuery(account: String): Map<Any?, Any?> = mapOf(
           kSecClass to kSecClassGenericPassword,
           kSecAttrService to SERVICE,
           kSecAttrAccount to account,
       )

       private fun keychainGet(account: String): String? = memScoped {
           val query = (
               keychainBaseQuery(account) + mapOf(
                   kSecReturnData to kCFBooleanTrue,
                   kSecMatchLimit to kSecMatchLimitOne,
               )
           ).toCFDictionary()
           try {
               val result = alloc<CFTypeRefVar>()
               val status = SecItemCopyMatching(query, result.ptr)
               if (status != errSecSuccess) return@memScoped null
               val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
               NSString.create(data, NSUTF8StringEncoding) as String?
           } finally {
               CFBridgingRelease(query)
           }
       }

       private fun keychainSet(account: String, value: String?) {
           keychainDelete(account)
           if (value == null) return
           val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
           val attributes = (
               keychainBaseQuery(account) + mapOf(
                   kSecValueData to data,
                   kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
               )
           ).toCFDictionary()
           try {
               SecItemAdd(attributes, null)
           } finally {
               CFBridgingRelease(attributes)
           }
       }

       private fun keychainDelete(account: String) {
           val query = keychainBaseQuery(account).toCFDictionary()
           try {
               val status = SecItemDelete(query)
               @Suppress("UNUSED_EXPRESSION")
               (status == errSecSuccess || status == errSecItemNotFound)
           } finally {
               CFBridgingRelease(query)
           }
       }

       private fun Map<Any?, Any?>.toCFDictionary(): CFDictionaryRef? {
           val dict = platform.Foundation.NSMutableDictionary()
           for ((k, v) in this) {
               if (k != null && v != null) dict.setObject(v, forKey = k as Any)
           }
           return CFBridgingRetain(dict) as CFDictionaryRef?
       }

       companion object {
           private const val SERVICE = "com.foxugly.trainingmanager_app"
           private const val KEY_ACCESS = "tm_access_token"
           private const val KEY_REFRESH = "tm_refresh_token"
           private const val KEY_REMEMBER = "tm_remember"
           private const val KEY_LANGUAGE = "tm_ui_language"
       }
   }
   ```

7. Implement `TestFakes.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app

   import com.foxugly.trainingmanager_app.data.storage.TokenStore

   /** In-memory [TokenStore] for tests. */
   internal class FakeTokenStore(
       private var access: String? = null,
       private var refresh: String? = null,
       private var remember: Boolean = false,
   ) : TokenStore {
       var cleared = false
           private set

       override fun getAccessToken() = access
       override fun setAccessToken(token: String?) { access = token }
       override fun getRefreshToken() = refresh
       override fun setRefreshToken(token: String?) { refresh = token }
       override fun getRemember() = remember
       override fun setRemember(value: Boolean) { remember = value }
       override fun clearAuthTokens() { access = null; refresh = null; cleared = true }
   }
   ```

8. Run — expected **pass**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

9. Commit:
   ```
   git commit -m "S1a: TokenStore seam + TokenStorage expect/actual + FakeTokenStore"
   ```

---

## Task 4 — Auth DTOs + exception types

TDD via serialization round-trips (mirrors PushIT's `ModelsTest`). These are the minimal hand-written
models S1a needs; the full surface comes from OpenAPI codegen (deferred).

**Files**
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/api/Models.kt`
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/api/Exceptions.kt`
- create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/data/api/ModelsTest.kt`

**Interfaces**
- Produces:
  - `@Serializable data class TokenObtainRequest(email, password, remember: Boolean = false)`
  - `@Serializable data class TokenPair(access: String, refresh: String)`
  - `@Serializable data class RefreshRequest(refresh: String)`
  - `@Serializable data class RefreshResponse(access: String, refresh: String? = null)`
  - `@Serializable data class UserProfile(id, email, emailConfirmed: Boolean?, language: String?, firstName: String?, lastName: String?)`
  - `class ApiException(statusCode: Int, operation, responseBody) : Exception`
  - `enum class NetworkErrorKind { OFFLINE, TIMEOUT }` + `class NetworkException(kind, message, cause) : Exception`
  - `class ResponseDecodingException(statusCode, operation, responseBody, cause) : Exception`
- Consumes: kotlinx.serialization.

**Steps**

1. Write the failing test `ModelsTest.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.api

   import kotlinx.serialization.json.Json
   import kotlin.test.Test
   import kotlin.test.assertEquals
   import kotlin.test.assertNull
   import kotlin.test.assertTrue

   class ModelsTest {
       private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

       @Test
       fun tokenObtainRequestSerializesRememberAndOmitsNothingExtra() {
           val body = json.encodeToString(
               TokenObtainRequest(email = "a@b.co", password = "pw", remember = true),
           )
           assertTrue(body.contains("\"email\":\"a@b.co\""))
           assertTrue(body.contains("\"remember\":true"))
       }

       @Test
       fun refreshResponseDecodesWithAndWithoutRotatedRefresh() {
           val rotated = json.decodeFromString<RefreshResponse>("""{"access":"x","refresh":"y"}""")
           assertEquals("x", rotated.access)
           assertEquals("y", rotated.refresh)

           val noRotation = json.decodeFromString<RefreshResponse>("""{"access":"x"}""")
           assertNull(noRotation.refresh)
       }

       @Test
       fun userProfileToleratesMissingOptionalFields() {
           val me = json.decodeFromString<UserProfile>(
               """{"id":7,"email":"a@b.co","email_confirmed":true,"language":"fr"}""",
           )
           assertEquals(7, me.id)
           assertEquals("fr", me.language)
           assertNull(me.firstName)
       }
   }
   ```

2. Run — expected **fail to compile**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

3. Implement `Models.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.api

   import kotlinx.serialization.SerialName
   import kotlinx.serialization.Serializable

   // Minimal hand-written auth DTOs for S1a. The full API surface is generated from
   // the OpenAPI schema in a later S1 plan and supersedes anything overlapping here.

   /** POST auth/token/ — SimpleJWT obtain-pair, plus the "stay logged in" flag the
    * backend maps to a 7d vs 30d refresh TTL. */
   @Serializable
   data class TokenObtainRequest(
       val email: String,
       val password: String,
       val remember: Boolean = false,
   )

   /** Response of POST auth/token/. */
   @Serializable
   data class TokenPair(
       val access: String,
       val refresh: String,
   )

   @Serializable
   data class RefreshRequest(
       val refresh: String,
   )

   @Serializable
   data class RefreshResponse(
       val access: String,
       // The backend rotates + blacklists refresh tokens, so each refresh returns a
       // NEW refresh token. Persist it, else the next refresh presents a blacklisted
       // token and the user is ejected. Nullable so a non-rotating backend still
       // deserializes.
       val refresh: String? = null,
   )

   /** GET me/. */
   @Serializable
   data class UserProfile(
       val id: Int,
       val email: String,
       @SerialName("email_confirmed") val emailConfirmed: Boolean? = null,
       val language: String? = null,
       @SerialName("first_name") val firstName: String? = null,
       @SerialName("last_name") val lastName: String? = null,
   )
   ```

4. Implement `Exceptions.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.api

   class ApiException(
       val statusCode: Int,
       operation: String,
       responseBody: String,
   ) : Exception(
       buildString {
           append("API $operation failed with HTTP $statusCode")
           if (responseBody.isNotBlank()) {
               append(": ")
               append(responseBody)
           }
       }
   )

   /** Transport failure kind, so the UI can show a localized message per case. */
   enum class NetworkErrorKind { OFFLINE, TIMEOUT }

   /** A transport-level failure (offline, DNS, timeout) — distinct from an HTTP
    * status error ([ApiException]) so the UI can show "check your connection". */
   class NetworkException(
       val kind: NetworkErrorKind,
       message: String,
       cause: Throwable,
   ) : Exception(message, cause)

   class ResponseDecodingException(
       val statusCode: Int,
       operation: String,
       responseBody: String,
       cause: Throwable,
   ) : Exception(
       buildString {
           append("API ")
           append(operation)
           append(" returned an unexpected response")
           if (responseBody.isNotBlank()) {
               append(": ")
               append(responseBody)
           }
       },
       cause,
   )
   ```

5. Run — expected **pass**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

6. Commit:
   ```
   git commit -m "S1a: auth DTOs + ApiException/NetworkException/ResponseDecodingException"
   ```

---

## Task 5 — `LanguageProvider` + `LanguageInterceptor` (Accept-Language) + `AuthInterceptor` (bearer attach + AUTH_PATHS + refresh)

TDD. Two Ktor client plugins tested directly against `MockEngine` (the 401-refresh-and-replay end of
`AuthInterceptor` is exercised through the API in Task 6; here we prove bearer attach, AUTH_PATHS skip,
and the language header).

**Files**
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/i18n/LanguageProvider.kt`
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/api/LanguageInterceptor.kt`
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/api/AuthInterceptor.kt`
- create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/data/api/InterceptorTest.kt`

**Interfaces**
- Produces:
  - `class LanguageProvider(initialTag: String = "fr") { var activeTag: String }` — the source of truth
    for the `Accept-Language` value (full `LanguageService` deferred).
  - `class LanguageInterceptor(provider: LanguageProvider) { val plugin: ClientPlugin<Unit> }`
  - `class AuthInterceptor(tokenStorage: TokenStore, var onAuthFailure: (() -> Unit)?) { val plugin; suspend fun refreshIfNeeded(client, staleAccessToken): Boolean }`; companion `AUTH_PATHS: List<String>`
- Consumes: `TokenStore` (Task 3), DTOs (Task 4), `AppLogger`.

**Steps**

1. Write the failing test `InterceptorTest.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.api

   import com.foxugly.trainingmanager_app.FakeTokenStore
   import com.foxugly.trainingmanager_app.i18n.LanguageProvider
   import io.ktor.client.HttpClient
   import io.ktor.client.engine.mock.MockEngine
   import io.ktor.client.engine.mock.respond
   import io.ktor.client.request.get
   import io.ktor.client.statement.HttpResponse
   import io.ktor.http.HttpHeaders
   import io.ktor.http.HttpStatusCode
   import kotlinx.coroutines.test.runTest
   import kotlin.test.Test
   import kotlin.test.assertEquals
   import kotlin.test.assertNull

   class InterceptorTest {

       private fun clientWith(
           store: FakeTokenStore,
           provider: LanguageProvider,
           captured: MutableMap<String, String?>,
       ): HttpClient {
           val auth = AuthInterceptor(store)
           val lang = LanguageInterceptor(provider)
           val engine = MockEngine { request ->
               captured["auth"] = request.headers[HttpHeaders.Authorization]
               captured["lang"] = request.headers[HttpHeaders.AcceptLanguage]
               respond("{}", HttpStatusCode.OK)
           }
           return HttpClient(engine) {
               install(auth.plugin)
               install(lang.plugin)
           }
       }

       @Test
       fun attachesBearerAndAcceptLanguageOnNormalCall() = runTest {
           val captured = mutableMapOf<String, String?>()
           val client = clientWith(
               FakeTokenStore(access = "tok"),
               LanguageProvider("nl"),
               captured,
           )
           client.get("https://test/api/v1/me/")
           assertEquals("Bearer tok", captured["auth"])
           assertEquals("nl", captured["lang"])
           client.close()
       }

       @Test
       fun doesNotAttachBearerOnAuthPaths() = runTest {
           val captured = mutableMapOf<String, String?>()
           val client = clientWith(
               FakeTokenStore(access = "tok"),
               LanguageProvider("fr"),
               captured,
           )
           client.get("https://test/api/v1/auth/token/")
           assertNull(captured["auth"])
           // Accept-Language is still sent on auth calls so backend errors localize.
           assertEquals("fr", captured["lang"])
           client.close()
       }
   }
   ```

2. Run — expected **fail to compile**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

3. Implement `LanguageProvider.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.i18n

   /**
    * Source of truth for the active UI language tag sent as `Accept-Language`.
    * S1a ships only this holder (default = fleet default "fr"); the full
    * LanguageService (optimistic switch + PATCH /me/ persistence + 5-locale
    * catalogs) is a later S1 plan.
    */
   class LanguageProvider(initialTag: String = "fr") {
       var activeTag: String = initialTag
   }
   ```

4. Implement `LanguageInterceptor.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.api

   import com.foxugly.trainingmanager_app.i18n.LanguageProvider
   import io.ktor.client.plugins.api.createClientPlugin
   import io.ktor.http.HttpHeaders

   /** Attaches `Accept-Language: <activeTag>` to every request so the backend
    * localizes translatable model fields + error labels. */
   class LanguageInterceptor(private val languageProvider: LanguageProvider) {
       val plugin = createClientPlugin("LanguageInterceptor") {
           onRequest { request, _ ->
               request.headers[HttpHeaders.AcceptLanguage] = languageProvider.activeTag
           }
       }
   }
   ```

5. Implement `AuthInterceptor.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.api

   import com.foxugly.trainingmanager_app.data.storage.TokenStore
   import com.foxugly.trainingmanager_app.diagnostics.AppLogger
   import io.ktor.client.HttpClient
   import io.ktor.client.call.body
   import io.ktor.client.plugins.api.createClientPlugin
   import io.ktor.client.request.post
   import io.ktor.client.request.setBody
   import io.ktor.http.ContentType
   import io.ktor.http.HttpHeaders
   import io.ktor.http.HttpStatusCode
   import io.ktor.http.contentType
   import kotlinx.coroutines.sync.Mutex
   import kotlinx.coroutines.sync.withLock

   class AuthInterceptor(
       private val tokenStorage: TokenStore,
       var onAuthFailure: (() -> Unit)? = null,
   ) {
       private val tag = "TM/AuthInterceptor"
       private val refreshMutex = Mutex()

       val plugin = createClientPlugin("AuthInterceptor") {
           onRequest { request, _ ->
               val path = request.url.encodedPath
               val isAuthPath = AUTH_PATHS.any { path.contains(it) }
               val token = tokenStorage.getAccessToken()
               if (token != null && !isAuthPath) {
                   request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                   AppLogger.debug(tag, "Authorization header attached to $path")
               }
           }
       }

       /**
        * Refresh the access token after a 401, then signal the caller to REPLAY its
        * original request lambda (preserving verb + body; the fresh token is
        * re-attached by [plugin]). Returns true if a usable access token is now in
        * place.
        *
        * Under [refreshMutex] we first check whether a concurrent caller already
        * refreshed (current token differs from the stale one): if so we skip a
        * redundant — and, with refresh-token rotation, failure-prone — second
        * auth/token/refresh/ round-trip.
        */
       suspend fun refreshIfNeeded(client: HttpClient, staleAccessToken: String?): Boolean =
           refreshMutex.withLock {
               val current = tokenStorage.getAccessToken()
               if (current != null && current != staleAccessToken) {
                   AppLogger.info(tag, "Access token already refreshed by a concurrent call; reusing it")
                   return@withLock true
               }
               val refreshToken = tokenStorage.getRefreshToken() ?: run {
                   AppLogger.warn(tag, "Unauthorized response and no refresh token available")
                   onAuthFailure?.invoke()
                   return@withLock false
               }
               try {
                   AppLogger.info(tag, "Refreshing access token after HTTP 401")
                   val refreshResponse = client.post("auth/token/refresh/") {
                       contentType(ContentType.Application.Json)
                       setBody(RefreshRequest(refreshToken))
                   }
                   if (refreshResponse.status == HttpStatusCode.OK) {
                       val body = refreshResponse.body<RefreshResponse>()
                       tokenStorage.setAccessToken(body.access)
                       // Persist the rotated refresh token (ROTATE_REFRESH_TOKENS +
                       // BLACKLIST_AFTER_ROTATION), else the next refresh sends a
                       // blacklisted token and ejects the user right after login.
                       body.refresh?.let { tokenStorage.setRefreshToken(it) }
                       AppLogger.info(tag, "Access token refresh succeeded")
                       true
                   } else {
                       AppLogger.warn(tag, "Access token refresh failed with HTTP ${refreshResponse.status.value}")
                       tokenStorage.clearAuthTokens()
                       onAuthFailure?.invoke()
                       false
                   }
               } catch (e: Exception) {
                   AppLogger.error(tag, "Access token refresh threw an exception", e)
                   tokenStorage.clearAuthTokens()
                   onAuthFailure?.invoke()
                   false
               }
           }

       companion object {
           // Paths that NEVER receive a bearer and are NEVER refreshed: the token
           // lifecycle + unauthenticated onboarding endpoints (S1 spec §3).
           val AUTH_PATHS: List<String> = listOf(
               "auth/token/",
               "auth/token/refresh/",
               "auth/register/",
               "auth/email/confirm/",
               "auth/email/resend/",
               "auth/password/reset/",
               "auth/password/reset/confirm/",
               "auth/magic-link/request/",
               "auth/magic-link/exchange/",
               "invitations/",
           )
       }
   }
   ```

6. Run — expected **pass**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

7. Commit:
   ```
   git commit -m "S1a: LanguageProvider + LanguageInterceptor + AuthInterceptor (bearer/AUTH_PATHS/refresh)"
   ```

---

## Task 6 — `TrainingManagerApi` (Ktor client config, `apiCall`, 401→refresh→replay, auth endpoints)

TDD via `MockEngine`. This is the core contract: replay the original POST with body + fresh token
after a 401, persist the rotated refresh token, fire `onAuthFailure` + clear tokens when refresh fails,
and surface `NetworkException` on transport failure.

**Files**
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/api/TrainingManagerApi.kt`
- create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/data/api/TrainingManagerApiAuthTest.kt`

**Interfaces**
- Produces:
  - `class TrainingManagerApi(tokenStorage: TokenStore, baseUrl: String = "https://tm-api.foxugly.com/api/v1/", enableLogging: Boolean = false, languageProvider: LanguageProvider = LanguageProvider(), engine: HttpClientEngine? = null) : AutoCloseable`
    with `var onAuthFailure: (() -> Unit)?` and suspend funcs:
    `login(TokenObtainRequest): Result<TokenPair>`, `refresh(RefreshRequest): Result<RefreshResponse>`,
    `logout(refreshToken: String): Result<Unit>`, `getMe(): Result<UserProfile>`,
    `register(body: Map<String,String?>): Result<Unit>` *(thin stub; real register DTO comes from codegen)*.
- Consumes: `AuthInterceptor`, `LanguageInterceptor`, `TokenStore`, DTOs, exceptions.

**Steps**

1. Write the failing test `TrainingManagerApiAuthTest.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.api

   import com.foxugly.trainingmanager_app.FakeTokenStore
   import io.ktor.client.engine.mock.MockEngine
   import io.ktor.client.engine.mock.respond
   import io.ktor.client.engine.mock.respondError
   import io.ktor.client.engine.mock.toByteArray
   import io.ktor.http.HttpHeaders
   import io.ktor.http.HttpMethod
   import io.ktor.http.HttpStatusCode
   import io.ktor.http.headersOf
   import io.ktor.utils.io.errors.IOException
   import kotlinx.coroutines.test.runTest
   import kotlin.test.Test
   import kotlin.test.assertEquals
   import kotlin.test.assertTrue

   private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")

   class TrainingManagerApiAuthTest {

       @Test
       fun replaysOriginalPostWithBodyAndFreshTokenAfter401() = runTest {
           val store = FakeTokenStore(access = "stale", refresh = "refresh-1")
           val seen = mutableListOf<Triple<String, HttpMethod, String>>()
           val engine = MockEngine { request ->
               val body = String(request.body.toByteArray())
               seen += Triple(request.url.encodedPath, request.method, body)
               when {
                   request.url.encodedPath.endsWith("/auth/token/refresh/") ->
                       respond("""{"access":"fresh","refresh":"refresh-2"}""", HttpStatusCode.OK, jsonHeader)
                   request.headers[HttpHeaders.Authorization] == "Bearer stale" ->
                       respond("", HttpStatusCode.Unauthorized)
                   else ->
                       respond("""{"id":1,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
               }
           }
           val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)

           val result = api.getMe()

           assertTrue(result.isSuccess, "getMe should succeed after refresh: ${result.exceptionOrNull()}")
           assertEquals("fresh", store.getAccessToken())
           assertEquals("refresh-2", store.getRefreshToken(), "rotated refresh token must be persisted")
           val meCalls = seen.filter { it.first.endsWith("/me/") }
           assertEquals(2, meCalls.size, "me should be attempted then replayed")
           assertTrue(meCalls.all { it.second == HttpMethod.Get }, "replay must keep the original verb")
           api.close()
       }

       @Test
       fun signalsAuthFailureAndClearsTokensWhenRefreshFails() = runTest {
           val store = FakeTokenStore(access = "stale", refresh = "bad")
           var authFailed = false
           val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
           val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)
           api.onAuthFailure = { authFailed = true }

           val result = api.getMe()

           assertTrue(result.isFailure)
           assertTrue(authFailed, "onAuthFailure must fire when refresh fails")
           assertTrue(store.cleared, "tokens must be cleared on refresh failure")
           api.close()
       }

       @Test
       fun surfacesNetworkExceptionOnTransportFailure() = runTest {
           val store = FakeTokenStore(access = "tok", refresh = "r")
           val engine = MockEngine { throw IOException("connection reset") }
           val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)

           val result = api.getMe()

           assertTrue(result.isFailure)
           assertTrue(
               result.exceptionOrNull() is NetworkException,
               "transport failure must surface as NetworkException, was ${result.exceptionOrNull()}",
           )
           api.close()
       }

       @Test
       fun mapsHttpErrorToApiException() = runTest {
           val store = FakeTokenStore(access = "tok", refresh = "r")
           val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
           val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)

           val result = api.getMe()

           assertTrue(result.exceptionOrNull() is ApiException)
           api.close()
       }
   }
   ```

2. Run — expected **fail to compile**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

3. Implement `TrainingManagerApi.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.api

   import com.foxugly.trainingmanager_app.data.storage.TokenStore
   import com.foxugly.trainingmanager_app.diagnostics.AppLogger
   import com.foxugly.trainingmanager_app.i18n.LanguageProvider
   import io.ktor.client.HttpClient
   import io.ktor.client.HttpClientConfig
   import io.ktor.client.call.body
   import io.ktor.client.engine.HttpClientEngine
   import io.ktor.client.network.sockets.ConnectTimeoutException
   import io.ktor.client.network.sockets.SocketTimeoutException
   import io.ktor.client.plugins.HttpRequestTimeoutException
   import io.ktor.client.plugins.HttpTimeout
   import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
   import io.ktor.client.plugins.defaultRequest
   import io.ktor.client.plugins.logging.LogLevel
   import io.ktor.client.plugins.logging.Logger
   import io.ktor.client.plugins.logging.Logging
   import io.ktor.client.request.get
   import io.ktor.client.request.post
   import io.ktor.client.request.setBody
   import io.ktor.client.request.url
   import io.ktor.client.statement.HttpResponse
   import io.ktor.client.statement.bodyAsText
   import io.ktor.client.statement.request
   import io.ktor.http.ContentType
   import io.ktor.http.HttpHeaders
   import io.ktor.http.HttpStatusCode
   import io.ktor.http.contentType
   import io.ktor.http.isSuccess
   import io.ktor.serialization.kotlinx.json.json
   import io.ktor.utils.io.errors.IOException
   import kotlinx.serialization.SerializationException
   import kotlinx.serialization.json.Json

   class TrainingManagerApi(
       private val tokenStorage: TokenStore,
       baseUrl: String = "https://tm-api.foxugly.com/api/v1/",
       enableLogging: Boolean = false,
       languageProvider: LanguageProvider = LanguageProvider(),
       // Tests inject a MockEngine here; production passes null (default engine).
       engine: HttpClientEngine? = null,
   ) : AutoCloseable {
       private val tag = "TM/Api"

       private val json = Json {
           ignoreUnknownKeys = true
           encodeDefaults = true
           // Omit null properties on encode so we never send `"field": null` to DRF.
           explicitNulls = false
       }

       private val authInterceptor = AuthInterceptor(tokenStorage)
       private val languageInterceptor = LanguageInterceptor(languageProvider)

       private val clientConfig: HttpClientConfig<*>.() -> Unit = {
           install(ContentNegotiation) {
               json(this@TrainingManagerApi.json)
           }
           defaultRequest {
               url(baseUrl)
               contentType(ContentType.Application.Json)
           }
           install(HttpTimeout) {
               requestTimeoutMillis = 30_000
               connectTimeoutMillis = 15_000
               socketTimeoutMillis = 30_000
           }
           install(Logging) {
               logger = object : Logger {
                   override fun log(message: String) {
                       AppLogger.debug(tag, message)
                   }
               }
               level = if (enableLogging) LogLevel.INFO else LogLevel.NONE
               sanitizeHeader { header -> header == HttpHeaders.Authorization }
           }
           install(authInterceptor.plugin)
           install(languageInterceptor.plugin)
       }

       val client = if (engine != null) HttpClient(engine, clientConfig) else HttpClient(clientConfig)

       override fun close() {
           client.close()
       }

       var onAuthFailure: (() -> Unit)?
           get() = authInterceptor.onAuthFailure
           set(value) { authInterceptor.onAuthFailure = value }

       // --- Auth ---
       suspend fun login(request: TokenObtainRequest): Result<TokenPair> = apiCall {
           client.post("auth/token/") { setBody(request) }
       }

       suspend fun refresh(request: RefreshRequest): Result<RefreshResponse> = apiCall {
           client.post("auth/token/refresh/") { setBody(request) }
       }

       suspend fun getMe(): Result<UserProfile> = apiCall {
           client.get("me/")
       }

       suspend fun logout(refreshToken: String): Result<Unit> = runCatching {
           val response = client.post("auth/logout/") { setBody(RefreshRequest(refreshToken)) }
           logResponse("logout", response)
           // A 401 here means the session is already gone server-side; we're tearing
           // it down anyway, so don't turn logout into a scary error. Other non-2xx
           // (5xx) still surface.
           if (!response.status.isSuccess() && response.status != HttpStatusCode.Unauthorized) {
               throw response.toApiException("logout")
           }
       }

       // --- Helpers ---
       // Every authenticated request must go through [apiCall] so a recoverable
       // expired session transparently refreshes instead of hard-failing.
       private suspend inline fun <reified T> apiCall(
           crossinline block: suspend () -> HttpResponse,
       ): Result<T> = runCatching<T> {
           val name = T::class.simpleName ?: "unknown"
           // Token on the request we're about to make — passed to the refresh guard
           // so concurrent 401s don't each re-POST auth/token/refresh/.
           val staleAccessToken = tokenStorage.getAccessToken()
           val response = block()
           logResponse(name, response)
           if (response.status == HttpStatusCode.Unauthorized) {
               AppLogger.warn(tag, "Unauthorized response received, attempting token refresh")
               if (!authInterceptor.refreshIfNeeded(client, staleAccessToken)) {
                   throw response.toApiException("auth $name")
               }
               // Replay the ORIGINAL request: same verb + body, fresh token attached
               // by the AuthInterceptor's onRequest.
               val retried = block()
               logResponse("retry $name", retried)
               if (!retried.status.isSuccess()) {
                   throw retried.toApiException("retry $name")
               }
               retried.decodeBody<T>(name, json)
           } else {
               if (!response.status.isSuccess()) {
                   throw response.toApiException(name)
               }
               response.decodeBody<T>(name, json)
           }
       }.recoverCatching { throwable ->
           // Surface transport failures as NetworkException so the UI can tell
           // "offline / timed out" apart from an HTTP or decoding error.
           throw when (throwable) {
               is HttpRequestTimeoutException,
               is ConnectTimeoutException,
               is SocketTimeoutException ->
                   NetworkException(NetworkErrorKind.TIMEOUT, "The request timed out.", throwable)
               is IOException ->
                   NetworkException(NetworkErrorKind.OFFLINE, "Could not reach the server.", throwable)
               else -> throwable
           }
       }.onFailure {
           AppLogger.error(tag, "API call failed: ${it.message}", it)
       }

       private fun logResponse(operation: String, response: HttpResponse) {
           AppLogger.info(
               tag,
               "$operation ${response.request.method.value} ${response.request.url.encodedPath} -> ${response.status.value}",
           )
       }

       private suspend fun HttpResponse.toApiException(operation: String): ApiException {
           val errorBody = runCatching { bodyAsText() }.getOrDefault("")
           return ApiException(
               statusCode = status.value,
               operation = operation,
               responseBody = errorBody.take(500),
           )
       }
   }

   private suspend inline fun <reified T> HttpResponse.decodeBody(operation: String, json: Json): T {
       val rawBody = bodyAsText()
       // A 204 / empty body for a Unit-returning call is success, not a decode error.
       if (rawBody.isBlank() && T::class == Unit::class) {
           @Suppress("UNCHECKED_CAST")
           return Unit as T
       }
       return try {
           json.decodeFromString<T>(rawBody)
       } catch (cause: SerializationException) {
           throw ResponseDecodingException(
               operation = operation,
               statusCode = status.value,
               responseBody = rawBody.take(500),
               cause = cause,
           )
       }
   }
   ```

4. Run — expected **pass** (all four tests green):
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

5. Commit:
   ```
   git commit -m "S1a: TrainingManagerApi (apiCall + 401 refresh/replay + NetworkException)"
   ```

---

## Task 7 — `AuthRepository` (login/logout/refresh/session helpers)

TDD via `MockEngine`-backed `TrainingManagerApi` + `FakeTokenStore`. Provides the auth-check stub used
by `App()` (Task 10).

**Files**
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/repository/AuthRepository.kt`
- create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/data/repository/AuthRepositoryTest.kt`

**Interfaces**
- Produces:
  - `class AuthRepository(api: TrainingManagerApi, tokenStorage: TokenStore)` with:
    `suspend fun login(email, password, remember): Result<UserProfile>`,
    `suspend fun logout(): Result<Unit>`, `suspend fun getCurrentUser(): Result<UserProfile>`,
    `fun hasRefreshToken(): Boolean`, `fun isAuthenticated(): Boolean`,
    `suspend fun tryRefresh(): Boolean`.
- Consumes: `TrainingManagerApi`, `TokenStore`, DTOs.

**Steps**

1. Write the failing test `AuthRepositoryTest.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.repository

   import com.foxugly.trainingmanager_app.FakeTokenStore
   import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
   import io.ktor.client.engine.mock.MockEngine
   import io.ktor.client.engine.mock.respond
   import io.ktor.http.HttpHeaders
   import io.ktor.http.HttpStatusCode
   import io.ktor.http.headersOf
   import kotlinx.coroutines.test.runTest
   import kotlin.test.Test
   import kotlin.test.assertEquals
   import kotlin.test.assertFalse
   import kotlin.test.assertTrue

   private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")

   class AuthRepositoryTest {

       @Test
       fun loginStoresTokensRememberAndReturnsProfile() = runTest {
           val store = FakeTokenStore()
           val engine = MockEngine { request ->
               when {
                   request.url.encodedPath.endsWith("/auth/token/") ->
                       respond("""{"access":"acc","refresh":"ref"}""", HttpStatusCode.OK, jsonHeader)
                   request.url.encodedPath.endsWith("/me/") ->
                       respond("""{"id":3,"email":"a@b.co","language":"fr"}""", HttpStatusCode.OK, jsonHeader)
                   else -> respond("", HttpStatusCode.NotFound)
               }
           }
           val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)
           val repo = AuthRepository(api, store)

           val result = repo.login("a@b.co", "pw", remember = true)

           assertTrue(result.isSuccess, "${result.exceptionOrNull()}")
           assertEquals(3, result.getOrNull()?.id)
           assertEquals("acc", store.getAccessToken())
           assertEquals("ref", store.getRefreshToken())
           assertTrue(store.getRemember())
           api.close()
       }

       @Test
       fun tryRefreshPersistsRotatedTokenOnSuccess() = runTest {
           val store = FakeTokenStore(refresh = "r1")
           val engine = MockEngine {
               respond("""{"access":"a2","refresh":"r2"}""", HttpStatusCode.OK, jsonHeader)
           }
           val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)
           val repo = AuthRepository(api, store)

           assertTrue(repo.tryRefresh())
           assertEquals("a2", store.getAccessToken())
           assertEquals("r2", store.getRefreshToken())
           api.close()
       }

       @Test
       fun tryRefreshClearsAndReturnsFalseOnFailure() = runTest {
           val store = FakeTokenStore(refresh = "bad")
           val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
           val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)
           val repo = AuthRepository(api, store)

           assertFalse(repo.tryRefresh())
           assertTrue(store.cleared)
           api.close()
       }

       @Test
       fun hasRefreshTokenReflectsStore() = runTest {
           assertFalse(AuthRepository(
               TrainingManagerApi(FakeTokenStore(), baseUrl = "https://test/api/v1/", engine = MockEngine { respond("", HttpStatusCode.OK) }),
               FakeTokenStore(),
           ).hasRefreshToken())
       }
   }
   ```

2. Run — expected **fail to compile**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

3. Implement `AuthRepository.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.data.repository

   import com.foxugly.trainingmanager_app.data.api.RefreshRequest
   import com.foxugly.trainingmanager_app.data.api.TokenObtainRequest
   import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
   import com.foxugly.trainingmanager_app.data.api.UserProfile
   import com.foxugly.trainingmanager_app.data.storage.TokenStore
   import com.foxugly.trainingmanager_app.diagnostics.AppLogger

   class AuthRepository(
       private val api: TrainingManagerApi,
       private val tokenStorage: TokenStore,
   ) {
       private val tag = "TM/AuthRepository"

       /** POST auth/token/ → store tokens + remember → GET me/ → profile. */
       suspend fun login(email: String, password: String, remember: Boolean): Result<UserProfile> {
           // Don't log the email — it's PII and would land in Logcat.
           AppLogger.info(tag, "Login requested (remember=$remember)")
           return api.login(TokenObtainRequest(email, password, remember)).mapCatching { pair ->
               tokenStorage.setAccessToken(pair.access)
               tokenStorage.setRefreshToken(pair.refresh)
               tokenStorage.setRemember(remember)
               api.getMe().getOrThrow()
           }.onFailure {
               AppLogger.error(tag, "Login failed: ${it.message}", it)
           }
       }

       suspend fun logout(): Result<Unit> {
           AppLogger.info(tag, "Logout requested")
           val refreshToken = tokenStorage.getRefreshToken()
           val result = if (refreshToken != null) api.logout(refreshToken) else Result.success(Unit)
           tokenStorage.clearAuthTokens()
           return result
       }

       suspend fun getCurrentUser(): Result<UserProfile> = api.getMe()

       fun isAuthenticated(): Boolean = tokenStorage.getAccessToken() != null

       fun hasRefreshToken(): Boolean = tokenStorage.getRefreshToken() != null

       /** Startup refresh: POST auth/token/refresh/, persisting the rotated token. */
       suspend fun tryRefresh(): Boolean {
           val refreshToken = tokenStorage.getRefreshToken() ?: return false
           AppLogger.info(tag, "Trying startup token refresh")
           return api.refresh(RefreshRequest(refreshToken)).fold(
               onSuccess = { response ->
                   tokenStorage.setAccessToken(response.access)
                   response.refresh?.let { tokenStorage.setRefreshToken(it) }
                   AppLogger.info(tag, "Startup token refresh succeeded")
                   true
               },
               onFailure = {
                   AppLogger.error(tag, "Startup token refresh failed", it)
                   tokenStorage.clearAuthTokens()
                   false
               },
           )
       }
   }
   ```

4. Run — expected **pass**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

5. Commit:
   ```
   git commit -m "S1a: AuthRepository (login/logout/tryRefresh + session helpers)"
   ```

---

## Task 8 — Material 3 emerald theme + dark mode

TDD on the color schemes (exposed as testable vals), then the composable applies them based on
`isSystemInDarkTheme()`. Replaces the placeholder theme from Task 2.

**Files**
- modify `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/theme/Theme.kt`
- create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/theme/ThemeTest.kt`

**Interfaces**
- Produces: `val TmLightColors: ColorScheme`, `val TmDarkColors: ColorScheme`,
  `@Composable fun TrainingManagerTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content)`.
- Consumes: nothing.

**Steps**

1. Write the failing test `ThemeTest.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.ui.theme

   import androidx.compose.ui.graphics.Color
   import kotlin.test.Test
   import kotlin.test.assertEquals
   import kotlin.test.assertNotEquals

   class ThemeTest {
       @Test
       fun lightSchemeUsesEmeraldStrongPrimary() {
           assertEquals(Color(0xFF059669), TmLightColors.primary)
       }

       @Test
       fun darkSchemeDiffersFromLightBackground() {
           assertNotEquals(TmLightColors.background, TmDarkColors.background)
       }
   }
   ```

2. Run — expected **fail to compile** (`TmLightColors`/`TmDarkColors` don't exist; current Theme.kt has
   neither):
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

3. Implement `Theme.kt` (overwrite the placeholder):
   ```kotlin
   package com.foxugly.trainingmanager_app.ui.theme

   import androidx.compose.foundation.isSystemInDarkTheme
   import androidx.compose.material3.ColorScheme
   import androidx.compose.material3.MaterialTheme
   import androidx.compose.material3.darkColorScheme
   import androidx.compose.material3.lightColorScheme
   import androidx.compose.runtime.Composable
   import androidx.compose.ui.graphics.Color

   /**
    * Fleet brand theme — emerald accent over slate neutrals, mirroring the
    * TrainingManager web charte so the native app reads as the same family. Ships
    * with a dark scheme scaffold (parity to be refined alongside the screens).
    */

   // Emerald accent (web --accent / --accent-strong). The strong shade is the
   // button fill — white-on-#059669 clears the contrast bar that #10b981 fails.
   private val EmeraldStrong = Color(0xFF059669)
   private val Emerald = Color(0xFF10B981)
   private val EmeraldContainer = Color(0xFFD1FAE5) // emerald-100
   private val OnEmeraldContainer = Color(0xFF065F46) // emerald-800

   // Slate neutrals.
   private val Slate900 = Color(0xFF0F172A)
   private val Slate800 = Color(0xFF1E293B)
   private val Slate700 = Color(0xFF334155)
   private val Slate500 = Color(0xFF64748B)
   private val Slate300 = Color(0xFFCBD5E1)
   private val Slate200 = Color(0xFFE2E8F0)
   private val Slate100 = Color(0xFFF1F5F9)
   private val Slate50 = Color(0xFFF8FAFC)

   private val Error = Color(0xFFDC2626)

   val TmLightColors: ColorScheme = lightColorScheme(
       primary = EmeraldStrong,
       onPrimary = Color.White,
       primaryContainer = EmeraldContainer,
       onPrimaryContainer = OnEmeraldContainer,
       secondary = Emerald,
       onSecondary = Color.White,
       secondaryContainer = EmeraldContainer,
       onSecondaryContainer = OnEmeraldContainer,
       background = Slate50,
       onBackground = Slate900,
       surface = Color.White,
       onSurface = Slate800,
       surfaceVariant = Slate100,
       onSurfaceVariant = Slate500,
       outline = Slate300,
       outlineVariant = Slate200,
       error = Error,
       onError = Color.White,
       errorContainer = Color(0xFFFEE2E2),
       onErrorContainer = Color(0xFF991B1B),
   )

   val TmDarkColors: ColorScheme = darkColorScheme(
       primary = Emerald,
       onPrimary = Slate900,
       primaryContainer = OnEmeraldContainer,
       onPrimaryContainer = EmeraldContainer,
       secondary = Emerald,
       onSecondary = Slate900,
       secondaryContainer = OnEmeraldContainer,
       onSecondaryContainer = EmeraldContainer,
       background = Slate900,
       onBackground = Slate100,
       surface = Slate800,
       onSurface = Slate100,
       surfaceVariant = Slate700,
       onSurfaceVariant = Slate300,
       outline = Slate500,
       outlineVariant = Slate700,
       error = Color(0xFFF87171),
       onError = Slate900,
       errorContainer = Color(0xFF7F1D1D),
       onErrorContainer = Color(0xFFFEE2E2),
   )

   /** Applies the fleet brand palette, following the system dark-mode setting. */
   @Composable
   fun TrainingManagerTheme(
       useDarkTheme: Boolean = isSystemInDarkTheme(),
       content: @Composable () -> Unit,
   ) {
       MaterialTheme(
           colorScheme = if (useDarkTheme) TmDarkColors else TmLightColors,
           content = content,
       )
   }
   ```

4. Run — expected **pass**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

5. Commit:
   ```
   git commit -m "S1a: Material 3 emerald theme + dark mode scaffold"
   ```

---

## Task 9 — Koin DI module

TDD: build a `koinApplication` from `appModule` with a `FakeTokenStore` and assert the graph resolves
`TrainingManagerApi` + `AuthRepository`. The module accepts a `TokenStore` (not the expect-class
`TokenStorage`) so it is fully testable in `commonTest`; the platform wraps `TokenStorage` before
passing it in (Task 10).

**Files**
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/di/AppModule.kt`
- create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/di/AppModuleTest.kt`

**Interfaces**
- Produces:
  - `fun appModule(tokenStore: TokenStore, apiBaseUrl: String, enableHttpLogging: Boolean): org.koin.core.module.Module`
    providing singletons: the given `TokenStore`, `LanguageProvider`, `TrainingManagerApi`, `AuthRepository`.
- Consumes: `TokenStore`, `LanguageProvider`, `TrainingManagerApi`, `AuthRepository`.

**Steps**

1. Write the failing test `AppModuleTest.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.di

   import com.foxugly.trainingmanager_app.FakeTokenStore
   import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
   import com.foxugly.trainingmanager_app.data.repository.AuthRepository
   import com.foxugly.trainingmanager_app.data.storage.TokenStore
   import org.koin.dsl.koinApplication
   import kotlin.test.Test
   import kotlin.test.assertSame
   import kotlin.test.assertTrue

   class AppModuleTest {
       @Test
       fun graphResolvesApiRepositoryAndSharesTokenStore() {
           val fake = FakeTokenStore(access = "a")
           val app = koinApplication {
               modules(
                   appModule(
                       tokenStore = fake,
                       apiBaseUrl = "https://test/api/v1/",
                       enableHttpLogging = false,
                   ),
               )
           }
           val koin = app.koin

           val api = koin.get<TrainingManagerApi>()
           val repo = koin.get<AuthRepository>()
           assertTrue(api === koin.get<TrainingManagerApi>(), "API must be a singleton")
           assertSame(fake, koin.get<TokenStore>(), "the injected TokenStore must be shared")
           api.close()
           app.close()
       }
   }
   ```

2. Run — expected **fail to compile**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

3. Implement `AppModule.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.di

   import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
   import com.foxugly.trainingmanager_app.data.repository.AuthRepository
   import com.foxugly.trainingmanager_app.data.storage.TokenStore
   import com.foxugly.trainingmanager_app.i18n.LanguageProvider
   import org.koin.core.module.Module
   import org.koin.dsl.module

   /**
    * The shared dependency graph. Takes a [TokenStore] (already wrapped from the
    * platform [com.foxugly.trainingmanager_app.data.storage.TokenStorage]) plus the
    * prod base URL + logging flag from the platform entry point, so the module is
    * fully constructible in commonTest with a fake store.
    */
   fun appModule(
       tokenStore: TokenStore,
       apiBaseUrl: String,
       enableHttpLogging: Boolean,
   ): Module = module {
       single { tokenStore }
       single { LanguageProvider() }
       single { TrainingManagerApi(get(), apiBaseUrl, enableHttpLogging, get()) }
       single { AuthRepository(get(), get()) }
   }
   ```

4. Run — expected **pass**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

5. Commit:
   ```
   git commit -m "S1a: Koin appModule wiring TokenStore -> Api -> AuthRepository"
   ```

---

## Task 10 — `App()` auth-check stub + platform entry-point wiring

TDD the pure routing decision, then wire `App()` (resolving `AuthRepository` from Koin) and the two
platform entry points. Replaces the placeholders from Task 2.

**Files**
- create `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/navigation/StartupRoute.kt`
- create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/navigation/StartupRouteTest.kt`
- modify `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/App.kt`
- modify `composeApp/src/iosMain/kotlin/com/foxugly/trainingmanager_app/MainViewController.kt`
- modify `androidApp/src/main/kotlin/com/foxugly/trainingmanager_app/MainActivity.kt`

**Interfaces**
- Produces:
  - `enum class StartupRoute { Loading, Authenticated, Unauthenticated }`
  - `fun startupRoute(hasRefreshToken: Boolean, refreshSucceeded: Boolean): StartupRoute`
  - `@Composable fun App()` — resolves `AuthRepository` via Koin, runs bootstrap, shows a placeholder
    per route.
  - `fun MainViewController(): UIViewController` — starts Koin (prod base URL, no debug logging) +
    hosts `App()`.
  - `MainActivity` — starts Koin (prod base URL, `BuildConfig.DEBUG` logging) + hosts `App()`.
- Consumes: `AuthRepository`, `appModule`, `TokenStorageStore`, `TokenStorage`, theme.

**Steps**

1. Write the failing test `StartupRouteTest.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.navigation

   import kotlin.test.Test
   import kotlin.test.assertEquals

   class StartupRouteTest {
       @Test
       fun noRefreshTokenGoesUnauthenticated() {
           assertEquals(
               StartupRoute.Unauthenticated,
               startupRoute(hasRefreshToken = false, refreshSucceeded = false),
           )
       }

       @Test
       fun refreshTokenAndSuccessfulRefreshGoesAuthenticated() {
           assertEquals(
               StartupRoute.Authenticated,
               startupRoute(hasRefreshToken = true, refreshSucceeded = true),
           )
       }

       @Test
       fun refreshTokenButFailedRefreshGoesUnauthenticated() {
           assertEquals(
               StartupRoute.Unauthenticated,
               startupRoute(hasRefreshToken = true, refreshSucceeded = false),
           )
       }
   }
   ```

2. Run — expected **fail to compile**:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   ```

3. Implement `StartupRoute.kt`:
   ```kotlin
   package com.foxugly.trainingmanager_app.navigation

   /** Where the app lands after the startup auth check. The full route graph
    * (Navigation Compose) is a later S1 plan; S1a only needs this three-way stub. */
   enum class StartupRoute { Loading, Authenticated, Unauthenticated }

   /**
    * Pure bootstrap decision (S1 spec §6): no refresh token → Unauthenticated;
    * refresh token + successful refresh → Authenticated; otherwise (refresh failed)
    * → Unauthenticated. Kept side-effect-free so it's unit-testable on the JVM host.
    */
   fun startupRoute(hasRefreshToken: Boolean, refreshSucceeded: Boolean): StartupRoute =
       if (hasRefreshToken && refreshSucceeded) StartupRoute.Authenticated
       else StartupRoute.Unauthenticated
   ```

4. Implement `App.kt` (overwrite the placeholder):
   ```kotlin
   package com.foxugly.trainingmanager_app

   import androidx.compose.foundation.layout.Box
   import androidx.compose.foundation.layout.fillMaxSize
   import androidx.compose.material3.CircularProgressIndicator
   import androidx.compose.material3.Text
   import androidx.compose.runtime.Composable
   import androidx.compose.runtime.LaunchedEffect
   import androidx.compose.runtime.getValue
   import androidx.compose.runtime.mutableStateOf
   import androidx.compose.runtime.remember
   import androidx.compose.runtime.setValue
   import androidx.compose.ui.Alignment
   import androidx.compose.ui.Modifier
   import com.foxugly.trainingmanager_app.data.repository.AuthRepository
   import com.foxugly.trainingmanager_app.navigation.StartupRoute
   import com.foxugly.trainingmanager_app.navigation.startupRoute
   import com.foxugly.trainingmanager_app.ui.theme.TrainingManagerTheme
   import org.koin.compose.koinInject

   /**
    * Root composable + startup auth-check stub. Runs the bootstrap once, then shows
    * a placeholder per [StartupRoute]. The real screens + Navigation Compose route
    * graph replace the placeholders in later S1 plans.
    */
   @Composable
   fun App(authRepository: AuthRepository = koinInject()) {
       var route by remember { mutableStateOf(StartupRoute.Loading) }

       LaunchedEffect(authRepository) {
           val hasRefresh = authRepository.hasRefreshToken()
           val refreshed = hasRefresh && authRepository.tryRefresh()
           route = startupRoute(hasRefresh, refreshed)
       }

       TrainingManagerTheme {
           Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
               when (route) {
                   StartupRoute.Loading -> CircularProgressIndicator()
                   StartupRoute.Authenticated -> Text("TrainingManager — signed in (placeholder home)")
                   StartupRoute.Unauthenticated -> Text("TrainingManager — sign in (placeholder login)")
               }
           }
       }
   }
   ```

5. Implement `MainViewController.kt` (overwrite): start Koin once (iOS prod entry — no debug logging),
   then host `App()`:
   ```kotlin
   package com.foxugly.trainingmanager_app

   import androidx.compose.ui.window.ComposeUIViewController
   import com.foxugly.trainingmanager_app.data.storage.TokenStorage
   import com.foxugly.trainingmanager_app.data.storage.TokenStorageStore
   import com.foxugly.trainingmanager_app.di.appModule
   import org.koin.core.context.startKoin
   import org.koin.core.context.stopKoin
   import org.koin.dsl.KoinAppDeclaration

   private const val PROD_API_BASE_URL = "https://tm-api.foxugly.com/api/v1/"

   fun MainViewController() = ComposeUIViewController(configure = { initKoinIfNeeded() }) {
       App()
   }

   // ComposeUIViewController may be created more than once; start Koin only once.
   private var koinStarted = false
   private fun initKoinIfNeeded() {
       if (koinStarted) return
       startKoin(iosKoinDeclaration())
       koinStarted = true
   }

   private fun iosKoinDeclaration(): KoinAppDeclaration = {
       modules(
           appModule(
               tokenStore = TokenStorageStore(TokenStorage()),
               apiBaseUrl = PROD_API_BASE_URL,
               // No BuildConfig on iOS; release builds stay quiet. The Xcode debug
               // scheme can flip this later via a build setting if needed.
               enableHttpLogging = false,
           ),
       )
   }

   @Suppress("unused")
   fun stopKoinForTesting() = stopKoin()
   ```

6. Implement `MainActivity.kt` (overwrite Task 2's minimal host): start Koin with prod URL +
   `BuildConfig.DEBUG` logging, then host `App()`:
   ```kotlin
   package com.foxugly.trainingmanager_app

   import android.os.Bundle
   import androidx.activity.ComponentActivity
   import androidx.activity.compose.setContent
   import androidx.activity.enableEdgeToEdge
   import com.foxugly.trainingmanager_app.data.storage.TokenStorage
   import com.foxugly.trainingmanager_app.data.storage.TokenStorageStore
   import com.foxugly.trainingmanager_app.di.appModule
   import com.foxugly.trainingmanager_app.diagnostics.AppLogger
   import org.koin.core.context.GlobalContext

   class MainActivity : ComponentActivity() {
       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)
           enableEdgeToEdge()

           // Start Koin once for the process. The graph is prod-only: fixed base URL,
           // HTTP logging only in debug builds.
           if (GlobalContext.getOrNull() == null) {
               org.koin.core.context.startKoin {
                   modules(
                       appModule(
                           tokenStore = TokenStorageStore(TokenStorage(applicationContext)),
                           apiBaseUrl = PROD_API_BASE_URL,
                           enableHttpLogging = BuildConfig.DEBUG,
                       ),
                   )
               }
               AppLogger.info(TAG, "Koin graph started")
           }

           setContent { App() }
       }

       companion object {
           private const val TAG = "TM/MainActivity"
           // Both release and debug builds talk to prod; debug only adds HTTP logging.
           private const val PROD_API_BASE_URL = "https://tm-api.foxugly.com/api/v1/"
       }
   }
   ```

7. Run the host tests (covers `StartupRouteTest` + every prior suite) and assemble the app:
   ```
   .\gradlew.bat :composeApp:testAndroidHostTest
   .\gradlew.bat :androidApp:assembleDebug
   ```
   Expected: both `BUILD SUCCESSFUL`; all tests green.

8. Commit:
   ```
   git commit -m "S1a: App() auth-check stub + Koin-wired MainActivity/MainViewController"
   ```

---

## Self-Review

### Spec coverage (Stage-1a items ↔ tasks)

| S1a scope item | Task(s) |
|---|---|
| 1. KMP scaffold (settings, gradle.properties, root build, catalog, wrapper, module builds, iosApp host, package dirs) | Task 1 (wrapper/settings/catalog), Task 2 (module builds, hosts, iosApp, manifest/res) |
| 2. Ktor client + AuthInterceptor (bearer except AUTH_PATHS; single-flight 401 refresh under mutex + concurrent-rotation guard; replay; onAuthFailure) + apiCall/Result + ApiException vs NetworkException + languageInterceptor | Task 4 (exceptions), Task 5 (AuthInterceptor + AUTH_PATHS + languageInterceptor), Task 6 (client + apiCall + refresh/replay) |
| 3. TokenStorage expect/actual (Android EncryptedSharedPreferences, iOS Keychain/NSUserDefaults via multiplatform-settings note) storing access+refresh+remember; TokenStore seam | Task 3 |
| 4. Koin DI module + minimal App() (auth-check stub) + Android MainActivity (prod base URL + BuildConfig.DEBUG logging) + iOS MainViewController host | Task 9 (Koin), Task 10 (App + entry points); Task 7 (AuthRepository powers the auth check) |
| 5. Material 3 theme (emerald + dark mode scaffold) | Task 8 |
| 6. commonTest (host/JVM) fake TokenStore + Ktor MockEngine proving bearer attach, refresh-once-on-401-and-replay, NetworkException on transport failure | Task 3 (FakeTokenStore), Task 5 (bearer attach + AUTH_PATHS + Accept-Language), Task 6 (refresh+replay, rotated refresh persisted, NetworkException, ApiException) |

Deferred items (codegen, nav route graph/deep links/bootstrap screens, auth/onboarding screens,
profile/change-password, i18n 5-locale catalogs + LanguageService) are listed in **Out of Scope** and
not implemented; the two open decisions (API host, Turnstile) are flagged there.

### Placeholder scan

No `TBD`, `add error handling`, or `similar to Task N` markers. Every file shows complete content. The
word "placeholder" appears only as deliberate UI stub text / comments describing intentionally-minimal
S1a pieces (the `App()` route labels, the Task 2 plain theme that Task 8 overwrites), each with an
explicit note about what replaces it and when.

### Type-consistency check across tasks

- `TokenStore` interface (Task 3) is the single seam consumed by `AuthInterceptor` (Task 5),
  `TrainingManagerApi` (Task 6), `AuthRepository` (Task 7), `appModule` (Task 9). `FakeTokenStore`
  (Task 3) and `TokenStorageStore` (Task 3) both implement exactly its 7 members
  (access/refresh/remember get+set + `clearAuthTokens`). The expect `TokenStorage` adds
  `getLanguage`/`setLanguage` (used by `LanguageProvider` seeding later) — superset, consistent.
- `RefreshResponse(access, refresh: String?)` (Task 4) is produced by the MockEngine bodies and
  consumed identically in `AuthInterceptor.refreshIfNeeded` (Task 5) and `AuthRepository.tryRefresh`
  (Task 7) — both persist the rotated `refresh` when non-null. `RefreshRequest(refresh)` posted to
  `auth/token/refresh/` in both places — same path constant.
- `TrainingManagerApi` constructor signature `(TokenStore, baseUrl, enableLogging, LanguageProvider,
  engine?)` (Task 6) matches every call site: tests pass `(store, baseUrl=, engine=)` using defaults
  for logging + language; `appModule` calls `TrainingManagerApi(get(), apiBaseUrl, enableHttpLogging,
  get())` resolving `TokenStore` + `LanguageProvider` (both provided in the module). `engine` defaults
  to null in production.
- `LanguageProvider` (Task 5) is consumed by `LanguageInterceptor` (Task 5) and provided as a Koin
  singleton (Task 9); its `activeTag` is the only mutable field — read in `onRequest`.
- `AuthInterceptor` constructor `(TokenStore, onAuthFailure)` (Task 5) — `TrainingManagerApi` (Task 6)
  constructs it with one arg and bridges `onAuthFailure` via a property delegate; `InterceptorTest`
  (Task 5) constructs it with one arg. Consistent.
- `startupRoute(Boolean, Boolean): StartupRoute` (Task 10) matches its test and its single call site
  in `App()`. `AuthRepository.hasRefreshToken()/tryRefresh()` (Task 7) are the exact inputs `App()`
  feeds it.
- `appModule(tokenStore, apiBaseUrl, enableHttpLogging)` (Task 9) is called with identical argument
  names/types from `MainActivity` (Android, `BuildConfig.DEBUG`) and `MainViewController` (iOS,
  `false`) in Task 10, both wrapping `TokenStorage` via `TokenStorageStore`.
- Test task is `.\gradlew.bat :composeApp:testAndroidHostTest` in every TDD task; build checkpoints use
  `:androidApp:assembleDebug`. No iOS target tasks anywhere.
