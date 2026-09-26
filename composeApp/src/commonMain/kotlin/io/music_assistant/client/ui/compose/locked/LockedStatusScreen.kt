package io.music_assistant.client.ui.compose.locked

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.branding.BrandConfig
import io.music_assistant.client.ui.compose.nav.exitApp
import io.music_assistant.client.utils.AuthProcessState
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.mass
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

/**
 * Music Assistant's own connection status, standing in for the Music tab's content while it
 * isn't reachable. There is nothing here for a guest to configure — server address and login
 * are fixed in [BrandConfig] and applied automatically (see
 * [io.music_assistant.client.auth.AuthenticationManager]) — so this screen only ever reflects
 * connection status, with a Retry action for the rare case the server is unreachable.
 *
 * This app only ever talks to the retreat's own Wi-Fi network — there is no internet-reachable
 * fallback. A plain connection failure here is the overwhelmingly common case (a guest who
 * isn't on the property Wi-Fi yet, or hasn't connected since arriving) rather than a genuine
 * fault, so [LockedStatus.NotOnNetwork] reads as an expected, on-brand prompt rather than a
 * scary technical error. [LockedStatus.SignInProblem] is the rarer, genuinely-broken case
 * (the server rejected the locked credentials) that a guest can't self-resolve.
 *
 * @param showExitButton Only meaningful when this covers the whole app (it no longer does —
 *   it's scoped to the Music tab, with the rest of the app usable regardless of this server's
 *   reachability — but the button is kept available for any future full-screen use).
 */
@Composable
fun LockedStatusScreen(showExitButton: Boolean = true) {
    val serviceClient: ServiceClient = koinInject()
    val sessionState by serviceClient.sessionState.collectAsStateWithLifecycle()

    val status = statusFor(sessionState)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            when (status) {
                is LockedStatus.NotOnNetwork ->
                    Icon(
                        imageVector = Icons.Filled.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                else ->
                    Image(
                        painter = painterResource(Res.drawable.mass),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp),
                    )
            }
            Text(
                text = BrandConfig.APP_NAME,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (status is LockedStatus.Progress) {
                CircularProgressIndicator()
            }
            Text(
                text = status.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (status is LockedStatus.SignInProblem) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
            )
            status.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (status is LockedStatus.NotOnNetwork || status is LockedStatus.SignInProblem) {
                Button(onClick = { serviceClient.connect(BrandConfig.lockedConnectionInfo) }) {
                    Text("Try Again")
                }
            }
        }
        if (showExitButton) {
            TextButton(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                onClick = { exitApp() },
            ) {
                Text("Exit")
            }
        }
    }
}

private sealed interface LockedStatus {
    val title: String
    val subtitle: String? get() = null

    data class Progress(override val title: String) : LockedStatus

    /**
     * Can't reach the server at all. On a build with no internet-reachable fallback, this is
     * what "not on the property Wi-Fi (yet)" looks like from the client's point of view — a
     * plain connection failure is indistinguishable from that far more common case, so the
     * copy leads with it instead of implying something is broken.
     */
    data object NotOnNetwork : LockedStatus {
        override val title = "Connect to Chase's Retreat Wi-Fi"
        override val subtitle =
            "This function only works on the property's network. Join the Wi-Fi, then try again."
    }

    /** Reached the server, but it rejected the locked-in credentials — a real, host-side fault. */
    data object SignInProblem : LockedStatus {
        override val title = "Having trouble connecting"
        override val subtitle = "Please let your host know — this one isn't something you can fix."
    }
}

private fun statusFor(sessionState: SessionState): LockedStatus = when (sessionState) {
    is SessionState.Connecting -> LockedStatus.Progress("Connecting…")

    is SessionState.Connected -> when (val dcs = sessionState.dataConnectionState) {
        is DataConnectionState.AwaitingAuth -> when (dcs.authProcessState) {
            is AuthProcessState.Failed -> LockedStatus.SignInProblem
            else -> LockedStatus.Progress("Signing in…")
        }

        DataConnectionState.AwaitingServerInfo -> LockedStatus.Progress("Connecting…")
        is DataConnectionState.Authenticated -> LockedStatus.Progress("Connected.")
    }

    is SessionState.Reconnecting -> LockedStatus.Progress("Reconnecting…")

    is SessionState.Disconnected.Error -> LockedStatus.NotOnNetwork
    SessionState.Disconnected.NoServerData -> LockedStatus.NotOnNetwork
    SessionState.Disconnected.Initial,
    SessionState.Disconnected.ByUser,
    SessionState.Disconnected.Backgrounded,
        -> LockedStatus.Progress("Connecting…")
}
