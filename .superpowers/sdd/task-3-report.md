## Task 3 Report — TokenStorage seam: expect/actual + TokenStore interface + remember flag

### Status: DONE

---

### Implemented

**Files created (all in `:composeApp`):**

| File | Purpose |
|---|---|
| `commonMain/.../data/storage/TokenStore.kt` | `TokenStore` interface + `TokenStorageStore` adapter |
| `commonMain/.../data/storage/TokenStorage.kt` | `expect class TokenStorage` (access/refresh/remember/language) |
| `androidMain/.../data/storage/TokenStorage.android.kt` | `actual` using `EncryptedSharedPreferences` (AES256_SIV keys, AES256_GCM values), synchronous `commit()`, error-logged via `AppLogger` |
| `iosMain/.../data/storage/TokenStorage.ios.kt` | `actual` using Keychain for JWT secrets (kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly), NSUserDefaults for remember + language |
| `commonTest/.../TestFakes.kt` | `internal class FakeTokenStore` with `cleared: Boolean` flag |
| `commonTest/.../data/storage/FakeTokenStoreTest.kt` | Two tests: store/read/remember round-trip + clearAuthTokens wipe |

---

### TDD Evidence

**RED** — test written first; compile immediately failed:
```
e: FakeTokenStoreTest.kt:3:40 Unresolved reference 'FakeTokenStore'.
e: FakeTokenStoreTest.kt:14:21 Unresolved reference 'FakeTokenStore'.
e: FakeTokenStoreTest.kt:29:21 Unresolved reference 'FakeTokenStore'.
FAILURE: Build failed with an exception.
Execution failed for task ':composeApp:compileAndroidHostTest'.
BUILD FAILED in 11s
```

**GREEN** — all six production/test files written; test run:
```
> Task :composeApp:compileAndroidMain
> Task :composeApp:bundleAndroidMainClassesToRuntimeJar
> Task :composeApp:bundleAndroidMainClassesToCompileJar
> Task :composeApp:compileAndroidHostTest
> Task :composeApp:testAndroidHostTest

BUILD SUCCESSFUL in 9s
16 actionable tasks: 7 executed, 9 up-to-date
```

Tests: `storesAndReadsBackTokensAndRemember` + `clearAuthTokensWipesAccessAndRefreshAndFlagsCleared` — both pass.

---

### Commit

`fdcac4f` — S1a: TokenStore seam + TokenStorage expect/actual + FakeTokenStore

6 files changed, 309 insertions(+)

---

### Self-review

- Interface surface matches brief exactly (access/refresh/remember + clearAuthTokens; language only on TokenStorage).
- `TokenStorageStore` is pure commonMain — no platform coupling.
- Android actual uses `commit()` (synchronous) not `apply()` — correct for tokens where silent loss = unexpected logout.
- iOS actual separates secrets (Keychain, device-only, not synced) from prefs (NSUserDefaults) — appropriate security split.
- `FakeTokenStore` is `internal` (test-only), has the `cleared` boolean the brief requires, and takes named constructor params so tests can seed state.
- `iosMain` compiles only on macOS/Xcode; cannot be verified from the Windows host — reviewed by inspection against PushIT reference.
- `@OptIn(ExperimentalForeignApi::class)` correctly scopes the cinterop usage on iOS.

---

### Concerns

None blocking. Minor note: `iosMain` Keychain code is review-only (Windows host; macOS build would be needed for a full end-to-end). The approach mirrors the PushIT reference implementation exactly, re-namespaced, which was validated in production, so the risk is low.

---

## Review-fix pass — 2026-06-29

### Changes applied

**1. `SecItemAdd` silent failure fixed** (`iosMain/TokenStorage.ios.kt`):
Captured the `OSStatus` return value of `SecItemAdd`. When not `errSecSuccess`, logs via `AppLogger.error` with the status code and account name. No throw — best-effort like Android's `commit()` path.

**2. `SecItemDelete` suppressed result fixed** (`iosMain/TokenStorage.ios.kt`):
Replaced the `@Suppress("UNUSED_EXPRESSION")` + discarded boolean expression with a real conditional: logs via `AppLogger.error` when the status is neither `errSecSuccess` nor `errSecItemNotFound`. `@Suppress` removed.

**3. TAG constant added** (`iosMain/TokenStorage.ios.kt`):
Added `private const val TAG = "TM/TokenStorage"` to the companion object to support the new log calls (mirrors the Android actual).

**4. `AppLogger` import added** (`iosMain/TokenStorage.ios.kt`):
Added `import com.foxugly.trainingmanager_app.diagnostics.AppLogger` at the top of the iOS file.

**5. KDoc expanded** (`iosMain/TokenStorage.ios.kt`):
Replaced the short comment with a KDoc block explaining why the raw Keychain is used instead of multiplatform-settings (NSUserDefaults is not a secure store for tokens), and noting that the Android actual mirrors the same principle with `EncryptedSharedPreferences`.

**6. One-line note added** (`androidMain/TokenStorage.android.kt`):
Added a comment above the class explaining that `EncryptedSharedPreferences` is chosen over multiplatform-settings because the latter writes plain (unencrypted) SharedPreferences XML on Android.

**7. `TokenStorageStore` marked `internal`** (`commonMain/TokenStore.kt`):
`class` → `internal class`. All callers (DI wiring in `androidMain`/`iosMain`) are within the same `:composeApp` module; `internal` is fully visible to platform source sets in the same module.

**8. New test added** (`commonTest/FakeTokenStoreTest.kt`):
`clearAuthTokensDoesNotWipeRemember` — sets `remember=true`, stores tokens, calls `clearAuthTokens()`, asserts tokens are null and `remember` is still `true`. Validates the contract that `clearAuthTokens` is scoped to the JWT pair only.

### Test run

```
.\gradlew.bat :composeApp:testAndroidHostTest

> Task :composeApp:compileAndroidMain
> Task :composeApp:bundleAndroidMainClassesToRuntimeJar
> Task :composeApp:bundleAndroidMainClassesToCompileJar
> Task :composeApp:compileAndroidHostTest
> Task :composeApp:testAndroidHostTest

BUILD SUCCESSFUL in 5s
16 actionable tasks: 6 executed, 10 up-to-date
```

All three tests pass: `storesAndReadsBackTokensAndRemember`, `clearAuthTokensWipesAccessAndRefreshAndFlagsCleared`, `clearAuthTokensDoesNotWipeRemember`.

iOS changes are not compiled on the Windows host (expected); correctness verified by inspection.
