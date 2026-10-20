package io.music_assistant.client.ui.compose.lights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.music_assistant.client.branding.LightsConfig
import io.music_assistant.client.data.repository.PanelMode
import org.koin.compose.viewmodel.koinViewModel

/**
 * Guest controls for the pool room ceiling lights and the LED sign. Everything goes through two
 * local-only Home Assistant webhooks (see [LightsConfig]), so it only works on the house Wi-Fi.
 * The app can't read the lights back, so the highlighted choice is just the guest's last tap.
 */
@Composable
fun LightsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: LightsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = statusBarPadding + contentPadding.calculateTopPadding() + 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Lights",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Works on the Chase's Retreat Wi-Fi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!LightsConfig.isConfigured) {
            item {
                Text(
                    text = "Lights aren't set up in this version of the app.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@LazyColumn
        }

        item {
            Section(title = "Pool Room") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetButton(
                        label = "Party",
                        icon = Icons.Filled.Celebration,
                        selected = state.poolRoom == PoolRoomChoice.Party,
                        onClick = viewModel::party,
                        modifier = Modifier.weight(1f),
                    )
                    PresetButton(
                        label = "White",
                        icon = Icons.Filled.Lightbulb,
                        selected = state.poolRoom == PoolRoomChoice.White,
                        onClick = viewModel::white,
                        modifier = Modifier.weight(1f),
                    )
                    PresetButton(
                        label = "Off",
                        icon = Icons.Filled.PowerSettingsNew,
                        selected = state.poolRoom == PoolRoomChoice.Off,
                        onClick = viewModel::off,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = "Color",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                val selectedColor = (state.poolRoom as? PoolRoomChoice.Color)?.color
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    POOL_ROOM_COLORS.forEach { color ->
                        ColorSwatch(
                            color = color,
                            selected = color == selectedColor,
                            onClick = { viewModel.color(color) },
                        )
                    }
                }

                Text(
                    text = "Brightness ${state.brightness}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Slider(
                    value = state.brightness.toFloat(),
                    onValueChange = { viewModel.setBrightness(it.toInt()) },
                    onValueChangeFinished = viewModel::applyBrightness,
                    valueRange = 10f..100f,
                )
                Text(
                    text = "Party follows the music. If the lights were off, give them up to 30 seconds to come on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Section(title = "LED Sign") {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PanelMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.panel == mode,
                            onClick = { viewModel.panel(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }
        }

        state.message?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        content()
    }
}

@Composable
private fun PresetButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content)
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
private fun ColorSwatch(color: LightColor, selected: Boolean, onClick: () -> Unit) {
    val fill = Color(color.hex.removePrefix("#").toLong(16) or 0xFF000000)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(fill)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClickLabel = color.name, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = color.name, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
