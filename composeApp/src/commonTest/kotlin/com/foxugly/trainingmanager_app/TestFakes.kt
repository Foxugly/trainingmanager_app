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
