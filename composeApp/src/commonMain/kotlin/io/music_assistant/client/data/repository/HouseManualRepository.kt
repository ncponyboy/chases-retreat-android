package io.music_assistant.client.data.repository

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.music_assistant.client.utils.myJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

/** One line within a [HouseManualSection]. [emphasized] calls out something safety-critical. */
@Serializable
data class HouseManualItem(
    val text: String,
    val emphasized: Boolean = false,
)

/** [hotTubStatus] marks where the live hot tub status card is shown (right after that section). */
@Serializable
data class HouseManualSection(
    val title: String,
    val items: List<HouseManualItem> = emptyList(),
    val hotTubStatus: Boolean = false,
)

@Serializable
private data class HouseManualDocument(val sections: List<HouseManualSection> = emptyList())

/**
 * The House Manual text lives in Firestore (`houseManual/main`, edited with the Chases Retreat
 * Manager site), so it can be changed without shipping an app update. The document has one
 * string field, `json`, holding `{"sections":[...]}`; Firestore's public REST API serves it.
 * The last good copy is cached on the device; if it can't be fetched (no signal at the
 * property) the cache is used, and before the first successful fetch the bundled
 * [defaultHouseManualSections] are shown.
 */
class HouseManualRepository(
    private val client: HttpClient,
    private val settings: Settings,
    private val url: String = HOUSE_MANUAL_URL,
) {
    fun cached(): List<HouseManualSection>? =
        settings.getStringOrNull(CACHE_KEY)?.let(::parse)

    suspend fun fetch(): Result<List<HouseManualSection>> = runCatching {
        val response: String = client.get(url) {
            // Defeats any intermediary cache so edits show up right away.
            parameter("t", Clock.System.now().toEpochMilliseconds())
        }.body()
        val text = extractJson(response) ?: error("house manual document has no json field")
        val sections = parse(text) ?: error("house manual has no usable sections")
        settings.putString(CACHE_KEY, text)
        sections
    }

    companion object {
        const val HOUSE_MANUAL_URL =
            "https://firestore.googleapis.com/v1/projects/chases-retreat/databases/(default)/documents/houseManual/main"
        private const val CACHE_KEY = "house_manual_json"

        /** Pulls the `json` string field out of a Firestore REST document response. */
        fun extractJson(response: String): String? = runCatching {
            myJson.parseToJsonElement(response).jsonObject["fields"]
                ?.jsonObject?.get("json")?.jsonObject?.get("stringValue")?.jsonPrimitive?.content
        }.getOrNull()

        /** Null when [text] isn't valid or has no section with a title and at least one line. */
        fun parse(text: String): List<HouseManualSection>? = runCatching {
            myJson.decodeFromString<HouseManualDocument>(text).sections
                .filter { it.title.isNotBlank() && it.items.isNotEmpty() }
                .takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
