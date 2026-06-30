package com.foxugly.trainingmanager_app

import com.foxugly.trainingmanager_app.data.storage.TokenStore

/** In-memory [TokenStore] for tests. */
internal class FakeTokenStore(
    private var access: String? = null,
    private var refresh: String? = null,
    private var remember: Boolean = false,
) : TokenStore {
    var cleared = false
        private set

    override fun getAccessToken() = access
    override fun setAccessToken(token: String?) { access = token }
    override fun getRefreshToken() = refresh
    override fun setRefreshToken(token: String?) { refresh = token }
    override fun getRemember() = remember
    override fun setRemember(value: Boolean) { remember = value }
    override fun clearAuthTokens() { access = null; refresh = null; cleared = true }
}

/**
 * Complete `GET /me/` JSON for the generated [com.foxugly.trainingmanager_app.api.generated.models.Me]
 * model. Unlike the old hand-written `UserProfile`, `Me` marks `email_confirmed`, `is_staff`,
 * `is_superuser`, `last_login` (key must be present), `date_joined`, `team_quota`, `calendar_token`
 * and `member_id` (key must be present) as required, so any mocked profile response MUST include them
 * or kotlinx.serialization throws at decode. Use this helper everywhere a profile/login response is
 * mocked. Optional fields are emitted only when explicitly provided (the server may omit them).
 */
internal fun meJson(
    id: Int = 1,
    email: String = "a@b.co",
    language: String? = "fr",
    firstName: String? = null,
    lastName: String? = null,
    weeklyRecapOptIn: Boolean? = null,
    digestEmail: Boolean? = null,
): String {
    val fields = buildList {
        add("\"id\":$id")
        add("\"email\":\"$email\"")
        add("\"email_confirmed\":true")
        add("\"is_staff\":false")
        add("\"is_superuser\":false")
        add("\"last_login\":null")
        add("\"date_joined\":\"2026-01-01T00:00:00Z\"")
        add("\"team_quota\":{\"used\":0,\"max\":3,\"can_create\":true}")
        add("\"calendar_token\":\"caltok\"")
        add("\"member_id\":null")
        language?.let { add("\"language\":\"$it\"") }
        firstName?.let { add("\"first_name\":\"$it\"") }
        lastName?.let { add("\"last_name\":\"$it\"") }
        weeklyRecapOptIn?.let { add("\"weekly_recap_opt_in\":$it") }
        digestEmail?.let { add("\"digest_email\":$it") }
    }
    return fields.joinToString(prefix = "{", postfix = "}")
}

/**
 * Complete `GET /notifications/` list-item JSON for the generated
 * [com.foxugly.trainingmanager_app.api.generated.models.Notification] model. Every field
 * (`id`, `type`, `title`, `body`, `url`, `is_read`, `created_at`) is `@Required`, and `type`
 * is now an enum: its value MUST be one of the wire serial names (e.g. `message_new_topic`),
 * not the Kotlin entry name — kotlinx.serialization throws on an unknown value at decode.
 */
internal fun notificationJson(
    id: Int = 7,
    type: String = "message_new_topic",
    title: String = "T",
    body: String = "B",
    url: String = "/teams/3",
    isRead: Boolean = false,
    createdAt: String = "2026-06-01T10:00:00Z",
): String =
    """{"id":$id,"type":"$type","title":"$title","body":"$body","url":"$url","is_read":$isRead,"created_at":"$createdAt"}"""

/** A full `PaginatedNotificationList` JSON wrapping the given item JSON strings. */
internal fun notificationListJson(vararg items: String): String =
    """{"count":${items.size},"results":[${items.joinToString(",")}]}"""

/**
 * One `DashboardMemberTeam` JSON item. The generated model marks `team_id`, `members_count`
 * and `my_member_id` (key must be present, value may be null) as `@Required`.
 */
internal fun dashboardMemberTeamJson(
    teamId: Int = 1,
    membersCount: Int = 4,
    myMemberId: Int? = null,
): String =
    """{"team_id":$teamId,"members_count":$membersCount,"my_member_id":${myMemberId ?: "null"}}"""

/**
 * Complete `GET /dashboard/summary/` JSON for the generated
 * [com.foxugly.trainingmanager_app.api.generated.models.DashboardSummary] model. Every field
 * (incl. the coach_* side) is `@Required`, so a mocked response MUST carry all ten keys or
 * kotlinx.serialization throws at decode. The list params take pre-built JSON array strings
 * (e.g. from [dashboardMemberTeamJson]); callers only supply what the test asserts on.
 */
internal fun dashboardSummaryJson(
    coachTeams: String = "[]",
    memberTeams: String = "[]",
    coachUpcoming: String = "[]",
    coachUpcomingTotal: Int = 0,
    coachAttendancePending: String = "[]",
    coachPendingTruncated: Boolean = false,
    memberUpcoming: String = "[]",
    memberUpcomingTotal: Int = 0,
    memberAttendanceHistory: String = "[]",
    memberHistoryTruncated: Boolean = false,
): String =
    """{"coach_teams":$coachTeams,"member_teams":$memberTeams,"coach_upcoming":$coachUpcoming,""" +
        """"coach_upcoming_total":$coachUpcomingTotal,"coach_attendance_pending":$coachAttendancePending,""" +
        """"coach_pending_truncated":$coachPendingTruncated,"member_upcoming":$memberUpcoming,""" +
        """"member_upcoming_total":$memberUpcomingTotal,"member_attendance_history":$memberAttendanceHistory,""" +
        """"member_history_truncated":$memberHistoryTruncated}"""

// --- Events domain (generated models) ---
// The generated event graph (Event → EventRoundDetail → Exercise → Modality/EnergySegment)
// is far stricter than the old hand-written DTOs: every nested model carries its own set of
// @Required fields (e.g. Event.sport, Exercise.language/usage_count, the nested Sport/EnergySystem),
// so a mocked response MUST carry them all or kotlinx.serialization throws at decode. The visibility
// and RSVP-status strings are now enums — their JSON values must be the wire serial names
// (`always`/`after`/`never`, `going`/`maybe`/`not_going`).

/** Complete `Sport` JSON — every field is `@Required`. */
internal fun sportJson(
    id: Int = 1,
    name: String = "Natation",
    slug: String = "natation",
    isActive: Boolean = true,
    energySystems: String = "[]",
    defaultTrainingType: String = "structured",
    createdAt: String = "2026-01-01T00:00:00Z",
): String =
    """{"id":$id,"name":"$name","slug":"$slug","is_active":$isActive,"energy_systems":$energySystems,""" +
        """"default_training_type":"$defaultTrainingType","created_at":"$createdAt"}"""

/** `PlaceMinimal` JSON — `id`/`name`/`address` all `@Required`. */
internal fun placeMinimalJson(id: Int = 1, name: String = "Stade X", address: String = "Rue Y"): String =
    """{"id":$id,"name":"$name","address":"$address"}"""

/** `ProgramMinimal` JSON — `id`/`name` both `@Required`. */
internal fun programMinimalJson(id: Int = 2, name: String = "Prépa"): String =
    """{"id":$id,"name":"$name"}"""

/** `EnergySystem` JSON — `id`/`name`/`is_active` all `@Required`. */
internal fun energySystemJson(id: Int = 1, name: String = "Aérobie", isActive: Boolean = true): String =
    """{"id":$id,"name":"$name","is_active":$isActive}"""

/** `EnergySegment` JSON — `id`/`abv`/`energy_system`/`is_active` `@Required`. */
internal fun energySegmentJson(id: Int = 3, abv: String = "PMA"): String =
    """{"id":$id,"abv":"$abv","energy_system":${energySystemJson()},"is_active":true}"""

/** `Modality` JSON — `id`/`name`/`sport`/`is_active` `@Required`. */
internal fun modalityJson(id: Int = 2, name: String = "Crawl"): String =
    """{"id":$id,"name":"$name","sport":${sportJson()},"is_active":true}"""

/**
 * `Exercise` JSON. `id`, `modality`, `energysegment`, `language`, `usage_count`, `created_at`
 * and `updated_at` are `@Required`; `order`/`repetition`/`distance`/`notes`/`t_start`/`t_break`
 * are optional and emitted only when supplied.
 */
internal fun exerciseJson(
    id: Int = 9,
    order: Long? = 1,
    repetition: Long? = 4,
    distance: Long? = 100,
    notes: String? = "sprint",
    tStart: String? = null,
    tBreak: String? = null,
    modality: String = modalityJson(),
    energysegment: String = energySegmentJson(),
): String {
    val fields = buildList {
        add("\"id\":$id")
        add("\"modality\":$modality")
        add("\"energysegment\":$energysegment")
        add("\"language\":\"fr\"")
        add("\"usage_count\":0")
        add("\"created_at\":\"2026-01-01T00:00:00Z\"")
        add("\"updated_at\":\"2026-01-01T00:00:00Z\"")
        order?.let { add("\"order\":$it") }
        repetition?.let { add("\"repetition\":$it") }
        distance?.let { add("\"distance\":$it") }
        notes?.let { add("\"notes\":\"$it\"") }
        tStart?.let { add("\"t_start\":\"$it\"") }
        tBreak?.let { add("\"t_break\":\"$it\"") }
    }
    return fields.joinToString(prefix = "{", postfix = "}")
}

/**
 * `EventRoundDetail` JSON — `id`/`order`/`count`/`t_start`/`t_break`/`sport`/`exercises` are all
 * `@Required` (the nullable `t_start`/`t_break` keys must still be present).
 */
internal fun roundDetailJson(
    id: Int = 1,
    order: Int = 1,
    count: Int = 3,
    tStart: String? = null,
    tBreak: String? = null,
    exercises: String = "[${exerciseJson()}]",
): String =
    """{"id":$id,"order":$order,"count":$count,""" +
        """"t_start":${tStart?.let { "\"$it\"" } ?: "null"},"t_break":${tBreak?.let { "\"$it\"" } ?: "null"},""" +
        """"sport":${sportJson()},"exercises":$exercises}"""

/**
 * Complete `Event` JSON. The generated model marks a large set of fields `@Required`
 * (`place`/`equipment_items`/`refer_program`/`team_id`/`sport`/`rounds`/`rounds_detail`/`members`/
 * `is_public`/`public_token`/`generated_by_ai`/`ai_response`/`ai_generated_at`/`created_at`/`updated_at`
 * on top of `id`/`name`), so all are always emitted. Nullable-but-required keys (`place`,
 * `public_token`, `ai_generated_at`) are emitted as `null` by default. The athlete-facing optional
 * fields are emitted only when supplied. `refer_program` is now non-null on the wire.
 */
internal fun eventJson(
    id: Int = 5,
    name: String = "Séance",
    place: String? = null,
    referProgram: String = programMinimalJson(),
    roundsDetail: String = "[]",
    location: String? = "Gym",
    equipment: String? = "",
    goal: String? = null,
    total: Long? = 0,
    date: String? = null,
    hourStart: String? = null,
    hourEnd: String? = null,
    visDistance: String? = "never",
    visGoal: String? = "never",
    visRounds: String? = "never",
    debrief: String? = "",
): String {
    val fields = buildList {
        add("\"id\":$id")
        add("\"name\":\"$name\"")
        add("\"place\":${place ?: "null"}")
        add("\"equipment_items\":[]")
        add("\"refer_program\":$referProgram")
        add("\"team_id\":1")
        add("\"sport\":${sportJson()}")
        add("\"rounds\":[]")
        add("\"rounds_detail\":$roundsDetail")
        add("\"members\":[]")
        add("\"is_public\":false")
        add("\"public_token\":null")
        add("\"generated_by_ai\":false")
        add("\"ai_response\":\"\"")
        add("\"ai_generated_at\":null")
        add("\"created_at\":\"2026-01-01T00:00:00Z\"")
        add("\"updated_at\":\"2026-01-01T00:00:00Z\"")
        goal?.let { add("\"goal\":\"$it\"") }
        location?.let { add("\"location\":\"$it\"") }
        equipment?.let { add("\"equipment\":\"$it\"") }
        total?.let { add("\"total\":$it") }
        date?.let { add("\"date\":\"$it\"") }
        hourStart?.let { add("\"hour_start\":\"$it\"") }
        hourEnd?.let { add("\"hour_end\":\"$it\"") }
        visDistance?.let { add("\"vis_distance\":\"$it\"") }
        visGoal?.let { add("\"vis_goal\":\"$it\"") }
        visRounds?.let { add("\"vis_rounds\":\"$it\"") }
        debrief?.let { add("\"debrief\":\"$it\"") }
    }
    return fields.joinToString(prefix = "{", postfix = "}")
}

/** A full `PaginatedEventList` JSON wrapping the given `Event` item JSON strings. */
internal fun eventListJson(vararg items: String): String =
    """{"count":${items.size},"next":null,"previous":null,"results":[${items.joinToString(",")}]}"""

/** `RsvpCounts` JSON — `going`/`maybe`/`not_going`/`no_response` all `@Required`. */
internal fun rsvpCountsJson(going: Int = 0, maybe: Int = 0, notGoing: Int = 0, noResponse: Int = 0): String =
    """{"going":$going,"maybe":$maybe,"not_going":$notGoing,"no_response":$noResponse}"""

/**
 * `RsvpSummary` JSON. `counts`/`total_members`/`my_status`/`by_member` are all `@Required`
 * (`my_status` may be null but the key must be present). `myStatus`, when set, must be one of
 * the wire enum values `going`/`maybe`/`not_going`.
 */
internal fun rsvpSummaryJson(
    counts: String = rsvpCountsJson(going = 1, maybe = 1, noResponse = 2),
    totalMembers: Int = 4,
    myStatus: String? = "going",
    byMember: String = "[]",
): String =
    """{"counts":$counts,"total_members":$totalMembers,""" +
        """"my_status":${myStatus?.let { "\"$it\"" } ?: "null"},"by_member":$byMember}"""

/**
 * `RotiSummary` JSON. `average`/`count`/`distribution`/`my_score` are all `@Required`
 * (`average`/`my_score` may be null but keys must be present).
 */
internal fun rotiSummaryJson(
    average: Double? = null,
    count: Int = 0,
    distribution: String = "{}",
    myScore: Int? = null,
): String =
    """{"average":${average ?: "null"},"count":$count,"distribution":$distribution,"my_score":${myScore ?: "null"}}"""

// --- Teams domain (generated models) ---
// The generated `Team` is far richer and stricter than the old hand-written `TeamDto`: on top of
// id/name it marks sports, sport, level (nullable key), owner, managers, default_pool, places,
// default_place (nullable key), equipment, logo_url (nullable key), created_at and updated_at as
// @Required, and `sport`/`owner` are now NON-null. `Member.user`/`created_at`/`updated_at` and
// `CustomUserPublic.first_name`/`last_name` are likewise @Required, so a mocked response MUST carry
// them all or kotlinx.serialization throws at decode.

/** `CustomUserPublic` JSON — `id`/`first_name`/`last_name` all `@Required` (non-null). */
internal fun customUserPublicJson(id: Int = 1, firstName: String = "Ann", lastName: String = "Lee"): String =
    """{"id":$id,"first_name":"$firstName","last_name":"$lastName"}"""

/**
 * Complete `Member` JSON. `id`/`firstname`/`lastname`/`fullname`/`teams`/`user`/`created_at`/
 * `updated_at` are `@Required`; `email`/`phonenumber` optional. `teams` takes a pre-built JSON
 * int-array string (e.g. "[3]").
 */
internal fun memberJson(
    id: Int = 1,
    firstname: String = "A",
    lastname: String = "X",
    fullname: String = "A X",
    teams: String = "[]",
    user: String = customUserPublicJson(),
): String =
    """{"id":$id,"firstname":"$firstname","lastname":"$lastname","fullname":"$fullname","teams":$teams,""" +
        """"user":$user,"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}"""

/** A full `PaginatedMemberList` JSON wrapping the given `Member` item JSON strings. */
internal fun memberListJson(vararg items: String): String =
    """{"count":${items.size},"results":[${items.joinToString(",")}]}"""

// --- Discussions domain (generated models) ---
// The generated `Topic`/`TopicMessage` are stricter than the old hand-written DTOs: `Topic` marks
// id/title/author/message_count/created_at/updated_at as @Required (author is now NON-null), and
// `audience` is the `AudienceEnum` ("team"/"coaches" wire values only). `TopicMessage` marks
// id/content/author/edited_at/created_at @Required (author NON-null; edited_at key must be present,
// value may be null). A mocked response MUST carry them all or kotlinx.serialization throws at decode.

/**
 * Complete `Topic` JSON. `id`/`title`/`author`/`message_count`/`created_at`/`updated_at` are
 * `@Required`; `audience`/`allow_athlete_replies` optional. `audience`, when set, must be a valid
 * `AudienceEnum` wire value (`team`/`coaches`).
 */
internal fun topicJson(
    id: Int = 1,
    title: String = "Topic",
    audience: String? = "team",
    allowAthleteReplies: Boolean? = true,
    author: String = customUserPublicJson(),
    messageCount: Int = 0,
    createdAt: String = "2026-06-01T10:00:00Z",
    updatedAt: String = "2026-06-01T10:00:00Z",
): String {
    val fields = buildList {
        add("\"id\":$id")
        add("\"title\":\"$title\"")
        add("\"author\":$author")
        add("\"message_count\":$messageCount")
        add("\"created_at\":\"$createdAt\"")
        add("\"updated_at\":\"$updatedAt\"")
        audience?.let { add("\"audience\":\"$it\"") }
        allowAthleteReplies?.let { add("\"allow_athlete_replies\":$it") }
    }
    return fields.joinToString(prefix = "{", postfix = "}")
}

/** A full `PaginatedTopicList` JSON wrapping the given `Topic` item JSON strings. */
internal fun topicListJson(vararg items: String): String =
    """{"count":${items.size},"results":[${items.joinToString(",")}]}"""

/**
 * Complete `TopicMessage` JSON. `id`/`content`/`author`/`edited_at`/`created_at` are `@Required`
 * (`edited_at` key must be present, value may be null); `author` is NON-null.
 */
internal fun topicMessageJson(
    id: Int = 9,
    content: String = "hi",
    author: String = customUserPublicJson(),
    editedAt: String? = null,
    createdAt: String = "2026-06-01T10:00:00Z",
): String =
    """{"id":$id,"content":"$content","author":$author,""" +
        """"edited_at":${editedAt?.let { "\"$it\"" } ?: "null"},"created_at":"$createdAt"}"""

/** A full `PaginatedTopicMessageList` JSON wrapping the given `TopicMessage` item JSON strings. */
internal fun topicMessageListJson(vararg items: String): String =
    """{"count":${items.size},"results":[${items.joinToString(",")}]}"""

/**
 * Complete `Team` JSON for the generated model. Emits every `@Required` key (nullable-but-required
 * keys `level`/`default_place`/`logo_url` default to `null`); the many optional flags/enums are
 * omitted (they decode to null). `sport`/`owner` are non-null on the wire.
 */
internal fun teamJson(
    id: Int = 3,
    name: String = "Sharks",
    sport: String = sportJson(),
    owner: String = customUserPublicJson(),
    managers: String = "[]",
    logoUrl: String? = null,
): String =
    """{"id":$id,"name":"$name","sports":[],"sport":$sport,"level":null,"owner":$owner,""" +
        """"managers":$managers,"default_pool":"","places":[],"default_place":null,"equipment":[],""" +
        """"logo_url":${logoUrl?.let { "\"$it\"" } ?: "null"},""" +
        """"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}"""

// --- Invitations domain (generated models) ---

/**
 * Complete `ValidateInvitation` JSON for the generated model. `email`/`team_name`/`status`/
 * `expires_at` are all `@Required`, and `status` is now the `InvitationStatusEnum` — its JSON value
 * MUST be a wire serial name (`pending`/`completed`/`expired`/`cancelled`), not the Kotlin entry
 * name, or kotlinx.serialization throws at decode.
 */
internal fun validateInvitationJson(
    email: String = "a@b.co",
    teamName: String = "Sharks",
    status: String = "pending",
    expiresAt: String = "2026-12-01T00:00:00Z",
): String =
    """{"email":"$email","team_name":"$teamName","status":"$status","expires_at":"$expiresAt"}"""
