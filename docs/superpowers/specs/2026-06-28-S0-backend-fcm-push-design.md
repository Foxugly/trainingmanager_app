# S0 — Backend FCM Push Infrastructure

**Target repo:** `trainingmanager_server` (Django/DRF)
**Date:** 2026-06-28 · **Status:** Draft for review · **Depends on:** —
**Reference template:** `PushIT_server` (`devices/`, `notifications/push.py`) — pattern only, simplified.

---

## 1. Why

The mobile app needs push (new session, reminder, new team message, RSVP nudge, etc.).
`trainingmanager_server` today has **only in-app notifications**: a `Notification` model
written through one service entry point, `notifications/services.py::notify(...)`. There
is **no FCM/Firebase**, no device registry, no push send. S0 adds exactly that, hooking
into the existing `notify()` so every in-app notification can also become a push.

## 2. Scope

**In:** device registry (FCM token per authenticated user); register/refresh/unregister
endpoints; Firebase Admin send helper with invalid-token cleanup; a **push channel**
added to `notify()` gated by `NotificationPreference`; OpenAPI schema regen; settings +
secret plumbing; tests.

**Out:** Celery/async pipeline (TM has no Celery — send synchronously in v1, see §6);
PushIT's app-token / QR / device-link / multi-app machinery (not needed — our device
belongs to one authenticated user); rich delivery analytics.

## 3. Key simplification vs PushIT

PushIT's `Device` carries app-token linking because it is a multi-app notification hub.
Here the athlete is a logged-in user, so the device links **directly to `request.user`**.
We keep only the health-tracking fields worth having.

## 4. Data model — `devices/models.py` (new app `devices`)

```python
class DevicePlatform(models.TextChoices):
    ANDROID = "android", _("Android")
    IOS = "ios", _("iOS")

class DeviceTokenStatus(models.TextChoices):
    ACTIVE = "active", _("Active")
    INVALID = "invalid", _("Invalid")
    REVOKED = "revoked", _("Revoked")

class Device(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE,
                             related_name="devices")
    push_token = models.CharField(max_length=512, unique=True)   # FCM registration token
    platform = models.CharField(max_length=20, choices=DevicePlatform.choices)
    status = models.CharField(max_length=20, choices=DeviceTokenStatus.choices,
                              default=DeviceTokenStatus.ACTIVE)
    device_name = models.CharField(max_length=120, blank=True, default="")
    last_seen_at = models.DateTimeField(null=True, blank=True)
    failure_count = models.PositiveIntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        indexes = [models.Index(fields=["user", "status"])]
```

`unique=True` on `push_token`: if a token re-registers under a different user (device
re-used), the register endpoint **reassigns** it to the current user (upsert by token).

## 5. API — under `/api/v1/devices/` (JWT auth, `IsAuthenticated`)

| Endpoint | Method | Body | Response | Behaviour |
|---|---|---|---|---|
| `/devices/register/` | POST | `{push_token, platform, device_name?}` | `{id, created}` | **Upsert by `push_token`**: create or update; (re)bind to `request.user`; set `status=active`, bump `last_seen_at`, reset `failure_count`. |
| `/devices/unregister/` | POST | `{push_token}` | `204` | Soft-revoke (`status=revoked`) the caller's device with that token (called on logout). |
| `/devices/` | GET | — | `[DeviceRead]` | List caller's active devices (debug/settings). |

Serializers: `DeviceRegisterSerializer` (`push_token` min_length 20 / max 512;
`platform` choice; optional `device_name`), `DeviceReadSerializer` (`id, platform,
status, device_name, last_seen_at, created_at`). Idempotent register so the client can
call it on every launch + on FCM token rotation.

## 6. Push send — `notifications/push.py` (new)

Mirror PushIT's `send_push_to_device` (verbatim pattern), with the same exception
hierarchy:

```python
class PushProviderError(Exception): ...
class InvalidPushTokenError(PushProviderError): ...      # UnregisteredError/InvalidArgument → mark device invalid
class TemporaryPushProviderError(PushProviderError): ...  # Unavailable → ignore in v1 (no retry queue)

def send_push_to_device(push_token, title, body, data=None, platform=None) -> str:
    _ensure_fcm_initialized()                # initialize firebase_admin once from settings.FCM_SERVICE_ACCOUNT_PATH
    msg = messaging.Message(
        token=push_token,
        notification=messaging.Notification(title=title, body=body),
        data={k: str(v) for k, v in (data or {}).items()},   # FCM data must be all-strings
        android=messaging.AndroidConfig(priority="high"),
    )
    try:
        return messaging.send(msg)
    except (messaging.UnregisteredError, InvalidArgumentError) as e:
        raise InvalidPushTokenError(str(e)) from e
    except UnavailableError as e:
        raise TemporaryPushProviderError(str(e)) from e
    except messaging.FirebaseError as e:
        raise PushProviderError(str(e)) from e
```

**`data` payload contract** (consumed by the app for deep-linking — see S4):
`{"type": <NotificationType>, "url": <Notification.url>, "notification_id": <id>}`.

## 7. Hook into `notify()` — the one change in `notifications/services.py`

Today `notify(recipient, type, title, body, url, ...)` creates the in-app `Notification`
when `NotificationPreference.in_app` is on. Add a **push channel** alongside it:

```python
def notify(recipient, type, title, body="", url="", *, actor=None, ...):
    if actor is not None and actor == recipient:
        return                      # never notify the actor of their own action (unchanged)
    prefs = _prefs_for(recipient, type)          # existing helper; defaults in_app/email True
    if prefs.in_app:
        notif = Notification.objects.create(recipient=recipient, type=type, title=title,
                                            body=body, url=url)
        if prefs.push:                            # NEW channel, default True
            _push_to_user_devices(recipient, title, body,
                                  data={"type": type, "url": url, "notification_id": notif.id})
    if prefs.email:
        ...                                        # unchanged
```

```python
def _push_to_user_devices(user, title, body, data):
    for device in user.devices.filter(status=DeviceTokenStatus.ACTIVE):
        try:
            send_push_to_device(device.push_token, title, body, data, device.platform)
            device.last_seen_at = now(); device.failure_count = 0; device.save(...)
        except InvalidPushTokenError:
            device.status = DeviceTokenStatus.INVALID; device.save(...)
        except PushProviderError:
            device.failure_count = F("failure_count") + 1; device.save(...)   # swallow; v1 best-effort
```

Add `push = models.BooleanField(default=True)` to `NotificationPreference` (+ migration);
expose it in the existing `/notifications/preferences/` GET/PATCH so the app can toggle
push per type. This makes **every existing trigger** (`MESSAGE_NEW_TOPIC`,
`MESSAGE_NEW_REPLY`, `NOTE_FOR_ATHLETE`, `PERFORMANCE_LOGGED`, `PB_BEATEN`, …) push-capable
with zero changes at the call sites.

> The two future types `SESSION_REMINDER` and `PLAN_GENERATED` already exist as enum
> values but have no triggers yet. S0 does **not** add the session-reminder cron; it just
> makes them push-capable once a trigger exists. (Flag: a `session_reminder` daily cron is
> a natural follow-up — out of S0 scope.)

## 8. Settings, secrets, deploy

- `requirements.txt`: `firebase-admin==7.4.0` (same as PushIT).
- `settings`: `FCM_SERVICE_ACCOUNT_PATH = env("FCM_SERVICE_ACCOUNT_PATH", default="")`.
  When empty (local/dev/CI), `send_push_to_device` **mocks** (logs + returns a fake id) so
  tests and non-configured envs never hit FCM.
- **Secret:** the service-account JSON delivered via the fleet's SSM→`/run` pattern
  (see global ops): SSM param (e.g. `/trainingmanager/prod/fcm-service-account`) written
  to a tmpfs file referenced by `FCM_SERVICE_ACCOUNT_PATH`. **Dedicated TM Firebase
  project** — never PushIT's (isolation rule).
- `INSTALLED_APPS += ["devices"]`; wire `devices.api_urls` under `/api/v1/devices/`.

## 9. OpenAPI

After the endpoints land, regenerate `openapi-schema.yaml` (the project's drf-spectacular
flow) so the mobile client codegen (S1/S4) picks up `/devices/*` and the `push` preference
field.

## 10. Testing

- `Device` upsert-by-token reassigns user; unregister revokes.
- `send_push_to_device` mocked (no `FCM_SERVICE_ACCOUNT_PATH`) returns fake id; with a
  patched `messaging.send`, `UnregisteredError` → `InvalidPushTokenError` → device marked
  `invalid`.
- `notify()` with `prefs.push` true and one active device calls the sender once with the
  correct `data`; actor==recipient sends nothing; invalid token flips device status.
- Preference PATCH toggles `push`.

## 11. Deliverables checklist

- [ ] `devices` app: model + migration + serializers + views + urls
- [ ] `notifications/push.py` (sender + exceptions + lazy init + mock fallback)
- [ ] `NotificationPreference.push` field + migration + serializer/endpoint exposure
- [ ] `notify()` push channel + `_push_to_user_devices`
- [ ] settings + requirements + SSM secret wiring
- [ ] tests (model, sender, notify hook, preference)
- [ ] OpenAPI regen
