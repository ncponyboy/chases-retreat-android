package io.music_assistant.client.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.music_assistant.client.stargazing.MoonPhase
import io.music_assistant.client.stargazing.PROPERTY_TIME_ZONE
import io.music_assistant.client.stargazing.SkyEvent
import io.music_assistant.client.stargazing.SkySpecialType
import io.music_assistant.client.stargazing.StargazingStatus
import io.music_assistant.client.stargazing.upcomingMeteorShowers
import io.music_assistant.client.stargazing.upcomingSkySpecials
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant

private const val PROPERTY_LAT = 36.5504
private const val PROPERTY_LON = -81.28871
private const val PROPERTY_ALT_M = 841

/** Minimum culmination elevation to count a pass as worth mentioning — low passes are easily
 *  lost behind the ridgelines around the property. */
private const val MIN_ISS_ELEVATION_DEG = 20.0

/** G-scale (NOAA's geomagnetic storm scale) thresholds — see [io.music_assistant.client.stargazing]
 *  research notes: this latitude needs G3 (Kp 7) for even a camera-only chance, G4 (Kp 8) for
 *  anything eye-visible. Don't lower these; they're not guesses. */
private const val AURORA_CAMERA_CHANCE_SCALE = 3
private const val AURORA_VISIBLE_CHANCE_SCALE = 4

@Serializable
private data class IssPassResponse(val passes: List<IssPass> = emptyList())

@Serializable
private data class IssPass(
    val rise: IssPassPoint,
    val culmination: IssPassCulmination = IssPassCulmination(),
    val visible: Boolean = false,
)

@Serializable
private data class IssPassPoint(val time: String, val compass: String = "")

@Serializable
private data class IssPassCulmination(val elevation_deg: Double = 0.0)

@Serializable
private data class NoaaScaleValue(val Scale: String = "0")

@Serializable
private data class NoaaScaleDay(val G: NoaaScaleValue? = null)

/**
 * Combines a handful of independent sky-event sources into one "tonight and the next few
 * nights" status. The moon phase and static calendars never fail (no network involved); the ISS
 * and NOAA calls are individually best-effort — if either is unreachable, its events are simply
 * omitted rather than breaking the rest of the card.
 */
class StargazingRepository(
    private val client: HttpClient,
) {
    suspend fun fetchStatus(): StargazingStatus {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(PROPERTY_TIME_ZONE).date

        val phase = MoonPhase.phaseFraction(now)
        val events = mutableListOf<SkyEvent>()

        upcomingMeteorShowers(today).forEach { (shower, peak) ->
            val whenText = if (peak == today) "peaks tonight" else "peaks ${peak.dayOfWeek.displayName()}"
            events += SkyEvent(
                title = "${shower.name} meteor shower",
                detail = "Best after midnight, away from the house lights — $whenText.",
            )
        }

        upcomingSkySpecials(today).forEach { special ->
            val label = when (special.type) {
                SkySpecialType.SUPERMOON -> "Supermoon"
                SkySpecialType.LUNAR_ECLIPSE_TOTAL -> "Total lunar eclipse"
                SkySpecialType.LUNAR_ECLIPSE_PARTIAL -> "Partial lunar eclipse"
                SkySpecialType.LUNAR_ECLIPSE_PENUMBRAL -> "Lunar eclipse (subtle)"
            }
            val whenText = if (special.date == today) "tonight" else "on ${special.date}"
            events += SkyEvent(title = label, detail = "Visible $whenText.")
        }

        fetchIssPasses().getOrNull()?.let(events::addAll)
        fetchAuroraAlert().getOrNull()?.let(events::add)

        return StargazingStatus(
            moonPhaseName = MoonPhase.phaseName(phase),
            moonIlluminationPercent = (MoonPhase.illumination(phase) * 100).roundToInt(),
            moonSkyNote = MoonPhase.skyNote(phase),
            events = events,
        )
    }

    private suspend fun fetchIssPasses(): Result<List<SkyEvent>> = runCatching {
        val response: IssPassResponse = client.get("https://iss-api.polluxlabs.io/iss-pass") {
            parameter("lat", PROPERTY_LAT)
            parameter("lon", PROPERTY_LON)
            parameter("alt", PROPERTY_ALT_M)
            parameter("n", 10)
            parameter("visible_only", true)
            parameter("days_ahead", 3)
        }.body()

        response.passes
            .filter { it.visible && it.culmination.elevation_deg >= MIN_ISS_ELEVATION_DEG }
            .take(1)
            .mapNotNull { pass ->
                val riseTime = runCatching {
                    Instant.parse(pass.rise.time).toLocalDateTime(PROPERTY_TIME_ZONE)
                }.getOrNull() ?: return@mapNotNull null
                SkyEvent(
                    title = "ISS pass overhead",
                    detail = "Rises in the ${pass.rise.compass} ${riseTime.formatClockTime()} — " +
                        "look for a bright, fast-moving \"star.\"",
                )
            }
    }

    private suspend fun fetchAuroraAlert(): Result<SkyEvent?> = runCatching {
        val scales: Map<String, NoaaScaleDay> =
            client.get("https://services.swpc.noaa.gov/products/noaa-scales.json").body()
        val maxScale = listOf("0", "1", "2", "3")
            .mapNotNull { scales[it]?.G?.Scale?.toIntOrNull() }
            .maxOrNull() ?: 0
        when {
            maxScale >= AURORA_VISIBLE_CHANCE_SCALE -> SkyEvent(
                title = "Aurora possible",
                detail = "A strong geomagnetic storm is forecast — worth stepping outside and " +
                    "looking north, low on the horizon.",
            )
            maxScale >= AURORA_CAMERA_CHANCE_SCALE -> SkyEvent(
                title = "Aurora — camera only",
                detail = "A geomagnetic storm is forecast. Unlikely to be visible to the eye " +
                    "this far south, but a phone camera on a tripod might catch a faint glow " +
                    "low on the northern horizon.",
            )
            else -> null
        }
    }
}

private fun kotlinx.datetime.DayOfWeek.displayName(): String =
    name.lowercase().replaceFirstChar(Char::uppercase)

private fun LocalDateTime.formatClockTime(): String {
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    val amPm = if (hour < 12) "am" else "pm"
    return "at $hour12:${minute.toString().padStart(2, '0')}$amPm"
}
