# Coach capabilities in the mobile app — events & trainings

**Date:** 2026-07-01
**Status:** approved (design), in implementation
**Scope owner:** Renaud

## Problem

The KMP app was built athlete-only (consult / RSVP / ROTI / discussions, no
manager access). A coach who manages teams can see nothing actionable on mobile
and must use the web to create events or edit trainings. We want the app to also
let a coach **create/edit/delete events** and **edit their trainings**
(rounds/exercises), including **AI training generation**, without duplicating
flows.

## Decisions (agreed with Renaud)

- **Scope for this batch:** events + trainings (rounds/exercises) only. Members,
  programs and team management stay web-only for now.
- **Create flow — single editor, two entry points** (avoid "double emploi"):
  - Team detail (managed team) → "+ Événement" (team pre-selected).
  - Events list → "+" FAB → pick the team in the form.
  - There is exactly **one** event editor screen; the entry points only differ
    by whether the team is pre-filled.
- **Edit training** = edit the event's rounds/exercises, reached from the event
  detail ("Modifier l'entraînement"). Not a separate creation path.
- **AI:** include a "Générer l'entraînement" action (`POST
  /events/{id}/generate-training/`).

## Coach-gating

Write affordances appear **only on teams the caller manages**. Signal: the
team's id is in the dashboard `coach_teams`, or the caller is in
`team.managers`. The server independently enforces this (403 "Not a manager of
this event team"); the UI gating just avoids showing dead buttons. A pure
athlete sees the app unchanged.

## Backend

No changes required. Full CRUD already exists and is JWT-authenticated with the
same manager permissions the web coach uses:

- Events: `POST /events/`, `PATCH/DELETE /events/{id}/`, plus
  `POST /events/{id}/generate-training/` (200 → `GenerateTrainingResponse`; 403
  non-manager; 409 `event_has_rounds` / `event_has_training` / `event_in_past`),
  `POST /events/{id}/rounds/reorder/` (204).
- Rounds: `POST /rounds/`, `PATCH/DELETE /rounds/{id}/`,
  `POST /rounds/{id}/exercises/reorder/` (204).
- Exercises: `POST /exercises/`, `PATCH/DELETE /exercises/{id}/`.

The app's request models are already generated (`EventRequest`,
`PatchedEventRequest`, `RoundRequest`, `ExerciseRequest`, the `Patched*`
variants, `GenerateTrainingRequestRequest`, `Reorder*RequestRequest`).

## Events filter

The events list defaults to **upcoming** (`date__gte` = today; the API param
already exists on `listEvents`), with a filter to widen to past events / by
team — useful for a coach hunting an event to edit.

## Increments (one PR each)

1. **Client write layer** — `TrainingManagerApi` + `AuthRepository` methods for
   event/round/exercise create/update/delete + generate-training + reorder,
   with API tests. *(this PR)*
2. **Events filter** — default upcoming + widen filter.
3. **Event editor** — create/edit/delete event, both entry points, coach-gating.
4. **Training editor** — rounds/exercises add/edit/delete/reorder + AI generate.

## Testing

- API tests assert each write hits the right verb + path and decodes its result
  (incl. 204 → `Unit`).
- ViewModel tests (per editor increment) with `MockEngine` cover happy path +
  the 403/409 error surfaces.
