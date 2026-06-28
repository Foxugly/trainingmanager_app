# S2 — Events & Training (outline)

**Target repo:** `trainingmanager_app` · **Depends on:** S1 · **Date:** 2026-06-28
**Status:** Outline — to be deepened when reached.

> The athlete's core daily loop: see my sessions → respond (RSVP/ROTI) → read what the
> session contains. All **read/respond**; no create/edit/delete, no attendance marking,
> no debrief authoring.

## Screens

1. **Dashboard** — `GET /teams?is_active=true` + `GET /dashboard/summary?today=YYYY-MM-DD`
   (local date). Shows: upcoming sessions (`member_upcoming`), attendance history
   (`member_attendance_history`, read-only status + icon), my teams cards. EmptyState
   when nothing. Cap ~20 + "view all".
2. **Events list / calendar** — `GET /events?date__gte&date__lte&refer_program…` scoped
   to the athlete's teams; month/calendar view + filters (team, program, date range);
   row → detail.
3. **Event detail** — `GET /events/{id}` (embeds `rounds_detail`, one shot). Tabs the
   athlete sees:
   - **Overview**: name, date/time, location, equipment; goal gated by `vis_goal`.
   - **RSVP**: going / maybe / not_going buttons; shows my status; submit via the events
     RSVP endpoint. (Aggregate counts/per-member = manager-only, hide.)
   - **ROTI**: only if team `roti_enabled`; 1–5 score; shows my score; submit. (Distribution
     = manager-only.)
   - **Training**: **read-only** rounds + exercises rendered from `rounds_detail`
     (per-round name/sets/reps, per-exercise name/modality/distance, total distance);
     gated by `vis_rounds` (always/after/never → "hidden until after the session"). Honour
     `vis_distance` for totals. Freeform sessions render `training_richtext` (HTML).
   - **Attachments**: list + **download only** (`GET …/attachments/`).
   - *Hidden:* Attendance tab, Debrief tab, all edit/AI/share actions.

## Visibility rules (athlete-critical)

`vis_distance`, `vis_goal`, `vis_rounds` ∈ {`always`, `after`, `never`}. `after` ⇒ visible
only once `event.date` has passed. Multi-sport: sport badge/selector only when the team has
>1 sport.

## API (authoritative shapes from generated client)

events list/retrieve (+ `rounds_detail`), rsvp get/submit, roti get/submit, attachments
list/download, dashboard summary, programs (read-only, for filters/labels).

## Tests

Visibility gating (always/after/never × past/future), RSVP/ROTI submit + optimistic state,
rounds_detail rendering + total computation, freeform vs structured.

## Open items

Confirm RSVP/ROTI verb + payload (`POST` vs `PUT`) from the regenerated schema; confirm the
exact dashboard summary shape for athletes; attachment download auth (signed URL vs bearer).
