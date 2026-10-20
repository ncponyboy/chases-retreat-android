package io.music_assistant.client.ui.compose.lights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.data.repository.LightsRepository
import io.music_assistant.client.data.repository.PanelMode
import io.music_assistant.client.data.repository.PoolRoomPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One color swatch on the Lights tab. */
data class LightColor(val name: String, val hex: String)

val POOL_ROOM_COLORS = listOf(
    LightColor("Red", "#FF0000"),
    LightColor("Orange", "#FF6A00"),
    LightColor("Yellow", "#FFC800"),
    LightColor("Green", "#00D84A"),
    LightColor("Teal", "#00C8C8"),
    LightColor("Blue", "#1E50FF"),
    LightColor("Purple", "#8C28FF"),
    LightColor("Pink", "#FF2D9B"),
)

/** What the guest last picked in this session (the app can't read the lights back). */
sealed interface PoolRoomChoice {
    data object Party : PoolRoomChoice
    data object White : PoolRoomChoice
    data class Color(val color: LightColor) : PoolRoomChoice
    data object Off : PoolRoomChoice
}

data class LightsUiState(
    val poolRoom: PoolRoomChoice? = null,
    val brightness: Int = 100,
    val panel: PanelMode? = null,
    /** A short line under the controls: what was sent, or why it couldn't be. */
    val message: String? = null,
    val messageIsError: Boolean = false,
)

class LightsViewModel(
    private val repository: LightsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LightsUiState())
    val state = _state.asStateFlow()

    fun party() = sendPoolRoom(PoolRoomChoice.Party, "Party mode is on. The lights follow the music.")

    fun white() = sendPoolRoom(PoolRoomChoice.White, "Lights set to white.")

    fun color(color: LightColor) = sendPoolRoom(PoolRoomChoice.Color(color), "Lights set to ${color.name.lowercase()}.")

    fun off() = sendPoolRoom(PoolRoomChoice.Off, "Lights turned off.")

    /** Moves the slider without sending; [applyBrightness] sends once the guest lets go. */
    fun setBrightness(value: Int) = _state.update { it.copy(brightness = value.coerceIn(10, 100)) }

    /** Re-sends the current White/Color choice at the new brightness (Party and Off ignore it). */
    fun applyBrightness() {
        when (val choice = _state.value.poolRoom) {
            PoolRoomChoice.White, is PoolRoomChoice.Color -> sendPoolRoom(choice, "Brightness set to ${_state.value.brightness}%.")
            else -> Unit
        }
    }

    fun panel(mode: PanelMode) {
        _state.update { it.copy(panel = mode) }
        viewModelScope.launch {
            report(repository.setPanel(mode), "LED sign set to ${mode.label.lowercase()}.")
        }
    }

    private fun sendPoolRoom(choice: PoolRoomChoice, success: String) {
        _state.update { it.copy(poolRoom = choice) }
        val brightness = _state.value.brightness
        viewModelScope.launch {
            val result = when (choice) {
                PoolRoomChoice.Party -> repository.setPoolRoom(PoolRoomPreset.Party)
                PoolRoomChoice.White -> repository.setPoolRoom(PoolRoomPreset.White, brightness = brightness)
                is PoolRoomChoice.Color -> repository.setPoolRoom(PoolRoomPreset.Color, choice.color.hex, brightness)
                PoolRoomChoice.Off -> repository.setPoolRoom(PoolRoomPreset.Off)
            }
            report(result, success)
        }
    }

    private fun report(result: Result<Unit>, success: String) {
        _state.update {
            if (result.isSuccess) {
                it.copy(message = success, messageIsError = false)
            } else {
                it.copy(
                    message = "Couldn't reach the house. Lights only work on the Chase's Retreat Wi-Fi.",
                    messageIsError = true,
                )
            }
        }
    }
}
