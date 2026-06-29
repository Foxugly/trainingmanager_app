# Deep-link activation — ops prerequisite

**Date:** 2026-06-29 · **Context:** S1c-a shipped the in-app deep-link plumbing. HTTPS App Links / Universal Links require hosted association files + the release signing identity, which are **ops-external** and not provisioned by the app build. Until these are in place, **only the custom scheme `trainingmanager://` triggers the app**; real e-mail links (`https://tm.foxugly.com/...`) open the browser.

## What the app already does
- Android `MainActivity` reads `intent.data` (launch + `onNewIntent`, `singleTop`) → `parseDeepLink` → `App(deepLink=…)`.
- iOS `ContentView.onOpenURL` → `MainViewControllerKt.handleDeepLink(uri:)` → `App(deepLink=…)`.
- Custom scheme `trainingmanager` registered: Android manifest `<data android:scheme="trainingmanager"/>`; iOS `CFBundleURLTypes`.
- HTTPS App Link intent-filter present (Android, `autoVerify=true`, host `tm.foxugly.com`, `pathPrefix=/auth/magic-link/`).

## Ops tasks to activate HTTPS links (per environment)

### Android App Links
Host at **`https://tm.foxugly.com/.well-known/assetlinks.json`**:
```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.foxugly.trainingmanager_app",
    "sha256_cert_fingerprints": ["<RELEASE_KEYSTORE_SHA256>"]
  }
}]
```
- `<RELEASE_KEYSTORE_SHA256>`: from the release keystore — `keytool -list -v -keystore <release.keystore> -alias <alias>` (SHA-256). Add the Play App Signing cert SHA-256 too once on Play.
- Served with `Content-Type: application/json`, no redirect, reachable anonymously.

### iOS Universal Links
- Host **`https://tm.foxugly.com/.well-known/apple-app-site-association`** (no extension, `application/json`):
```json
{ "applinks": { "details": [ { "appID": "<TEAMID>.com.foxugly.trainingmanager_app",
  "paths": [ "/auth/magic-link/*" ] } ] } }
```
- Add the **Associated Domains** entitlement to the iOS app target: `applinks:tm.foxugly.com` (needs an `.entitlements` file — not yet wired).

## Paths to cover (extend as later slices land)
- `/auth/magic-link/*` — **S1c-a (now)**
- `/auth/confirm-email/*`, `/auth/reset-password/*` — S1c-b
- `/invitation/*` — S1c-c

Update both `assetlinks.json`/AASA `paths`, the Android `pathPrefix` intent-filters, and `parseDeepLink` together when adding a flow.

## Manual QA without hosted files (custom scheme)
```
adb shell am start -W -a android.intent.action.VIEW \
  -d "trainingmanager://app/auth/magic-link/TESTTOKEN" com.foxugly.trainingmanager_app
```
Expected: app opens the magic-link exchange screen and shows "Lien invalide" (TESTTOKEN rejected by backend).
