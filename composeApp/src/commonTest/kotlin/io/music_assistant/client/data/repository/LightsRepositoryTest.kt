package io.music_assistant.client.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LightsRepositoryTest {
    private data class Sent(val method: HttpMethod, val url: String, val body: String, val contentType: String?)

    private fun repo(sent: MutableList<Sent>, fail: Boolean = false) = LightsRepository(
        HttpClient(
            MockEngine { request ->
                sent += Sent(
                    request.method,
                    request.url.toString(),
                    request.body.toByteArray().decodeToString(),
                    request.body.contentType?.toString(),
                )
                if (fail) respondError(HttpStatusCode.NotFound) else respond("")
            },
        ) { expectSuccess = true },
        baseUrl = "http://192.168.1.231:8123/",
        poolLightsWebhookId = "pool-id",
        panelWebhookId = "panel-id",
    )

    @Test
    fun colorPostsPresetColorAndBrightnessToThePoolWebhook() = runTest {
        val sent = mutableListOf<Sent>()
        assertTrue(repo(sent).setPoolRoom(PoolRoomPreset.Color, "#1E50FF", 80).isSuccess)
        val req = sent.single()
        assertEquals(HttpMethod.Post, req.method)
        assertEquals("http://192.168.1.231:8123/api/webhook/pool-id", req.url)
        assertEquals("""{"preset":"color","color":"#1E50FF","brightness":80}""", req.body)
        assertEquals("application/json", req.contentType)
    }

    @Test
    fun partyAndOffSendOnlyThePreset() {
        assertEquals("""{"preset":"party"}""", LightsRepository.poolRoomBody(PoolRoomPreset.Party, "#FF0000", 50).toString())
        assertEquals("""{"preset":"off"}""", LightsRepository.poolRoomBody(PoolRoomPreset.Off, null, 50).toString())
    }

    @Test
    fun whiteClampsBrightnessAndDropsColor() {
        assertEquals("""{"preset":"white","brightness":10}""", LightsRepository.poolRoomBody(PoolRoomPreset.White, "#FF0000", 0).toString())
        assertEquals("""{"preset":"white","brightness":100}""", LightsRepository.poolRoomBody(PoolRoomPreset.White, null, 150).toString())
    }

    @Test
    fun panelSendsHomeAssistantsModeName() = runTest {
        val sent = mutableListOf<Sent>()
        assertTrue(repo(sent).setPanel(PanelMode.SportsTicker).isSuccess)
        assertEquals("http://192.168.1.231:8123/api/webhook/panel-id", sent.single().url)
        assertEquals("""{"mode":"Sports Ticker"}""", sent.single().body)
    }

    @Test
    fun unreachableHouseIsAFailureNotACrash() = runTest {
        assertTrue(repo(mutableListOf(), fail = true).setPanel(PanelMode.Off).isFailure)
        val notSetUp = LightsRepository(HttpClient(MockEngine { respond("") }), "", "", "")
        assertTrue(notSetUp.setPoolRoom(PoolRoomPreset.Off).isFailure)
    }
}
