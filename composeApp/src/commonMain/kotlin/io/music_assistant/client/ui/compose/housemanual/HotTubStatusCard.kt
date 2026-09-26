package io.music_assistant.client.ui.compose.housemanual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * Live hot tub status, sitting just below the static "Hot Tub" section in the house manual.
 * Renders nothing at all when [HotTubUiState.NotConfigured] or still [HotTubUiState.Loading] —
 * the card is meant to be invisible until the hardware is actually installed and reporting, not
 * a placeholder guests see and wonder about.
 */
@Composable
fun HotTubStatusCard(viewModel: HotTubViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        HotTubUiState.NotConfigured, HotTubUiState.Loading -> Unit

        HotTubUiState.Unavailable -> {
            Text(
                text = "Hot tub status is unavailable right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is HotTubUiState.Available -> {
            val status = s.status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (status.isHeating) "Heating" else "Ready",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                status.currentTempF?.let { temp ->
                    Text(
                        text = "${temp.roundToInt()}°F",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                status.targetTempF?.let { target ->
                    Text(
                        text = "Set to ${target.roundToInt()}°F",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
