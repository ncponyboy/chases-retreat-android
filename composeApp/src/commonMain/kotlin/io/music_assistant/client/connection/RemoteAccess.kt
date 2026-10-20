package io.music_assistant.client.connection

import kotlin.concurrent.Volatile

/**
 * Off-property access to the house Music Assistant, for App Review (and anyone else the host
 * hands a login to). Guests never get this automatically: the built-in guest login only ever
 * travels over the house LAN, and a remote (WebRTC) session signs in solely with credentials
 * typed into the "Remote access sign-in" form.
 *
 * Saved in the secrets store once someone signs in (see
 * [io.music_assistant.client.settings.SettingsRepository.setRemoteAccessLogin]), so a relaunch
 * or a dropped connection reconnects remotely on its own instead of falling back to the
 * "Connect to Wi-Fi" screen. Every cold start still tries the house address first; the saved
 * login is only used when the house isn't reachable. The host can shut access off by
 * disabling the account on the server.
 */
object RemoteAccess {
    /** @param remoteId raw 26-character Music Assistant remote ID (the "access code"). */
    data class Credentials(val remoteId: String, val username: String, val password: String)

    @Volatile
    var credentials: Credentials? = null
}
