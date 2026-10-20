package io.music_assistant.client.connection

import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.branding.BrandConfig
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import io.music_assistant.client.utils.connectionInfo
import io.music_assistant.client.utils.mainDispatcher
import io.music_assistant.client.webrtc.model.RemoteId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ConnectionManager(
    private val serviceClient: ServiceClient,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)

    val serverBaseUrl: StateFlow<String?> = serviceClient.sessionState
        .map { state ->
            when (state) {
                is SessionState.Connected.Direct -> state.connectionInfo.webUrl
                is SessionState.Reconnecting.Direct -> state.connectionInfo.webUrl
                else -> null
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * True once any session has fully signed in since launch. App-wide (not per screen), so
     * switching tabs doesn't forget it: the Music tab keeps showing its content during a
     * reconnect instead of flashing back to the status screen.
     */
    private val _hasAuthenticated = MutableStateFlow(false)
    val hasAuthenticated: StateFlow<Boolean> = _hasAuthenticated.asStateFlow()

    /** True while a dropped remote session is waiting to retry on its own. */
    private val _remoteRetryPending = MutableStateFlow(false)
    val remoteRetryPending: StateFlow<Boolean> = _remoteRetryPending.asStateFlow()

    /** Whether the most recent connection attempt went through remote access (WebRTC). */
    private var lastAttemptRemote = false
    private var remoteRetryCount = 0
    private var remoteRetryJob: Job? = null

    init {
        RemoteAccess.credentials = settings.getRemoteAccessLogin()
        scope.launch {
            serviceClient.sessionState.collect { state ->
                when (state) {
                    is SessionState.Connected -> {
                        state.connectionInfo?.let { connInfo ->
                            settings.updateConnectionInfo(connInfo)
                        }
                        if (state.dataConnectionState is DataConnectionState.Authenticated) {
                            _hasAuthenticated.value = true
                            remoteRetryCount = 0
                            _remoteRetryPending.value = false
                        }
                    }

                    is SessionState.Reconnecting -> {
                        state.connectionInfo?.let { connInfo ->
                            settings.updateConnectionInfo(connInfo)
                        }
                    }

                    is SessionState.Disconnected -> {
                        when (state) {
                            SessionState.Disconnected.ByUser,
                            SessionState.Disconnected.NoServerData,
                            SessionState.Disconnected.Backgrounded,
                                -> Unit

                            // The house address failed (not on the property Wi-Fi): fall back
                            // to remote access if someone has signed in remotely before. A
                            // dropped remote session retries on its own with a backoff.
                            is SessionState.Disconnected.Error -> onConnectionError()

                            // Locked build: every cold start goes to the house address first,
                            // even if the last session was a remote one.
                            SessionState.Disconnected.Initial -> connectHouse()
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    /** Try the house address; on failure, a saved remote login takes over (see [onConnectionError]). */
    fun retryConnection() {
        remoteRetryJob?.cancel()
        remoteRetryCount = 0
        _remoteRetryPending.value = false
        connectHouse()
    }

    private fun connectHouse() {
        lastAttemptRemote = false
        serviceClient.connect(BrandConfig.lockedConnectionInfo)
    }

    private fun onConnectionError() {
        val saved = RemoteAccess.credentials ?: return
        val remoteId = RemoteId.parse(saved.remoteId) ?: return
        if (!lastAttemptRemote) {
            connectRemoteNow(remoteId)
            return
        }
        if (remoteRetryCount >= MAX_REMOTE_RETRIES) {
            _remoteRetryPending.value = false
            return
        }
        val wait = REMOTE_RETRY_DELAYS_MS[minOf(remoteRetryCount, REMOTE_RETRY_DELAYS_MS.lastIndex)]
        remoteRetryCount++
        _remoteRetryPending.value = true
        remoteRetryJob?.cancel()
        remoteRetryJob = scope.launch {
            delay(wait)
            if (serviceClient.sessionState.value is SessionState.Disconnected.Error) {
                connectRemoteNow(remoteId)
            }
        }
    }

    private fun connectRemoteNow(remoteId: RemoteId) {
        lastAttemptRemote = true
        serviceClient.connectWebRTC(remoteId)
    }

    /**
     * Off-property sign-in (App Review, or someone the host gives a login to). Connects through
     * the server's WebRTC remote-access ID and signs in with exactly these credentials; see
     * [RemoteAccess] for why the built-in guest login is never used remotely. The login is
     * saved so the app reconnects on its own after a relaunch or a dropped connection.
     */
    fun connectRemote(remoteId: RemoteId, username: String, password: String) {
        val credentials = RemoteAccess.Credentials(remoteId.rawId, username.trim(), password)
        RemoteAccess.credentials = credentials
        settings.setRemoteAccessLogin(credentials)
        remoteRetryJob?.cancel()
        remoteRetryCount = 0
        _remoteRetryPending.value = false
        scope.launch {
            // connectWebRTC() refuses while Connecting/Connected, so clear any house attempt first.
            if (serviceClient.sessionState.value !is SessionState.Disconnected) {
                serviceClient.disconnectByUser()
                withTimeoutOrNull(3_000) {
                    serviceClient.sessionState.first { it is SessionState.Disconnected }
                }
            }
            connectRemoteNow(remoteId)
        }
    }

    private companion object {
        const val MAX_REMOTE_RETRIES = 8
        val REMOTE_RETRY_DELAYS_MS = listOf(1_000L, 2_000L, 5_000L, 10_000L, 20_000L, 30_000L)
    }
}
