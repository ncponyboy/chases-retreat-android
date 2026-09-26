package io.music_assistant.client.branding

import io.music_assistant.client.api.ConnectionInfo

/**
 * Single source of truth for this locked-down, single-property build.
 *
 * This fork is pinned to one Music Assistant server with one guest account: there is no
 * "add server" / "change account" flow, so every value the rest of the app would normally
 * read from user-editable settings comes from here instead.
 *
 * The server address and guest login are deliberately not written in this file: they live in the
 * gitignored local.properties (see local.properties.example) and are generated into
 * [BrandSecrets] at build time. To point the app at a different server or credentials, edit
 * local.properties and rebuild.
 */
object BrandConfig {
    const val APP_NAME = "Chase's Retreat"

    const val SERVER_HOST = BrandSecrets.SERVER_HOST
    const val SERVER_PORT = BrandSecrets.SERVER_PORT
    const val SERVER_IS_TLS = BrandSecrets.SERVER_IS_TLS

    const val SERVER_USERNAME = BrandSecrets.SERVER_USERNAME
    const val SERVER_PASSWORD = BrandSecrets.SERVER_PASSWORD

    val lockedConnectionInfo: ConnectionInfo
        get() = ConnectionInfo(
            host = SERVER_HOST,
            port = SERVER_PORT,
            isTls = SERVER_IS_TLS,
            basePath = "",
        )
}
