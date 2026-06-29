# S4 — Push Client + Notifications Inbox (outline)

**Target repo:** `trainingmanager_app` · **Depends on:** **S0 + S1** · **Date:** 2026-06-28
**Status:** Outline — to be deepened when reached.
**Reference:** `PushIT_app` FcmTokenProvider + `PushItFirebaseService` (pattern, re-namespaced).

> Requires S0 (backend `/devices/*` + push send) live, and a **dedicated TM Firebase
> project** (isolation rule — never PushIT's): `google-services.json` (Android),
> `GoogleService-Info.plist` (iOS), APNs key for iOS.

## Client pieces

1. **`FcmTokenProvider` expect/actual** (copy PushIT): Android via Firebase SDK; iOS via
   APNs→FCM token. Observe token rotation.
2. **Device registration**: after bootstrap reaches Home, `POST /devices/register/`
   `{push_token, platform, device_name}`; re-call on token rotation; on **logout**
   `POST /devices/unregister/` then clear tokens.
3. **Receive + handle**:
   - Android: a `FirebaseMessagingService` (mirror `PushItFirebaseService`) builds/show
     notifications when backgrounded; foreground messages update in-app state.
   - iOS: `MainViewController` FCM/APNs delegate handling.
   - **Deep-link from tap**: read the `data` payload `{type, url, notification_id}` (S0
     contract) and route to the matching screen (e.g. `url=/teams/3` → team detail;
     event/topic urls → their screens). Reuse S1's deep-link router.
4. **Notifications inbox** (in-app):
   - List — `GET /notifications/?is_read=` (paginated); unread badge via
     `GET /notifications/unread_count/`.
   - Actions — `POST /notifications/{id}/read/`, `POST /notifications/read_all/`.
   - **Preferences** — `GET/PATCH /notifications/preferences/` exposing per-type
     `in_app` / `email` / **`push`** (the `push` flag added in S0). Toggling here gates
     server-side push.
   - Tapping an item routes via its `url` (same deep-link router).

## Tests

Device register/unregister lifecycle (login/logout/rotation), payload→route mapping,
inbox read/read-all/unread-count, preference toggle round-trip. (FCM token provider is
platform code — host-test the routing/registration logic with a fake provider.)

## Open items

iOS APNs/Apple Developer provisioning (overview §7.4); foreground notification UX
(in-app banner vs silent badge); whether to also surface `SESSION_REMINDER` once a backend
trigger exists (follow-up to S0).
