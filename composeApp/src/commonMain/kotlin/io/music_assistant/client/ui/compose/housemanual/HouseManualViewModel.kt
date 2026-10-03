package io.music_assistant.client.ui.compose.housemanual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.data.repository.HouseManualRepository
import io.music_assistant.client.data.repository.HouseManualSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HouseManualViewModel(
    private val repository: HouseManualRepository,
) : ViewModel() {
    private val _sections = MutableStateFlow(repository.cached() ?: defaultHouseManualSections)
    val sections: StateFlow<List<HouseManualSection>> = _sections.asStateFlow()

    /** Best-effort: on failure the cached (or bundled) text stays on screen. */
    fun refresh() {
        viewModelScope.launch {
            repository.fetch().onSuccess { _sections.value = it }
        }
    }
}
