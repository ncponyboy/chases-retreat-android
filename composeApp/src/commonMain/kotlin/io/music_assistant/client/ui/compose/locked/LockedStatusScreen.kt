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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.branding.BrandConfig
import io.music_assistant.client.connection.ConnectionManager
import io.music_assistant.client.webrtc.model.RemoteId
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
    val connectionManager: ConnectionManager = koinInject()
    var showRemoteSignIn by remember { mutableStateOf(false) }
    val sessionState by serviceClient.sessionState.collectAsStateWithLifecycle()
    val remoteRetryPending by connectionManager.remoteRetryPending.collectAsStateWithLifecycle()

    val status = statusFor(sessionState, remoteRetryPending)

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
                // House address first; a saved remote-access login takes over if that fails.
                Button(onClick = { connectionManager.retryConnection() }) {
                    Text("Try Again")
                }
                // Off-property access needs a login the host hands out (e.g. to App Review);
                // the built-in guest login never works remotely. See RemoteAccess.
                TextButton(onClick = { showRemoteSignIn = true }) {
                    Text("Remote access sign-in")
                }
            }
        }
        if (showRemoteSignIn) {
            RemoteSignInDialog(
                onDismiss = { showRemoteSignIn = false },
                onSignIn = { remoteId, username, password ->
                    showRemoteSignIn = false
                    connectionManager.connectRemote(remoteId, username, password)
                },
            )
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

private fun statusFor(sessionState: SessionState, remoteRetryPending: Boolean): LockedStatus =
    // A dropped remote-access session that is retrying on its own, not a Wi-Fi problem.
    if (remoteRetryPending && sessionState is SessionState.Disconnected.Error) {
        LockedStatus.Progress("Reconnecting…")
    } else {
        statusFor(sessionState)
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
    // ByUser here means a connection was deliberately dropped (e.g. a remote session with no
    // remote-access login), so offer the same Try Again / sign-in choices.
    SessionState.Disconnected.ByUser -> LockedStatus.NotOnNetwork
    SessionState.Disconnected.Initial,
    SessionState.Disconnected.Backgrounded,
        -> LockedStatus.Progress("Connecting…")
}

@Composable
private fun RemoteSignInDialog(
    onDismiss: () -> Unit,
    onSignIn: (RemoteId, String, String) -> Unit,
) {
    var accessCode by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remote access sign-in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "For use away from Chase's Retreat with a login from your host.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = accessCode,
                    onValueChange = { accessCode = it; codeError = false },
                    label = { Text("Access code") },
                    isError = codeError,
                    supportingText = if (codeError) {
                        { Text("That access code isn't valid.") }
                    } else {
                        null
                    },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = accessCode.isNotBlank() && username.isNotBlank() && password.isNotEmpty(),
                onClick = {
                    val id = RemoteId.parse(accessCode)
                    if (id == null) codeError = true else onSignIn(id, username, password)
                },
            ) {
                Text("Sign in")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
