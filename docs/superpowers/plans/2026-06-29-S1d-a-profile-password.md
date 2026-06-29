# S1d-a — Profile + Change-password Implementation Plan

> Executes inline (autonomous). First S1d slice (full 5-locale i18n + LanguageService = S1d-b).

**Goal:** An authenticated **Profile** screen (view/edit first name, last name, language, weekly-recap + digest-email flags via `GET`/`PATCH /me/`) reachable from Home, plus a **Change-password** screen (`POST auth/password/change/`, no logout).

**Contracts (schema):** `GET me/` → `Me`; `PATCH me/` ← `PatchedMeRequest {first_name?, last_name?, language?, weekly_recap_opt_in?, digest_email?}` → `Me`. `POST auth/password/change/ {current_password, new_password}` → 200 `{detail}`, 400 `code=current_password_invalid` | `code=password_unchanged` | `fields.new_password`, 401. Both authenticated (bearer) — NOT in `AUTH_PATHS`. `LanguageEnum = fr|nl|en|it|es`.

## Global Constraints
Same as prior slices. Verify `:composeApp:testAndroidHostTest`; APK `:androidApp:assembleDebug`. Editing the profile language persists `me.language` + updates `LanguageProvider.activeTag` (Accept-Language) but does NOT live-retranslate the UI — that's S1d-b. FR strings only.

---

## Task 1 — Extend UserProfile + DTOs + API (TDD)
Modify `data/api/Models.kt` — extend `UserProfile`:
```kotlin
@Serializable
data class UserProfile(
    val id: Int,
    val email: String,
    @SerialName("email_confirmed") val emailConfirmed: Boolean? = null,
    val language: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("weekly_recap_opt_in") val weeklyRecapOptIn: Boolean? = null,
    @SerialName("digest_email") val digestEmail: Boolean? = null,
)
```
Create `data/api/ProfileDtos.kt`:
```kotlin
package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** PATCH me/ — only non-null fields are sent (explicitNulls=false). */
@Serializable
data class PatchMeBody(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val language: String? = null,
    @SerialName("weekly_recap_opt_in") val weeklyRecapOptIn: Boolean? = null,
    @SerialName("digest_email") val digestEmail: Boolean? = null,
)

/** POST auth/password/change/ */
@Serializable
data class PasswordChangeBody(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)
```
`TrainingManagerApi` (after `getMe`):
```kotlin
    suspend fun patchMe(body: PatchMeBody): Result<UserProfile> = apiCall {
        client.patch("me/") { setBody(body) }
    }

    suspend fun changePassword(body: PasswordChangeBody): Result<Unit> = apiCall {
        client.post("auth/password/change/") { setBody(body) }
    }
```
Add import `io.ktor.client.request.patch`. Test `data/api/ProfileApiTest.kt`: patchMe sends fields + decodes UserProfile; changePassword success on 200; changePassword 400 → ApiException 400. Verify, commit `feat(s1d-a): patch-me + change-password API + extend UserProfile + tests`.

---

## Task 2 — AuthRepository (TDD)
Add (imports `PasswordChangeBody`, `PatchMeBody`):
```kotlin
    suspend fun updateProfile(body: PatchMeBody): Result<UserProfile> = api.patchMe(body)

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        api.changePassword(PasswordChangeBody(currentPassword, newPassword))
            .onFailure { if (it is CancellationException) throw it }
```
Test `AuthRepositoryProfileTest.kt`: updateProfile returns updated profile; changePassword success; changePassword failure surfaces. Verify, commit `feat(s1d-a): AuthRepository updateProfile + changePassword + tests`.

---

## Task 3 — ViewModels + strings (TDD)
`ui/profile/ProfileStrings.kt` (FR): title, firstName, lastName, language, languageNames map (fr/nl/en/it/es → Français/Nederlands/English/Italiano/Español), weeklyRecap, digestEmail, save, saved, loadFailed, saveFailed, changePasswordCta, emailLabel; change-password: cpTitle, currentPassword, newPassword, confirmPassword, cpSubmit, cpCurrentInvalid, cpUnchanged, cpWeak, mismatch, tooShort, cpSuccess, cpFailed, back.

`ui/profile/ProfileViewModel.kt`:
```kotlin
class ProfileViewModel(authRepository, languageProvider) {
    isLoading(true, private set); loadError; email(ro); firstName; lastName; language("fr");
    weeklyRecapOptIn(true); digestEmail(false); isSaving(private set); saveError; saved(private set)
    val languages = listOf("fr","nl","en","it","es")
    suspend load(): getCurrentUser → fill fields (defaults if null); on failure loadError
    suspend save(): PATCH via updateProfile(PatchMeBody(...)); on success saved=true + languageProvider.activeTag=language; failure saveError
    fun consumeSaved(){saved=false}
}
```
`ui/profile/ChangePasswordViewModel.kt`:
```kotlin
class ChangePasswordViewModel(authRepository) {
    currentPassword; newPassword; confirmPassword; isLoading(private set); error; success(private set)
    canSubmit = !isLoading && currentPassword.isNotBlank() && newPassword.length>=8 && confirmPassword.isNotBlank()
    suspend submit(): local validate (tooShort/mismatch) → changePassword; map 400 by message
      (current_password_invalid→cpCurrentInvalid, password_unchanged→cpUnchanged, else cpWeak/cpFailed); success=true
}
```
Tests: `ProfileViewModelTest` (load fills fields + sets language; save success sets saved + updates LanguageProvider; save failure sets saveError), `ChangePasswordViewModelTest` (mismatch; success; current_password_invalid mapping). Verify, commit `feat(s1d-a): profile + change-password ViewModels + strings + tests`.

---

## Task 4 — Screens (compile)
`ui/profile/ProfileScreen.kt`: loading spinner; loadError → message; form: read-only email, firstName/lastName fields, a language selector (simple `DropdownMenu` over `languages` using `ProfileStrings.languageNames`), two switches (weeklyRecap, digestEmail), Save button (spinner), saved → small confirmation, a "Change password" text button (onChangePassword), and a back/Home button. `ui/profile/ChangePasswordScreen.kt`: three password fields + submit + ErrorBanner(error) + success state + back. Verify compile, commit `feat(s1d-a): profile + change-password screens`.

---

## Task 5 — Wiring (compile + tests + APK)
- `Routes.kt`: `@Serializable object ProfileRoute`, `@Serializable object ChangePasswordRoute`.
- `AppModule.kt`: `factory { ProfileViewModel(get(), get()) }` (LanguageProvider is a single), `factory { ChangePasswordViewModel(get()) }`.
- `HomePlaceholderScreen.kt`: add an `onProfile: () -> Unit` param + a "Profil" button.
- `App.kt`: Home `onProfile` → navigate `ProfileRoute`; `composable<ProfileRoute>` (Profile screen; onChangePassword → navigate ChangePasswordRoute; onBack → popBackStack); `composable<ChangePasswordRoute>` (onBack → popBackStack).
- Verify `:composeApp:testAndroidHostTest` + `:androidApp:assembleDebug`. Commit `feat(s1d-a): routes + factories + Home entry + App wiring`.

## Self-Review
Covers S1 §8 profile (GET/PATCH me: name, language, flags) + change-password (no logout). Deferred: full 5-locale i18n + LanguageService live switch (S1d-b — this slice persists language + sets Accept-Language tag only), account-delete (low priority). Types: `PatchMeBody`/`PasswordChangeBody` → API → repo → VMs → screens → routes. `UserProfile` extended (back-compat: new fields nullable-default). LanguageProvider (S1a single) injected into ProfileViewModel.
