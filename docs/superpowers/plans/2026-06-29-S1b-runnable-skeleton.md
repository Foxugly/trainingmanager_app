# S1b — Runnable Skeleton (codegen + navigation + bootstrap + Login) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the S1a foundation into a runnable app: an OpenAPI **models-only** codegen pipeline, a Navigation Compose graph with a startup/bootstrap gate, and a real **Login** screen that authenticates against the existing (tested) auth stack and lands on a placeholder **Home** with logout.

**Architecture:** Reuse S1a's hand-written Ktor `TrainingManagerApi` + `AuthInterceptor` + `AuthRepository` verbatim (already tested, single-flight 401 refresh+replay). Add (1) an `org.openapi.generator` Gradle task that emits **only** `@Serializable` kotlinx models from the schema into `build/generated/openapi` (no generated Ktor runtime → no Ktor-3.x compatibility risk; generated services deferred to the slice before S2). (2) Navigation Compose Multiplatform with type-safe `@Serializable` routes, wrapped by a bootstrap gate in `App()` that reuses the pure `startupRoute(...)`. (3) A plain testable `LoginViewModel` (Compose snapshot state, no androidx ViewModel — mirrors PushIT's `SessionViewModel`) wrapping `AuthRepository.login`, consumed by a `LoginScreen` composable; a `HomePlaceholderScreen` with a logout button.

**Tech Stack:** Kotlin 2.3.21, Compose Multiplatform 1.10.3, Ktor 3.1.3, kotlinx-serialization 1.8.1, kotlinx-datetime 0.8.0, Koin 4.1.0, Navigation Compose `2.9.0-alpha13`, openapi-generator `7.21.0`. Tests: kotlin-test + kotlinx-coroutines-test `runTest` + Ktor `MockEngine`, run JVM-host via `:composeApp:testAndroidHostTest`.

## Global Constraints

- **Package:** `com.foxugly.trainingmanager_app` (generated models under `…​.api.generated`).
- **Verify task (every task):** `.\gradlew.bat :composeApp:testAndroidHostTest` — JVM host, Windows-friendly. **Never run iOS targets on Windows.** APK sanity: `.\gradlew.bat :androidApp:assembleDebug`.
- **Toolchain:** JDK 17 Temurin; Android SDK at `C:\Users\Renaud\AppData\Local\Android\Sdk`.
- **Config-cache is ON** (`org.gradle.configuration-cache=true`): every new Gradle task MUST be configuration-cache compatible (no `Project` access at execution time).
- **API base URL:** `https://tm-api.foxugly.com/api/v1/` (confirmed). Endpoints are relative to it.
- **Codegen scope = models only** this slice. Do NOT generate or wire Ktor service classes. Do NOT add `google-services`/Firebase (S4).
- **Auth stack is frozen:** do not modify `TrainingManagerApi`, `AuthInterceptor`, `AuthRepository` behavior; only consume them. Keep the hand-written `UserProfile` DTO for now (migration to the generated `Me` model is S1c/S1d).
- **No Compose UI tests** exist (no `compose-ui-test` dependency). Put all logic in plain testable classes (ViewModels / pure functions); composables stay thin and are validated by `assembleDebug`.
- **Tests:** kotlin-test asserts, `runTest` for suspend, `MockEngine` injected via `TrainingManagerApi(engine = …)`, shared `internal FakeTokenStore`.
- DRY, YAGNI, TDD, frequent commits. `git add` only the exact files each step names.

---

## File Structure

**Created:**
- `openapi/Training_Manager_API.yaml` — committed copy of the backend schema (mirrors the frontend `openapi/` convention; the codegen input).
- `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/navigation/Routes.kt` — `@Serializable` type-safe route objects (`LoginRoute`, `HomeRoute`).
- `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/components/ErrorBanner.kt` — reusable error banner (re-namespaced from PushIT).
- `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/components/PasswordEyeIcons.kt` — hand-built eye `ImageVector`s (re-namespaced from PushIT; avoids material-icons-extended).
- `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginStrings.kt` — minimal FR string holder for the Login screen (full i18n is S1d).
- `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginError.kt` — pure `mapLoginError(...)` function + `LoginErrorText` model.
- `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginViewModel.kt` — plain testable state holder wrapping `AuthRepository.login`.
- `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginScreen.kt` — the Login composable.
- `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/home/HomePlaceholderScreen.kt` — placeholder Home + logout button.
- `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/api/generated/GeneratedModelSmokeTest.kt` — proves a generated model deserializes (codegen pipeline smoke).
- `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginErrorTest.kt` — `mapLoginError` truth table.
- `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginViewModelTest.kt` — login flow over MockEngine.

**Modified:**
- `gradle/libs.versions.toml` — add `openapi-generator` plugin; reference the existing `navigation-compose` library.
- `build.gradle.kts` (root) — declare the `openapi-generator` plugin (apply false).
- `composeApp/build.gradle.kts` — apply openapi plugin, configure `openApiGenerate` (models-only), register generated srcDir, make compile depend on it, add `navigation-compose` to `commonMain`.
- `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/App.kt` — replace placeholder `when` with bootstrap gate + `NavHost`.
- `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/di/AppModule.kt` — add `LoginViewModel` factory.
- `.gitignore` — already updated (Firebase configs ignored); committed in Task 0.

---

## Task 0 — Hygiene: commit the Firebase `.gitignore` guard

**Files:**
- Modify: `.gitignore` (already edited — adds `google-services.json` + `GoogleService-Info.plist`).

- [ ] **Step 1: Confirm the file is ignored**

Run: `git -C . check-ignore -v google-services.json`
Expected: a line citing `.gitignore:…:google-services.json`.

- [ ] **Step 2: Commit**

```bash
git add .gitignore
git commit -m "chore: gitignore Firebase config files (google-services.json / plist)"
```

---

## Task 1 — OpenAPI models-only codegen pipeline

**Files:**
- Create: `openapi/Training_Manager_API.yaml` (copy of `D:\Projects\PycharmProjects\trainingmanager_server\openapi-schema.yaml`)
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`, `composeApp/build.gradle.kts`
- Test: `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/api/generated/GeneratedModelSmokeTest.kt`

**Interfaces:**
- Produces: generated `@Serializable` data classes in package `com.foxugly.trainingmanager_app.api.generated` under `composeApp/build/generated/openapi/src/commonMain/kotlin/…`, registered as a `commonMain` source dir. The `Me` model (fields incl. `id`, `email`, `emailConfirmed`, `firstName`, `lastName`, `language`, `isStaff`, `weeklyRecapOptIn`, `digestEmail`) is generated and available to later slices. **No** Ktor service/infrastructure files are generated.
- Consumes: nothing (root infra).

- [ ] **Step 1: Copy the schema into the repo**

Run (PowerShell):
```powershell
Copy-Item "D:\Projects\PycharmProjects\trainingmanager_server\openapi-schema.yaml" "D:\Projects\IdeaProjects\trainingmanager_app\openapi\Training_Manager_API.yaml"
```
(Create the `openapi\` dir first if absent: `New-Item -ItemType Directory -Force openapi`.)

- [ ] **Step 2: Add the plugin to the version catalog**

In `gradle/libs.versions.toml`, under `[versions]` add:
```toml
openapiGenerator = "7.21.0"
```
Under `[plugins]` add:
```toml
openapiGenerator = { id = "org.openapi.generator", version.ref = "openapiGenerator" }
```

- [ ] **Step 3: Declare the plugin in the root build (apply false)**

In `build.gradle.kts` (root) add to the `plugins {}` block:
```kotlin
    alias(libs.plugins.openapiGenerator) apply false
```

- [ ] **Step 4: Apply + configure codegen in `composeApp/build.gradle.kts`**

Add to the `plugins {}` block:
```kotlin
    alias(libs.plugins.openapiGenerator)
```

After the `kotlin { … }` block (top-level in the file), add the generation task. The two globals `models=""` + `supportingFiles=false` restrict output to model classes only — **no Ktor runtime is emitted**:
```kotlin
val openApiOutDir = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("kotlin")
    library.set("multiplatform") // selects kotlinx.serialization @Serializable models
    inputSpec.set(layout.projectDirectory.dir("../openapi").file("Training_Manager_API.yaml").asFile.path)
    outputDir.set(openApiOutDir.get().asFile.path)
    packageName.set("com.foxugly.trainingmanager_app.api.generated")
    // Models only — do not emit apis, infrastructure, or supporting/runtime files.
    globalProperties.set(
        mapOf(
            "models" to "",
            "modelDocs" to "false",
            "modelTests" to "false",
            "apis" to "false",
            "apiDocs" to "false",
            "apiTests" to "false",
            "supportingFiles" to "false",
        ),
    )
    configOptions.set(
        mapOf(
            "serializationLibrary" to "kotlinx_serialization",
            "dateLibrary" to "kotlinx-datetime",
            "enumPropertyNaming" to "UPPERCASE",
            "sourceFolder" to "src/commonMain/kotlin",
        ),
    )
}

// Register generated models as a commonMain source dir and ensure generation
// runs before any Kotlin compilation. dependsOn on the task name keeps this
// configuration-cache compatible (no Project access at execution time).
kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(openApiOutDir.map { it.dir("src/commonMain/kotlin") })
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(tasks.named("openApiGenerate"))
}
```

- [ ] **Step 5: Run generation and verify it emits models but NO Ktor runtime**

Run: `.\gradlew.bat :composeApp:openApiGenerate`
Expected: `BUILD SUCCESSFUL`. Then inspect:

Run (PowerShell):
```powershell
$g = "composeApp\build\generated\openapi\src\commonMain\kotlin\com\foxugly\trainingmanager_app\api\generated"
Get-ChildItem -Recurse $g -Filter "*.kt" | Measure-Object | Select-Object Count
Test-Path "composeApp\build\generated\openapi\src\commonMain\kotlin\com\foxugly\trainingmanager_app\api\generated\models\Me.kt"
Get-ChildItem -Recurse "composeApp\build\generated\openapi" -Filter "*.kt" | Where-Object { $_.FullName -match "infrastructure|HttpClient|ApiClient" }
```
Expected: a positive `Count` (dozens of model files), `Me.kt` exists (`True`), and the last command returns **nothing** (no infrastructure/Ktor files). If infra files appear, the globals didn't take — re-check Step 4 `globalProperties` before proceeding.

- [ ] **Step 6: Write the failing smoke test**

The generated `Me` lives at `com.foxugly.trainingmanager_app.api.generated.models.Me`. Inspect the actual generated field names from `Me.kt` (Step 5) and use them verbatim. Create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/api/generated/GeneratedModelSmokeTest.kt`:
```kotlin
package com.foxugly.trainingmanager_app.api.generated

import com.foxugly.trainingmanager_app.api.generated.models.Me
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratedModelSmokeTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun generatedMeDeserializesFromBackendJson() {
        val body = """
            {"id":7,"email":"a@b.co","email_confirmed":true,"first_name":"Ann","last_name":"Lee",
             "language":"fr","weekly_recap_opt_in":true,"digest_email":false,"is_staff":false,
             "is_superuser":false,"last_login":null,"date_joined":"2026-01-01T00:00:00Z",
             "team_quota":{"used":0,"limit":3},"calendar_token":"tok","member_id":null}
        """.trimIndent()
        val me = json.decodeFromString<Me>(body)
        assertEquals(7, me.id)
        assertEquals("a@b.co", me.email)
    }
}
```
> If the generated `Me` requires a non-null `team_quota` of a generated `TeamQuotaStatus` type, the JSON above already supplies it; adjust the nested fields to match the generated `TeamQuotaStatus` constructor if compilation complains.

- [ ] **Step 7: Run the test to verify it fails (then passes after generation wiring)**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*GeneratedModelSmokeTest*"`
Expected first run before Step 4 wiring would fail to resolve `Me`; with Step 4 in place the `openApiGenerate` dependency runs and the test PASSES. Confirm `BUILD SUCCESSFUL` and the test is green.

- [ ] **Step 8: Confirm generated output is not committed**

Run: `git -C . status --porcelain | findstr /C:"build/generated"`
Expected: no output (build/ is git-ignored). Generated models are reproduced by the build, not committed.

- [ ] **Step 9: Commit**

```bash
git add openapi/Training_Manager_API.yaml gradle/libs.versions.toml build.gradle.kts composeApp/build.gradle.kts composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/api/generated/GeneratedModelSmokeTest.kt
git commit -m "feat(codegen): OpenAPI models-only Kotlin codegen pipeline + smoke test"
```

---

## Task 2 — Reusable UI components (ErrorBanner + password eye icons)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/components/ErrorBanner.kt`
- Create: `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/components/PasswordEyeIcons.kt`

**Interfaces:**
- Produces: `@Composable fun ErrorBanner(message: String, modifier: Modifier = Modifier)`; `val PasswordVisibleIcon: ImageVector`, `val PasswordHiddenIcon: ImageVector`.
- Consumes: Material 3 theme colors from S1a.

> No unit test (pure composable / vector constants). Validated by `assembleDebug` in Task 5.

- [ ] **Step 1: Create `ErrorBanner.kt`**

```kotlin
package com.foxugly.trainingmanager_app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}
```

- [ ] **Step 2: Create `PasswordEyeIcons.kt`** (hand-built vectors so no material-icons-extended dependency)

```kotlin
package com.foxugly.trainingmanager_app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PasswordVisibleIcon: ImageVector = ImageVector.Builder(
    name = "PasswordVisible", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 4.5f)
        curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
        curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
        curveTo(17f, 19.5f, 21.27f, 16.39f, 23f, 12f)
        curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
        close()
        moveTo(12f, 17f)
        curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
        curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
        curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
        curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
        close()
        moveTo(12f, 9f)
        curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
        curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
        curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
        curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
        close()
    }
}.build()

val PasswordHiddenIcon: ImageVector = ImageVector.Builder(
    name = "PasswordHidden", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 7f)
        curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
        curveTo(17f, 12.65f, 16.87f, 13.26f, 16.64f, 13.83f)
        lineTo(19.56f, 16.75f)
        curveTo(21.07f, 15.49f, 22.26f, 13.86f, 23f, 12f)
        curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
        curveTo(10.6f, 4.5f, 9.26f, 4.75f, 8.01f, 5.2f)
        lineTo(10.17f, 7.36f)
        curveTo(10.74f, 7.13f, 11.35f, 7f, 12f, 7f)
        close()
        moveTo(2f, 4.27f)
        lineTo(4.28f, 6.55f)
        lineTo(4.74f, 7.01f)
        curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1f, 12f)
        curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
        curveTo(13.55f, 19.5f, 15.03f, 19.2f, 16.38f, 18.66f)
        lineTo(16.8f, 19.08f)
        lineTo(19.73f, 22f)
        lineTo(21f, 20.73f)
        lineTo(3.27f, 3f)
        lineTo(2f, 4.27f)
        close()
    }
}.build()
```

- [ ] **Step 3: Verify it compiles**

Run: `.\gradlew.bat :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/components/ErrorBanner.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/components/PasswordEyeIcons.kt
git commit -m "feat(ui): reusable ErrorBanner + password eye icons (re-namespaced from PushIT)"
```

---

## Task 3 — Login error mapping (pure function, TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginStrings.kt`
- Create: `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginError.kt`
- Test: `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginErrorTest.kt`

**Interfaces:**
- Produces: `object LoginStrings` (FR copy); `fun mapLoginError(throwable: Throwable, strings: LoginStrings = LoginStrings): String?` returning a localized message, or `null` for `CancellationException` (caller shows nothing).
- Consumes: `ApiException`, `NetworkException`, `NetworkErrorKind` from S1a.

> Scope note: minimal mapping (401 / 400-email-not-verified / 5xx / offline / timeout). Full `applyServerError` field-map + 429 `Retry-After` countdown are **S1c** (needed by register/forgot, not login).

- [ ] **Step 1: Create `LoginStrings.kt`**

```kotlin
package com.foxugly.trainingmanager_app.ui.login

// Minimal FR copy for S1b. Full 5-locale i18n (Compose resources + LanguageService) is S1d.
object LoginStrings {
    const val title = "Connexion"
    const val email = "E-mail"
    const val password = "Mot de passe"
    const val rememberMe = "Rester connecté"
    const val login = "Se connecter"
    const val showPassword = "Afficher le mot de passe"
    const val hidePassword = "Masquer le mot de passe"
    const val invalidCredentials = "E-mail ou mot de passe incorrect."
    const val emailNotVerified = "Votre e-mail n'est pas encore confirmé."
    const val serverError = "Le serveur a rencontré une erreur. Réessayez plus tard."
    const val networkOffline = "Impossible de joindre le serveur. Vérifiez votre connexion."
    const val networkTimeout = "La requête a expiré. Réessayez."
    const val loginFailed = "La connexion a échoué."
}
```

- [ ] **Step 2: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginErrorTest.kt`:
```kotlin
package com.foxugly.trainingmanager_app.ui.login

import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginErrorTest {
    @Test fun invalidCredentialsOn401() =
        assertEquals(LoginStrings.invalidCredentials, mapLoginError(ApiException(401, "login", "{}")))

    @Test fun emailNotVerifiedOn400WithCode() =
        assertEquals(LoginStrings.emailNotVerified, mapLoginError(ApiException(400, "login", """{"code":"email_not_verified"}""")))

    @Test fun genericOn400WithoutThatCode() =
        assertEquals(LoginStrings.loginFailed, mapLoginError(ApiException(400, "login", """{"detail":"x"}""")))

    @Test fun serverErrorOn500() =
        assertEquals(LoginStrings.serverError, mapLoginError(ApiException(503, "login", "{}")))

    @Test fun offlineOnNetworkOffline() =
        assertEquals(LoginStrings.networkOffline, mapLoginError(NetworkException(NetworkErrorKind.OFFLINE, "x", RuntimeException())))

    @Test fun timeoutOnNetworkTimeout() =
        assertEquals(LoginStrings.networkTimeout, mapLoginError(NetworkException(NetworkErrorKind.TIMEOUT, "x", RuntimeException())))

    @Test fun nullOnCancellation() =
        assertNull(mapLoginError(CancellationException("cancelled")))
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*LoginErrorTest*"`
Expected: FAIL — `mapLoginError` unresolved.

- [ ] **Step 4: Create `LoginError.kt`**

`ApiException` only exposes `statusCode` publicly (the body is baked into `message`), so detect the email-not-verified code from the exception message text.
```kotlin
package com.foxugly.trainingmanager_app.ui.login

import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import kotlinx.coroutines.CancellationException

fun mapLoginError(throwable: Throwable, strings: LoginStrings = LoginStrings): String? = when {
    throwable is CancellationException -> null
    throwable is NetworkException && throwable.kind == NetworkErrorKind.TIMEOUT -> strings.networkTimeout
    throwable is NetworkException -> strings.networkOffline
    throwable is ApiException && throwable.statusCode in 500..599 -> strings.serverError
    throwable is ApiException && throwable.statusCode == 401 -> strings.invalidCredentials
    throwable is ApiException && throwable.statusCode == 400 &&
        throwable.message?.contains("email_not_verified") == true -> strings.emailNotVerified
    throwable is ApiException -> strings.loginFailed
    else -> strings.loginFailed
}
```
> `LoginStrings` is an `object`; the `strings: LoginStrings` parameter type refers to that object's type — keep the default `= LoginStrings`. (If the compiler objects to using the object name as a type, change the signature to `mapLoginError(throwable: Throwable): String?` and reference `LoginStrings.*` directly; tests call it with one arg so either form works.)

- [ ] **Step 5: Run the test to verify it passes**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*LoginErrorTest*"`
Expected: PASS (7 tests).

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginStrings.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginError.kt composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginErrorTest.kt
git commit -m "feat(login): pure login error mapping + FR strings + tests"
```

---

## Task 4 — `LoginViewModel` (plain testable state holder, TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginViewModel.kt`
- Test: `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  class LoginViewModel(private val authRepository: AuthRepository) {
      var email: String            // by mutableStateOf, public set
      var password: String         // by mutableStateOf, public set
      var rememberMe: Boolean      // by mutableStateOf, public set
      var passwordVisible: Boolean // by mutableStateOf, public set
      var isLoading: Boolean       // private set
      var error: String?           // private set
      val canSubmit: Boolean       // derived: !isLoading && email/password non-blank
      suspend fun submit(onSuccess: () -> Unit)
  }
  ```
- Consumes: `AuthRepository.login(email, password, remember): Result<UserProfile>` (S1a), `mapLoginError` (Task 3).

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginViewModelTest.kt`. Mirrors S1a test style: real `AuthRepository` + `TrainingManagerApi(engine = MockEngine)` + `FakeTokenStore`.
```kotlin
package com.foxugly.trainingmanager_app.ui.login

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")

    private fun vm(store: FakeTokenStore, engine: MockEngine): LoginViewModel =
        LoginViewModel(AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store))

    @Test
    fun successInvokesOnSuccessAndStoresTokens() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("auth/token/") ->
                    respond("""{"access":"acc","refresh":"ref"}""", HttpStatusCode.OK, jsonHeader)
                request.url.encodedPath.endsWith("me/") ->
                    respond("""{"id":1,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val sut = vm(store, engine)
        sut.email = "a@b.co"; sut.password = "pw"; sut.rememberMe = true
        var ok = false
        sut.submit { ok = true }
        assertTrue(ok, "onSuccess should fire")
        assertEquals("acc", store.getAccessToken())
        assertEquals("ref", store.getRefreshToken())
        assertTrue(store.getRemember())
        assertFalse(sut.isLoading)
        assertNull(sut.error)
    }

    @Test
    fun failureSetsErrorAndDoesNotCallOnSuccess() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { respond("""{"detail":"bad"}""", HttpStatusCode.Unauthorized, jsonHeader) }
        val sut = vm(store, engine)
        sut.email = "a@b.co"; sut.password = "wrong"
        var ok = false
        sut.submit { ok = true }
        assertFalse(ok)
        assertEquals(LoginStrings.invalidCredentials, sut.error)
        assertFalse(sut.isLoading)
        assertNull(store.getAccessToken())
    }

    @Test
    fun canSubmitReflectsFieldsAndLoading() {
        val sut = LoginViewModel(AuthRepository(TrainingManagerApi(FakeTokenStore(), baseUrl = "https://test/api/v1/", engine = MockEngine { respond("", HttpStatusCode.OK) }), FakeTokenStore()))
        assertFalse(sut.canSubmit)
        sut.email = "a@b.co"
        assertFalse(sut.canSubmit)
        sut.password = "pw"
        assertTrue(sut.canSubmit)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*LoginViewModelTest*"`
Expected: FAIL — `LoginViewModel` unresolved.

- [ ] **Step 3: Create `LoginViewModel.kt`**

```kotlin
package com.foxugly.trainingmanager_app.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

class LoginViewModel(private val authRepository: AuthRepository) {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var rememberMe by mutableStateOf(false)
    var passwordVisible by mutableStateOf(false)

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val canSubmit: Boolean
        get() = !isLoading && email.isNotBlank() && password.isNotBlank()

    fun clearError() { error = null }

    suspend fun submit(onSuccess: () -> Unit) {
        if (isLoading || email.isBlank() || password.isBlank()) return
        isLoading = true
        error = null
        authRepository.login(email.trim(), password, rememberMe).fold(
            onSuccess = { onSuccess() },
            onFailure = { error = mapLoginError(it) },
        )
        isLoading = false
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*LoginViewModelTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginViewModel.kt composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginViewModelTest.kt
git commit -m "feat(login): testable LoginViewModel wrapping AuthRepository.login + tests"
```

---

## Task 5 — `LoginScreen` composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginScreen.kt`

**Interfaces:**
- Produces: `@Composable fun LoginScreen(viewModel: LoginViewModel, onLoginSuccess: () -> Unit)`.
- Consumes: `LoginViewModel` (Task 4), `ErrorBanner` + eye icons (Task 2), `LoginStrings` (Task 3).

> No unit test (composable); validated by `assembleDebug` here.

- [ ] **Step 1: Create `LoginScreen.kt`**

```kotlin
package com.foxugly.trainingmanager_app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import com.foxugly.trainingmanager_app.ui.components.PasswordHiddenIcon
import com.foxugly.trainingmanager_app.ui.components.PasswordVisibleIcon
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(LoginStrings.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        viewModel.error?.let {
            ErrorBanner(it)
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it; viewModel.clearError() },
            label = { Text(LoginStrings.email) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it; viewModel.clearError() },
            label = { Text(LoginStrings.password) },
            visualTransformation = if (viewModel.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { viewModel.passwordVisible = !viewModel.passwordVisible }) {
                    Icon(
                        imageVector = if (viewModel.passwordVisible) PasswordHiddenIcon else PasswordVisibleIcon,
                        contentDescription = if (viewModel.passwordVisible) LoginStrings.hidePassword else LoginStrings.showPassword,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = viewModel.rememberMe,
                onCheckedChange = { viewModel.rememberMe = it },
            )
            Text(LoginStrings.rememberMe)
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { scope.launch { viewModel.submit(onLoginSuccess) } },
            enabled = viewModel.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(LoginStrings.login)
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles (Android target)**

Run: `.\gradlew.bat :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginScreen.kt
git commit -m "feat(login): LoginScreen composable (email/password/remember/eye-toggle/error)"
```

---

## Task 6 — Home placeholder + logout

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/home/HomePlaceholderScreen.kt`

**Interfaces:**
- Produces: `@Composable fun HomePlaceholderScreen(authRepository: AuthRepository, onLoggedOut: () -> Unit)`.
- Consumes: `AuthRepository.logout(): Result<Unit>` (S1a, best-effort, always clears tokens).

> No unit test (composable); `logout` already covered by S1a `AuthRepositoryTest`. Validated by `assembleDebug` in Task 7.

- [ ] **Step 1: Create `HomePlaceholderScreen.kt`**

```kotlin
package com.foxugly.trainingmanager_app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun HomePlaceholderScreen(authRepository: AuthRepository, onLoggedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("TrainingManager", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Accueil (placeholder) — connecté", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { scope.launch { authRepository.logout(); onLoggedOut() } }) {
            Text("Se déconnecter")
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `.\gradlew.bat :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/home/HomePlaceholderScreen.kt
git commit -m "feat(home): placeholder Home screen with best-effort logout"
```

---

## Task 7 — Navigation graph + bootstrap gate + Koin wiring (integration)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/navigation/Routes.kt`
- Modify: `composeApp/build.gradle.kts` (add `navigation-compose` to commonMain)
- Modify: `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/di/AppModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/App.kt`

**Interfaces:**
- Consumes: `startupRoute(hasRefreshToken, refreshSucceeded): StartupRoute` + `AuthRepository` (S1a); `LoginScreen`, `HomePlaceholderScreen`, `LoginViewModel`.
- Produces: `@Serializable object LoginRoute`, `@Serializable object HomeRoute`; a `LoginViewModel` Koin factory; the wired `NavHost` inside `App()`.

- [ ] **Step 1: Add `navigation-compose` to `commonMain`**

In `composeApp/build.gradle.kts`, in `commonMain.dependencies { … }` add:
```kotlin
            implementation(libs.navigation.compose)
```

- [ ] **Step 2: Verify the nav library resolves against Compose MP 1.10.3**

Run: `.\gradlew.bat :composeApp:dependencies --configuration androidDebugRuntimeClasspath` (or `:composeApp:compileDebugKotlinAndroid`)
Expected: `BUILD SUCCESSFUL` — `org.jetbrains.androidx.navigation:navigation-compose:2.9.0-alpha13` resolves with no Compose version conflict. If it fails to resolve / conflicts, bump `navigationCompose` in the catalog to the version aligned with Compose MP 1.10.3 (check the JetBrains compatibility table) and re-run.

- [ ] **Step 3: Create `Routes.kt`**

```kotlin
package com.foxugly.trainingmanager_app.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object HomeRoute
```

- [ ] **Step 4: Add a `LoginViewModel` factory to Koin**

In `composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/di/AppModule.kt`, add inside the `module { … }` (and the matching import):
```kotlin
import com.foxugly.trainingmanager_app.ui.login.LoginViewModel
// …
    factory { LoginViewModel(get()) }
```

- [ ] **Step 5: Rewrite `App.kt` — bootstrap gate + NavHost**

Replace the placeholder `when (route)` body. The bootstrap reuses the existing pure `startupRoute(...)`; while `route == Loading` show a spinner; otherwise mount the `NavHost` with the resolved start destination. Bind `api.onAuthFailure` is owned by `TrainingManagerApi` (injected into `AuthRepository`); for S1b the eject-to-login is handled by navigating to `LoginRoute` after logout and on the unauthenticated start. (Wiring `onAuthFailure` through to the nav controller is S1c, when authenticated screens make real calls.)

```kotlin
package com.foxugly.trainingmanager_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.navigation.HomeRoute
import com.foxugly.trainingmanager_app.navigation.LoginRoute
import com.foxugly.trainingmanager_app.navigation.StartupRoute
import com.foxugly.trainingmanager_app.navigation.startupRoute
import com.foxugly.trainingmanager_app.ui.home.HomePlaceholderScreen
import com.foxugly.trainingmanager_app.ui.login.LoginScreen
import com.foxugly.trainingmanager_app.ui.login.LoginViewModel
import com.foxugly.trainingmanager_app.ui.theme.TrainingManagerTheme
import org.koin.compose.koinInject

@Composable
fun App(authRepository: AuthRepository = koinInject()) {
    var route by remember { mutableStateOf(StartupRoute.Loading) }
    LaunchedEffect(authRepository) {
        val hasRefresh = authRepository.hasRefreshToken()
        val refreshed = hasRefresh && authRepository.tryRefresh()
        route = startupRoute(hasRefresh, refreshed)
    }

    TrainingManagerTheme {
        when (route) {
            StartupRoute.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            else -> {
                val navController = rememberNavController()
                val startDestination: Any =
                    if (route == StartupRoute.Authenticated) HomeRoute else LoginRoute
                NavHost(navController = navController, startDestination = startDestination) {
                    composable<LoginRoute> {
                        val loginViewModel: LoginViewModel = koinInject()
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable<HomeRoute> {
                        HomePlaceholderScreen(
                            authRepository = authRepository,
                            onLoggedOut = {
                                navController.navigate(LoginRoute) {
                                    popUpTo<HomeRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
```
> The type-safe `composable<LoginRoute>` / `navigate(HomeRoute)` / `popUpTo<…>` APIs require Navigation Compose ≥ 2.8 (we have 2.9.0-alpha13) and the `kotlinx-serialization` plugin (already applied). If the alpha's type-safe API differs, fall back to string routes (`NavHost(startDestination = "login")` + `composable("login")`) — functionally identical for two destinations.

- [ ] **Step 6: Verify existing tests still pass (App refactor + new factory)**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest`
Expected: `BUILD SUCCESSFUL` — all S1a tests + Tasks 1/3/4 tests green (the `AppModuleTest` still resolves the graph; `startupRoute` tests unchanged).

- [ ] **Step 7: Build the APK end-to-end**

Run: `.\gradlew.bat :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`; an APK at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`. This is the runnable-skeleton gate: app boots → bootstrap → Login (or Home if a valid refresh token exists) → on success Home → logout returns to Login.

- [ ] **Step 8: Commit**

```bash
git add composeApp/build.gradle.kts composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/navigation/Routes.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/di/AppModule.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/App.kt
git commit -m "feat(nav): Navigation Compose graph + bootstrap gate wiring Login -> Home"
```

---

## Self-Review

**1. Spec coverage (S1 spec ↔ this slice).** S1b is the agreed "runnable skeleton" subset of S1:

| S1 spec item | Task | Notes |
|---|---|---|
| §4 OpenAPI codegen task + generated models | Task 1 | **Models-only** (deviation, agreed): generated services deferred to the slice before S2. |
| §6 Navigation Compose + startup/bootstrap | Task 7 (graph + gate), reuses S1a `startupRoute` | Deep links deferred to S1c. |
| §7 Login screen (`POST auth/token/` then `GET /me/`, remember, error states) | Tasks 3–5 | Uses S1a auth stack. Magic-link toggle, `returnUrl`, resend-on-`email_not_verified` action → S1c. |
| §7 Register / confirm / resend / forgot / reset / magic-link / invitation | — | **S1c** (out of this slice). |
| §8 Profile / change-password / full i18n / LanguageService | — | **S1d** (out of this slice). Minimal FR `LoginStrings` only. |
| §9 Android `MainActivity` / iOS host | unchanged from S1a | Already wire Koin + base URL + App(). |
| §10 commonTest (login error mapping + login flow) | Tasks 1,3,4 | Codegen smoke, `mapLoginError` table, `LoginViewModel` over MockEngine. |
| §9 Turnstile | — | Not on login (no captcha). Register/forgot Turnstile-on-mobile decision → S1c. |

Explicitly deferred (not gaps): generated services, deep links, the 8 non-login auth screens, profile, full i18n, 429/`Retry-After` countdown, full `applyServerError` field map. All belong to S1c/S1d per the agreed decomposition.

**2. Placeholder scan.** Every code step contains complete code; every run step has an exact command + expected output. The only intentional "placeholder" is the **Home screen content** (a labelled placeholder by design — real Home is S2) and the **FR-only `LoginStrings`** (full i18n is S1d) — both are scoped deliverables, not plan gaps. Two steps include a documented fallback (codegen globals in Task 1 Step 5; nav type-safe API vs string routes in Task 7 Step 5) — these are verification-with-contingency, not TODOs.

**3. Type consistency.** `mapLoginError(throwable, strings = LoginStrings): String?` is defined in Task 3 and consumed in Task 4 (`LoginViewModel.submit`). `LoginViewModel(authRepository)` defined in Task 4, factory-registered in Task 7 Step 4, injected in Task 7 Step 5. `AuthRepository.login(email, password, remember)` / `logout()` / `hasRefreshToken()` / `tryRefresh()` are S1a signatures (verified). `startupRoute(hasRefreshToken, refreshSucceeded): StartupRoute` + `StartupRoute.{Loading,Authenticated,Unauthenticated}` are S1a. `ErrorBanner(message, modifier)` + `PasswordVisibleIcon`/`PasswordHiddenIcon` (Task 2) consumed in `LoginScreen` (Task 5). `LoginRoute`/`HomeRoute` (Task 7 Step 3) used in the NavHost (Step 5). `FakeTokenStore` + `TrainingManagerApi(store, baseUrl, engine)` test wiring matches the S1a `AuthRepositoryTest` form. Consistent throughout.
