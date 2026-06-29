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
