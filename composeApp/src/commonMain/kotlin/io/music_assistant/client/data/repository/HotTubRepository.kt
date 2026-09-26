package io.music_assistant.client.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.music_assistant.client.branding.HomeAssistantConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class HaEntityState(
    val state: String,
    val attributes: Map<String, JsonElement> = emptyMap(),
)

data class HotTubStatus(
    val currentTempF: Double?,
    val targetTempF: Double?,
    val isHeating: Boolean,
    val isConnected: Boolean,
)

/**
 * Reads exactly two Home Assistant entities — a climate entity and a "connected" binary
 * sensor — and nothing else. See [HomeAssistantConfig] for why the scope stays this narrow:
 * the token this client carries should never be able to reveal, even in principle, anything
 * about the rest of the house.
 */
class HotTubRepository(
    private val client: HttpClient,
) {
    suspend fun fetchStatus(): Result<HotTubStatus> = runCatching {
        val climate = getState(HomeAssistantConfig.CLIMATE_ENTITY_ID)
        val connected = getState(HomeAssistantConfig.CONNECTED_ENTITY_ID)
        HotTubStatus(
            currentTempF = climate.attributes["current_temperature"]?.jsonPrimitive?.doubleOrNull,
            targetTempF = climate.attributes["temperature"]?.jsonPrimitive?.doubleOrNull,
            isHeating = climate.attributes["hvac_action"]?.jsonPrimitive?.content == "heating",
            isConnected = connected.state == "on",
        )
    }

    private suspend fun getState(entityId: String): HaEntityState =
        client.get("${HomeAssistantConfig.BASE_URL}/api/states/$entityId") {
            header("Authorization", "Bearer ${HomeAssistantConfig.LONG_LIVED_TOKEN}")
        }.body()
}
