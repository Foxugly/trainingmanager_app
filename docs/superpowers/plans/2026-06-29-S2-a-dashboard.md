# S2-a — Athlete Dashboard Implementation Plan

> Executes inline (autonomous). First S2 slice. DTOs hand-written (codegen deferred).

**Goal:** Replace the Home placeholder with a real athlete **dashboard** from `GET /dashboard/summary/`: upcoming sessions, recent attendance history, my-teams count. Profile + logout stay reachable.

**Contract (schema, `DashboardSummary`):** one authenticated `GET /api/v1/dashboard/summary/`. Athlete-relevant fields (all required): `member_teams[]` (`team_id`, `members_count`, `my_member_id?` — NO team name), `member_upcoming[]` + `member_upcoming_total`, `member_attendance_history[]`, `member_history_truncated`. Each event item: `event{id,name,date?,hour_start?,hour_end?,location,place?}`, `team_id`, `team_name`, `program_id?`, `program_name`. History item adds `attendance_id`, `status_code`, `status?`. The `coach_*` fields + nested `place`/`status` objects are ignored (`ignoreUnknownKeys=true`). Authenticated (bearer) — NOT in `AUTH_PATHS`.

## Global Constraints
Same as prior slices. Verify `:composeApp:testAndroidHostTest`; APK `:androidApp:assembleDebug`. i18n: new strings added to the `Strings` interface + all 5 locales.

## Tasks
1. **DTOs** `data/api/DashboardDtos.kt`: `DashboardEvent`, `DashboardEventItem`, `DashboardHistoryItem`, `DashboardMemberTeam`, `DashboardSummary` (only athlete `member_*` fields; coach_*/place/status omitted via ignoreUnknownKeys; list/int defaults for safety).
2. **API + repo**: `TrainingManagerApi.getDashboard(): Result<DashboardSummary>` = `apiCall { client.get("dashboard/summary/") }`; `AuthRepository.getDashboard()`. Tests: decode a representative payload (with coach_* present → ignored); 401 surfaces.
3. **Strings**: add to interface + 5 locales — `dashboardTitle`, `dashboardUpcoming`, `dashboardNoUpcoming`, `dashboardHistory`, `dashboardNoHistory`, `dashboardLoadFailed`, `retry`, `fun dashboardTeams(count): String`.
4. **DashboardViewModel** (`ui/dashboard/`): `isLoading`, `error`, `summary: DashboardSummary?`; `suspend load()`. Test: load success populates summary; failure sets error.
5. **DashboardScreen**: top row (title + Profile + logout); loading spinner; error + retry; else "my teams: N", upcoming list (name, date/time, location, team — program), recent history (name, date, team, status_code). Reuses `LocalStrings`.
6. **Wire**: Koin `factory { DashboardViewModel(get(), get<LanguageService>().strings) }`; `App` `composable<HomeRoute>` renders `DashboardScreen` (keep `HomePlaceholderScreen` deletion) with onProfile/onLoggedOut. Keep route name HomeRoute. Verify tests + APK. Commit per task; PR; merge.

## Self-Review
Covers S2 dashboard (athlete view). Deferred to later S2 slices: events list/calendar + event detail, RSVP, ROTI, rounds/exercises, attachments, team names (needs the Teams list — S3 or a teams fetch). Hand-written DTOs (no codegen). Types: `DashboardSummary` → API → repo → VM → screen. Replaces `HomePlaceholderScreen` (removed).
