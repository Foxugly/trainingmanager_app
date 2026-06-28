# S3 — Teams + Discussions + Messages Hub (outline)

**Target repo:** `trainingmanager_app` · **Depends on:** S1 · **Date:** 2026-06-28
**Status:** Outline — to be deepened when reached.

> **"Messaging" = team discussions, not private DMs.** The backend has no private
> messaging; the web "Messages" page is a cross-team aggregation of team discussion
> **topics**. Hence Teams, Discussions, and the Messages hub are one sub-project.

## Screens

1. **Teams list** — `GET /teams?is_active=true`. Cards: name, sport(s), member count,
   role badge. Athlete sees teams where they are a member.
2. **Team detail** — `GET /teams/{id}`. Athlete-visible tabs:
   - **Overview**: name, sport(s), members count, created.
   - **Members**: read-only roster (name, role, join date).
   - **Stats**: team aggregate + the athlete's own stats card (read-only).
   - **Discussions** (chat): see below.
   - *Hidden (manager-only):* join requests, slots editor, managers, place pool, audit,
     templates; `coaches`-audience topics are not shown to athletes.
3. **Discussions**:
   - **Topics list** — `GET /teams/{id}/topics/`, only `audience=team`; sort by
     `-updated_at`; show title/author/last-activity/message_count.
   - **Thread** — `GET /teams/{id}/topics/{topic_id}/messages/`; show messages
     (author, body HTML, created_at, edited_at).
   - **Post** — `POST …/messages/ {body}` when `topic.allow_athlete_replies` (athletes
     may reply only on team-audience topics that allow it). **Edit/delete own** message
     (`PATCH`/`DELETE …/messages/{id}`).
   - **Emoji**: curated picker, inserts as text (not fragile HTML). Rich text kept simple
     on mobile (bold/italic/links) — render server HTML safely; compose plain or
     lightweight formatting.
   - **Load pattern**: cancel in-flight request on topic/team switch (mirror the web's
     `toObservable + switchMap`).
4. **Messages hub** — aggregate `GET /teams/{id}/topics/` across the athlete's teams;
   cross-team topic list (title, team name, last activity) → deep-link into the team's
   discussion thread.

## API (authoritative shapes from generated client)

teams list/retrieve, members, stats, topics list, messages list/create/update/delete.

## Tests

Topic audience filtering (team vs coaches), reply-permission gating, post/edit/delete own,
switch-cancels-in-flight, cross-team hub aggregation + sort.

## Open items

Confirm `allow_athlete_replies` / `topic_creation` fields and whether athletes can create
topics (default: **no** — reply only); confirm stats endpoint shape; mark-read semantics.
