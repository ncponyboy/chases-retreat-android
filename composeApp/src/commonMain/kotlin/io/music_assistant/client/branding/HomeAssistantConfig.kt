package io.music_assistant.client.branding

/**
 * Config for the optional, narrowly-scoped Home Assistant read of hot tub status.
 *
 * Deliberately separate from [BrandConfig] and deliberately narrow: this app never talks to
 * Home Assistant's general API and never lists entities — it only ever fetches the two specific
 * entity ids below via `/api/states/<entity_id>`. Point it at a dedicated, non-admin Home
 * Assistant user (a long-lived access token for a user that has no reason to see anything else
 * in the house) before going live, not your own admin account.
 *
 * [BASE_URL] left blank keeps the hot tub card hidden entirely — its default state until the
 * hardware is installed, reporting real data, and a scoped token has been created for it.
 */
object HomeAssistantConfig {
    const val BASE_URL = ""
    const val LONG_LIVED_TOKEN = ""

    // Entity ids from the `brianfeucht/esphome-balboa-spa` ESPHome component. Confirm/adjust
    // these against the real entity ids once the device is reporting into Home Assistant.
    const val CLIMATE_ENTITY_ID = "climate.spa"
    const val CONNECTED_ENTITY_ID = "binary_sensor.spa_connected"

    val isConfigured: Boolean get() = BASE_URL.isNotBlank() && LONG_LIVED_TOKEN.isNotBlank()
}
