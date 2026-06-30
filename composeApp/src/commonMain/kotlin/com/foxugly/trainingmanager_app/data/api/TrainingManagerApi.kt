package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.api.generated.models.AttachmentDownloadResponse
import com.foxugly.trainingmanager_app.api.generated.models.CompleteInvitationRequest
import com.foxugly.trainingmanager_app.api.generated.models.DashboardSummary
import com.foxugly.trainingmanager_app.api.generated.models.DeviceRegisterRequest
import com.foxugly.trainingmanager_app.api.generated.models.DeviceUnregisterRequest
import com.foxugly.trainingmanager_app.api.generated.models.EmailConfirmRequest
import com.foxugly.trainingmanager_app.api.generated.models.Event
import com.foxugly.trainingmanager_app.api.generated.models.MagicLinkExchangeRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.MagicLinkRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.Me
import com.foxugly.trainingmanager_app.api.generated.models.PaginatedAttachmentList
import com.foxugly.trainingmanager_app.api.generated.models.PaginatedEventList
import com.foxugly.trainingmanager_app.api.generated.models.PaginatedMemberList
import com.foxugly.trainingmanager_app.api.generated.models.PaginatedNotificationList
import com.foxugly.trainingmanager_app.api.generated.models.PaginatedTopicList
import com.foxugly.trainingmanager_app.api.generated.models.PaginatedTopicMessageList
import com.foxugly.trainingmanager_app.api.generated.models.PasswordChangeRequest
import com.foxugly.trainingmanager_app.api.generated.models.PasswordResetConfirmRequest
import com.foxugly.trainingmanager_app.api.generated.models.PatchedMeRequest
import com.foxugly.trainingmanager_app.api.generated.models.PasswordResetRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.RegisterRequest
import com.foxugly.trainingmanager_app.api.generated.models.RotiSummary
import com.foxugly.trainingmanager_app.api.generated.models.RotiUpsertRequest
import com.foxugly.trainingmanager_app.api.generated.models.RsvpSummary
import com.foxugly.trainingmanager_app.api.generated.models.RsvpUpsertRequest
import com.foxugly.trainingmanager_app.api.generated.models.Team
import com.foxugly.trainingmanager_app.api.generated.models.TokenObtainPairResponse
import com.foxugly.trainingmanager_app.api.generated.models.TokenRefresh
import com.foxugly.trainingmanager_app.api.generated.models.TokenRefreshRequest
import com.foxugly.trainingmanager_app.api.generated.models.TopicMessage
import com.foxugly.trainingmanager_app.api.generated.models.TopicMessageRequest
import com.foxugly.trainingmanager_app.api.generated.models.ValidateInvitation
import com.foxugly.trainingmanager_app.api.generated.models.VerifiedTokenObtainPairRequest
import com.foxugly.trainingmanager_app.data.storage.TokenStore
import com.foxugly.trainingmanager_app.diagnostics.AppLogger
import com.foxugly.trainingmanager_app.i18n.LanguageProvider
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// DRF StandardPagination defaults to 50/page and caps page_size at 200. The
// athlete app shows flat lists (no infinite scroll), so request the server max
// on every list endpoint to avoid silently truncating events / notifications /
// messages / members at 50. (A list genuinely exceeding 200 would still need
// real .next paging — tracked as a follow-up.)
private const val LIST_PAGE_SIZE = 200

class TrainingManagerApi(
    private val tokenStorage: TokenStore,
    baseUrl: String = "https://tm-api.foxugly.com/api/v1/",
    enableLogging: Boolean = false,
    languageProvider: LanguageProvider = LanguageProvider(),
    // Tests inject a MockEngine here; production passes null (default engine).
    engine: HttpClientEngine? = null,
) : AutoCloseable {
    private val tag = "TM/Api"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        // Omit null properties on encode so we never send `"field": null` to DRF.
        explicitNulls = false
    }

    private val authInterceptor = AuthInterceptor(tokenStorage)
    private val languageInterceptor = LanguageInterceptor(languageProvider)

    private val clientConfig: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) {
            json(this@TrainingManagerApi.json)
        }
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    AppLogger.debug(tag, message)
                }
            }
            level = if (enableLogging) LogLevel.INFO else LogLevel.NONE
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        install(authInterceptor.plugin)
        install(languageInterceptor.plugin)
    }

    private val client = if (engine != null) HttpClient(engine, clientConfig) else HttpClient(clientConfig)

    override fun close() {
        client.close()
    }

    var onAuthFailure: (() -> Unit)?
        get() = authInterceptor.onAuthFailure
        set(value) { authInterceptor.onAuthFailure = value }

    // --- Auth ---
    suspend fun login(request: VerifiedTokenObtainPairRequest): Result<TokenObtainPairResponse> = apiCall {
        client.post("auth/token/") { setBody(request) }
    }

    suspend fun refresh(request: TokenRefreshRequest): Result<TokenRefresh> = apiCall {
        client.post("auth/token/refresh/") { setBody(request) }
    }

    suspend fun getMe(): Result<Me> = apiCall {
        client.get("me/")
    }

    suspend fun patchMe(body: PatchedMeRequest): Result<Me> = apiCall {
        client.patch("me/") { setBody(body) }
    }

    suspend fun changePassword(body: PasswordChangeRequest): Result<Unit> = apiCall {
        client.post("auth/password/change/") { setBody(body) }
    }

    suspend fun logout(refreshToken: String): Result<Unit> = runCatching {
        val response = client.post("auth/logout/") { setBody(TokenRefreshRequest(refreshToken)) }
        logResponse("logout", response)
        // A 401 here means the session is already gone server-side; we're tearing
        // it down anyway, so don't turn logout into a scary error. Other non-2xx
        // (5xx) still surface.
        if (!response.status.isSuccess() && response.status != HttpStatusCode.Unauthorized) {
            throw response.toApiException("logout")
        }
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun register(body: RegisterRequest): Result<Unit> = apiCall {
        client.post("auth/register/") { setBody(body) }
    }

    suspend fun requestPasswordReset(body: PasswordResetRequestRequest): Result<Unit> = apiCall {
        client.post("auth/password/reset/") { setBody(body) }
    }

    suspend fun magicLinkRequest(request: MagicLinkRequestRequest): Result<Unit> = apiCall {
        client.post("auth/magic-link/request/") { setBody(request) }
    }

    suspend fun magicLinkExchange(request: MagicLinkExchangeRequestRequest): Result<TokenObtainPairResponse> = apiCall {
        client.post("auth/magic-link/exchange/") { setBody(request) }
    }

    suspend fun confirmEmail(request: EmailConfirmRequest): Result<TokenObtainPairResponse> = apiCall {
        client.post("auth/email/confirm/") { setBody(request) }
    }

    suspend fun confirmPasswordReset(request: PasswordResetConfirmRequest): Result<TokenObtainPairResponse> = apiCall {
        client.post("auth/password/reset/confirm/") { setBody(request) }
    }

    suspend fun lookupInvitation(token: String): Result<ValidateInvitation> = apiCall {
        client.get("invitations/lookup/$token/")
    }

    suspend fun completeInvitation(token: String, request: CompleteInvitationRequest): Result<TokenObtainPairResponse> = apiCall {
        client.post("invitations/lookup/$token/") { setBody(request) }
    }

    suspend fun getDashboard(): Result<DashboardSummary> = apiCall {
        client.get("dashboard/summary/")
    }

    suspend fun listEvents(dateGte: String? = null, page: Int? = null): Result<PaginatedEventList> = apiCall {
        client.get("events/") {
            parameter("page_size", LIST_PAGE_SIZE)
            dateGte?.let { parameter("date__gte", it) }
            page?.let { parameter("page", it) }
        }
    }

    suspend fun getEvent(id: Int): Result<Event> = apiCall {
        client.get("events/$id/")
    }

    suspend fun getRsvp(eventId: Int): Result<RsvpSummary> = apiCall {
        client.get("events/$eventId/rsvp/")
    }

    suspend fun setRsvp(eventId: Int, body: RsvpUpsertRequest): Result<RsvpSummary> = apiCall {
        client.put("events/$eventId/rsvp/") { setBody(body) }
    }

    suspend fun setRoti(eventId: Int, body: RotiUpsertRequest): Result<RotiSummary> = apiCall {
        client.put("events/$eventId/roti/") { setBody(body) }
    }

    suspend fun listAttachments(targetType: String, targetId: Int): Result<PaginatedAttachmentList> = apiCall {
        client.get("attachments/") {
            parameter("page_size", LIST_PAGE_SIZE)
            parameter("target_type", targetType)
            parameter("target_id", targetId)
        }
    }

    suspend fun attachmentDownloadUrl(id: Int): Result<AttachmentDownloadResponse> = apiCall {
        client.get("attachments/$id/download/")
    }

    suspend fun getTeam(id: Int): Result<Team> = apiCall {
        client.get("teams/$id/")
    }

    suspend fun listMembers(): Result<PaginatedMemberList> = apiCall {
        client.get("members/") {
            parameter("page_size", LIST_PAGE_SIZE)
        }
    }

    suspend fun registerDevice(body: DeviceRegisterRequest): Result<Unit> = apiCall {
        client.post("devices/register/") { setBody(body) }
    }

    suspend fun unregisterDevice(body: DeviceUnregisterRequest): Result<Unit> = apiCall {
        client.post("devices/unregister/") { setBody(body) }
    }

    suspend fun listNotifications(): Result<PaginatedNotificationList> = apiCall {
        client.get("notifications/") {
            parameter("page_size", LIST_PAGE_SIZE)
        }
    }

    suspend fun markNotificationRead(id: Int): Result<Unit> = apiCall {
        client.post("notifications/$id/read/")
    }

    suspend fun markAllNotificationsRead(): Result<Unit> = apiCall {
        client.post("notifications/read_all/")
    }

    suspend fun listTopics(teamId: Int): Result<PaginatedTopicList> = apiCall {
        client.get("teams/$teamId/topics/") {
            parameter("page_size", LIST_PAGE_SIZE)
        }
    }

    suspend fun listMessages(teamId: Int, topicId: Int): Result<PaginatedTopicMessageList> = apiCall {
        client.get("teams/$teamId/topics/$topicId/messages/") {
            parameter("page_size", LIST_PAGE_SIZE)
        }
    }

    suspend fun postMessage(teamId: Int, topicId: Int, body: TopicMessageRequest): Result<TopicMessage> = apiCall {
        client.post("teams/$teamId/topics/$topicId/messages/") { setBody(body) }
    }

    suspend fun deleteMessage(teamId: Int, topicId: Int, messageId: Int): Result<Unit> = apiCall {
        client.delete("teams/$teamId/topics/$topicId/messages/$messageId/")
    }

    // --- Helpers ---
    // Every authenticated request must go through [apiCall] so a recoverable
    // expired session transparently refreshes instead of hard-failing.
    private suspend inline fun <reified T> apiCall(
        crossinline block: suspend () -> HttpResponse,
    ): Result<T> = runCatching<T> {
        val name = T::class.simpleName ?: "unknown"
        // Token on the request we're about to make — passed to the refresh guard
        // so concurrent 401s don't each re-POST auth/token/refresh/.
        val staleAccessToken = tokenStorage.getAccessToken()
        val response = block()
        logResponse(name, response)
        if (response.status == HttpStatusCode.Unauthorized) {
            AppLogger.warn(tag, "Unauthorized response received, attempting token refresh")
            if (!authInterceptor.refreshIfNeeded(client, staleAccessToken)) {
                throw response.toApiException("auth $name")
            }
            // Replay the ORIGINAL request: same verb + body, fresh token attached
            // by the AuthInterceptor's onRequest.
            val retried = block()
            logResponse("retry $name", retried)
            if (!retried.status.isSuccess()) {
                throw retried.toApiException("retry $name")
            }
            retried.decodeBody<T>(name, json)
        } else {
            if (!response.status.isSuccess()) {
                throw response.toApiException(name)
            }
            response.decodeBody<T>(name, json)
        }
    }.onFailure { if (it is CancellationException) throw it }
    .recoverCatching { throwable ->
        // Surface transport failures as NetworkException so the UI can tell
        // "offline / timed out" apart from an HTTP or decoding error.
        throw when (throwable) {
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException ->
                NetworkException(NetworkErrorKind.TIMEOUT, "The request timed out.", throwable)
            is IOException ->
                NetworkException(NetworkErrorKind.OFFLINE, "Could not reach the server.", throwable)
            else -> throwable
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        // Log only safe, non-secret detail — never the raw response body or message
        // (ResponseDecodingException / ApiException embed up to 500 chars of body).
        val detail = when (e) {
            is ApiException -> "ApiException status=${e.statusCode}"
            is ResponseDecodingException -> "ResponseDecodingException status=${e.statusCode}"
            is NetworkException -> "NetworkException kind=${e.kind}"
            else -> e::class.simpleName ?: "error"
        }
        AppLogger.error(tag, "API call failed: $detail")
    }

    private fun logResponse(operation: String, response: HttpResponse) {
        AppLogger.info(
            tag,
            "$operation ${response.request.method.value} ${response.request.url.encodedPath} -> ${response.status.value}",
        )
    }

    private suspend fun HttpResponse.toApiException(operation: String): ApiException {
        val errorBody = runCatching { bodyAsText() }.getOrElse { if (it is CancellationException) throw it else "" }
        return ApiException(
            statusCode = status.value,
            operation = operation,
            responseBody = errorBody.take(500),
        )
    }
}

private suspend inline fun <reified T> HttpResponse.decodeBody(operation: String, json: Json): T {
    val rawBody = bodyAsText()
    // A 204 / empty body for a Unit-returning call is success, not a decode error.
    if (rawBody.isBlank() && T::class == Unit::class) {
        @Suppress("UNCHECKED_CAST")
        return Unit as T
    }
    return try {
        json.decodeFromString<T>(rawBody)
    } catch (cause: SerializationException) {
        throw ResponseDecodingException(
            operation = operation,
            statusCode = status.value,
            responseBody = rawBody.take(500),
            cause = cause,
        )
    }
}
