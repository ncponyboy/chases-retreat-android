package io.music_assistant.client.data.repository

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FamilyCalendarRepositoryTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun MockRequestHandleScope.ok(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, json)

    private fun repo(
        settings: MapSettings = MapSettings(),
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = FamilyCalendarRepository(HttpClient(MockEngine { handler(it) }), settings) to settings

    @Test
    fun normalizesAddresses() {
        assertEquals("https://x.ui.nabu.casa", FamilyCalendarRepository.normalizeAddress(" x.ui.nabu.casa/ "))
        assertEquals("http://192.168.1.5:8123", FamilyCalendarRepository.normalizeAddress("http://192.168.1.5:8123"))
        assertEquals(null, FamilyCalendarRepository.normalizeAddress("  "))
        assertEquals(null, FamilyCalendarRepository.normalizeAddress("not an address"))
    }

    @Test
    fun signInStoresRefreshTokenButNeverPassword() = runTest {
        val (repo, settings) = repo {
            when (it.url.encodedPath) {
                "/auth/login_flow" -> ok("""{"type":"form","flow_id":"f1","step_id":"init","errors":{}}""")
                "/auth/login_flow/f1" -> ok("""{"type":"create_entry","result":"authcode"}""")
                "/auth/token" -> ok("""{"access_token":"acc","refresh_token":"ref","token_type":"Bearer"}""")
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        assertFalse(repo.isSignedIn)
        assertEquals(FamilyLoginResult.Success, repo.signIn("home.example.com", "mom", "hunter2"))
        assertTrue(repo.isSignedIn)
        assertEquals("home.example.com", repo.serverLabel)
        assertEquals("ref", settings.getStringOrNull("family_calendar_refresh_token"))
        assertFalse(settings.keys.any { settings.getStringOrNull(it) == "hunter2" })
    }

    @Test
    fun wrongPasswordIsAFriendlyFailure() = runTest {
        val (repo, _) = repo {
            when (it.url.encodedPath) {
                "/auth/login_flow" -> ok("""{"type":"form","flow_id":"f1","step_id":"init"}""")
                else -> ok("""{"type":"form","flow_id":"f1","step_id":"init","errors":{"base":"invalid_auth"}}""")
            }
        }
        val result = repo.signIn("home.example.com", "mom", "wrong")
        assertEquals(FamilyLoginResult.Failure("Wrong username or password."), result)
        assertFalse(repo.isSignedIn)
    }

    @Test
    fun twoFactorAsksForCodeThenSignsIn() = runTest {
        var loginPosts = 0
        val (repo, _) = repo {
            when (it.url.encodedPath) {
                "/auth/login_flow" -> ok("""{"type":"form","flow_id":"f1","step_id":"init"}""")
                "/auth/login_flow/f1" -> {
                    loginPosts++
                    if (loginPosts == 1) ok("""{"type":"form","flow_id":"f1","step_id":"mfa","errors":{}}""")
                    else ok("""{"type":"create_entry","result":"authcode"}""")
                }
                "/auth/token" -> ok("""{"access_token":"acc","refresh_token":"ref"}""")
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        assertEquals(FamilyLoginResult.NeedsCode, repo.signIn("home.example.com", "mom", "pw"))
        assertFalse(repo.isSignedIn)
        assertEquals(FamilyLoginResult.Success, repo.submitCode("123456"))
        assertTrue(repo.isSignedIn)
    }

    @Test
    fun aNonHomeAssistantAddressIsRejected() = runTest {
        val (repo, _) = repo { respond("<html/>", HttpStatusCode.NotFound) }
        val result = repo.signIn("example.com", "mom", "pw")
        assertIs<FamilyLoginResult.Failure>(result)
        assertFalse(repo.isSignedIn)
    }

    private fun signedInSettings() = MapSettings(
        "family_calendar_base_url" to "https://home.example.com",
        "family_calendar_refresh_token" to "ref",
    )

    @Test
    fun readsCalendarsAndParsesTimedAndAllDayEvents() = runTest {
        val (repo, _) = repo(signedInSettings()) {
            when (it.url.encodedPath) {
                "/auth/token" -> ok("""{"access_token":"acc"}""")
                "/api/calendars" -> ok("""[{"entity_id":"calendar.family","name":"Family"}]""")
                "/api/calendars/calendar.family" -> {
                    assertEquals("Bearer acc", it.headers[HttpHeaders.Authorization])
                    assertNotNull(it.url.parameters["start"])
                    assertNotNull(it.url.parameters["end"])
                    ok(
                        """[
                        {"summary":"Dinner","start":{"dateTime":"2026-09-26T18:30:00"},"end":{"dateTime":"2026-09-26T20:00:00"},"location":"Grandma's"},
                        {"summary":"Beach week","start":{"date":"2026-10-03"},"end":{"date":"2026-10-08"}},
                        {"summary":"Old style","start":"2026-11-01","end":"2026-11-02"}
                        ]""",
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        // "Family" doesn't match the house calendar's name, so it's not one the app shows.
        assertEquals(emptyList(), repo.calendars())

        val events = repo.events("calendar.family", LocalDate(2026, 9, 25), LocalDate(2026, 12, 1))
        assertEquals(3, events.size)

        val dinner = events[0]
        assertEquals("Dinner", dinner.title)
        assertEquals(LocalDate(2026, 9, 26), dinner.startDate)
        assertEquals(LocalTime(18, 30), dinner.startTime)
        assertEquals(LocalTime(20, 0), dinner.endTime)
        assertEquals("Grandma's", dinner.location)

        // Home Assistant's all-day end date is exclusive: Oct 3 → Oct 8 covers the 3rd through the 7th.
        val beach = events[1]
        assertEquals(LocalDate(2026, 10, 3), beach.startDate)
        assertEquals(LocalDate(2026, 10, 7), beach.endDate)
        assertEquals(null, beach.startTime)

        assertEquals(LocalDate(2026, 11, 1), events[2].endDate)
    }

    @Test
    fun aRevokedSignInSignsOutAndTellsTheCaller() = runTest {
        val settings = signedInSettings()
        val (repo, _) = repo(settings) {
            when (it.url.encodedPath) {
                "/auth/token" -> ok("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        assertTrue(repo.isSignedIn)
        assertFailsWith<FamilySessionExpiredException> { repo.calendars() }
        assertFalse(repo.isSignedIn)
        assertEquals(null, settings.getStringOrNull("family_calendar_refresh_token"))
    }

    @Test
    fun anExpiredAccessTokenIsRefreshedOnce() = runTest {
        var calendarCalls = 0
        var tokenCalls = 0
        val (repo, _) = repo(signedInSettings()) {
            when (it.url.encodedPath) {
                "/auth/token" -> {
                    tokenCalls++
                    ok("""{"access_token":"acc$tokenCalls"}""")
                }
                "/api/calendars" -> {
                    calendarCalls++
                    if (it.headers[HttpHeaders.Authorization] == "Bearer acc1") {
                        respond("", HttpStatusCode.Unauthorized)
                    } else {
                        ok("""[{"entity_id":"calendar.chase_s_retreat","name":"Chase's retreat"}]""")
                    }
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        assertEquals(1, repo.calendars().size)
        assertEquals(2, tokenCalls)
        assertEquals(2, calendarCalls)
    }

    @Test
    fun signOutRevokesAndForgets() = runTest {
        var revoked: HttpMethod? = null
        val settings = signedInSettings()
        val (repo, _) = repo(settings) {
            if (it.url.encodedPath == "/auth/revoke") revoked = it.method
            ok("{}")
        }
        repo.signOut()
        assertFalse(repo.isSignedIn)
        assertEquals(HttpMethod.Post, revoked)
    }

    private val allCalendars = """[
        {"entity_id":"calendar.west_jefferson","name":"West Jefferson"},
        {"entity_id":"calendar.jaime_sang","name":"Jaime & sang"},
        {"entity_id":"calendar.sangmenon1_gmail_com","name":"Sangmenon1@gmail.com"},
        {"entity_id":"calendar.birthdays","name":"Birthdays"},
        {"entity_id":"calendar.chase_s_retreat_2","name":"Chase's retreat"},
        {"entity_id":"calendar.chase_s_retreat","name":"Chase's retreat"}
    ]"""

    @Test
    fun onlyTheHouseCalendarIsShownAndPersonalOnesAreHidden() = runTest {
        val (repo, _) = repo(signedInSettings()) {
            when (it.url.encodedPath) {
                "/auth/token" -> ok("""{"access_token":"acc"}""")
                "/api/calendars" -> ok(allCalendars)
                // The duplicate _2 copy can't take new events; the original can.
                "/api/states/calendar.chase_s_retreat" -> ok("""{"attributes":{"supported_features":3}}""")
                "/api/states/calendar.chase_s_retreat_2" -> ok("""{"attributes":{}}""")
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val calendars = repo.calendars()
        assertEquals(listOf(FamilyCalendar("calendar.chase_s_retreat", "Chase's retreat", canCreate = true)), calendars)
    }

    @Test
    fun createsAllDayStaysWithHomeAssistantsExclusiveEndDate() = runTest {
        var body: String? = null
        var path: String? = null
        val (repo, _) = repo(signedInSettings()) { request ->
            when (request.url.encodedPath) {
                "/auth/token" -> ok("""{"access_token":"acc"}""")
                "/api/services/calendar/create_event" -> {
                    path = request.url.encodedPath
                    body = (request.body as io.ktor.http.content.TextContent).text
                    assertEquals("Bearer acc", request.headers[HttpHeaders.Authorization])
                    ok("[]")
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        repo.createAllDayEvent(
            "calendar.chase_s_retreat", "  Sam's family ", LocalDate(2026, 10, 20), LocalDate(2026, 10, 22), " bring dogs ",
        )
        assertEquals("/api/services/calendar/create_event", path)
        val sent = kotlinx.serialization.json.Json.parseToJsonElement(body!!).let { it as kotlinx.serialization.json.JsonObject }
        assertEquals("calendar.chase_s_retreat", (sent["entity_id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("Sam's family", (sent["summary"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("2026-10-20", (sent["start_date"] as kotlinx.serialization.json.JsonPrimitive).content)
        // Last night is the 22nd, so Home Assistant's exclusive end is the 23rd.
        assertEquals("2026-10-23", (sent["end_date"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("bring dogs", (sent["description"] as kotlinx.serialization.json.JsonPrimitive).content)
    }
}
