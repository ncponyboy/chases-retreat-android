package io.music_assistant.client.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.music_assistant.client.branding.LightsConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The pool room presets the "Pool Room Lights - Local webhook" automation accepts. */
enum class PoolRoomPreset(val wire: String) { Party("party"), White("white"), Color("color"), Off("off") }

/**
 * The LED sign's modes, as the Home Assistant `input_select.wled_panel_mode` options spell them
 * ([wire]); [label] is what guests see.
 */
enum class PanelMode(val wire: String, val label: String) {
    SportsTicker("Sports Ticker", "Scores"),
    Welcome("Welcome", "Welcome"),
    MusicSync("Music Sync", "Music"),
    Clock("Clock", "Clock"),
    Fire("Fire", "Fire"),
    Off("Off", "Off"),
}

/**
 * Sends the Lights tab's buttons to the house's two local-only Home Assistant webhooks (see
 * [LightsConfig]). Fire-and-forget: Home Assistant answers every webhook call with an empty 200,
 * so success only means the house received it, and a failure almost always means the phone isn't
 * on the house Wi-Fi.
 */
class LightsRepository(
    private val client: HttpClient,
    private val baseUrl: String = LightsConfig.HA_LOCAL_URL,
    private val poolLightsWebhookId: String = LightsConfig.POOL_LIGHTS_WEBHOOK_ID,
    private val panelWebhookId: String = LightsConfig.PANEL_WEBHOOK_ID,
) {
    /** [colorHex] is "#RRGGBB" and only used with [PoolRoomPreset.Color]; [brightness] is 10-100. */
    suspend fun setPoolRoom(
        preset: PoolRoomPreset,
        colorHex: String? = null,
        brightness: Int? = null,
    ): Result<Unit> = send(poolLightsWebhookId, poolRoomBody(preset, colorHex, brightness))

    suspend fun setPanel(mode: PanelMode): Result<Unit> =
        send(panelWebhookId, JsonObject(mapOf("mode" to JsonPrimitive(mode.wire))))

    private suspend fun send(webhookId: String, body: JsonObject): Result<Unit> = runCatching {
        check(baseUrl.isNotBlank() && webhookId.isNotBlank()) { "Lights are not set up in this build" }
        client.post("${baseUrl.trimEnd('/')}/api/webhook/$webhookId") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        Unit
    }

    companion object {
        fun poolRoomBody(preset: PoolRoomPreset, colorHex: String?, brightness: Int?): JsonObject =
            JsonObject(
                buildMap {
                    put("preset", JsonPrimitive(preset.wire))
                    if (preset == PoolRoomPreset.Color && colorHex != null) put("color", JsonPrimitive(colorHex))
                    if ((preset == PoolRoomPreset.White || preset == PoolRoomPreset.Color) && brightness != null) {
                        put("brightness", JsonPrimitive(brightness.coerceIn(10, 100)))
                    }
                },
            )
    }
}
