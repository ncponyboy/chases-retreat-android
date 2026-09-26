package io.music_assistant.client.stargazing

/** One thing worth going outside for, already filtered to "within the next few days." */
data class SkyEvent(
    val title: String,
    val detail: String,
)

data class StargazingStatus(
    val moonPhaseName: String,
    val moonIlluminationPercent: Int,
    val moonSkyNote: String,
    val events: List<SkyEvent>,
)
