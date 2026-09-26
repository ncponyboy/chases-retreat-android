package io.music_assistant.client.ui.compose.housemanual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.branding.HomeAssistantConfig
import io.music_assistant.client.data.repository.HotTubRepository
import io.music_assistant.client.data.repository.HotTubStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HotTubUiState {
    /** [HomeAssistantConfig] has no url/token set — the card stays hidden. */
    data object NotConfigured : HotTubUiState
    data object Loading : HotTubUiState
    data object Unavailable : HotTubUiState
    data class Available(val status: HotTubStatus) : HotTubUiState
}

private const val POLL_INTERVAL_MS = 30_000L

class HotTubViewModel(
    private val repository: HotTubRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<HotTubUiState>(
        if (HomeAssistantConfig.isConfigured) HotTubUiState.Loading else HotTubUiState.NotConfigured,
    )
    val state = _state.asStateFlow()

    init {
        if (HomeAssistantConfig.isConfigured) {
            viewModelScope.launch {
                while (true) {
                    val result = repository.fetchStatus()
                    _state.value = result.fold(
                        onSuccess = { HotTubUiState.Available(it) },
                        onFailure = { HotTubUiState.Unavailable },
                    )
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }
}
