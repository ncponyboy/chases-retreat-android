package io.music_assistant.client.ui.compose.thingstodo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.data.repository.StargazingRepository
import io.music_assistant.client.stargazing.StargazingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StargazingUiState {
    data object Loading : StargazingUiState
    data class Content(val status: StargazingStatus) : StargazingUiState
}

/**
 * One fetch per screen visit (a new instance is created each time, via Koin's `factory`) rather
 * than continuous polling — none of this data changes fast enough to need a live loop like the
 * hot tub card does.
 */
class StargazingViewModel(
    private val repository: StargazingRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<StargazingUiState>(StargazingUiState.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = StargazingUiState.Content(repository.fetchStatus())
        }
    }
}
