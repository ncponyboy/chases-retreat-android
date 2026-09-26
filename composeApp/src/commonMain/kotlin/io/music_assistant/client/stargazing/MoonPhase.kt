package io.music_assistant.client.stargazing

import kotlin.math.PI
import kotlin.math.cos
import kotlin.time.Instant

/**
 * Local, offline moon-phase calculation — accurate to well within an hour over any date range
 * we'd plausibly use, which is more than enough for "how washed-out is tonight's sky" framing.
 * No network call, no API, so this is always available even if every other data source fails.
 */
object MoonPhase {
    private val KNOWN_NEW_MOON = Instant.parse("2000-01-06T18:14:00Z")
    private const val SYNODIC_MONTH_DAYS = 29.530588853

    /** 0.0 = new moon, 0.5 = full moon, back to 1.0 = new moon. */
    fun phaseFraction(now: Instant): Double {
        val daysSince = (now - KNOWN_NEW_MOON).inWholeSeconds / 86_400.0
        val phase = (daysSince / SYNODIC_MONTH_DAYS) % 1.0
        return if (phase < 0) phase + 1.0 else phase
    }

    /** 0.0 = new (dark), 1.0 = full (bright). */
    fun illumination(phaseFraction: Double): Double = (1 - cos(2 * PI * phaseFraction)) / 2

    /**
     * Plain-language phase name — deliberately skips real astronomy terms ("Waxing Gibbous",
     * "First Quarter") since most guests won't know what those mean. Doesn't distinguish
     * growing vs. shrinking either; guests care what it looks like tonight, not which direction
     * it's headed.
     */
    fun phaseName(phaseFraction: Double): String {
        val illum = illumination(phaseFraction)
        return when {
            illum < 0.10 -> "New Moon"
            illum < 0.45 -> "Crescent Moon"
            illum < 0.55 -> "Half Moon"
            illum < 0.90 -> "Nearly Full Moon"
            else -> "Full Moon"
        }
    }

    /** Guest-facing framing for how much the moon will interfere with faint objects tonight. */
    fun skyNote(phaseFraction: Double): String {
        val illum = illumination(phaseFraction)
        return when {
            illum < 0.15 -> "Dark skies tonight — great for faint stars and the Milky Way."
            illum < 0.5 -> "Fairly dark skies tonight."
            illum < 0.85 -> "Moonlight will wash out some fainter stars tonight."
            else -> "Bright moonlight tonight — great for moon-gazing, tougher on faint stars."
        }
    }
}
