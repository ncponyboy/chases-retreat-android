package io.music_assistant.client.data.repository

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

/** A calendar entity on the family's Home Assistant (e.g. `calendar.family`). */
data class FamilyCalendar(val entityId: String, val name: String, val canCreate: Boolean = false)

/**
 * Which of the house's calendars this app is allowed to show. Home Assistant lists every calendar
 * it has — including people's personal ones — to any signed-in user, so the app must pick the
 * family calendar itself rather than showing whatever comes back.
 */
object FamilyCalendarConfig {
    /** Only calendars whose name contains this (case-insensitive) are shown: "Chase's retreat". */
    const val CALENDAR_NAME_KEYWORD = "retreat"

    /** Bit for "can create events" in a Home Assistant calendar's `supported_features`. */
    const val FEATURE_CREATE_EVENT = 1
}

/**
 * One event as returned by Home Assistant, with times already converted to the phone's local
 * time zone. All-day events have null times; [endDate] is inclusive for both kinds.
 */
data class FamilyEvent(
    val calendarId: String,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val location: String?,
    val description: String?,
)

sealed interface FamilyLoginResult {
    data object Success : FamilyLoginResult

    /** The account has two-factor login on; call [FamilyCalendarRepository.submitCode]. */
    data object NeedsCode : FamilyLoginResult

    data class Failure(val message: String) : FamilyLoginResult
}

/** The saved sign-in is no longer valid (revoked, or the server can't be reached to refresh). */
class FamilySessionExpiredException : Exception("Signed out")

/**
 * Talks to the family's own Home Assistant — signing in as the person's own Home Assistant user
 * and reading its calendars. The address is typed in by the family member, never bundled in the
 * app, so nothing about the house's server is discoverable from the public store build.
 *
 * Only a refresh token and the server address are kept, in the secrets store (excluded from
 * platform backups). The password is used once to sign in and never saved. Access tokens live in
 * memory only.
 *
 * The app only ever requests calendar endpoints. Note that Home Assistant does not restrict a
 * non-admin user's token to calendars — that's a limit of this client, not of the account — so
 * every family member's account should stay non-admin and be revoked (Profile → Security) if a
 * phone is lost.
 */
class FamilyCalendarRepository(
    private val client: HttpClient,
    private val secrets: Settings,
) {
    private var accessToken: String? = null
    private var pendingFlowId: String? = null
    private var pendingBaseUrl: String? = null

    val isSignedIn: Boolean
        get() = !secrets.getStringOrNull(KEY_REFRESH_TOKEN).isNullOrBlank() && savedBaseUrl != null

    private val savedBaseUrl: String?
        get() = secrets.getStringOrNull(KEY_BASE_URL)?.takeIf { it.isNotBlank() }

    /** The server the person is signed in to, without the scheme — for display. */
    val serverLabel: String?
        get() = savedBaseUrl?.substringAfter("://")

    suspend fun signIn(address: String, username: String, password: String): FamilyLoginResult {
        val baseUrl = normalizeAddress(address)
            ?: return FamilyLoginResult.Failure("Enter your Home Assistant address, like example.ui.nabu.casa")
        return try {
            val start = client.post("$baseUrl/auth/login_flow") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("client_id", CLIENT_ID)
                        put("redirect_uri", REDIRECT_URI)
                        put("handler", buildJsonArray { add(JsonPrimitive("homeassistant")); add(JsonNull) })
                    }.toString(),
                )
            }
            if (!start.status.isSuccess()) return FamilyLoginResult.Failure(notHomeAssistantMessage(start))
            val flowId = start.json()["flow_id"].string()
                ?: return FamilyLoginResult.Failure(notHomeAssistantMessage(start))

            val step = client.post("$baseUrl/auth/login_flow/$flowId") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("client_id", CLIENT_ID)
                        put("username", username.trim())
                        put("password", password)
                    }.toString(),
                )
            }
            handleLoginStep(baseUrl, flowId, step)
        } catch (e: Exception) {
            FamilyLoginResult.Failure("Couldn't reach that address. Check it and your connection, then try again.")
        }
    }

    suspend fun submitCode(code: String): FamilyLoginResult {
        val baseUrl = pendingBaseUrl
        val flowId = pendingFlowId
        if (baseUrl == null || flowId == null) return FamilyLoginResult.Failure("Please sign in again.")
        return try {
            val step = client.post("$baseUrl/auth/login_flow/$flowId") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("client_id", CLIENT_ID)
                        put("code", code.trim())
                    }.toString(),
                )
            }
            handleLoginStep(baseUrl, flowId, step)
        } catch (e: Exception) {
            FamilyLoginResult.Failure("Couldn't reach that address. Check your connection and try again.")
        }
    }

    private suspend fun handleLoginStep(baseUrl: String, flowId: String, response: HttpResponse): FamilyLoginResult {
        val body = response.json()
        return when (body["type"].string()) {
            "create_entry" -> {
                val code = body["result"].string()
                    ?: return FamilyLoginResult.Failure("Sign-in didn't complete. Please try again.")
                exchangeCode(baseUrl, code)
            }

            "form" -> {
                val errors = body["errors"] as? JsonObject
                val stepId = body["step_id"].string()
                when {
                    errors?.get("base").string() == "invalid_auth" ->
                        FamilyLoginResult.Failure("Wrong username or password.")

                    errors?.get("base").string() == "invalid_code" ->
                        FamilyLoginResult.Failure("That code didn't work. Wait for the next one and try again.")

                    errors?.get("base").string() == "too_many_attempts" ->
                        FamilyLoginResult.Failure("Too many attempts. Wait a few minutes and try again.")

                    stepId == "mfa" -> {
                        pendingBaseUrl = baseUrl
                        pendingFlowId = flowId
                        FamilyLoginResult.NeedsCode
                    }

                    else -> FamilyLoginResult.Failure("Sign-in failed. Check your details and try again.")
                }
            }

            else -> FamilyLoginResult.Failure("Sign-in failed. Check your details and try again.")
        }
    }

    private suspend fun exchangeCode(baseUrl: String, code: String): FamilyLoginResult {
        val response = client.submitForm(
            url = "$baseUrl/auth/token",
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("client_id", CLIENT_ID)
            },
        )
        val body = response.json()
        val access = body["access_token"].string()
        val refresh = body["refresh_token"].string()
        if (!response.status.isSuccess() || access == null || refresh == null) {
            return FamilyLoginResult.Failure("Sign-in didn't complete. Please try again.")
        }
        pendingFlowId = null
        pendingBaseUrl = null
        accessToken = access
        secrets.putString(KEY_BASE_URL, baseUrl)
        secrets.putString(KEY_REFRESH_TOKEN, refresh)
        return FamilyLoginResult.Success
    }

    /** Signs out on this phone and revokes the saved sign-in on the server (best effort). */
    suspend fun signOut() {
        val baseUrl = savedBaseUrl
        val refresh = secrets.getStringOrNull(KEY_REFRESH_TOKEN)
        clearSession()
        if (baseUrl != null && !refresh.isNullOrBlank()) {
            runCatching {
                client.submitForm(
                    url = "$baseUrl/auth/revoke",
                    formParameters = parameters { append("token", refresh) },
                )
            }
        }
    }

    /**
     * The calendars this app may show (see [FamilyCalendarConfig]). Home Assistant can list the
     * same calendar twice when it's shared to two linked accounts; those are collapsed to one,
     * preferring the copy that can accept new events.
     */
    suspend fun calendars(): List<FamilyCalendar> {
        val body = authorizedGet("/api/calendars")
        val allowed = Json.parseToJsonElement(body).jsonArray.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["entity_id"].string() ?: return@mapNotNull null
            val name = obj["name"].string() ?: id.substringAfter('.').replace('_', ' ')
            if (!name.contains(FamilyCalendarConfig.CALENDAR_NAME_KEYWORD, ignoreCase = true)) return@mapNotNull null
            FamilyCalendar(id, name, canCreate = supportsCreate(id))
        }
        return allowed
            .groupBy { it.name.lowercase() }
            .values
            .map { copies -> copies.firstOrNull { it.canCreate } ?: copies.first() }
    }

    private suspend fun supportsCreate(entityId: String): Boolean = runCatching {
        val features = (Json.parseToJsonElement(authorizedGet("/api/states/$entityId")).jsonObject["attributes"] as? JsonObject)
            ?.get("supported_features")?.let { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() } ?: 0
        features and FamilyCalendarConfig.FEATURE_CREATE_EVENT != 0
    }.getOrDefault(false)

    /**
     * Adds an all-day event (or a multi-day block, like a stay) to [calendarId]. [lastDay] is the
     * final day included; Home Assistant wants the day after.
     */
    suspend fun createAllDayEvent(
        calendarId: String,
        title: String,
        firstDay: LocalDate,
        lastDay: LocalDate,
        notes: String?,
    ) {
        val body = buildJsonObject {
            put("entity_id", calendarId)
            put("summary", title.trim())
            put("start_date", firstDay.toString())
            put("end_date", lastDay.plus(1, DateTimeUnit.DAY).toString())
            if (!notes.isNullOrBlank()) put("description", notes.trim())
        }.toString()
        authorizedPost("/api/services/calendar/create_event", body)
    }

    suspend fun events(calendarId: String, from: LocalDate, until: LocalDate): List<FamilyEvent> {
        val zone = TimeZone.currentSystemDefault()
        val start = from.atStartOfDayIn(zone).toString()
        val end = until.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toString()
        val body = authorizedGet("/api/calendars/$calendarId") {
            parameter("start", start)
            parameter("end", end)
        }
        return (Json.parseToJsonElement(body) as? JsonArray).orEmpty().mapNotNull { item ->
            parseEvent(calendarId, item as? JsonObject ?: return@mapNotNull null, zone)
        }
    }

    // ---- HTTP with automatic token refresh ----

    private suspend fun authorizedGet(path: String, block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}): String =
        authorized(path) { url, token ->
            client.get(url) {
                header("Authorization", "Bearer $token")
                block()
            }
        }

    private suspend fun authorizedPost(path: String, jsonBody: String): String =
        authorized(path) { url, token ->
            client.post(url) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }
        }

    /** Runs a request with the current access token, refreshing it and retrying once on a 401. */
    private suspend fun authorized(path: String, request: suspend (url: String, token: String) -> HttpResponse): String {
        val baseUrl = savedBaseUrl ?: throw FamilySessionExpiredException()
        if (accessToken == null) refreshAccessToken(baseUrl)
        var response = request("$baseUrl$path", accessToken.orEmpty())
        if (response.status == HttpStatusCode.Unauthorized) {
            refreshAccessToken(baseUrl)
            response = request("$baseUrl$path", accessToken.orEmpty())
        }
        if (!response.status.isSuccess()) error("Server returned ${response.status.value}")
        return response.bodyAsText()
    }

    private suspend fun refreshAccessToken(baseUrl: String) {
        val refresh = secrets.getStringOrNull(KEY_REFRESH_TOKEN)
        if (refresh.isNullOrBlank()) throw FamilySessionExpiredException()
        val response = client.submitForm(
            url = "$baseUrl/auth/token",
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refresh)
                append("client_id", CLIENT_ID)
            },
        )
        when {
            response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.Unauthorized -> {
                // Revoked or removed on the server — the saved sign-in is dead.
                clearSession()
                throw FamilySessionExpiredException()
            }

            !response.status.isSuccess() -> error("Server returned ${response.status.value}")
        }
        accessToken = response.json()["access_token"].string() ?: error("No access token")
    }

    private fun clearSession() {
        accessToken = null
        pendingFlowId = null
        pendingBaseUrl = null
        secrets.remove(KEY_REFRESH_TOKEN)
        secrets.remove(KEY_BASE_URL)
    }

    // ---- Parsing ----

    private fun parseEvent(calendarId: String, obj: JsonObject, zone: TimeZone): FamilyEvent? {
        val startRaw = obj["start"].timeString() ?: return null
        val endRaw = obj["end"].timeString() ?: startRaw
        val title = obj["summary"].string()?.takeIf { it.isNotBlank() } ?: "(No title)"
        val location = obj["location"].string()?.takeIf { it.isNotBlank() }
        val description = obj["description"].string()?.takeIf { it.isNotBlank() }

        // All-day events are plain dates and Home Assistant's end date is exclusive.
        if (!startRaw.contains('T')) {
            val start = runCatching { LocalDate.parse(startRaw) }.getOrNull() ?: return null
            val endExclusive = runCatching { LocalDate.parse(endRaw) }.getOrNull() ?: start.plus(1, DateTimeUnit.DAY)
            val lastDay = endExclusive.plus(-1, DateTimeUnit.DAY).let { if (it < start) start else it }
            return FamilyEvent(calendarId, title, start, lastDay, null, null, location, description)
        }

        val start = parseLocal(startRaw, zone) ?: return null
        val end = parseLocal(endRaw, zone) ?: start
        // An event ending exactly at midnight belongs to the day before.
        val lastDay = if (end.time == LocalTime(0, 0) && end.date > start.date) {
            end.date.plus(-1, DateTimeUnit.DAY)
        } else {
            end.date
        }
        return FamilyEvent(calendarId, title, start.date, lastDay, start.time, end.time, location, description)
    }

    private fun parseLocal(raw: String, zone: TimeZone): LocalDateTime? =
        runCatching { Instant.parse(raw).toLocalDateTime(zone) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(raw) }.getOrNull()

    /** Home Assistant sends `{"dateTime": ...}` / `{"date": ...}` or a bare string, by version. */
    private fun JsonElement?.timeString(): String? = when (this) {
        is JsonObject -> get("dateTime").string() ?: get("date").string()
        is JsonPrimitive -> contentOrNull
        else -> null
    }

    private fun JsonElement?.string(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

    private suspend fun HttpResponse.json(): JsonObject =
        runCatching { Json.parseToJsonElement(bodyAsText()).jsonObject }.getOrDefault(JsonObject(emptyMap()))

    private fun notHomeAssistantMessage(response: HttpResponse): String =
        if (response.status == HttpStatusCode.NotFound) {
            "That address doesn't look like a Home Assistant server."
        } else {
            "Couldn't sign in (${response.status.value}). Check the address and try again."
        }

    private fun LocalDate.atStartOfDayIn(zone: TimeZone): Instant = LocalDateTime(this, LocalTime(0, 0)).toInstant(zone)

    companion object {
        // Home Assistant identifies the app by a URL. It's only a label — it appears in the
        // person's Profile → Security list of signed-in apps — and is never fetched, because the
        // redirect URI shares its origin.
        private const val CLIENT_ID = "https://chasesretreat.app/"
        private const val REDIRECT_URI = "https://chasesretreat.app/callback"
        private const val KEY_BASE_URL = "family_calendar_base_url"
        private const val KEY_REFRESH_TOKEN = "family_calendar_refresh_token"

        /** "example.ui.nabu.casa" → "https://example.ui.nabu.casa"; keeps an explicit http://. */
        fun normalizeAddress(raw: String): String? {
            var value = raw.trim().trimEnd('/')
            if (value.isEmpty() || value.contains(' ')) return null
            if (!value.contains("://")) value = "https://$value"
            return value.takeIf { it.substringAfter("://").isNotBlank() }
        }
    }
}
