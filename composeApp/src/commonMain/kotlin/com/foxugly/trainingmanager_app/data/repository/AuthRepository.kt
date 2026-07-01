package com.foxugly.trainingmanager_app.data.repository

import com.foxugly.trainingmanager_app.api.generated.models.CompleteInvitationRequest
import com.foxugly.trainingmanager_app.api.generated.models.DeviceRegisterRequest
import com.foxugly.trainingmanager_app.api.generated.models.DeviceUnregisterRequest
import com.foxugly.trainingmanager_app.api.generated.models.EmailConfirmRequest
import com.foxugly.trainingmanager_app.api.generated.models.EventRequest
import com.foxugly.trainingmanager_app.api.generated.models.ExerciseRequest
import com.foxugly.trainingmanager_app.api.generated.models.GenerateTrainingRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.LanguageEnum
import com.foxugly.trainingmanager_app.api.generated.models.PatchedEventRequest
import com.foxugly.trainingmanager_app.api.generated.models.PatchedExerciseRequest
import com.foxugly.trainingmanager_app.api.generated.models.PatchedRoundRequest
import com.foxugly.trainingmanager_app.api.generated.models.ReorderExercisesRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.ReorderRoundsRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.RoundRequest
import com.foxugly.trainingmanager_app.api.generated.models.MagicLinkExchangeRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.MagicLinkRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.Me
import com.foxugly.trainingmanager_app.api.generated.models.PasswordChangeRequest
import com.foxugly.trainingmanager_app.api.generated.models.PasswordResetConfirmRequest
import com.foxugly.trainingmanager_app.api.generated.models.PasswordResetRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.PatchedMeRequest
import com.foxugly.trainingmanager_app.api.generated.models.PlatformEnum
import com.foxugly.trainingmanager_app.api.generated.models.RegisterRequest
import com.foxugly.trainingmanager_app.api.generated.models.TokenObtainPairResponse
import com.foxugly.trainingmanager_app.api.generated.models.TokenRefreshRequest
import com.foxugly.trainingmanager_app.api.generated.models.ValidateInvitation
import com.foxugly.trainingmanager_app.api.generated.models.VerifiedTokenObtainPairRequest
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.storage.TokenStore
import com.foxugly.trainingmanager_app.diagnostics.AppLogger
import kotlinx.coroutines.CancellationException

class AuthRepository(
    private val api: TrainingManagerApi,
    private val tokenStorage: TokenStore,
    // Supplies the current FCM token so logout can unregister this device. Default
    // no-op keeps tests and non-push platforms simple.
    private val fcmTokenProvider: suspend () -> String? = { null },
) {
    private val tag = "TM/AuthRepository"

    /** POST auth/token/ → store tokens + remember → GET me/ → profile. */
    suspend fun login(email: String, password: String, remember: Boolean): Result<Me> {
        // Don't log the email — it's PII and would land in Logcat.
        AppLogger.info(tag, "Login requested (remember=$remember)")
        return api.login(VerifiedTokenObtainPairRequest(email, password, remember)).mapCatching { pair ->
            tokenStorage.setAccessToken(pair.access)
            tokenStorage.setRefreshToken(pair.refresh)
            tokenStorage.setRemember(remember)
            api.getMe().getOrThrow()
        }.onFailure {
            if (it is CancellationException) throw it
            AppLogger.error(tag, "Login failed: ${it.message}", it)
        }
    }

    suspend fun logout(): Result<Unit> {
        AppLogger.info(tag, "Logout requested")
        // Unregister this device's push token first (while the bearer is still valid)
        // so the logged-out user stops receiving pushes here. Best-effort.
        runCatching { fcmTokenProvider() }.getOrNull()?.let { token ->
            api.unregisterDevice(
                DeviceUnregisterRequest(token),
            ).onFailure { if (it is CancellationException) throw it }
        }
        val refreshToken = tokenStorage.getRefreshToken()
        if (refreshToken != null) {
            api.logout(refreshToken).onFailure {
                if (it is CancellationException) throw it
                AppLogger.error(tag, "Logout API call failed (tokens cleared locally): ${it.message}", it)
            }
        }
        tokenStorage.clearAuthTokens()
        return Result.success(Unit)
    }

    /** POST auth/magic-link/request/ — always-200, no token handling. */
    suspend fun requestMagicLink(email: String): Result<Unit> {
        AppLogger.info(tag, "Magic-link requested")
        return api.magicLinkRequest(MagicLinkRequestRequest(email.trim()))
            .onFailure { if (it is CancellationException) throw it }
    }

    /** POST auth/register/ — self-signup with a Turnstile captcha token. */
    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        language: String,
        turnstileToken: String,
    ): Result<Unit> =
        api.register(
            RegisterRequest(
                email = email.trim(),
                password = password,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                turnstileToken = turnstileToken,
                // The generated request models `language` as an enum; callers pass a
                // wire code ("en"/"fr"/…), so decode it back (null → field omitted).
                language = LanguageEnum.decode(language),
            ),
        ).onFailure { if (it is CancellationException) throw it }

    /** POST auth/password/reset/ — always-200; carries the captcha token. */
    suspend fun requestPasswordReset(email: String, turnstileToken: String): Result<Unit> =
        api.requestPasswordReset(
            PasswordResetRequestRequest(email = email.trim(), turnstileToken = turnstileToken),
        ).onFailure { if (it is CancellationException) throw it }

    /** POST auth/magic-link/exchange/ → auto-login. */
    suspend fun exchangeMagicLink(token: String): Result<Me> =
        autoLogin { api.magicLinkExchange(MagicLinkExchangeRequestRequest(token)) }

    /** POST auth/email/confirm/ → auto-login. */
    suspend fun confirmEmail(key: String): Result<Me> =
        autoLogin { api.confirmEmail(EmailConfirmRequest(key)) }

    /** POST auth/password/reset/confirm/ → auto-login. */
    suspend fun confirmPasswordReset(key: String, newPassword: String): Result<Me> =
        autoLogin { api.confirmPasswordReset(PasswordResetConfirmRequest(key, newPassword)) }

    /** Shared: a token-pair call → persist access+refresh+remember → GET me/ → profile. */
    private suspend inline fun autoLogin(
        crossinline tokenCall: suspend () -> Result<TokenObtainPairResponse>,
    ): Result<Me> = tokenCall().mapCatching { pair ->
        tokenStorage.setAccessToken(pair.access)
        tokenStorage.setRefreshToken(pair.refresh)
        tokenStorage.setRemember(true)
        api.getMe().getOrThrow()
    }.onFailure {
        if (it is CancellationException) throw it
        AppLogger.error(tag, "Auto-login failed: ${it.message}", it)
    }

    suspend fun lookupInvitation(token: String): Result<ValidateInvitation> = api.lookupInvitation(token)

    /** POST invitations/lookup/{token}/ → create account + join + auto-login. */
    suspend fun acceptInvitation(token: String, password: String): Result<Me> =
        autoLogin { api.completeInvitation(token, CompleteInvitationRequest(password)) }

    suspend fun getCurrentUser(): Result<Me> = api.getMe()

    suspend fun getDashboard() = api.getDashboard()

    suspend fun listEvents(dateGte: String? = null, dateLte: String? = null) =
        api.listEvents(dateGte = dateGte, dateLte = dateLte)

    suspend fun getEvent(id: Int) = api.getEvent(id)

    suspend fun getRsvp(eventId: Int) = api.getRsvp(eventId)

    suspend fun setRsvp(eventId: Int, status: com.foxugly.trainingmanager_app.api.generated.models.RsvpStatusEnum) =
        api.setRsvp(eventId, com.foxugly.trainingmanager_app.api.generated.models.RsvpUpsertRequest(status))

    suspend fun setRoti(eventId: Int, score: Int) =
        api.setRoti(eventId, com.foxugly.trainingmanager_app.api.generated.models.RotiUpsertRequest(score))

    suspend fun listEventAttachments(eventId: Int) = api.listAttachments("event", eventId)

    suspend fun attachmentDownloadUrl(id: Int) = api.attachmentDownloadUrl(id)

    suspend fun getTeam(id: Int) = api.getTeam(id)

    suspend fun listMembers() = api.listMembers()

    suspend fun registerDevice(pushToken: String, platform: String, deviceName: String = "") =
        api.registerDevice(
            DeviceRegisterRequest(
                pushToken = pushToken,
                // The generated request models the platform as an enum; the FCM provider
                // hands us the wire value ("android"/"ios"), so decode it back.
                platform = PlatformEnum.decode(platform)
                    ?: throw IllegalArgumentException("Unsupported device platform: $platform"),
                deviceName = deviceName,
            ),
        )

    suspend fun unregisterDevice(pushToken: String) =
        api.unregisterDevice(DeviceUnregisterRequest(pushToken))

    suspend fun listNotifications() = api.listNotifications()

    suspend fun markNotificationRead(id: Int) = api.markNotificationRead(id)

    suspend fun markAllNotificationsRead() = api.markAllNotificationsRead()

    suspend fun listTopics(teamId: Int) = api.listTopics(teamId)

    suspend fun listMessages(teamId: Int, topicId: Int) = api.listMessages(teamId, topicId)

    suspend fun postMessage(teamId: Int, topicId: Int, content: String) =
        api.postMessage(teamId, topicId, com.foxugly.trainingmanager_app.api.generated.models.TopicMessageRequest(content))

    suspend fun deleteMessage(teamId: Int, topicId: Int, messageId: Int) =
        api.deleteMessage(teamId, topicId, messageId)

    // --- Coach writes (managers only, enforced server-side) ---
    suspend fun createEvent(body: EventRequest) = api.createEvent(body)

    suspend fun updateEvent(id: Int, body: PatchedEventRequest) = api.updateEvent(id, body)

    suspend fun deleteEvent(id: Int) = api.deleteEvent(id)

    suspend fun generateTraining(id: Int, body: GenerateTrainingRequestRequest) = api.generateTraining(id, body)

    suspend fun createRound(body: RoundRequest) = api.createRound(body)

    suspend fun updateRound(id: Int, body: PatchedRoundRequest) = api.updateRound(id, body)

    suspend fun deleteRound(id: Int) = api.deleteRound(id)

    suspend fun reorderRounds(eventId: Int, body: ReorderRoundsRequestRequest) = api.reorderRounds(eventId, body)

    suspend fun createExercise(body: ExerciseRequest) = api.createExercise(body)

    suspend fun updateExercise(id: Int, body: PatchedExerciseRequest) = api.updateExercise(id, body)

    suspend fun deleteExercise(id: Int) = api.deleteExercise(id)

    suspend fun reorderExercises(roundId: Int, body: ReorderExercisesRequestRequest) = api.reorderExercises(roundId, body)

    /** PATCH me/ — partial profile update. */
    suspend fun updateProfile(body: PatchedMeRequest): Result<Me> = api.patchMe(body)

    /** POST auth/password/change/ — no logout; tokens stay valid. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        api.changePassword(PasswordChangeRequest(currentPassword, newPassword))
            .onFailure { if (it is CancellationException) throw it }

    fun isAuthenticated(): Boolean = tokenStorage.getAccessToken() != null

    fun hasRefreshToken(): Boolean = tokenStorage.getRefreshToken() != null

    /** Startup refresh: POST auth/token/refresh/, persisting the rotated token pair. */
    suspend fun tryRefresh(): Boolean {
        val refreshToken = tokenStorage.getRefreshToken() ?: return false
        AppLogger.info(tag, "Trying startup token refresh")
        return api.refresh(TokenRefreshRequest(refreshToken)).fold(
            onSuccess = { response ->
                tokenStorage.setAccessToken(response.access)
                // Persist the rotated refresh token (ROTATE_REFRESH_TOKENS +
                // BLACKLIST_AFTER_ROTATION). The generated TokenRefresh always carries a
                // refresh (rotation is on server-side), so write it unconditionally —
                // skipping it would send a blacklisted token on the next refresh and eject the user.
                tokenStorage.setRefreshToken(response.refresh)
                AppLogger.info(tag, "Startup token refresh succeeded")
                true
            },
            onFailure = {
                if (it is CancellationException) throw it
                AppLogger.error(tag, "Startup token refresh failed", it)
                tokenStorage.clearAuthTokens()
                false
            },
        )
    }
}
