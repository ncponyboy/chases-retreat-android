package io.music_assistant.client.stargazing

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil

/** The property's own timezone — used for every "which night is tonight" calculation here. */
val PROPERTY_TIME_ZONE: TimeZone = TimeZone.of("America/New_York")

/** A day either side of "tonight" still counts as worth mentioning; showers/specials aren't a
 *  single-instant event. */
private const val LOOKBACK_DAYS = 1
private const val LOOKAHEAD_DAYS = 3

data class MeteorShower(val name: String, val peakMonth: Int, val peakDay: Int, val zhr: Int)

/**
 * Annual peak dates. These repeat every year, so — unlike [skySpecials] below — this list never
 * needs refreshing. ZHR (zenith hourly rate) is the shower's typical peak rate under ideal
 * conditions; real counts here will be lower, but it's a reasonable relative-intensity signal.
 */
private val meteorShowers = listOf(
    MeteorShower("Quadrantids", 1, 3, 120),
    MeteorShower("Lyrids", 4, 22, 18),
    MeteorShower("Eta Aquariids", 5, 5, 50),
    MeteorShower("Perseids", 8, 12, 100),
    MeteorShower("Draconids", 10, 8, 10),
    MeteorShower("Orionids", 10, 21, 20),
    MeteorShower("Leonids", 11, 17, 15),
    MeteorShower("Geminids", 12, 13, 120),
    MeteorShower("Ursids", 12, 22, 10),
)

enum class SkySpecialType { SUPERMOON, LUNAR_ECLIPSE_TOTAL, LUNAR_ECLIPSE_PARTIAL, LUNAR_ECLIPSE_PENUMBRAL }

data class SkySpecial(val name: String, val date: LocalDate, val type: SkySpecialType)

/**
 * Specific dated events — unlike meteor showers these don't repeat annually, so this list needs
 * fresh entries added periodically. Currently covers through early 2027; add more as new dates
 * are confirmed.
 */
private val skySpecials = listOf(
    SkySpecial("Supermoon", LocalDate(2026, 1, 3), SkySpecialType.SUPERMOON),
    SkySpecial("Total Lunar Eclipse", LocalDate(2026, 3, 3), SkySpecialType.LUNAR_ECLIPSE_TOTAL),
    SkySpecial("Partial Lunar Eclipse", LocalDate(2026, 8, 27), SkySpecialType.LUNAR_ECLIPSE_PARTIAL),
    SkySpecial("Supermoon", LocalDate(2026, 11, 24), SkySpecialType.SUPERMOON),
    SkySpecial("Supermoon", LocalDate(2026, 12, 24), SkySpecialType.SUPERMOON),
    SkySpecial("Supermoon", LocalDate(2027, 1, 22), SkySpecialType.SUPERMOON),
    SkySpecial("Supermoon", LocalDate(2027, 2, 20), SkySpecialType.SUPERMOON),
    SkySpecial("Penumbral Lunar Eclipse", LocalDate(2027, 2, 20), SkySpecialType.LUNAR_ECLIPSE_PENUMBRAL),
    SkySpecial("Partial Lunar Eclipse", LocalDate(2027, 8, 17), SkySpecialType.LUNAR_ECLIPSE_PARTIAL),
)

/** This year's (or next year's, near a year boundary) occurrence of an annual month/day event. */
private fun nearestOccurrence(today: LocalDate, month: Int, day: Int): LocalDate {
    val thisYear = LocalDate(today.year, month, day)
    val diff = today.daysUntil(thisYear)
    // A peak just behind us (within the lookback window) should resolve to itself, not roll
    // forward a full year.
    return if (diff < -LOOKBACK_DAYS) LocalDate(today.year + 1, month, day) else thisYear
}

/** Meteor showers whose peak falls within the "worth mentioning" window around [today]. */
fun upcomingMeteorShowers(today: LocalDate): List<Pair<MeteorShower, LocalDate>> =
    meteorShowers.mapNotNull { shower ->
        val peak = nearestOccurrence(today, shower.peakMonth, shower.peakDay)
        val diff = today.daysUntil(peak)
        if (diff in -LOOKBACK_DAYS..LOOKAHEAD_DAYS) shower to peak else null
    }

/** Supermoons/eclipses within the "worth mentioning" window around [today]. */
fun upcomingSkySpecials(today: LocalDate): List<SkySpecial> =
    skySpecials.filter { special -> today.daysUntil(special.date) in -LOOKBACK_DAYS..LOOKAHEAD_DAYS }
