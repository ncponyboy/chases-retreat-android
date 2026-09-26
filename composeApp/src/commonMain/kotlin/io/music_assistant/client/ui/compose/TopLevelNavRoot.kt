package io.music_assistant.client.ui.compose

import androidx.compose.runtime.Composable
import io.music_assistant.client.ui.compose.home.MainNavigationRoot

/**
 * The app's entry point below [App]. Always shows [MainNavigationRoot] — the 5-tab shell
 * (Welcome / Music / Local Businesses / Things to Do / House Manual) — regardless of whether
 * Music Assistant is reachable.
 *
 * This used to switch between a "Main" and a full-screen "Settings" (connection status)
 * destination whenever the Music Assistant server was unreachable, which made sense back when
 * this app was nothing but a Music Assistant client. Now that Local Businesses / Things to Do /
 * House Manual are independent, always-available content, that made the whole app unusable
 * whenever a guest wasn't yet on the property Wi-Fi. Connection status is now handled inside
 * the Music tab only — see the Music entry in [MainNavigationRoot] and
 * [io.music_assistant.client.ui.compose.locked.LockedStatusScreen].
 */
@Composable
fun TopLevelNavRoot() {
    MainNavigationRoot()
}
