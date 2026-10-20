package io.music_assistant.client.branding

/**
 * Where the Lights tab sends its requests: the house's Home Assistant on the property network,
 * and the ids of two webhooks there ("Pool Room Lights - Local webhook" and "Panel - Local mode
 * webhook"). Both webhooks are `local_only` in Home Assistant, so they only respond to devices on
 * the house Wi-Fi; a request from anywhere else (including through the cloud) is ignored. The
 * app carries no Home Assistant token, and the webhooks can only do what those two automations
 * allow. Values come from local.properties via the generated [BrandSecrets].
 */
object LightsConfig {
    const val HA_LOCAL_URL = BrandSecrets.HA_LOCAL_URL
    const val POOL_LIGHTS_WEBHOOK_ID = BrandSecrets.POOL_LIGHTS_WEBHOOK_ID
    const val PANEL_WEBHOOK_ID = BrandSecrets.PANEL_WEBHOOK_ID

    val isConfigured: Boolean
        get() = HA_LOCAL_URL.isNotBlank() && POOL_LIGHTS_WEBHOOK_ID.isNotBlank() && PANEL_WEBHOOK_ID.isNotBlank()
}
